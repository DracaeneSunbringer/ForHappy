package com.jinhuan.forhappy.core.network.scheduler

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks whether app is in foreground so background polling, badge sync, and preloading
 * automatically back off to avoid burning mobile battery or waking closed HTTP/2 sockets.
 */
object BackgroundGate {
    private val _isForeground = MutableStateFlow(true)
    val isForeground: StateFlow<Boolean> = _isForeground.asStateFlow()

    fun setForeground(foreground: Boolean) {
        _isForeground.value = foreground
    }

    fun pollingIntervalMs(baseMs: Long): Long =
        if (_isForeground.value) baseMs else (baseMs * 6L).coerceAtLeast(18_000L)
}
