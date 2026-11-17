//! EdgePin: pins all 52dmeme.com subdomains to the single winning Cloudflare edge IP and IP family,
//! with fast failure tripwire (2 failures within 60 seconds -> unpin + re-race).

use parking_lot::RwLock;
use std::net::IpAddr;
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::Arc;
use std::time::{Duration, Instant};

pub const WINNER_FAIL_THRESHOLD: usize = 2;
pub const WINNER_FAIL_WINDOW: Duration = Duration::from_secs(60);

const COVERED_BASE: &str = "52dmeme.com";
const CHALLENGE_BASE: &str = "challenges.cloudflare.com";

pub fn is_covered_host(host: &str) -> bool {
    let h = host.trim_end_matches('.').to_ascii_lowercase();
    is_domain_or_sub(&h, COVERED_BASE) || is_domain_or_sub(&h, CHALLENGE_BASE)
}

pub fn is_challenge_host(host: &str) -> bool {
    let h = host.trim_end_matches('.').to_ascii_lowercase();
    is_domain_or_sub(&h, CHALLENGE_BASE)
}

#[inline]
fn is_domain_or_sub(host: &str, domain: &str) -> bool {
    host == domain
        || (host.len() > domain.len() + 1
            && host.ends_with(domain)
            && host.as_bytes()[host.len() - domain.len() - 1] == b'.')
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum PinStatus {
    None,
    Winner(IpAddr),
}

struct Inner {
    pin: PinStatus,
    fail_times: Vec<Instant>,
}

pub trait PinEventListener: Send + Sync {
    fn on_winner_demoted(&self, ip: IpAddr);
}

pub struct EdgePin {
    inner: RwLock<Inner>,
    epoch: AtomicU64,
    listener: RwLock<Option<Arc<dyn PinEventListener>>>,
}

impl EdgePin {
    pub fn new() -> Arc<Self> {
        Arc::new(Self {
            inner: RwLock::new(Inner {
                pin: PinStatus::None,
                fail_times: Vec::new(),
            }),
            epoch: AtomicU64::new(1),
            listener: RwLock::new(None),
        })
    }

    pub fn set_listener(&self, listener: Arc<dyn PinEventListener>) {
        *self.listener.write() = Some(listener);
    }

    pub fn epoch(&self) -> u64 {
        self.epoch.load(Ordering::Acquire)
    }

    pub fn current_winner(&self) -> Option<IpAddr> {
        match self.inner.read().pin {
            PinStatus::Winner(ip) => Some(ip),
            PinStatus::None => None,
        }
    }

    pub fn set_winner(&self, ip: IpAddr) {
        let mut g = self.inner.write();
        let changed = g.pin != PinStatus::Winner(ip);
        g.pin = PinStatus::Winner(ip);
        g.fail_times.clear();
        if changed {
            self.epoch.fetch_add(1, Ordering::Release);
        }
    }

    pub fn clear(&self) {
        let mut g = self.inner.write();
        if g.pin != PinStatus::None {
            g.pin = PinStatus::None;
            g.fail_times.clear();
            self.epoch.fetch_add(1, Ordering::Release);
        }
    }

    pub fn report_success(&self, ip: IpAddr) {
        let mut g = self.inner.write();
        if g.pin == PinStatus::Winner(ip) {
            g.fail_times.clear();
        }
    }

    pub fn report_failure(&self, ip: IpAddr) {
        let demoted = {
            let mut g = self.inner.write();
            if g.pin != PinStatus::Winner(ip) {
                false
            } else {
                let now = Instant::now();
                g.fail_times
                    .retain(|t| now.saturating_duration_since(*t) <= WINNER_FAIL_WINDOW);
                g.fail_times.push(now);
                if g.fail_times.len() >= WINNER_FAIL_THRESHOLD {
                    g.pin = PinStatus::None;
                    g.fail_times.clear();
                    self.epoch.fetch_add(1, Ordering::Release);
                    true
                } else {
                    false
                }
            }
        };
        if demoted {
            if let Some(listener) = self.listener.read().as_ref() {
                listener.on_winner_demoted(ip);
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::sync::atomic::AtomicBool;

    struct FlagListener(Arc<AtomicBool>);
    impl PinEventListener for FlagListener {
        fn on_winner_demoted(&self, _ip: IpAddr) {
            self.0.store(true, Ordering::SeqCst);
        }
    }

    #[test]
    fn covered_host_matching_and_tripwire() {
        assert!(is_covered_host("52dmeme.com"));
        assert!(is_covered_host("img.52dmeme.com"));
        assert!(is_covered_host("cc.52dmeme.com"));
        assert!(!is_covered_host("evil52dmeme.com"));

        let pin = EdgePin::new();
        let fired = Arc::new(AtomicBool::new(false));
        pin.set_listener(Arc::new(FlagListener(fired.clone())));
        let ip: IpAddr = "104.16.1.1".parse().unwrap();
        pin.set_winner(ip);
        pin.report_failure(ip);
        assert_eq!(pin.current_winner(), Some(ip));
        assert!(!fired.load(Ordering::SeqCst));
        pin.report_failure(ip);
        assert_eq!(pin.current_winner(), None);
        assert!(fired.load(Ordering::SeqCst));
    }
}
