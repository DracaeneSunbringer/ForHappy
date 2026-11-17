//! Single-flight upstream HTTP/2 + HTTP/1.1 connection pool with:
//! - 32 MiB H2 connection window & 20s idle PING keepalive
//! - Android sleep/suspend detection (wall clock jump > 45s) & explicit app-resume eviction
//! - Automatic ECH retry_configs key adoption
//! - Preconnect support for 52dmeme.com and img.52dmeme.com

use crate::doh::ExternalDnsCache;
use crate::ech_tls::{self, TlsConnectError};
use crate::edge_pin::{self, EdgePin};
use crate::selector::EdgeSelector;
use bytes::Bytes;
use http_body_util::combinators::BoxBody;
use http_body_util::{BodyExt, Empty};
use hyper::body::Incoming;
use hyper::client::conn::{http1, http2};
use hyper::{Method, Request, Response};
use hyper_util::rt::{TokioExecutor, TokioIo, TokioTimer};
use parking_lot::Mutex;
use std::collections::HashMap;
use std::net::IpAddr;
use std::sync::atomic::{AtomicU64, AtomicUsize, Ordering};
use std::sync::Arc;
use std::time::{Duration, Instant, SystemTime};
use tokio::sync::watch;

pub type PoolBody = BoxBody<Bytes, hyper::Error>;

const H2_MAX_CONNS_PER_KEY: usize = 3;
const H2_SOFT_STREAM_CAP: usize = 48;
const H2_IDLE_TTL: Duration = Duration::from_secs(90);
const H1_IDLE_TTL: Duration = Duration::from_secs(30);
const H2_CONN_WINDOW: u32 = 32 * 1024 * 1024;
const H2_STREAM_WINDOW: u32 = 4 * 1024 * 1024;
const H2_KEEPALIVE_INTERVAL: Duration = Duration::from_secs(20);
const H2_KEEPALIVE_TIMEOUT: Duration = Duration::from_secs(10);
const SUSPEND_GAP: Duration = Duration::from_secs(45);

#[derive(Clone, Debug, PartialEq, Eq, Hash)]
struct PoolKey {
    host: String,
    port: u16,
}

struct H2Conn {
    sender: http2::SendRequest<PoolBody>,
    in_flight: Arc<AtomicUsize>,
    epoch: u64,
    last_used: Mutex<Instant>,
}

struct H1Conn {
    sender: http1::SendRequest<PoolBody>,
    epoch: u64,
    last_used: Mutex<Instant>,
}

enum DialedSender {
    H2(Arc<H2Conn>),
    H1(H1Conn),
}

struct HostBucket {
    h2: Vec<Arc<H2Conn>>,
    h1: Vec<H1Conn>,
}

impl Default for HostBucket {
    fn default() -> Self {
        Self {
            h2: Vec::new(),
            h1: Vec::new(),
        }
    }
}

pub struct UpstreamPool {
    edge_pin: Arc<EdgePin>,
    selector: Arc<EdgeSelector>,
    external_dns: Arc<ExternalDnsCache>,
    buckets: Mutex<HashMap<PoolKey, HostBucket>>,
    inflight_dials: Mutex<HashMap<PoolKey, watch::Receiver<Option<Result<(), String>>>>>,
    generation: AtomicU64,
    last_wall_tick: Mutex<SystemTime>,
}

impl UpstreamPool {
    pub fn new(
        edge_pin: Arc<EdgePin>,
        selector: Arc<EdgeSelector>,
        external_dns: Arc<ExternalDnsCache>,
    ) -> Arc<Self> {
        let pool = Arc::new(Self {
            edge_pin,
            selector,
            external_dns,
            buckets: Mutex::new(HashMap::new()),
            inflight_dials: Mutex::new(HashMap::new()),
            generation: AtomicU64::new(1),
            last_wall_tick: Mutex::new(SystemTime::now()),
        });
        let p = pool.clone();
        tokio::spawn(async move {
            let mut interval = tokio::time::interval(Duration::from_secs(15));
            loop {
                interval.tick().await;
                p.reap_and_detect_suspend();
            }
        });
        pool
    }

