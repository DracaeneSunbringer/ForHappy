//! JNI exports bound to `com.jinhuan.forhappy.core.network.rust.RustNetBridge`.

use jni::objects::{JClass, JString};
use jni::sys::jstring;
use jni::JNIEnv;

fn to_jstring(env: &mut JNIEnv, s: &str) -> jstring {
    env.new_string(s)
        .map(|js| js.into_raw())
        .unwrap_or(std::ptr::null_mut())
}

#[no_mangle]
pub extern "system" fn Java_com_jinhuan_forhappy_core_network_rust_RustNetBridge_nativeStart(
    mut env: JNIEnv,
    _cls: JClass,
    config_json: JString,
) -> jstring {
    let cfg: String = env
        .get_string(&config_json)
        .map(|s| s.into())
        .unwrap_or_default();
    let reply = match crate::start_engine(&cfg) {
        Ok(port) => serde_json::json!({ "ok": true, "port": port }).to_string(),
        Err(err) => serde_json::json!({ "ok": false, "error": err }).to_string(),
    };
    to_jstring(&mut env, &reply)
}

#[no_mangle]
pub extern "system" fn Java_com_jinhuan_forhappy_core_network_rust_RustNetBridge_nativeStop(
    _env: JNIEnv,
    _cls: JClass,
) {
    crate::stop_engine();
}

#[no_mangle]
pub extern "system" fn Java_com_jinhuan_forhappy_core_network_rust_RustNetBridge_nativeReselect(
    _env: JNIEnv,
    _cls: JClass,
) {
    crate::trigger_reselect();
}

#[no_mangle]
pub extern "system" fn Java_com_jinhuan_forhappy_core_network_rust_RustNetBridge_nativeOnNetworkChanged(
    mut env: JNIEnv,
    _cls: JClass,
    network_sig: JString,
) {
    let sig: String = env
        .get_string(&network_sig)
        .map(|s| s.into())
        .unwrap_or_default();
    crate::on_network_changed(&sig);
}

#[no_mangle]
pub extern "system" fn Java_com_jinhuan_forhappy_core_network_rust_RustNetBridge_nativeOnAppResumed(
    _env: JNIEnv,
    _cls: JClass,
) {
    crate::on_app_resumed();
}

#[no_mangle]
pub extern "system" fn Java_com_jinhuan_forhappy_core_network_rust_RustNetBridge_nativeGetStatusJson(
    mut env: JNIEnv,
    _cls: JClass,
) -> jstring {
    let status = crate::status_json();
    to_jstring(&mut env, &status)
}
