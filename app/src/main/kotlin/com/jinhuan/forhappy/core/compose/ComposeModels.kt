package com.jinhuan.forhappy.core.compose

import com.jinhuan.forhappy.core.model.MemeItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.ceil
import kotlin.math.roundToInt

const val MAX_COMPOSE_FILES = 12
const val MAX_SINGLE_FILE_BYTES = 20L * 1024L * 1024L
const val COMPOSE_DRAFT_KV_KEY = "compose.draft.v2"

@Serializable
enum class ComposeGenre {
    IMAGE,
    TEXT,
    LINK,
}

@Serializable
enum class PublishStrategy {
    MERGE_ONE,
    SOLO_EACH,
    CUSTOM,
}

@Serializable
enum class MaskBrushStyle {
    MOSAIC,
    BLUR,
    BLACKOUT,
}

@Serializable
enum class MaskToolKind {
    BOX,
    STROKE,
}

@Serializable
data class NormPoint(
    val x: Float,
    val y: Float,
)

@Serializable
sealed interface MaskOperation {
    val id: String
    val style: MaskBrushStyle

    @Serializable
    @SerialName("box")
    data class BoxOp(
        override val id: String,
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        override val style: MaskBrushStyle,
    ) : MaskOperation

    @Serializable
    @SerialName("stroke")
    data class StrokeOp(
        override val id: String,
        val points: List<NormPoint>,
        val radiusNorm: Float,
        override val style: MaskBrushStyle,
    ) : MaskOperation
}

@Serializable
data class ComposeTrayItem(
    val id: String,
    val localFilePath: String? = null,
    val maskedFilePath: String? = null,
    val posterLocalPath: String? = null,
    val keptRemoteUrl: String? = null,
    val keptRemoteOrigUrl: String? = null,
    val mimeType: String = "image/jpeg",
    val fileName: String = "image.jpg",
    val originalBytes: Long = 0L,
    val compressedBytes: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val durationSec: Int = 0,
    val isVideo: Boolean = false,
    val isGif: Boolean = false,
    val isLive: Boolean = false,
    val soloInCustom: Boolean = false,
    val keepOriginalUnmasked: Boolean = false,
    val maskOps: List<MaskOperation> = emptyList(),
    val uploadedKey: String? = null,
    val uploadedOrigKey: String? = null,
    val uploadedPosterKey: String? = null,
    val uploadState: String = "idle", // "idle" | "uploading" | "ok" | "failed"
    val uploadError: String? = null,
    val postedMemeId: Long? = null,
) {
    val isMasked: Boolean
        get() = maskOps.isNotEmpty()

    val effectiveBytes: Long
        get() = if (compressedBytes > 0L) compressedBytes else originalBytes

    val isOverLimit: Boolean
        get() = keptRemoteUrl == null && effectiveBytes > MAX_SINGLE_FILE_BYTES

    val displayPathOrUrl: String?
        get() = maskedFilePath ?: localFilePath ?: keptRemoteUrl
}

@Serializable
data class PuzzleLayerDraft(
    val id: String,
    val kind: String = "pass", // "pass" | "quiz" | "image"
    val prompt: String = "",
    val answer: String = "",
    val note: String = "",
    val optionsRaw: String = "",
    val clueLocalPath: String? = null,
    val uploadedImgUrl: String? = null,
)

@Serializable
data class PuzzleDraftSpec(
    val layers: List<PuzzleLayerDraft> = emptyList(),
    val hint: String = "",
    val rewardKind: String = "secret", // "secret" | "credit" | "cdk"
    val rewardType: String = "text",   // "text" | "link" | "codes"
    val rewardText: String = "",
    val codes: String = "",
    val maxWinners: Int = 0,
)

@Serializable
data class ComposeDraftSnapshot(
    val genre: ComposeGenre = ComposeGenre.IMAGE,
    val strategy: PublishStrategy = PublishStrategy.MERGE_ONE,
    val trayItems: List<ComposeTrayItem> = emptyList(),
    val title: String = "",
    val content: String = "",
    val tags: List<String> = emptyList(),
    val spoiler: Boolean = false,
    val aiOff: Boolean = false,
    val shareRawUrl: String = "",
    val puzzleEnabled: Boolean = false,
    val puzzle: PuzzleDraftSpec = PuzzleDraftSpec(),
    val savedAtMs: Long = 0L,
)

