package com.jinhuan.forhappy

import com.jinhuan.forhappy.core.clock.IsleClockRules
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class IsleClockRulesTest {
    @After
    fun resetClock() {
        IsleClockRules.b12ResetForTest()
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectClockBeforeLaunch() {
        IsleClockRules.b12ConfigureClock(IsleClockRules.ISLE_LAUNCH - 10L)
    }

    @Test
    fun configuredLongNightClockSlowsNightWindow() {
        val epoch = IsleClockRules.ISLE_LAUNCH + 86400L
        IsleClockRules.b12ConfigureClock(epoch)
        // Before epoch: linear local Beijing time
        assertEquals(epoch - 100L + 28800L, IsleClockRules.b12ClockSeconds(epoch - 100L))
        // Night freezing from 22:00 to 08:00 CST (compute exact 23:00 CST since ISLE_LAUNCH is 12:00Z / 20:00 CST)
        val cstDayEpoch = Math.floorDiv(IsleClockRules.ISLE_LAUNCH + 28800L, 86400L) + 10L
        val nightStartUtc = cstDayEpoch * 86400L - 28800L - 3600L // 23:00 CST of day-1
        val nightEndUtc = nightStartUtc + 7L * 3600L // 06:00 CST
        assertEquals(23, IsleClockRules.hourOf(nightStartUtc))
        assertTrue(IsleClockRules.b12IsLongNight(nightStartUtc))
        assertEquals(
            IsleClockRules.b12ClockSeconds(nightStartUtc),
            IsleClockRules.b12ClockSeconds(nightEndUtc),
        )
    }

    @Test
    fun deterministicHashAndMarketEventsMatchRules() {
        val h1 = IsleClockRules.worldHash("weather:2335-01-02")
        val h2 = IsleClockRules.worldHash("weather:2335-01-02")
        assertTrue(h1 in 0.0..<1.0)
        assertEquals(h1, h2, 0.0)

        val t = IsleClockRules.ISLE_LAUNCH + 100_000L
        val snap = IsleClockRules.snapshot(t)
        assertEquals(2, snap.events.size)
        assertTrue(snap.events[0].guildId != snap.events[1].guildId)
        val price = IsleClockRules.priceOf("turnip", snap.dayKey, 0)
        assertTrue(price > 0)
        assertTrue(abs(IsleClockRules.supplyFactor(0, 1000) - 1.06) < 1e-6)
    }
}
