package com.jinhuan.forhappy.core.moyu

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.jinhuan.forhappy.core.model.MoyuConfig
import com.jinhuan.forhappy.core.model.TodayStats
import java.io.ByteArrayOutputStream
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Pure Kotlin + local Canvas calculator for the `发现` Moyu Dashboard, `摸鱼日历`,
 * and 3-layout shareable daily Moyu report (`版式A / 版式B / 版式C`).
 */
object MoyuCalculator {
    private val SHANGHAI_ZONE: ZoneId = ZoneId.of("Asia/Shanghai")

    enum class WorkPhase {
        WEEKEND,
        BEFORE_WORK,
        WORKING,
        OFF_WORK,
    }

    data class MoyuSnapshot(
        val phase: WorkPhase,
        val countdownText: String,
        val progress: Float,
        val earnedTodayCny: Double,
        val daysToWeekend: Long,
        val daysToPayday: Long,
        val daysToNewYear: Long,
        val holidayCountdowns: List<Pair<String, Long>>,
        val dayLabel: String,
    ) {
        val formattedEarnedToday: String
            get() = String.format(Locale.CHINA, "¥%.1f", earnedTodayCny.coerceAtLeast(0.0))

        val formattedEarnedExact: String
            get() = String.format(Locale.CHINA, "¥%.2f", earnedTodayCny.coerceAtLeast(0.0))
    }

