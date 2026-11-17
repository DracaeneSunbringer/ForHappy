package com.jinhuan.forhappy.core.network.rust

/**
 * JNI declarations matching `rust/forhappy-net/src/jni_bridge.rs`.
 */
object RustNetBridge {
    @Volatile
    var libraryLoaded: Boolean = false
        private set

    @Volatile
    var loadError: String? = null
        private set

    init {
        try {
            System.loadLibrary("forhappy_net")
            libraryLoaded = true
        } catch (t: Throwable) {
            loadError = t.message ?: t.javaClass.simpleName
            libraryLoaded = false
        }
    }

    external fun nativeStart(configJson: String): String
    external fun nativeStop()
    external fun nativeReselect()
    external fun nativeOnNetworkChanged(networkSig: String)
    external fun nativeOnAppResumed()
    external fun nativeGetStatusJson(): String
}