data class SocialPreviewMedia(
    val kind: String,
    val url: String,
    val poster: String? = null,
)

data class SocialPreviewData(
    val available: Boolean,
    val description: String = "",
    val sensitive: Boolean = false,
    val embedUrl: String? = null,
    val media: List<SocialPreviewMedia> = emptyList(),
)

data class ComposeLaunchRequest(
    val triggerSeq: Long,
    val initialGenre: ComposeGenre = ComposeGenre.IMAGE,
    val autoOpenPicker: Boolean = false,
    val sharedUris: List<String> = emptyList(),
    val preStagedPaths: List<String> = emptyList(),
    val initialTitle: String = "",
    val initialContent: String = "",
    val initialShareUrl: String = "",
    val initialTags: List<String> = emptyList(),
    val editingMeme: MemeItem? = null,
)

data class PlannedPostBatch(
    val batchIndex: Int,
    val items: List<ComposeTrayItem>,
    val alreadyPostedId: Long?,
)

fun planPublishBatches(
    items: List<ComposeTrayItem>,
    strategy: PublishStrategy,
): List<PlannedPostBatch> {
    if (items.isEmpty()) return emptyList()
    return when (strategy) {
        PublishStrategy.MERGE_ONE -> {
            val sharedId = resolveSharedPostedId(items)
            listOf(PlannedPostBatch(batchIndex = 0, items = items, alreadyPostedId = sharedId))
        }
        PublishStrategy.SOLO_EACH -> {
            items.mapIndexed { idx, item ->
                PlannedPostBatch(batchIndex = idx, items = listOf(item), alreadyPostedId = item.postedMemeId)
            }
        }
        PublishStrategy.CUSTOM -> {
            val bundle = items.filter { !it.soloInCustom }
            val result = mutableListOf<PlannedPostBatch>()
            var bundleInserted = false
            for (item in items) {
                if (item.soloInCustom) {
                    result += PlannedPostBatch(
                        batchIndex = result.size,
                        items = listOf(item),
                        alreadyPostedId = item.postedMemeId,
                    )
                } else if (!bundleInserted) {
                    bundleInserted = true
                    result += PlannedPostBatch(
                        batchIndex = result.size,
                        items = bundle,
                        alreadyPostedId = resolveSharedPostedId(bundle),
                    )
                }
            }
            result
        }
    }
}

private fun resolveSharedPostedId(batchItems: List<ComposeTrayItem>): Long? {
    if (batchItems.isEmpty()) return null
    val ids = batchItems.map { it.postedMemeId }
    if (ids.any { it == null }) return null
    val distinct = ids.filterNotNull().distinct()
    return if (distinct.size == 1) distinct.first() else null
}

fun pendingBatchesToPublish(batches: List<PlannedPostBatch>): List<PlannedPostBatch> =
    batches.filter { it.alreadyPostedId == null }

fun computePublishButtonLabel(
    genre: ComposeGenre,
    batches: List<PlannedPostBatch>,
): String {
    if (genre != ComposeGenre.IMAGE) return "发布"
    val postedAny = batches.any { it.alreadyPostedId != null }
    val pendingCount = batches.count { it.alreadyPostedId == null }
    return when {
        postedAny && pendingCount > 0 -> "补发剩余 $pendingCount 项"
        batches.size > 1 -> "发布 ${batches.size} 帖"
        else -> "发布"
    }
}

data class DetectedSocialLink(
    val url: String,
    val hostLabel: String,
    val rawText: String,
)

object SocialLinkDetector {
    private val urlRegex = Regex("""https?://[^\s<>'"()\[\]{}]+""", RegexOption.IGNORE_CASE)

    private val supportedHosts: Map<String, String> = linkedMapOf(
        "bilibili.com" to "bilibili.com",
        "b23.tv" to "b23.tv",
        "x.com" to "x.com",
        "twitter.com" to "twitter.com",
        "xiaohongshu.com" to "xiaohongshu.com",
        "xhslink.com" to "xhslink.com",
        "douyin.com" to "douyin.com",
        "iesdouyin.com" to "iesdouyin.com",
        "weibo.com" to "weibo.com",
        "weibo.cn" to "weibo.cn",
        "youtube.com" to "youtube.com",
        "youtu.be" to "youtu.be",
        "tiktok.com" to "tiktok.com",
        "pixiv.net" to "pixiv.net",
        "zhihu.com" to "zhihu.com",
        "tieba.baidu.com" to "tieba.baidu.com",
        "threads.net" to "threads.net",
        "instagram.com" to "instagram.com",
        "reddit.com" to "reddit.com",
        "acfun.cn" to "acfun.cn",
        "music.163.com" to "music.163.com",
        "linux.do" to "linux.do",
    )

