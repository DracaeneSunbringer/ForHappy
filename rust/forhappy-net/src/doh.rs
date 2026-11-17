//! Direct-IP public DoH client (RFC 8484) for Cloudflare candidate discovery, ECH key fetching,
//! and anti-poisoning resolution of external hosts.

use crate::cfnet;
use crate::ech_tls;
use bytes::Bytes;
use hickory_proto::op::{Message, Query, ResponseCode};
use hickory_proto::rr::rdata::svcb::{SvcParamKey, SvcParamValue};
use hickory_proto::rr::{Name, RData, RecordType};
use http_body_util::{BodyExt, Full};
use hyper_util::rt::TokioIo;
use parking_lot::RwLock;
use rustls::pki_types::ServerName;
use rustls::ClientConfig;
use std::collections::HashMap;
use std::net::{IpAddr, Ipv4Addr, SocketAddr};
use std::str::FromStr;
use std::sync::Arc;
use std::time::{Duration, Instant};
use tokio::net::TcpStream;
use tokio::task::JoinSet;
use tokio_rustls::TlsConnector;

#[derive(Clone, Copy)]
pub struct PublicDohEndpoint {
    pub name: &'static str,
    pub ip: IpAddr,
    pub sni: &'static str,
    pub path: &'static str,
}

/// All endpoints use IP literals with IP-SAN TLS certificates — never touched by system DNS.
pub const PUBLIC_DOH_ENDPOINTS: &[PublicDohEndpoint] = &[
    PublicDohEndpoint {
        name: "AliDNS-1",
        ip: IpAddr::V4(Ipv4Addr::new(223, 5, 5, 5)),
        sni: "dns.alidns.com",
        path: "/dns-query",
    },
    PublicDohEndpoint {
        name: "AliDNS-2",
        ip: IpAddr::V4(Ipv4Addr::new(223, 6, 6, 6)),
        sni: "dns.alidns.com",
        path: "/dns-query",
    },
    PublicDohEndpoint {
        name: "DNSPod-1",
        ip: IpAddr::V4(Ipv4Addr::new(1, 12, 12, 12)),
        sni: "doh.pub",
        path: "/dns-query",
    },
    PublicDohEndpoint {
        name: "DNSPod-2",
        ip: IpAddr::V4(Ipv4Addr::new(120, 53, 53, 53)),
        sni: "doh.pub",
        path: "/dns-query",
    },
    PublicDohEndpoint {
        name: "Cloudflare",
        ip: IpAddr::V4(Ipv4Addr::new(1, 1, 1, 1)),
        sni: "cloudflare-dns.com",
        path: "/dns-query",
    },
    PublicDohEndpoint {
        name: "Quad9",
        ip: IpAddr::V4(Ipv4Addr::new(9, 9, 9, 9)),
        sni: "dns.quad9.net",
        path: "/dns-query",
    },
];

#[derive(Clone, Copy, PartialEq, Eq)]
pub enum Verdict {
    Accept,
    Negative,
    Reject,
}

pub fn encode_dns_query(domain: &str, rtype: RecordType) -> Result<Vec<u8>, String> {
    let fqdn = if domain.ends_with('.') {
        domain.to_string()
    } else {
        format!("{domain}.")
    };
    let name = Name::from_str(&fqdn).map_err(|e| format!("bad domain {domain}: {e}"))?;
    let mut msg = Message::new();
    msg.add_query(Query::query(name, rtype));
    msg.set_recursion_desired(true);
    msg.to_vec().map_err(|e| format!("dns encode failed: {e}"))
}

