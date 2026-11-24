package com.jinhuan.forhappy.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.Base64
import android.util.Xml
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.PathParser
import coil3.ImageLoader
import coil3.compose.AsyncImage
import com.jinhuan.forhappy.core.cosmetics.BadgeAndCosmeticRegistry
import com.jinhuan.forhappy.core.cosmetics.BadgeArtKind
import com.jinhuan.forhappy.core.cosmetics.ChatBubbleCosmetics
import com.jinhuan.forhappy.core.cosmetics.CommunityTitleRenderSpec
import com.jinhuan.forhappy.core.cosmetics.ResolvedBadgeChip
import com.jinhuan.forhappy.core.cosmetics.ResolvedTitleChip
import com.jinhuan.forhappy.core.network.media.MediaUrlRewriter
import com.jinhuan.forhappy.ui.theme.LocalForHappyPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.StringReader
import java.net.URL
import java.net.URLDecoder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

val LocalCosmeticHttpClient = staticCompositionLocalOf<OkHttpClient?> { null }

data class RasterizedArtwork(
    val bitmap: ImageBitmap,
    val intrinsicWidth: Float,
    val intrinsicHeight: Float,
)

/**
 * Zero-dependency loader, disk cache & rasterizer for bundled `assets/badges/v4/atlas.webp`,
 * `assets/badges/v5/<id>.svg`, `OFFICIAL_ART` SVGs, remote SVGs/WebPs, sprite-sheet cells,
 * and `meme-hall-skins` CSS-embedded data-URI artwork.
 */
object BadgeArtworkCache {
    private val svgCache = ConcurrentHashMap<String, ImageBitmap>()
    private val artworkCache = ConcurrentHashMap<String, RasterizedArtwork>()
    private val failedUrls = ConcurrentHashMap<String, Boolean>()

    @Volatile
    private var atlasBitmap: ImageBitmap? = null
    private val atlasLock = Any()

    fun getOrLoadAtlas(context: Context): ImageBitmap? {
        atlasBitmap?.let { return it }
        synchronized(atlasLock) {
            atlasBitmap?.let { return it }
            val loaded = runCatching {
                context.assets.open("badges/v4/atlas.webp").use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }.getOrNull()
            atlasBitmap = loaded
            return loaded
        }
    }

    fun getOrRenderAssetSvg(context: Context, assetPath: String, targetPx: Int = 128): ImageBitmap? {
        val key = "asset:$assetPath:$targetPx"
        svgCache[key]?.let { return it }
        val xmlText = runCatching {
            context.assets.open(assetPath).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrNull() ?: return null
        val bmp = MiniBadgeSvgRasterizer.renderSvgXml(xmlText, targetPx) ?: return null
        svgCache[key] = bmp
        return bmp
    }

    fun getOrRenderOfficialSvg(innerSvg: String, strokeHex: String = "#FDE68A", targetPx: Int = 96): ImageBitmap? {
        val key = "official:${innerSvg.hashCode()}:$strokeHex:$targetPx"
        svgCache[key]?.let { return it }
        val fullXml = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32"><g fill="none" stroke="$strokeHex" stroke-width="2.1" stroke-linecap="round" stroke-linejoin="round">$innerSvg</g></svg>"""
        val bmp = MiniBadgeSvgRasterizer.renderSvgXml(fullXml, targetPx) ?: return null
        svgCache[key] = bmp
        return bmp
    }

    fun getOrRenderXmlSvg(cacheKey: String, xmlText: String, targetPx: Int = 128): ImageBitmap? {
        val key = "xml:$cacheKey:$targetPx"
        svgCache[key]?.let { return it }
        val bmp = MiniBadgeSvgRasterizer.renderSvgXml(xmlText, targetPx) ?: return null
        svgCache[key] = bmp
        return bmp
    }

    suspend fun getOrFetchBitmap(
        context: Context,
        okHttpClient: OkHttpClient?,
        source: String,
        reqSizePx: Int = 512,
    ): ImageBitmap? = getOrFetchNineSliceArtwork(context, okHttpClient, source, reqSizePx)?.bitmap

    suspend fun getOrFetchNineSliceArtwork(
        context: Context,
        okHttpClient: OkHttpClient?,
        source: String,
        reqSizePx: Int = 768,
    ): RasterizedArtwork? {
        val cacheKey = "$source#$reqSizePx"
        artworkCache[cacheKey]?.let { return it }
        if (failedUrls.containsKey(cacheKey)) return null

        val loaded = withContext(Dispatchers.IO) {
            if (source.startsWith("community-css-dark:") || source.startsWith("community-css:")) {
                loadCommunityCssArtwork(context, okHttpClient, source, reqSizePx)
            } else {
                val bytes = fetchBytesWithDiskCache(context, okHttpClient, source) ?: return@withContext null
                decodeBytesToArtwork(bytes, source, reqSizePx)
            }
        }
        if (loaded != null) {
            artworkCache[cacheKey] = loaded
        } else {
            failedUrls[cacheKey] = true
        }
        return loaded
    }

    suspend fun ensureCommunityCssLoaded(
        context: Context,
        okHttpClient: OkHttpClient?,
        spec: String,
    ) {
        val parts = spec.split(":")
        if (parts.size < 3) return
        val id = parts[0]
        val kind = parts[1]
        val sha = parts[2]
        withContext(Dispatchers.IO) {
            val cssUrl = "https://52dmeme.com/api/community-skins/css/$id/$kind/$sha"
            val cssBytes = fetchBytesWithDiskCache(context, okHttpClient, cssUrl) ?: return@withContext
            ChatBubbleCosmetics.registerParsedCommunityCss(kind, sha, cssBytes.toString(Charsets.UTF_8))
        }
    }

    suspend fun getOrCropSpriteCell(
        context: Context,
        okHttpClient: OkHttpClient?,
        source: String,
        cols: Int,
        rows: Int,
        col: Int,
        row: Int,
        reqSizePx: Int = 2048,
    ): ImageBitmap? {
        val key = "sprite:$source:${cols}x$rows:$col:$row"
        svgCache[key]?.let { return it }
        val sheet = getOrFetchBitmap(context, okHttpClient, source, reqSizePx) ?: return null
        val cropped = withContext(Dispatchers.Default) {
            runCatching {
                val androidBmp = sheet.asAndroidBitmap()
                val w = androidBmp.width
                val h = androidBmp.height
                val safeCols = max(1, cols)
                val safeRows = max(1, rows)
                val cellW = max(1, w / safeCols)
                val cellH = max(1, h / safeRows)
                val sx = (col.coerceIn(0, safeCols - 1) * cellW).coerceIn(0, max(0, w - 1))
                val sy = (row.coerceIn(0, safeRows - 1) * cellH).coerceIn(0, max(0, h - 1))
                val cw = min(cellW, w - sx).coerceAtLeast(1)
                val ch = min(cellH, h - sy).coerceAtLeast(1)
                Bitmap.createBitmap(androidBmp, sx, sy, cw, ch).asImageBitmap()
            }.getOrNull()
        }
        if (cropped != null) {
            svgCache[key] = cropped
        }
        return cropped
    }

    private fun loadCommunityCssArtwork(
        context: Context,
        okHttpClient: OkHttpClient?,
        source: String,
        reqSizePx: Int,
    ): RasterizedArtwork? {
        val isDark = source.startsWith("community-css-dark:")
        val rest = source.removePrefix("community-css-dark:").removePrefix("community-css:")
        val parts = rest.split(":")
        if (parts.size < 3) return null
        val id = parts[0]
        val kind = parts[1]
        val sha = parts[2]
        val explicitIdx = parts.getOrNull(3)?.toIntOrNull()
        val cssUrl = "https://52dmeme.com/api/community-skins/css/$id/$kind/$sha"
        val cssBytes = fetchBytesWithDiskCache(context, okHttpClient, cssUrl) ?: return null
        val cssText = cssBytes.toString(Charsets.UTF_8)
        ChatBubbleCosmetics.registerParsedCommunityCss(kind, sha, cssText)
        val imageUrls = ChatBubbleCosmetics.extractCssImageUrls(cssText, preferDark = isDark)
        val pickedUrl = when {
            explicitIdx != null -> {
                (imageUrls.getOrNull(explicitIdx)
                    ?: if (isDark) ChatBubbleCosmetics.extractCssImageUrls(cssText, preferDark = false).getOrNull(explicitIdx) else null)
                    ?: return null
            }
            imageUrls.isEmpty() -> return null
            isDark -> imageUrls.last()
            else -> imageUrls.first()
        }
        return if (pickedUrl.startsWith("data:image/", ignoreCase = true)) {
            decodeDataUriArtwork(pickedUrl, reqSizePx)
        } else {
            val fullUrl = if (pickedUrl.startsWith("/")) "https://52dmeme.com$pickedUrl" else pickedUrl
            val bytes = fetchBytesWithDiskCache(context, okHttpClient, fullUrl) ?: return null
            decodeBytesToArtwork(bytes, fullUrl, reqSizePx)
        }
    }

    private fun decodeDataUriArtwork(dataUri: String, reqSizePx: Int): RasterizedArtwork? {
        val commaIdx = dataUri.indexOf(',')
        if (commaIdx <= 0) return null
        val header = dataUri.substring(0, commaIdx).lowercase()
        val payload = dataUri.substring(commaIdx + 1)
        return if (header.contains("image/svg+xml")) {
            val svgXml = if (header.contains(";base64")) {
                String(Base64.decode(payload, Base64.DEFAULT), Charsets.UTF_8)
            } else {
                URLDecoder.decode(payload.replace("+", "%2B"), "UTF-8")
            }
            MiniBadgeSvgRasterizer.renderSvgDetailed(svgXml, min(reqSizePx, 1024))
        } else if (header.contains(";base64")) {
            val bytes = runCatching { Base64.decode(payload, Base64.DEFAULT) }.getOrNull() ?: return null
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() ?: return null
            RasterizedArtwork(bmp, bmp.width.toFloat(), bmp.height.toFloat())
        } else {
            null
        }
    }

    private fun decodeBytesToArtwork(bytes: ByteArray, source: String, reqSizePx: Int): RasterizedArtwork? {
        val cleanUrl = source.substringBefore('?').lowercase()
        val headText = bytes.take(min(bytes.size, 256)).toByteArray().toString(Charsets.UTF_8).trimStart()
        if (cleanUrl.endsWith(".svg") || headText.startsWith("<svg", ignoreCase = true) ||
            (headText.startsWith("<?xml", ignoreCase = true) && headText.contains("<svg", ignoreCase = true))
        ) {
            return MiniBadgeSvgRasterizer.renderSvgDetailed(bytes.toString(Charsets.UTF_8), min(reqSizePx, 1024))
        }
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() ?: return null
        return RasterizedArtwork(bmp, bmp.width.toFloat(), bmp.height.toFloat())
    }

    private fun fetchBytesWithDiskCache(
        context: Context,
        okHttpClient: OkHttpClient?,
        rawUrl: String,
    ): ByteArray? {
        val dir = File(context.cacheDir, "forhappy_cosmetic_cache").apply { mkdirs() }
        val file = File(dir, sha256Hex(rawUrl))
        if (file.exists() && file.length() > 0L) {
            val cached = runCatching { file.readBytes() }.getOrNull()
            if (cached != null && cached.isNotEmpty()) return cached
        }
        val reqUrl = MediaUrlRewriter.rewriteMediaUrl(rawUrl)
        val downloaded = runCatching {
            if (okHttpClient != null) {
                val req = Request.Builder().url(reqUrl).get().build()
                okHttpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) resp.body?.bytes() else null
                }
            } else {
                URL(reqUrl).openStream().use { it.readBytes() }
            }
        }.getOrNull()
        if (downloaded != null && downloaded.isNotEmpty()) {
            runCatching { file.writeBytes(downloaded) }
        }
        return downloaded
    }

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}

