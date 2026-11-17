//! Local Cloudflare edge selector:
//! 1. Gathers CF candidates via public DoH on popular CF-hosted domains + custom nodes + CIDR sampling.
//! 2. Fetches CF ECH keys via public DoH HTTPS records (and saves retry_configs on rotation).
//! 3. Concurrent ECH/TLS probes to `/cdn-cgi/trace` checking `h=52dmeme.com`.
//! 4. Grace window + IPv6 Auto preference + cooldown/rate guards + persistent state.

use crate::cfnet;
use crate::doh;
use crate::ech_tls::{self, TlsConnectError};
use crate::edge_pin::{EdgePin, PinEventListener};
use base64::engine::general_purpose::STANDARD;
use base64::Engine;
use bytes::Bytes;
use http_body_util::{BodyExt, Empty};
use hyper_util::rt::TokioIo;
use parking_lot::Mutex;
use serde::{Deserialize, Serialize};
use std::net::IpAddr;
use std::sync::{Arc, Weak};
use std::time::{Duration, Instant, SystemTime, UNIX_EPOCH};
use tokio::sync::{Notify, Semaphore};
use tokio::task::JoinSet;

const PROBE_INNER_SNI: &str = "52dmeme.com";
const PROBE_CONCURRENCY: usize = 8;
const RACE_TOTAL_MS: u64 = 4500;
const GRACE_AFTER_FIRST_MS: u64 = 400;
const FAMILY_GRACE_MS: u64 = 1200;
const V6_TOLERANCE_MIN_MS: u64 = 300;
const ECH_KEY_MAX_AGE_SECS: u64 = 48 * 3600;
const MIN_AUTO_RACE_GAP_SECS: u64 = 30;

const VENDOR_DOMAINS: &[&str] = &[
    "www.visa.com.hk",
    "www.visa.com.sg",
    "www.shopify.com",
    "openai.com",
    "icook.tw",
    "japan.com",
    "cf.090227.xyz",
    "skk.moe",
];

const HTTPS_KEY_DOMAINS: &[&str] = &[
    "52dmeme.com",
    "cloudflare-ech.com",
    "icook.tw",
    "skk.moe",
    "japan.com",
];

fn now_unix() -> u64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| d.as_secs())
        .unwrap_or(0)
}

#[derive(Clone, Copy, Debug, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum Ipv6Mode {
    Auto,
    PreferV6,
    PreferV4,
}

impl Default for Ipv6Mode {
    fn default() -> Self {
        Self::Auto
    }
}

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct WinnerInfo {
    pub ip: IpAddr,
    pub latency_ms: u32,
    pub verified_at: u64,
    pub ech_accepted: bool,
    pub source: String,
}

#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct StoredEchKey {
    pub b64: String,
    pub seen_at: u64,
}

#[derive(Clone, Debug, Default, Serialize, Deserialize)]
pub struct SelectState {
    pub v: u8,
    pub winner: Option<WinnerInfo>,
    pub ech_keys: Vec<StoredEchKey>,
    pub network_sig: Option<String>,
}

impl SelectState {
    pub fn from_json(raw: &str) -> Option<Self> {
        let mut s: Self = serde_json::from_str(raw).ok()?;
        let now = now_unix();
        s.ech_keys.retain(|k| {
            now.saturating_sub(k.seen_at) < ECH_KEY_MAX_AGE_SECS
                && STANDARD
                    .decode(&k.b64)
                    .ok()
                    .is_some_and(|b| ech_tls::ech_key_usable(&b))
        });
        Some(s)
    }
}

#[derive(Clone, Debug)]
struct ProbeResult {
    ip: IpAddr,
    latency_ms: u32,
    ok: bool,
    ech_accepted: bool,
    source: String,
    new_ech_key: Option<Vec<u8>>,
}

struct InnerState {
    state: SelectState,
    phase: String,
    last_error: Option<String>,
    last_race_at: Option<Instant>,
    racing: bool,
    custom_nodes: Vec<String>,
    ipv6_mode: Ipv6Mode,
    ech_enabled: bool,
    logs: Vec<String>,
}

pub struct EdgeSelector {
    edge_pin: Arc<EdgePin>,
    inner: Mutex<InnerState>,
    done_notify: Notify,
}