    pub fn invalidate_all(&self, reason: &str) {
        self.generation.fetch_add(1, Ordering::Release);
        let mut buckets = self.buckets.lock();
        buckets.clear();
        self.selector.push_log(format!("pool: invalidated all connections ({reason})"));
    }

    fn current_epoch(&self) -> u64 {
        self.generation
            .load(Ordering::Acquire)
            .wrapping_add(self.edge_pin.epoch())
    }

    fn reap_and_detect_suspend(&self) {
        let now_wall = SystemTime::now();
        let jumped = {
            let mut last = self.last_wall_tick.lock();
            let elapsed = now_wall.duration_since(*last).unwrap_or(Duration::ZERO);
            *last = now_wall;
            elapsed > Duration::from_secs(15) + SUSPEND_GAP
        };
        if jumped {
            self.invalidate_all("system sleep/suspend gap detected");
            return;
        }
        let epoch = self.current_epoch();
        let mut buckets = self.buckets.lock();
        for bucket in buckets.values_mut() {
            bucket.h2.retain(|c| {
                c.epoch == epoch
                    && !c.sender.is_closed()
                    && (c.in_flight.load(Ordering::Relaxed) > 0
                        || c.last_used.lock().elapsed() < H2_IDLE_TTL)
            });
            bucket.h1.retain(|c| {
                c.epoch == epoch
                    && !c.sender.is_closed()
                    && c.last_used.lock().elapsed() < H1_IDLE_TTL
            });
        }
        buckets.retain(|_, b| !b.h2.is_empty() || !b.h1.is_empty());
    }

    pub fn preconnect(self: &Arc<Self>, hosts: &[&str]) {
        for &h in hosts {
            let pool = self.clone();
            let host = h.to_string();
            tokio::spawn(async move {
                let key = PoolKey { host, port: 443 };
                let _ = pool.ensure_connection(&key).await;
            });
        }
    }

    pub async fn resolve_connect_ips(&self, host: &str) -> Vec<IpAddr> {
        if edge_pin::is_covered_host(host) {
            if let Some(winner) = self.edge_pin.current_winner() {
                return vec![winner];
            }
            if let Some(winner) = self.selector.wait_for_winner(Duration::from_secs(4)).await {
                return vec![winner];
            }
        }
        self.external_dns.resolve(host).await
    }

    async fn dial_single(&self, key: &PoolKey) -> Result<DialedSender, String> {
        let covered = edge_pin::is_covered_host(&key.host);
        let ips = self.resolve_connect_ips(&key.host).await;
        if ips.is_empty() {
            return Err(format!("DNS resolution failed for {}", key.host));
        }

        let tcp_to = Duration::from_millis(3500);
        let tls_to = Duration::from_millis(5000);
        let mut last_err = String::from("no target IP");

        for ip in ips {
            let mut active_ech = if covered {
                self.selector.latest_ech_key()
            } else {
                None
            };

            for attempt in 0..2 {
                let alpn = vec![b"h2".to_vec(), b"http/1.1".to_vec()];
                let tls_cfg = ech_tls::build_tls_config(active_ech.as_deref(), alpn)?;
                match ech_tls::connect_tls(ip, key.port, &key.host, tls_cfg, tcp_to, tls_to).await {
                    Ok((stream, _ech_accepted)) => {
                        let is_h2 = stream.get_ref().1.alpn_protocol() == Some(b"h2");
                        let epoch = self.current_epoch();
                        if is_h2 {
                            let mut builder = http2::Builder::new(TokioExecutor::new());
                            builder
                                .timer(TokioTimer::new())
                                .initial_connection_window_size(H2_CONN_WINDOW)
                                .initial_stream_window_size(H2_STREAM_WINDOW)
                                .keep_alive_interval(Some(H2_KEEPALIVE_INTERVAL))
                                .keep_alive_timeout(H2_KEEPALIVE_TIMEOUT)
                                .keep_alive_while_idle(true);
                            let (sender, conn) = builder
                                .handshake(TokioIo::new(stream))
                                .await
                                .map_err(|e| format!("h2 handshake failed: {e}"))?;
                            tokio::spawn(async move {
                                let _ = conn.await;
                            });
                            self.edge_pin.report_success(ip);
                            let h2_conn = Arc::new(H2Conn {
                                sender,
                                in_flight: Arc::new(AtomicUsize::new(0)),
                                epoch,
                                last_used: Mutex::new(Instant::now()),
                            });
                            let mut buckets = self.buckets.lock();
                            let bucket = buckets.entry(key.clone()).or_default();
                            bucket.h2.push(h2_conn.clone());
                            return Ok(DialedSender::H2(h2_conn));
                        } else {
                            let (sender, conn) = http1::Builder::new()
                                .handshake(TokioIo::new(stream))
                                .await
                                .map_err(|e| format!("h1 handshake failed: {e}"))?;
                            tokio::spawn(async move {
                                let _ = conn.await;
                            });
                            self.edge_pin.report_success(ip);
                            return Ok(DialedSender::H1(H1Conn {
                                sender,
                                epoch,
                                last_used: Mutex::new(Instant::now()),
                            }));
                        }
                    }
                    Err(TlsConnectError::EchRejected {
                        retry_configs: Some(fresh),
                    }) if attempt == 0 => {
                        self.selector.adopt_ech_key(&fresh);
                        active_ech = Some(fresh);
                        continue;
                    }
                    Err(TlsConnectError::EchRejected { .. }) | Err(TlsConnectError::Tls(_))
                        if attempt == 0 && active_ech.is_some() =>
                    {
                        active_ech = None;
                        continue;
                    }
                    Err(e) => {
                        last_err = e.to_string();
                        if covered {
                            self.edge_pin.report_failure(ip);
                        }
                        break;
                    }
                }
            }
        }
        Err(last_err)
    }