async fn send_doh_query(
    ep: PublicDohEndpoint,
    wire: Arc<Vec<u8>>,
    tls_cfg: Arc<ClientConfig>,
    timeout: Duration,
) -> Option<Message> {
    let addr = SocketAddr::new(ep.ip, 443);
    let tcp = tokio::time::timeout(timeout, TcpStream::connect(addr))
        .await
        .ok()?
        .ok()?;
    let _ = tcp.set_nodelay(true);

    // Connect using IP ServerName (all listed public DoH endpoints include IP SANs),
    // fallback to sni if needed.
    let sni = ServerName::from(ep.ip);
    let connector = TlsConnector::from(tls_cfg);
    let stream = tokio::time::timeout(timeout, connector.connect(sni, tcp))
        .await
        .ok()?
        .ok()?;

    let (mut sender, conn) = hyper::client::conn::http1::handshake(TokioIo::new(stream))
        .await
        .ok()?;
    tokio::spawn(async move {
        let _ = conn.await;
    });

    let req = http::Request::builder()
        .method(http::Method::POST)
        .uri(ep.path)
        .header(http::header::HOST, ep.sni)
        .header(http::header::CONTENT_TYPE, "application/dns-message")
        .header(http::header::ACCEPT, "application/dns-message")
        .header(http::header::CONNECTION, "close")
        .body(Full::new(Bytes::copy_from_slice(&wire)))
        .ok()?;

    let resp = tokio::time::timeout(timeout, sender.send_request(req))
        .await
        .ok()?
        .ok()?;
    if !resp.status().is_success() {
        return None;
    }
    let body = tokio::time::timeout(timeout, resp.into_body().collect())
        .await
        .ok()?
        .ok()?
        .to_bytes();
    Message::from_vec(&body).ok()
}

pub async fn public_query(
    domain: &str,
    rtype: RecordType,
    accept: fn(&Message) -> Verdict,
    timeout: Duration,
) -> Option<Message> {
    let wire = Arc::new(encode_dns_query(domain, rtype).ok()?);
    let tls_cfg = ech_tls::build_tls_config(None, vec![b"http/1.1".to_vec()]).ok()?;

    let mut set = JoinSet::new();
    for &ep in PUBLIC_DOH_ENDPOINTS {
        let w = wire.clone();
        let cfg = tls_cfg.clone();
        set.spawn(async move { send_doh_query(ep, w, cfg, timeout).await });
    }

    let deadline = Instant::now() + timeout;
    let mut negatives = 0usize;
    while let Ok(Some(res)) =
        tokio::time::timeout(deadline.saturating_duration_since(Instant::now()), set.join_next())
            .await
    {
        if let Ok(Some(msg)) = res {
            match accept(&msg) {
                Verdict::Accept => {
                    set.abort_all();
                    return Some(msg);
                }
                Verdict::Negative => {
                    negatives += 1;
                    if negatives >= 3 {
                        break;
                    }
                }
                Verdict::Reject => {}
            }
        }
    }
    set.abort_all();
    None
}

pub fn accept_cf_ips(msg: &Message) -> Verdict {
    let ips = extract_ips(msg);
    if ips.is_empty() {
        return if matches!(
            msg.response_code(),
            ResponseCode::NoError | ResponseCode::NXDomain
        ) {
            Verdict::Negative
        } else {
            Verdict::Reject
        };
    }
    if cfnet::all_cloudflare(&ips) {
        Verdict::Accept
    } else {
        Verdict::Reject
    }
}

pub fn accept_any_ips(msg: &Message) -> Verdict {
    let ips = extract_ips(msg);
    if ips.is_empty() {
        return if matches!(
            msg.response_code(),
            ResponseCode::NoError | ResponseCode::NXDomain
        ) {
            Verdict::Negative
        } else {
            Verdict::Reject
        };
    }
    Verdict::Accept
}

pub fn accept_ech_https(msg: &Message) -> Verdict {
    if extract_ech_from_message(msg).is_some() {
        Verdict::Accept
    } else if matches!(
        msg.response_code(),
        ResponseCode::NoError | ResponseCode::NXDomain
    ) {
        Verdict::Negative
    } else {
        Verdict::Reject
    }
}

pub fn extract_ips(msg: &Message) -> Vec<IpAddr> {
    msg.answers()
        .iter()
        .filter_map(|rec| match rec.data() {
            Some(RData::A(a)) => Some(IpAddr::V4(a.0)),
            Some(RData::AAAA(aaaa)) => Some(IpAddr::V6(aaaa.0)),
            _ => None,
        })
        .collect()
}