internal object MiniBadgeSvgRasterizer {

    private data class GradStop(val offset: Float, val color: Int)

    private sealed interface SvgGradDef {
        data class Linear(
            val x1: Float,
            val y1: Float,
            val x2: Float,
            val y2: Float,
            val stops: List<GradStop>,
        ) : SvgGradDef

        data class Radial(
            val cx: Float,
            val cy: Float,
            val r: Float,
            val stops: List<GradStop>,
        ) : SvgGradDef
    }

    private data class StyleFrame(
        val fill: String?,
        val stroke: String?,
        val strokeWidth: Float,
        val strokeCap: Paint.Cap,
        val strokeJoin: Paint.Join,
        val strokeDashArray: FloatArray? = null,
        val opacity: Float,
        val fillOpacity: Float,
        val strokeOpacity: Float,
        val hasTransform: Boolean,
    )

    private val NAMED_COLORS = mapOf(
        "white" to AndroidColor.WHITE,
        "black" to AndroidColor.BLACK,
        "transparent" to AndroidColor.TRANSPARENT,
    )

    private val TRANSFORM_FN_RE = Regex("""(translate|rotate|scale)\(\s*([^)]+)\)""")
    private val RGB_RE = Regex("""rgba?\(\s*(\d+(?:\.\d+)?)\s*,\s*(\d+(?:\.\d+)?)\s*,\s*(\d+(?:\.\d+)?)(?:\s*,\s*(\d+(?:\.\d+)?))?\s*\)""")

    fun renderSvgXml(xmlText: String, sizePx: Int): ImageBitmap? =
        renderSvgDetailed(xmlText, sizePx)?.bitmap

    fun renderSvgDetailed(xmlText: String, maxDimPx: Int): RasterizedArtwork? = runCatching {
        val probe = Xml.newPullParser()
        probe.setInput(StringReader(xmlText))
        var minX = 0f
        var minY = 0f
        var viewW = 64f
        var viewH = 64f
        var evProbe = probe.eventType
        while (evProbe != XmlPullParser.END_DOCUMENT) {
            if (evProbe == XmlPullParser.START_TAG && probe.name == "svg") {
                val vb = probe.getAttributeValue(null, "viewBox")
                if (!vb.isNullOrBlank()) {
                    val nums = vb.trim().split(Regex("""[\s,]+""")).mapNotNull { it.toFloatOrNull() }
                    if (nums.size >= 4) {
                        minX = nums[0]
                        minY = nums[1]
                        viewW = max(1f, nums[2])
                        viewH = max(1f, nums[3])
                    }
                } else {
                    val wAttr = probe.getAttributeValue(null, "width")?.removeSuffix("px")?.trim()?.toFloatOrNull()
                    val hAttr = probe.getAttributeValue(null, "height")?.removeSuffix("px")?.trim()?.toFloatOrNull()
                    if (wAttr != null && hAttr != null && wAttr > 0f && hAttr > 0f) {
                        viewW = wAttr
                        viewH = hAttr
                    }
                }
                break
            }
            evProbe = probe.next()
        }

        val scale = maxDimPx.toFloat() / max(viewW, viewH)
        val bmpW = max(1, (viewW * scale).roundToInt())
        val bmpH = max(1, (viewH * scale).roundToInt())
        val bitmap = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(xmlText))

        val grads = HashMap<String, SvgGradDef>()
        var activeGradId: String? = null
        var activeGradKind: String? = null
        var gx1 = 0f
        var gy1 = 0f
        var gx2 = 1f
        var gy2 = 0f
        var gcx = 0.5f
        var gcy = 0.5f
        var gr = 0.5f
        val activeStops = ArrayList<GradStop>()

        val stack = ArrayDeque<StyleFrame>()
        stack.addLast(
            StyleFrame(
                fill = "#000000",
                stroke = null,
                strokeWidth = 1f,
                strokeCap = Paint.Cap.BUTT,
                strokeJoin = Paint.Join.MITER,
                opacity = 1f,
                fillOpacity = 1f,
                strokeOpacity = 1f,
                hasTransform = false,
            ),
        )

