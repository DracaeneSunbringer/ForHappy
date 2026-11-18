package com.jinhuan.forhappy.core.chat

import android.graphics.Bitmap
import android.os.Build
import java.io.ByteArrayOutputStream
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Pure Kotlin + Android helper for 9-grid irreversible clue mosaics (`makePacketClue` parity),
 * 6 native cipher game encoders/previews, and external red-packet URL/token recognition.
 */
object RedpackMosaicEngine {
    const val MAX_UPLOAD_BYTES = 3 * 1024 * 1024
    const val DEFAULT_MAX_EDGE = 1200
    const val DEFAULT_WEBP_QUALITY = 82
    const val ALL_NINE_BITS = 0x1FF

    enum class CipherGameSpec(
        val key: String,
        val label: String,
        val shortHint: String,
    ) {
        RAILS("rails", "栅栏密码", "按上下 Z 字排列拆行后竖读还原"),
        STAIRS("stairs", "楼梯偏移", "每字按步长循环移位"),
        RADIO("radio", "电波摩斯", "点划节奏还原汉字或字母"),
        REVERSE("reverse", "镜面反转", "字序首尾颠倒"),
        PAIRS("pairs", "两两交换", "相邻两个字符两两互换位置"),
        TILES("tiles", "乱序字块", "点击打乱的字块拼回正确顺序"),
        ;

        companion object {
            fun fromKey(key: String?): CipherGameSpec =
                entries.firstOrNull { it.key == key } ?: RAILS
        }
    }

    enum class ExternalPacketPlatform(
        val key: String,
        val label: String,
        val createUrl: String,
        val homeUrl: String,
    ) {
        HB("hb", "HB 口令红包", "https://hb.unsnow.org/", "https://hb.unsnow.org/"),
        FISH("fish", "摸鱼派", "https://hb.lafi.sh/", "https://hb.lafi.sh/"),
        CREDIT("credit", "LinuxDo Credit", "https://credit.linux.do/", "https://credit.linux.do/"),
        ;

        companion object {
            fun fromKey(key: String?): ExternalPacketPlatform = when (key?.lowercase()) {
                "hb" -> HB
                "fish" -> FISH
                "credit", "ldc" -> CREDIT
                else -> HB
            }
        }
    }

    data class ExternalPacketMatch(
        val platform: ExternalPacketPlatform,
        val urlOrCode: String,
    )

    private val URL_REGEX = Regex("""https?://[^\s<>"')\]]+""", RegexOption.IGNORE_CASE)

