package com.jinhuan.forhappy.core.navigation

import android.os.SystemClock

/**
 * Pure Kotlin one-shot gate preventing `onStop -> onStart` background stealth triggers
 * (auto-blur / Boss Key) from firing when leaving temporarily to pick photos/files, share,
 * complete OAuth, or authorize unknown-source APK installation.
 */
class StealthExemptionTracker(
    private val ttlMs: Long = 180_000L,
) {
    private var armed: Boolean = false
    private var expiresAtMs: Long = 0L

    @Synchronized
    fun markInternalJump(nowMs: Long) {
        armed = true
        expiresAtMs = nowMs + ttlMs
    }

    @Synchronized
    fun consumeShouldSuppress(nowMs: Long): Boolean {
        val ok = armed && nowMs in 0L..expiresAtMs
        armed = false
        expiresAtMs = 0L
        return ok
    }

    @Synchronized
    fun clear() {
        armed = false
        expiresAtMs = 0L
    }
}

object StealthExemptionGate {
    private val tracker = StealthExemptionTracker()

    fun markInternalJump() {
        tracker.markInternalJump(SystemClock.uptimeMillis())
    }

    fun consumeShouldSuppress(): Boolean =
        tracker.consumeShouldSuppress(SystemClock.uptimeMillis())

    fun clear() {
        tracker.clear()
    }
}
