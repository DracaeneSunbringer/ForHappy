package com.jinhuan.forhappy

import com.jinhuan.forhappy.core.compose.ComposeDraftSnapshot
import com.jinhuan.forhappy.core.compose.ComposeGenre
import com.jinhuan.forhappy.core.compose.ComposeTrayItem
import com.jinhuan.forhappy.core.compose.MaskBrushStyle
import com.jinhuan.forhappy.core.compose.MaskOperation
import com.jinhuan.forhappy.core.compose.MaskPixelEngine
import com.jinhuan.forhappy.core.compose.NormPoint
import com.jinhuan.forhappy.core.compose.PublishStrategy
import com.jinhuan.forhappy.core.compose.PuzzleDraftSpec
import com.jinhuan.forhappy.core.compose.PuzzleLayerDraft
import com.jinhuan.forhappy.core.compose.SocialLinkDetector
import com.jinhuan.forhappy.core.compose.computePublishButtonLabel
import com.jinhuan.forhappy.core.compose.pendingBatchesToPublish
import com.jinhuan.forhappy.core.compose.planPublishBatches
import com.jinhuan.forhappy.core.model.ForHappyJson
import com.jinhuan.forhappy.core.navigation.DeepLinkRouter
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase3ComposeAndMaskTest {

    @Test
    fun batchPlanningAndPartialRetryWorkAcrossAllThreeStrategies() {
        val items = listOf(
            ComposeTrayItem(id = "a", soloInCustom = false),
            ComposeTrayItem(id = "b", soloInCustom = true),
            ComposeTrayItem(id = "c", soloInCustom = false),
            ComposeTrayItem(id = "d", soloInCustom = true),
        )

        // MERGE_ONE collapses all 4 into 1 batch
        val merged = planPublishBatches(items, PublishStrategy.MERGE_ONE)
        assertEquals(1, merged.size)
        assertEquals(listOf("a", "b", "c", "d"), merged[0].items.map { it.id })
        assertEquals("发布", computePublishButtonLabel(ComposeGenre.IMAGE, merged))

        // SOLO_EACH splits into 4 individual batches
        val solo = planPublishBatches(items, PublishStrategy.SOLO_EACH)
        assertEquals(4, solo.size)
        assertEquals("发布 4 帖", computePublishButtonLabel(ComposeGenre.IMAGE, solo))

        // CUSTOM combines non-solo items [a, c] into one batch at slot 0, keeping solo [b] and [d] separate
        val custom = planPublishBatches(items, PublishStrategy.CUSTOM)
        assertEquals(3, custom.size)
        assertEquals(listOf("a", "c"), custom[0].items.map { it.id })
        assertEquals(listOf("b"), custom[1].items.map { it.id })
        assertEquals(listOf("d"), custom[2].items.map { it.id })
        assertEquals("发布 3 帖", computePublishButtonLabel(ComposeGenre.IMAGE, custom))

        // Simulate partial publish success on batch 0 ([a, c] -> postedMemeId = 9901L), batch 1 & 2 still pending
        val partialItems = items.map { cur ->
            if (cur.id == "a" || cur.id == "c") cur.copy(postedMemeId = 9901L) else cur
        }
        val partialBatches = planPublishBatches(partialItems, PublishStrategy.CUSTOM)
        assertEquals(9901L, partialBatches[0].alreadyPostedId)
        assertNull(partialBatches[1].alreadyPostedId)
        assertNull(partialBatches[2].alreadyPostedId)
        val pending = pendingBatchesToPublish(partialBatches)
        assertEquals(listOf("b", "d"), pending.flatMap { it.items }.map { it.id })
        assertEquals("补发剩余 2 项", computePublishButtonLabel(ComposeGenre.IMAGE, partialBatches))
    }

    @Test
    fun maskPixelEngineAppliesMosaicBlurAndBlackoutWithoutMutatingExterior() {
        val w = 64
        val h = 64
        // High-contrast checkerboard so mosaic/blur/blackout all strictly change pixel values
        val src = IntArray(w * h) { idx ->
            val x = idx % w
            val y = idx / w
            if (((x / 2) + (y / 2)) % 2 == 0) 0xFFFF0000.toInt() else 0xFF0000FF.toInt()
        }

        // Box Blackout in center [0.25..0.50] -> pixels [16..32)
        val blackoutOut = MaskPixelEngine.applyMaskOperations(
            src,
            w,
            h,
            listOf(MaskOperation.BoxOp("b1", 0.25f, 0.25f, 0.50f, 0.50f, MaskBrushStyle.BLACKOUT)),
        )
        assertEquals(0xFF111111.toInt(), blackoutOut[20 * w + 20])
        assertEquals(src[0], blackoutOut[0])
        assertEquals(src[63 * w + 63], blackoutOut[63 * w + 63])

        // Box Mosaic in [0.25..0.75] -> pixels [16..48)
        val mosaicOut = MaskPixelEngine.applyMaskOperations(
            src,
            w,
            h,
            listOf(MaskOperation.BoxOp("m1", 0.25f, 0.25f, 0.75f, 0.75f, MaskBrushStyle.MOSAIC)),
        )
        // Average of red & blue checkerboard is violet (~0xFF7F007F) across the 16x16 cell
        assertNotEquals(src[20 * w + 20], mosaicOut[20 * w + 20])
        assertEquals(mosaicOut[20 * w + 20], mosaicOut[21 * w + 21])
        assertEquals(src[2 * w + 2], mosaicOut[2 * w + 2])

        // Box Blur in [0.25..0.75] -> modifies interior, preserves corner exterior [0,0]
        val blurOut = MaskPixelEngine.applyMaskOperations(
            src,
            w,
            h,
            listOf(MaskOperation.BoxOp("l1", 0.25f, 0.25f, 0.75f, 0.75f, MaskBrushStyle.BLUR)),
        )
        assertNotEquals(src[24 * w + 24], blurOut[24 * w + 24])
        assertEquals(src[0], blurOut[0])

        // StrokeOp blackout across middle horizontal band (y = 0.5, radiusNorm = 0.12 -> ~7.6px)
        val strokeOut = MaskPixelEngine.applyMaskOperations(
            src,
            w,
            h,
            listOf(
                MaskOperation.StrokeOp(
                    id = "s1",
                    points = listOf(NormPoint(0.25f, 0.5f), NormPoint(0.75f, 0.5f)),
                    radiusNorm = 0.12f,
                    style = MaskBrushStyle.BLACKOUT,
                ),
            ),
        )
        assertEquals(0xFF111111.toInt(), strokeOut[32 * w + 32])
        assertEquals(src[2 * w + 2], strokeOut[2 * w + 2])
    }

    @Test
    fun socialLinkDetectorRecognizesMajorPlatformsAndIgnoresPlainText() {
        val bili = SocialLinkDetector.detectFirstSocialLink("快看这个 https://www.bilibili.com/video/BV1xx411c7mD?p=1 好笑")
        assertNotNull(bili)
        assertEquals("bilibili.com", bili?.hostLabel)
        assertEquals("https://www.bilibili.com/video/BV1xx411c7mD?p=1", bili?.url)

        val xPost = SocialLinkDetector.detectFirstSocialLink("https://x.com/user/status/123456")
        assertNotNull(xPost)
        assertEquals("x.com", xPost?.hostLabel)

        val xhs = SocialLinkDetector.detectFirstSocialLink("复制打开小红书 http://xhslink.com/a/abc123")
        assertNotNull(xhs)
        assertEquals("xhslink.com", xhs?.hostLabel)

        assertNull(SocialLinkDetector.detectFirstSocialLink("今天摸鱼真开心，没有外链"))
        assertNull(SocialLinkDetector.detectFirstSocialLink("https://example.org/unknown-site"))
    }

    @Test
    fun deepLinkRouterExtractsAndDeduplicatesSharePayloads() {
        val single = DeepLinkRouter.extractSharePayload(
            action = "android.intent.action.SEND",
            mimeType = "image/*",
            extraText = "分享配文",
            singleStreamUri = "content://media/external/images/media/101",
            multiStreamUris = emptyList(),
            clipUris = listOf("content://media/external/images/media/101"),
        )
        assertNotNull(single)
        assertEquals(listOf("content://media/external/images/media/101"), single?.uris)
        assertEquals("分享配文", single?.sharedText)

        val multi = DeepLinkRouter.extractSharePayload(
            action = "android.intent.action.SEND_MULTIPLE",
            mimeType = "image/*",
            extraText = null,
            singleStreamUri = null,
            multiStreamUris = listOf("content://img/1", "content://img/2", "content://img/1"),
            clipUris = listOf("content://img/3"),
        )
        assertNotNull(multi)
        assertEquals(listOf("content://img/1", "content://img/2", "content://img/3"), multi?.uris)

        assertNull(
            DeepLinkRouter.extractSharePayload(
                action = "android.intent.action.VIEW",
                mimeType = "text/plain",
                extraText = "https://52dmeme.com/m/1",
                singleStreamUri = null,
                multiStreamUris = emptyList(),
                clipUris = emptyList(),
            ),
        )
    }

    @Test
    fun composeDraftSnapshotRoundTripsCleanlyThroughJson() {
        val original = ComposeDraftSnapshot(
            genre = ComposeGenre.IMAGE,
            strategy = PublishStrategy.CUSTOM,
            trayItems = listOf(
                ComposeTrayItem(
                    id = "item-1",
                    localFilePath = "/cache/raw.jpg",
                    maskedFilePath = "/cache/masked.jpg",
                    soloInCustom = true,
                    keepOriginalUnmasked = true,
                    maskOps = listOf(
                        MaskOperation.BoxOp("m1", 0.1f, 0.2f, 0.4f, 0.5f, MaskBrushStyle.MOSAIC),
                        MaskOperation.StrokeOp("m2", listOf(NormPoint(0.2f, 0.2f), NormPoint(0.5f, 0.5f)), 0.03f, MaskBrushStyle.BLUR),
                    ),
                ),
            ),
            title = "测试标题",
            content = "正文 Markdown",
            tags = listOf("梗图", "打码"),
            spoiler = true,
            aiOff = true,
            puzzleEnabled = true,
            puzzle = PuzzleDraftSpec(
                layers = listOf(PuzzleLayerDraft(id = "l1", kind = "pass", prompt = "密码？", answer = "42")),
                hint = "答案在宇宙里",
                rewardKind = "secret",
                rewardType = "text",
                rewardText = "隐藏奖励",
                maxWinners = 5,
            ),
            savedAtMs = 1731900000000L,
        )

        val json = ForHappyJson.encodeToString(original)
        val decoded = ForHappyJson.decodeFromString<ComposeDraftSnapshot>(json)
        assertEquals(original.genre, decoded.genre)
        assertEquals(original.strategy, decoded.strategy)
        assertEquals(2, decoded.trayItems.first().maskOps.size)
        assertTrue(decoded.trayItems.first().keepOriginalUnmasked)
        assertEquals("隐藏奖励", decoded.puzzle.rewardText)
    }
}