    fun detectFirstSocialLink(rawText: String?): DetectedSocialLink? {
        val text = rawText?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        for (match in urlRegex.findAll(text)) {
            val url = match.value.trimEnd('.', ',', ';', ')', ']', '>', '!', '?')
            val host = url
                .substringAfter("://", "")
                .substringBefore('/')
                .substringBefore('?')
                .substringBefore('#')
                .substringBefore(':')
                .lowercase()
                .removePrefix("www.")
                .removePrefix("m.")
            val matchedLabel = supportedHosts.entries.firstOrNull { (domain, _) ->
                host == domain || host.endsWith(".$domain")
            }?.value
            if (matchedLabel != null) {
                return DetectedSocialLink(
                    url = url,
                    hostLabel = matchedLabel,
                    rawText = text,
                )
            }
        }
        return null
    }
}

/**
 * Pure ARGB `IntArray` image masking engine supporting irreversible mosaic resampling,
 * 3-pass separable box blur, and opaque blackout for rectangular boxes and freehand strokes.
 */
object MaskPixelEngine {

    private const val BLACKOUT_ARGB = 0xFF111111.toInt()

    fun applyMaskOperations(
        sourcePixels: IntArray,
        width: Int,
        height: Int,
        ops: List<MaskOperation>,
    ): IntArray {
        if (width <= 0 || height <= 0 || sourcePixels.size < width * height || ops.isEmpty()) {
            return sourcePixels.copyOf()
        }
        val out = sourcePixels.copyOf()
        for (op in ops) {
            when (op) {
                is MaskOperation.BoxOp -> applyBoxOp(out, width, height, op)
                is MaskOperation.StrokeOp -> applyStrokeOp(out, width, height, op)
            }
        }
        return out
    }

    private fun applyBoxOp(
        pixels: IntArray,
        width: Int,
        height: Int,
        op: MaskOperation.BoxOp,
    ) {
        val x0 = (minOf(op.left, op.right) * width).roundToInt().coerceIn(0, width)
        val y0 = (minOf(op.top, op.bottom) * height).roundToInt().coerceIn(0, height)
        val x1 = (maxOf(op.left, op.right) * width).roundToInt().coerceIn(0, width)
        val y1 = (maxOf(op.top, op.bottom) * height).roundToInt().coerceIn(0, height)
        if (x1 <= x0 || y1 <= y0) return

        when (op.style) {
            MaskBrushStyle.BLACKOUT -> {
                for (y in y0 until y1) {
                    val row = y * width
                    for (x in x0 until x1) {
                        pixels[row + x] = BLACKOUT_ARGB
                    }
                }
            }
            MaskBrushStyle.MOSAIC -> {
                applyMosaicRect(pixels, width, height, x0, y0, x1, y1)
            }
            MaskBrushStyle.BLUR -> {
                applyBlurRect(pixels, width, height, x0, y0, x1, y1)
            }
        }
    }