        var inDefs = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val tag = parser.name
                    when (tag) {
                        "svg" -> {
                            canvas.scale(bmpW.toFloat() / viewW, bmpH.toFloat() / viewH)
                            if (minX != 0f || minY != 0f) {
                                canvas.translate(-minX, -minY)
                            }
                        }

                        "defs" -> inDefs = true

                        "linearGradient" -> {
                            activeGradId = parser.getAttributeValue(null, "id")
                            activeGradKind = "linear"
                            gx1 = parser.floatAttr("x1", 0f)
                            gy1 = parser.floatAttr("y1", 0f)
                            gx2 = parser.floatAttr("x2", 1f)
                            gy2 = parser.floatAttr("y2", 0f)
                            activeStops.clear()
                        }

                        "radialGradient" -> {
                            activeGradId = parser.getAttributeValue(null, "id")
                            activeGradKind = "radial"
                            gcx = parser.floatAttr("cx", 0.5f)
                            gcy = parser.floatAttr("cy", 0.5f)
                            gr = parser.floatAttr("r", 0.5f)
                            activeStops.clear()
                        }

                        "stop" -> {
                            val rawOff = parser.getAttributeValue(null, "offset")?.trim().orEmpty()
                            val off = if (rawOff.endsWith("%")) {
                                (rawOff.removeSuffix("%").toFloatOrNull() ?: 0f) / 100f
                            } else {
                                rawOff.toFloatOrNull() ?: if (activeStops.isEmpty()) 0f else 1f
                            }
                            var sc = parseSvgColor(parser.getAttributeValue(null, "stop-color")) ?: AndroidColor.WHITE
                            val stopOpacity = parser.getAttributeValue(null, "stop-opacity")?.toFloatOrNull()
                            if (stopOpacity != null) {
                                val baseA = AndroidColor.alpha(sc)
                                val combinedA = (baseA * stopOpacity.coerceIn(0f, 1f)).roundToInt().coerceIn(0, 255)
                                sc = AndroidColor.argb(combinedA, AndroidColor.red(sc), AndroidColor.green(sc), AndroidColor.blue(sc))
                            }
                            activeStops.add(GradStop(off.coerceIn(0f, 1f), sc))
                        }

                        "g" -> {
                            val cur = stack.last()
                            val tf = parser.getAttributeValue(null, "transform")
                            val pushedTf = !tf.isNullOrBlank()
                            if (pushedTf) {
                                canvas.save()
                                applyTransform(canvas, tf!!)
                            }
                            stack.addLast(mergeStyle(cur, parser, pushedTf))
                        }

                        "path", "circle", "ellipse", "rect", "polygon", "polyline", "line" -> if (!inDefs) {
                            val cur = stack.last()
                            val tf = parser.getAttributeValue(null, "transform")
                            if (!tf.isNullOrBlank()) {
                                canvas.save()
                                applyTransform(canvas, tf)
                            }
                            val st = mergeStyle(cur, parser, false)
                            val bounds = RectF()
                            val path = when (tag) {
                                "path" -> {
                                    val d = parser.getAttributeValue(null, "d")
                                    if (!d.isNullOrBlank()) runCatching { PathParser.createPathFromPathData(d) }.getOrNull() else null
                                }
                                "circle" -> {
                                    val cx = parser.floatAttr("cx", 0f)
                                    val cy = parser.floatAttr("cy", 0f)
                                    val r = parser.floatAttr("r", 0f)
                                    android.graphics.Path().apply { addCircle(cx, cy, r, android.graphics.Path.Direction.CW) }
                                }
                                "ellipse" -> {
                                    val cx = parser.floatAttr("cx", 0f)
                                    val cy = parser.floatAttr("cy", 0f)
                                    val rx = parser.floatAttr("rx", 0f)
                                    val ry = parser.floatAttr("ry", 0f)
                                    android.graphics.Path().apply {
                                        addOval(RectF(cx - rx, cy - ry, cx + rx, cy + ry), android.graphics.Path.Direction.CW)
                                    }
                                }
                                "rect" -> {
                                    val x = parser.floatAttr("x", 0f)
                                    val y = parser.floatAttr("y", 0f)
                                    val w = parser.floatAttr("width", 0f)
                                    val h = parser.floatAttr("height", 0f)
                                    val rx = parser.floatAttr("rx", 0f)
                                    val ry = parser.floatAttr("ry", rx)
                                    android.graphics.Path().apply {
                                        addRoundRect(RectF(x, y, x + w, y + h), rx, ry, android.graphics.Path.Direction.CW)
                                    }
                                }
                                "polygon", "polyline" -> {
                                    val ptsRaw = parser.getAttributeValue(null, "points").orEmpty()
                                    val coords = ptsRaw.trim().split(Regex("""[\s,]+""")).mapNotNull { it.toFloatOrNull() }
                                    if (coords.size >= 4) {
                                        android.graphics.Path().apply {
                                            moveTo(coords[0], coords[1])
                                            var i = 2
                                            while (i + 1 < coords.size) {
                                                lineTo(coords[i], coords[i + 1])
                                                i += 2
                                            }
                                            if (tag == "polygon") close()
                                        }
                                    } else {
                                        null
                                    }
                                }
                                "line" -> {
                                    val x1 = parser.floatAttr("x1", 0f)
                                    val y1 = parser.floatAttr("y1", 0f)
                                    val x2 = parser.floatAttr("x2", 0f)
                                    val y2 = parser.floatAttr("y2", 0f)
                                    android.graphics.Path().apply {
                                        moveTo(x1, y1)
                                        lineTo(x2, y2)
                                    }
                                }
                                else -> null
                            }
                            if (path != null) {
                                path.computeBounds(bounds, true)
                                drawStyledPath(canvas, path, bounds, st, grads)
                            }
                            if (!tf.isNullOrBlank()) {
                                canvas.restore()
                            }
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "defs" -> inDefs = false
                        "linearGradient" -> {
                            val id = activeGradId
                            if (!id.isNullOrBlank() && activeStops.isNotEmpty()) {
                                grads[id] = SvgGradDef.Linear(gx1, gy1, gx2, gy2, activeStops.toList())
                            }
                            activeGradId = null
                            activeGradKind = null
                        }
                        "radialGradient" -> {
                            val id = activeGradId
                            if (!id.isNullOrBlank() && activeStops.isNotEmpty()) {
                                grads[id] = SvgGradDef.Radial(gcx, gcy, gr, activeStops.toList())
                            }
                            activeGradId = null
                            activeGradKind = null
                        }
                        "g" -> if (stack.size > 1) {
                            val popped = stack.removeLast()
                            if (popped.hasTransform) canvas.restore()
                        }
                    }
                }
            }
            event = parser.next()
        }
        RasterizedArtwork(bitmap.asImageBitmap(), viewW, viewH)
    }.getOrNull()

    private fun XmlPullParser.floatAttr(name: String, default: Float): Float {
        val v = getAttributeValue(null, name)?.trim() ?: return default
        if (v.endsWith("%")) {
            return (v.removeSuffix("%").toFloatOrNull() ?: (default * 100f)) / 100f
        }
        return v.toFloatOrNull() ?: default
    }

    private fun mergeStyle(parent: StyleFrame, parser: XmlPullParser, hasTf: Boolean): StyleFrame {
        val fill = parser.getAttributeValue(null, "fill") ?: parent.fill
        val stroke = parser.getAttributeValue(null, "stroke") ?: parent.stroke
        val sw = parser.getAttributeValue(null, "stroke-width")?.toFloatOrNull() ?: parent.strokeWidth
        val cap = when (parser.getAttributeValue(null, "stroke-linecap")) {
            "round" -> Paint.Cap.ROUND
            "square" -> Paint.Cap.SQUARE
            "butt" -> Paint.Cap.BUTT
            else -> parent.strokeCap
        }
        val join = when (parser.getAttributeValue(null, "stroke-linejoin")) {
            "round" -> Paint.Join.ROUND
            "bevel" -> Paint.Join.BEVEL
            "miter" -> Paint.Join.MITER
            else -> parent.strokeJoin
        }
        val rawDash = parser.getAttributeValue(null, "stroke-dasharray")?.trim()
        val dash = when {
            rawDash == null -> parent.strokeDashArray
            rawDash.equals("none", ignoreCase = true) || rawDash.isEmpty() -> null
            else -> rawDash.split(Regex("""[\s,]+""")).mapNotNull { it.toFloatOrNull() }
                .takeIf { it.isNotEmpty() }
                ?.toFloatArray()
                ?: parent.strokeDashArray
        }
        val op = parser.getAttributeValue(null, "opacity")?.toFloatOrNull() ?: 1f
        val fillOp = parser.getAttributeValue(null, "fill-opacity")?.toFloatOrNull() ?: parent.fillOpacity
        val strokeOp = parser.getAttributeValue(null, "stroke-opacity")?.toFloatOrNull() ?: parent.strokeOpacity
        return StyleFrame(
            fill = fill,
            stroke = stroke,
            strokeWidth = sw,
            strokeCap = cap,
            strokeJoin = join,
            strokeDashArray = dash,
            opacity = (parent.opacity * op).coerceIn(0f, 1f),
            fillOpacity = fillOp.coerceIn(0f, 1f),
            strokeOpacity = strokeOp.coerceIn(0f, 1f),
            hasTransform = hasTf,
        )
    }

    private fun applyTransform(canvas: AndroidCanvas, raw: String) {
        for (m in TRANSFORM_FN_RE.findAll(raw)) {
            val nums = Regex("""[+-]?\d*\.?\d+""").findAll(m.groupValues[2]).mapNotNull { it.value.toFloatOrNull() }.toList()
            when (m.groupValues[1].lowercase()) {
                "translate" -> {
                    val tx = nums.getOrNull(0) ?: 0f
                    val ty = nums.getOrNull(1) ?: 0f
                    canvas.translate(tx, ty)
                }
                "rotate" -> {
                    val deg = nums.getOrNull(0) ?: 0f
                    val cx = nums.getOrNull(1)
                    val cy = nums.getOrNull(2)
                    if (cx != null && cy != null) canvas.rotate(deg, cx, cy) else canvas.rotate(deg)
                }
                "scale" -> {
                    val sx = nums.getOrNull(0) ?: 1f
                    val sy = nums.getOrNull(1) ?: sx
                    canvas.scale(sx, sy)
                }
            }
        }
    }

    private fun drawStyledPath(
        canvas: AndroidCanvas,
        path: android.graphics.Path,
        bounds: RectF,
        st: StyleFrame,
        grads: Map<String, SvgGradDef>,
    ) {
        val fillAlphaInt = (st.opacity * st.fillOpacity * 255f).roundToInt().coerceIn(0, 255)
        if (!st.fill.isNullOrBlank() && st.fill != "none" && fillAlphaInt > 0) {
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            }
            if (applyPaintColorOrShader(fillPaint, st.fill, bounds, grads, fillAlphaInt)) {
                canvas.drawPath(path, fillPaint)
            }
        }
        val strokeAlphaInt = (st.opacity * st.strokeOpacity * 255f).roundToInt().coerceIn(0, 255)
        if (!st.stroke.isNullOrBlank() && st.stroke != "none" && st.strokeWidth > 0f && strokeAlphaInt > 0) {
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = st.strokeWidth
                strokeCap = st.strokeCap
                strokeJoin = st.strokeJoin
                st.strokeDashArray?.takeIf { it.isNotEmpty() }?.let { rawArr ->
                    val arr = if (rawArr.size == 1) floatArrayOf(rawArr[0], rawArr[0]) else rawArr
                    pathEffect = DashPathEffect(arr, 0f)
                }
            }
            if (applyPaintColorOrShader(strokePaint, st.stroke, bounds, grads, strokeAlphaInt)) {
                canvas.drawPath(path, strokePaint)
            }
        }
    }

    private fun applyPaintColorOrShader(
        paint: Paint,
        spec: String,
        bounds: RectF,
        grads: Map<String, SvgGradDef>,
        alphaInt: Int,
    ): Boolean {
        val clean = spec.trim()
        if (clean.startsWith("url(#") && clean.endsWith(")")) {
            val id = clean.removePrefix("url(#").removeSuffix(")").trim()
            val def = grads[id] ?: return false
            val w = max(1f, bounds.width())
            val h = max(1f, bounds.height())
            when (def) {
                is SvgGradDef.Linear -> {
                    val colors = def.stops.map { it.color }.toIntArray()
                    val pos = def.stops.map { it.offset }.toFloatArray()
                    paint.shader = LinearGradient(
                        bounds.left + def.x1 * w,
                        bounds.top + def.y1 * h,
                        bounds.left + def.x2 * w,
                        bounds.top + def.y2 * h,
                        colors,
                        pos,
                        Shader.TileMode.CLAMP,
                    )
                }
                is SvgGradDef.Radial -> {
                    val colors = def.stops.map { it.color }.toIntArray()
                    val pos = def.stops.map { it.offset }.toFloatArray()
                    val rad = max(1f, def.r * max(w, h))
                    paint.shader = RadialGradient(
                        bounds.left + def.cx * w,
                        bounds.top + def.cy * h,
                        rad,
                        colors,
                        pos,
                        Shader.TileMode.CLAMP,
                    )
                }
            }
            paint.alpha = alphaInt
            return true
        }
        val color = parseSvgColor(clean) ?: return false
        paint.color = color
        paint.alpha = ((AndroidColor.alpha(color) / 255f) * alphaInt).roundToInt().coerceIn(0, 255)
        return true
    }

    private fun parseSvgColor(raw: String?): Int? {
        val s = raw?.trim()?.lowercase() ?: return null
        if (s.isEmpty() || s == "none") return null
        NAMED_COLORS[s]?.let { return it }
        if (s.startsWith("#")) {
            val col = ChatBubbleCosmetics.parseHexColorPure(s)
            if (col != Color.Transparent || s == "#0000" || s == "#00000000") return col.toArgb()
            return null
        }
        RGB_RE.matchEntire(s)?.let { m ->
            val r = (m.groupValues[1].toFloatOrNull() ?: 0f).roundToInt().coerceIn(0, 255)
            val g = (m.groupValues[2].toFloatOrNull() ?: 0f).roundToInt().coerceIn(0, 255)
            val b = (m.groupValues[3].toFloatOrNull() ?: 0f).roundToInt().coerceIn(0, 255)
            val aFloat = m.groupValues[4].takeIf { it.isNotBlank() }?.toFloatOrNull() ?: 1f
            val a = (aFloat * 255f).roundToInt().coerceIn(0, 255)
            return AndroidColor.argb(a, r, g, b)
        }
        return null
    }
}

