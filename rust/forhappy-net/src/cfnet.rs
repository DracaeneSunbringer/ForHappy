//! Cloudflare official IPv4/IPv6 CIDR validation and deterministic sampling.

use std::net::{IpAddr, Ipv4Addr, Ipv6Addr, SocketAddr, UdpSocket};
use std::time::{SystemTime, UNIX_EPOCH};

struct V4Net {
    net: u32,
    mask: u32,
}

impl V4Net {
    const fn new(a: u8, b: u8, c: u8, d: u8, prefix: u8) -> Self {
        let ip = u32::from_be_bytes([a, b, c, d]);
        let mask = if prefix == 0 {
            0
        } else {
            !0u32 << (32 - prefix)
        };
        Self {
            net: ip & mask,
            mask,
        }
    }

    #[inline]
    fn contains(&self, ip: Ipv4Addr) -> bool {
        (u32::from(ip) & self.mask) == self.net
    }

    fn host_count(&self) -> u32 {
        (!self.mask).saturating_sub(1)
    }

    fn ith_host(&self, idx: u32) -> Ipv4Addr {
        let range = !self.mask;
        let offset = if range <= 2 {
            1
        } else {
            1 + (idx % (range - 2))
        };
        Ipv4Addr::from(self.net | offset)
    }
}

struct V6Net {
    net: u128,
    mask: u128,
}

impl V6Net {
    const fn new(segs: [u16; 8], prefix: u8) -> Self {
        let ip = ((segs[0] as u128) << 112)
            | ((segs[1] as u128) << 96)
            | ((segs[2] as u128) << 80)
            | ((segs[3] as u128) << 64)
            | ((segs[4] as u128) << 48)
            | ((segs[5] as u128) << 32)
            | ((segs[6] as u128) << 16)
            | (segs[7] as u128);
        let mask = if prefix == 0 {
            0
        } else {
            !0u128 << (128 - prefix)
        };
        Self {
            net: ip & mask,
            mask,
        }
    }

    #[inline]
    fn contains(&self, ip: Ipv6Addr) -> bool {
        (u128::from(ip) & self.mask) == self.net
    }
}

/// Official Cloudflare IPv4 CIDRs (https://www.cloudflare.com/ips-v4)
const CF_V4: &[V4Net] = &[
    V4Net::new(173, 245, 48, 0, 20),
    V4Net::new(103, 21, 244, 0, 22),
    V4Net::new(103, 22, 200, 0, 22),
    V4Net::new(103, 31, 4, 0, 22),
    V4Net::new(141, 101, 64, 0, 18),
    V4Net::new(108, 162, 192, 0, 18),
    V4Net::new(190, 93, 240, 0, 20),
    V4Net::new(188, 114, 96, 0, 20),
    V4Net::new(197, 234, 240, 0, 22),
    V4Net::new(198, 41, 128, 0, 17),
    V4Net::new(162, 158, 0, 0, 15),
    V4Net::new(104, 16, 0, 0, 13),
    V4Net::new(104, 24, 0, 0, 14),
    V4Net::new(172, 64, 0, 0, 13),
    V4Net::new(131, 0, 72, 0, 22),
];

/// Official Cloudflare IPv6 CIDRs (https://www.cloudflare.com/ips-v6)
const CF_V6: &[V6Net] = &[
    V6Net::new([0x2400, 0xcb00, 0, 0, 0, 0, 0, 0], 32),
    V6Net::new([0x2606, 0x4700, 0, 0, 0, 0, 0, 0], 32),
    V6Net::new([0x2803, 0xf800, 0, 0, 0, 0, 0, 0], 32),
    V6Net::new([0x2405, 0xb500, 0, 0, 0, 0, 0, 0], 32),
    V6Net::new([0x2405, 0x8100, 0, 0, 0, 0, 0, 0], 32),
    V6Net::new([0x2a06, 0x98c0, 0, 0, 0, 0, 0, 0], 29),
    V6Net::new([0x2c0f, 0xf248, 0, 0, 0, 0, 0, 0], 32),
];

/// Subnets used by Cloudflare WARP/DNS/owned infra rather than shared Anycast CDN edge.
const SKIP_V4: &[V4Net] = &[
    V4Net::new(104, 28, 0, 0, 14),
    V4Net::new(172, 70, 0, 0, 15),
    V4Net::new(162, 159, 0, 0, 16),
];

pub fn is_cloudflare_ip(ip: IpAddr) -> bool {
    match ip {
        IpAddr::V4(v4) => CF_V4.iter().any(|net| net.contains(v4)),
        IpAddr::V6(v6) => CF_V6.iter().any(|net| net.contains(v6)),
    }
}

pub fn all_cloudflare(ips: &[IpAddr]) -> bool {
    !ips.is_empty() && ips.iter().all(|ip| is_cloudflare_ip(*ip))
}