    fn pick_pooled(&self, key: &PoolKey) -> Option<DialedSender> {
        let epoch = self.current_epoch();
        let mut buckets = self.buckets.lock();
        let bucket = buckets.get_mut(key)?;
        bucket.h2.retain(|c| c.epoch == epoch && !c.sender.is_closed());
        if let Some(best_h2) = bucket
            .h2
            .iter()
            .filter(|c| c.in_flight.load(Ordering::Relaxed) < H2_SOFT_STREAM_CAP)
            .min_by_key(|c| c.in_flight.load(Ordering::Relaxed))
        {
            *best_h2.last_used.lock() = Instant::now();
            return Some(DialedSender::H2(best_h2.clone()));
        }
        if bucket.h2.len() >= H2_MAX_CONNS_PER_KEY {
            if let Some(least) = bucket
                .h2
                .iter()
                .min_by_key(|c| c.in_flight.load(Ordering::Relaxed))
            {
                *least.last_used.lock() = Instant::now();
                return Some(DialedSender::H2(least.clone()));
            }
        }
        while let Some(h1) = bucket.h1.pop() {
            if h1.epoch == epoch && !h1.sender.is_closed() {
                return Some(DialedSender::H1(h1));
            }
        }
        None
    }

    async fn ensure_connection(&self, key: &PoolKey) -> Result<DialedSender, String> {
        if let Some(existing) = self.pick_pooled(key) {
            return Ok(existing);
        }

        enum Role {
            Leader(watch::Sender<Option<Result<(), String>>>),
            Follower(watch::Receiver<Option<Result<(), String>>>),
        }

        let role = {
            let mut dials = self.inflight_dials.lock();
            if let Some(rx) = dials.get(key) {
                Role::Follower(rx.clone())
            } else {
                let (tx, rx) = watch::channel(None);
                dials.insert(key.clone(), rx);
                Role::Leader(tx)
            }
        };

        match role {
            Role::Leader(tx) => {
                let res = self.dial_single(key).await;
                self.inflight_dials.lock().remove(key);
                let _ = tx.send(Some(res.as_ref().map(|_| ()).map_err(|e| e.clone())));
                res
            }
            Role::Follower(mut rx) => {
                loop {
                    let current = rx.borrow().clone();
                    if let Some(outcome) = current {
                        match outcome {
                            Ok(()) => {
                                if let Some(c) = self.pick_pooled(key) {
                                    return Ok(c);
                                }
                                return self.dial_single(key).await;
                            }
                            Err(e) => return Err(e),
                        }
                    }
                    if rx.changed().await.is_err() {
                        return self.dial_single(key).await;
                    }
                }
            }
        }
    }

