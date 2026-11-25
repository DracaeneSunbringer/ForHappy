package com.jinhuan.forhappy.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import com.jinhuan.forhappy.core.cosmetics.BgScaleMode
import com.jinhuan.forhappy.core.cosmetics.BubbleBgImageSpec
import com.jinhuan.forhappy.core.cosmetics.BubbleOrnament
import com.jinhuan.forhappy.core.cosmetics.BubblePseudoLayer
import com.jinhuan.forhappy.core.cosmetics.BubbleShapeKind
import com.jinhuan.forhappy.core.cosmetics.ChatBubbleCosmetics
import com.jinhuan.forhappy.core.cosmetics.NineSliceSpec
import com.jinhuan.forhappy.core.cosmetics.ResolvedChatBubbleStyle
import com.jinhuan.forhappy.core.network.media.MediaUrlRewriter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Provides dynamic timeline `(textColor, linkColor, mutedColor)` for animating bubbles (e.g. `rem-dual-form`)
 * without forcing recomposition of message metadata headers.
 */
val LocalAnimatedBubbleColors = compositionLocalOf<Triple<Color, Color, Color>?> { null }

/**
 * Draws a 9-slice artwork onto this [DrawScope] relative to `(boxX, boxY, boxW, boxH)` with CSS
 * `border-image-outset` expansion and proportional clamping on compact short-text bubbles.
 */
internal fun DrawScope.drawNineSliceArtwork(
    spec: NineSliceSpec,
    loadedArt: RasterizedArtwork,
    boxX: Float,
    boxY: Float,
    boxW: Float,
    boxH: Float,
    alpha: Float = 1f,
) {
    val loaded = loadedArt.bitmap
    val bw = loaded.width
    val bh = loaded.height
    val oTop = spec.outsetTopDp.dp.toPx().coerceAtLeast(0f)
    val oRight = spec.outsetRightDp.dp.toPx().coerceAtLeast(0f)
    val oBottom = spec.outsetBottomDp.dp.toPx().coerceAtLeast(0f)
    val oLeft = spec.outsetLeftDp.dp.toPx().coerceAtLeast(0f)

    val ox = (boxX - oLeft).roundToInt()
    val oy = (boxY - oTop).roundToInt()
    val cw = (boxW + oLeft + oRight).roundToInt()
    val ch = (boxH + oTop + oBottom).roundToInt()
    if (bw <= 0 || bh <= 0 || cw <= 0 || ch <= 0) return

    val scaleX = bw.toFloat() / loadedArt.intrinsicWidth.coerceAtLeast(1f)
    val scaleY = bh.toFloat() / loadedArt.intrinsicHeight.coerceAtLeast(1f)
    fun slicePx(v: Float, bmpTotal: Int, axisScale: Float): Int {
        if (spec.isPercentSlice) {
            val ratio = if (v <= 1f) v else v / 100f
            return (bmpTotal * ratio).roundToInt().coerceAtLeast(0)
        }
        return (v * axisScale).roundToInt().coerceAtLeast(0)
    }
    var sTop = slicePx(spec.sliceTop, bh, scaleY)
    var sBottom = slicePx(spec.sliceBottom, bh, scaleY)
    var sLeft = slicePx(spec.sliceLeft, bw, scaleX)
    var sRight = slicePx(spec.sliceRight, bw, scaleX)

    if (sLeft + sRight >= bw) {
        val scale = (bw - 1).coerceAtLeast(1).toFloat() / (sLeft + sRight).coerceAtLeast(1)
        sLeft = (sLeft * scale).roundToInt().coerceAtLeast(0)
        sRight = (bw - 1 - sLeft).coerceAtLeast(0)
    }
    if (sTop + sBottom >= bh) {
        val scale = (bh - 1).coerceAtLeast(1).toFloat() / (sTop + sBottom).coerceAtLeast(1)
        sTop = (sTop * scale).roundToInt().coerceAtLeast(0)
        sBottom = (bh - 1 - sTop).coerceAtLeast(0)
    }
    val sCenterW = (bw - sLeft - sRight).coerceAtLeast(0)
    val sCenterH = (bh - sTop - sBottom).coerceAtLeast(0)

    var dTopF = spec.borderTopDp.dp.toPx().coerceAtLeast(0f)
    var dBottomF = spec.borderBottomDp.dp.toPx().coerceAtLeast(0f)
    var dLeftF = spec.borderLeftDp.dp.toPx().coerceAtLeast(0f)
    var dRightF = spec.borderRightDp.dp.toPx().coerceAtLeast(0f)

    if (dLeftF + dRightF > cw.toFloat() && (dLeftF + dRightF) > 0f) {
        val scale = cw.toFloat() / (dLeftF + dRightF)
        dLeftF *= scale
        dRightF *= scale
    }
    if (dTopF + dBottomF > ch.toFloat() && (dTopF + dBottomF) > 0f) {
        val scale = ch.toFloat() / (dTopF + dBottomF)
        dTopF *= scale
        dBottomF *= scale
    }

    val lW = dLeftF.roundToInt().coerceIn(0, cw)
    val rW = dRightF.roundToInt().coerceIn(0, cw - lW)
    val cW = (cw - lW - rW).coerceAtLeast(0)

    val tH = dTopF.roundToInt().coerceIn(0, ch)
    val bH = dBottomF.roundToInt().coerceIn(0, ch - tH)
    val cH = (ch - tH - bH).coerceAtLeast(0)

    fun patch(sx: Int, sy: Int, sw: Int, sh: Int, dx: Int, dy: Int, dw: Int, dh: Int) {
        if (sw <= 0 || sh <= 0 || dw <= 0 || dh <= 0 || alpha <= 0.005f) return
        drawImage(
            image = loaded,
            srcOffset = IntOffset(sx, sy),
            srcSize = IntSize(sw, sh),
            dstOffset = IntOffset(ox + dx, oy + dy),
            dstSize = IntSize(dw, dh),
            alpha = alpha.coerceIn(0f, 1f),
            filterQuality = FilterQuality.Medium,
        )
    }

    // Top row
    patch(0, 0, sLeft, sTop, 0, 0, lW, tH)
    patch(sLeft, 0, sCenterW, sTop, lW, 0, cW, tH)
    patch(sLeft + sCenterW, 0, sRight, sTop, lW + cW, 0, rW, tH)

    // Middle row
    patch(0, sTop, sLeft, sCenterH, 0, tH, lW, cH)
    if (spec.fillCenter && !spec.isEdgeTrimOnly) {
        patch(sLeft, sTop, sCenterW, sCenterH, lW, tH, cW, cH)
    }
    patch(sLeft + sCenterW, sTop, sRight, sCenterH, lW + cW, tH, rW, cH)

    // Bottom row
    patch(0, sTop + sCenterH, sLeft, sBottom, 0, tH + cH, lW, bH)
    patch(sLeft, sTop + sCenterH, sCenterW, sBottom, lW, tH + cH, cW, bH)
    patch(sLeft + sCenterW, sTop + sCenterH, sRight, sBottom, lW + cW, tH + cH, rW, bH)
}