pub fn extract_ech_from_message(msg: &Message) -> Option<Vec<u8>> {
    for rec in msg.answers() {
        if let Some(RData::HTTPS(https)) = rec.data() {
            for (key, value) in https.svc_params() {
                if *key == SvcParamKey::EchConfig {
                    if let SvcParamValue::EchConfig(ech) = value {
                        let bytes = ech_tls::ensure_ech_len_prefix(ech.0.clone());
                        if ech_tls::ech_key_usable(&bytes) {
                            return Some(bytes);
                        }
                    }
                }
            }
        }
    }
    None
}

pub async fn resolve_cf_domain(domain: &str, v4: bool, v6: bool) -> Vec<IpAddr> {
    let timeout = Duration::from_millis(2800);
    let mut out = Vec::new();
    match (v4, v6) {
        (true, true) => {
            let (m4, m6) = tokio::join!(
                public_query(domain, RecordType::A, accept_cf_ips, timeout),
                public_query(domain, RecordType::AAAA, accept_cf_ips, timeout),
            );
            if let Some(m) = m4 {
                for ip in extract_ips(&m) {
                    if !cfnet::is_skipped(ip) && !out.contains(&ip) {
                        out.push(ip);
                    }
                }
            }
            if let Some(m) = m6 {
                for ip in extract_ips(&m) {
                    if !out.contains(&ip) {
                        out.push(ip);
                    }
                }
            }
        }
        (true, false) => {
            if let Some(m) = public_query(domain, RecordType::A, accept_cf_ips, timeout).await {
                for ip in extract_ips(&m) {
                    if !cfnet::is_skipped(ip) && !out.contains(&ip) {
                        out.push(ip);
                    }
                }
            }
        }
        (false, true) => {
            if let Some(m) = public_query(domain, RecordType::AAAA, accept_cf_ips, timeout).await {
                for ip in extract_ips(&m) {
                    if !out.contains(&ip) {
                        out.push(ip);
                    }
                }
            }
        }
        (false, false) => {}
    }
    out
}

pub async fn fetch_ech_key(domain: &str) -> Option<Vec<u8>> {
    let msg = public_query(
        domain,
        RecordType::HTTPS,
        accept_ech_https,
        Duration::from_millis(2800),
    )
    .await?;
    extract_ech_from_message(&msg)
}

/// Cached resolver for non-covered external hosts (e.g. OAuth providers, external image links).
pub struct ExternalDnsCache {
    cache: RwLock<HashMap<String, (Vec<IpAddr>, Instant)>>,
}

impl ExternalDnsCache {
    pub fn new() -> Self {
        Self {
            cache: RwLock::new(HashMap::new()),
        }
    }

    pub async fn resolve(&self, host: &str) -> Vec<IpAddr> {
        if let Ok(ip) = host.trim_matches(['[', ']']).parse::<IpAddr>() {
            return vec![ip];
        }
        let key = host.to_ascii_lowercase();
        if let Some((ips, at)) = self.cache.read().get(&key) {
            if at.elapsed() < Duration::from_secs(300) && !ips.is_empty() {
                return ips.clone();
            }
        }
        let timeout = Duration::from_millis(2500);
        let (m4, m6) = tokio::join!(
            public_query(&key, RecordType::A, accept_any_ips, timeout),
            public_query(&key, RecordType::AAAA, accept_any_ips, timeout),
        );
        let mut ips = Vec::new();
        if let Some(m) = m4 {
            ips.extend(extract_ips(&m));
        }
        if let Some(m) = m6 {
            for ip in extract_ips(&m) {
                if !ips.contains(&ip) {
                    ips.push(ip);
                }
            }
        }
        if ips.is_empty() {
            if let Ok(addrs) = tokio::net::lookup_host((host, 443)).await {
                for sa in addrs {
                    if !ips.contains(&sa.ip()) {
                        ips.push(sa.ip());
                    }
                }
            }
        }
        if !ips.is_empty() {
            let mut w = self.cache.write();
            if w.len() > 256 {
                w.clear();
            }
            w.insert(key, (ips.clone(), Instant::now()));
        }
        ips
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn encode_dns_query_produces_valid_wire_message() {
        let wire = encode_dns_query("icook.tw", RecordType::HTTPS).unwrap();
        let parsed = Message::from_vec(&wire).unwrap();
        assert_eq!(parsed.queries().len(), 1);
        assert_eq!(parsed.queries()[0].query_type(), RecordType::HTTPS);
    }
}
