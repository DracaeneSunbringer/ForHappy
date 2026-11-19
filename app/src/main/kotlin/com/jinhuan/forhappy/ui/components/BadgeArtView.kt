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
import android.util.Xml
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.jinhuan.forhappy.core.cosmetics.ResolvedBadgeChip
import com.jinhuan.forhappy.core.cosmetics.ResolvedTitleChip
import com.jinhuan.forhappy.core.network.media.MediaUrlRewriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Zero-dependency loader & rasterizer for bundled `assets/badges/v4/atlas.webp` (6x8 sprite sheet)
 * and `assets/badges/v5/<id>.svg` / `resource-respect.svg` / `OFFICIAL_ART` vector badges.
 */
object BadgeArtworkCache {
    private val svgCache = ConcurrentHashMap<String, ImageBitmap>()

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
        val hasTransform: Boolean,
    )

    private val NAMED_COLORS = mapOf(
        "white" to AndroidColor.WHITE,
        "black" to AndroidColor.BLACK,
        "transparent" to AndroidColor.TRANSPARENT,
    )

    private val TRANSLATE_RE = Regex("""translate\(\s*([+-]?\d*\.?\d+)(?:[\s,]+([+-]?\d*\.?\d+))?\s*\)""")
    private val SCALE_RE = Regex("""scale\(\s*([+-]?\d*\.?\d+)(?:[\s,]+([+-]?\d*\.?\d+))?\s*\)""")

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
                            val vb = parser.getAttributeValue(null, "viewBox")
                            if (!vb.isNullOrBlank()) {
                                val nums = vb.trim().split(Regex("""[\s,]+""")).mapNotNull { it.toFloatOrNull() }
                                if (nums.size >= 4) {
                                    viewW = max(1f, nums[2])
                                    viewH = max(1f, nums[3])
                                }
                            }
                            canvas.scale(sizePx / viewW, sizePx / viewH)
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
                            val sc = parseSvgColor(parser.getAttributeValue(null, "stop-color")) ?: AndroidColor.WHITE
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

                        "path", "circle", "ellipse", "rect" -> if (!inDefs) {
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
        return StyleFrame(
            fill = fill,
            stroke = stroke,
            strokeWidth = sw,
            strokeCap = cap,
            strokeJoin = join,
            opacity = (parent.opacity * op).coerceIn(0f, 1f),
            hasTransform = hasTf,
        )
    }

    private fun applyTransform(canvas: AndroidCanvas, raw: String) {
        TRANSLATE_RE.find(raw)?.let { m ->
            val tx = m.groupValues[1].toFloatOrNull() ?: 0f
            val ty = m.groupValues[2].toFloatOrNull() ?: 0f
            canvas.translate(tx, ty)
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
        val alphaInt = (st.opacity * 255f).roundToInt().coerceIn(0, 255)
        if (!st.fill.isNullOrBlank() && st.fill != "none") {
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            }
            if (applyPaintColorOrShader(fillPaint, st.fill, bounds, grads, alphaInt)) {
                canvas.drawPath(path, fillPaint)
            }
        }
        if (!st.stroke.isNullOrBlank() && st.stroke != "none" && st.strokeWidth > 0f) {
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = st.strokeWidth
                strokeCap = st.strokeCap
                strokeJoin = st.strokeJoin
            }
            if (applyPaintColorOrShader(strokePaint, st.stroke, bounds, grads, alphaInt)) {
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
            val rewritten = remember(art.url) { MediaUrlRewriter.rewriteMediaUrl(art.url) }
            AsyncImage(
                model = rewritten,
                imageLoader = imageLoader,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = modifier
                    .size(size)
                    .clip(CircleShape),
            )
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
    if (mini) {
        BadgeArtIcon(
            art = badge.art,
            level = badge.level,
            imageLoader = imageLoader,
            size = 19.dp,
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
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (badge.levelText.isNotBlank()) {
                Text(
                    text = badge.levelText,
                    color = accent.copy(alpha = 0.78f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/**
 * Renders the user's equipped title (`titleEl`) with authentic fill, border, rarity & `Lv` badge.
 */
@Composable
fun TitleChipView(
    title: ResolvedTitleChip,
    modifier: Modifier = Modifier,
) {
    val bgColor = parseSafeHex(title.bgHex, Color(0xFF1F6B4F))
    val fgColor = parseSafeHex(title.textHex, Color.White)
    val bdColor = parseSafeHex(title.borderHex, bgColor)
    val shape = when (title.shape) {
        "tag" -> RoundedCornerShape(5.dp)
        "ribbon" -> RoundedCornerShape(topStart = 10.dp, bottomEnd = 10.dp, topEnd = 3.dp, bottomStart = 3.dp)
        else -> RoundedCornerShape(999.dp)
    }
    val isOutline = title.fillMode == "outline"

    Surface(
        color = if (isOutline) bgColor.copy(alpha = 0.12f) else bgColor,
        shape = shape,
        border = BorderStroke(1.dp, bdColor.copy(alpha = if (isOutline) 0.75f else 0.55f)),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = title.label,
                color = if (isOutline) bgColor else fgColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!title.levelText.isNullOrBlank()) {
                Text(
                    text = title.levelText,
                    color = (if (isOutline) bgColor else fgColor).copy(alpha = 0.85f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
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