    fun compute(
        nowEpochSec: Long,
        config: MoyuConfig,
        zoneId: ZoneId = SHANGHAI_ZONE,
    ): MoyuSnapshot {
        val nowDt = Instant.ofEpochSecond(nowEpochSec).atZone(zoneId)
        val today = nowDt.toLocalDate()
        val isWeekend = nowDt.dayOfWeek == DayOfWeek.SATURDAY || nowDt.dayOfWeek == DayOfWeek.SUNDAY
        val startTime = parseHm(config.startHm, 9, 0)
        val offTime = parseHm(config.offHm, 18, 0)
        val startDt = today.atTime(startTime).atZone(zoneId)
        var offDt = today.atTime(offTime).atZone(zoneId)
        if (!offDt.isAfter(startDt)) {
            offDt = offDt.plusDays(1)
        }

        val (phase, countdownText, progress) = when {
            isWeekend -> Triple(WorkPhase.WEEKEND, "周末愉快，好好休息", 1f)
            nowDt.isBefore(startDt) -> {
                val sec = ChronoUnit.SECONDS.between(nowDt, startDt).coerceAtLeast(0L)
                Triple(WorkPhase.BEFORE_WORK, "距开工 ${formatHms(sec)}", 0f)
            }
            !nowDt.isBefore(offDt) -> {
                Triple(WorkPhase.OFF_WORK, "已下班 · 今日圆满结束", 1f)
            }
            else -> {
                val left = ChronoUnit.SECONDS.between(nowDt, offDt).coerceAtLeast(0L)
                val total = ChronoUnit.SECONDS.between(startDt, offDt).coerceAtLeast(1L)
                val ratio = (1f - left.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                Triple(WorkPhase.WORKING, "距下班 ${formatHms(left)}", ratio)
            }
        }

        val daysPerMonth = config.workDaysPerMonth.coerceIn(1.0, 31.0)
        val dailyPay = if (config.monthlySalary > 0.0) config.monthlySalary / daysPerMonth else 0.0
        val earnedToday = when (phase) {
            WorkPhase.WEEKEND -> 0.0
            WorkPhase.BEFORE_WORK -> 0.0
            WorkPhase.WORKING -> dailyPay * progress.toDouble()
            WorkPhase.OFF_WORK -> dailyPay
        }

        val daysToWeekend = when (nowDt.dayOfWeek) {
            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> 0L
            else -> ChronoUnit.DAYS.between(
                today,
                today.plusDays((DayOfWeek.FRIDAY.value - nowDt.dayOfWeek.value).toLong()),
            ).coerceAtLeast(0L)
        }

        val targetPayDay = config.payDay.coerceIn(1, 31)
        val thisMonthPay = LocalDate.of(
            today.year,
            today.month,
            targetPayDay.coerceAtMost(today.lengthOfMonth()),
        )
        val nextPayDate = if (!thisMonthPay.isBefore(today)) {
            thisMonthPay
        } else {
            val nm = today.plusMonths(1)
            LocalDate.of(nm.year, nm.month, targetPayDay.coerceAtMost(nm.lengthOfMonth()))
        }
        val daysToPayday = ChronoUnit.DAYS.between(today, nextPayDate).coerceAtLeast(0L)

        val nextNewYear = LocalDate.of(today.year + 1, 1, 1)
        val daysToNewYear = ChronoUnit.DAYS.between(today, nextNewYear).coerceAtLeast(0L)

        val customCountdowns = config.holidays.take(20).mapNotNull { h ->
            val parsed = runCatching { LocalDate.parse(h.dateIso.trim()) }.getOrNull() ?: return@mapNotNull null
            if (h.name.isBlank()) return@mapNotNull null
            val targetDate = if (!parsed.isBefore(today)) parsed else parsed.withYear(today.year + 1)
            val d = ChronoUnit.DAYS.between(today, targetDate).coerceAtLeast(0L)
            h.name.trim() to d
        }

        return MoyuSnapshot(
            phase = phase,
            countdownText = countdownText,
            progress = progress,
            earnedTodayCny = earnedToday,
            daysToWeekend = daysToWeekend,
            daysToPayday = daysToPayday,
            daysToNewYear = daysToNewYear,
            holidayCountdowns = customCountdowns,
            dayLabel = today.toString(),
        )
    }

    fun formatReportText(
        style: String, // "A" | "B" | "C"
        snapshot: MoyuSnapshot,
        today: TodayStats?,
        foregroundSec: Long,
        username: String,
    ): List<String> {
        val seen = today?.seen ?: 0
        val newSeen = if ((today?.newSeen ?: 0) > 0) today!!.newSeen else seen
        val posts = today?.posts ?: 0
        val msgs = today?.msgs ?: 0
        val moyuDur = formatHms(foregroundSec)
        val badgeLeft = (200 - newSeen).coerceAtLeast(0)
        val postLeft = (5 - posts).coerceAtLeast(0)

        return when (style) {
            "B" -> listOf(
                "极简战报 · ${snapshot.dayLabel}",
                "今日看帖 $seen · 发图 $posts · 群聊 $msgs",
                "前台摸鱼 $moyuDur · 收益 ${snapshot.formattedEarnedExact}",
                "距发薪 ${snapshot.daysToPayday} 天 · 距周末 ${snapshot.daysToWeekend} 天",
            )
            "C" -> listOf(
                "尽欢摸鱼认证证书",
                "特授予 @$username · ${snapshot.dayLabel}",
                "带薪专注时长 $moyuDur · 折合 ${snapshot.formattedEarnedExact}",
                "站内活跃：浏览 $seen · 发言 $msgs · 发布 $posts",
            )
            else -> listOf(
                "今日尽欢摸鱼日报 · ${snapshot.dayLabel}",
                "今天看了 $seen 帖 · 发图 $posts · 水群 $msgs",
                if (badgeLeft > 0) "🏅 再看 $badgeLeft 个未看帖 → 摸鱼高手" else "🏅 今日「摸鱼高手」已达标！",
                if (postLeft > 0) "🏅 再发 $postLeft 图 → 发图达人" else "🏅 今日「发图达人」已达标！",
            )
        }
    }

    fun renderReportPng(
        style: String,
        snapshot: MoyuSnapshot,
        today: TodayStats?,
        foregroundSec: Long,
        username: String,
    ): ByteArray {
        val width = 900
        val height = 560
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val (bgHex, cardHex, accentHex, titleHex, bodyHex) = when (style) {
            "B" -> listOf(0xFF132219.toInt(), 0xFF1B3024.toInt(), 0xFF34A853.toInt(), 0xFFFFFFFF.toInt(), 0xFFB8D1C2.toInt())
            "C" -> listOf(0xFFFDF5E6.toInt(), 0xFFFFFFFF.toInt(), 0xFFC58C24.toInt(), 0xFF3D290A.toInt(), 0xFF6B532C.toInt())
            else -> listOf(0xFFF6F4EE.toInt(), 0xFFFFFFFF.toInt(), 0xFF1B7F4B.toInt(), 0xFF1B241E.toInt(), 0xFF5A655E.toInt())
        }

        canvas.drawColor(bgHex)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = cardHex }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentHex
            alpha = 70
            this.style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val rect = RectF(44f, 44f, width - 44f, height - 44f)
        canvas.drawRoundRect(rect, 32f, 32f, cardPaint)
        canvas.drawRoundRect(rect, 32f, 32f, borderPaint)

        val accentBar = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentHex }
        canvas.drawRoundRect(RectF(80f, 84f, 92f, 134f), 6f, 6f, accentBar)

        val lines = formatReportText(style, snapshot, today, foregroundSec, username)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = titleHex
            textSize = 40f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText(lines.firstOrNull().orEmpty(), 112f, 124f, titlePaint)

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bodyHex
            textSize = 30f
        }
        lines.drop(1).forEachIndexed { idx, text ->
            canvas.drawText(text, 84f, 220f + idx * 78f, bodyPaint)
        }

        val footPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentHex
            textSize = 24f
        }
        canvas.drawText("52dmeme · @${username.ifBlank { "欢友" }} · ${snapshot.countdownText}", 84f, 482f, footPaint)

        return ByteArrayOutputStream().use { baos ->
            bmp.compress(Bitmap.CompressFormat.PNG, 95, baos)
            bmp.recycle()
            baos.toByteArray()
        }
    }

    fun parseHm(raw: String, defHour: Int, defMin: Int): LocalTime {
        val parts = raw.trim().split(':')
        val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: defHour
        val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: defMin
        return LocalTime.of(h, m)
    }

    fun formatHms(totalSeconds: Long): String {
        val s = totalSeconds.coerceAtLeast(0L)
        val h = s / 3600L
        val m = (s % 3600L) / 60L
        val sec = s % 60L
        return String.format(Locale.CHINA, "%02d:%02d:%02d", h, m, sec)
    }
}
