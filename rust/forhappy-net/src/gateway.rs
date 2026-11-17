//! Local dual-mode gateway listening on 127.0.0.1:0:
//! - Gateway HTTP mode (from OkHttp): plain loopback HTTP forwarded via UpstreamPool (TLS+ECH+H2).
//! - HTTP CONNECT tunnel mode (from Android WebView ProxyController): routes covered hosts
//!   through EdgePin winner IP and external hosts via anti-poisoning DoH.

use crate::edge_pin;
use crate::pool::{PoolBody, UpstreamPool};
use bytes::Bytes;
use http_body_util::{BodyExt, Empty, Full};
use hyper::body::Incoming;
use hyper::server::conn::http1;
use hyper::service::service_fn;
use hyper::{Method, Request, Response, StatusCode};
use hyper_util::rt::TokioIo;
use std::net::{SocketAddr, TcpListener as StdTcpListener};
use std::sync::Arc;
use std::time::Duration;
use tokio::net::{TcpListener, TcpStream};
use tokio::sync::oneshot;

pub struct GatewayHandle {
    pub port: u16,
    shutdown_tx: Option<oneshot::Sender<()>>,
}

impl GatewayHandle {
    pub fn stop(&mut self) {
        if let Some(tx) = self.shutdown_tx.take() {
            let _ = tx.send(());
        }
    }
}

pub fn spawn_gateway(pool: Arc<UpstreamPool>) -> Result<GatewayHandle, String> {
    let std_listener =
        StdTcpListener::bind("127.0.0.1:0").map_err(|e| format!("bind loopback failed: {e}"))?;
    std_listener
        .set_nonblocking(true)
        .map_err(|e| format!("set_nonblocking failed: {e}"))?;
    let port = std_listener
        .local_addr()
        .map_err(|e| format!("local_addr failed: {e}"))?
        .port();
    let listener =
        TcpListener::from_std(std_listener).map_err(|e| format!("tokio listener failed: {e}"))?;

    let (tx, mut rx) = oneshot::channel();
    tokio::spawn(async move {
        loop {
            tokio::select! {
                _ = &mut rx => break,
                accepted = listener.accept() => {
                    let Ok((stream, _)) = accepted else { break; };
                    let _ = stream.set_nodelay(true);
                    let pool = pool.clone();
                    tokio::spawn(async move {
                        let io = TokioIo::new(stream);
                        let svc = service_fn(move |req| {
                            let pool = pool.clone();
                            async move { handle_request(pool, req).await }
                        });
                        let _ = http1::Builder::new()
                            .preserve_header_case(true)
                            .serve_connection(io, svc)
                            .with_upgrades()
                            .await;
                    });
                }
            }
        }
    });

    Ok(GatewayHandle {
        port,
        shutdown_tx: Some(tx),
    })
}

async fn handle_request(
    pool: Arc<UpstreamPool>,
    req: Request<Incoming>,
) -> Result<Response<PoolBody>, hyper::Error> {
    if req.method() == Method::CONNECT {
        return Ok(handle_connect(pool, req));
    }

    let target_host = req
        .headers()
        .get("x-forhappy-target-host")
        .and_then(|v| v.to_str().ok())
        .map(|s| s.trim().to_string())
        .or_else(|| {
            req.headers()
                .get(http::header::HOST)
                .and_then(|v| v.to_str().ok())
                .map(|s| s.split(':').next().unwrap_or(s).trim().to_string())
        })
        .unwrap_or_else(|| "52dmeme.com".to_string());

    let target_port = req
        .headers()
        .get("x-forhappy-target-port")
        .and_then(|v| v.to_str().ok())
        .and_then(|s| s.trim().parse::<u16>().ok())
        .unwrap_or(443);

    let (parts, incoming) = req.into_parts();
    let pool_req = Request::from_parts(parts, incoming.boxed());

    match pool.forward_http(&target_host, target_port, pool_req).await {
        Ok(resp) => {
            let (rparts, rbody) = resp.into_parts();
            Ok(Response::from_parts(rparts, rbody.boxed()))
        }
        Err(err) => {
            let body = Full::new(Bytes::from(err))
                .map_err(|never| match never {})
                .boxed();
            Ok(Response::builder()
                .status(StatusCode::BAD_GATEWAY)
                .header(http::header::CONTENT_TYPE, "text/plain; charset=utf-8")
                .body(body)
                .unwrap())
        }
    }
}

fn handle_connect(pool: Arc<UpstreamPool>, req: Request<Incoming>) -> Response<PoolBody> {
    let authority = req
        .uri()
        .authority()
        .map(|a| a.to_string())
        .unwrap_or_default();
    let (host, port) = parse_host_port(&authority);

    tokio::spawn(async move {
        let Ok(upgraded) = hyper::upgrade::on(req).await else {
            return;
        };
        let ips = pool.resolve_connect_ips(&host).await;
        if ips.is_empty() {
            return;
        }
        let covered = edge_pin::is_covered_host(&host);
        let mut upstream = None;
        for ip in ips {
            let addr = SocketAddr::new(ip, port);
            if let Ok(Ok(stream)) =
                tokio::time::timeout(Duration::from_secs(4), TcpStream::connect(addr)).await
            {
                let _ = stream.set_nodelay(true);
                upstream = Some(stream);
                break;
            } else if covered {
                // Only count failures for direct covered-host tunnels if TCP failed.
            }
        }
        let Some(mut upstream_tcp) = upstream else {
            return;
        };
        let mut client_io = TokioIo::new(upgraded);
        let _ = tokio::io::copy_bidirectional(&mut client_io, &mut upstream_tcp).await;
    });

    Response::builder()
        .status(StatusCode::OK)
        .body(Empty::<Bytes>::new().map_err(|n| match n {}).boxed())
        .unwrap()
}

fn parse_host_port(authority: &str) -> (String, u16) {
    if let Some(rest) = authority.strip_prefix('[') {
        if let Some((host, tail)) = rest.split_once(']') {
            let port = tail
                .strip_prefix(':')
                .and_then(|p| p.parse::<u16>().ok())
                .unwrap_or(443);
            return (host.to_string(), port);
        }
    }
    if let Some((host, port_str)) = authority.rsplit_once(':') {
        if let Ok(port) = port_str.parse::<u16>() {
            return (host.to_string(), port);
        }
    }
    (authority.to_string(), 443)
}
