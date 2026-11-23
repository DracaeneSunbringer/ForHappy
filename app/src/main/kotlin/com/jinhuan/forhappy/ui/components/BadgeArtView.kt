package com.jinhuan.forhappy.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.PathParser
import coil3.ImageLoader
import coil3.compose.AsyncImage
import com.jinhuan.forhappy.core.cosmetics.BadgeArtKind
import com.jinhuan.forhappy.core.cosmetics.ChatBubbleCosmetics
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
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

val LocalCosmeticHttpClient = staticCompositionLocalOf<OkHttpClient?> { null }

/**
 * Zero-dependency loader, disk cache & rasterizer for bundled `assets/badges/v4/atlas.webp`,
 * `assets/badges/v5/<id>.svg`, `OFFICIAL_ART` SVGs, remote SVGs/WebPs, sprite-sheet cells,
 * and `meme-hall-skins` CSS-embedded data-URI artwork.
 */
object BadgeArtworkCache {
    private val svgCache = ConcurrentHashMap<String, ImageBitmap>()
    private val bitmapCache = ConcurrentHashMap<String, ImageBitmap>()
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
    ): ImageBitmap? {
        val cacheKey = "$source#$reqSizePx"
        bitmapCache[cacheKey]?.let { return it }
        if (failedUrls.containsKey(cacheKey)) return null

        val loaded = withContext(Dispatchers.IO) {
            if (source.startsWith("community-css-dark:") || source.startsWith("community-css:")) {
                loadCommunityCssBitmap(context, okHttpClient, source, reqSizePx)
            } else {
                val bytes = fetchBytesWithDiskCache(context, okHttpClient, source) ?: return@withContext null
                decodeBytesToBitmap(bytes, source, reqSizePx)
            }
        }
        if (loaded != null) {
            bitmapCache[cacheKey] = loaded
        } else {
            failedUrls[cacheKey] = true
        }
        return loaded
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
        bitmapCache[key]?.let { return it }
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
            bitmapCache[key] = cropped
        }
        return cropped
    }

    private fun loadCommunityCssBitmap(
        context: Context,
        okHttpClient: OkHttpClient?,
        source: String,
        reqSizePx: Int,
    ): ImageBitmap? {
        val isDark = source.startsWith("community-css-dark:")
        val rest = source.removePrefix("community-css-dark:").removePrefix("community-css:")
        val parts = rest.split(":")
        if (parts.size < 3) return null
        val id = parts[0]
        val kind = parts[1]
        val sha = parts[2]
        val cssUrl = "https://52dmeme.com/api/community-skins/css/$id/$kind/$sha"
        val cssBytes = fetchBytesWithDiskCache(context, okHttpClient, cssUrl) ?: return null
        val cssText = cssBytes.toString(Charsets.UTF_8)
        val dataUri = extractDataUriFromCss(cssText, preferDark = isDark) ?: return null
        return decodeDataUriBitmap(dataUri, reqSizePx)
    }

    private fun extractDataUriFromCss(cssText: String, preferDark: Boolean): String? {
        val searchSlice = if (preferDark) {
            val idx = cssText.indexOf("data-theme")
            if (idx >= 0) cssText.substring(idx) else cssText
        } else {
            cssText
        }
        val regex = Regex("""url\(\s*['"]?(data:image/[^'")]+)['"]?\s*\)""")
        val matches = regex.findAll(searchSlice).map { it.groupValues[1] }.toList()
        if (matches.isNotEmpty()) {
            return if (preferDark && !searchSlice.startsWith("[data-theme")) matches.last() else matches.first()
        }
        if (preferDark) {
            return regex.findAll(cssText).lastOrNull()?.groupValues?.getOrNull(1)
        }
        return null
    }

    private fun decodeDataUriBitmap(dataUri: String, reqSizePx: Int): ImageBitmap? {
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
            MiniBadgeSvgRasterizer.renderSvgXml(svgXml, min(reqSizePx, 256))
        } else if (header.contains(";base64")) {
            val bytes = runCatching { Base64.decode(payload, Base64.DEFAULT) }.getOrNull() ?: return null
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } else {
            null
        }
    }

    private fun decodeBytesToBitmap(bytes: ByteArray, source: String, reqSizePx: Int): ImageBitmap? {
        val cleanUrl = source.substringBefore('?').lowercase()
        val headText = bytes.take(min(bytes.size, 256)).toByteArray().toString(Charsets.UTF_8).trimStart()
        if (cleanUrl.endsWith(".svg") || headText.startsWith("<svg", ignoreCase = true) ||
            (headText.startsWith("<?xml", ignoreCase = true) && headText.contains("<svg", ignoreCase = true))
        ) {
            return MiniBadgeSvgRasterizer.renderSvgXml(bytes.toString(Charsets.UTF_8), min(reqSizePx, 256))
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
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

    private val TRANSLATE_RE = Regex("""translate\(\s*([+-]?\d*\.?\d+)(?:[\s,]+([+-]?\d*\.?\d+))?\s*\)""")
    private val SCALE_RE = Regex("""scale\(\s*([+-]?\d*\.?\d+)(?:[\s,]+([+-]?\d*\.?\d+))?\s*\)""")
    private val ROTATE_RE = Regex("""rotate\(\s*([+-]?\d*\.?\d+)(?:[\s,]+([+-]?\d*\.?\d+)[\s,]+([+-]?\d*\.?\d+))?\s*\)""")
    private val RGB_RE = Regex("""rgba?\(\s*(\d+(?:\.\d+)?)\s*,\s*(\d+(?:\.\d+)?)\s*,\s*(\d+(?:\.\d+)?)(?:\s*,\s*(\d+(?:\.\d+)?))?\s*\)""")

    fun renderSvgXml(xmlText: String, sizePx: Int): ImageBitmap? = runCatching {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(xmlText))

        var viewW = 64f
        var viewH = 64f
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
                            var minX = 0f
                            var minY = 0f
                            val vb = parser.getAttributeValue(null, "viewBox")
                            if (!vb.isNullOrBlank()) {
                                val nums = vb.trim().split(Regex("""[\s,]+""")).mapNotNull { it.toFloatOrNull() }
                                if (nums.size >= 4) {
                                    minX = nums[0]
                                    minY = nums[1]
                                    viewW = max(1f, nums[2])
                                    viewH = max(1f, nums[3])
                                }
                            }
                            canvas.scale(sizePx / viewW, sizePx / viewH)
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
        bitmap.asImageBitmap()
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
        val op = parser.getAttributeValue(null, "opacity")?.toFloatOrNull() ?: 1f
        val fillOp = parser.getAttributeValue(null, "fill-opacity")?.toFloatOrNull() ?: parent.fillOpacity
        val strokeOp = parser.getAttributeValue(null, "stroke-opacity")?.toFloatOrNull() ?: parent.strokeOpacity
        return StyleFrame(
            fill = fill,
            stroke = stroke,
            strokeWidth = sw,
            strokeCap = cap,
            strokeJoin = join,
            opacity = (parent.opacity * op).coerceIn(0f, 1f),
            fillOpacity = fillOp.coerceIn(0f, 1f),
            strokeOpacity = strokeOp.coerceIn(0f, 1f),
            hasTransform = hasTf,
        )
    }

    private fun applyTransform(canvas: AndroidCanvas, raw: String) {
        TRANSLATE_RE.find(raw)?.let { m ->
            val tx = m.groupValues[1].toFloatOrNull() ?: 0f
            val ty = m.groupValues[2].toFloatOrNull() ?: 0f
            canvas.translate(tx, ty)
        }
        ROTATE_RE.find(raw)?.let { m ->
            val deg = m.groupValues[1].toFloatOrNull() ?: 0f
            val cx = m.groupValues[2].toFloatOrNull()
            val cy = m.groupValues[3].toFloatOrNull()
            if (cx != null && cy != null) {
                canvas.rotate(deg, cx, cy)
            } else {
                canvas.rotate(deg)
            }
        }
        SCALE_RE.find(raw)?.let { m ->
            val sx = m.groupValues[1].toFloatOrNull() ?: 1f
            val sy = m.groupValues[2].toFloatOrNull() ?: sx
            canvas.scale(sx, sy)
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
            val hex = s.removePrefix("#")
            val expanded = if (hex.length == 3) {
                "${hex[0]}${hex[0]}${hex[1]}${hex[1]}${hex[2]}${hex[2]}"
            } else {
                hex
            }
            return runCatching { AndroidColor.parseColor("#$expanded") }.getOrNull()
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
    val ringBrush = remember(level) { Brush.linearGradient(levelRingColors(level)) }

    when (art) {
        is BadgeArtKind.BundledSvg -> {
            var bmp by remember(art.assetPath) { mutableStateOf<ImageBitmap?>(null) }
            LaunchedEffect(art.assetPath) {
                bmp = withContext(Dispatchers.IO) {
                    BadgeArtworkCache.getOrRenderAssetSvg(context, art.assetPath, 128)
                }
            }
            Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
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

        is BadgeArtKind.InlineOfficialSvg -> {
            var bmp by remember(art.innerSvg) { mutableStateOf<ImageBitmap?>(null) }
            LaunchedEffect(art.innerSvg) {
                bmp = withContext(Dispatchers.IO) {
                    BadgeArtworkCache.getOrRenderOfficialSvg(art.innerSvg, "#FDE68A", 96)
                }
            }
            Box(
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(Color(0xFF1E3A8A))
                    .border(1.dp, Color(0xFF60A5FA), CircleShape)
                    .padding(2.dp),
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
            Box(
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(Color(0xFFFFFBEB))
                    .border(1.5.dp, ringBrush, CircleShape)
                    .padding(1.5.dp)
                    .clip(CircleShape),
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
                    }
                }
            }
        }

        is BadgeArtKind.RemoteImage -> {
            if (art.url.startsWith("community-css")) {
                val httpClient = LocalCosmeticHttpClient.current
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
            val httpClient = LocalCosmeticHttpClient.current
            val bmp by produceState<ImageBitmap?>(initialValue = null, art.url) {
                value = BadgeArtworkCache.getOrFetchBitmap(context, httpClient, art.url, 128)
            }
            Box(
                modifier = modifier.size(size),
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
            val httpClient = LocalCosmeticHttpClient.current
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

/**
 * Renders the single equipped badge matching `app.js` `badgeChip(id, mini, n, lv)`.
 * - `mini = true` (group chat header): compact sticker icon only.
 * - `mini = false` (profile header & preview): sticker icon + Chinese name + level badge (`LvX` / `★`).
 */
@Composable
fun BadgeChipView(
    badge: ResolvedBadgeChip,
    imageLoader: ImageLoader,
    mini: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (mini || badge.id.startsWith("pt:owner-52")) {
        val iconSize = when {
            mini && (badge.id.startsWith("pt:owner-52") || badge.id.startsWith("ft:")) -> 24.dp
            mini -> 21.dp
            else -> 25.dp
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

    val (accent, softBg) = tierThemeColors(badge.tier)
    Surface(
        color = softBg.copy(alpha = 0.88f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.40f)),
        modifier = modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
    ) {
        Row(
            modifier = Modifier.padding(start = 3.dp, end = 7.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            BadgeArtIcon(
                art = badge.art,
                level = badge.level,
                imageLoader = imageLoader,
                size = 18.dp,
            )
            Text(
                text = badge.name,
                color = accent,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
            )
            if (badge.levelText.isNotBlank()) {
                Text(
                    text = badge.levelText,
                    color = accent.copy(alpha = 0.78f),
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                )
            }
        }
    }
}

/**
 * Renders the user's equipped title (`titleEl`) with authentic shape, fill, font, effect,
 * artwork banner, vector emblem, safe CSS override & `Lv` badge.
 */
@Composable
fun TitleChipView(
    title: ResolvedTitleChip,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val palette = LocalForHappyPalette.current
    val httpClient = LocalCosmeticHttpClient.current

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
        title.specialKind == "raffle_nameplate" -> RoundedCornerShape(topStart = 7.dp, topEnd = 3.dp, bottomEnd = 9.dp, bottomStart = 3.dp)
        title.shape == "rect" -> RoundedCornerShape(4.dp)
        title.shape == "oval" -> RoundedCornerShape(50)
        title.shape == "tag" -> RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 10.dp, bottomEnd = 10.dp)
        title.shape == "blob" -> RoundedCornerShape(topStart = 11.dp, topEnd = 5.dp, bottomEnd = 10.dp, bottomStart = 6.dp)
        title.shape == "ribbon" -> CutCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 8.dp, bottomEnd = 8.dp)
        title.shape == "seal" -> RoundedCornerShape(3.dp)
        else -> RoundedCornerShape(999.dp)
    }

    val hasBorder = title.shape != "none" && fillMode != "none"
    val borderWidth = (css?.borderWidthDp ?: if (title.shape == "seal") 1.5f else 1f).dp
    val borderColorEffective = when (fillMode) {
        "soft" -> bdColor.copy(alpha = 0.48f)
        "glass" -> Color.White.copy(alpha = 0.45f)
        "outline" -> bdColor.copy(alpha = 0.85f)
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

    val loadedBanner = bannerBmp
    val isPv2Banner = title.specialKind == "personal_v2" && loadedBanner != null

    Box(
        modifier = modifier
            .then(
                if (isPv2Banner) {
                    Modifier
                } else {
                    Modifier
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
                        )
                },
            ),
    ) {
        if (loadedBanner != null) {
            Image(
                bitmap = loadedBanner,
                contentDescription = null,
                contentScale = if (isPv2Banner) ContentScale.FillBounds else ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            if (!isPv2Banner) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.10f)),
                )
            }
        }

        if (title.shape == "seal" && hasBorder && !isPv2Banner) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(2.dp)
                    .border(0.6.dp, fgColor.copy(alpha = 0.45f), RoundedCornerShape(1.5.dp)),
            )
        }

        if (shimmerFraction >= 0f && !isPv2Banner) {
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

        val effectiveTextColor = if (isPv2Banner) Color(0xFF304052) else fgColor

        Row(
            modifier = Modifier.padding(
                horizontal = when {
                    isPv2Banner -> 14.dp
                    title.specialKind == "raffle_nameplate" -> 8.dp
                    fillMode == "none" -> 3.dp
                    else -> 7.dp
                },
                vertical = when {
                    isPv2Banner -> 4.dp
                    title.specialKind == "raffle_nameplate" -> 3.dp
                    else -> 2.dp
                },
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (title.specialKind == "raffle_nameplate") 4.dp else 3.dp),
        ) {
            if (title.shape == "tag" && !isPv2Banner && symbolPath == null) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(effectiveTextColor.copy(alpha = 0.72f)),
                )
            }
            if (symbolPath != null) {
                Canvas(modifier = Modifier.size(12.dp)) {
                    scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
                        drawPath(
                            path = symbolPath,
                            color = effectiveTextColor,
                            style = Stroke(width = 2.0f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                        )
                    }
                }
            }
            Text(
                text = title.label,
                color = effectiveTextColor,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontFamily = fontFamily,
                fontWeight = fontWeight,
                fontStyle = fontStyle,
                letterSpacing = letterSpacing,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = if (loadedBanner != null && !isPv2Banner) {
                    TextStyle(
                        shadow = Shadow(color = Color.Black.copy(alpha = 0.72f), offset = Offset(0f, 1f), blurRadius = 2.5f),
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                    )
                } else {
                    TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                },
            )
            if (!title.levelText.isNullOrBlank()) {
                if (title.specialKind == "raffle_nameplate") {
                    val lvBg = parseSafeHex(title.borderHex, effectiveTextColor).copy(
                        alpha = if (palette.isDark) 0.45f else 0.88f,
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(lvBg)
                            .padding(horizontal = 3.5.dp, vertical = 1.dp),
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
                } else {
                    Text(
                        text = title.levelText,
                        color = effectiveTextColor.copy(alpha = 0.88f),
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

fun parseSafeHex(hex: String, fallback: Color): Color {
    val clean = hex.trim().removePrefix("#")
    val parsed = clean.toLongOrNull(16) ?: return fallback
    return when (clean.length) {
        6 -> Color(0xFF000000L or parsed)
        8 -> Color(parsed)
        else -> fallback
    }
}