impl EdgeSelector {
    pub fn new(
        edge_pin: Arc<EdgePin>,
        initial_state_json: Option<&str>,
        custom_nodes: Vec<String>,
        ipv6_mode: Ipv6Mode,
        ech_enabled: bool,
        network_sig: Option<String>,
    ) -> Arc<Self> {
        let mut state = initial_state_json
            .and_then(SelectState::from_json)
            .unwrap_or_else(|| SelectState {
                v: 1,
                ..Default::default()
            });
        state.v = 1;

        // Hydrate EdgePin immediately on cold start if same network sig or recent winner.
        if let Some(w) = &state.winner {
            let same_net = network_sig.is_none()
                || state.network_sig.is_none()
                || network_sig == state.network_sig;
            if same_net && now_unix().saturating_sub(w.verified_at) < 7 * 86400 {
                edge_pin.set_winner(w.ip);
            }
        }

        let sel = Arc::new(Self {
            edge_pin: edge_pin.clone(),
            inner: Mutex::new(InnerState {
                state,
                phase: if edge_pin.current_winner().is_some() {
                    "ready".to_string()
                } else {
                    "idle".to_string()
                },
                last_error: None,
                last_race_at: None,
                racing: false,
                custom_nodes,
                ipv6_mode,
                ech_enabled,
                logs: Vec::new(),
            }),
            done_notify: Notify::new(),
        });

        edge_pin.set_listener(Arc::new(SelectorPinListener(Arc::downgrade(&sel))));
        sel
    }

    pub fn push_log(&self, line: impl Into<String>) {
        let mut g = self.inner.lock();
        if g.logs.len() >= 80 {
            g.logs.remove(0);
        }
        g.logs.push(line.into());
    }

    pub fn latest_ech_key(&self) -> Option<Vec<u8>> {
        let g = self.inner.lock();
        if !g.ech_enabled {
            return None;
        }
        g.state
            .ech_keys
            .first()
            .and_then(|k| STANDARD.decode(&k.b64).ok())
            .filter(|b| ech_tls::ech_key_usable(b))
    }

    pub fn adopt_ech_key(&self, bytes: &[u8]) {
        if !ech_tls::ech_key_usable(bytes) {
            return;
        }
        let b64 = STANDARD.encode(bytes);
        let mut g = self.inner.lock();
        g.state.ech_keys.retain(|k| k.b64 != b64);
        g.state.ech_keys.insert(
            0,
            StoredEchKey {
                b64,
                seen_at: now_unix(),
            },
        );
        g.state.ech_keys.truncate(3);
    }

    pub fn update_network_sig(self: &Arc<Self>, sig: String) {
        let changed = {
            let mut g = self.inner.lock();
            if g.state.network_sig.as_deref() == Some(&sig) {
                false
            } else {
                g.state.network_sig = Some(sig);
                true
            }
        };
        if changed {
            self.push_log("select: network changed, scheduling re-race");
            self.spawn_race(true);
        }
    }

    pub fn spawn_race(self: &Arc<Self>, force: bool) {
        let sel = self.clone();
        crate::runtime().spawn(async move {
            sel.run_race(force).await;
        });
    }

    pub async fn wait_for_winner(&self, timeout: Duration) -> Option<IpAddr> {
        if let Some(ip) = self.edge_pin.current_winner() {
            return Some(ip);
        }
        let _ = tokio::time::timeout(timeout, self.done_notify.notified()).await;
        self.edge_pin.current_winner()
    }

    pub fn status_snapshot(&self) -> serde_json::Value {
        let g = self.inner.lock();
        let state_json = serde_json::to_string(&g.state).unwrap_or_default();
        serde_json::json!({
            "phase": g.phase,
            "winnerIp": self.edge_pin.current_winner().map(|ip| ip.to_string()),
            "latencyMs": g.state.winner.as_ref().map(|w| w.latency_ms),
            "echAccepted": g.state.winner.as_ref().map(|w| w.ech_accepted).unwrap_or(false),
            "echKeyCount": g.state.ech_keys.len(),
            "winnerSource": g.state.winner.as_ref().map(|w| w.source.clone()),
            "lastError": g.last_error,
            "selectStateJson": state_json,
            "logs": g.logs,
        })
    }

