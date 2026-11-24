package com.jinhuan.forhappy.ui.components

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import com.jinhuan.forhappy.core.cosmetics.BubbleBgImageSpec
import com.jinhuan.forhappy.core.cosmetics.BubbleOrnament
import com.jinhuan.forhappy.core.cosmetics.BubblePseudoLayer
import com.jinhuan.forhappy.core.cosmetics.BubbleShapeKind
import com.jinhuan.forhappy.core.cosmetics.NineSliceSpec
import com.jinhuan.forhappy.core.cosmetics.ResolvedChatBubbleStyle
import com.jinhuan.forhappy.core.network.media.MediaUrlRewriter
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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
        if (sw <= 0 || sh <= 0 || dw <= 0 || dh <= 0) return
        drawImage(
            image = loaded,
            srcOffset = IntOffset(sx, sy),
            srcSize = IntSize(sw, sh),
            dstOffset = IntOffset(ox + dx, oy + dy),
            dstSize = IntSize(dw, dh),
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
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val httpClient = LocalCosmeticHttpClient.current
    val bitmapArt by produceState<RasterizedArtwork?>(initialValue = null, spec.sourceUrl) {
        value = BadgeArtworkCache.getOrFetchNineSliceArtwork(context, httpClient, spec.sourceUrl, reqSizePx = 1024)
    }

    val loadedArt = bitmapArt
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
        )
    }
}