pub fn is_skipped(ip: IpAddr) -> bool {
    match ip {
        IpAddr::V4(v4) => SKIP_V4.iter().any(|net| net.contains(v4)),
        IpAddr::V6(_) => false,
    }
}

struct XorShift64(u64);

impl XorShift64 {
    fn from_clock() -> Self {
        let nanos = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .map(|d| d.as_nanos() as u64)
            .unwrap_or(0x9e37_79b9_7f4a_7c15);
        Self(nanos ^ 0xa076_1d64_78bd_642f | 1)
    }

    fn next(&mut self) -> u64 {
        let mut x = self.0;
        x ^= x << 13;
        x ^= x >> 7;
        x ^= x << 17;
        self.0 = x;
        x
    }
}

/// Prefer standard shared Anycast CDN prefixes for random sampling.
const SAMPLE_V4_POOL: &[V4Net] = &[
    V4Net::new(104, 16, 0, 0, 13),
    V4Net::new(104, 24, 0, 0, 15),
    V4Net::new(172, 64, 0, 0, 14),
    V4Net::new(162, 158, 0, 0, 16),
    V4Net::new(188, 114, 96, 0, 20),
    V4Net::new(141, 101, 64, 0, 18),
    V4Net::new(108, 162, 192, 0, 18),
];

pub fn sample_random_v4(count: usize) -> Vec<IpAddr> {
    let mut rng = XorShift64::from_clock();
    let mut out = Vec::with_capacity(count);
    let mut attempts = 0;
    while out.len() < count && attempts < count * 8 {
        attempts += 1;
        let net = &SAMPLE_V4_POOL[(rng.next() as usize) % SAMPLE_V4_POOL.len()];
        let idx = (rng.next() as u32) % net.host_count().max(1);
        let ip = IpAddr::V4(net.ith_host(idx));
        if !is_skipped(ip) && !out.contains(&ip) {
            out.push(ip);
        }
    }
    out
}

pub fn sample_random_v6(count: usize) -> Vec<IpAddr> {
    let mut rng = XorShift64::from_clock();
    let mut out = Vec::with_capacity(count);
    let prefixes: &[[u16; 3]] = &[
        [0x2606, 0x4700, 0x3030],
        [0x2606, 0x4700, 0x3031],
        [0x2606, 0x4700, 0x3032],
        [0x2606, 0x4700, 0x3033],
        [0x2606, 0x4700, 0x3034],
        [0x2606, 0x4700, 0x3035],
        [0x2606, 0x4700, 0x3036],
        [0x2606, 0x4700, 0x3037],
        [0x2606, 0x4700, 0x3038],
        [0x2803, 0xf800, 0x0050],
        [0x2400, 0xcb00, 0x2048],
    ];
    for _ in 0..(count * 4) {
        if out.len() >= count {
            break;
        }
        let p = prefixes[(rng.next() as usize) % prefixes.len()];
        let r = rng.next();
        let addr = Ipv6Addr::new(
            p[0],
            p[1],
            p[2],
            (r & 0xffff) as u16,
            ((r >> 16) & 0xffff) as u16,
            ((r >> 32) & 0xffff) as u16,
            ((r >> 48) & 0xffff) as u16,
            ((rng.next() & 0xfffe) as u16) | 1,
        );
        let ip = IpAddr::V6(addr);
        if is_cloudflare_ip(ip) && !out.contains(&ip) {
            out.push(ip);
        }
    }
    out
}

/// Checks OS routing table for IPv6 reachability without sending network traffic.
pub fn has_local_ipv6_route() -> bool {
    let Ok(sock) = UdpSocket::bind("[::]:0") else {
        return false;
    };
    let target = SocketAddr::new(IpAddr::V6(Ipv6Addr::new(0x2606, 0x4700, 0x4700, 0, 0, 0, 0, 0x1111)), 443);
    sock.connect(target).is_ok()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn cloudflare_cidr_filter_works() {
        assert!(is_cloudflare_ip("104.16.132.229".parse().unwrap()));
        assert!(is_cloudflare_ip("172.67.180.10".parse().unwrap()));
        assert!(is_cloudflare_ip("2606:4700:3031::ac43:1234".parse().unwrap()));
        assert!(!is_cloudflare_ip("127.0.0.1".parse().unwrap()));
        assert!(!is_cloudflare_ip("31.13.66.35".parse().unwrap())); // classic poisoned Facebook IP
        assert!(!is_cloudflare_ip("8.8.8.8".parse().unwrap()));
    }

    #[test]
    fn sample_ips_all_in_cf_ranges() {
        let v4 = sample_random_v4(8);
        assert_eq!(v4.len(), 8);
        assert!(all_cloudflare(&v4));
        let v6 = sample_random_v6(4);
        assert_eq!(v6.len(), 4);
        assert!(all_cloudflare(&v6));
    }
}
