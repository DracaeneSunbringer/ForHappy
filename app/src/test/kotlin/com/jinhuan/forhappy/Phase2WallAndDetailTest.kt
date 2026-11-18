package com.jinhuan.forhappy

import com.jinhuan.forhappy.core.model.AnchorTag
import com.jinhuan.forhappy.core.model.AuthorOption
import com.jinhuan.forhappy.core.model.ForHappyJson
import com.jinhuan.forhappy.core.model.MemeItem
import com.jinhuan.forhappy.core.model.MemeMediaItem
import com.jinhuan.forhappy.core.model.PuzzleSpec
import com.jinhuan.forhappy.core.model.SeriesItem
import com.jinhuan.forhappy.core.model.SiteNotice
import com.jinhuan.forhappy.core.model.WallFilterState
import com.jinhuan.forhappy.core.model.WhoReactedResponse
import com.jinhuan.forhappy.core.seen.computeVisibleAreaRatio
import com.jinhuan.forhappy.core.seen.shouldAutoReblurRevealed
import com.jinhuan.forhappy.core.seen.shouldCountSeenDwell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase2WallAndDetailTest {

    @Test
    fun wallFilterStateMapsNsfwModesAndBlurLogic() {
        assertNull(WallFilterState(nsfwMode = "blur").nsfwQueryValue())
        assertNull(WallFilterState(nsfwMode = "show").nsfwQueryValue())
        assertEquals("0", WallFilterState(nsfwMode = "hide").nsfwQueryValue())
        assertEquals("only", WallFilterState(nsfwMode = "only").nsfwQueryValue())

        val safeMeme = MemeItem(id = 1L, nsfw = 0, sensitive = false, spoiler = false)
        val riskyMeme = MemeItem(id = 2L, nsfw = 1, sensitive = false, spoiler = false)

        assertFalse(WallFilterState(nsfwMode = "blur").shouldBlurMeme(safeMeme, blurAllEnabled = false))
        assertTrue(WallFilterState(nsfwMode = "blur").shouldBlurMeme(riskyMeme, blurAllEnabled = false))
        assertTrue(WallFilterState(nsfwMode = "only").shouldBlurMeme(riskyMeme, blurAllEnabled = false))
        assertFalse(WallFilterState(nsfwMode = "show").shouldBlurMeme(riskyMeme, blurAllEnabled = false))
        assertTrue(WallFilterState(nsfwMode = "show").shouldBlurMeme(safeMeme, blurAllEnabled = true))
    }

    @Test
    fun wallFilterStateSummaryCapsuleLimitsToThreeSegmentsWithPlusOverflow() {
        val defaultState = WallFilterState()
        assertEquals("最新·双列", defaultState.summaryCapsuleLabel())

        val threeSegmentState = WallFilterState(
            sort = "hot",
            unseenOnly = true,
            nsfwMode = "blur",
            textFilter = "",
            viewMode = "dual",
        )
        assertEquals("热门·未看·双列", threeSegmentState.summaryCapsuleLabel())

        val overflowState = WallFilterState(
            sort = "hot",
            unseenOnly = true,
            nsfwMode = "hide",
            textFilter = "only",
            viewMode = "pager",
        )
        assertEquals("热门·未看·隐藏敏感 +2", overflowState.summaryCapsuleLabel())
    }

    @Test
    fun seenDwellObserverEnforcesViewportRatioAndScrollSpeedGates() {
        // Full overlap
        assertEquals(1.0f, computeVisibleAreaRatio(100f, 300f, 0f, 1000f), 0.001f)
        // Half overlap (bottom half cut off)
        assertEquals(0.5f, computeVisibleAreaRatio(900f, 1100f, 0f, 1000f), 0.001f)
        // 60% overlap threshold
        assertEquals(0.6f, computeVisibleAreaRatio(880f, 1080f, 0f, 1000f), 0.001f)
        // Disjoint / invalid geometry
        assertEquals(0f, computeVisibleAreaRatio(1100f, 1300f, 0f, 1000f), 0.001f)
        assertEquals(0f, computeVisibleAreaRatio(300f, 300f, 0f, 1000f), 0.001f)

        // Dwell gating
        assertTrue(
            shouldCountSeenDwell(
                memeId = 42L,
                isMediaReady = true,
                isBlurred = false,
                visibleRatio = 0.60f,
                scrollSpeedDpPerSec = 1500f,
            ),
        )
        assertFalse(
            shouldCountSeenDwell(
                memeId = 42L,
                isMediaReady = true,
                isBlurred = false,
                visibleRatio = 0.59f,
                scrollSpeedDpPerSec = 0f,
            ),
        )
        assertFalse(
            shouldCountSeenDwell(
                memeId = 42L,
                isMediaReady = true,
                isBlurred = false,
                visibleRatio = 1.0f,
                scrollSpeedDpPerSec = 1501f,
            ),
        )
        assertFalse(
            shouldCountSeenDwell(
                memeId = 42L,
                isMediaReady = true,
                isBlurred = true,
                visibleRatio = 1.0f,
                scrollSpeedDpPerSec = 0f,
            ),
        )
        assertFalse(
            shouldCountSeenDwell(
                memeId = 42L,
                isMediaReady = false,
                isBlurred = false,
                visibleRatio = 1.0f,
                scrollSpeedDpPerSec = 0f,
            ),
        )

        // Auto-reblur rule when revealed card leaves >= 50% viewport
        assertFalse(shouldAutoReblurRevealed(wasRevealed = false, visibleRatio = 0.2f))
        assertFalse(shouldAutoReblurRevealed(wasRevealed = true, visibleRatio = 0.50f))
        assertTrue(shouldAutoReblurRevealed(wasRevealed = true, visibleRatio = 0.49f))
    }

    @Test
    fun memeMediaAndLongImageDetectionHandleSingleAndMultiItems() {
        val longSingle = MemeItem(
            id = 100L,
            image = "/uploads/tall.png",
            width = 800,
            height = 2400,
        )
        assertTrue(longSingle.isLongImage)
        assertEquals(1, longSingle.mediaItems.size)
        assertEquals("/uploads/tall.png", longSingle.mediaItems.first().resolvedFullUrl)
        assertFalse(longSingle.mediaItems.first().isVideo)

        val videoSingle = MemeItem(
            id = 101L,
            thumb = "/uploads/vid_thumb.jpg",
            media = "/uploads/clip.mp4",
            ext = "mp4",
        )
        assertEquals(1, videoSingle.mediaItems.size)
        assertTrue(videoSingle.mediaItems.first().isVideo)
        assertFalse(videoSingle.mediaItems.first().isLive)

        val liveItem = MemeMediaItem(
            u = "/uploads/live_cover.jpg",
            o = "/uploads/live_orig.jpg",
            v = "/uploads/live_motion.mp4",
            k = "l",
        )
        assertTrue(liveItem.isLive)
        assertFalse(liveItem.isVideo)
        assertTrue(liveItem.hasOriginalUnmasked)
    }

    @Test
    fun phase2ProtocolDtosDecodeExactSitePayloads() {
        val anchor = ForHappyJson.decodeFromString<AnchorTag>(
            """
            {
              "id": 12777,
              "day": "2026-11-16",
              "thumb": "/uploads/t12777.webp",
              "spoiler": false,
              "note": "猫猫表情包",
              "title": "原标题"
            }
            """.trimIndent(),
        )
        assertEquals(12777L, anchor.id)
        assertEquals("2026-11-16", anchor.day)
        assertEquals("猫猫表情包", anchor.displayLabel)

        val notice = ForHappyJson.decodeFromString<SiteNotice>(
            """{"id":9,"kind":"daily","body":"今日公告测试","created_at":1731800000}""",
        )
        assertEquals("daily", notice.kind)
        assertEquals("今日公告测试", notice.body)

        val series = ForHappyJson.decodeFromString<SeriesItem>(
            """
            {
              "name": "每周精选",
              "desc": "周报合集",
              "n": 18,
              "likes": 99,
              "first_at": 1700000000,
              "last_at": 1730000000,
              "covers": [{"src": "/uploads/c1.jpg", "spoiler": false}]
            }
            """.trimIndent(),
        )
        assertEquals("每周精选", series.name)
        assertEquals(18, series.n)
        assertEquals(1, series.covers.size)

        val author = ForHappyJson.decodeFromString<AuthorOption>(
            """{"username":"alice","name":"AliceLin","avatar":"/a.png","posts":25}""",
        )
        assertEquals("AliceLin", author.displayName)
        assertEquals(25, author.posts)

        val who = ForHappyJson.decodeFromString<WhoReactedResponse>(
            """
            {
              "items": [{"username":"u1","name":"小一","emoji":"👍"}],
              "replies": [{"username":"u2","text":"好图"}]
            }
            """.trimIndent(),
        )
        assertEquals(1, who.resolvedReactions.size)
        assertEquals("👍", who.resolvedReactions.first().emoji)
        assertEquals("好图", who.replies.first().text)

        val puzzle = ForHappyJson.decodeFromString<PuzzleSpec>(
            """
            {
              "id": 88,
              "status": "open",
              "reward_kind": "ldc",
              "reward_type": "fixed",
              "reward": "5",
              "layer": 1,
              "layer_count": 3,
              "solved": false,
              "passed": [{"idx": 0, "kind": "pass", "prompt": "第一层"}],
              "all_layers": [
                {"idx": 0, "kind": "pass", "prompt": "第一层"},
                {"idx": 1, "kind": "choice", "prompt": "第二层", "data": {"options": ["A", "B"]}}
              ],
              "winners": [{"id": 1, "username": "w1", "name": "赢家"}]
            }
            """.trimIndent(),
        )
        assertEquals(3, puzzle.layerCount)
        assertEquals(1, puzzle.passed.size)
        assertEquals(2, puzzle.allLayers.size)
        assertEquals("赢家", puzzle.winners.first().displayName)
    }
}
