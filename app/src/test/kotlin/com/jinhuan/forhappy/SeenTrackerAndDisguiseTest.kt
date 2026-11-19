package com.jinhuan.forhappy

import com.jinhuan.forhappy.core.model.DayEntry
import com.jinhuan.forhappy.core.model.ForHappyJson
import com.jinhuan.forhappy.core.model.MemeItem
import com.jinhuan.forhappy.core.model.MemesPageResponse
import com.jinhuan.forhappy.core.model.SeenVisitEntry
import com.jinhuan.forhappy.core.seen.SeenTracker
import com.jinhuan.forhappy.core.seen.computeVisibleAreaRatio
import com.jinhuan.forhappy.core.seen.shouldCountSeenDwell
import com.jinhuan.forhappy.ui.disguise.FeishuDisguiseHelpers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap

class SeenTrackerAndDisguiseTest {

    @Test
    fun seenTrackerQueuesAndFlushesUserVisitsInUtc8Day() = runBlocking {
        val kv = ConcurrentHashMap<String, String>()
        val sentBatches = mutableListOf<Pair<List<SeenVisitEntry>, Long>>()
        // 2026-11-17 16:30:00 UTC -> 2026-11-18 00:30:00 CST (UTC+8)
        var clockSec = 1794933000L
        val tracker = SeenTracker(
            kvGet = { kv[it] },
            kvPut = { k, v -> kv[k] = v },
            sendVisitsFn = { visits, uid ->
                sentBatches.add(visits to uid)
                true
            },
            nowSecFn = { clockSec },
        )

        assertEquals("2026-11-18", SeenTracker.dayOfUtc8(clockSec))
        tracker.bindUserAndSync(42L)

        val enqueued = tracker.markSeenSuspend(memeId = 12345L, existingSeenAt = null, memeDay = "2026-11-17")
        assertTrue(enqueued)
        assertTrue(tracker.isSeen(12345L))
        assertEquals(1, tracker.dailySeenCount.value)
        assertEquals(1, tracker.newlySeenByDay.value["2026-11-17"])

        // Duplicate mark on the same day does not re-queue
        val dup = tracker.markSeenSuspend(memeId = 12345L, existingSeenAt = null, memeDay = "2026-11-17")
        assertFalse(dup)

        val flushed = tracker.flushNow()
        assertTrue(flushed)
        assertEquals(1, sentBatches.size)
        assertEquals(42L, sentBatches[0].second)
        assertEquals(listOf(SeenVisitEntry(12345L, clockSec)), sentBatches[0].first)
        assertEquals("", kv["seen-pending:42"])
    }

    @Test
    fun seenTrackerSplitsBatchesAt100EntriesAndHandlesGuestMode() = runBlocking {
        val kv = ConcurrentHashMap<String, String>()
        val sentBatches = mutableListOf<Int>()
        val tracker = SeenTracker(
            kvGet = { kv[it] },
            kvPut = { k, v -> kv[k] = v },
            sendVisitsFn = { visits, _ ->
                sentBatches.add(visits.size)
                true
            },
            nowSecFn = { 1794910000L },
        )

        // Guest mode persists locally without scheduling network flush (returns false for flushNeeded)
        assertFalse(tracker.markSeenSuspend(999L, memeDay = "2026-11-17"))
        assertTrue(tracker.isSeen(999L))
        assertTrue(sentBatches.isEmpty())

        // Switch to authenticated user and enqueue 105 distinct items
        tracker.bindUserAndSync(7L)
        for (i in 1L..105L) {
            tracker.markSeenSuspend(i, memeDay = "2026-11-17")
        }
        assertTrue(tracker.flushNow())
        assertEquals(listOf(100, 5), sentBatches)
    }