/**
 * Renders 9-slice bitmap borders (`NineSliceSpec`) with proportional clamping on compact
 * short-text bubbles (`"1"`, `"?"`) so edge frames never invert or overlap.
 */
@Composable
fun NineSliceFrameCanvas(
    spec: NineSliceSpec,
    fallbackColor: Color = Color.Transparent,
    alpha: Float = 1f,
    modifier: Modifier = Modifier,
) {
    val loadedArt = rememberAnimatedCosmeticArtwork(spec.sourceUrl, reqSizePx = 1024)
    if (loadedArt == null) {
        if (fallbackColor != Color.Transparent) {
            Box(modifier = modifier.clip(RoundedCornerShape(14.dp)).background(fallbackColor))
        }
        return
    }
    Canvas(modifier = modifier) {
        drawNineSliceArtwork(
            spec = spec,
            loadedArt = loadedArt,
            boxX = 0f,
            boxY = 0f,
            boxW = size.width,
            boxH = size.height,
            alpha = alpha,
        )
    }
}

@Composable
internal fun BubblePseudoLayerCanvas(
    layer: BubblePseudoLayer,
    modifier: Modifier = Modifier,
) {
    val paused = LocalPauseCosmeticAnimations.current
    val timeline = layer.pseudoTimeline
    val allUrls = remember(layer) {
        buildSet {
            layer.bgImagesBottomToTop.forEach { add(it.sourceUrl) }
            layer.nineSliceSpec?.sourceUrl?.let { add(it) }
            timeline?.frames?.forEach { frame ->
                frame.bgImagesBottomToTop.forEach { add(it.sourceUrl) }
                frame.nineSliceSpec?.sourceUrl?.let { add(it) }
            }
        }.toList()
    }
    val hasPureShape = remember(layer) {
        layer.shapeFillColor != null || layer.shapeBorderColor != null || layer.shadowDot != null ||
            (timeline?.frames?.any { it.shapeFillColor != null || it.shapeBorderColor != null || it.shadowDot != null } == true)
    }
    val artMap = rememberAnimatedCosmeticArtworkMap(allUrls, reqSizePx = 1024)
    if (artMap.isEmpty() && !hasPureShape) return

    val timelineClock = rememberCosmeticElapsedMs(active = timeline != null, intervalMs = 50L)
    val activeLayer = if (!paused && timeline != null && timeline.frames.isNotEmpty()) {
        val idx = (((timelineClock.value % timeline.periodMs).toDouble() / timeline.periodMs.toDouble()) * timeline.frames.size)
            .toInt()
            .coerceIn(0, timeline.frames.lastIndex)
        timeline.frames[idx]
    } else {
        layer
    }
    val activeTracks = if (activeLayer.visualAnim.isNotEmpty()) activeLayer.visualAnim else layer.visualAnim
    val visualClock = rememberCosmeticElapsedMs(active = activeTracks.isNotEmpty(), intervalMs = 33L)

    Canvas(modifier = modifier) {
        val evalAnim = if (!paused && activeTracks.isNotEmpty()) {
            ChatBubbleCosmetics.evaluateVisualAnim(activeTracks, visualClock.value)
        } else {
            null
        }
        val effAlpha = (evalAnim?.opacity ?: activeLayer.opacity).coerceIn(0f, 1f)
        if (effAlpha <= 0.01f) return@Canvas

        val rawH = when {
            activeLayer.heightDp != null -> activeLayer.heightDp.dp.toPx().let { h ->
                if (activeLayer.heightCapToParent) min(h, size.height) else h
            }
            activeLayer.topDp != null && activeLayer.bottomDp != null ->
                (size.height - activeLayer.topDp.dp.toPx() - activeLayer.bottomDp.dp.toPx()).coerceAtLeast(1f)
            hasPureShape && activeLayer.shadowDot != null -> 6.dp.toPx()
            else -> null
        }
        val rawW = when {
            activeLayer.widthDp != null -> activeLayer.widthDp.dp.toPx()
            activeLayer.leftDp != null && activeLayer.rightDp != null ->
                (size.width - activeLayer.leftDp.dp.toPx() - activeLayer.rightDp.dp.toPx()).coerceAtLeast(1f)
            hasPureShape && activeLayer.shadowDot != null -> 6.dp.toPx()
            else -> null
        }
        val ar = activeLayer.aspectRatio
        val pw = when {
            activeLayer.widthDp != null && activeLayer.heightCapToParent && ar != null && ar > 0f && rawH != null ->
                min(activeLayer.widthDp.dp.toPx(), rawH * ar)
            rawW != null -> rawW
            ar != null && ar > 0f && rawH != null -> rawH * ar
            else -> size.width
        }.coerceAtLeast(1f)
        val ph = when {
            rawH != null -> rawH
            ar != null && ar > 0f && rawW != null -> rawW / ar
            else -> size.height
        }.coerceAtLeast(1f)
        val selfDx = if (activeLayer.centerX) {
            if (activeLayer.translateSelfX != 0f) pw * activeLayer.translateSelfX else -pw * 0.5f
        } else {
            pw * activeLayer.translateSelfX
        }
        val selfDy = if (activeLayer.centerY) {
            if (activeLayer.translateSelfY != 0f) ph * activeLayer.translateSelfY else -ph * 0.5f
        } else {
            ph * activeLayer.translateSelfY
        }
        val basePx0 = when {
            activeLayer.centerX -> size.width * 0.5f + selfDx + activeLayer.offsetXDp.dp.toPx()
            activeLayer.leftDp != null -> activeLayer.leftDp.dp.toPx() + selfDx
            activeLayer.rightDp != null -> size.width - activeLayer.rightDp.dp.toPx() - pw + selfDx
            else -> activeLayer.offsetXDp.dp.toPx() + selfDx
        }
        val basePy0 = when {
            activeLayer.centerY -> size.height * 0.5f + selfDy + activeLayer.offsetYDp.dp.toPx()
            activeLayer.topDp != null -> activeLayer.topDp.dp.toPx() + selfDy
            activeLayer.bottomDp != null -> size.height - activeLayer.bottomDp.dp.toPx() - ph + selfDy
            else -> activeLayer.offsetYDp.dp.toPx() + selfDy
        }
        val px0 = basePx0 + (evalAnim?.let { it.txDpDelta.dp.toPx() + it.txSelfDelta * pw } ?: 0f)
        val py0 = basePy0 + (evalAnim?.let { it.tyDpDelta.dp.toPx() + it.tySelfDelta * ph } ?: 0f)
        val effScaleX = activeLayer.scaleX * (evalAnim?.scaleX ?: 1f)
        val effScaleY = activeLayer.scaleY * (evalAnim?.scaleY ?: 1f)
        val effRotate = activeLayer.rotateDeg + (evalAnim?.rotateDeg ?: 0f)
        val pivot = Offset(px0 + pw * 0.5f, py0 + ph * 0.5f)

        withTransform({
            if (effRotate != 0f) rotate(effRotate, pivot = pivot)
            if (effScaleX != 1f || effScaleY != 1f) scale(effScaleX, effScaleY, pivot = pivot)
        }) {
            if (activeLayer.shapeFillColor != null) {
                val fillCol = activeLayer.shapeFillColor.copy(alpha = activeLayer.shapeFillColor.alpha * effAlpha)
                if (activeLayer.isCircle) {
                    drawOval(color = fillCol, topLeft = Offset(px0, py0), size = Size(pw, ph))
                } else {
                    drawRect(color = fillCol, topLeft = Offset(px0, py0), size = Size(pw, ph))
                }
            }
            if (activeLayer.shapeBorderColor != null && activeLayer.shapeBorderWidthDp > 0f) {
                val bdCol = activeLayer.shapeBorderColor.copy(alpha = activeLayer.shapeBorderColor.alpha * effAlpha)
                val stroke = Stroke(width = activeLayer.shapeBorderWidthDp.dp.toPx())
                if (activeLayer.isCircle) {
                    drawOval(color = bdCol, topLeft = Offset(px0, py0), size = Size(pw, ph), style = stroke)
                } else {
                    drawRect(color = bdCol, topLeft = Offset(px0, py0), size = Size(pw, ph), style = stroke)
                }
            }
            activeLayer.shadowDot?.let { dot ->
                val dotCol = dot.color.copy(alpha = dot.color.alpha * effAlpha)
                val r = (min(pw, ph) * 0.5f + dot.spreadDp.dp.toPx()).coerceAtLeast(1f)
                drawCircle(
                    color = dotCol,
                    radius = r,
                    center = Offset(px0 + pw * 0.5f + dot.dxDp.dp.toPx(), py0 + ph * 0.5f + dot.dyDp.dp.toPx()),
                )
            }

            clipRect(
                left = px0,
                top = py0,
                right = (px0 + pw).coerceAtLeast(px0 + 1f),
                bottom = (py0 + ph).coerceAtLeast(py0 + 1f),
            ) {
                activeLayer.bgImagesBottomToTop.forEach { bg: BubbleBgImageSpec ->
                    if ((bg.widthDp != null && bg.widthDp <= 0f) || (bg.heightDp != null && bg.heightDp <= 0f)) {
                        return@forEach
                    }
                    val art = artMap[bg.sourceUrl] ?: return@forEach
                    val (imgW, imgH) = when (bg.scaleMode) {
                        BgScaleMode.CONTAIN -> {
                            val artW = art.intrinsicWidth.coerceAtLeast(1f)
                            val artH = art.intrinsicHeight.coerceAtLeast(1f)
                            val s = min(pw / artW, ph / artH)
                            (artW * s).coerceAtLeast(1f) to (artH * s).coerceAtLeast(1f)
                        }
                        BgScaleMode.COVER -> {
                            val artW = art.intrinsicWidth.coerceAtLeast(1f)
                            val artH = art.intrinsicHeight.coerceAtLeast(1f)
                            val s = maxOf(pw / artW, ph / artH)
                            (artW * s).coerceAtLeast(1f) to (artH * s).coerceAtLeast(1f)
                        }
                        BgScaleMode.STRETCH -> {
                            val w = when {
                                bg.widthSubFromFullDp != null -> (pw - bg.widthSubFromFullDp.dp.toPx()).coerceAtLeast(1f)
                                bg.widthFraction != null -> (pw * bg.widthFraction).coerceAtLeast(1f)
                                bg.widthDp != null -> bg.widthDp.dp.toPx()
                                bg.heightDp != null -> bg.heightDp.dp.toPx() * (art.intrinsicWidth / art.intrinsicHeight.coerceAtLeast(1f))
                                else -> pw
                            }
                            val h = when {
                                bg.heightFraction != null -> (ph * bg.heightFraction).coerceAtLeast(1f)
                                bg.heightDp != null -> bg.heightDp.dp.toPx()
                                else -> w * (art.intrinsicHeight / art.intrinsicWidth.coerceAtLeast(1f))
                            }
                            w to h
                        }
                    }
                    val dx = when {
                        bg.alignCenterX -> px0 + (pw - imgW) * 0.5f
                        bg.offsetRightDp != null -> px0 + pw - imgW - bg.offsetRightDp.dp.toPx()
                        else -> px0 + bg.offsetXDp.dp.toPx()
                    }
                    val dy = when {
                        bg.alignCenterY -> py0 + (ph - imgH) * 0.5f
                        bg.offsetBottomDp != null -> py0 + ph - imgH - bg.offsetBottomDp.dp.toPx()
                        else -> py0 + bg.offsetYDp.dp.toPx()
                    }
                    drawImage(
                        image = art.bitmap,
                        dstOffset = IntOffset(dx.roundToInt(), dy.roundToInt()),
                        dstSize = IntSize(imgW.roundToInt().coerceAtLeast(1), imgH.roundToInt().coerceAtLeast(1)),
                        alpha = effAlpha,
                        filterQuality = FilterQuality.Medium,
                    )
                }
            }

            activeLayer.nineSliceSpec?.let { ns ->
                val art = artMap[ns.sourceUrl] ?: return@let
                drawNineSliceArtwork(
                    spec = ns,
                    loadedArt = art,
                    boxX = px0,
                    boxY = py0,
                    boxW = pw,
                    boxH = ph,
                    alpha = effAlpha,
                )
            }
        }
    }
}

