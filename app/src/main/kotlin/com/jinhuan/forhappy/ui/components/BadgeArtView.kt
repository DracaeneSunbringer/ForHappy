package com.jinhuan.forhappy.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.DashPathEffect
import android.graphics.ImageDecoder
import android.graphics.LinearGradient
import android.graphics.Movie
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.Build
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
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
import com.jinhuan.forhappy.core.cosmetics.CssVisualAnimTrack
import com.jinhuan.forhappy.core.cosmetics.ResolvedBadgeChip
import com.jinhuan.forhappy.core.cosmetics.ResolvedTitleChip
import com.jinhuan.forhappy.core.network.media.MediaUrlRewriter
import com.jinhuan.forhappy.ui.theme.LocalForHappyPalette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.StringReader
import java.net.URL
import java.net.URLDecoder
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

val LocalCosmeticHttpClient = staticCompositionLocalOf<OkHttpClient?> { null }
val LocalPauseCosmeticAnimations = staticCompositionLocalOf { false }

data class RasterizedArtwork(
    val bitmap: ImageBitmap,
    val intrinsicWidth: Float,
    val intrinsicHeight: Float,
    val animatedBytes: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RasterizedArtwork) return false
        return bitmap == other.bitmap &&
            intrinsicWidth == other.intrinsicWidth &&
            intrinsicHeight == other.intrinsicHeight
    }

    override fun hashCode(): Int {
        var result = bitmap.hashCode()
        result = 31 * result + intrinsicWidth.hashCode()
        result = 31 * result + intrinsicHeight.hashCode()
        return result
    }
}

/**
 * Checks container headers to determine whether raw image bytes contain an animated WebP,
 * multi-frame GIF, or APNG sequence. Pure-byte check usable from JVM unit tests.
 */
internal fun isAnimatedRasterBytes(bytes: ByteArray): Boolean {
    if (bytes.size < 14) return false
    // 1. WebP (`RIFF....WEBP` -> `VP8X` anim flag 0x02 or `ANIM` chunk)
    if (bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
        bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
        bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() &&
        bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()
    ) {
        var pos = 12L
        val limit = min(bytes.size, 8192).toLong()
        while (pos + 8L <= limit) {
            val p = pos.toInt()
            val c0 = bytes[p].toInt().toChar()
            val c1 = bytes[p + 1].toInt().toChar()
            val c2 = bytes[p + 2].toInt().toChar()
            val c3 = bytes[p + 3].toInt().toChar()
            val fourCc = "$c0$c1$c2$c3"
            if (fourCc == "VP8X" && p + 8 < bytes.size) {
                if ((bytes[p + 8].toInt() and 0x02) != 0) return true
            } else if (fourCc == "ANIM") {
                return true
            }
            val chunkLen = (bytes[p + 4].toLong() and 0xFFL) or
                ((bytes[p + 5].toLong() and 0xFFL) shl 8) or
                ((bytes[p + 6].toLong() and 0xFFL) shl 16) or
                ((bytes[p + 7].toLong() and 0xFFL) shl 24)
            if (chunkLen < 0L || chunkLen > bytes.size.toLong()) break
            val padded = (chunkLen + 1L) and 1L.inv()
            pos += 8L + padded
        }
        return false
    }
    // 2. GIF (`GIF87a` / `GIF89a` -> >= 2 image descriptors `0x2C`)
    if (bytes[0] == 'G'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
        bytes[2] == 'F'.code.toByte() && bytes[3] == '8'.code.toByte() &&
        (bytes[4] == '7'.code.toByte() || bytes[4] == '9'.code.toByte()) &&
        bytes[5] == 'a'.code.toByte()
    ) {
        val flags = bytes[10].toInt() and 0xFF
        val gctSize = if ((flags and 0x80) != 0) 3 * (1 shl ((flags and 0x07) + 1)) else 0
        var pos = 13 + gctSize
        var imageCount = 0
        while (pos < bytes.size) {
            when (bytes[pos].toInt() and 0xFF) {
                0x3B -> return false // Trailer
                0x21 -> { // Extension block
                    pos += 2
                    while (pos < bytes.size) {
                        val subLen = bytes[pos].toInt() and 0xFF
                        pos += 1 + subLen
                        if (subLen == 0) break
                    }
                }
                0x2C -> { // Image descriptor
                    imageCount++
                    if (imageCount >= 2) return true
                    if (pos + 9 >= bytes.size) return false
                    val packed = bytes[pos + 9].toInt() and 0xFF
                    val lctSize = if ((packed and 0x80) != 0) 3 * (1 shl ((packed and 0x07) + 1)) else 0
                    pos += 10 + lctSize + 1 // +1 for LZW minimum code size
                    while (pos < bytes.size) {
                        val subLen = bytes[pos].toInt() and 0xFF
                        pos += 1 + subLen
                        if (subLen == 0) break
                    }
                }
                else -> return false
            }
        }
        return false
    }
    // 3. APNG (`\x89PNG\r\n\x1a\n` -> `acTL` chunk before `IDAT`)
    if (bytes.size >= 33 &&
        (bytes[0].toInt() and 0xFF) == 0x89 && bytes[1] == 'P'.code.toByte() &&
        bytes[2] == 'N'.code.toByte() && bytes[3] == 'G'.code.toByte() &&
        bytes[4] == 0x0D.toByte() && bytes[5] == 0x0A.toByte() &&
        bytes[6] == 0x1A.toByte() && bytes[7] == 0x0A.toByte()
    ) {
        var pos = 8L
        while (pos + 12L <= bytes.size.toLong()) {
            val p = pos.toInt()
            val len = ((bytes[p].toLong() and 0xFFL) shl 24) or
                ((bytes[p + 1].toLong() and 0xFFL) shl 16) or
                ((bytes[p + 2].toLong() and 0xFFL) shl 8) or
                (bytes[p + 3].toLong() and 0xFFL)
            val type = "${bytes[p + 4].toInt().toChar()}${bytes[p + 5].toInt().toChar()}${bytes[p + 6].toInt().toChar()}${bytes[p + 7].toInt().toChar()}"
            if (type == "acTL") return true
            if (type == "IDAT" || type == "IEND") return false
            if (len < 0L || len > bytes.size.toLong()) break
            pos += 12L + len
        }
    }
    return false
}