    pub async fn run_race(self: &Arc<Self>, force: bool) {
        {
            let mut g = self.inner.lock();
            if g.racing {
                return;
            }
            if !force {
                if let Some(last) = g.last_race_at {
                    if last.elapsed().as_secs() < MIN_AUTO_RACE_GAP_SECS {
                        return;
                    }
                }
            }
            g.racing = true;
            g.phase = "racing".to_string();
            g.last_race_at = Some(Instant::now());
        }

        let result = self.run_race_inner().await;
        {
            let mut g = self.inner.lock();
            g.racing = false;
            match result {
                Ok(w) => {
                    g.phase = "ready".to_string();
                    g.last_error = None;
                    g.state.winner = Some(w);
                }
                Err(err) => {
                    g.phase = if self.edge_pin.current_winner().is_some() {
                        "ready".to_string()
                    } else {
                        "failed".to_string()
                    };
                    g.last_error = Some(err);
                }
            }
        }
        self.done_notify.notify_waiters();
    }

    async fn run_race_inner(self: &Arc<Self>) -> Result<WinnerInfo, String> {
        let (ipv6_mode, ech_enabled, custom_nodes, prev_winner, persisted_key) = {
            let g = self.inner.lock();
            (
                g.ipv6_mode,
                g.ech_enabled,
                g.custom_nodes.clone(),
                g.state.winner.as_ref().map(|w| w.ip),
                g.state
                    .ech_keys
                    .first()
                    .and_then(|k| STANDARD.decode(&k.b64).ok()),
            )
        };

        let has_v6 = match ipv6_mode {
            Ipv6Mode::PreferV4 => false,
            Ipv6Mode::PreferV6 => true,
            Ipv6Mode::Auto => cfnet::has_local_ipv6_route(),
        };
        let want_v4 = true;

        // Step 1: Collect fresh ECH key + CF candidates via public DoH concurrently.
        let mut key_tasks = JoinSet::new();
        if ech_enabled {
            for &d in HTTPS_KEY_DOMAINS {
                key_tasks.spawn(async move { doh::fetch_ech_key(d).await });
            }
        }

        let mut cand_tasks = JoinSet::new();
        for &d in VENDOR_DOMAINS {
            cand_tasks.spawn(async move {
                let ips = doh::resolve_cf_domain(d, want_v4, has_v6).await;
                (d.to_string(), ips)
            });
        }
        for node in &custom_nodes {
            let n = node.trim().to_string();
            if n.is_empty() {
                continue;
            }
            if let Ok(ip) = n.parse::<IpAddr>() {
                if cfnet::is_cloudflare_ip(ip) {
                    cand_tasks.spawn(async move { ("custom".to_string(), vec![ip]) });
                }
            } else {
                let domain = n.clone();
                cand_tasks.spawn(async move {
                    let ips = doh::resolve_cf_domain(&domain, want_v4, has_v6).await;
                    ("custom".to_string(), ips)
                });
            }
        }

        let gather_deadline = Instant::now() + Duration::from_millis(2600);
        let mut ech_key = persisted_key.filter(|b| ech_tls::ech_key_usable(b));
        while ech_enabled && Instant::now() < gather_deadline {
            let rem = gather_deadline.saturating_duration_since(Instant::now());
            match tokio::time::timeout(rem.min(Duration::from_millis(800)), key_tasks.join_next())
                .await
            {
                Ok(Some(Ok(Some(fresh)))) => {
                    self.adopt_ech_key(&fresh);
                    ech_key = Some(fresh);
                    break;
                }
                Ok(None) => break,
                _ => continue,
            }
        }
        key_tasks.abort_all();

        let mut candidates: Vec<(IpAddr, String)> = Vec::new();
        let mut seen = std::collections::HashSet::new();

        if let Some(prev) = prev_winner {
            if seen.insert(prev) {
                candidates.push((prev, "sticky".to_string()));
            }
        }

        while Instant::now() < gather_deadline && candidates.len() < 18 {
            let rem = gather_deadline.saturating_duration_since(Instant::now());
            match tokio::time::timeout(rem, cand_tasks.join_next()).await {
                Ok(Some(Ok((src, ips)))) => {
                    for ip in ips.into_iter().take(3) {
                        if seen.insert(ip) {
                            let tag = if src == "custom" {
                                "custom".to_string()
                            } else {
                                "vendor_doh".to_string()
                            };
                            candidates.push((ip, tag));
                        }
                    }
                }
                _ => break,
            }
        }
        cand_tasks.abort_all();

        for ip in cfnet::sample_random_v4(10) {
            if seen.insert(ip) {
                candidates.push((ip, "cf_sample".to_string()));
            }
        }
        if has_v6 {
            for ip in cfnet::sample_random_v6(4) {
                if seen.insert(ip) {
                    candidates.push((ip, "cf_sample_v6".to_string()));
                }
            }
        }
        candidates.truncate(28);
        self.push_log(format!(
            "select: probing {} candidates (has_v6={}, ech_key={})",
            candidates.len(),
            has_v6,
            ech_key.is_some()
        ));

        // Step 2: Concurrent trace probes
        let sem = Arc::new(Semaphore::new(PROBE_CONCURRENCY));
        let mut probe_set = JoinSet::new();
        for (ip, source) in candidates {
            let perm_sem = sem.clone();
            let key = if ech_enabled { ech_key.clone() } else { None };
            probe_set.spawn(async move {
                let _permit = perm_sem.acquire().await.ok();
                probe_single(ip, source, key).await
            });
        }

        let mut passed: Vec<ProbeResult> = Vec::new();
        let race_deadline = Instant::now() + Duration::from_millis(RACE_TOTAL_MS);
        let mut converge_at: Option<Instant> = None;

        loop {
            let target_deadline = converge_at
                .map(|c| c.min(race_deadline))
                .unwrap_or(race_deadline);
            let rem = target_deadline.saturating_duration_since(Instant::now());
            if rem.is_zero() {
                break;
            }
            match tokio::time::timeout(rem, probe_set.join_next()).await {
                Ok(Some(Ok(res))) => {
                    if let Some(new_key) = &res.new_ech_key {
                        self.adopt_ech_key(new_key);
                    }
                    if res.ok {
                        let first_pass = passed.is_empty();
                        passed.push(res);
                        if first_pass {
                            let has_v6_pass = passed.iter().any(|p| p.ip.is_ipv6());
                            let grace = if has_v6 && !has_v6_pass && ipv6_mode != Ipv6Mode::PreferV4
                            {
                                Duration::from_millis(FAMILY_GRACE_MS)
                            } else {
                                Duration::from_millis(GRACE_AFTER_FIRST_MS)
                            };
                            converge_at = Some(Instant::now() + grace);
                        } else if passed.len() >= 4 {
                            break;
                        }
                    }
                }
                Ok(Some(Err(_))) => continue,
                Ok(None) => break,
                Err(_) => break,
            }
        }
        probe_set.abort_all();

        if passed.is_empty() {
            self.push_log("select: all candidates failed in current round");
            return Err("all Cloudflare candidates failed".to_string());
        }

        // Custom nodes take priority if any passed.
        let pool = if passed.iter().any(|p| p.source == "custom") {
            passed
                .into_iter()
                .filter(|p| p.source == "custom")
                .collect::<Vec<_>>()
        } else {
            passed
        };

        let best = pick_best(&pool, ipv6_mode);
        self.edge_pin.set_winner(best.ip);
        self.push_log(format!(
            "select: winner {} ({} ms, ech={}, src={})",
            best.ip, best.latency_ms, best.ech_accepted, best.source
        ));

        Ok(WinnerInfo {
            ip: best.ip,
            latency_ms: best.latency_ms,
            verified_at: now_unix(),
            ech_accepted: best.ech_accepted,
            source: best.source.clone(),
        })
    }
}