    /**
     * Recognizes `hb.unsnow.org`, `credit.linux.do`, and `hb.lafi.sh` red-packet links or share codes.
     */
    fun detectExternalPacketLink(raw: String): ExternalPacketMatch? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        for (match in URL_REGEX.findAll(trimmed)) {
            val url = match.value
            val lower = url.lowercase()
            when {
                "hb.unsnow.org" in lower -> return ExternalPacketMatch(ExternalPacketPlatform.HB, url)
                "hb.lafi.sh" in lower -> return ExternalPacketMatch(ExternalPacketPlatform.FISH, url)
                "credit.linux.do" in lower -> return ExternalPacketMatch(ExternalPacketPlatform.CREDIT, url)
            }
        }
        return null
    }

    /**
     * Generates a readable client-side preview for the 6 supported native cipher games so the
     * sender can verify what players see before submitting to `/api/chat/groups/{gid}/redpacks`.
     */
    fun previewCipherText(secret: String, gameKey: String): String {
        val clean = secret.trim()
        if (clean.isEmpty()) return ""
        val chars = clean.toList()
        return when (CipherGameSpec.fromKey(gameKey)) {
            CipherGameSpec.REVERSE -> chars.reversed().joinToString("")
            CipherGameSpec.PAIRS -> buildString {
                var i = 0
                while (i < chars.size) {
                    if (i + 1 < chars.size) {
                        append(chars[i + 1])
                        append(chars[i])
                    } else {
                        append(chars[i])
                    }
                    i += 2
                }
            }
            CipherGameSpec.RAILS -> {
                val top = StringBuilder()
                val bottom = StringBuilder()
                chars.forEachIndexed { idx, ch ->
                    if (idx % 2 == 0) top.append(ch) else bottom.append(ch)
                }
                "$top$bottom"
            }
            CipherGameSpec.STAIRS -> chars.mapIndexed { idx, ch ->
                if (ch.code in 0x4E00..0x9FA5) ch else ((ch.code + idx + 1).toChar())
            }.joinToString("")
            CipherGameSpec.RADIO -> chars.joinToString(" / ") { ch ->
                val code = ch.code
                buildString {
                    for (bit in 3 downTo 0) {
                        append(if (((code shr bit) and 1) == 1) "—" else "·")
                    }
                }
            }
            CipherGameSpec.TILES -> chars.sortedBy { (it.code * 31) % 97 }.joinToString(" · ")
        }
    }

    /**
     * Pure pixel implementation matching `52dmeme.com`'s `makePacketClue(img, bits)`:
     * 1. Downsample source pixels to coarse grid `max(1, ceil(w / 40.0)) x max(1, ceil(h / 40.0))`
     * 2. Nearest-neighbor upscale back to full `(width, height)`
     * 3. Overwrite any 3x3 cell `i in 0..8` whose bit `(maskBits and (1 shl i)) != 0` with original sharp pixels.
     *
     * Cells with bit == 0 lose high-frequency detail irreversibly before upload.
     */
    fun synthesizeNineGridCluePixels(
        srcPixels: IntArray,
        width: Int,
        height: Int,
        maskBits: Int,
    ): IntArray {
        require(width > 0 && height > 0) { "width and height must be positive" }
        require(srcPixels.size == width * height) { "srcPixels size mismatch" }
        val effectiveBits = maskBits and ALL_NINE_BITS
        require(effectiveBits != 0) { "maskBits must expose at least 1 cell (1..511)" }

        val gridW = max(1, ceil(width / 40.0).toInt())
        val gridH = max(1, ceil(height / 40.0).toInt())
        val downsampled = IntArray(gridW * gridH)

        // Box-average downsampling per coarse cell for authentic blocky mosaic colors.
        for (gy in 0 until gridH) {
            val yStart = (gy * height) / gridH
            val yEnd = max(yStart + 1, ((gy + 1) * height) / gridH)
            for (gx in 0 until gridW) {
                val xStart = (gx * width) / gridW
                val xEnd = max(xStart + 1, ((gx + 1) * width) / gridW)
                var sumA = 0L
                var sumR = 0L
                var sumG = 0L
                var sumB = 0L
                var count = 0L
                for (y in yStart until yEnd) {
                    val rowOffset = y * width
                    for (x in xStart until xEnd) {
                        val px = srcPixels[rowOffset + x]
                        sumA += (px ushr 24) and 0xFF
                        sumR += (px ushr 16) and 0xFF
                        sumG += (px ushr 8) and 0xFF
                        sumB += px and 0xFF
                        count++
                    }
                }
                val a = (sumA / count).toInt() and 0xFF
                val r = (sumR / count).toInt() and 0xFF
                val g = (sumG / count).toInt() and 0xFF
                val b = (sumB / count).toInt() and 0xFF
                downsampled[gy * gridW + gx] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        val out = IntArray(width * height)
        for (y in 0 until height) {
            val cellRow = ((y * 3L) / height).toInt().coerceIn(0, 2)
            val gy = minOf(gridH - 1, floor((y.toDouble() / height) * gridH).toInt())
            val rowOffset = y * width
            val gridRowOffset = gy * gridW
            for (x in 0 until width) {
                val cellCol = ((x * 3L) / width).toInt().coerceIn(0, 2)
                val cellIndex = cellRow * 3 + cellCol
                val revealOriginal = (effectiveBits and (1 shl cellIndex)) != 0
                out[rowOffset + x] = if (revealOriginal) {
                    srcPixels[rowOffset + x]
                } else {
                    val gx = minOf(gridW - 1, floor((x.toDouble() / width) * gridW).toInt())
                    downsampled[gridRowOffset + gx]
                }
            }
        }
        return out
    }

    /**
     * Android Bitmap wrapper scaling to `<= maxEdge`, synthesizing irreversible 9-grid mosaic,
     * and encoding as WebP quality 82 (reducing quality/dimensions only if > 3MB).
     */
    fun encodeNineGridClueWebp(
        source: Bitmap,
        maskBits: Int,
        maxEdge: Int = DEFAULT_MAX_EDGE,
        quality: Int = DEFAULT_WEBP_QUALITY,
    ): ByteArray {
        val effectiveBits = maskBits and ALL_NINE_BITS
        require(effectiveBits != 0) { "请至少选择 1 个清晰九宫格区域" }
        val scaled = scaleBitmapIfNeeded(source, maxEdge)
        val w = scaled.width
        val h = scaled.height
        val srcPixels = IntArray(w * h)
        scaled.getPixels(srcPixels, 0, w, 0, 0, w, h)
        val mixedPixels = synthesizeNineGridCluePixels(srcPixels, w, h, effectiveBits)
        val clueBitmap = Bitmap.createBitmap(mixedPixels, w, h, Bitmap.Config.ARGB_8888)
        return try {
            var q = quality.coerceIn(40, 95)
            var bytes = compressWebp(clueBitmap, q)
            while (bytes.size > MAX_UPLOAD_BYTES && q > 45) {
                q -= 10
                bytes = compressWebp(clueBitmap, q)
            }
            bytes
        } finally {
            if (scaled !== source) scaled.recycle()
            clueBitmap.recycle()
        }
    }

    fun renderNineGridPreviewBitmap(source: Bitmap, maskBits: Int): Bitmap {
        val previewSrc = scaleBitmapIfNeeded(source, 480)
        val w = previewSrc.width
        val h = previewSrc.height
        val pixels = IntArray(w * h)
        previewSrc.getPixels(pixels, 0, w, 0, 0, w, h)
        val mixed = if ((maskBits and ALL_NINE_BITS) != 0) {
            synthesizeNineGridCluePixels(pixels, w, h, maskBits)
        } else {
            // 0 bits -> full coarse mosaic preview so user knows they must tap >= 1 cell
            val fullMosaic = synthesizeNineGridCluePixels(pixels, w, h, 1)
            val reMasked = synthesizeNineGridCluePixels(fullMosaic, w, h, 0x1FE)
            reMasked
        }
        if (previewSrc !== source) previewSrc.recycle()
        return Bitmap.createBitmap(mixed, w, h, Bitmap.Config.ARGB_8888)
    }

    private fun scaleBitmapIfNeeded(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val maxDim = max(bitmap.width, bitmap.height)
        if (maxDim <= maxEdge) return bitmap
        val ratio = maxEdge.toFloat() / maxDim.toFloat()
        val targetW = max(1, (bitmap.width * ratio).roundToInt())
        val targetH = max(1, (bitmap.height * ratio).roundToInt())
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    @Suppress("DEPRECATION")
    private fun compressWebp(bitmap: Bitmap, quality: Int): ByteArray {
        val baos = ByteArrayOutputStream()
        val fmt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            Bitmap.CompressFormat.WEBP
        }
        bitmap.compress(fmt, quality, baos)
        return baos.toByteArray()
    }
}