private fun levelRingColors(level: Int): List<Color> = when (level.coerceIn(1, 5)) {
    1 -> listOf(Color(0xFFB87333), Color(0xFFD99B66)) // 铜
    2 -> listOf(Color(0xFF94A3B8), Color(0xFFE2E8F0)) // 银
    3 -> listOf(Color(0xFFD97706), Color(0xFFFBBF24)) // 金
    4 -> listOf(Color(0xFFEC4899), Color(0xFF8B5CF6), Color(0xFF06B6D4)) // 彩虹
    else -> listOf(Color(0xFFF59E0B), Color(0xFFFDE68A), Color(0xFFEA580C)) // 传说
}

private fun tierThemeColors(tier: String): Pair<Color, Color> = when (tier) {
    "legend" -> Color(0xFFD97706) to Color(0xFFFEF3C7)
    "epic" -> Color(0xFF7C3AED) to Color(0xFFEDE9FE)
    "fun" -> Color(0xFFDB2777) to Color(0xFFFCE7F3)
    else -> Color(0xFF2563EB) to Color(0xFFDBEAFE)
}

@Composable
fun BadgeArtIcon(
    art: BadgeArtKind,
    level: Int,
    imageLoader: ImageLoader,
    size: Dp = 24.dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val httpClient = LocalCosmeticHttpClient.current
    val ringBrush = remember(level) { Brush.linearGradient(levelRingColors(level)) }

    when (art) {
        is BadgeArtKind.BundledSvg -> {
            val isV5 = art.assetPath.startsWith("badges/v5/")
            var bmp by remember(art.assetPath) { mutableStateOf<ImageBitmap?>(null) }
            LaunchedEffect(art.assetPath) {
                bmp = withContext(Dispatchers.IO) {
                    BadgeArtworkCache.getOrRenderAssetSvg(context, art.assetPath, 128)
                }
            }
            Box(
                modifier = if (isV5) modifier.width(size * 0.912f).height(size) else modifier.size(size),
                contentAlignment = Alignment.Center,
            ) {
                bmp?.let {
                    Image(
                        bitmap = it,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (isV5 && level >= 4) {
                    V5LevelStarOverlay(level = level, modifier = Modifier.matchParentSize())
                }
            }
        }

        is BadgeArtKind.InlineOfficialSvg -> {
            val pal = ChatBubbleCosmetics.OFFICIAL_PALETTES[art.styleId]
            val bgCol = pal?.let { parseSafeHex(it.softHex, Color(0xFFEEFBF4)) } ?: Color(0xFFEEFBF4)
            val borderCol = pal?.let { parseSafeHex(it.edgeHex, Color(0xFF91CBB7)) } ?: Color(0xFF91CBB7)
            val strokeHex = pal?.inkHex ?: "#28786f"
            var bmp by remember(art.innerSvg, strokeHex) { mutableStateOf<ImageBitmap?>(null) }
            LaunchedEffect(art.innerSvg, strokeHex) {
                bmp = withContext(Dispatchers.IO) {
                    BadgeArtworkCache.getOrRenderOfficialSvg(art.innerSvg, strokeHex, 96)
                }
            }
            Box(
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(bgCol)
                    .border(1.2.dp, borderCol, CircleShape)
                    .padding(3.dp),
                contentAlignment = Alignment.Center,
            ) {
                bmp?.let {
                    Image(bitmap = it, contentDescription = null, modifier = Modifier.fillMaxSize())
                }
            }
        }

        is BadgeArtKind.AtlasV4 -> {
            var atlas by remember { mutableStateOf<ImageBitmap?>(null) }
            LaunchedEffect(Unit) {
                atlas = withContext(Dispatchers.IO) {
                    BadgeArtworkCache.getOrLoadAtlas(context)
                }
            }
            val pipColors = remember(level) { levelRingColors(level) }
            val pipCount = level.coerceIn(0, 5)
            Box(
                modifier = modifier.size(size),
                contentAlignment = Alignment.Center,
            ) {
                val sheet = atlas
                if (sheet != null) {
                    val col = (art.tileIndex % 6).coerceIn(0, 5)
                    val row = (art.tileIndex / 6).coerceIn(0, 7)
                    val tileW = max(1, sheet.width / 6)
                    val tileH = max(1, sheet.height / 8)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawImage(
                            image = sheet,
                            srcOffset = IntOffset(col * tileW, row * tileH),
                            srcSize = IntSize(tileW, tileH),
                            dstSize = IntSize(this.size.width.roundToInt(), this.size.height.roundToInt()),
                        )
                        if (pipCount > 0) {
                            val r = 1.25.dp.toPx()
                            val gap = 3.2.dp.toPx()
                            val totalW = (pipCount - 1) * gap
                            val startX = (this.size.width - totalW) * 0.5f
                            val cy = this.size.height - 1.8.dp.toPx()
                            for (i in 0 until pipCount) {
                                val cx = startX + i * gap
                                val colPip = pipColors[i % pipColors.size]
                                drawCircle(color = Color.White.copy(alpha = 0.85f), radius = r + 0.6.dp.toPx(), center = Offset(cx, cy))
                                drawCircle(color = colPip, radius = r, center = Offset(cx, cy))
                            }
                        }
                    }
                }
            }
        }

        is BadgeArtKind.CommunityCssBadge -> {
            val catalogRev by BadgeAndCosmeticRegistry.catalogRevision.collectAsState()
            val badgeSpec = remember(art.id, art.imgSha, art.cssSha, art.isDarkTheme, catalogRev) {
                ChatBubbleCosmetics.resolveCommunityBadgeSpec(art.id, art.imgSha, art.cssSha, art.isDarkTheme)
            }
            if (badgeSpec == null && !art.cssSha.isNullOrBlank()) {
                LaunchedEffect(art.id, art.cssSha) {
                    BadgeArtworkCache.ensureCommunityCssLoaded(context, httpClient, "${art.id}:badge:${art.cssSha}")
                }
            }
            val isDarkVariant = art.isDarkTheme &&
                (art.id == "shadow-dao" || art.id == "shadow-path" || badgeSpec?.hasDarkImageVariant == true)
            val srcUrl = remember(art.id, art.imgSha, art.cssSha, isDarkVariant, badgeSpec) {
                when {
                    badgeSpec != null && !badgeSpec.hasImage && art.imgSha == null -> null
                    isDarkVariant && !art.cssSha.isNullOrBlank() ->
                        "community-css-dark:${art.id}:badge:${art.cssSha}:${badgeSpec?.baseUrlIdx ?: 0}"
                    badgeSpec != null && badgeSpec.hasImage && art.imgSha == null && !art.cssSha.isNullOrBlank() ->
                        "community-css:${art.id}:badge:${art.cssSha}:${badgeSpec.baseUrlIdx}"
                    art.imgSha != null -> "https://52dmeme.com/api/community-skins/image/${art.imgSha}"
                    !art.cssSha.isNullOrBlank() ->
                        "community-css:${art.id}:badge:${art.cssSha}:${badgeSpec?.baseUrlIdx ?: 0}"
                    else -> null
                }
            }
            val bmp by produceState<ImageBitmap?>(
                initialValue = null,
                srcUrl,
                badgeSpec?.spriteCols,
                badgeSpec?.spriteRows,
                badgeSpec?.spriteCol,
                badgeSpec?.spriteRow,
            ) {
                value = when {
                    srcUrl == null -> null
                    badgeSpec != null && (badgeSpec.spriteCols > 1 || badgeSpec.spriteRows > 1) ->
                        BadgeArtworkCache.getOrCropSpriteCell(
                            context = context,
                            okHttpClient = httpClient,
                            source = srcUrl,
                            cols = badgeSpec.spriteCols,
                            rows = badgeSpec.spriteRows,
                            col = badgeSpec.spriteCol,
                            row = badgeSpec.spriteRow,
                            reqSizePx = 1024,
                        )
                    else -> BadgeArtworkCache.getOrFetchBitmap(context, httpClient, srcUrl, 256)
                }
            }
            val scaleFactor = if (badgeSpec != null && badgeSpec.heightDp > 0f) {
                size.value / badgeSpec.heightDp
            } else {
                1f
            }
            val targetW = if (badgeSpec != null) (badgeSpec.widthDp * scaleFactor).dp else size
            val topRes = ((badgeSpec?.topReserveDp ?: 0f) * scaleFactor).dp
            val rightRes = ((badgeSpec?.rightReserveDp ?: 0f) * scaleFactor).dp
            val bottomRes = ((badgeSpec?.bottomReserveDp ?: 0f) * scaleFactor).dp
            val leftRes = ((badgeSpec?.leftReserveDp ?: 0f) * scaleFactor).dp
            Box(
                modifier = modifier.padding(start = leftRes, top = topRes, end = rightRes, bottom = bottomRes),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier.width(targetW).height(size),
                    contentAlignment = Alignment.Center,
                ) {
                    bmp?.let {
                        Image(
                            bitmap = it,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    badgeSpec?.pseudoLayers?.forEach { layer ->
                        BubblePseudoLayerCanvas(layer = layer, modifier = Modifier.matchParentSize())
                    }
                }
            }
        }

        is BadgeArtKind.RemoteImage -> {
            if (art.url.startsWith("community-css")) {
                val bmp by produceState<ImageBitmap?>(initialValue = null, art.url) {
                    value = BadgeArtworkCache.getOrFetchBitmap(context, httpClient, art.url, 128)
                }
                Box(
                    modifier = modifier.size(size),
                    contentAlignment = Alignment.Center,
                ) {
                    bmp?.let {
                        Image(
                            bitmap = it,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            } else {
                val rewritten = remember(art.url) { MediaUrlRewriter.rewriteMediaUrl(art.url) }
                AsyncImage(
                    model = rewritten,
                    imageLoader = imageLoader,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = modifier.size(size),
                )
            }
        }

        is BadgeArtKind.RemoteSvg -> {
            val isV5 = art.url.contains("/badges/v5/")
            val bmp by produceState<ImageBitmap?>(initialValue = null, art.url) {
                value = BadgeArtworkCache.getOrFetchBitmap(context, httpClient, art.url, 128)
            }
            Box(
                modifier = if (isV5) modifier.width(size * 0.912f).height(size) else modifier.size(size),
                contentAlignment = Alignment.Center,
            ) {
                val loaded = bmp
                if (loaded != null) {
                    Image(
                        bitmap = loaded,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(art.fallbackGlyph, fontSize = (size.value * 0.55f).sp)
                }
                if (isV5 && level >= 4) {
                    V5LevelStarOverlay(level = level, modifier = Modifier.matchParentSize())
                }
            }
        }

        is BadgeArtKind.InlineXmlSvg -> {
            val bmp by produceState<ImageBitmap?>(initialValue = null, art.xml) {
                value = withContext(Dispatchers.Default) {
                    BadgeArtworkCache.getOrRenderXmlSvg("inline_${art.xml.hashCode()}", art.xml, 128)
                }
            }
            Box(
                modifier = modifier.size(size),
                contentAlignment = Alignment.Center,
            ) {
                bmp?.let {
                    Image(
                        bitmap = it,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        is BadgeArtKind.SpriteSheetCell -> {
            val bmp by produceState<ImageBitmap?>(
                initialValue = null,
                art.url,
                art.cols,
                art.rows,
                art.col,
                art.row,
            ) {
                value = BadgeArtworkCache.getOrCropSpriteCell(
                    context = context,
                    okHttpClient = httpClient,
                    source = art.url,
                    cols = art.cols,
                    rows = art.rows,
                    col = art.col,
                    row = art.row,
                    reqSizePx = 1024,
                )
            }
            Box(
                modifier = modifier.size(size),
                contentAlignment = Alignment.Center,
            ) {
                bmp?.let {
                    Image(
                        bitmap = it,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        is BadgeArtKind.EmojiGlyph -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
                    .border(1.dp, ringBrush, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(art.glyph, fontSize = (size.value * 0.55f).sp)
            }
        }
    }
}

@Composable
private fun V5LevelStarOverlay(
    level: Int,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val r = 1.6.dp.toPx()
        val starCol = if (level >= 5) Color(0xFFFFD54F) else Color(0xFFFDE047)
        val cx = size.width - r - 1.dp.toPx()
        val cy = r + 1.dp.toPx()
        drawCircle(Color.White.copy(alpha = 0.8f), radius = r + 0.6.dp.toPx(), center = Offset(cx, cy))
        drawCircle(starCol, radius = r, center = Offset(cx, cy))
    }
}

/**
 * Renders the single equipped badge matching `app.js` `badgeChip(id, mini, n, lv)`.
 * - Icon-only badges (`mini`, `community:*`, `pt:*`, `ot:*`, `ft:claude`, `gr:*`) render only the artwork.
 * - Card non-icon badges (`ft:*` except claude, `bd:*`) render a borderless row (no fake outer pill).
 */
@Composable
fun BadgeChipView(
    badge: ResolvedBadgeChip,
    imageLoader: ImageLoader,
    mini: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val palette = LocalForHappyPalette.current
    val httpClient = LocalCosmeticHttpClient.current

    badge.dynamicCssSpec?.let { spec ->
        LaunchedEffect(spec) {
            BadgeArtworkCache.ensureCommunityCssLoaded(context, httpClient, spec)
        }
    }

    if (mini || badge.alwaysIconOnly) {
        val iconSize = when {
            badge.id.startsWith("pt:owner-52") -> if (mini) 23.dp else 26.dp
            badge.id.startsWith("ft:") -> if (mini) 22.dp else 24.dp
            badge.id.startsWith("community:") -> if (mini) 20.dp else 23.dp
            mini -> 20.dp
            else -> 23.dp
        }
        BadgeArtIcon(
            art = badge.art,
            level = badge.level,
            imageLoader = imageLoader,
            size = iconSize,
            modifier = if (onClick != null) modifier.clickable { onClick() } else modifier,
        )
        return
    }

    val (tierAccent, _) = tierThemeColors(badge.tier)
    val rawLabelColor = if (badge.id.startsWith("ft:")) {
        parseSafeHex(badge.textHex, tierAccent)
    } else {
        tierAccent
    }
    val readableLabelColor = ChatBubbleCosmetics.ensureReadableForeground(rawLabelColor, palette.surface, minContrast = 3.2f)
    val lvBg = badge.levelBgHex?.let { parseSafeHex(it, tierAccent) } ?: tierAccent

    Row(
        modifier = modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BadgeArtIcon(
            art = badge.art,
            level = badge.level,
            imageLoader = imageLoader,
            size = 20.dp,
        )
        Text(
            text = badge.name,
            color = readableLabelColor,
            fontSize = 11.5.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
        )
        if (badge.levelText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(lvBg)
                    .padding(horizontal = 4.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = badge.levelText,
                    color = Color.White,
                    fontSize = 8.5.sp,
                    lineHeight = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                )
            }
        }
    }
}

/**
 * Renders the user's equipped title (`titleEl`) 1:1 matching `52dmeme.com` web styles.
 */
@Composable
fun TitleChipView(
    title: ResolvedTitleChip,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val palette = LocalForHappyPalette.current
    val httpClient = LocalCosmeticHttpClient.current

    title.dynamicCssSpec?.let { cssSpec ->
        LaunchedEffect(cssSpec) {
            BadgeArtworkCache.ensureCommunityCssLoaded(context, httpClient, cssSpec)
        }
    }

    if (title.specialKind == "owner52" && title.bgImageUrl != null) {
        val ownerArt by produceState<ImageBitmap?>(initialValue = null, title.bgImageUrl) {
            value = BadgeArtworkCache.getOrFetchBitmap(context, httpClient, title.bgImageUrl, 256)
        }
        val loadedOwnerArt = ownerArt
        if (loadedOwnerArt != null) {
            Image(
                bitmap = loadedOwnerArt,
                contentDescription = title.label,
                contentScale = ContentScale.Fit,
                modifier = modifier
                    .width(52.dp)
                    .height(28.dp),
            )
            return
        }
    }

    if (title.specialKind == "community" && title.communitySpec != null) {
        CommunityTitleChipBox(title = title, spec = title.communitySpec, modifier = modifier)
        return
    }

    if (title.specialKind == "personal_v2" && title.bgImageUrl != null) {
        PersonalV2TitleChipBox(title = title, modifier = modifier)
        return
    }

    if (title.specialKind == "shiguang") {
        ShiguangTitleChipBox(title = title, modifier = modifier)
        return
    }

    if (title.specialKind == "raffle_nameplate" || title.specialKind == "raffle_oldfarmer") {
        RaffleNameplateTitleChipBox(title = title, isDarkTheme = palette.isDark, modifier = modifier)
        return
    }

    val css = title.customCss
    val baseBg = css?.bgColor ?: css?.bgGradientColors?.firstOrNull() ?: parseSafeHex(title.bgHex, Color(0xFF1F6B4F))
    val baseSecBg = css?.bgGradientColors?.lastOrNull()
        ?: title.secondaryBgHex?.let { parseSafeHex(it, baseBg) }
        ?: baseBg
    val rawFg = css?.textColor ?: parseSafeHex(title.textHex, Color.White)
    val bdColor = css?.borderColor ?: parseSafeHex(title.borderHex, baseBg)

    val fillMode = when {
        css?.bgGradientColors != null -> "grad"
        css?.bgColor != null -> "solid"
        else -> title.fillMode
    }

    val chipBgBrush: Brush? = when (fillMode) {
        "grad" -> Brush.linearGradient(
            if (css?.bgGradientColors != null && css.bgGradientColors.size >= 2) {
                css.bgGradientColors
            } else {
                listOf(baseBg, baseSecBg)
            },
        )
        else -> null
    }
    val chipBgColor: Color = when (fillMode) {
        "solid" -> baseBg
        "soft" -> baseBg.copy(alpha = 0.16f)
        "glass" -> baseBg.copy(alpha = 0.24f)
        "outline", "none" -> Color.Transparent
        else -> baseBg
    }

    val effectiveBgForContrast = when (fillMode) {
        "soft" -> ChatBubbleCosmetics.blendColors(baseBg, palette.surface, 0.16f)
        "glass" -> ChatBubbleCosmetics.blendColors(baseBg, palette.surface, 0.24f)
        "outline", "none" -> palette.surface
        else -> baseBg
    }

    val desiredFg = when {
        css?.textColor != null -> rawFg
        fillMode == "outline" || fillMode == "none" -> {
            if (title.specialKind == "standard") bdColor else rawFg
        }
        fillMode == "soft" && rawFg == Color.White -> baseBg
        else -> rawFg
    }
    val fgColor = ChatBubbleCosmetics.ensureReadableForeground(desiredFg, effectiveBgForContrast, minContrast = 4.0f)

    val shape = when {
        css?.cornerRadiiDp != null && css.cornerRadiiDp.size == 4 -> RoundedCornerShape(
            topStart = css.cornerRadiiDp[0].dp,
            topEnd = css.cornerRadiiDp[1].dp,
            bottomEnd = css.cornerRadiiDp[2].dp,
            bottomStart = css.cornerRadiiDp[3].dp,
        )
        css?.cornerRadiusDp != null -> RoundedCornerShape(css.cornerRadiusDp.dp)
        title.shape == "rect" -> RoundedCornerShape(4.dp)
        title.shape == "oval" -> RoundedCornerShape(50)
        title.shape == "tag" -> RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 10.dp, bottomEnd = 10.dp)
        title.shape == "blob" -> RoundedCornerShape(topStart = 11.dp, topEnd = 5.dp, bottomEnd = 10.dp, bottomStart = 6.dp)
        title.shape == "ribbon" -> CutCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 8.dp, bottomEnd = 8.dp)
        title.shape == "seal" -> RoundedCornerShape(3.dp)
        else -> RoundedCornerShape(999.dp)
    }

    val hasBorder = title.shape != "none" && fillMode != "none"
    val borderWidth = (css?.borderWidthDp ?: when {
        title.specialKind == "personal_v1" -> 1.4f
        title.shape == "seal" -> 1.5f
        else -> 1f
    }).dp
    val borderColorEffective = when {
        title.specialKind == "personal_v1" || title.specialKind == "official" || title.specialKind == "guangjie" -> bdColor
        fillMode == "soft" -> bdColor.copy(alpha = 0.48f)
        fillMode == "glass" -> Color.White.copy(alpha = 0.45f)
        fillMode == "outline" -> bdColor.copy(alpha = 0.85f)
        else -> bdColor.copy(alpha = 0.65f)
    }

    val fontFamily = when (title.fontKey) {
        "kai", "song" -> FontFamily.Serif
        "mono" -> FontFamily.Monospace
        "xing" -> FontFamily.Cursive
        "hei", "round" -> FontFamily.SansSerif
        else -> null
    }
    val fontWeight = css?.fontWeight ?: FontWeight.SemiBold
    val fontStyle = css?.fontStyle
    val letterSpacing = css?.letterSpacingSp?.sp ?: TextStyle.Default.letterSpacing

    val bgImageUrl = title.bgImageUrl
    val bannerBmp by produceState<ImageBitmap?>(initialValue = null, bgImageUrl) {
        value = if (!bgImageUrl.isNullOrBlank()) {
            BadgeArtworkCache.getOrFetchBitmap(context, httpClient, bgImageUrl, 256)
        } else {
            null
        }
    }
    val leadingIconBmp by produceState<ImageBitmap?>(initialValue = null, title.leadingIconUrl) {
        value = if (!title.leadingIconUrl.isNullOrBlank()) {
            BadgeArtworkCache.getOrFetchBitmap(context, httpClient, title.leadingIconUrl, 96)
        } else {
            null
        }
    }
    val trailingIconBmp by produceState<ImageBitmap?>(initialValue = null, title.trailingIconUrl) {
        value = if (!title.trailingIconUrl.isNullOrBlank()) {
            BadgeArtworkCache.getOrFetchBitmap(context, httpClient, title.trailingIconUrl, 96)
        } else {
            null
        }
    }

    val hasMotion = title.effect == "shine" || title.effect == "flow" || title.effect == "aurora"
    val shimmerFraction = if (hasMotion) {
        val transition = rememberInfiniteTransition(label = "title_fx")
        val frac by transition.animateFloat(
            initialValue = -0.35f,
            targetValue = 1.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "title_shimmer",
        )
        frac
    } else {
        -1f
    }

    val symbolPath = remember(title.symbolSvgPath) {
        title.symbolSvgPath?.let { rawPath ->
            runCatching { PathParser.createPathFromPathData(rawPath).asComposePath() }.getOrNull()
        }
    }

    val hardShadowColor = title.hardShadowHex?.let { parseSafeHex(it, Color.Transparent) }?.takeIf { it != Color.Transparent }
    val hardShadowOff = title.hardShadowOffsetDp
    val loadedBanner = bannerBmp

    Box(
        modifier = modifier.then(
            if (hardShadowColor != null && hardShadowOff != null) {
                Modifier.padding(
                    end = hardShadowOff.first.coerceAtLeast(0f).dp,
                    bottom = hardShadowOff.second.coerceAtLeast(0f).dp,
                )
            } else {
                Modifier
            },
        ),
    ) {
        if (hardShadowColor != null && hardShadowOff != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = hardShadowOff.first.dp, y = hardShadowOff.second.dp)
                    .clip(shape)
                    .background(hardShadowColor),
            )
        }
        Box(
            modifier = Modifier
                .clip(shape)
                .then(
                    if (chipBgBrush != null) {
                        Modifier.background(chipBgBrush)
                    } else if (chipBgColor != Color.Transparent) {
                        Modifier.background(chipBgColor)
                    } else {
                        Modifier
                    },
                )
                .then(
                    if (hasBorder) {
                        Modifier.border(borderWidth, borderColorEffective, shape)
                    } else {
                        Modifier
                    },
                ),
        ) {
            if (loadedBanner != null) {
                Image(
                    bitmap = loadedBanner,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
                Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.10f)))
            }

            if (title.shape == "seal" && hasBorder) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(2.dp)
                        .border(0.6.dp, fgColor.copy(alpha = 0.45f), RoundedCornerShape(1.5.dp)),
                )
            }

            if (shimmerFraction >= 0f) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val band = size.width * 0.28f
                    val cx = shimmerFraction * (size.width + band * 2f) - band
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.24f), Color.Transparent),
                            start = Offset(cx - band, 0f),
                            end = Offset(cx + band, size.height),
                        ),
                    )
                }
            }

            Row(
                modifier = Modifier.padding(
                    horizontal = if (fillMode == "none") 3.dp else 8.dp,
                    vertical = 2.5.dp,
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                leadingIconBmp?.let {
                    Image(bitmap = it, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(14.dp))
                }
                if (symbolPath != null) {
                    Canvas(modifier = Modifier.size(12.dp)) {
                        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
                            drawPath(
                                path = symbolPath,
                                color = fgColor,
                                style = Stroke(width = 1.7.dp.toPx() / (size.width / 24f).coerceAtLeast(0.1f), cap = StrokeCap.Round, join = StrokeJoin.Round),
                            )
                        }
                    }
                }
                Text(
                    text = title.label,
                    color = fgColor,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    fontFamily = fontFamily,
                    fontWeight = fontWeight,
                    fontStyle = fontStyle,
                    letterSpacing = letterSpacing,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = if (loadedBanner != null) {
                        TextStyle(
                            shadow = Shadow(color = Color.Black.copy(alpha = 0.72f), offset = Offset(0f, 1f), blurRadius = 2.5f),
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                        )
                    } else {
                        TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    },
                )
                trailingIconBmp?.let {
                    Image(bitmap = it, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(14.dp))
                }
                if (!title.levelText.isNullOrBlank()) {
                    Text(
                        text = title.levelText,
                        color = fgColor.copy(alpha = 0.88f),
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                    )
                }
            }
        }
    }
}