@Composable
internal fun BubblePseudoLayerCanvas(
    layer: BubblePseudoLayer,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val httpClient = LocalCosmeticHttpClient.current
    val hasPureShape = layer.shapeFillColor != null || layer.shapeBorderColor != null || layer.shadowDot != null
    val allUrls = remember(layer) {
        buildList {
            layer.bgImagesBottomToTop.forEach { add(it.sourceUrl) }
            layer.nineSliceSpec?.sourceUrl?.let { add(it) }
        }
    }
    val artMap by produceState<Map<String, RasterizedArtwork>>(initialValue = emptyMap(), allUrls) {
        val out = LinkedHashMap<String, RasterizedArtwork>(allUrls.size)
        for (url in allUrls) {
            BadgeArtworkCache.getOrFetchNineSliceArtwork(context, httpClient, url, reqSizePx = 1024)?.let {
                out[url] = it
            }
        }
        value = out
    }
    if (artMap.isEmpty() && !hasPureShape) return

    Canvas(modifier = modifier) {
        val pw = when {
            layer.widthDp != null -> layer.widthDp.dp.toPx()
            layer.leftDp != null && layer.rightDp != null -> (size.width - layer.leftDp.dp.toPx() - layer.rightDp.dp.toPx()).coerceAtLeast(1f)
            hasPureShape && layer.shadowDot != null -> 6.dp.toPx()
            else -> size.width
        }
        val ph = when {
            layer.heightDp != null -> layer.heightDp.dp.toPx()
            layer.topDp != null && layer.bottomDp != null -> (size.height - layer.topDp.dp.toPx() - layer.bottomDp.dp.toPx()).coerceAtLeast(1f)
            hasPureShape && layer.shadowDot != null -> 6.dp.toPx()
            else -> size.height
        }
        val px0 = when {
            layer.centerX -> (size.width - pw) * 0.5f + layer.offsetXDp.dp.toPx()
            layer.leftDp != null -> layer.leftDp.dp.toPx()
            layer.rightDp != null -> size.width - layer.rightDp.dp.toPx() - pw
            else -> layer.offsetXDp.dp.toPx()
        }
        val py0 = when {
            layer.centerY -> (size.height - ph) * 0.5f + layer.offsetYDp.dp.toPx()
            layer.topDp != null -> layer.topDp.dp.toPx()
            layer.bottomDp != null -> size.height - layer.bottomDp.dp.toPx() - ph
            else -> layer.offsetYDp.dp.toPx()
        }

        if (layer.shapeFillColor != null) {
            if (layer.isCircle) {
                drawOval(
                    color = layer.shapeFillColor,
                    topLeft = Offset(px0, py0),
                    size = Size(pw, ph),
                )
            } else {
                drawRect(
                    color = layer.shapeFillColor,
                    topLeft = Offset(px0, py0),
                    size = Size(pw, ph),
                )
            }
        }
        if (layer.shapeBorderColor != null && layer.shapeBorderWidthDp > 0f) {
            val stroke = Stroke(width = layer.shapeBorderWidthDp.dp.toPx())
            if (layer.isCircle) {
                drawOval(
                    color = layer.shapeBorderColor,
                    topLeft = Offset(px0, py0),
                    size = Size(pw, ph),
                    style = stroke,
                )
            } else {
                drawRect(
                    color = layer.shapeBorderColor,
                    topLeft = Offset(px0, py0),
                    size = Size(pw, ph),
                    style = stroke,
                )
            }
        }
        layer.shadowDot?.let { dot ->
            val r = (min(pw, ph) * 0.5f + dot.spreadDp.dp.toPx()).coerceAtLeast(1f)
            drawCircle(
                color = dot.color,
                radius = r,
                center = Offset(px0 + pw * 0.5f + dot.dxDp.dp.toPx(), py0 + ph * 0.5f + dot.dyDp.dp.toPx()),
            )
        }

        layer.bgImagesBottomToTop.forEach { bg: BubbleBgImageSpec ->
            val art = artMap[bg.sourceUrl] ?: return@forEach
            val imgW = when {
                bg.widthSubFromFullDp != null -> (pw - bg.widthSubFromFullDp.dp.toPx()).coerceAtLeast(1f)
                bg.widthFraction != null -> (pw * bg.widthFraction).coerceAtLeast(1f)
                bg.widthDp != null -> bg.widthDp.dp.toPx()
                bg.heightDp != null -> bg.heightDp.dp.toPx() * (art.intrinsicWidth / art.intrinsicHeight.coerceAtLeast(1f))
                else -> pw
            }
            val imgH = when {
                bg.heightFraction != null -> (ph * bg.heightFraction).coerceAtLeast(1f)
                bg.heightDp != null -> bg.heightDp.dp.toPx()
                else -> imgW * (art.intrinsicHeight / art.intrinsicWidth.coerceAtLeast(1f))
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
            )
        }

        layer.nineSliceSpec?.let { ns ->
            val art = artMap[ns.sourceUrl] ?: return@let
            drawNineSliceArtwork(
                spec = ns,
                loadedArt = art,
                boxX = px0,
                boxY = py0,
                boxW = pw,
                boxH = ph,
            )
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
    style.dynamicCssSpec?.let { spec ->
        LaunchedEffect(spec) {
            BadgeArtworkCache.ensureCommunityCssLoaded(context, httpClient, spec)
        }
    }

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
        is BubbleOrnament.PersonalV1Mascot -> 16f
        is BubbleOrnament.PersonalV2Mascot -> 18f
        is BubbleOrnament.RaffleRailcannon -> 18f
        else -> 0f
    }
    val startReserveDp = max(
        style.startOverflowReserveDp,
        (style.nineSliceSpec?.outsetLeftDp ?: 0f).coerceAtLeast(0f),
    ).dp
    val topReserveDp = max(
        style.topOverflowReserveDp,
        (style.nineSliceSpec?.outsetTopDp ?: 0f).coerceAtLeast(0f),
    ).dp
    val bottomReserveDp = max(
        style.bottomOverflowReserveDp,
        (style.nineSliceSpec?.outsetBottomDp ?: 0f).coerceAtLeast(0f),
    ).dp
    val trailingReserveDp = max(
        max(ornamentEndDp, style.endOverflowReserveDp),
        (style.nineSliceSpec?.outsetRightDp ?: 0f).coerceAtLeast(0f),
    ).dp
    val shouldClipContainer = when (val k = style.shapeKind) {
        is BubbleShapeKind.Rounded ->
            k.topStartDp > 0f || k.topEndDp > 0f || k.bottomEndDp > 0f || k.bottomStartDp > 0f
        is BubbleShapeKind.CutCyber -> k.cutDp > 0f
    }

    Box(
        modifier = modifier
            .alpha(style.opacity.coerceIn(0.2f, 1f))
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

        val hasOpaqueNineSlice = style.nineSliceSpec != null &&
            style.nineSliceSpec.fillCenter &&
            !style.nineSliceSpec.isEdgeTrimOnly

        // Main bubble container (leave sculpted 9-slice frames unclipped so alpha edges never truncate)
        Box(
            modifier = Modifier
                .then(if (style.minWidthDp > 0f) Modifier.defaultMinSize(minWidth = style.minWidthDp.dp) else Modifier)
                .then(if (shouldClipContainer) Modifier.clip(bubbleShape) else Modifier)
                .then(
                    when {
                        hasOpaqueNineSlice -> Modifier
                        style.fillBrush != null -> Modifier.background(style.fillBrush)
                        style.fillColor != Color.Transparent -> Modifier.background(style.fillColor)
                        else -> Modifier
                    },
                )
                .then(
                    if (!hasOpaqueNineSlice && !style.dashedBorder && style.borderWidthDp > 0f) {
                        when {
                            style.borderBrush != null -> Modifier.border(style.borderWidthDp.dp, style.borderBrush, bubbleShape)
                            style.borderColor != Color.Transparent -> Modifier.border(style.borderWidthDp.dp, style.borderColor, bubbleShape)
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
            style.nineSliceSpec?.let { spec ->
                NineSliceFrameCanvas(
                    spec = spec,
                    fallbackColor = if (hasOpaqueNineSlice) style.fillColor else Color.Transparent,
                    modifier = Modifier.matchParentSize(),
                )
            }

            // 2b. CSS pseudo-element overlays (`::before` / `::after`: tentacles, feet, duck head)
            style.pseudoLayers.forEach { layer ->
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

            // 5. P3R protagonist bust crop inside left 42dp gutter
            if (style.ornament is BubbleOrnament.P3rZeroHour) {
                P3rLeftPortraitLayer(
                    spriteSheetUrl = style.ornament.spriteSheetUrl,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(38.dp)
                        .matchParentSize(),
                )
            }

            // 6. Message body content
            Box(
                modifier = Modifier.padding(style.contentPadding),
                content = content,
            )
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
private fun P3rLeftPortraitLayer(
    spriteSheetUrl: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val httpClient = LocalCosmeticHttpClient.current
    val portrait by produceState<ImageBitmap?>(initialValue = null, spriteSheetUrl) {
        value = BadgeArtworkCache.getOrCropSpriteCell(
            context = context,
            okHttpClient = httpClient,
            source = spriteSheetUrl,
            cols = 7,
            rows = 9,
            col = 0,
            row = 0,
            reqSizePx = 2048,
        )
    }
    Box(modifier = modifier, contentAlignment = Alignment.BottomStart) {
        Canvas(modifier = Modifier.matchParentSize()) {
            // Cyan diagonal Persona stripe behind portrait
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width * 0.92f, 0f)
                lineTo(size.width * 0.56f, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(path, Color(0xFF031A5C).copy(alpha = 0.65f))
            drawLine(
                color = Color(0xFF52D6FF),
                start = Offset(size.width * 0.92f, 0f),
                end = Offset(size.width * 0.56f, size.height),
                strokeWidth = 1.6.dp.toPx(),
            )
        }
        portrait?.let { bmp ->
            Image(
                bitmap = bmp,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(start = 2.dp, bottom = 1.dp)
                    .size(34.dp)
                    .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 8.dp, topEnd = 6.dp, bottomEnd = 6.dp)),
            )
        }
    }
}

@Composable
private fun BubblePatternLayer(
    style: ResolvedChatBubbleStyle,
    modifier: Modifier = Modifier,
) {
    val orn = style.ornament
    if (orn == BubbleOrnament.None) return
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
                    drawCircle(
                        color = if (idx == 4) Color(0xFFFDE68A).copy(alpha = 0.72f) else Color.White.copy(alpha = 0.55f),
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
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF00F0FF).copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.2f),
                        radius = maxOf(size.width * 0.45f, 24f),
                    ),
                    radius = maxOf(size.width * 0.45f, 24f),
                    center = Offset(size.width * 0.85f, size.height * 0.2f),
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
            BubbleOrnament.LantingInk -> {
                // Red cinnabar seal mark at top-right corner
                val sealSize = 9.dp.toPx()
                val sx = size.width - sealSize - 6.dp.toPx()
                val sy = 6.dp.toPx()
                drawRoundRect(
                    color = Color(0xFF991B1B).copy(alpha = 0.75f),
                    topLeft = Offset(sx, sy),
                    size = Size(sealSize, sealSize),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
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
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFF8DC).copy(alpha = 0.24f), Color.Transparent),
                        center = Offset(size.width * 0.88f, size.height * 0.16f),
                        radius = 22.dp.toPx(),
                    ),
                    radius = 22.dp.toPx(),
                    center = Offset(size.width * 0.88f, size.height * 0.16f),
                )
                val sparks = listOf(0.18f to 0.92f, 0.58f to 0.90f, 0.86f to 0.12f)
                sparks.forEach { (fx, fy) ->
                    drawCircle(
                        color = style.borderColor.copy(alpha = 0.55f),
                        radius = 1.6.dp.toPx(),
                        center = Offset(size.width * fx, size.height * fy),
                    )
                }
            }
            BubbleOrnament.PresetPetal, BubbleOrnament.PresetSakura -> {
                val petals = listOf(0.88f to 0.24f, 0.14f to 0.78f, 0.92f to 0.72f)
                petals.forEach { (fx, fy) ->
                    drawCircle(
                        color = Color(0xFFF472B6).copy(alpha = 0.35f),
                        radius = 2.6.dp.toPx(),
                        center = Offset(size.width * fx, size.height * fy),
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
private fun BubbleCornerMascotLayer(
    ornament: BubbleOrnament,
    imageLoader: ImageLoader?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val httpClient = LocalCosmeticHttpClient.current

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
                            .offset(x = 14.dp, y = 2.dp)
                            .size(22.dp),
                    )
                }
            }
        }
        is BubbleOrnament.PersonalV2Mascot -> {
            val rewritten = remember(ornament.webpUrl) { MediaUrlRewriter.rewriteMediaUrl(ornament.webpUrl) }
            Box(modifier = modifier) {
                if (imageLoader != null) {
                    AsyncImage(
                        model = rewritten,
                        imageLoader = imageLoader,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .offset(x = 15.dp, y = 0.dp)
                            .width(24.dp)
                            .height(28.dp),
                    )
                } else {
                    val bmp by produceState<ImageBitmap?>(initialValue = null, ornament.webpUrl) {
                        value = BadgeArtworkCache.getOrFetchBitmap(context, httpClient, ornament.webpUrl, 192)
                    }
                    bmp?.let {
                        Image(
                            bitmap = it,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .offset(x = 15.dp, y = 0.dp)
                                .width(24.dp)
                                .height(28.dp),
                        )
                    }
                }
            }
        }
        is BubbleOrnament.RaffleChickengun -> {
            val sz = ornament.sizeDp.coerceIn(18f, 44f).dp
            val rewritten = remember(ornament.webpUrl) { MediaUrlRewriter.rewriteMediaUrl(ornament.webpUrl) }
            Box(modifier = modifier) {
                if (imageLoader != null) {
                    AsyncImage(
                        model = rewritten,
                        imageLoader = imageLoader,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (ornament.sizeDp * 0.56f).dp, y = 2.dp)
                            .size(sz),
                    )
                } else {
                    val bmp by produceState<ImageBitmap?>(initialValue = null, ornament.webpUrl) {
                        value = BadgeArtworkCache.getOrFetchBitmap(context, httpClient, ornament.webpUrl, 192)
                    }
                    bmp?.let {
                        Image(
                            bitmap = it,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = (ornament.sizeDp * 0.56f).dp, y = 2.dp)
                                .size(sz),
                        )
                    }
                }
            }
        }
        is BubbleOrnament.RaffleRailcannon -> {
            val rewritten = remember(ornament.cannonUrl) { MediaUrlRewriter.rewriteMediaUrl(ornament.cannonUrl) }
            Box(modifier = modifier) {
                if (imageLoader != null) {
                    AsyncImage(
                        model = rewritten,
                        imageLoader = imageLoader,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 16.dp, y = (-3).dp)
                            .size(22.dp),
                    )
                }
            }
        }
        is BubbleOrnament.RaffleWhalebuddy -> {
            val rewritten = remember(ornament.personUrl) { MediaUrlRewriter.rewriteMediaUrl(ornament.personUrl) }
            Box(modifier = modifier) {
                if (imageLoader != null) {
                    AsyncImage(
                        model = rewritten,
                        imageLoader = imageLoader,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = 8.dp, y = 0.dp)
                            .width(36.dp)
                            .height(21.dp),
                    )
                } else {
                    val staticUrl = "https://52dmeme.com/assets/raffle/whale-motion/person-still.webp"
                    val bmp by produceState<ImageBitmap?>(initialValue = null, staticUrl) {
                        value = BadgeArtworkCache.getOrFetchBitmap(context, httpClient, staticUrl, 128)
                    }
                    bmp?.let {
                        Image(
                            bitmap = it,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = 8.dp, y = 0.dp)
                                .width(36.dp)
                                .height(21.dp),
                        )
                    }
                }
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
    style?.dynamicCssSpec?.let { spec ->
        LaunchedEffect(spec) {
            BadgeArtworkCache.ensureCommunityCssLoaded(context, httpClient, spec)
        }
    }

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
        (style?.nineSliceSpec?.outsetLeftDp ?: 0f).coerceAtLeast(0f),
    ).dp
    val topRes = max(
        style?.topOverflowReserveDp ?: 0f,
        (style?.nineSliceSpec?.outsetTopDp ?: 0f).coerceAtLeast(0f),
    ).dp
    val endRes = max(
        style?.endOverflowReserveDp ?: 0f,
        (style?.nineSliceSpec?.outsetRightDp ?: 0f).coerceAtLeast(0f),
    ).dp
    val bottomRes = max(
        style?.bottomOverflowReserveDp ?: 0f,
        (style?.nineSliceSpec?.outsetBottomDp ?: 0f).coerceAtLeast(0f),
    ).dp
    val hasNineSlice = style?.nineSliceSpec != null
    val shouldClip = !hasNineSlice

    Box(
        modifier = modifier
            .padding(start = startRes, top = topRes, end = endRes, bottom = bottomRes)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .then(if (shouldClip) Modifier.clip(pillShape) else Modifier)
                .then(
                    when {
                        hasNineSlice -> Modifier
                        style?.fillBrush != null -> Modifier.background(style.fillBrush)
                        style != null && style.fillColor != Color.Transparent -> Modifier.background(style.fillColor)
                        else -> Modifier.background(defaultContainerColor)
                    },
                )
                .then(
                    when {
                        hasNineSlice -> Modifier
                        style?.borderBrush != null && style.borderWidthDp > 0f ->
                            Modifier.border(style.borderWidthDp.dp, style.borderBrush, pillShape)
                        style != null && style.borderWidthDp > 0f && style.borderColor != Color.Transparent ->
                            Modifier.border(style.borderWidthDp.dp, style.borderColor, pillShape)
                        defaultBorderColor != Color.Transparent ->
                            Modifier.border(0.8.dp, defaultBorderColor, pillShape)
                        else -> Modifier
                    },
                ),
        ) {
            style?.nineSliceSpec?.let { spec ->
                NineSliceFrameCanvas(
                    spec = spec,
                    fallbackColor = style.fillColor,
                    modifier = Modifier.matchParentSize(),
                )
            }
            style?.pseudoLayers?.forEach { layer ->
                BubblePseudoLayerCanvas(
                    layer = layer,
                    modifier = Modifier.matchParentSize(),
                )
            }
            Box(
                modifier = Modifier.padding(
                    style?.contentPadding ?: PaddingValues(horizontal = 8.dp, vertical = 3.dp),
                ),
                content = content,
            )
        }
    }
}
