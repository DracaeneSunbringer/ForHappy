package com.jinhuan.forhappy

import com.jinhuan.forhappy.core.model.DecoConfig
import com.jinhuan.forhappy.core.model.DynamicModule
import com.jinhuan.forhappy.core.model.ForHappyJson
import com.jinhuan.forhappy.core.model.HbKeyStatus
import com.jinhuan.forhappy.core.model.MeItemsResponse
import com.jinhuan.forhappy.core.model.MemeItem
import com.jinhuan.forhappy.core.model.MoyuConfig
import com.jinhuan.forhappy.core.model.MoyuCustomHoliday
import com.jinhuan.forhappy.core.model.NOTIF_CATEGORY_TABS
import com.jinhuan.forhappy.core.model.NOTIF_PREF_OPTIONS
import com.jinhuan.forhappy.core.model.RoadCommentItem
import com.jinhuan.forhappy.core.model.TodayStats
import com.jinhuan.forhappy.core.moyu.MoyuCalculator
import com.jinhuan.forhappy.ui.hub.resolveHubCatalog
import com.jinhuan.forhappy.ui.notifications.NOTIF_PREF_GROUPS
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class Phase5DiscoverMeSettingsTest {

    private val shanghai = ZoneId.of("Asia/Shanghai")

    private fun epochSec(year: Int, month: Int, day: Int, hour: Int, min: Int): Long =
        ZonedDateTime.of(year, month, day, hour, min, 0, 0, shanghai).toEpochSecond()

    @Test
    fun moyuCalculatorComputesWorkingBeforeAfterWeekendAndFebPaydayClamping() {
        val cfg = MoyuConfig(
            startHm = "09:00",
            offHm = "18:00",
            payDay = 31,
            monthlySalary = 10_000.0,
            workDaysPerMonth = 20.0,
            holidays = listOf(MoyuCustomHoliday("周年庆", "2026-01-15")),
        )

        // Wednesday 2026-02-18 13:30 CST (halfway through 09:00-18:00)
        val working = MoyuCalculator.compute(epochSec(2026, 2, 18, 13, 30), cfg, shanghai)
        assertEquals(MoyuCalculator.WorkPhase.WORKING, working.phase)
        assertEquals(0.5f, working.progress, 0.001f)
        assertEquals(250.0, working.earnedTodayCny, 0.05)
        assertEquals("¥250.0", working.formattedEarnedToday)
        assertEquals(2L, working.daysToWeekend) // Wed -> Fri = 2 days
        assertEquals(10L, working.daysToPayday) // Feb 2026 has 28 days: 28 - 18 = 10
        assertTrue(working.countdownText.startsWith("距下班"))
        // 2026-01-15 is before 2026-02-18 so rolls over to 2027-01-15
        assertEquals("周年庆" to 331L, working.holidayCountdowns.first())

        // Before work
        val before = MoyuCalculator.compute(epochSec(2026, 2, 18, 8, 0), cfg, shanghai)
        assertEquals(MoyuCalculator.WorkPhase.BEFORE_WORK, before.phase)
        assertEquals(0f, before.progress, 0.001f)
        assertEquals(0.0, before.earnedTodayCny, 0.001)
        assertTrue(before.countdownText.startsWith("距开工"))

        // Off work
        val off = MoyuCalculator.compute(epochSec(2026, 2, 18, 19, 0), cfg, shanghai)
        assertEquals(MoyuCalculator.WorkPhase.OFF_WORK, off.phase)
        assertEquals(1f, off.progress, 0.001f)
        assertEquals(500.0, off.earnedTodayCny, 0.01)

        // Saturday 2026-02-21
        val weekend = MoyuCalculator.compute(epochSec(2026, 2, 21, 14, 0), cfg, shanghai)
        assertEquals(MoyuCalculator.WorkPhase.WEEKEND, weekend.phase)
        assertEquals(0.0, weekend.earnedTodayCny, 0.001)
        assertEquals(0L, weekend.daysToWeekend)
    }

    @Test
    fun moyuReportStylesRenderFourNonEmptyLines() {
        val cfg = MoyuConfig(monthlySalary = 8_000.0, workDaysPerMonth = 20.0)
        val snap = MoyuCalculator.compute(epochSec(2026, 3, 10, 15, 0), cfg, shanghai)
        val today = TodayStats(day = "2026-03-10", seen = 150, newSeen = 140, posts = 2, msgs = 18)

        val styleA = MoyuCalculator.formatReportText("A", snap, today, foregroundSec = 3661L, username = "reimu")
        val styleB = MoyuCalculator.formatReportText("B", snap, today, foregroundSec = 3661L, username = "reimu")
        val styleC = MoyuCalculator.formatReportText("C", snap, today, foregroundSec = 3661L, username = "reimu")

        assertEquals(4, styleA.size)
        assertTrue(styleA.any { it.contains("再看 60 个未看帖") })
        assertEquals(4, styleB.size)
        assertTrue(styleB.any { it.contains("01:01:01") })
        assertEquals(4, styleC.size)
        assertTrue(styleC.any { it.contains("@reimu") })
    }

    @Test
    fun phase5ModelsRoundTripSerialization() {
        val moyuCfg = MoyuConfig(
            startHm = "09:30",
            offHm = "18:30",
            payDay = 15,
            monthlySalary = 12345.0,
            workDaysPerMonth = 22.0,
            showInWallHeader = true,
            showPetOnCard = true,
            holidays = listOf(MoyuCustomHoliday("发薪纪念日", "2026-06-15")),
        )
        val decodedMoyu = ForHappyJson.decodeFromString(MoyuConfig.serializer(), ForHappyJson.encodeToString(moyuCfg))
        assertEquals(moyuCfg, decodedMoyu)

        val decoCfg = DecoConfig(
            customTitle = "摸鱼宗师",
            nickGradient = "aurora",
            bubbleSkin = "moss",
            cleanHideOthersBadges = true,
        )
        val decodedDeco = ForHappyJson.decodeFromString(DecoConfig.serializer(), ForHappyJson.encodeToString(decoCfg))
        assertEquals(decoCfg, decodedDeco)

        val itemsResp = MeItemsResponse(items = listOf(MemeItem(id = 42L, title = "测试")), more = true, total = 99)
        assertEquals(itemsResp, ForHappyJson.decodeFromString(MeItemsResponse.serializer(), ForHappyJson.encodeToString(itemsResp)))

        val hb = HbKeyStatus(bound = true, keyId = "hb_123", revealedKey = "secret-xyz")
        assertEquals(hb, ForHappyJson.decodeFromString(HbKeyStatus.serializer(), ForHappyJson.encodeToString(hb)))

        val comment = RoadCommentItem(id = 7L, roadId = 9L, body = "支持这个需求", authorName = "marisa")
        assertEquals("marisa", comment.displayAuthor)
    }

    @Test
    fun discoverCatalogResolvesDynamicModulesAndExcludesPrimaryTabs() {
        val dynamic = listOf(
            DynamicModule(id = "wall", label = "图廊", route = "/wall", isNewFromTopbar = true),
            DynamicModule(id = "chat", label = "群聊", route = "/chat", isNewFromTopbar = true),
            DynamicModule(id = "novels", label = "接龙书房", route = "/novels", isNewFromTopbar = false),
            DynamicModule(id = "wiki", label = "萌新百科", route = "/wiki", isNewFromTopbar = true),
        )
        val catalog = resolveHubCatalog(dynamic)

        assertFalse(catalog.any { it.route == "/wall" || it.route == "/chat" })
        assertTrue(catalog.any { it.route == "/pets" && it.label == "鱼鱼谷" })
        assertTrue(catalog.any { it.route == "/moyu" })
        assertTrue(catalog.any { it.route == "/road" })
        val wiki = catalog.first { it.route == "/wiki" }
        assertTrue(wiki.isNew)
        assertEquals(1, catalog.count { it.route == "/novels" })
    }

    @Test
    fun notificationNineTabsAndSixteenGroupedPrefsAreComplete() {
        assertEquals(9, NOTIF_CATEGORY_TABS.size)
        assertEquals(
            listOf("all", "at", "cmt", "rx", "like", "chat", "post", "farm", "system"),
            NOTIF_CATEGORY_TABS.map { it.id },
        )

        val groupedKeys = NOTIF_PREF_GROUPS.flatMap { it.second }
        val expectedKeys = NOTIF_PREF_OPTIONS.map { it.first }
        assertEquals(16, expectedKeys.size)
        assertEquals(expectedKeys.size, groupedKeys.distinct().size)
        assertEquals(expectedKeys.toSet(), groupedKeys.toSet())
    }
}
