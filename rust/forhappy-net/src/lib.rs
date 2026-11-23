//! ForHappy native network acceleration engine:
//! DoH + ECH + Cloudflare edge selector + EdgePin + H2 UpstreamPool + Dual-mode local gateway.

pub mod cfnet;
pub mod doh;
pub mod ech_tls;
pub mod edge_pin;
pub mod gateway;
pub mod jni_bridge;
pub mod pool;
pub mod selector;

use doh::ExternalDnsCache;
use edge_pin::EdgePin;
use gateway::GatewayHandle;
use parking_lot::Mutex;
use pool::UpstreamPool;
use selector::{EdgeSelector, Ipv6Mode};
use serde::Deserialize;
use std::sync::{Arc, OnceLock};
use tokio::runtime::Runtime;

static RUNTIME: OnceLock<Runtime> = OnceLock::new();
static ENGINE: Mutex<Option<EngineInstance>> = Mutex::new(None);

pub(crate) fn runtime() -> &'static Runtime {
    RUNTIME.get_or_init(|| {
        tokio::runtime::Builder::new_multi_thread()
            .enable_all()
            .thread_name("forhappy-net")
            .build()
            .expect("failed to start tokio runtime")
    })
}

#[derive(Debug, Deserialize)]
#[serde(untagged)]
pub enum CustomNodeEntry {
    Ip(String),
    Obj {
        ip: String,
        #[serde(default = "default_true")]
        enabled: bool,
    },
}

#[derive(Debug, Deserialize, Default)]
#[serde(rename_all = "camelCase")]
pub struct EngineConfig {
    #[serde(default)]
    pub select_state_json: Option<String>,
    #[serde(default)]
    pub custom_nodes: Vec<CustomNodeEntry>,
    #[serde(default)]
    pub ipv6_mode: Ipv6Mode,
    #[serde(default = "default_true")]
    pub ech_enabled: bool,
    #[serde(default)]
    pub network_sig: Option<String>,
}

fn default_true() -> bool {
    true
}

struct EngineInstance {
    port: u16,
    gateway: GatewayHandle,
    selector: Arc<EdgeSelector>,
    pool: Arc<UpstreamPool>,
}

pub fn start_engine(config_json: &str) -> Result<u16, String> {
    let config: EngineConfig = if config_json.trim().is_empty() {
        EngineConfig::default()
    } else {
        serde_json::from_str(config_json).map_err(|e| format!("invalid config json: {e}"))?
    };

    stop_engine();

    let rt = runtime();
    let edge_pin = EdgePin::new();
    let external_dns = Arc::new(ExternalDnsCache::new());
    let custom_ips: Vec<String> = config
        .custom_nodes
        .into_iter()
        .filter_map(|e| match e {
            CustomNodeEntry::Ip(s) => Some(s),
            CustomNodeEntry::Obj { ip, enabled } if enabled => Some(ip),
            _ => None,
        })
        .collect();

    let selector = EdgeSelector::new(
        edge_pin.clone(),
        config.select_state_json.as_deref(),
        custom_ips,
        config.ipv6_mode,
        config.ech_enabled,
        config.network_sig,
    );

    let (gateway, pool) = rt.block_on(async {
        let pool = UpstreamPool::new(edge_pin.clone(), selector.clone(), external_dns);
        let gateway = gateway::spawn_gateway(pool.clone())?;
        Ok::<_, String>((gateway, pool))
    })?;

    let port = gateway.port;
    rt.spawn({
        let sel = selector.clone();
        let pool = pool.clone();
        async move {
            let had_winner = edge_pin.current_winner().is_some();
            if had_winner {
                pool.preconnect(&["52dmeme.com"]);
            } else {
                sel.run_race(true).await;
                pool.preconnect(&["52dmeme.com"]);
            }
        }
    });

    *ENGINE.lock() = Some(EngineInstance {
        port,
        gateway,
        selector,
        pool,
    });
    Ok(port)
}

pub fn stop_engine() {
    if let Some(mut inst) = ENGINE.lock().take() {
        inst.gateway.stop();
        inst.pool.invalidate_all("engine stopped");
    }
}

pub fn trigger_reselect() {
    if let Some(inst) = ENGINE.lock().as_ref() {
        let sel = inst.selector.clone();
        let pool = inst.pool.clone();
        runtime().spawn(async move {
            sel.run_race(true).await;
            pool.invalidate_all("manual reselect");
            pool.preconnect(&["52dmeme.com"]);
        });
    }
}

pub fn on_network_changed(sig: &str) {
    if let Some(inst) = ENGINE.lock().as_ref() {
        inst.pool.invalidate_all("network changed");
        inst.selector.update_network_sig(sig.to_string());
    }
}

pub fn on_app_resumed() {
    if let Some(inst) = ENGINE.lock().as_ref() {
        let pool = inst.pool.clone();
        runtime().spawn(async move {
            pool.invalidate_all("app resumed from background");
            pool.preconnect(&["52dmeme.com"]);
        });
    }
}

pub fn status_json() -> String {
    let guard = ENGINE.lock();
    match guard.as_ref() {
        Some(inst) => {
            let mut v = inst.selector.status_snapshot();
            if let Some(obj) = v.as_object_mut() {
                obj.insert("running".to_string(), serde_json::json!(true));
                obj.insert("port".to_string(), serde_json::json!(inst.port));
            }
            v.to_string()
        }
        None => serde_json::json!({
            "running": false,
            "port": 0,
            "phase": "stopped"
        })
        .to_string(),
    }
}