/**
 * Renders a chat message bubble with full support for all web preset, community (`meme-hall-skins`),
 * owner52, growth, personal v1/v2, raffle (`claude` Shiguang leather book / `chickengun` / `railcannon`),
 * official, and validated `deco.cssb` styles, plus fallback default Surface styling when unstyled.
 */
@Composable
fun ChatCosmeticBubbleBox(
    style: ResolvedChatBubbleStyle?,
    defaultContainerColor: Color,
    defaultBorderColor: Color = Color.Transparent,
    defaultShape: Shape = RoundedCornerShape(15.dp),
    defaultPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    imageLoader: ImageLoader? = null,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    if (style == null) {
        Surface(
            color = defaultContainerColor,
            shape = defaultShape,
            border = if (defaultBorderColor != Color.Transparent) {
                androidx.compose.foundation.BorderStroke(1.dp, defaultBorderColor)
            } else {
                null
            },
            modifier = modifier,
        ) {
            Box(modifier = Modifier.padding(defaultPadding), content = content)
        }
        return
    }

    val context = LocalContext.current
    val httpClient = LocalCosmeticHttpClient.current
    val paused = LocalPauseCosmeticAnimations.current
    style.dynamicCssSpec?.let { spec ->
        LaunchedEffect(spec) {
            BadgeArtworkCache.ensureCommunityCssLoaded(context, httpClient, spec)
        }
    }

    val bubbleTimeline = style.animationTimeline
    val timelineClock = rememberCosmeticElapsedMs(active = bubbleTimeline != null, intervalMs = 60L)
    val visualClock = rememberCosmeticElapsedMs(active = style.visualAnim.isNotEmpty(), intervalMs = 33L)
    val activeFrame = if (!paused && bubbleTimeline != null && bubbleTimeline.frames.isNotEmpty()) {
        val idx = (((timelineClock.value % bubbleTimeline.periodMs).toDouble() / bubbleTimeline.periodMs.toDouble()) * bubbleTimeline.frames.size)
            .toInt()
            .coerceIn(0, bubbleTimeline.frames.lastIndex)
        bubbleTimeline.frames[idx]
    } else {
        null
    }

    val effFillColor = activeFrame?.fillColor ?: style.fillColor
    val effFillGradCols = activeFrame?.fillGradColors
    val effFillAngle = activeFrame?.fillGradAngleDeg
    val effFillBrush = when {
        effFillGradCols != null && effFillGradCols.size >= 2 -> remember(effFillGradCols, effFillAngle) {
            Brush.linearGradient(effFillGradCols)
        }
        activeFrame != null -> null
        else -> style.fillBrush
    }
    val effBorderColor = activeFrame?.borderColor ?: style.borderColor
    val effBorderGradCols = activeFrame?.borderGradColors ?: style.borderGradientColors
    val effNineSlice = activeFrame?.nineSliceSpec ?: style.nineSliceSpec
    val effPseudoLayers = activeFrame?.pseudoLayers ?: style.pseudoLayers
    val effOpacity = (activeFrame?.opacity ?: style.opacity).coerceIn(0.15f, 1f)

    val bubbleShape = remember(style.shapeKind) {
        when (val k = style.shapeKind) {
            is BubbleShapeKind.Rounded -> RoundedCornerShape(
                topStart = k.topStartDp.dp,
                topEnd = k.topEndDp.dp,
                bottomEnd = k.bottomEndDp.dp,
                bottomStart = k.bottomStartDp.dp,
            )
            is BubbleShapeKind.CutCyber -> CutCornerShape(k.cutDp.dp)
        }
    }

    val ornamentEndDp = when (val orn = style.ornament) {
        is BubbleOrnament.RaffleChickengun -> (orn.sizeDp * 0.60f).coerceIn(14f, 30f)
        is BubbleOrnament.PersonalV1Mascot -> 24f
        is BubbleOrnament.PersonalV2Mascot -> 28f
        is BubbleOrnament.RaffleRailcannon -> 18f
        else -> 0f
    }
    val startReserveDp = max(
        style.startOverflowReserveDp,
        (effNineSlice?.outsetLeftDp ?: 0f).coerceAtLeast(0f),
    ).dp
    val topReserveDp = max(
        style.topOverflowReserveDp,
        (effNineSlice?.outsetTopDp ?: 0f).coerceAtLeast(0f),
    ).dp
    val bottomReserveDp = max(
        style.bottomOverflowReserveDp,
        (effNineSlice?.outsetBottomDp ?: 0f).coerceAtLeast(0f),
    ).dp
    val trailingReserveDp = max(
        max(ornamentEndDp, style.endOverflowReserveDp),
        (effNineSlice?.outsetRightDp ?: 0f).coerceAtLeast(0f),
    ).dp
    val shouldClipContainer = when (val k = style.shapeKind) {
        is BubbleShapeKind.Rounded ->
            k.topStartDp > 0f || k.topEndDp > 0f || k.bottomEndDp > 0f || k.bottomStartDp > 0f
        is BubbleShapeKind.CutCyber -> k.cutDp > 0f
    }

    Box(
        modifier = modifier
            .applyCssVisualAnim(style.visualAnim, visualClock, paused)
            .alpha(effOpacity)
            .padding(start = startReserveDp, top = topReserveDp, end = trailingReserveDp, bottom = bottomReserveDp),
    ) {
        // Hard offset shadow (official, pixel, personal v1 accents)
        val (sx, sy) = style.shadowOffsetDp
        val shadowCol = style.shadowColor
        if (shadowCol != null && (sx != 0f || sy != 0f)) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = sx.dp, y = sy.dp)
                    .then(if (shouldClipContainer) Modifier.clip(bubbleShape) else Modifier)
                    .background(shadowCol),
            )
        }

        val hasOpaqueNineSlice = effNineSlice != null &&
            effNineSlice.fillCenter &&
            !effNineSlice.isEdgeTrimOnly
        val hasRotatingBorder = !hasOpaqueNineSlice && !style.dashedBorder && style.borderWidthDp > 0f &&
            effBorderGradCols != null && effBorderGradCols.size >= 2
        val borderSweepAngle = if (hasRotatingBorder && !paused) {
            val tr = rememberInfiniteTransition(label = "bubble_border_sweep")
            val a by tr.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart),
                label = "sweep_deg",
            )
            a
        } else {
            0f
        }

        // Main bubble container (leave sculpted 9-slice frames unclipped so alpha edges never truncate)
        Box(
            modifier = Modifier
                .then(
                    if (style.minWidthDp > 0f || style.minHeightDp > 0f) {
                        Modifier.defaultMinSize(minWidth = style.minWidthDp.dp, minHeight = style.minHeightDp.dp)
                    } else {
                        Modifier
                    },
                )
                .then(if (shouldClipContainer) Modifier.clip(bubbleShape) else Modifier)
                .then(
                    when {
                        hasOpaqueNineSlice -> Modifier
                        effFillBrush != null -> Modifier.background(effFillBrush)
                        effFillColor != Color.Transparent -> Modifier.background(effFillColor)
                        else -> Modifier
                    },
                )
                .then(
                    if (!hasOpaqueNineSlice && !style.dashedBorder && style.borderWidthDp > 0f) {
                        when {
                            hasRotatingBorder -> Modifier.drawWithContent {
                                drawContent()
                                val outline = bubbleShape.createOutline(size, layoutDirection, this)
                                val outlinePath = Path().apply { addOutline(outline) }
                                clipPath(outlinePath) {
                                    drawOutline(
                                        outline = outline,
                                        brush = createRotatingSweepBrush(effBorderGradCols!!, borderSweepAngle, size),
                                        style = Stroke(width = style.borderWidthDp.dp.toPx() * 2f),
                                    )
                                }
                            }
                            style.borderBrush != null -> Modifier.border(style.borderWidthDp.dp, style.borderBrush, bubbleShape)
                            effBorderColor != Color.Transparent -> Modifier.border(style.borderWidthDp.dp, effBorderColor, bubbleShape)
                            else -> Modifier
                        }
                    } else {
                        Modifier
                    },
                ),
        ) {
            // 1. Background pattern / ornament underlays inside bubble
            BubblePatternLayer(style = style, modifier = Modifier.matchParentSize())

            // 2. 9-slice bitmap skin frame (community skins, personal-v2, raffle frames)
            effNineSlice?.let { spec ->
                NineSliceFrameCanvas(
                    spec = spec,
                    fallbackColor = if (hasOpaqueNineSlice) effFillColor else Color.Transparent,
                    modifier = Modifier.matchParentSize(),
                )
            }

            // 2b. CSS pseudo-element overlays (`::before` / `::after`: tentacles, feet, duck head)
            effPseudoLayers.forEach { layer ->
                BubblePseudoLayerCanvas(
                    layer = layer,
                    modifier = Modifier.matchParentSize(),
                )
            }

            // 3. Dashed border, double inner border, left/bottom accent bars
            if (style.dashedBorder || style.doubleBorder || style.leftAccentBarColor != null || style.bottomAccentBarColor != null) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawEdgeAccents(style)
                }
            }

            // 4. Shiguang right-edge bookmark ribbon
            if (style.ornament is BubbleOrnament.ShiguangBook) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 9.dp)
                        .width(7.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
                        .background(
                            if (style.ornament.level >= 2) Color(0xFFB91C1C) else Color(0xFFD97706),
                        )
                        .border(0.6.dp, Color(0xFFFDE68A), RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp)),
                )
            }

            // 6. Message body content
            if (activeFrame != null) {
                CompositionLocalProvider(
                    LocalAnimatedBubbleColors provides Triple(
                        activeFrame.textColor,
                        activeFrame.linkColor,
                        activeFrame.mutedColor,
                    ),
                ) {
                    Box(
                        modifier = Modifier.padding(style.contentPadding),
                        content = content,
                    )
                }
            } else {
                Box(
                    modifier = Modifier.padding(style.contentPadding),
                    content = content,
                )
            }
        }

        // Outer corner ornaments / mascots floating at trailing corner without clipping or overlapping text
        BubbleCornerMascotLayer(
            ornament = style.ornament,
            imageLoader = imageLoader,
            modifier = Modifier.matchParentSize(),
        )
    }
}