@Composable
fun rememberCosmeticElapsedMs(
    active: Boolean,
    intervalMs: Long = 33L,
): State<Long> {
    val paused = LocalPauseCosmeticAnimations.current
    val elapsed = remember { mutableLongStateOf(0L) }
    LaunchedEffect(active, paused, intervalMs) {
        if (!active || paused) {
            elapsed.longValue = 0L
            return@LaunchedEffect
        }
        val startNs = System.nanoTime()
        var lastEmit = -intervalMs
        while (true) {
            withFrameMillis {
                val nowMs = (System.nanoTime() - startNs) / 1_000_000L
                if (nowMs - lastEmit >= intervalMs) {
                    elapsed.longValue = nowMs
                    lastEmit = nowMs
                }
            }
        }
    }
    return elapsed
}

fun Modifier.applyCssVisualAnim(
    tracks: List<CssVisualAnimTrack>,
    elapsedMsState: State<Long>,
    paused: Boolean,
): Modifier {
    if (tracks.isEmpty() || paused) return this
    return graphicsLayer {
        val eval = ChatBubbleCosmetics.evaluateVisualAnim(tracks, elapsedMsState.value)
        translationX += eval.txDpDelta.dp.toPx() + eval.txSelfDelta * size.width
        translationY += eval.tyDpDelta.dp.toPx() + eval.tySelfDelta * size.height
        scaleX *= eval.scaleX
        scaleY *= eval.scaleY
        rotationZ += eval.rotateDeg
        eval.opacity?.let { alpha *= it }
    }
}

/**
 * Zero-dependency loader, disk cache & rasterizer for bundled `assets/badges/v4/atlas.webp`,
 * `assets/badges/v5/<id>.svg`, `OFFICIAL_ART` SVGs, remote SVGs/WebPs, sprite-sheet cells,
 * and `meme-hall-skins` CSS-embedded data-URI artwork.
 */
object BadgeArtworkCache {
    private val svgCache = ConcurrentHashMap<String, ImageBitmap>()
    private val artworkCache = ConcurrentHashMap<String, RasterizedArtwork>()
    private val failedUrls = ConcurrentHashMap<String, Boolean>()
    private val attemptedCatalogRefreshIds = ConcurrentHashMap<String, Boolean>()

    internal class AnimatedSessionHandle(
        val key: String,
        val baseArt: RasterizedArtwork,
        val targetW: Int,
        val targetH: Int,
    ) {
        val frameBitmapState = mutableStateOf(baseArt.bitmap)
        var refCount: Int = 0
        var job: Job? = null
    }

    private val animatedSessionsLock = Any()
    private val animatedSessions = HashMap<String, AnimatedSessionHandle>()
    private val animatedScope: CoroutineScope by lazy {
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }

    internal fun retainAnimatedSession(cacheKey: String, art: RasterizedArtwork, reqSizePx: Int): AnimatedSessionHandle {
        val rawBytes = art.animatedBytes
        val handle = synchronized(animatedSessionsLock) {
            val existing = animatedSessions[cacheKey]
            val h = if (existing != null) {
                existing
            } else {
                val maxDim = max(art.bitmap.width, art.bitmap.height).coerceAtLeast(1)
                val capDim = min(reqSizePx, 640).coerceAtLeast(1)
                val scale = min(1f, capDim.toFloat() / maxDim.toFloat())
                val tw = (art.bitmap.width * scale).roundToInt().coerceAtLeast(1)
                val th = (art.bitmap.height * scale).roundToInt().coerceAtLeast(1)
                AnimatedSessionHandle(cacheKey, art, tw, th).also { animatedSessions[cacheKey] = it }
            }
            h.refCount++
            h
        }
        if (rawBytes != null && rawBytes.isNotEmpty()) {
            synchronized(animatedSessionsLock) {
                if (handle.job == null && handle.refCount > 0) {
                    handle.job = animatedScope.launch {
                        runAnimatedSessionLoop(handle, rawBytes)
                    }
                }
            }
        }
        return handle
    }

