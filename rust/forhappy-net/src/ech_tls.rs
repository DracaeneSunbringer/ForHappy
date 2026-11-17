//! ECH (Encrypted Client Hello) + rustls 0.23 TLS connector using aws-lc-rs.

use rustls::client::{EchConfig, EchMode, EchStatus};
use rustls::crypto::CryptoProvider;
use rustls::internal::msgs::codec::{Codec, Reader};
use rustls::internal::msgs::handshake::EchConfigPayload;
use rustls::pki_types::{EchConfigListBytes, ServerName};
use rustls::{ClientConfig, RootCertStore};
use std::io;
use std::net::{IpAddr, SocketAddr};
use std::sync::{Arc, OnceLock};
use std::time::Duration;
use tokio::net::TcpStream;
use tokio_rustls::client::TlsStream;
use tokio_rustls::TlsConnector;

static ROOT_STORE: OnceLock<RootCertStore> = OnceLock::new();
static CRYPTO_PROVIDER: OnceLock<Arc<CryptoProvider>> = OnceLock::new();

pub fn root_store() -> &'static RootCertStore {
    ROOT_STORE.get_or_init(|| {
        let mut store = RootCertStore::empty();
        store.extend(webpki_roots::TLS_SERVER_ROOTS.iter().cloned());
        store
    })
}

pub fn crypto_provider() -> Arc<CryptoProvider> {
    CRYPTO_PROVIDER
        .get_or_init(|| Arc::new(rustls::crypto::aws_lc_rs::default_provider()))
        .clone()
}

pub fn ech_key_usable(key: &[u8]) -> bool {
    if key.len() < 4 {
        return false;
    }
    let mut reader = Reader::init(key);
    match Vec::<EchConfigPayload>::read(&mut reader) {
        Ok(configs) if !reader.any_left() => configs
            .iter()
            .any(|config| matches!(config, EchConfigPayload::V18(_))),
        _ => false,
    }
}

pub fn ensure_ech_len_prefix(bytes: Vec<u8>) -> Vec<u8> {
    if bytes.len() >= 2 {
        let declared = u16::from_be_bytes([bytes[0], bytes[1]]) as usize;
        if declared == bytes.len().saturating_sub(2) {
            return bytes;
        }
    }
    if bytes.len() > u16::MAX as usize {
        return bytes;
    }
    let mut out = Vec::with_capacity(bytes.len() + 2);
    out.extend_from_slice(&(bytes.len() as u16).to_be_bytes());
    out.extend_from_slice(&bytes);
    out
}

pub fn encode_ech_config_list(configs: &[EchConfigPayload]) -> Vec<u8> {
    let mut out = Vec::new();
    configs.to_vec().encode(&mut out);
    out
}

pub fn ech_rejection_retry_configs(err: &io::Error) -> Option<Option<Vec<u8>>> {
    let inner = err.get_ref()?;
    let rustls_err = inner.downcast_ref::<rustls::Error>()?;
    match rustls_err {
        rustls::Error::PeerIncompatible(
            rustls::PeerIncompatible::ServerRejectedEncryptedClientHello(configs),
        ) => {
            let encoded = configs
                .as_deref()
                .filter(|c| !c.is_empty())
                .map(encode_ech_config_list)
                .filter(|b| ech_key_usable(b));
            Some(encoded)
        }
        _ => None,
    }
}

pub fn build_tls_config(
    ech_bytes: Option<&[u8]>,
    alpn_protocols: Vec<Vec<u8>>,
) -> Result<Arc<ClientConfig>, String> {
    let provider = crypto_provider();
    let roots = root_store().clone();
    let mut config = match ech_bytes {
        Some(raw) if ech_key_usable(raw) => {
            let ech_list = EchConfigListBytes::from(raw.to_vec());
            let ech_config = EchConfig::new(
                ech_list,
                rustls::crypto::aws_lc_rs::hpke::ALL_SUPPORTED_SUITES,
            )
            .map_err(|e| format!("Invalid ECH config: {e}"))?;
            ClientConfig::builder_with_provider(provider)
                .with_ech(EchMode::Enable(ech_config))
                .map_err(|e| format!("Failed to enable ECH: {e}"))?
                .with_root_certificates(roots)
                .with_no_client_auth()
        }
        _ => ClientConfig::builder_with_provider(provider)
            .with_safe_default_protocol_versions()
            .map_err(|e| format!("TLS version error: {e}"))?
            .with_root_certificates(roots)
            .with_no_client_auth(),
    };
    config.alpn_protocols = alpn_protocols;
    Ok(Arc::new(config))
}

#[derive(Debug)]
pub enum TlsConnectError {
    TcpConnect(String),
    TcpTimeout,
    TlsTimeout,
    EchRejected { retry_configs: Option<Vec<u8>> },
    Tls(String),
}

impl std::fmt::Display for TlsConnectError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::TcpConnect(s) => write!(f, "tcp connect failed: {s}"),
            Self::TcpTimeout => write!(f, "tcp timeout"),
            Self::TlsTimeout => write!(f, "tls timeout"),
            Self::EchRejected { retry_configs } => write!(
                f,
                "ech rejected ({} bytes)",
                retry_configs.as_ref().map(|b| b.len()).unwrap_or(0)
            ),
            Self::Tls(s) => write!(f, "tls failed: {s}"),
        }
    }
}

pub async fn connect_tls(
    ip: IpAddr,
    port: u16,
    server_name: &str,
    tls: Arc<ClientConfig>,
    tcp_timeout: Duration,
    tls_timeout: Duration,
) -> Result<(TlsStream<TcpStream>, bool), TlsConnectError> {
    let addr = SocketAddr::new(ip, port);
    let tcp = match tokio::time::timeout(tcp_timeout, TcpStream::connect(addr)).await {
        Ok(Ok(s)) => s,
        Ok(Err(e)) => return Err(TlsConnectError::TcpConnect(e.to_string())),
        Err(_) => return Err(TlsConnectError::TcpTimeout),
    };
    let _ = tcp.set_nodelay(true);

    let sni = ServerName::try_from(server_name.to_string())
        .map_err(|e| TlsConnectError::Tls(format!("bad server name: {e}")))?;
    let connector = TlsConnector::from(tls);
    match tokio::time::timeout(tls_timeout, connector.connect(sni, tcp)).await {
        Ok(Ok(stream)) => {
            let accepted = matches!(stream.get_ref().1.ech_status(), EchStatus::Accepted);
            Ok((stream, accepted))
        }
        Ok(Err(e)) => {
            if let Some(retry_configs) = ech_rejection_retry_configs(&e) {
                Err(TlsConnectError::EchRejected { retry_configs })
            } else {
                Err(TlsConnectError::Tls(e.to_string()))
            }
        }
        Err(_) => Err(TlsConnectError::TlsTimeout),
    }
}