@Composable
private fun BubblePatternLayer(
    style: ResolvedChatBubbleStyle,
    modifier: Modifier = Modifier,
) {
    val orn = style.ornament
    if (orn == BubbleOrnament.None) return
    val paused = LocalPauseCosmeticAnimations.current
    val hasAmbientAnim = !paused && (
        orn == BubbleOrnament.PresetGalaxy ||
            orn == BubbleOrnament.PresetNightStars ||
            orn == BubbleOrnament.PresetAurora ||
            orn is BubbleOrnament.Owner52 ||
            orn == BubbleOrnament.PresetPetal ||
            orn == BubbleOrnament.PresetSakura
        )
    val phase = if (hasAmbientAnim) {
        val tr = rememberInfiniteTransition(label = "bubble_pattern")
        val p by tr.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart),
            label = "pattern_phase",
        )
        p
    } else {
        0f
    }
    Canvas(modifier = modifier) {
        when (orn) {
            BubbleOrnament.PresetCandyStripes -> {
                val stripeW = 10.dp.toPx()
                val step = 22.dp.toPx()
                var x = -size.height
                while (x < size.width + size.height) {
                    val p = Path().apply {
                        moveTo(x, size.height)
                        lineTo(x + stripeW, size.height)
                        lineTo(x + size.height + stripeW, 0f)
                        lineTo(x + size.height, 0f)
                        close()
                    }
                    drawPath(p, Color.White.copy(alpha = 0.28f))
                    x += step
                }
            }
            BubbleOrnament.PresetPaperLines -> {
                val step = 18.dp.toPx()
                var y = step
                while (y < size.height - 4.dp.toPx()) {
                    drawLine(
                        color = style.borderColor.copy(alpha = 0.35f),
                        start = Offset(8.dp.toPx(), y),
                        end = Offset(size.width - 8.dp.toPx(), y),
                        strokeWidth = 0.8.dp.toPx(),
                    )
                    y += step
                }
            }
            BubbleOrnament.PresetGalaxy, BubbleOrnament.PresetNightStars -> {
                val stars = listOf(
                    0.12f to 0.24f, 0.28f to 0.72f, 0.46f to 0.18f,
                    0.68f to 0.30f, 0.84f to 0.22f, 0.76f to 0.78f, 0.91f to 0.56f,
                )
                stars.forEachIndexed { idx, (fx, fy) ->
                    val r = if (idx % 2 == 0) 1.8.dp.toPx() else 1.1.dp.toPx()
                    val twinkle = if (hasAmbientAnim) (0.35f + 0.65f * ((sin(phase + idx * 1.1f) + 1f) * 0.5f)) else 1f
                    val baseAlpha = (if (idx == 4) 0.78f else 0.60f) * twinkle
                    drawCircle(
                        color = if (idx == 4) Color(0xFFFDE68A).copy(alpha = baseAlpha) else Color.White.copy(alpha = baseAlpha),
                        radius = r,
                        center = Offset(size.width * fx, size.height * fy),
                    )
                }
            }
            BubbleOrnament.PresetBoba -> {
                val pearls = listOf(0.86f to 0.76f, 0.92f to 0.82f, 0.79f to 0.84f)
                pearls.forEach { (fx, fy) ->
                    drawCircle(
                        color = Color(0xFF4A2C16).copy(alpha = 0.28f),
                        radius = 3.2.dp.toPx(),
                        center = Offset(size.width * fx, size.height * fy),
                    )
                }
            }
            BubbleOrnament.PresetAurora -> {
                val cxFrac = if (hasAmbientAnim) 0.55f + 0.30f * cos(phase) else 0.85f
                val cyFrac = if (hasAmbientAnim) 0.28f + 0.15f * sin(phase) else 0.2f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF00F0FF).copy(alpha = 0.24f), Color.Transparent),
                        center = Offset(size.width * cxFrac, size.height * cyFrac),
                        radius = maxOf(size.width * 0.45f, 24f),
                    ),
                    radius = maxOf(size.width * 0.45f, 24f),
                    center = Offset(size.width * cxFrac, size.height * cyFrac),
                )
            }
            BubbleOrnament.PresetJelly -> {
                // Glossy jelly highlight strip at top
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.42f),
                    topLeft = Offset(10.dp.toPx(), 3.dp.toPx()),
                    size = Size((size.width - 20.dp.toPx()).coerceAtLeast(8f), 4.dp.toPx()),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                )
            }
            is BubbleOrnament.ShiguangBook -> {
                // Leather book spine highlight & inner gold frame
                drawRect(
                    color = Color(0xFF3A180B).copy(alpha = 0.55f),
                    topLeft = Offset.Zero,
                    size = Size(7.dp.toPx(), size.height),
                )
                drawLine(
                    color = Color(0xFFF2D08E).copy(alpha = 0.48f),
                    start = Offset(7.dp.toPx(), 0f),
                    end = Offset(7.dp.toPx(), size.height),
                    strokeWidth = 1.dp.toPx(),
                )
                val inset = 4.dp.toPx()
                drawRoundRect(
                    color = Color(0xFFF2D08E).copy(alpha = if (orn.level >= 2) 0.52f else 0.25f),
                    topLeft = Offset(9.dp.toPx(), inset),
                    size = Size((size.width - 14.dp.toPx()).coerceAtLeast(4f), (size.height - inset * 2f).coerceAtLeast(4f)),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                    style = Stroke(width = 0.9.dp.toPx()),
                )
            }
            is BubbleOrnament.Owner52 -> {
                // Subtle iridescent corner glint & micro-sparks
                val glintAlpha = if (hasAmbientAnim) 0.16f + 0.14f * ((sin(phase) + 1f) * 0.5f) else 0.24f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFF8DC).copy(alpha = glintAlpha), Color.Transparent),
                        center = Offset(size.width * 0.88f, size.height * 0.16f),
                        radius = 22.dp.toPx(),
                    ),
                    radius = 22.dp.toPx(),
                    center = Offset(size.width * 0.88f, size.height * 0.16f),
                )
                val sparks = listOf(0.18f to 0.92f, 0.58f to 0.90f, 0.86f to 0.12f)
                sparks.forEachIndexed { idx, (fx, fy) ->
                    val tw = if (hasAmbientAnim) (0.35f + 0.65f * ((sin(phase * 1.4f + idx * 1.5f) + 1f) * 0.5f)) else 1f
                    drawCircle(
                        color = style.borderColor.copy(alpha = 0.60f * tw),
                        radius = 1.6.dp.toPx(),
                        center = Offset(size.width * fx, size.height * fy),
                    )
                }
            }
            BubbleOrnament.PresetPetal, BubbleOrnament.PresetSakura -> {
                val petals = listOf(0.88f to 0.24f, 0.14f to 0.78f, 0.92f to 0.72f)
                petals.forEachIndexed { idx, (fx, fy) ->
                    val dy = if (hasAmbientAnim) sin(phase + idx * 1.4f) * 1.6.dp.toPx() else 0f
                    drawCircle(
                        color = Color(0xFFF472B6).copy(alpha = 0.35f),
                        radius = 2.6.dp.toPx(),
                        center = Offset(size.width * fx, size.height * fy + dy),
                    )
                }
            }
            BubbleOrnament.PresetBit -> {
                val px = 4.dp.toPx()
                drawRect(style.borderColor.copy(alpha = 0.4f), Offset(3.dp.toPx(), 3.dp.toPx()), Size(px, px))
                drawRect(style.borderColor.copy(alpha = 0.4f), Offset(size.width - px - 3.dp.toPx(), size.height - px - 3.dp.toPx()), Size(px, px))
            }
            BubbleOrnament.PresetMint, BubbleOrnament.PresetLemon, BubbleOrnament.PresetBunny,
            BubbleOrnament.PresetLaowu, BubbleOrnament.PresetPaw, BubbleOrnament.PresetPuff,
            BubbleOrnament.PresetCyber, BubbleOrnament.PresetPrism, BubbleOrnament.PresetYinlu -> {
                // Soft ambient corner accent dot
                drawCircle(
                    color = style.borderColor.copy(alpha = 0.22f),
                    radius = 8.dp.toPx(),
                    center = Offset(size.width - 8.dp.toPx(), size.height - 8.dp.toPx()),
                )
            }
            else -> Unit
        }
    }
}