@Composable
private fun CommunityTitleChipBox(
    title: ResolvedTitleChip,
    spec: CommunityTitleRenderSpec,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val httpClient = LocalCosmeticHttpClient.current
    val leadingIconBmp by produceState<ImageBitmap?>(initialValue = null, title.leadingIconUrl) {
        value = title.leadingIconUrl?.takeIf { it.isNotBlank() }?.let {
            BadgeArtworkCache.getOrFetchBitmap(context, httpClient, it, 96)
        }
    }
    val trailingIconBmp by produceState<ImageBitmap?>(initialValue = null, title.trailingIconUrl) {
        value = title.trailingIconUrl?.takeIf { it.isNotBlank() }?.let {
            BadgeArtworkCache.getOrFetchBitmap(context, httpClient, it, 96)
        }
    }

    val startRes = max(spec.startOverflowReserveDp, (spec.nineSliceSpec?.outsetLeftDp ?: 0f).coerceAtLeast(0f)).dp
    val topRes = max(spec.topOverflowReserveDp, (spec.nineSliceSpec?.outsetTopDp ?: 0f).coerceAtLeast(0f)).dp
    val endRes = max(spec.endOverflowReserveDp, (spec.nineSliceSpec?.outsetRightDp ?: 0f).coerceAtLeast(0f)).dp
    val bottomRes = max(spec.bottomOverflowReserveDp, (spec.nineSliceSpec?.outsetBottomDp ?: 0f).coerceAtLeast(0f)).dp

    val r = spec.cornerRadiiDp
    val shape = RoundedCornerShape(
        topStart = r.getOrElse(0) { 999f }.dp,
        topEnd = r.getOrElse(1) { 999f }.dp,
        bottomEnd = r.getOrElse(2) { 999f }.dp,
        bottomStart = r.getOrElse(3) { 999f }.dp,
    )
    val hasNineSlice = spec.nineSliceSpec != null
    val bgModifier = when {
        hasNineSlice -> Modifier
        spec.bgColors.size >= 2 -> Modifier.drawBehind {
            val angleRad = ((spec.bgAngleDeg ?: 135f) - 90f) * PI / 180.0
            val dx = cos(angleRad).toFloat()
            val dy = sin(angleRad).toFloat()
            val cx = size.width * 0.5f
            val cy = size.height * 0.5f
            val halfSpan = max(size.width, size.height) * 0.5f
            drawRect(
                brush = Brush.linearGradient(
                    colors = spec.bgColors,
                    start = Offset(cx - dx * halfSpan, cy - dy * halfSpan),
                    end = Offset(cx + dx * halfSpan, cy + dy * halfSpan),
                ),
            )
        }
        spec.bgColors.size == 1 -> Modifier.background(spec.bgColors[0])
        else -> Modifier
    }

    Box(modifier = modifier.padding(start = startRes, top = topRes, end = endRes, bottom = bottomRes)) {
        Box(
            modifier = Modifier
                .then(
                    spec.fixedHeightDp?.let { Modifier.height(it.dp) }
                        ?: Modifier.defaultMinSize(minHeight = spec.minHeightDp.dp),
                )
                .then(if (hasNineSlice) Modifier else Modifier.clip(shape))
                .then(bgModifier)
                .then(
                    if (!hasNineSlice && spec.borderWidthDp > 0f && spec.borderColor != Color.Transparent) {
                        Modifier.border(spec.borderWidthDp.dp, spec.borderColor, shape)
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            spec.nineSliceSpec?.let { ns ->
                NineSliceFrameCanvas(spec = ns, modifier = Modifier.matchParentSize())
            }
            spec.pseudoLayers.forEach { layer ->
                BubblePseudoLayerCanvas(layer = layer, modifier = Modifier.matchParentSize())
            }
            Row(
                modifier = Modifier.padding(
                    start = spec.padLeftDp.dp,
                    top = spec.padTopDp.dp,
                    end = spec.padRightDp.dp,
                    bottom = spec.padBottomDp.dp,
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                leadingIconBmp?.let {
                    Image(bitmap = it, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(14.dp))
                }
                Text(
                    text = title.label,
                    color = spec.textColor,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    fontWeight = spec.fontWeight,
                    letterSpacing = spec.letterSpacingSp?.sp ?: TextStyle.Default.letterSpacing,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        shadow = spec.textShadowColor?.let { Shadow(color = it, offset = Offset(0f, 1f), blurRadius = 2.2f) },
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                    ),
                )
                trailingIconBmp?.let {
                    Image(bitmap = it, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
private fun PersonalV2TitleChipBox(
    title: ResolvedTitleChip,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val httpClient = LocalCosmeticHttpClient.current
    val artUrl = title.bgImageUrl ?: return
    val bannerBmp by produceState<ImageBitmap?>(initialValue = null, artUrl) {
        value = BadgeArtworkCache.getOrFetchBitmap(context, httpClient, artUrl, 256)
    }
    val spec = title.personalV2Art ?: ChatBubbleCosmetics.resolvePersonalV2Art(title.key.removePrefix("personal:"))
    val loaded = bannerBmp

    if (loaded == null) {
        Box(
            modifier = modifier
                .height(25.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFFF3F6FA))
                .border(1.dp, Color(0xFFB7C4D4), RoundedCornerShape(999.dp))
                .padding(horizontal = spec.titlePadDp.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title.label,
                color = Color(0xFF304052),
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
            )
        }
        return
    }

    Layout(
        modifier = modifier,
        content = {
            Image(
                bitmap = loaded,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
            )
            Text(
                text = title.label,
                color = Color(0xFF304052),
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
            )
        },
    ) { measurables, constraints ->
        val chipH = 25.dp.roundToPx().coerceIn(constraints.minHeight, constraints.maxHeight)
        val padPx = spec.titlePadDp.dp.roundToPx()
        val textPlaceable = measurables[1].measure(
            constraints.copy(
                minWidth = 0,
                minHeight = 0,
                maxHeight = chipH,
            ),
        )
        val minBannerW = (loaded.width.toFloat() / loaded.height.coerceAtLeast(1).toFloat() * chipH).roundToInt()
        val targetW = max(textPlaceable.width + padPx * 2, minBannerW)
            .coerceIn(constraints.minWidth, constraints.maxWidth)
        val imgPlaceable = measurables[0].measure(Constraints.fixed(targetW, chipH))
        layout(targetW, chipH) {
            imgPlaceable.placeRelative(0, 0)
            val safePad = (padPx / 2).coerceAtMost((targetW - textPlaceable.width).coerceAtLeast(0) / 2)
            val tx = (targetW * spec.titleX - textPlaceable.width * 0.5f)
                .roundToInt()
                .coerceIn(safePad, (targetW - textPlaceable.width - safePad).coerceAtLeast(safePad))
            val ty = (chipH * spec.titleY - textPlaceable.height * 0.5f)
                .roundToInt()
                .coerceIn(0, (chipH - textPlaceable.height).coerceAtLeast(0))
            textPlaceable.placeRelative(tx, ty)
        }
    }
}

@Composable
private fun ShiguangTitleChipBox(
    title: ResolvedTitleChip,
    modifier: Modifier = Modifier,
) {
    val bodyShape = remember {
        GenericShape { size, _ ->
            val cut = size.height * 0.22f
            moveTo(cut, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width - cut, size.height)
            lineTo(0f, size.height)
            lineTo(cut * 0.65f, size.height * 0.5f)
            close()
        }
    }
    val lvShape = remember {
        GenericShape { size, _ ->
            val cut = size.height * 0.22f
            moveTo(cut, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width - cut, size.height)
            lineTo(0f, size.height)
            close()
        }
    }
    Row(
        modifier = modifier.height(23.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .clip(bodyShape)
                .background(Brush.verticalGradient(listOf(Color(0xFFFAF0DC), Color(0xFFEAD3AE))))
                .border(1.dp, Color(0xFFB8894A), bodyShape)
                .padding(start = 10.dp, end = 9.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title.label,
                color = Color(0xFF542710),
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
            )
        }
        if (!title.levelText.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .offset(x = (-3).dp)
                    .clip(lvShape)
                    .background(Brush.verticalGradient(listOf(Color(0xFF7A3E1D), Color(0xFF542710))))
                    .border(0.8.dp, Color(0xFFB8894A), lvShape)
                    .padding(start = 7.dp, end = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = title.levelText,
                    color = Color(0xFFFFE6B3),
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                )
            }
        }
    }
}

@Composable
private fun RaffleNameplateTitleChipBox(
    title: ResolvedTitleChip,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    val isOldFarmer = title.specialKind == "raffle_oldfarmer"
    val rtColor = parseSafeHex(title.raffleColorHex ?: title.borderHex, Color(0xFF488CAF))
    val rtAccent = parseSafeHex(title.raffleAccentHex ?: title.secondaryBgHex ?: title.borderHex, Color(0xFFB6DCEC))
    val shape = RoundedCornerShape(topStart = 7.dp, topEnd = 3.dp, bottomEnd = 9.dp, bottomStart = 3.dp)

    val bgStops = when {
        isDarkTheme -> listOf(
            ChatBubbleCosmetics.blendColors(rtColor, Color(0xFF18222D), 0.30f),
            ChatBubbleCosmetics.blendColors(rtAccent, Color(0xFF18222D), 0.20f),
            ChatBubbleCosmetics.blendColors(rtColor, Color(0xFF111922), 0.36f),
        )
        isOldFarmer -> listOf(
            Color(0xFFFFFBF2),
            Color(0xFFFFF0D4),
            Color(0xFFF6DEB0),
        )
        else -> listOf(
            Color.White,
            ChatBubbleCosmetics.blendColors(rtAccent, Color.White, 0.34f),
            ChatBubbleCosmetics.blendColors(rtAccent, rtColor, 0.72f),
        )
    }
    val fgColor = if (isDarkTheme) {
        Color(0xFFF8FAFC)
    } else {
        ChatBubbleCosmetics.blendColors(rtColor, Color(0xFF212E40), 0.65f)
    }

    val symbolPath = remember(title.symbolSvgPath) {
        title.symbolSvgPath?.let { rawPath ->
            runCatching { PathParser.createPathFromPathData(rawPath).asComposePath() }.getOrNull()
        }
    }

    Box(
        modifier = modifier
            .height(25.dp)
            .clip(shape)
            .background(Brush.linearGradient(bgStops))
            .border(1.2.dp, rtColor, shape),
        contentAlignment = Alignment.Center,
    ) {
        // 2.dp inset crisp white inner ring (`title-growth.css`)
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(2.dp)
                .border(0.8.dp, Color.White.copy(alpha = if (isDarkTheme) 0.25f else 0.78f), RoundedCornerShape(5.dp, 2.dp, 7.dp, 2.dp)),
        )
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (symbolPath != null) {
                Canvas(modifier = Modifier.size(14.dp)) {
                    val sx = (size.width / 24f).coerceAtLeast(0.1f)
                    scale(sx, (size.height / 24f).coerceAtLeast(0.1f), pivot = Offset.Zero) {
                        drawPath(
                            path = symbolPath,
                            color = fgColor,
                            style = Stroke(
                                width = 1.7.dp.toPx() / sx,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            ),
                        )
                    }
                }
            }
            Text(
                text = title.label,
                color = fgColor,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
            )
            if (!title.levelText.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.5.dp))
                        .background(rtColor)
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = title.levelText,
                        color = Color.White,
                        fontSize = 8.5.sp,
                        lineHeight = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                    )
                }
            }
        }
    }
}

fun parseSafeHex(hex: String, fallback: Color): Color {
    val clean = hex.trim().removePrefix("#")
    val parsed = clean.toLongOrNull(16) ?: return fallback
    return when (clean.length) {
        6 -> Color(0xFF000000L or parsed)
        8 -> Color(parsed)
        else -> fallback
    }
}
