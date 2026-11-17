package com.jinhuan.forhappy.core.network.rust

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import com.jinhuan.forhappy.core.db.ForHappyDatabase
import com.jinhuan.forhappy.core.model.CustomEdgeNode
import com.jinhuan.forhappy.core.model.ForHappyJson
import com.jinhuan.forhappy.core.model.RustNetConfig
import com.jinhuan.forhappy.core.model.RustNetStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.util.concurrent.Executors

/**
 * Lifecycle manager around [RustNetBridge]:
 * - Loads/saves persisted `select_state` JSON & node/IPv6/ECH preferences in SQLite.
 * - Starts local Rust loopback gateway and binds Android WebView ProxyController (`CONNECT` tunnel).
 * - Watches ConnectivityManager network switches & app foreground resume.
 */
class RustNetManager(
    private val context: Context,
    private val db: ForHappyDatabase,
) {
    companion object {
        private const val KV_SELECT_STATE = "rust.select_state"
        private const val KV_CUSTOM_NODES = "rust.custom_nodes"
        private const val KV_IPV6_MODE = "rust.ipv6_mode"
        private const val KV_ECH_ENABLED = "rust.ech_enabled"

        val COVERED_HOSTS = setOf(
            "52dmeme.com",
            "cc.52dmeme.com",
            "img.52dmeme.com",
            "challenges.cloudflare.com",
        )

        fun isCoveredHost(host: String): Boolean =
            host.lowercase().trimEnd('.') in COVERED_HOSTS
    }

    @Serializable
    private data class StartAck(
        val ok: Boolean = false,
        val port: Int = 0,
        val error: String? = null,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val proxyExec = Executors.newSingleThreadExecutor()

    private val _status = MutableStateFlow(RustNetStatus())
    val status: StateFlow<RustNetStatus> = _status.asStateFlow()

    private val _config = MutableStateFlow(RustNetConfig())
    val config: StateFlow<RustNetConfig> = _config.asStateFlow()

    @Volatile
    var gatewayPort: Int = 0
        private set

    @Volatile
    private var appInForeground: Boolean = true

    private var statusPollJob: Job? = null
    private var lastPersistedStateJson: String = ""
    private var connectivityRegistered = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            onConnectivitySignatureChanged()
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            onConnectivitySignatureChanged()
        }

        override fun onLost(network: Network) {
            onConnectivitySignatureChanged()
        }
    }

    fun start() {
        scope.launch {
            val savedState = db.getKv(KV_SELECT_STATE).orEmpty()
            val savedNodes = db.getKv(KV_CUSTOM_NODES)?.let { raw ->
                runCatching { ForHappyJson.decodeFromString<List<CustomEdgeNode>>(raw) }.getOrNull()
            } ?: emptyList()
            val ipv6Mode = db.getKv(KV_IPV6_MODE) ?: "auto"
            val echEnabled = db.getKv(KV_ECH_ENABLED)?.toBooleanStrictOrNull() ?: true
            val netSig = computeNetworkSignature()

            val cfg = RustNetConfig(
                selectStateJson = savedState,
                customNodes = savedNodes,
                ipv6Mode = ipv6Mode,
                echEnabled = echEnabled,
                networkSig = netSig,
            )
            _config.value = cfg
            lastPersistedStateJson = savedState

            if (!RustNetBridge.libraryLoaded) {
                _status.value = RustNetStatus(
                    running = false,
                    phase = "fallback_okhttp",
                    lastError = "Rust JNI 库未加载 (${RustNetBridge.loadError ?: "unknown"})，已降级标准网络",
                )
                return@launch
            }

            val ackRaw = runCatching {
                RustNetBridge.nativeStart(ForHappyJson.encodeToString(cfg))
            }.getOrElse { """{"ok":false,"error":"${it.message}"}""" }

            val ack = runCatching { ForHappyJson.decodeFromString<StartAck>(ackRaw) }
                .getOrDefault(StartAck(ok = false, error = ackRaw))

            if (ack.ok && ack.port > 0) {
                gatewayPort = ack.port
                applyWebViewProxy(ack.port)
                refreshStatusNow()
                startStatusLoop()
                registerConnectivityObserver()
            } else {
                gatewayPort = 0
                _status.value = RustNetStatus(
                    running = false,
                    phase = "failed",
                    lastError = ack.error ?: "Rust gateway failed to start",
                )
            }
        }
    }

    fun stop() {
        statusPollJob?.cancel()
        gatewayPort = 0
        if (RustNetBridge.libraryLoaded) {
            runCatching { RustNetBridge.nativeStop() }
        }
        clearWebViewProxy()
    }

    fun onForegroundChanged(foreground: Boolean) {
        appInForeground = foreground
        if (foreground && RustNetBridge.libraryLoaded) {
            scope.launch {
                runCatching { RustNetBridge.nativeOnAppResumed() }
                refreshStatusNow()
            }
        }
    }

    fun triggerReselect() {
        if (!RustNetBridge.libraryLoaded) return
        scope.launch {
            runCatching { RustNetBridge.nativeReselect() }
            refreshStatusNow()
        }
    }

    fun updateSettings(
        ipv6Mode: String? = null,
        echEnabled: Boolean? = null,
        customNodes: List<CustomEdgeNode>? = null,
    ) {
        scope.launch {
            val cur = _config.value
            val next = cur.copy(
                ipv6Mode = ipv6Mode ?: cur.ipv6Mode,
                echEnabled = echEnabled ?: cur.echEnabled,
                customNodes = customNodes ?: cur.customNodes,
                selectStateJson = lastPersistedStateJson,
                networkSig = computeNetworkSignature(),
            )
            db.putKv(KV_IPV6_MODE, next.ipv6Mode)
            db.putKv(KV_ECH_ENABLED, next.echEnabled.toString())
            db.putKv(KV_CUSTOM_NODES, ForHappyJson.encodeToString(next.customNodes))
            _config.value = next

            // Restart Rust engine cleanly with updated ECH / IPv6 / custom node config
            if (RustNetBridge.libraryLoaded) {
                runCatching { RustNetBridge.nativeStop() }
                val ackRaw = runCatching { RustNetBridge.nativeStart(ForHappyJson.encodeToString(next)) }.getOrNull()
                val ack = ackRaw?.let { runCatching { ForHappyJson.decodeFromString<StartAck>(it) }.getOrNull() }
                if (ack?.ok == true && ack.port > 0) {
                    gatewayPort = ack.port
                    applyWebViewProxy(ack.port)
                    refreshStatusNow()
                }
            }
        }
    }

    private fun startStatusLoop() {
        statusPollJob?.cancel()
        statusPollJob = scope.launch {
            while (isActive) {
                refreshStatusNow()
                delay(if (appInForeground) 1_500L else 12_000L)
            }
        }
    }

    private suspend fun refreshStatusNow() {
        if (!RustNetBridge.libraryLoaded) return
        val raw = runCatching { RustNetBridge.nativeGetStatusJson() }.getOrNull() ?: return
        val parsed = runCatching { ForHappyJson.decodeFromString<RustNetStatus>(raw) }.getOrNull() ?: return
        if (parsed.port > 0) {
            gatewayPort = parsed.port
        }
        _status.value = parsed
        val ss = parsed.selectStateJson
        if (ss.isNotBlank() && ss != lastPersistedStateJson) {
            lastPersistedStateJson = ss
            db.putKv(KV_SELECT_STATE, ss)
        }
    }

    private fun onConnectivitySignatureChanged() {
        if (!RustNetBridge.libraryLoaded) return
        val sig = computeNetworkSignature()
        if (sig == _config.value.networkSig) return
        _config.value = _config.value.copy(networkSig = sig)
        scope.launch {
            runCatching { RustNetBridge.nativeOnNetworkChanged(sig) }
            refreshStatusNow()
        }
    }

    private fun registerConnectivityObserver() {
        if (connectivityRegistered) return
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val req = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        runCatching {
            cm.registerNetworkCallback(req, networkCallback)
            connectivityRegistered = true
        }
    }

    private fun computeNetworkSignature(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return "unknown"
        val active = cm.activeNetwork ?: return "offline"
        val caps = cm.getNetworkCapabilities(active) ?: return "unknown"
        val transport = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cell"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "eth"
            else -> "other"
        }
        val link = cm.getLinkProperties(active)
        val dns = link?.dnsServers?.joinToString(",") { it.hostAddress.orEmpty() }.orEmpty()
        return "$transport:${active.networkHandle}:$dns"
    }

    private fun applyWebViewProxy(port: Int) {
        if (port <= 0) return
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) return
        val cfg = ProxyConfig.Builder()
            .addProxyRule("http://127.0.0.1:$port")
            .addBypassRule("127.0.0.1")
            .addBypassRule("localhost")
            .build()
        runCatching {
            ProxyController.getInstance().setProxyOverride(cfg, proxyExec) {}
        }
    }

    private fun clearWebViewProxy() {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) return
        runCatching {
            ProxyController.getInstance().clearProxyOverride(proxyExec) {}
        }
    }
}