private fun DrawScope.drawEdgeAccents(style: ResolvedChatBubbleStyle) {
    style.leftAccentBarColor?.let { barCol ->
        drawRect(
            color = barCol,
            topLeft = Offset.Zero,
            size = Size(style.leftAccentBarWidthDp.dp.toPx().coerceAtLeast(2f), size.height),
        )
    }
    style.bottomAccentBarColor?.let { barCol ->
        val h = 3.dp.toPx()
        drawRect(
            color = barCol,
            topLeft = Offset(0f, size.height - h),
            size = Size(size.width, h),
        )
    }
    if (style.doubleBorder && style.borderColor != Color.Transparent) {
        val inset = 3.5.dp.toPx()
        drawRoundRect(
            color = style.borderColor.copy(alpha = 0.65f),
            topLeft = Offset(inset, inset),
            size = Size((size.width - inset * 2).coerceAtLeast(2f), (size.height - inset * 2).coerceAtLeast(2f)),
            cornerRadius = CornerRadius(11.dp.toPx(), 11.dp.toPx()),
            style = Stroke(width = 1.dp.toPx()),
        )
    }
    if (style.dashedBorder && style.borderColor != Color.Transparent) {
        val half = (style.borderWidthDp.dp.toPx() / 2f).coerceAtLeast(0.5f)
        drawRoundRect(
            color = style.borderColor,
            topLeft = Offset(half, half),
            size = Size((size.width - half * 2).coerceAtLeast(2f), (size.height - half * 2).coerceAtLeast(2f)),
            cornerRadius = CornerRadius(13.dp.toPx(), 13.dp.toPx()),
            style = Stroke(
                width = style.borderWidthDp.dp.toPx().coerceAtLeast(1f),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)),
            ),
        )
    }
}