fn pick_best(pool: &[ProbeResult], mode: Ipv6Mode) -> &ProbeResult {
    let best_v4 = pool
        .iter()
        .filter(|p| p.ip.is_ipv4())
        .min_by_key(|p| p.latency_ms);
    let best_v6 = pool
        .iter()
        .filter(|p| p.ip.is_ipv6())
        .min_by_key(|p| p.latency_ms);

    match (mode, best_v6, best_v4) {
        (Ipv6Mode::PreferV6, Some(v6), _) => v6,
        (Ipv6Mode::PreferV4, _, Some(v4)) => v4,
        (Ipv6Mode::Auto, Some(v6), Some(v4)) => {
            if v6.latency_ms <= v4.latency_ms.saturating_mul(2)
                && v6.latency_ms <= v4.latency_ms.saturating_add(V6_TOLERANCE_MIN_MS as u32)
            {
                v6
            } else {
                v4
            }
        }
        (_, Some(v6), None) => v6,
        (_, None, Some(v4)) => v4,
        _ => &pool[0],
    }
}

async fn probe_single(ip: IpAddr, source: String, ech_key: Option<Vec<u8>>) -> ProbeResult {
    let start = Instant::now();
    let tcp_to = Duration::from_millis(1800);
    let tls_to = Duration::from_millis(2500);
    let http_to = Duration::from_millis(2000);

    let mut active_key = ech_key;
    let mut rotated_key: Option<Vec<u8>> = None;

    for attempt in 0..2 {
        let Ok(tls_cfg) =
            ech_tls::build_tls_config(active_key.as_deref(), vec![b"http/1.1".to_vec()])
        else {
            break;
        };
        match ech_tls::connect_tls(ip, 443, PROBE_INNER_SNI, tls_cfg, tcp_to, tls_to).await {
            Ok((stream, ech_accepted)) => {
                let (mut sender, conn) =
                    match hyper::client::conn::http1::handshake(TokioIo::new(stream)).await {
                        Ok(v) => v,
                        Err(_) => {
                            return ProbeResult {
                                ip,
                                latency_ms: start.elapsed().as_millis() as u32,
                                ok: false,
                                ech_accepted,
                                source,
                                new_ech_key: rotated_key,
                            }
                        }
                    };
                tokio::spawn(async move {
                    let _ = conn.await;
                });
                let req = http::Request::builder()
                    .method(http::Method::GET)
                    .uri("/cdn-cgi/trace")
                    .header(http::header::HOST, PROBE_INNER_SNI)
                    .header(http::header::USER_AGENT, "okhttp/4.12.0")
                    .header(http::header::CONNECTION, "close")
                    .body(Empty::<Bytes>::new())
                    .unwrap();
                let resp = match tokio::time::timeout(http_to, sender.send_request(req)).await {
                    Ok(Ok(r)) => r,
                    _ => {
                        return ProbeResult {
                            ip,
                            latency_ms: start.elapsed().as_millis() as u32,
                            ok: false,
                            ech_accepted,
                            source,
                            new_ech_key: rotated_key,
                        }
                    }
                };
                if resp.status().as_u16() != 200 {
                    return ProbeResult {
                        ip,
                        latency_ms: start.elapsed().as_millis() as u32,
                        ok: false,
                        ech_accepted,
                        source,
                        new_ech_key: rotated_key,
                    };
                }
                let body = match tokio::time::timeout(http_to, resp.into_body().collect()).await {
                    Ok(Ok(b)) => b.to_bytes(),
                    _ => Bytes::new(),
                };
                let ok = String::from_utf8_lossy(&body)
                    .lines()
                    .any(|line| line.trim_end().strip_prefix("h=") == Some(PROBE_INNER_SNI));
                return ProbeResult {
                    ip,
                    latency_ms: start.elapsed().as_millis() as u32,
                    ok,
                    ech_accepted,
                    source,
                    new_ech_key: rotated_key,
                };
            }
            Err(TlsConnectError::EchRejected {
                retry_configs: Some(fresh),
            }) if attempt == 0 => {
                rotated_key = Some(fresh.clone());
                active_key = Some(fresh);
                continue;
            }
            Err(TlsConnectError::EchRejected { .. }) | Err(TlsConnectError::Tls(_))
                if attempt == 0 && active_key.is_some() =>
            {
                // Fallback to standard TLS on second attempt if ECH rejected without retry_configs
                active_key = None;
                continue;
            }
            Err(_) => break,
        }
    }

    ProbeResult {
        ip,
        latency_ms: start.elapsed().as_millis() as u32,
        ok: false,
        ech_accepted: false,
        source,
        new_ech_key: rotated_key,
    }
}

struct SelectorPinListener(Weak<EdgeSelector>);

impl PinEventListener for SelectorPinListener {
    fn on_winner_demoted(&self, ip: IpAddr) {
        if let Some(sel) = self.0.upgrade() {
            sel.push_log(format!("select: winner {ip} demoted after consecutive failures"));
            sel.spawn_race(false);
        }
    }
}