    private fun applyStrokeOp(
        pixels: IntArray,
        width: Int,
        height: Int,
        op: MaskOperation.StrokeOp,
    ) {
        if (op.points.isEmpty()) return
        val radiusPx = (op.radiusNorm * minOf(width, height)).coerceAtLeast(6f)
        val radiusSq = radiusPx * radiusPx
        val pad = ceil(radiusPx).toInt()

        val ptsX = FloatArray(op.points.size) { i -> op.points[i].x.coerceIn(0f, 1f) * width }
        val ptsY = FloatArray(op.points.size) { i -> op.points[i].y.coerceIn(0f, 1f) * height }

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (i in ptsX.indices) {
            if (ptsX[i] < minX) minX = ptsX[i]
            if (ptsY[i] < minY) minY = ptsY[i]
            if (ptsX[i] > maxX) maxX = ptsX[i]
            if (ptsY[i] > maxY) maxY = ptsY[i]
        }

        val x0 = (minX.toInt() - pad).coerceIn(0, width)
        val y0 = (minY.toInt() - pad).coerceIn(0, height)
        val x1 = (ceil(maxX).toInt() + pad + 1).coerceIn(0, width)
        val y1 = (ceil(maxY).toInt() + pad + 1).coerceIn(0, height)
        if (x1 <= x0 || y1 <= y0) return

        val effectBuffer: IntArray? = when (op.style) {
            MaskBrushStyle.BLACKOUT -> null
            MaskBrushStyle.MOSAIC -> {
                pixels.copyOf().also { buf ->
                    applyMosaicRect(buf, width, height, x0, y0, x1, y1)
                }
            }
            MaskBrushStyle.BLUR -> {
                pixels.copyOf().also { buf ->
                    applyBlurRect(buf, width, height, x0, y0, x1, y1)
                }
            }
        }

        for (py in y0 until y1) {
            val cy = py + 0.5f
            val row = py * width
            for (px in x0 until x1) {
                val cx = px + 0.5f
                if (isNearPolyline(cx, cy, ptsX, ptsY, radiusSq)) {
                    pixels[row + px] = when (op.style) {
                        MaskBrushStyle.BLACKOUT -> BLACKOUT_ARGB
                        MaskBrushStyle.MOSAIC, MaskBrushStyle.BLUR -> effectBuffer!![row + px]
                    }
                }
            }
        }
    }

    private fun applyMosaicRect(
        pixels: IntArray,
        width: Int,
        height: Int,
        x0: Int,
        y0: Int,
        x1: Int,
        y1: Int,
    ) {
        val boxW = x1 - x0
        val boxH = y1 - y0
        val cell = maxOf(16, maxOf(width, height) / 18, minOf(boxW, boxH) / 3).coerceAtLeast(1)

        var by = y0
        while (by < y1) {
            val nextY = minOf(by + cell, y1)
            var bx = x0
            while (bx < x1) {
                val nextX = minOf(bx + cell, x1)
                var sumA = 0L
                var sumR = 0L
                var sumG = 0L
                var sumB = 0L
                var count = 0L
                for (y in by until nextY) {
                    val row = y * width
                    for (x in bx until nextX) {
                        val c = pixels[row + x]
                        sumA += (c ushr 24) and 0xFF
                        sumR += (c ushr 16) and 0xFF
                        sumG += (c ushr 8) and 0xFF
                        sumB += c and 0xFF
                        count++
                    }
                }
                if (count > 0L) {
                    val avg = ((sumA / count).toInt() shl 24) or
                        ((sumR / count).toInt() shl 16) or
                        ((sumG / count).toInt() shl 8) or
                        (sumB / count).toInt()
                    for (y in by until nextY) {
                        val row = y * width
                        for (x in bx until nextX) {
                            pixels[row + x] = avg
                        }
                    }
                }
                bx = nextX
            }
            by = nextY
        }
    }