    @Test
    fun dayEntryAndMemeItemDecodeUnseenAndSeenAtFields() {
        val rawDay = """
            {
              "day": "2026-11-17",
              "n": 88,
              "unseen": 23,
              "latest": {
                "id": 12720,
                "at": 1794910000,
                "by": "GotohHitori",
                "text": "[alu:blush] 今日份快乐",
                "src": true
              }
            }
        """.trimIndent()
        val day = ForHappyJson.decodeFromString<DayEntry>(rawDay)
        assertEquals("2026-11-17", day.day)
        assertEquals(88, day.totalCount)
        assertEquals(23, day.unseen)
        assertEquals(12720L, day.latest?.id)
        assertEquals(true, day.latest?.src)

        val rawMeme = """
            {
              "id": 12720,
              "day": "2026-11-17",
              "seen_at": 1794910500,
              "source_url": "https://linux.do/t/topic/12345"
            }
        """.trimIndent()
        val meme = ForHappyJson.decodeFromString<MemeItem>(rawMeme)
        assertEquals(1794910500L, meme.seenAt)
        assertEquals("https://linux.do/t/topic/12345", meme.sourceUrl)
    }

    @Test
    fun feishuDisguiseHelpersMatchUnsigned32BitJsHashAndDayGroupName() {
        // JS check: 12720 * 2654435761 = 33764422879920; >>> 0 = 1684966064; % 40 = 24 -> "韩冰"
        assertEquals("韩冰", FeishuDisguiseHelpers.imFake(12720L))
        // epochDay(2026-11-17) = 20774 -> 20774 % 12 = 2 -> "设计走查"
        assertEquals("设计走查 · 11月17日", FeishuDisguiseHelpers.imGroupName("2026-11-17"))
    }

    @Test
    fun tallSingleColumnCardMeetsSeenCoverageAgainstClippedViewport() {
        // Card height = 2600px (> viewport height 1000px). Filling the 1000px viewport must yield 1.0f, not 1000/2600=0.38f!
        val fullViewportRatio = computeVisibleAreaRatio(
            itemTopPx = -400f,
            itemBottomPx = 2200f,
            viewportTopPx = 0f,
            viewportBottomPx = 1000f,
        )
        assertEquals(1.0f, fullViewportRatio, 0.001f)
        assertTrue(
            shouldCountSeenDwell(
                memeId = 12800L,
                isMediaReady = true,
                isBlurred = false,
                visibleRatio = fullViewportRatio,
                scrollSpeedDpPerSec = 0f,
            ),
        )

        // When scrolled mostly off so only 400px of the 1000px viewport is covered -> 0.40f (< 0.60f)
        val partialRatio = computeVisibleAreaRatio(
            itemTopPx = 600f,
            itemBottomPx = 3200f,
            viewportTopPx = 0f,
            viewportBottomPx = 1000f,
        )
        assertEquals(0.40f, partialRatio, 0.001f)
        assertFalse(
            shouldCountSeenDwell(
                memeId = 12800L,
                isMediaReady = true,
                isBlurred = false,
                visibleRatio = partialRatio,
                scrollSpeedDpPerSec = 0f,
            ),
        )
    }

    @Test
    fun memesPageResponsePreservesUnseenSnapshotAnchorAndTotals() {
        val resp = MemesPageResponse(
            items = listOf(MemeItem(id = 12801L, day = "2026-11-19")),
            offset = 0,
            more = true,
            anchor = "1795060123",
            unseenTotal = 42,
            total = 120,
        )
        val encoded = ForHappyJson.encodeToString(MemesPageResponse.serializer(), resp)
        val decoded = ForHappyJson.decodeFromString<MemesPageResponse>(encoded)
        assertEquals("1795060123", decoded.anchor)
        assertEquals(42, decoded.unseenTotal)
        assertEquals(120, decoded.total)

        // MemeItem without explicit `day` resolves UTC+8 day from `created_at`
        // 1794933000 UTC = 2026-11-18 00:30:00 CST (UTC+8)
        val noDayMeme = MemeItem(id = 12805L, day = null, createdAt = 1794933000L)
        assertEquals("2026-11-18", noDayMeme.resolvedDay)
    }
}
