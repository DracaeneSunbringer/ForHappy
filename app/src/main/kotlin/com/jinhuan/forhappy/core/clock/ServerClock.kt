package com.jinhuan.forhappy.core.clock

import android.os.SystemClock
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

/**
 * Calibrates local elapsed time against authoritative server epoch seconds from JSON `now`
 * or HTTP `Date` headers so countdowns and island time remain immune to device clock drift.
 */
object ServerClock {
    private val calibrated = AtomicBoolean(false)
    private val baseServerMs = AtomicLong(0L)
    private val baseElapsedMs = AtomicLong(0L)

    fun calibrateFromServerSec(serverSec: Double?) {
        if (serverSec == null || !serverSec.isFinite() || serverSec <= 1_500_000_000.0) return
        val serverMs = (serverSec * 1000.0).toLong()
        baseElapsedMs.set(SystemClock.elapsedRealtime())
        baseServerMs.set(serverMs)
        calibrated.set(true)
    }

    fun calibrateFromServerSecLong(serverSec: Long?) {
        if (serverSec == null || serverSec <= 1_500_000_000L) return
        calibrateFromServerSec(serverSec.toDouble())
    }

    fun calibrateFromHttpDate(dateHeader: String?) {
        if (dateHeader.isNullOrBlank()) return
        runCatching {
            val zdt = ZonedDateTime.parse(dateHeader.trim(), DateTimeFormatter.RFC_1123_DATE_TIME)
            val ms = zdt.toInstant().toEpochMilli()
            if (ms > 1_500_000_000_000L && !calibrated.get()) {
                baseElapsedMs.set(SystemClock.elapsedRealtime())
                baseServerMs.set(ms)
                calibrated.set(true)
            }
        }
    }

    fun serverNowMs(): Long {
        if (!calibrated.get()) return System.currentTimeMillis()
        val delta = max(0L, SystemClock.elapsedRealtime() - baseElapsedMs.get())
        return baseServerMs.get() + delta
    }

    fun serverNowSec(): Long = serverNowMs() / 1000L

    fun isCalibrated(): Boolean = calibrated.get()

    fun formatCountdown(targetEpochSec: Long, nowSec: Long = serverNowSec()): String {
        val left = max(0L, targetEpochSec - nowSec)
        if (left == 0L) return "已就绪"
        val days = left / 86400L
        val hours = (left % 86400L) / 3600L
        val minutes = (left % 3600L) / 60L
        val seconds = left % 60L
        return buildString {
            if (days > 0L) append("${days}天 ")
            if (hours > 0L) append("${hours}小时 ")
            if (minutes > 0L || hours > 0L || days > 0L) append("${minutes}分 ")
            append(seconds.toString().padStart(2, '0')).append("秒")
        }
    }
}