    internal fun releaseAnimatedSession(handle: AnimatedSessionHandle) {
        synchronized(animatedSessionsLock) {
            handle.refCount = (handle.refCount - 1).coerceAtLeast(0)
            if (handle.refCount <= 0) {
                handle.job?.cancel()
                handle.job = null
                animatedSessions.remove(handle.key)
            }
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun runAnimatedSessionLoop(handle: AnimatedSessionHandle, bytes: ByteArray) {
        val tw = handle.targetW
        val th = handle.targetH
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val drawable = withContext(Dispatchers.IO) {
                runCatching {
                    val src = ImageDecoder.createSource(ByteBuffer.wrap(bytes))
                    ImageDecoder.decodeDrawable(src) { decoder, _, _ ->
                        decoder.setTargetSize(tw, th)
                    }
                }.getOrNull()
            } ?: return
            if (drawable is AnimatedImageDrawable) {
                val dummyCb = object : Drawable.Callback {
                    override fun invalidateDrawable(who: Drawable) {}
                    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {}
                    override fun unscheduleDrawable(who: Drawable, what: Runnable) {}
                }
                val bufs = Array(3) { Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888) }
                val canvases = Array(3) { AndroidCanvas(bufs[it]) }
                val imgs = Array(3) { bufs[it].asImageBitmap() }
                var bufIdx = 0
                try {
                    drawable.callback = dummyCb
                    drawable.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                    drawable.setBounds(0, 0, tw, th)
                    drawable.start()
                    var lastDrawMs = -36L
                    val startNs = System.nanoTime()
                    while (true) {
                        withFrameMillis {
                            val nowMs = (System.nanoTime() - startNs) / 1_000_000L
                            if (nowMs - lastDrawMs >= 36L) {
                                lastDrawMs = nowMs
                                bufIdx = (bufIdx + 1) % 3
                                canvases[bufIdx].drawColor(AndroidColor.TRANSPARENT, PorterDuff.Mode.CLEAR)
                                drawable.draw(canvases[bufIdx])
                                handle.frameBitmapState.value = imgs[bufIdx]
                            }
                        }
                    }
                } finally {
                    runCatching {
                        drawable.stop()
                        drawable.callback = null
                    }
                }
            }
            return
        }

        val movie = withContext(Dispatchers.IO) {
            runCatching { Movie.decodeByteArray(bytes, 0, bytes.size) }.getOrNull()
        } ?: return
        val duration = movie.duration().takeIf { it > 0 } ?: 1000
        val mw = movie.width().coerceAtLeast(1)
        val mh = movie.height().coerceAtLeast(1)
        val sx = tw.toFloat() / mw.toFloat()
        val sy = th.toFloat() / mh.toFloat()
        val bufs = Array(3) { Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888) }
        val canvases = Array(3) { AndroidCanvas(bufs[it]) }
        val imgs = Array(3) { bufs[it].asImageBitmap() }
        var bufIdx = 0
        val startNs = System.nanoTime()
        var lastDrawMs = -36L
        while (true) {
            withFrameMillis {
                val nowMs = (System.nanoTime() - startNs) / 1_000_000L
                if (nowMs - lastDrawMs >= 36L) {
                    lastDrawMs = nowMs
                    bufIdx = (bufIdx + 1) % 3
                    movie.setTime((nowMs % duration).toInt())
                    val c = canvases[bufIdx]
                    c.drawColor(AndroidColor.TRANSPARENT, PorterDuff.Mode.CLEAR)
                    c.save()
                    c.scale(sx, sy)
                    movie.draw(c, 0f, 0f)
                    c.restore()
                    handle.frameBitmapState.value = imgs[bufIdx]
                }
            }
        }
    }

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
        if (spec.startsWith("catalog:")) {
            val skinId = spec.removePrefix("catalog:").trim()
            if (skinId.isNotEmpty() && attemptedCatalogRefreshIds.putIfAbsent(skinId, true) == null) {
                withContext(Dispatchers.IO) {
                    val catalogUrl = "https://52dmeme.com/api/community-skins"
                    val reqUrl = MediaUrlRewriter.rewriteMediaUrl(catalogUrl)
                    val catalogBytes = runCatching {
                        if (okHttpClient != null) {
                            val req = Request.Builder().url(reqUrl).get().build()
                            okHttpClient.newCall(req).execute().use { resp ->
                                if (resp.isSuccessful) resp.body?.bytes() else null
                            }
                        } else {
                            URL(reqUrl).openStream().use { it.readBytes() }
                        }
                    }.getOrNull()
                    if (catalogBytes != null && catalogBytes.isNotEmpty()) {
                        BadgeAndCosmeticRegistry.updateCommunitySkinsCatalog(catalogBytes.toString(Charsets.UTF_8))
                    }
                }
            }
            return
        }
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
        val imageUrls = if (isDark) {
            ChatBubbleCosmetics.extractCssImageUrls(cssText, preferDark = true)
        } else {
            ChatBubbleCosmetics.extractAllCssImageUrls(cssText)
        }
        val pickedUrl = when {
            explicitIdx != null -> {
                (imageUrls.getOrNull(explicitIdx)
                    ?: ChatBubbleCosmetics.extractAllCssImageUrls(cssText).getOrNull(explicitIdx))
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
            decodeBytesToArtwork(bytes, "data:raster", reqSizePx)
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
        val animBytes = bytes.takeIf(::isAnimatedRasterBytes)
        return RasterizedArtwork(bmp, bmp.width.toFloat(), bmp.height.toFloat(), animatedBytes = animBytes)
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

@Composable
fun rememberAnimatedCosmeticArtwork(
    source: String?,
    reqSizePx: Int = 512,
): RasterizedArtwork? {
    val context = LocalContext.current
    val httpClient = LocalCosmeticHttpClient.current
    val paused = LocalPauseCosmeticAnimations.current
    val baseArt by produceState<RasterizedArtwork?>(initialValue = null, source, reqSizePx) {
        value = if (source.isNullOrBlank()) {
            null
        } else {
            BadgeArtworkCache.getOrFetchNineSliceArtwork(context, httpClient, source, reqSizePx)
        }
    }
    val loaded = baseArt ?: return null
    val animBytes = loaded.animatedBytes
    if (paused || animBytes == null || source.isNullOrBlank()) return loaded
    val cacheKey = "$source#$reqSizePx"
    val handle = remember(cacheKey, loaded) {
        BadgeArtworkCache.retainAnimatedSession(cacheKey, loaded, reqSizePx)
    }
    DisposableEffect(handle) {
        onDispose { BadgeArtworkCache.releaseAnimatedSession(handle) }
    }
    return loaded.copy(bitmap = handle.frameBitmapState.value)
}

@Composable
fun rememberAnimatedCosmeticBitmap(
    source: String?,
    reqSizePx: Int = 512,
): ImageBitmap? = rememberAnimatedCosmeticArtwork(source, reqSizePx)?.bitmap

@Composable
fun rememberAnimatedCosmeticArtworkMap(
    urls: List<String>,
    reqSizePx: Int = 1024,
): Map<String, RasterizedArtwork> {
    if (urls.isEmpty()) return emptyMap()
    val context = LocalContext.current
    val httpClient = LocalCosmeticHttpClient.current
    val paused = LocalPauseCosmeticAnimations.current
    val baseMap by produceState<Map<String, RasterizedArtwork>>(initialValue = emptyMap(), urls, reqSizePx) {
        val out = LinkedHashMap<String, RasterizedArtwork>(urls.size)
        for (url in urls) {
            BadgeArtworkCache.getOrFetchNineSliceArtwork(context, httpClient, url, reqSizePx)?.let {
                out[url] = it
            }
        }
        value = out
    }
    if (baseMap.isEmpty()) return emptyMap()
    val handles = remember(baseMap, paused, reqSizePx) {
        if (paused) {
            emptyMap()
        } else {
            val map = LinkedHashMap<String, BadgeArtworkCache.AnimatedSessionHandle>()
            for ((url, art) in baseMap) {
                if (art.animatedBytes != null) {
                    map[url] = BadgeArtworkCache.retainAnimatedSession("$url#$reqSizePx", art, reqSizePx)
                }
            }
            map
        }
    }
    DisposableEffect(handles) {
        onDispose {
            handles.values.forEach { BadgeArtworkCache.releaseAnimatedSession(it) }
        }
    }
    if (handles.isEmpty()) return baseMap
    return buildMap(baseMap.size) {
        for ((url, art) in baseMap) {
            val h = handles[url]
            put(url, if (h != null) art.copy(bitmap = h.frameBitmapState.value) else art)
        }
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
            val paused = LocalPauseCosmeticAnimations.current
            val badgeSpec = remember(art.id, art.imgSha, art.cssSha, art.isDarkTheme, catalogRev) {
                ChatBubbleCosmetics.resolveCommunityBadgeSpec(art.id, art.imgSha, art.cssSha, art.isDarkTheme)
            }
            if (badgeSpec == null && !art.cssSha.isNullOrBlank()) {
                LaunchedEffect(art.id, art.cssSha) {
                    BadgeArtworkCache.ensureCommunityCssLoaded(context, httpClient, "${art.id}:badge:${art.cssSha}")
                }
            } else if (badgeSpec == null && art.cssSha.isNullOrBlank() && art.imgSha.isNullOrBlank()) {
                LaunchedEffect(art.id) {
                    BadgeArtworkCache.ensureCommunityCssLoaded(context, httpClient, "catalog:${art.id}")
                }
            }
            val badgeTimeline = badgeSpec?.animationTimeline
            val timelineClock = rememberCosmeticElapsedMs(active = badgeTimeline != null, intervalMs = 45L)
            val visualClock = rememberCosmeticElapsedMs(active = badgeSpec?.visualAnim?.isNotEmpty() == true, intervalMs = 33L)
            val activeFrame = if (!paused && badgeTimeline != null && badgeTimeline.frames.isNotEmpty()) {
                val idx = (((timelineClock.value % badgeTimeline.periodMs).toDouble() / badgeTimeline.periodMs.toDouble()) * badgeTimeline.frames.size)
                    .toInt()
                    .coerceIn(0, badgeTimeline.frames.lastIndex)
                badgeTimeline.frames[idx]
            } else {
                null
            }
            val activeUrlIdx = activeFrame?.baseUrlIdx ?: badgeSpec?.baseUrlIdx ?: 0
            val activeCol = activeFrame?.spriteCol ?: badgeSpec?.spriteCol ?: 0
            val activeRow = activeFrame?.spriteRow ?: badgeSpec?.spriteRow ?: 0
            val activePseudo = activeFrame?.pseudoLayers ?: badgeSpec?.pseudoLayers.orEmpty()
            val frameOpacity = activeFrame?.opacity ?: 1f

            val isDarkVariant = art.isDarkTheme && badgeSpec?.hasDarkImageVariant == true
            val hasBaseCssImg = badgeSpec?.hasBaseImage == true
            val hasAnyImg = badgeSpec?.hasImage == true
            val srcUrl = remember(art.id, art.imgSha, art.cssSha, art.isDarkTheme, isDarkVariant, badgeSpec, activeUrlIdx) {
                when {
                    badgeSpec != null && !hasBaseCssImg && art.imgSha == null -> null
                    badgeSpec != null && hasBaseCssImg && !art.cssSha.isNullOrBlank() ->
                        "community-css:${art.id}:badge:${art.cssSha}:$activeUrlIdx"
                    isDarkVariant && !art.cssSha.isNullOrBlank() ->
                        "community-css-dark:${art.id}:badge:${art.cssSha}:$activeUrlIdx"
                    art.imgSha != null && activeUrlIdx == 0 ->
                        "https://52dmeme.com/api/community-skins/image/${art.imgSha}"
                    !art.cssSha.isNullOrBlank() && (badgeSpec == null || hasAnyImg) ->
                        "community-css:${art.id}:badge:${art.cssSha}:$activeUrlIdx"
                    else -> null
                }
            }
            val isSpriteSheet = badgeSpec != null && (badgeSpec.spriteCols > 1 || badgeSpec.spriteRows > 1)
            val bmp = rememberAnimatedCosmeticBitmap(srcUrl, reqSizePx = if (isSpriteSheet) 1024 else 256)
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
                modifier = modifier
                    .applyCssVisualAnim(badgeSpec?.visualAnim.orEmpty(), visualClock, paused)
                    .padding(start = leftRes, top = topRes, end = rightRes, bottom = bottomRes),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier.width(targetW).height(size),
                    contentAlignment = Alignment.Center,
                ) {
                    bmp?.let { sheet ->
                        if (isSpriteSheet && frameOpacity > 0.01f) {
                            val safeCols = max(1, badgeSpec!!.spriteCols)
                            val safeRows = max(1, badgeSpec.spriteRows)
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val cellW = max(1, sheet.width / safeCols)
                                val cellH = max(1, sheet.height / safeRows)
                                val sx = (activeCol.coerceIn(0, safeCols - 1) * cellW).coerceIn(0, max(0, sheet.width - 1))
                                val sy = (activeRow.coerceIn(0, safeRows - 1) * cellH).coerceIn(0, max(0, sheet.height - 1))
                                val cw = min(cellW, sheet.width - sx).coerceAtLeast(1)
                                val ch = min(cellH, sheet.height - sy).coerceAtLeast(1)
                                drawImage(
                                    image = sheet,
                                    srcOffset = IntOffset(sx, sy),
                                    srcSize = IntSize(cw, ch),
                                    dstSize = IntSize(this.size.width.roundToInt().coerceAtLeast(1), this.size.height.roundToInt().coerceAtLeast(1)),
                                    alpha = frameOpacity,
                                    filterQuality = FilterQuality.Medium,
                                )
                            }
                        } else if (frameOpacity > 0.01f) {
                            Image(
                                bitmap = sheet,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                alpha = frameOpacity,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                    activePseudo.forEach { layer ->
                        BubblePseudoLayerCanvas(layer = layer, modifier = Modifier.matchParentSize())
                    }
                }
            }
        }

        is BadgeArtKind.RemoteImage -> {
            val paused = LocalPauseCosmeticAnimations.current
            if (art.url.startsWith("community-css") || paused) {
                val bmp = if (art.url.startsWith("community-css") && !paused) {
                    rememberAnimatedCosmeticBitmap(art.url, 128)
                } else {
                    val staticBmp by produceState<ImageBitmap?>(initialValue = null, art.url) {
                        value = BadgeArtworkCache.getOrFetchBitmap(context, httpClient, art.url, 128)
                    }
                    staticBmp
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

internal fun createRotatingSweepBrush(
    colors: List<Color>,
    angleDeg: Float,
    size: Size,
): Brush {
    if (colors.size < 2) return SolidColor(colors.firstOrNull() ?: Color.Transparent)
    val closed = if (colors.first() == colors.last()) colors else colors + colors.first()
    val intColors = IntArray(closed.size) { closed[it].toArgb() }
    val cx = size.width * 0.5f
    val cy = size.height * 0.5f
    val shader = android.graphics.SweepGradient(cx, cy, intColors, null)
    if (angleDeg != 0f) {
        val m = android.graphics.Matrix()
        m.setRotate(angleDeg, cx, cy)
        shader.setLocalMatrix(m)
    }
    return ShaderBrush(shader)
}

@Composable
private fun rememberTitleEffectMotionModifier(
    effect: String,
    paused: Boolean,
): Modifier {
    val norm = effect.trim().lowercase()
    if (paused) return Modifier
    return when (norm) {
        "pulse", "glow", "breathe" -> {
            val tr = rememberInfiniteTransition(label = "title_pulse")
            val scale by tr.animateFloat(
                initialValue = 1f,
                targetValue = 1.032f,
                animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
                label = "scale",
            )
            Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
        }
        "bounce", "float" -> {
            val tr = rememberInfiniteTransition(label = "title_float")
            val dy by tr.animateFloat(
                initialValue = -1.4f,
                targetValue = 1.4f,
                animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
                label = "dy",
            )
            Modifier.graphicsLayer { translationY = dy.dp.toPx() }
        }
        "shake", "tremor" -> {
            val tr = rememberInfiniteTransition(label = "title_shake")
            val phase by tr.animateFloat(
                initialValue = 0f,
                targetValue = (2.0 * PI).toFloat(),
                animationSpec = infiniteRepeatable(tween(260, easing = LinearEasing), RepeatMode.Restart),
                label = "shake_phase",
            )
            Modifier.graphicsLayer {
                translationX = sin(phase) * 0.9.dp.toPx()
                rotationZ = cos(phase) * 1.1f
            }
        }
        else -> Modifier
    }
}

@Composable
private fun TitleEffectOverlay(
    effect: String,
    paused: Boolean,
    maskToContent: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val norm = effect.trim().lowercase()
    if (paused || norm.isEmpty() || norm == "none") return
    val blend = if (maskToContent) BlendMode.SrcAtop else BlendMode.SrcOver
    when (norm) {
        "shine" -> {
            val tr = rememberInfiniteTransition(label = "title_shine")
            val frac by tr.animateFloat(
                initialValue = -0.35f,
                targetValue = 1.35f,
                animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart),
                label = "shine_frac",
            )
            Canvas(modifier = modifier) {
                val band = max(size.width * 0.28f, 10f)
                val cx = frac * (size.width + band * 2f) - band
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.28f), Color.Transparent),
                        start = Offset(cx - band, 0f),
                        end = Offset(cx + band, size.height),
                    ),
                    blendMode = blend,
                )
            }
        }
        "flow", "aurora", "rainbow" -> {
            val tr = rememberInfiniteTransition(label = "title_flow")
            val frac by tr.animateFloat(
                initialValue = -0.5f,
                targetValue = 1.5f,
                animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
                label = "flow_frac",
            )
            Canvas(modifier = modifier) {
                val band = max(size.width * 0.45f, 16f)
                val cx = frac * (size.width + band * 2f) - band
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF00F0FF).copy(alpha = 0.22f),
                            Color(0xFFC084FC).copy(alpha = 0.24f),
                            Color(0xFFFF6EC7).copy(alpha = 0.22f),
                            Color.Transparent,
                        ),
                        start = Offset(cx - band, 0f),
                        end = Offset(cx + band, size.height),
                    ),
                    blendMode = blend,
                )
            }
        }
        "pulse", "glow", "breathe" -> {
            val tr = rememberInfiniteTransition(label = "title_glow")
            val alpha by tr.animateFloat(
                initialValue = 0.04f,
                targetValue = 0.20f,
                animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
                label = "glow_alpha",
            )
            Canvas(modifier = modifier) {
                drawRect(
                    color = Color(0xFFFFF3C4).copy(alpha = alpha),
                    blendMode = blend,
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
    val paused = LocalPauseCosmeticAnimations.current

    title.dynamicCssSpec?.let { cssSpec ->
        LaunchedEffect(cssSpec) {
            BadgeArtworkCache.ensureCommunityCssLoaded(context, httpClient, cssSpec)
        }
    }

    if (title.specialKind == "owner52" && title.bgImageUrl != null) {
        val loadedOwnerArt = rememberAnimatedCosmeticBitmap(title.bgImageUrl, 256)
        if (loadedOwnerArt != null) {
            Box(
                modifier = modifier
                    .then(rememberTitleEffectMotionModifier(title.effect, paused))
                    .then(
                        if (!paused && title.effect.isNotBlank() && title.effect != "none") {
                            Modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        } else {
                            Modifier
                        },
                    ),
            ) {
                Image(
                    bitmap = loadedOwnerArt,
                    contentDescription = title.label,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .width(52.dp)
                        .height(28.dp),
                )
                TitleEffectOverlay(
                    effect = title.effect,
                    paused = paused,
                    maskToContent = true,
                    modifier = Modifier.matchParentSize(),
                )
            }
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

    val loadedBanner = rememberAnimatedCosmeticBitmap(title.bgImageUrl, 256)
    val leadingIconBmp = rememberAnimatedCosmeticBitmap(title.leadingIconUrl, 96)
    val trailingIconBmp = rememberAnimatedCosmeticBitmap(title.trailingIconUrl, 96)

    val symbolPath = remember(title.symbolSvgPath) {
        title.symbolSvgPath?.let { rawPath ->
            runCatching { PathParser.createPathFromPathData(rawPath).asComposePath() }.getOrNull()
        }
    }

    val hardShadowColor = title.hardShadowHex?.let { parseSafeHex(it, Color.Transparent) }?.takeIf { it != Color.Transparent }
    val hardShadowOff = title.hardShadowOffsetDp
    val motionMod = rememberTitleEffectMotionModifier(title.effect, paused)

    Box(
        modifier = modifier
            .then(motionMod)
            .then(
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

            TitleEffectOverlay(
                effect = title.effect,
                paused = paused,
                maskToContent = false,
                modifier = Modifier.matchParentSize(),
            )

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
    val paused = LocalPauseCosmeticAnimations.current
    val timeline = spec.animationTimeline
    val timelineClock = rememberCosmeticElapsedMs(active = timeline != null, intervalMs = 60L)
    val visualClock = rememberCosmeticElapsedMs(active = spec.visualAnim.isNotEmpty(), intervalMs = 33L)
    val activeFrame = if (!paused && timeline != null && timeline.frames.isNotEmpty()) {
        val idx = (((timelineClock.value % timeline.periodMs).toDouble() / timeline.periodMs.toDouble()) * timeline.frames.size)
            .toInt()
            .coerceIn(0, timeline.frames.lastIndex)
        timeline.frames[idx]
    } else {
        null
    }

    val effBgColors = activeFrame?.bgColors ?: spec.bgColors
    val effBgAngleDeg = activeFrame?.bgAngleDeg ?: spec.bgAngleDeg
    val effBorderColor = activeFrame?.borderColor ?: spec.borderColor
    val effBorderGradColors = activeFrame?.borderGradientColors ?: spec.borderGradientColors
    val effTextColor = activeFrame?.textColor ?: spec.textColor
    val effNineSlice = activeFrame?.nineSliceSpec ?: spec.nineSliceSpec
    val effPseudoLayers = activeFrame?.pseudoLayers ?: spec.pseudoLayers
    val effLeadingUrl = activeFrame?.leadingIconUrl ?: title.leadingIconUrl
    val effTrailingUrl = activeFrame?.trailingIconUrl ?: title.trailingIconUrl

    val leadingIconBmp = rememberAnimatedCosmeticBitmap(effLeadingUrl, 96)
    val trailingIconBmp = rememberAnimatedCosmeticBitmap(effTrailingUrl, 96)

    val startRes = max(spec.startOverflowReserveDp, (effNineSlice?.outsetLeftDp ?: 0f).coerceAtLeast(0f)).dp
    val topRes = max(spec.topOverflowReserveDp, (effNineSlice?.outsetTopDp ?: 0f).coerceAtLeast(0f)).dp
    val endRes = max(spec.endOverflowReserveDp, (effNineSlice?.outsetRightDp ?: 0f).coerceAtLeast(0f)).dp
    val bottomRes = max(spec.bottomOverflowReserveDp, (effNineSlice?.outsetBottomDp ?: 0f).coerceAtLeast(0f)).dp

    val r = spec.cornerRadiiDp
    val shape = RoundedCornerShape(
        topStart = r.getOrElse(0) { 999f }.dp,
        topEnd = r.getOrElse(1) { 999f }.dp,
        bottomEnd = r.getOrElse(2) { 999f }.dp,
        bottomStart = r.getOrElse(3) { 999f }.dp,
    )
    val hasNineSlice = effNineSlice != null
    val hasGradBorder = !hasNineSlice && spec.borderWidthDp > 0f && effBorderGradColors != null && effBorderGradColors.size >= 2
    val borderSweepAngle = if (hasGradBorder && !paused) {
        val tr = rememberInfiniteTransition(label = "comm_title_border")
        val a by tr.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart),
            label = "sweep_angle",
        )
        a
    } else {
        0f
    }

    val bgModifier = when {
        hasNineSlice -> Modifier
        effBgColors.size >= 2 -> Modifier.drawBehind {
            val angleRad = ((effBgAngleDeg ?: 135f) - 90f) * PI / 180.0
            val dx = cos(angleRad).toFloat()
            val dy = sin(angleRad).toFloat()
            val cx = size.width * 0.5f
            val cy = size.height * 0.5f
            val halfSpan = max(size.width, size.height) * 0.5f
            drawRect(
                brush = Brush.linearGradient(
                    colors = effBgColors,
                    start = Offset(cx - dx * halfSpan, cy - dy * halfSpan),
                    end = Offset(cx + dx * halfSpan, cy + dy * halfSpan),
                ),
            )
        }
        effBgColors.size == 1 -> Modifier.background(effBgColors[0])
        else -> Modifier
    }

    Box(
        modifier = modifier
            .applyCssVisualAnim(spec.visualAnim, visualClock, paused)
            .then(rememberTitleEffectMotionModifier(title.effect, paused))
            .padding(start = startRes, top = topRes, end = endRes, bottom = bottomRes),
    ) {
        Box(
            modifier = Modifier
                .then(
                    spec.fixedHeightDp?.let { Modifier.height(it.dp) }
                        ?: Modifier.defaultMinSize(minHeight = spec.minHeightDp.dp),
                )
                .then(if (hasNineSlice) Modifier else Modifier.clip(shape))
                .then(bgModifier)
                .then(
                    if (!hasNineSlice && spec.borderWidthDp > 0f) {
                        when {
                            hasGradBorder -> Modifier.drawBehind {
                                val bwPx = spec.borderWidthDp.dp.toPx()
                                drawOutline(
                                    outline = shape.createOutline(size, layoutDirection, this),
                                    brush = createRotatingSweepBrush(effBorderGradColors!!, borderSweepAngle, size),
                                    style = Stroke(width = bwPx * 2f),
                                )
                            }
                            effBorderColor != Color.Transparent ->
                                Modifier.border(spec.borderWidthDp.dp, effBorderColor, shape)
                            else -> Modifier
                        }
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            effNineSlice?.let { ns ->
                NineSliceFrameCanvas(spec = ns, modifier = Modifier.matchParentSize())
            }
            effPseudoLayers.forEach { layer ->
                BubblePseudoLayerCanvas(layer = layer, modifier = Modifier.matchParentSize())
            }
            TitleEffectOverlay(
                effect = title.effect,
                paused = paused,
                maskToContent = false,
                modifier = Modifier.matchParentSize(),
            )
            val effPadLeft = if (leadingIconBmp != null) (spec.padLeftDp - 16f).coerceAtLeast(5f) else spec.padLeftDp
            val effPadRight = if (trailingIconBmp != null) (spec.padRightDp - 16f).coerceAtLeast(5f) else spec.padRightDp
            Row(
                modifier = Modifier.padding(
                    start = effPadLeft.dp,
                    top = spec.padTopDp.dp,
                    end = effPadRight.dp,
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
                    color = effTextColor,
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
    val paused = LocalPauseCosmeticAnimations.current
    val artUrl = title.bgImageUrl ?: return
    val loaded = rememberAnimatedCosmeticBitmap(artUrl, 256)
    val spec = title.personalV2Art ?: ChatBubbleCosmetics.resolvePersonalV2Art(title.key.removePrefix("personal:"))
    val motionMod = rememberTitleEffectMotionModifier(title.effect, paused)

    if (loaded == null) {
        Box(
            modifier = modifier
                .then(motionMod)
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

    Box(
        modifier = modifier
            .then(motionMod)
            .then(
                if (!paused && title.effect.isNotBlank() && title.effect != "none") {
                    Modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                } else {
                    Modifier
                },
            ),
    ) {
        Layout(
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
        TitleEffectOverlay(
            effect = title.effect,
            paused = paused,
            maskToContent = true,
            modifier = Modifier.matchParentSize(),
        )
    }
}

@Composable
private fun ShiguangTitleChipBox(
    title: ResolvedTitleChip,
    modifier: Modifier = Modifier,
) {
    val paused = LocalPauseCosmeticAnimations.current
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
        modifier = modifier
            .then(rememberTitleEffectMotionModifier(title.effect, paused))
            .height(23.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .clip(bodyShape)
                .background(Brush.verticalGradient(listOf(Color(0xFFFAF0DC), Color(0xFFEAD3AE))))
                .border(1.dp, Color(0xFFB8894A), bodyShape),
            contentAlignment = Alignment.Center,
        ) {
            TitleEffectOverlay(
                effect = title.effect,
                paused = paused,
                maskToContent = false,
                modifier = Modifier.matchParentSize(),
            )
            Text(
                text = title.label,
                color = Color(0xFF542710),
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                modifier = Modifier.padding(start = 10.dp, end = 9.dp),
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
    val paused = LocalPauseCosmeticAnimations.current
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

    val bannerSpec = remember(title.key) {
        ChatBubbleCosmetics.RAFFLE_TITLE_BANNERS[title.key.removePrefix("raffle:")]
    }
    val bannerUrl = if (bannerSpec != null) title.bgImageUrl else null
    val motionMod = rememberTitleEffectMotionModifier(title.effect, paused)
    if (bannerSpec != null && !bannerUrl.isNullOrBlank()) {
        val loadedBanner = rememberAnimatedCosmeticBitmap(bannerUrl, 256)
        if (loadedBanner != null) {
            val ink = parseSafeHex(bannerSpec.inkHex, fgColor)
            Row(
                modifier = modifier.then(motionMod),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                val bannerUseOffscreen = !paused && title.effect.isNotBlank() && title.effect != "none"
                Box(
                    modifier = if (bannerUseOffscreen) {
                        Modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    } else {
                        Modifier
                    },
                ) {
                    Layout(
                        content = {
                            Image(
                                bitmap = loadedBanner,
                                contentDescription = null,
                                contentScale = ContentScale.FillBounds,
                            )
                            Text(
                                text = title.label,
                                color = ink,
                                fontSize = 11.sp,
                                lineHeight = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                            )
                        },
                    ) { measurables, constraints ->
                        val chipH = bannerSpec.heightDp.dp.roundToPx().coerceIn(constraints.minHeight, constraints.maxHeight)
                        val padL = bannerSpec.padLeftDp.dp.roundToPx()
                        val padR = bannerSpec.padRightDp.dp.roundToPx()
                        val textPlaceable = measurables[1].measure(
                            constraints.copy(minWidth = 0, minHeight = 0, maxHeight = chipH),
                        )
                        val aspectW = (loadedBanner.width.toFloat() / loadedBanner.height.coerceAtLeast(1).toFloat() * chipH).roundToInt()
                        val minW = max(bannerSpec.minWidthDp.dp.roundToPx(), aspectW)
                        val targetW = max(textPlaceable.width + padL + padR, minW)
                            .coerceIn(constraints.minWidth, constraints.maxWidth)
                        val imgPlaceable = measurables[0].measure(Constraints.fixed(targetW, chipH))
                        layout(targetW, chipH) {
                            imgPlaceable.placeRelative(0, 0)
                            val availW = (targetW - textPlaceable.width).coerceAtLeast(0)
                            val safeL = padL.coerceAtMost(availW)
                            val safeMaxL = (targetW - textPlaceable.width - padR).coerceAtLeast(safeL)
                            val tx = (targetW * bannerSpec.titleXFraction - textPlaceable.width * 0.5f)
                                .roundToInt()
                                .coerceIn(safeL, safeMaxL)
                            val ty = (chipH * bannerSpec.titleYFraction - textPlaceable.height * 0.5f)
                                .roundToInt()
                                .coerceIn(0, (chipH - textPlaceable.height).coerceAtLeast(0))
                            textPlaceable.placeRelative(tx, ty)
                        }
                    }
                    TitleEffectOverlay(
                        effect = title.effect,
                        paused = paused,
                        maskToContent = true,
                        modifier = Modifier.matchParentSize(),
                    )
                }
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
            return
        }
    }

    val symbolPath = remember(title.symbolSvgPath) {
        title.symbolSvgPath?.let { rawPath ->
            runCatching { PathParser.createPathFromPathData(rawPath).asComposePath() }.getOrNull()
        }
    }

    Box(
        modifier = modifier
            .then(motionMod)
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
        TitleEffectOverlay(
            effect = title.effect,
            paused = paused,
            maskToContent = false,
            modifier = Modifier.matchParentSize(),
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