    private fun applyBlurRect(
        pixels: IntArray,
        width: Int,
        height: Int,
        x0: Int,
        y0: Int,
        x1: Int,
        y1: Int,
    ) {
        val radius = maxOf(12, minOf(width, height) / 24)
        val pad = minOf(radius * 2, maxOf(width, height))
        val sx0 = (x0 - pad).coerceAtLeast(0)
        val sy0 = (y0 - pad).coerceAtLeast(0)
        val sx1 = (x1 + pad).coerceAtMost(width)
        val sy1 = (y1 + pad).coerceAtMost(height)
        val roiW = sx1 - sx0
        val roiH = sy1 - sy0
        if (roiW <= 0 || roiH <= 0) return

        var src = IntArray(roiW * roiH)
        var dst = IntArray(roiW * roiH)
        for (ry in 0 until roiH) {
            val srcRow = (sy0 + ry) * width + sx0
            val roiRow = ry * roiW
            for (rx in 0 until roiW) {
                src[roiRow + rx] = pixels[srcRow + rx]
            }
        }

        val win = (radius * 2 + 1).coerceAtLeast(1)
        repeat(3) {
            // Horizontal pass (sliding window with edge clamp)
            for (ry in 0 until roiH) {
                val rowOff = ry * roiW
                var sumA = 0L
                var sumR = 0L
                var sumG = 0L
                var sumB = 0L
                for (dx in -radius..radius) {
                    val sampleX = dx.coerceIn(0, roiW - 1)
                    val c = src[rowOff + sampleX]
                    sumA += (c ushr 24) and 0xFF
                    sumR += (c ushr 16) and 0xFF
                    sumG += (c ushr 8) and 0xFF
                    sumB += c and 0xFF
                }
                for (rx in 0 until roiW) {
                    dst[rowOff + rx] = ((sumA / win).toInt() shl 24) or
                        ((sumR / win).toInt() shl 16) or
                        ((sumG / win).toInt() shl 8) or
                        (sumB / win).toInt()
                    val outX = (rx - radius).coerceIn(0, roiW - 1)
                    val inX = (rx + radius + 1).coerceIn(0, roiW - 1)
                    val cOut = src[rowOff + outX]
                    val cIn = src[rowOff + inX]
                    sumA += ((cIn ushr 24) and 0xFF) - ((cOut ushr 24) and 0xFF)
                    sumR += ((cIn ushr 16) and 0xFF) - ((cOut ushr 16) and 0xFF)
                    sumG += ((cIn ushr 8) and 0xFF) - ((cOut ushr 8) and 0xFF)
                    sumB += (cIn and 0xFF) - (cOut and 0xFF)
                }
            }
            // Vertical pass (sliding window with edge clamp)
            for (rx in 0 until roiW) {
                var sumA = 0L
                var sumR = 0L
                var sumG = 0L
                var sumB = 0L
                for (dy in -radius..radius) {
                    val sampleY = dy.coerceIn(0, roiH - 1)
                    val c = dst[sampleY * roiW + rx]
                    sumA += (c ushr 24) and 0xFF
                    sumR += (c ushr 16) and 0xFF
                    sumG += (c ushr 8) and 0xFF
                    sumB += c and 0xFF
                }
                for (ry in 0 until roiH) {
                    src[ry * roiW + rx] = ((sumA / win).toInt() shl 24) or
                        ((sumR / win).toInt() shl 16) or
                        ((sumG / win).toInt() shl 8) or
                        (sumB / win).toInt()
                    val outY = (ry - radius).coerceIn(0, roiH - 1)
                    val inY = (ry + radius + 1).coerceIn(0, roiH - 1)
                    val cOut = dst[outY * roiW + rx]
                    val cIn = dst[inY * roiW + rx]
                    sumA += ((cIn ushr 24) and 0xFF) - ((cOut ushr 24) and 0xFF)
                    sumR += ((cIn ushr 16) and 0xFF) - ((cOut ushr 16) and 0xFF)
                    sumG += ((cIn ushr 8) and 0xFF) - ((cOut ushr 8) and 0xFF)
                    sumB += (cIn and 0xFF) - (cOut and 0xFF)
                }
            }
        }

        // Write back strictly inside target box [x0, x1) × [y0, y1)
        for (y in y0 until y1) {
            val outRow = y * width
            val roiRow = (y - sy0) * roiW
            for (x in x0 until x1) {
                pixels[outRow + x] = src[roiRow + (x - sx0)]
            }
        }
    }

    private fun isNearPolyline(
        px: Float,
        py: Float,
        ptsX: FloatArray,
        ptsY: FloatArray,
        radiusSq: Float,
    ): Boolean {
        if (ptsX.size == 1) {
            val dx = px - ptsX[0]
            val dy = py - ptsY[0]
            return dx * dx + dy * dy <= radiusSq
        }
        for (i in 0 until ptsX.lastIndex) {
            val ax = ptsX[i]
            val ay = ptsY[i]
            val bx = ptsX[i + 1]
            val by = ptsY[i + 1]
            val vx = bx - ax
            val vy = by - ay
            val lenSq = vx * vx + vy * vy
            val t = if (lenSq <= 1e-6f) 0f else (((px - ax) * vx + (py - ay) * vy) / lenSq).coerceIn(0f, 1f)
            val projX = ax + t * vx
            val projY = ay + t * vy
            val dx = px - projX
            val dy = py - projY
            if (dx * dx + dy * dy <= radiusSq) return true
        }
        return false
    }
}