@Composable
private fun MascotRasterArtwork(
    url: String,
    imageLoader: ImageLoader?,
    animate: Boolean,
    reqSizePx: Int = 192,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val httpClient = LocalCosmeticHttpClient.current
    if (animate && imageLoader != null) {
        val rewritten = remember(url) { MediaUrlRewriter.rewriteMediaUrl(url) }
        AsyncImage(
            model = rewritten,
            imageLoader = imageLoader,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier,
        )
    } else if (animate) {
        rememberAnimatedCosmeticBitmap(url, reqSizePx)?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = modifier,
            )
        }
    } else {
        val staticBmp by produceState<ImageBitmap?>(initialValue = null, url, reqSizePx) {
            value = BadgeArtworkCache.getOrFetchBitmap(context, httpClient, url, reqSizePx)
        }
        staticBmp?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun BubbleCornerMascotLayer(
    ornament: BubbleOrnament,
    imageLoader: ImageLoader?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val httpClient = LocalCosmeticHttpClient.current
    val paused = LocalPauseCosmeticAnimations.current

    when (ornament) {
        is BubbleOrnament.PersonalV1Mascot -> {
            val bmp by produceState<ImageBitmap?>(initialValue = null, ornament.svgUrl) {
                value = BadgeArtworkCache.getOrFetchBitmap(context, httpClient, ornament.svgUrl, 128)
            }
            bmp?.let { loaded ->
                Box(modifier = modifier) {
                    Image(
                        bitmap = loaded,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 22.dp, y = 1.dp)
                            .size(width = 23.dp, height = 25.dp),
                    )
                }
            }
        }
        is BubbleOrnament.PersonalV2Mascot -> {
            Box(modifier = modifier) {
                MascotRasterArtwork(
                    url = ornament.webpUrl,
                    imageLoader = imageLoader,
                    animate = !paused,
                    reqSizePx = 192,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 26.dp, y = 0.dp)
                        .width(24.dp)
                        .height(28.dp),
                )
            }
        }
        is BubbleOrnament.RaffleChickengun -> {
            val sz = ornament.sizeDp.coerceIn(18f, 44f).dp
            val playMotion = !paused && ornament.cgMotion
            val playFire = !paused && ornament.cgFire
            val motionFrac = if (playMotion || playFire) {
                val tr = rememberInfiniteTransition(label = "chickengun_fx")
                val f by tr.animateFloat(
                    initialValue = 0f,
                    targetValue = (2.0 * PI).toFloat(),
                    animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Restart),
                    label = "cg_phase",
                )
                f
            } else {
                0f
            }
            Box(modifier = modifier) {
                MascotRasterArtwork(
                    url = ornament.webpUrl,
                    imageLoader = imageLoader,
                    animate = playMotion,
                    reqSizePx = 192,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (ornament.sizeDp * 0.56f).dp, y = 2.dp)
                        .then(
                            if (playMotion) {
                                Modifier.graphicsLayer {
                                    val pulse = (sin(motionFrac) + 1f) * 0.5f
                                    translationX = -pulse * 1.8.dp.toPx()
                                    rotationZ = -pulse * 5.5f
                                }
                            } else {
                                Modifier
                            },
                        )
                        .size(sz),
                )
                if (playFire) {
                    Canvas(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (ornament.sizeDp * 0.56f + 8f).dp, y = (-ornament.sizeDp * 0.22f).dp)
                            .size(12.dp),
                    ) {
                        val pulse = ((sin(motionFrac * 2f) + 1f) * 0.5f).coerceIn(0.25f, 1f)
                        val coreR = 2.8.dp.toPx() * (0.7f + 0.5f * pulse)
                        drawCircle(
                            color = Color(0xFFFF6B00).copy(alpha = 0.65f * pulse),
                            radius = coreR * 1.6f,
                            center = Offset(size.width * 0.55f, size.height * 0.5f),
                        )
                        drawCircle(
                            color = Color(0xFFFFEE58).copy(alpha = 0.90f * pulse),
                            radius = coreR * 0.85f,
                            center = Offset(size.width * 0.45f, size.height * 0.5f),
                        )
                    }
                }
            }
        }
        is BubbleOrnament.RaffleRailcannon -> {
            val playGlow = !paused && ornament.stage >= 3
            val pulse = if (playGlow) {
                val tr = rememberInfiniteTransition(label = "railcannon_fx")
                val p by tr.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 0.85f,
                    animationSpec = infiniteRepeatable(tween(950), RepeatMode.Reverse),
                    label = "rc_glow",
                )
                p
            } else {
                0f
            }
            Box(modifier = modifier) {
                if (playGlow) {
                    Canvas(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 18.dp, y = (-3).dp)
                            .size(22.dp),
                    ) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.35f * pulse), Color.Transparent),
                                center = Offset(size.width * 0.5f, size.height * 0.5f),
                                radius = size.width * 0.65f,
                            ),
                        )
                    }
                }
                MascotRasterArtwork(
                    url = ornament.cannonUrl,
                    imageLoader = imageLoader,
                    animate = !paused,
                    reqSizePx = 128,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 16.dp, y = (-3).dp)
                        .size(22.dp),
                )
            }
        }
        is BubbleOrnament.RaffleWhalebuddy -> {
            val floatPhase = if (!paused) {
                val tr = rememberInfiniteTransition(label = "whalebuddy_fx")
                val f by tr.animateFloat(
                    initialValue = 0f,
                    targetValue = (2.0 * PI).toFloat(),
                    animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
                    label = "whale_phase",
                )
                f
            } else {
                0f
            }
            Box(modifier = modifier) {
                MascotRasterArtwork(
                    url = ornament.personUrl,
                    imageLoader = imageLoader,
                    animate = !paused,
                    reqSizePx = 128,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = 8.dp, y = 0.dp)
                        .then(
                            if (!paused) {
                                Modifier.graphicsLayer { translationY = sin(floatPhase) * 1.2.dp.toPx() }
                            } else {
                                Modifier
                            },
                        )
                        .width(36.dp)
                        .height(21.dp),
                )
            }
        }
        else -> Unit
    }
}