    pub async fn forward_http(
        self: &Arc<Self>,
        target_host: &str,
        target_port: u16,
        req: Request<PoolBody>,
    ) -> Result<Response<Incoming>, String> {
        let key = PoolKey {
            host: target_host.to_ascii_lowercase(),
            port: target_port,
        };
        let is_safe_retry = matches!(
            *req.method(),
            Method::GET | Method::HEAD | Method::OPTIONS
        );
        let (parts, body) = req.into_parts();
        let req_uri_str = parts
            .uri
            .path_and_query()
            .map(|pq| pq.as_str())
            .unwrap_or("/");

        let mut body_opt = Some(body);
        for attempt in 0..2 {
            let sender = if attempt == 0 {
                self.ensure_connection(&key).await?
            } else {
                self.dial_single(&key).await?
            };

            let req_body = body_opt
                .take()
                .unwrap_or_else(|| Empty::<Bytes>::new().map_err(|n| match n {}).boxed());

            match sender {
                DialedSender::H2(h2) => {
                    let mut builder = Request::builder().method(parts.method.clone()).uri(format!(
                        "https://{}{}",
                        key.host, req_uri_str
                    ));
                    for (name, val) in &parts.headers {
                        if !is_hop_by_hop(name.as_str()) && name != http::header::HOST {
                            builder = builder.header(name, val);
                        }
                    }
                    let out_req = builder
                        .body(req_body)
                        .map_err(|e| format!("build h2 request failed: {e}"))?;

                    let mut send_handle = h2.sender.clone();
                    if send_handle.ready().await.is_err() {
                        if attempt == 0 && is_safe_retry {
                            continue;
                        }
                        return Err("upstream h2 connection closed before send".to_string());
                    }

                    h2.in_flight.fetch_add(1, Ordering::Relaxed);
                    let resp_res = send_handle.send_request(out_req).await;
                    h2.in_flight.fetch_sub(1, Ordering::Relaxed);
                    *h2.last_used.lock() = Instant::now();

                    match resp_res {
                        Ok(resp) => return Ok(resp),
                        Err(e) => {
                            if attempt == 0 && is_safe_retry {
                                continue;
                            }
                            return Err(format!("h2 upstream error: {e}"));
                        }
                    }
                }
                DialedSender::H1(mut h1) => {
                    let mut builder = Request::builder()
                        .method(parts.method.clone())
                        .uri(req_uri_str)
                        .header(http::header::HOST, &key.host);
                    for (name, val) in &parts.headers {
                        if !is_hop_by_hop(name.as_str()) && name != http::header::HOST {
                            builder = builder.header(name, val);
                        }
                    }
                    let out_req = builder
                        .body(req_body)
                        .map_err(|e| format!("build h1 request failed: {e}"))?;

                    if h1.sender.ready().await.is_err() {
                        if attempt == 0 && is_safe_retry {
                            continue;
                        }
                        return Err("upstream h1 connection closed before send".to_string());
                    }
                    let resp_res = h1.sender.send_request(out_req).await;
                    *h1.last_used.lock() = Instant::now();
                    match resp_res {
                        Ok(resp) => {
                            let mut buckets = self.buckets.lock();
                            let b = buckets.entry(key.clone()).or_default();
                            if b.h1.len() < 4 && !h1.sender.is_closed() {
                                b.h1.push(h1);
                            }
                            return Ok(resp);
                        }
                        Err(e) => {
                            if attempt == 0 && is_safe_retry {
                                continue;
                            }
                            return Err(format!("h1 upstream error: {e}"));
                        }
                    }
                }
            }
        }
        Err("upstream send retries exhausted".to_string())
    }
}

fn is_hop_by_hop(name: &str) -> bool {
    matches!(
        name.to_ascii_lowercase().as_str(),
        "connection"
            | "keep-alive"
            | "proxy-authenticate"
            | "proxy-authorization"
            | "proxy-connection"
            | "te"
            | "trailer"
            | "transfer-encoding"
            | "upgrade"
            | "x-forhappy-target-host"
            | "x-forhappy-target-port"
    )
}