/**
 * Renders a compact `+1 / boost` pill chip using either the boost author's cosmetic frame
 * (`ResolvedChatBubbleStyle`, e.g. community `reply` 9-slice, Shiguang trim, official/growth/raffle)
 * or the standard surface2 chip when unstyled.
 */
@Composable
fun ChatCosmeticBoostPill(
    style: ResolvedChatBubbleStyle?,
    defaultContainerColor: Color,
    defaultBorderColor: Color = Color.Transparent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val context = LocalContext.current
    val httpClient = LocalCosmeticHttpClient.current
    val paused = LocalPauseCosmeticAnimations.current
    style?.dynamicCssSpec?.let { spec ->
        LaunchedEffect(spec) {
            BadgeArtworkCache.ensureCommunityCssLoaded(context, httpClient, spec)
        }
    }

    val boostTimeline = style?.animationTimeline
    val timelineClock = rememberCosmeticElapsedMs(active = boostTimeline != null, intervalMs = 60L)
    val visualClock = rememberCosmeticElapsedMs(active = style?.visualAnim?.isNotEmpty() == true, intervalMs = 33L)
    val activeFrame = if (!paused && boostTimeline != null && boostTimeline.frames.isNotEmpty()) {
        val idx = (((timelineClock.value % boostTimeline.periodMs).toDouble() / boostTimeline.periodMs.toDouble()) * boostTimeline.frames.size)
            .toInt()
            .coerceIn(0, boostTimeline.frames.lastIndex)
        boostTimeline.frames[idx]
    } else {
        null
    }

    val effFillColor = activeFrame?.fillColor ?: style?.fillColor ?: defaultContainerColor
    val effFillGradCols = activeFrame?.fillGradColors
    val effFillBrush = when {
        effFillGradCols != null && effFillGradCols.size >= 2 -> remember(effFillGradCols) {
            Brush.linearGradient(effFillGradCols)
        }
        activeFrame != null -> null
        else -> style?.fillBrush
    }
    val effBorderColor = activeFrame?.borderColor ?: style?.borderColor ?: defaultBorderColor
    val effBorderGradCols = activeFrame?.borderGradColors ?: style?.borderGradientColors
    val effNineSlice = activeFrame?.nineSliceSpec ?: style?.nineSliceSpec
    val effPseudoLayers = activeFrame?.pseudoLayers ?: style?.pseudoLayers.orEmpty()

    val pillShape = if (style != null) {
        when (val k = style.shapeKind) {
            is BubbleShapeKind.Rounded -> RoundedCornerShape(
                topStart = min(k.topStartDp, 12f).dp,
                topEnd = min(k.topEndDp, 12f).dp,
                bottomEnd = min(k.bottomEndDp, 12f).dp,
                bottomStart = min(k.bottomStartDp, 12f).dp,
            )
            is BubbleShapeKind.CutCyber -> CutCornerShape(6.dp)
        }
    } else {
        RoundedCornerShape(999.dp)
    }

    val startRes = max(
        style?.startOverflowReserveDp ?: 0f,
        (effNineSlice?.outsetLeftDp ?: 0f).coerceAtLeast(0f),
    ).dp
    val topRes = max(
        style?.topOverflowReserveDp ?: 0f,
        (effNineSlice?.outsetTopDp ?: 0f).coerceAtLeast(0f),
    ).dp
    val endRes = max(
        style?.endOverflowReserveDp ?: 0f,
        (effNineSlice?.outsetRightDp ?: 0f).coerceAtLeast(0f),
    ).dp
    val bottomRes = max(
        style?.bottomOverflowReserveDp ?: 0f,
        (effNineSlice?.outsetBottomDp ?: 0f).coerceAtLeast(0f),
    ).dp
    val hasNineSlice = effNineSlice != null
    val shouldClip = !hasNineSlice
    val hasRotatingBorder = !hasNineSlice && (style?.borderWidthDp ?: 0f) > 0f &&
        effBorderGradCols != null && effBorderGradCols.size >= 2
    val borderSweepAngle = if (hasRotatingBorder && !paused) {
        val tr = rememberInfiniteTransition(label = "boost_border_sweep")
        val a by tr.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart),
            label = "boost_sweep_deg",
        )
        a
    } else {
        0f
    }

    Box(
        modifier = modifier
            .applyCssVisualAnim(style?.visualAnim.orEmpty(), visualClock, paused)
            .padding(start = startRes, top = topRes, end = endRes, bottom = bottomRes)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .then(if (shouldClip) Modifier.clip(pillShape) else Modifier)
                .then(
                    when {
                        hasNineSlice -> Modifier
                        effFillBrush != null -> Modifier.background(effFillBrush)
                        style != null && effFillColor != Color.Transparent -> Modifier.background(effFillColor)
                        else -> Modifier.background(defaultContainerColor)
                    },
                )
                .then(
                    when {
                        hasNineSlice -> Modifier
                        hasRotatingBorder -> Modifier.drawWithContent {
                            drawContent()
                            val outline = pillShape.createOutline(size, layoutDirection, this)
                            val outlinePath = Path().apply { addOutline(outline) }
                            clipPath(outlinePath) {
                                drawOutline(
                                    outline = outline,
                                    brush = createRotatingSweepBrush(effBorderGradCols!!, borderSweepAngle, size),
                                    style = Stroke(width = style!!.borderWidthDp.dp.toPx() * 2f),
                                )
                            }
                        }
                        style?.borderBrush != null && style.borderWidthDp > 0f ->
                            Modifier.border(style.borderWidthDp.dp, style.borderBrush, pillShape)
                        style != null && style.borderWidthDp > 0f && effBorderColor != Color.Transparent ->
                            Modifier.border(style.borderWidthDp.dp, effBorderColor, pillShape)
                        defaultBorderColor != Color.Transparent ->
                            Modifier.border(0.8.dp, defaultBorderColor, pillShape)
                        else -> Modifier
                    },
                ),
        ) {
            effNineSlice?.let { spec ->
                NineSliceFrameCanvas(
                    spec = spec,
                    fallbackColor = effFillColor,
                    modifier = Modifier.matchParentSize(),
                )
            }
            effPseudoLayers.forEach { layer ->
                BubblePseudoLayerCanvas(
                    layer = layer,
                    modifier = Modifier.matchParentSize(),
                )
            }
            if (activeFrame != null) {
                CompositionLocalProvider(
                    LocalAnimatedBubbleColors provides Triple(
                        activeFrame.textColor,
                        activeFrame.linkColor,
                        activeFrame.mutedColor,
                    ),
                ) {
                    Box(
                        modifier = Modifier.padding(
                            style?.contentPadding ?: PaddingValues(horizontal = 8.dp, vertical = 3.dp),
                        ),
                        content = content,
                    )
                }
            } else {
                Box(
                    modifier = Modifier.padding(
                        style?.contentPadding ?: PaddingValues(horizontal = 8.dp, vertical = 3.dp),
                    ),
                    content = content,
                )
            }
        }
    }
}
