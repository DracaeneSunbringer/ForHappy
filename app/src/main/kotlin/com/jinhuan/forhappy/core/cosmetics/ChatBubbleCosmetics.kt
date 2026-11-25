package com.jinhuan.forhappy.core.cosmetics

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jinhuan.forhappy.core.clock.ServerClock
import com.jinhuan.forhappy.core.model.ChatBoost
import com.jinhuan.forhappy.core.model.ChatUser
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Authoritative chat bubble, boost frame, title typography & safe-CSS cosmetic engine
 * aligned 1:1 with `kourou25/meme-hall-frontend` (`app.js`, `chat.css`, `official-titles.css`,
 * `personal-palettes.css`, `personal.css`, `personal-v2-art.css`, `title-bubble-art.css`,
 * `title-growth.css`, `title-anime-palettes.css`, `title-symbols.js`, `shiguang-skin.css`,
 * `owner52.css`) and `kourou25/meme-hall-skins`.
 */

data class ParsedSafeCss(
    val textColor: Color? = null,
    val bgColor: Color? = null,
    val bgGradientColors: List<Color>? = null,
    val bgGradientAngleDeg: Float? = null,
    val borderColor: Color? = null,
    val borderWidthDp: Float? = null,
    val cornerRadiusDp: Float? = null,
    val cornerRadiiDp: List<Float>? = null,
    val fontWeight: FontWeight? = null,
    val fontStyle: FontStyle? = null,
    val letterSpacingSp: Float? = null,
    val opacity: Float? = null,
)

data class OfficialPaletteSpec(
    val inkHex: String,
    val softHex: String,
    val edgeHex: String,
    val accentHex: String,
    val cornersDp: List<Float>, // topStart, topEnd, bottomEnd, bottomStart
)

data class PersonalV1PaletteSpec(
    val inkHex: String,
    val paperHex: String,
    val accentHex: String,
)

data class PersonalV2ArtSpec(
    val titleX: Float = 0.50f,
    val titleY: Float = 0.50f,
    val titlePadDp: Float = 16f,
)

data class PseudoBoxShadowDot(
    val dxDp: Float,
    val dyDp: Float,
    val spreadDp: Float,
    val color: Color,
)

sealed interface CssTimingFn {
    fun transform(t: Float): Float

    data object Linear : CssTimingFn {
        override fun transform(t: Float): Float = t.coerceIn(0f, 1f)
    }

    data class CubicBezier(
        val x1: Float,
        val y1: Float,
        val x2: Float,
        val y2: Float,
    ) : CssTimingFn {
        override fun transform(t: Float): Float {
            val x = t.coerceIn(0f, 1f)
            if (x <= 0f) return 0f
            if (x >= 1f) return 1f
            var lo = 0f
            var hi = 1f
            var u = x
            repeat(14) {
                val xEst = sampleBezier(u, x1, x2)
                if (xEst < x) lo = u else hi = u
                u = (lo + hi) * 0.5f
            }
            return sampleBezier(u, y1, y2).coerceIn(-0.5f, 1.5f)
        }

        private fun sampleBezier(u: Float, p1: Float, p2: Float): Float {
            val inv = 1f - u
            return 3f * inv * inv * u * p1 + 3f * inv * u * u * p2 + u * u * u
        }

        companion object {
            val Ease = CubicBezier(0.25f, 0.1f, 0.25f, 1f)
            val EaseIn = CubicBezier(0.42f, 0f, 1f, 1f)
            val EaseOut = CubicBezier(0f, 0f, 0.58f, 1f)
            val EaseInOut = CubicBezier(0.42f, 0f, 0.58f, 1f)
        }
    }

    data class Steps(
        val count: Int,
        val jumpStart: Boolean = false,
        val jumpNone: Boolean = false,
    ) : CssTimingFn {
        override fun transform(t: Float): Float {
            val n = count.coerceAtLeast(1)
            val x = t.coerceIn(0f, 0.999999f)
            val raw = floor(x * n).toInt()
            return when {
                jumpNone && n > 1 -> (raw.toFloat() / (n - 1).toFloat()).coerceIn(0f, 1f)
                jumpStart -> ((raw + 1).toFloat() / n.toFloat()).coerceIn(0f, 1f)
                else -> (raw.toFloat() / n.toFloat()).coerceIn(0f, 1f)
            }
        }
    }
}

enum class CssAnimDirection { NORMAL, REVERSE, ALTERNATE, ALTERNATE_REVERSE }

data class CssAnimSpec(
    val name: String,
    val durationMs: Long,
    val delayMs: Long = 0L,
    val timing: CssTimingFn = CssTimingFn.CubicBezier.Ease,
    val direction: CssAnimDirection = CssAnimDirection.NORMAL,
    val iterationCount: Float = Float.POSITIVE_INFINITY,
) {
    val cyclePeriodMs: Long
        get() = when {
            durationMs <= 0L -> 0L
            direction == CssAnimDirection.ALTERNATE || direction == CssAnimDirection.ALTERNATE_REVERSE -> durationMs * 2L
            else -> durationMs
        }

    fun directedFracAt(elapsedMs: Long): Float {
        if (durationMs <= 0L) return 0f
        val shifted = elapsedMs - delayMs
        if (shifted < 0L) {
            return if (direction == CssAnimDirection.REVERSE || direction == CssAnimDirection.ALTERNATE_REVERSE) 1f else 0f
        }
        val totalCycles = shifted.toDouble() / durationMs.toDouble()
        if (!iterationCount.isInfinite() && totalCycles >= iterationCount.toDouble()) {
            val completed = kotlin.math.ceil(iterationCount.toDouble()).toLong() - 1L
            val lastFrac = (iterationCount - completed.toFloat()).coerceIn(0f, 1f)
            val forward = when (direction) {
                CssAnimDirection.NORMAL -> true
                CssAnimDirection.REVERSE -> false
                CssAnimDirection.ALTERNATE -> (completed % 2L) == 0L
                CssAnimDirection.ALTERNATE_REVERSE -> (completed % 2L) != 0L
            }
            return if (forward) lastFrac else (1f - lastFrac)
        }
        val cycleIdx = floor(totalCycles).toLong()
        val frac = (totalCycles - cycleIdx).toFloat().coerceIn(0f, 0.999999f)
        val forward = when (direction) {
            CssAnimDirection.NORMAL -> true
            CssAnimDirection.REVERSE -> false
            CssAnimDirection.ALTERNATE -> (cycleIdx % 2L) == 0L
            CssAnimDirection.ALTERNATE_REVERSE -> (cycleIdx % 2L) != 0L
        }
        return if (forward) frac else (1f - frac)
    }

    fun progressAt(elapsedMs: Long): Float = timing.transform(directedFracAt(elapsedMs))
}

data class CssVisualTransformStop(
    val fraction: Float,
    val txDpDelta: Float? = null,
    val tyDpDelta: Float? = null,
    val txSelfDelta: Float? = null,
    val tySelfDelta: Float? = null,
    val scaleX: Float? = null,
    val scaleY: Float? = null,
    val rotateDeg: Float? = null,
    val opacity: Float? = null,
)

data class CssVisualAnimTrack(
    val spec: CssAnimSpec,
    val stops: List<CssVisualTransformStop>,
)

data class EvaluatedVisualAnim(
    val txDpDelta: Float = 0f,
    val tyDpDelta: Float = 0f,
    val txSelfDelta: Float = 0f,
    val tySelfDelta: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val rotateDeg: Float = 0f,
    val opacity: Float? = null,
)

data class PseudoLayerTimeline(
    val periodMs: Long,
    val frames: List<BubblePseudoLayer>,
)

data class BubbleTimelineFrame(
    val fillColor: Color,
    val fillGradColors: List<Color>? = null,
    val fillGradAngleDeg: Float? = null,
    val textColor: Color,
    val linkColor: Color,
    val mutedColor: Color,
    val borderColor: Color,
    val borderGradColors: List<Color>? = null,
    val nineSliceSpec: NineSliceSpec? = null,
    val pseudoLayers: List<BubblePseudoLayer> = emptyList(),
    val opacity: Float = 1f,
)

data class CommunityBubbleTimeline(
    val periodMs: Long,
    val frames: List<BubbleTimelineFrame>,
)

data class TitleTimelineFrame(
    val bgColors: List<Color>,
    val bgAngleDeg: Float?,
    val borderColor: Color,
    val borderGradientColors: List<Color>?,
    val textColor: Color,
    val nineSliceSpec: NineSliceSpec?,
    val pseudoLayers: List<BubblePseudoLayer>,
    val leadingIconUrl: String?,
    val trailingIconUrl: String?,
)

data class CommunityTitleTimeline(
    val periodMs: Long,
    val frames: List<TitleTimelineFrame>,
)

data class BadgeTimelineFrame(
    val baseUrlIdx: Int,
    val spriteCol: Int,
    val spriteRow: Int,
    val pseudoLayers: List<BubblePseudoLayer> = emptyList(),
    val opacity: Float = 1f,
)

data class CommunityBadgeTimeline(
    val periodMs: Long,
    val frames: List<BadgeTimelineFrame>,
)

data class CommunityTitleRenderSpec(
    val fixedHeightDp: Float? = null,
    val minHeightDp: Float = 22f,
    val padTopDp: Float = 2f,
    val padRightDp: Float = 8f,
    val padBottomDp: Float = 2f,
    val padLeftDp: Float = 8f,
    val startOverflowReserveDp: Float = 0f,
    val endOverflowReserveDp: Float = 0f,
    val topOverflowReserveDp: Float = 0f,
    val bottomOverflowReserveDp: Float = 0f,
    val bgColors: List<Color> = emptyList(),
    val bgAngleDeg: Float? = null,
    val borderColor: Color = Color.Transparent,
    val borderWidthDp: Float = 0f,
    val cornerRadiiDp: List<Float> = listOf(999f, 999f, 999f, 999f),
    val textColor: Color = Color(0xFF1E293B),
    val fontWeight: FontWeight = FontWeight.Bold,
    val letterSpacingSp: Float? = null,
    val textShadowColor: Color? = null,
    val nineSliceSpec: NineSliceSpec? = null,
    val pseudoLayers: List<BubblePseudoLayer> = emptyList(),
    val borderGradientColors: List<Color>? = null,
    val visualAnim: List<CssVisualAnimTrack> = emptyList(),
    val animationTimeline: CommunityTitleTimeline? = null,
)

data class CommunityBadgeRenderSpec(
    val widthDp: Float = 24f,
    val heightDp: Float = 24f,
    val hasImage: Boolean = false,
    val hasBaseImage: Boolean = false,
    val hasDarkImageVariant: Boolean = false,
    val baseUrlIdx: Int = 0,
    val spriteCols: Int = 1,
    val spriteRows: Int = 1,
    val spriteCol: Int = 0,
    val spriteRow: Int = 0,
    val topReserveDp: Float = 0f,
    val rightReserveDp: Float = 0f,
    val bottomReserveDp: Float = 0f,
    val leftReserveDp: Float = 0f,
    val pseudoLayers: List<BubblePseudoLayer> = emptyList(),
    val visualAnim: List<CssVisualAnimTrack> = emptyList(),
    val animationTimeline: CommunityBadgeTimeline? = null,
)

data class RaffleBubbleSliceSpec(
    val framePath: String,
    val sliceTop: Int,
    val sliceRight: Int,
    val sliceBottom: Int,
    val sliceLeft: Int,
    val topRatio: Float,
    val bottomRatio: Float,
    val isCollectionStyle: Boolean = false,
    val borderTopDp: Float? = null,
    val borderRightDp: Float? = null,
    val borderBottomDp: Float? = null,
    val borderLeftDp: Float? = null,
    val inkHex: String? = null,
)

data class RaffleTitleBannerSpec(
    val id: String,
    val bannerPath: String,
    val titleXFraction: Float,
    val titleYFraction: Float,
    val inkHex: String,
    val padLeftDp: Float = 14f,
    val padRightDp: Float = 14f,
    val minWidthDp: Float = 0f,
    val heightDp: Float = 26f,
)

sealed interface BubbleShapeKind {
    data class Rounded(
        val topStartDp: Float = 15f,
        val topEndDp: Float = 15f,
        val bottomEndDp: Float = 15f,
        val bottomStartDp: Float = 15f,
    ) : BubbleShapeKind

    data class CutCyber(val cutDp: Float = 10f) : BubbleShapeKind
}

data class NineSliceSpec(
    val sourceUrl: String,
    val sliceTop: Float,
    val sliceRight: Float,
    val sliceBottom: Float,
    val sliceLeft: Float,
    val isPercentSlice: Boolean = false,
    val fillCenter: Boolean = true,
    val borderTopDp: Float = 10f,
    val borderRightDp: Float = 14f,
    val borderBottomDp: Float = 10f,
    val borderLeftDp: Float = 14f,
    val isEdgeTrimOnly: Boolean = false,
    val outsetTopDp: Float = 0f,
    val outsetRightDp: Float = 0f,
    val outsetBottomDp: Float = 0f,
    val outsetLeftDp: Float = 0f,
)

enum class BgScaleMode { STRETCH, CONTAIN, COVER }

data class BubbleBgImageSpec(
    val sourceUrl: String,
    val widthDp: Float? = null,
    val widthFraction: Float? = null,
    val widthSubFromFullDp: Float? = null,
    val heightDp: Float? = null,
    val heightFraction: Float? = null,
    val alignCenterX: Boolean = false,
    val offsetXDp: Float = 0f,
    val offsetRightDp: Float? = null,
    val alignCenterY: Boolean = false,
    val offsetYDp: Float = 0f,
    val offsetBottomDp: Float? = null,
    val scaleMode: BgScaleMode = BgScaleMode.STRETCH,
)

data class BubblePseudoLayer(
    val topDp: Float? = null,
    val rightDp: Float? = null,
    val bottomDp: Float? = null,
    val leftDp: Float? = null,
    val widthDp: Float? = null,
    val heightDp: Float? = null,
    val heightCapToParent: Boolean = false,
    val aspectRatio: Float? = null,
    val translateSelfX: Float = 0f,
    val translateSelfY: Float = 0f,
    val centerX: Boolean = false,
    val centerY: Boolean = false,
    val offsetXDp: Float = 0f,
    val offsetYDp: Float = 0f,
    val shapeFillColor: Color? = null,
    val shapeBorderColor: Color? = null,
    val shapeBorderWidthDp: Float = 0f,
    val isCircle: Boolean = false,
    val shadowDot: PseudoBoxShadowDot? = null,
    val bgImagesBottomToTop: List<BubbleBgImageSpec> = emptyList(),
    val nineSliceSpec: NineSliceSpec? = null,
    val opacity: Float = 1f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val rotateDeg: Float = 0f,
    val visualAnim: List<CssVisualAnimTrack> = emptyList(),
    val pseudoTimeline: PseudoLayerTimeline? = null,
)

sealed interface BubbleOrnament {
    data object None : BubbleOrnament
    data object PresetJelly : BubbleOrnament
    data object PresetPuff : BubbleOrnament
    data object PresetPaw : BubbleOrnament
    data object PresetGalaxy : BubbleOrnament
    data object PresetBit : BubbleOrnament
    data object PresetPetal : BubbleOrnament
    data object PresetBoba : BubbleOrnament
    data object PresetCyber : BubbleOrnament
    data object PresetPrism : BubbleOrnament
    data object PresetMint : BubbleOrnament
    data object PresetBunny : BubbleOrnament
    data object PresetLemon : BubbleOrnament
    data object PresetLaowu : BubbleOrnament
    data object PresetCandyStripes : BubbleOrnament
    data object PresetPaperLines : BubbleOrnament
    data object PresetSakura : BubbleOrnament
    data object PresetNightStars : BubbleOrnament
    data object PresetAurora : BubbleOrnament
    data object PresetYinlu : BubbleOrnament
    data class ShiguangBook(val level: Int) : BubbleOrnament
    data class PersonalV1Mascot(val svgUrl: String) : BubbleOrnament
    data class PersonalV2Mascot(val webpUrl: String) : BubbleOrnament
    data class RaffleChickengun(
        val webpUrl: String,
        val sizeDp: Float,
        val cgMotion: Boolean = true,
        val cgFire: Boolean = true,
        val stage: Int = 1,
    ) : BubbleOrnament
    data class RaffleRailcannon(val cannonUrl: String, val stage: Int) : BubbleOrnament
    data class RaffleWhalebuddy(val personUrl: String, val stage: Int) : BubbleOrnament
    data class Owner52(val skinIndex: Int, val isBVariant: Boolean) : BubbleOrnament
}

data class ResolvedChatBubbleStyle(
    val key: String,
    val fillColor: Color,
    val fillBrush: Brush? = null,
    val textColor: Color,
    val linkColor: Color,
    val mutedColor: Color,
    val borderColor: Color = Color.Transparent,
    val borderBrush: Brush? = null,
    val borderWidthDp: Float = 0f,
    val dashedBorder: Boolean = false,
    val doubleBorder: Boolean = false,
    val leftAccentBarColor: Color? = null,
    val leftAccentBarWidthDp: Float = 0f,
    val bottomAccentBarColor: Color? = null,
    val shapeKind: BubbleShapeKind = BubbleShapeKind.Rounded(15f, 15f, 15f, 15f),
    val shadowColor: Color? = null,
    val shadowOffsetDp: Pair<Float, Float> = 0f to 0f,
    val fontFamily: FontFamily? = null,
    val fontWeight: FontWeight? = null,
    val fontStyle: FontStyle? = null,
    val letterSpacingSp: Float? = null,
    val opacity: Float = 1f,
    val minWidthDp: Float = 0f,
    val minHeightDp: Float = 0f,
    val endOverflowReserveDp: Float = 0f,
    val contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    val nineSliceSpec: NineSliceSpec? = null,
    val pseudoLayers: List<BubblePseudoLayer> = emptyList(),
    val ornament: BubbleOrnament = BubbleOrnament.None,
    val dynamicCssSpec: String? = null,
    val topOverflowReserveDp: Float = 0f,
    val bottomOverflowReserveDp: Float = 0f,
    val startOverflowReserveDp: Float = 0f,
    val borderGradientColors: List<Color>? = null,
    val visualAnim: List<CssVisualAnimTrack> = emptyList(),
    val animationTimeline: CommunityBubbleTimeline? = null,
)

object ChatBubbleCosmetics {

    private const val ORIGIN = "https://52dmeme.com"

    val OFFICIAL_PALETTES: Map<String, OfficialPaletteSpec> = mapOf(
        "serene" to OfficialPaletteSpec("#28786f", "#eefbf4", "#91cbb7", "#bedfc9", listOf(18f, 18f, 7f, 18f)),
        "gentleman" to OfficialPaletteSpec("#ac315b", "#fff0f4", "#ec9aad", "#ffc1ca", listOf(17f, 7f, 17f, 7f)),
        "coder" to OfficialPaletteSpec("#316652", "#e9f7ec", "#70a487", "#b5ddbe", listOf(4f, 4f, 4f, 4f)),
        "banana" to OfficialPaletteSpec("#856022", "#fff8d9", "#dcb962", "#efb9d6", listOf(7f, 20f, 20f, 7f)),
        "peach" to OfficialPaletteSpec("#a75258", "#fff1e9", "#e7a69a", "#f9b9be", listOf(20f, 20f, 12f, 12f)),
        "oracle" to OfficialPaletteSpec("#65528f", "#f1efff", "#aa9acb", "#a8cfed", listOf(10f, 3f, 10f, 3f)),
        "whale" to OfficialPaletteSpec("#286986", "#eaf8ff", "#8cbed8", "#a4d9eb", listOf(20f, 20f, 20f, 5f)),
        "diver" to OfficialPaletteSpec("#9edcd6", "#183f4b", "#467f89", "#295f6c", listOf(13f, 13f, 5f, 5f)),
        "reborn" to OfficialPaletteSpec("#9d452f", "#fff3dc", "#d98a65", "#f5cf7e", listOf(5f, 15f, 5f, 15f)),
    )

    val PERSONAL_V1_PALETTES: Map<String, PersonalV1PaletteSpec> = mapOf(
        "p001" to PersonalV1PaletteSpec("#4b6a9b", "#dceaff", "#f1b6cd"),
        "p002" to PersonalV1PaletteSpec("#367a78", "#e0f2e9", "#f2bc72"),
        "p003" to PersonalV1PaletteSpec("#65528c", "#e9e3fa", "#efc97c"),
        "p004" to PersonalV1PaletteSpec("#7653a0", "#efe3ff", "#e9accc"),
        "p005" to PersonalV1PaletteSpec("#965638", "#fff0dc", "#e79f65"),
        "p006" to PersonalV1PaletteSpec("#3d668c", "#deeff7", "#e4a879"),
        "p007" to PersonalV1PaletteSpec("#386e94", "#e3f4ff", "#91c9db"),
        "p008" to PersonalV1PaletteSpec("#507f73", "#e5f5ed", "#bca6d8"),
        "p009" to PersonalV1PaletteSpec("#396c80", "#e3f3f2", "#b6c8ed"),
        "p010" to PersonalV1PaletteSpec("#86643e", "#fff2d9", "#c69b72"),
        "p011" to PersonalV1PaletteSpec("#4b6a9b", "#dceaff", "#f1b6cd"),
        "p012" to PersonalV1PaletteSpec("#765aa4", "#f6edff", "#e4bc64"),
        "p013" to PersonalV1PaletteSpec("#52577d", "#eeedf8", "#e9aa62"),
        "p014" to PersonalV1PaletteSpec("#537a43", "#eff6cd", "#e4cc67"),
        "p015" to PersonalV1PaletteSpec("#407c88", "#e3f4f4", "#e7a390"),
        "p016" to PersonalV1PaletteSpec("#526a82", "#eaf0f8", "#d6ac72"),
        "p017" to PersonalV1PaletteSpec("#985248", "#fff0e4", "#cf8f70"),
        "p018" to PersonalV1PaletteSpec("#86672c", "#fff8db", "#97cbd1"),
        "p019" to PersonalV1PaletteSpec("#477952", "#e5f3dc", "#c9bd72"),
        "p020" to PersonalV1PaletteSpec("#486c79", "#e6f5f5", "#87b6bc"),
        "p021" to PersonalV1PaletteSpec("#9b6637", "#fff0dd", "#b7a5d6"),
        "p022" to PersonalV1PaletteSpec("#6e5a97", "#eee7f7", "#dcbf78"),
        "p023" to PersonalV1PaletteSpec("#71859b", "#eff8ff", "#c2b7df"),
        "p024" to PersonalV1PaletteSpec("#5b558d", "#eae8ff", "#8abbd7"),
        "p025" to PersonalV1PaletteSpec("#60778c", "#edf2f8", "#cbd5a4"),
        "p026" to PersonalV1PaletteSpec("#9b666c", "#fff0ed", "#a6bcd5"),
        "p027" to PersonalV1PaletteSpec("#5d7b65", "#eaf2df", "#dfcb7b"),
        "p028" to PersonalV1PaletteSpec("#826240", "#fbefdf", "#91a8bd"),
        "p029" to PersonalV1PaletteSpec("#5d7b45", "#edf5d9", "#de9f82"),
        "p030" to PersonalV1PaletteSpec("#3c7b82", "#e0f4f2", "#e9aa96"),
        "p031" to PersonalV1PaletteSpec("#826c37", "#fbf4d8", "#b9ce87"),
        "p032" to PersonalV1PaletteSpec("#406e9c", "#e7f3ff", "#eac774"),
        "p033" to PersonalV1PaletteSpec("#626184", "#f1f0fc", "#c4afdb"),
        "p034" to PersonalV1PaletteSpec("#8a6253", "#fff0e0", "#dcbd6c"),
        "p035" to PersonalV1PaletteSpec("#3b7a72", "#def3e8", "#f1b3a1"),
        "p036" to PersonalV1PaletteSpec("#4f6791", "#e7f0fd", "#a5d3c5"),
        "p037" to PersonalV1PaletteSpec("#85534f", "#f6e6e3", "#acb8bd"),
        "p038" to PersonalV1PaletteSpec("#535f91", "#eaeffc", "#bad1e8"),
        "p039" to PersonalV1PaletteSpec("#658361", "#edf7eb", "#e0b9ca"),
        "p040" to PersonalV1PaletteSpec("#71549b", "#f0e6fd", "#c8b9df"),
        "p041" to PersonalV1PaletteSpec("#87634d", "#faf0e4", "#a7ced7"),
        "p042" to PersonalV1PaletteSpec("#8b633c", "#f8ecd8", "#cb9667"),
        "p043" to PersonalV1PaletteSpec("#7f743e", "#fff8dd", "#9cbfd7"),
        "p044" to PersonalV1PaletteSpec("#567e48", "#edf5df", "#d9c48f"),
        "p045" to PersonalV1PaletteSpec("#8c6679", "#f8e8ed", "#bea28c"),
        "p046" to PersonalV1PaletteSpec("#557f6e", "#edf7ed", "#dbb9cf"),
        "p047" to PersonalV1PaletteSpec("#936648", "#fff2d9", "#dd9a83"),
        "p048" to PersonalV1PaletteSpec("#816b51", "#f6eedf", "#bac998"),
        "p049" to PersonalV1PaletteSpec("#776143", "#faf0d8", "#b8a2bf"),
        "p050" to PersonalV1PaletteSpec("#8a684e", "#fff1dc", "#a1caca"),
        "p051" to PersonalV1PaletteSpec("#826991", "#fff3df", "#c3a6d8"),
        "p052" to PersonalV1PaletteSpec("#92585f", "#fae7e7", "#b4bfd6"),
        "p053" to PersonalV1PaletteSpec("#64804b", "#eff5de", "#dfa974"),
        "p054" to PersonalV1PaletteSpec("#527e87", "#edf8fa", "#add0d2"),
        "p055" to PersonalV1PaletteSpec("#587277", "#eaf1f0", "#bba68d"),
        "p056" to PersonalV1PaletteSpec("#5c80a0", "#eaf5ff", "#d4c6e8"),
        "p057" to PersonalV1PaletteSpec("#4f898a", "#e5f6f1", "#b8cb95"),
        "p058" to PersonalV1PaletteSpec("#7a719c", "#f1eefa", "#bfc7e5"),
        "p059" to PersonalV1PaletteSpec("#98704e", "#fff1e2", "#a2b9d6"),
    )

    val TITLE_SYMBOL_PATHS: Map<String, String> = mapOf(
        "oldfarmer" to "M15 8C6 5 4 16 3 21c5-1 16-3 13-12ZM8 11l3 2m-5 3 3 1M15 8l1-6 3 4 3-2-1 5-5 1",
        "newcomer" to "M12 21V11M12 14C3 15 3 6 3 6s9-1 9 8ZM12 11C12 3 21 3 21 3s1 8-9 8Z",
        "mona" to "M12 18C2 17 3 10 3 10s6 0 9 8Zm0 0c10-1 9-8 9-8s-6 0-9 8ZM12 18C5 11 12 3 12 3s7 8 0 15ZM5 21h14",
        "lace" to "M5 20c-5-4 0-9 4-7l4 3c6 1 8-5 4-8-3-2-5 2-2 3M5 17l5-2M8 20l4-3M17 8V4l4 1",
        "giant" to "M7 3c-5 10 2 18 11 16C8 16 9 8 10 5L7 3ZM7 3l1-2",
        "worship" to "M6 8c-3 9 5 14 13 10C9 17 9 12 10 8H6ZM5 3h9M4 6l-2 2M18 3l2 2",
        "muscle" to "M3 17V9l3-3 4 2-2 4 3 1 2-3 5 1 3 4-2 5H7l-4-3ZM6 7l2-3",
        "deepseek" to "M2 12c3-7 15-8 17-1l3-3v8l-3-2c-4 7-14 7-17-2ZM7 11h.1M12 6V3m-3 1 3-2 3 2",
        "deepseekupgrade" to "M3 13c3-6 11-6 14-1l5-4v9l-5-3c-3 6-12 6-14-1ZM6 6l3-3 3 3 3-3 3 3M8 12h.1",
        "maid" to "M12 10C5 1 0 8 4 14l8-4Zm0 0c7-9 12-2 8 4l-8-4ZM10 12l-3 9 5-3 5 3-3-9",
        "whalepunch" to "M3 15c3-6 10-6 13-1l6-3v9l-6-3c-5 4-11 3-13-2ZM9 2 6 8h5l-2 5 8-8h-5l2-3",
        "nightdevil" to "M6 8 3 3l1 10M18 8l3-5-1 10M4 11c2-7 14-7 16 0v3c0 8-16 8-16 0v-3ZM8 12l2 1m4 0 2-1M9 17q3 2 6 0",
        "whalebuddy" to "M2 13c2-7 13-8 16-2l4-4v10l-4-3c-3 7-14 7-16-1ZM7 11h.1M10 4l2 2 2-2",
        "gpt" to "M3 18c8 6 17-1 13-8l5 1-5-5 1-4-5 3-6-1 1 5-4 4 6-1-1 6M12 8h.1M10 15l5 3",
        "claude" to "M12 10c-8-10-12 4-3 3-10 7 4 12 4 3 8 9 12-5 3-4 7-9-7-13-4-2ZM12 11v3M10 12h4",
        "gemini" to "M7 3l2 5 5 2-5 2-2 5-2-5-4-2 4-2 2-5ZM17 10l2 4 4 2-4 2-2 4-2-4-4-2 4-2 2-4Z",
        "whitedragon" to "M3 19 6 5l6 3 6-3 3 14-9-4-9 4ZM6 5 7 1l5 3 5-3 1 4M8 11l1 1m6 0 1-1",
        "rascal" to "M4 9 2 4l6 2M16 6l6-2-2 5M4 10c2-5 14-5 16 0v6c-3 6-13 6-16 0v-6ZM7 12h3m4 0h3M8 17l8-2",
        "weirdo" to "M4 5q8-4 16 0v11q-8 10-16 0V5ZM7 9h3m5-1 2 3M7 16q5-6 10 0",
        "blacksilk" to "M4 22C1 10 10 2 21 2c0 10-8 18-17 20ZM4 22 17 6M6 15l8 1M9 10l8 1",
        "loli" to "M8 11C0-2 12-1 10 10m4 0C12-1 24-2 16 11M5 16c0-9 14-9 14 0s-14 9-14 0ZM9 15h.1m6 0h.1",
        "electric" to "M13 2 3 14h8l-1 8 11-13h-8l0-7Z",
        "railcannon" to "M2 16 13 5l4 4L6 20l-4-4ZM10 20 19 11l3 3-9 9-3-3ZM3 17l3 3M11 21l2 2M15 3l3-1 4 5-1 3",
        "chickengun" to "M2 14h12V9l7 2-3 5H8l-3 5H2l3-7ZM3 10h8M14 6l2-3M19 7l3-1",
        "blade" to "M6 22 3 19l5-5 2 2-4 6ZM8 13C13 9 15 4 20 2c2 8-2 13-8 16l-4-5Z",
        "spear" to "M3 22 15 10M14 11l-1-5 8-4-3 8-4 1ZM10 13l4 4",
        "sword" to "M3 21 8 16M5 14l5 5M8 14 17 3l4-1-1 4-10 10",
        "halberd" to "M5 22 18 2M12 7l-4 1 2 7 8 1 1-4M13 5l8 6M8 16l5 4",
        "snow" to "M12 2v20M3 7l18 10M3 17 21 7M9 4l3 3 3-3M9 20l3-3 3 3M3 10l4-1-1-4M18 19l-1-4 4-1",
        "moon" to "M18 3C5-1-2 13 8 20c6 4 13 0 14-5C12 20 6 9 18 3Z",
        "wind" to "M2 6h12c7 0 7-6 2-4M2 11h17c5 0 5 7 0 6M5 16h7c6 0 5 7 1 5",
        "flower" to "M12 9c-7-11-13 3-4 3-10 7 4 13 4 4 7 10 13-4 4-4 9-7-5-13-4-3ZM12 17v6m0-2 5-2",
        "sun" to "M8 8q4-4 8 0t0 8q-4 4-8 0T8 8ZM12 1v3m0 16v3M1 12h3m16 0h3M4 4l2 2m12 12 2 2M4 20l2-2M18 6l2-2",
        "star" to "M12 2l3 6 7 1-5 5 1 8-6-4-6 4 1-8-5-5 7-1 3-6ZM12 8v5",
        "cloud" to "M6 19C-1 17 1 9 7 10 7 2 19 2 19 11c7 1 5 8-1 8H6ZM7 22h9",
    )

    val PERSONAL_V2_ART_SPECS: Map<String, PersonalV2ArtSpec> = mapOf(
        "pv2-p001-doodle" to PersonalV2ArtSpec(0.50f, 0.51f, 16f),
        "pv2-p001-ssr" to PersonalV2ArtSpec(0.50f, 0.46f, 16f),
        "pv2-p002-doodle" to PersonalV2ArtSpec(0.50f, 0.51f, 16f),
        "pv2-p002-ssr" to PersonalV2ArtSpec(0.50f, 0.55f, 16f),
        "pv2-p003-doodle" to PersonalV2ArtSpec(0.50f, 0.46f, 16f),
        "pv2-p003-ssr" to PersonalV2ArtSpec(0.50f, 0.57f, 16f),
        "pv2-p004-doodle" to PersonalV2ArtSpec(0.50f, 0.50f, 16f),
        "pv2-p004-ssr" to PersonalV2ArtSpec(0.50f, 0.50f, 16f),
        "pv2-p005-doodle" to PersonalV2ArtSpec(0.50f, 0.50f, 16f),
        "pv2-p005-ssr" to PersonalV2ArtSpec(0.50f, 0.47f, 16f),
        "pv2-p006-doodle" to PersonalV2ArtSpec(0.50f, 0.40f, 16f),
        "pv2-p006-ssr" to PersonalV2ArtSpec(0.50f, 0.45f, 16f),
        "pv2-p007-doodle" to PersonalV2ArtSpec(0.50f, 0.51f, 16f),
        "pv2-p007-ssr" to PersonalV2ArtSpec(0.50f, 0.51f, 16f),
        "pv2-p008-doodle" to PersonalV2ArtSpec(0.50f, 0.52f, 16f),
        "pv2-p008-ssr" to PersonalV2ArtSpec(0.50f, 0.50f, 16f),
        "pv2-p009-doodle" to PersonalV2ArtSpec(0.50f, 0.50f, 16f),
        "pv2-p009-ssr" to PersonalV2ArtSpec(0.50f, 0.42f, 16f),
        "pv2-p010-doodle" to PersonalV2ArtSpec(0.50f, 0.50f, 16f),
        "pv2-p010-ssr" to PersonalV2ArtSpec(0.50f, 0.49f, 16f),
        "pv2-p011-doodle" to PersonalV2ArtSpec(0.50f, 0.50f, 16f),
        "pv2-p011-ssr" to PersonalV2ArtSpec(0.50f, 0.48f, 16f),
        "pv2-p012-doodle" to PersonalV2ArtSpec(0.50f, 0.56f, 16f),
        "pv2-p012-ssr" to PersonalV2ArtSpec(0.50f, 0.58f, 16f),
        "pv2-p013-doodle" to PersonalV2ArtSpec(0.50f, 0.51f, 16f),
        "pv2-p013-ssr" to PersonalV2ArtSpec(0.50f, 0.45f, 16f),
        "pv2-p014-doodle" to PersonalV2ArtSpec(0.50f, 0.50f, 16f),
        "pv2-p014-ssr" to PersonalV2ArtSpec(0.50f, 0.42f, 16f),
        "pv2-p015-doodle" to PersonalV2ArtSpec(0.50f, 0.58f, 16f),
        "pv2-p015-ssr" to PersonalV2ArtSpec(0.50f, 0.52f, 16f),
        "pv2-p016-doodle" to PersonalV2ArtSpec(0.50f, 0.49f, 16f),
        "pv2-p016-ssr" to PersonalV2ArtSpec(0.50f, 0.49f, 16f),
        "pv2-p017-doodle" to PersonalV2ArtSpec(0.50f, 0.48f, 16f),
        "pv2-p017-ssr" to PersonalV2ArtSpec(0.50f, 0.44f, 16f),
        "pv2-p018-doodle" to PersonalV2ArtSpec(0.50f, 0.58f, 16f),
        "pv2-p018-ssr" to PersonalV2ArtSpec(0.50f, 0.51f, 16f),
        "pv2-p019-doodle" to PersonalV2ArtSpec(0.50f, 0.56f, 16f),
        "pv2-p019-ssr" to PersonalV2ArtSpec(0.50f, 0.55f, 16f),
        "pv2-p020-doodle" to PersonalV2ArtSpec(0.50f, 0.52f, 16f),
        "pv2-p020-ssr" to PersonalV2ArtSpec(0.50f, 0.47f, 16f),
        "pv2-p021-doodle" to PersonalV2ArtSpec(0.50f, 0.55f, 16f),
        "pv2-p021-ssr" to PersonalV2ArtSpec(0.50f, 0.50f, 16f),
        "pv2-p022-doodle" to PersonalV2ArtSpec(0.50f, 0.56f, 16f),
        "pv2-p022-ssr" to PersonalV2ArtSpec(0.50f, 0.52f, 16f),
        "pv2-p023-doodle" to PersonalV2ArtSpec(0.50f, 0.52f, 16f),
        "pv2-p023-ssr" to PersonalV2ArtSpec(0.50f, 0.52f, 16f),
        "pv2-p024-doodle" to PersonalV2ArtSpec(0.50f, 0.46f, 16f),
        "pv2-p024-ssr" to PersonalV2ArtSpec(0.50f, 0.40f, 16f),
        "pv2-p025-doodle" to PersonalV2ArtSpec(0.50f, 0.56f, 16f),
        "pv2-p025-ssr" to PersonalV2ArtSpec(0.50f, 0.50f, 16f),
        "pv2-p026-doodle" to PersonalV2ArtSpec(0.50f, 0.50f, 16f),
        "pv2-p026-ssr" to PersonalV2ArtSpec(0.50f, 0.49f, 16f),
        "pv2-p027-doodle" to PersonalV2ArtSpec(0.50f, 0.45f, 16f),
        "pv2-p027-ssr" to PersonalV2ArtSpec(0.50f, 0.48f, 16f),
        "pv2-p028-doodle" to PersonalV2ArtSpec(0.50f, 0.50f, 16f),
        "pv2-p028-ssr" to PersonalV2ArtSpec(0.50f, 0.54f, 16f),
        "pv2-p029-doodle" to PersonalV2ArtSpec(0.50f, 0.50f, 16f),
        "pv2-p029-ssr" to PersonalV2ArtSpec(0.50f, 0.44f, 16f),
        "pv2-p030-doodle" to PersonalV2ArtSpec(0.50f, 0.57f, 16f),
        "pv2-p030-ssr" to PersonalV2ArtSpec(0.50f, 0.51f, 16f),
        "pv2-p031-doodle" to PersonalV2ArtSpec(0.50f, 0.53f, 16f),
        "pv2-p031-ssr" to PersonalV2ArtSpec(0.50f, 0.47f, 16f),
        "pv2-p032-doodle" to PersonalV2ArtSpec(0.50f, 0.54f, 16f),
        "pv2-p032-ssr" to PersonalV2ArtSpec(0.50f, 0.48f, 16f),
        "pv2-p033-doodle" to PersonalV2ArtSpec(0.50f, 0.48f, 16f),
        "pv2-p033-ssr" to PersonalV2ArtSpec(0.50f, 0.44f, 16f),
        "pv2-p034-doodle" to PersonalV2ArtSpec(0.50f, 0.51f, 16f),
        "pv2-p034-ssr" to PersonalV2ArtSpec(0.50f, 0.47f, 16f),
        "pv2-p035-doodle" to PersonalV2ArtSpec(0.50f, 0.53f, 16f),
        "pv2-p035-ssr" to PersonalV2ArtSpec(0.50f, 0.54f, 16f),
        "pv2-p036-doodle" to PersonalV2ArtSpec(0.50f, 0.48f, 16f),
        "pv2-p036-ssr" to PersonalV2ArtSpec(0.50f, 0.52f, 16f),
        "pv2-p037-doodle" to PersonalV2ArtSpec(0.50f, 0.51f, 16f),
        "pv2-p037-ssr" to PersonalV2ArtSpec(0.50f, 0.48f, 16f),
        "pv2-p038-doodle" to PersonalV2ArtSpec(0.50f, 0.60f, 16f),
        "pv2-p038-ssr" to PersonalV2ArtSpec(0.50f, 0.40f, 16f),
        "pv2-p039-doodle" to PersonalV2ArtSpec(0.50f, 0.52f, 16f),
        "pv2-p039-ssr" to PersonalV2ArtSpec(0.50f, 0.42f, 16f),
        "pv2-p040-doodle" to PersonalV2ArtSpec(0.50f, 0.47f, 16f),
        "pv2-p040-ssr" to PersonalV2ArtSpec(0.50f, 0.44f, 16f),
        "pv2-p041-doodle" to PersonalV2ArtSpec(0.50f, 0.49f, 16f),
        "pv2-p041-ssr" to PersonalV2ArtSpec(0.50f, 0.53f, 16f),
        "pv2-p042-doodle" to PersonalV2ArtSpec(0.50f, 0.53f, 16f),
        "pv2-p042-ssr" to PersonalV2ArtSpec(0.50f, 0.44f, 16f),
        "pv2-p043-doodle" to PersonalV2ArtSpec(0.50f, 0.49f, 16f),
        "pv2-p043-ssr" to PersonalV2ArtSpec(0.50f, 0.52f, 16f),
        "pv2-p044-doodle" to PersonalV2ArtSpec(0.50f, 0.52f, 16f),
        "pv2-p044-ssr" to PersonalV2ArtSpec(0.50f, 0.50f, 16f),
        "pv2-p045-doodle" to PersonalV2ArtSpec(0.50f, 0.52f, 16f),
        "pv2-p045-ssr" to PersonalV2ArtSpec(0.50f, 0.49f, 16f),
        "pv2-p046-doodle" to PersonalV2ArtSpec(0.50f, 0.56f, 16f),
        "pv2-p046-ssr" to PersonalV2ArtSpec(0.50f, 0.49f, 16f),
        "pv2-p047-doodle" to PersonalV2ArtSpec(0.50f, 0.50f, 16f),
        "pv2-p047-ssr" to PersonalV2ArtSpec(0.50f, 0.47f, 16f),
        "pv2-p048-doodle" to PersonalV2ArtSpec(0.50f, 0.52f, 16f),
        "pv2-p048-ssr" to PersonalV2ArtSpec(0.50f, 0.47f, 16f),
        "pv2-p049-doodle" to PersonalV2ArtSpec(0.50f, 0.50f, 16f),
        "pv2-p049-ssr" to PersonalV2ArtSpec(0.50f, 0.41f, 16f),
        "pv2-p050-doodle" to PersonalV2ArtSpec(0.50f, 0.56f, 16f),
        "pv2-p050-ssr" to PersonalV2ArtSpec(0.50f, 0.51f, 16f),
        "pv2-p051-doodle" to PersonalV2ArtSpec(0.50f, 0.54f, 16f),
        "pv2-p051-ssr" to PersonalV2ArtSpec(0.50f, 0.56f, 16f),
        "pv2-p052-doodle" to PersonalV2ArtSpec(0.50f, 0.48f, 16f),
        "pv2-p052-ssr" to PersonalV2ArtSpec(0.50f, 0.46f, 16f),
        "pv2-p053-doodle" to PersonalV2ArtSpec(0.50f, 0.56f, 16f),
        "pv2-p053-ssr" to PersonalV2ArtSpec(0.50f, 0.40f, 16f),
        "pv2-p054-doodle" to PersonalV2ArtSpec(0.50f, 0.51f, 16f),
        "pv2-p054-ssr" to PersonalV2ArtSpec(0.50f, 0.48f, 16f),
        "pv2-p055-doodle" to PersonalV2ArtSpec(0.50f, 0.46f, 16f),
        "pv2-p055-ssr" to PersonalV2ArtSpec(0.50f, 0.45f, 16f),
        "pv2-p056-doodle" to PersonalV2ArtSpec(0.50f, 0.49f, 16f),
        "pv2-p056-ssr" to PersonalV2ArtSpec(0.50f, 0.51f, 16f),
        "pv2-p057-doodle" to PersonalV2ArtSpec(0.50f, 0.41f, 16f),
        "pv2-p057-ssr" to PersonalV2ArtSpec(0.50f, 0.40f, 16f),
        "pv2-p058-doodle" to PersonalV2ArtSpec(0.50f, 0.42f, 16f),
        "pv2-p058-ssr" to PersonalV2ArtSpec(0.50f, 0.44f, 16f),
        "pv2-p059-doodle" to PersonalV2ArtSpec(0.50f, 0.53f, 16f),
        "pv2-p059-ssr" to PersonalV2ArtSpec(0.50f, 0.52f, 16f),
    )

    private val dynamicPersonalV2Art = ConcurrentHashMap<String, PersonalV2ArtSpec>()
    private val dynamicPersonalV1Palettes = ConcurrentHashMap<String, PersonalV1PaletteSpec>()
    private val dynamicTitleSymbolPaths = ConcurrentHashMap<String, String>()

    fun resolvePersonalV2Art(id: String): PersonalV2ArtSpec =
        dynamicPersonalV2Art[id] ?: PERSONAL_V2_ART_SPECS[id] ?: PersonalV2ArtSpec()

    fun resolvePersonalV1Palette(id: String): PersonalV1PaletteSpec =
        dynamicPersonalV1Palettes[id] ?: PERSONAL_V1_PALETTES[id]
        ?: PersonalV1PaletteSpec("#367a78", "#e0f2e9", "#f2bc72")

    fun resolveTitleSymbolPath(id: String): String =
        dynamicTitleSymbolPaths[id] ?: TITLE_SYMBOL_PATHS[id] ?: TITLE_SYMBOL_PATHS.getValue("star")

    fun parsePersonalV2ArtCss(cssText: String): Map<String, PersonalV2ArtSpec> {
        if (cssText.isBlank()) return emptyMap()
        val ruleRe = Regex("""\.pt-(pv2-[a-z0-9-]+)\s*\{([^}]*)\}""")
        val xRe = Regex("""--pv-title-x\s*:\s*(\d+(?:\.\d+)?)%""")
        val yRe = Regex("""--pv-title-y\s*:\s*(\d+(?:\.\d+)?)%""")
        val padRe = Regex("""--pv-title-pad\s*:\s*(\d+(?:\.\d+)?)px""")
        val out = LinkedHashMap<String, PersonalV2ArtSpec>()
        for (m in ruleRe.findAll(cssText)) {
            val id = m.groupValues[1]
            val body = m.groupValues[2]
            val x = (xRe.find(body)?.groupValues?.get(1)?.toFloatOrNull() ?: 50f) / 100f
            val y = (yRe.find(body)?.groupValues?.get(1)?.toFloatOrNull() ?: 50f) / 100f
            val pad = padRe.find(body)?.groupValues?.get(1)?.toFloatOrNull() ?: 16f
            out[id] = PersonalV2ArtSpec(x, y, pad)
        }
        if (out.isNotEmpty()) {
            dynamicPersonalV2Art.putAll(out)
        }
        return out
    }

    fun parsePersonalPalettesCss(cssText: String): Map<String, PersonalV1PaletteSpec> {
        if (cssText.isBlank()) return emptyMap()
        val ruleRe = Regex("""\.pt-(p\d+)\s*\{([^}]*)\}""")
        val inkRe = Regex("""--pt-ink\s*:\s*(#[0-9a-fA-F]{3,8})""")
        val paperRe = Regex("""--pt-paper\s*:\s*(#[0-9a-fA-F]{3,8})""")
        val accentRe = Regex("""--pt-accent\s*:\s*(#[0-9a-fA-F]{3,8})""")
        val out = LinkedHashMap<String, PersonalV1PaletteSpec>()
        for (m in ruleRe.findAll(cssText)) {
            val id = m.groupValues[1]
            val body = m.groupValues[2]
            val ink = inkRe.find(body)?.groupValues?.get(1) ?: continue
            val paper = paperRe.find(body)?.groupValues?.get(1) ?: continue
            val accent = accentRe.find(body)?.groupValues?.get(1) ?: ink
            out[id] = PersonalV1PaletteSpec(ink, paper, accent)
        }
        if (out.isNotEmpty()) {
            dynamicPersonalV1Palettes.putAll(out)
        }
        return out
    }

    fun parseTitleSymbolsJs(jsText: String): Map<String, String> {
        if (jsText.isBlank()) return emptyMap()
        val pairRe = Regex("""([a-z0-9_-]+)\s*:\s*['"](M[^'"]+)['"]""")
        val out = LinkedHashMap<String, String>()
        for (m in pairRe.findAll(jsText)) {
            val id = m.groupValues[1].trim()
            val path = m.groupValues[2].trim()
            if (id.isNotEmpty() && path.isNotEmpty()) {
                out[id] = path
            }
        }
        if (out.isNotEmpty()) {
            dynamicTitleSymbolPaths.putAll(out)
        }
        return out
    }

    fun applyWebDynamicAssets(
        v2ArtSpecs: Map<String, PersonalV2ArtSpec> = emptyMap(),
        v1Palettes: Map<String, PersonalV1PaletteSpec> = emptyMap(),
        symbolPaths: Map<String, String> = emptyMap(),
    ) {
        if (v2ArtSpecs.isNotEmpty()) dynamicPersonalV2Art.putAll(v2ArtSpecs)
        if (v1Palettes.isNotEmpty()) dynamicPersonalV1Palettes.putAll(v1Palettes)
        if (symbolPaths.isNotEmpty()) dynamicTitleSymbolPaths.putAll(symbolPaths)
    }

    val RAFFLE_BUBBLE_SLICES: Map<String, RaffleBubbleSliceSpec> = mapOf(
        "gpt" to RaffleBubbleSliceSpec(
            "/assets/raffle/collection/gpt-frame.webp", 220, 200, 70, 355, 0.2500f, 0.1429f,
            isCollectionStyle = true,
            borderTopDp = 27f, borderRightDp = 27f, borderBottomDp = 8f, borderLeftDp = 48f, inkHex = "#67528a",
        ),
        "claude" to RaffleBubbleSliceSpec("/assets/raffle/bubble-anime-v9/claude.webp", 35, 140, 20, 140, 0.2500f, 0.1429f),
        "gemini" to RaffleBubbleSliceSpec(
            "/assets/raffle/collection/gemini-frame.webp", 265, 175, 40, 505, 0.2500f, 0.1429f,
            isCollectionStyle = true,
            borderTopDp = 27f, borderRightDp = 21f, borderBottomDp = 8f, borderLeftDp = 60f, inkHex = "#624785",
        ),
        "deepseek" to RaffleBubbleSliceSpec("/assets/raffle/bubble-anime-v9/deepseek.webp", 35, 140, 20, 140, 0.2500f, 0.1429f),
        "deepseekupgrade" to RaffleBubbleSliceSpec(
            "/assets/raffle/collection/deepseekupgrade-frame.webp", 40, 145, 65, 145, 0.50f, 0.60f,
            isCollectionStyle = true,
            borderTopDp = 7.2f, borderRightDp = 14.4f, borderBottomDp = 8.64f, borderLeftDp = 14.4f, inkHex = "#203e70",
        ),
        "maid" to RaffleBubbleSliceSpec(
            "/assets/raffle/collection/maid-frame.webp", 65, 160, 120, 160, 0.54f, 0.63f,
            isCollectionStyle = true,
            borderTopDp = 12f, borderRightDp = 22f, borderBottomDp = 14f, borderLeftDp = 22f, inkHex = "#29466c",
        ),
        "nightdevil" to RaffleBubbleSliceSpec(
            "/assets/raffle/collection/nightdevil-frame.webp", 150, 210, 85, 210, 0.58f, 0.50f,
            isCollectionStyle = true,
            borderTopDp = 15f, borderRightDp = 26f, borderBottomDp = 13f, borderLeftDp = 26f, inkHex = "#513d66",
        ),
        "whitedragon" to RaffleBubbleSliceSpec(
            "/assets/raffle/collection/whitedragon-frame.webp", 45, 160, 85, 160, 0.38f, 0.69f,
            isCollectionStyle = true,
            borderTopDp = 10f, borderRightDp = 26f, borderBottomDp = 18f, borderLeftDp = 26f, inkHex = "#705443",
        ),
        "newcomer" to RaffleBubbleSliceSpec("/assets/raffle/bubble-growth-v8/newcomer.webp", 64, 128, 61, 128, 0.5000f, 0.4766f),
        "mona" to RaffleBubbleSliceSpec("/assets/raffle/bubble-growth-v8/mona.webp", 80, 128, 46, 128, 0.6250f, 0.3594f),
        "lace" to RaffleBubbleSliceSpec("/assets/raffle/bubble-growth-v8/lace.webp", 69, 128, 56, 128, 0.5391f, 0.4375f),
        "giant" to RaffleBubbleSliceSpec("/assets/raffle/bubble-growth-v8/giant.webp", 55, 128, 70, 128, 0.4297f, 0.5469f),
        "worship" to RaffleBubbleSliceSpec("/assets/raffle/bubble-growth-v8/worship.webp", 57, 128, 69, 128, 0.4453f, 0.5391f),
        "muscle" to RaffleBubbleSliceSpec("/assets/raffle/bubble-growth-v8/muscle.webp", 53, 128, 72, 128, 0.4141f, 0.5625f),
        "whalepunch" to RaffleBubbleSliceSpec(
            "/assets/raffle/collection/whalepunch-frame.webp", 88, 180, 100, 180, 0.54f, 0.54f,
            isCollectionStyle = true,
            borderTopDp = 14f, borderRightDp = 26f, borderBottomDp = 14f, borderLeftDp = 26f, inkHex = "#203d72",
        ),
        "railcannon" to RaffleBubbleSliceSpec("/assets/raffle/bubble-anime-v9/railcannon.webp", 62, 100, 62, 100, 0.6200f, 0.6200f),
        "rascal" to RaffleBubbleSliceSpec("/assets/raffle/bubble-growth-v8/rascal.webp", 65, 128, 61, 128, 0.5078f, 0.4766f),
        "weirdo" to RaffleBubbleSliceSpec("/assets/raffle/bubble-growth-v8/weirdo.webp", 64, 128, 62, 128, 0.5000f, 0.4844f),
        "blacksilk" to RaffleBubbleSliceSpec("/assets/raffle/bubble-growth-v8/blacksilk.webp", 63, 128, 62, 128, 0.4922f, 0.4844f),
        "loli" to RaffleBubbleSliceSpec("/assets/raffle/bubble-growth-v8/loli.webp", 75, 128, 50, 128, 0.5859f, 0.3906f),
        "electric" to RaffleBubbleSliceSpec("/assets/raffle/bubble-growth-v8/electric.webp", 58, 128, 68, 128, 0.4531f, 0.5313f),
        "snow" to RaffleBubbleSliceSpec("/assets/raffle/bubble-growth-v8/snow.webp", 66, 128, 60, 128, 0.5156f, 0.4688f),
        "moon" to RaffleBubbleSliceSpec("/assets/raffle/bubble-growth-v8/moon.webp", 46, 128, 80, 128, 0.3594f, 0.6250f),
        "cloud" to RaffleBubbleSliceSpec("/assets/raffle/bubble-growth-v8/cloud.webp", 72, 128, 53, 128, 0.5625f, 0.4141f),
        "blade" to RaffleBubbleSliceSpec("/assets/raffle/bubble-growth-v8/blade.webp", 74, 128, 51, 128, 0.5781f, 0.3984f),
        "spear" to RaffleBubbleSliceSpec("/assets/raffle/bubble-growth-v8/spear.webp", 71, 128, 54, 128, 0.5547f, 0.4219f),
        "sword" to RaffleBubbleSliceSpec("/assets/raffle/bubble-growth-v8/sword.webp", 65, 128, 61, 128, 0.5078f, 0.4766f),
        "halberd" to RaffleBubbleSliceSpec("/assets/raffle/bubble-growth-v8/halberd.webp", 68, 128, 57, 128, 0.5313f, 0.4453f),
        "wind" to RaffleBubbleSliceSpec("/assets/raffle/bubble-growth-v8/wind.webp", 68, 128, 58, 128, 0.5313f, 0.4531f),
        "flower" to RaffleBubbleSliceSpec("/assets/raffle/bubble-growth-v8/flower.webp", 50, 128, 75, 128, 0.3906f, 0.5859f),
        "sun" to RaffleBubbleSliceSpec("/assets/raffle/bubble-growth-v8/sun.webp", 71, 128, 54, 128, 0.5547f, 0.4219f),
        "star" to RaffleBubbleSliceSpec("/assets/raffle/bubble-growth-v8/star.webp", 58, 128, 67, 128, 0.4531f, 0.5234f),
        "whalebuddy" to RaffleBubbleSliceSpec("/assets/raffle/whale-motion/body-still.webp", 66, 156, 16, 32, 0.72f, 0.35f, isCollectionStyle = true),
    )

    val RAFFLE_TITLE_BANNERS: Map<String, RaffleTitleBannerSpec> = mapOf(
        "maid" to RaffleTitleBannerSpec("maid", "/assets/raffle/collection/maid-title.webp", 0.50f, 0.49f, "#29466c", 14f, 14f, 136f, 30f),
        "whalepunch" to RaffleTitleBannerSpec("whalepunch", "/assets/raffle/title-layout-v2/whalepunch-title.webp", 0.50f, 0.55f, "#203d72", 14f, 14f, 136f, 30f),
        "nightdevil" to RaffleTitleBannerSpec("nightdevil", "/assets/raffle/collection/nightdevil-title.webp", 0.50f, 0.47f, "#513d66", 14f, 14f, 136f, 30f),
        "whalebuddy" to RaffleTitleBannerSpec("whalebuddy", "/assets/raffle/collection/whalebuddy-title.webp", 0.43f, 0.58f, "#305a89", 14f, 14f, 136f, 30f),
        "gpt" to RaffleTitleBannerSpec("gpt", "/assets/raffle/collection/gpt-title.webp", 0.50f, 0.50f, "#67528a", 14f, 14f, 136f, 30f),
        "gemini" to RaffleTitleBannerSpec("gemini", "/assets/raffle/collection/gemini-title.webp", 0.48f, 0.58f, "#624785", 14f, 14f, 148f, 30f),
        "whitedragon" to RaffleTitleBannerSpec("whitedragon", "/assets/raffle/collection/whitedragon-title.webp", 0.50f, 0.46f, "#705443", 14f, 14f, 136f, 30f),
        "deepseek" to RaffleTitleBannerSpec("deepseek", "/assets/raffle/v2/deepseek-title.webp", 0.49f, 0.58f, "#254f86", 14f, 14f, 0f, 26f),
        "deepseekupgrade" to RaffleTitleBannerSpec("deepseekupgrade", "/assets/raffle/collection/deepseekupgrade-title.webp", 0.50f, 0.61f, "#203e70", 14f, 14f, 0f, 26f),
        "mona" to RaffleTitleBannerSpec("mona", "/assets/raffle/mona-title.webp", 0.50f, 0.55f, "#4c4384", 14f, 14f, 0f, 26f),
        "worship" to RaffleTitleBannerSpec("worship", "/assets/raffle/worship-title.webp", 0.59f, 0.53f, "#463778", 26f, 12f, 0f, 26f),
        "muscle" to RaffleTitleBannerSpec("muscle", "/assets/raffle/muscle-frame.webp", 0.50f, 0.57f, "#674326", 14f, 14f, 0f, 26f),
    )

    fun ensureReadableNickColors(colors: List<Color>, isDarkTheme: Boolean): List<Color> {
        if (!isDarkTheme || colors.isEmpty()) return colors
        return colors.map { c ->
            if (c != Color.Transparent && c.alpha > 0.1f && c.luminance() < 0.08f) {
                val lift = 0.76f
                val ratio = 0.60f
                Color(
                    red = c.red * (1f - ratio) + lift * ratio,
                    green = c.green * (1f - ratio) + lift * ratio,
                    blue = c.blue * (1f - ratio) + lift * ratio,
                    alpha = c.alpha,
                )
            } else {
                c
            }
        }
    }

    private fun mapSingleParsedPseudoLayer(
        layer: ParsedPseudoLayer,
        cssPrefix: String?,
        parentFallbackUrl: String?,
        includeTimeline: Boolean,
    ): BubblePseudoLayer? {
        val hasShape = layer.shapeFillColor != null || layer.shapeBorderColor != null || layer.shadowDot != null
        val mappedBgs = if (cssPrefix != null) {
            layer.bgImagesBottomToTop.map { bg ->
                BubbleBgImageSpec(
                    sourceUrl = "$cssPrefix:${bg.urlIdx}",
                    widthDp = bg.widthDp,
                    widthFraction = bg.widthFraction,
                    widthSubFromFullDp = bg.widthSubFromFullDp,
                    heightDp = bg.heightDp,
                    heightFraction = bg.heightFraction,
                    alignCenterX = bg.alignCenterX,
                    offsetXDp = bg.offsetXDp,
                    offsetRightDp = bg.offsetRightDp,
                    alignCenterY = bg.alignCenterY,
                    offsetYDp = bg.offsetYDp,
                    offsetBottomDp = bg.offsetBottomDp,
                    scaleMode = bg.scaleMode,
                )
            }
        } else {
            emptyList()
        }
        val mappedNs = layer.nineSlice?.let { ns ->
            val nsSrc = when {
                ns.urlIdx == -1 -> parentFallbackUrl
                cssPrefix != null -> "$cssPrefix:${ns.urlIdx}"
                else -> null
            } ?: return@let null
            NineSliceSpec(
                sourceUrl = nsSrc,
                sliceTop = ns.sliceTop,
                sliceRight = ns.sliceRight,
                sliceBottom = ns.sliceBottom,
                sliceLeft = ns.sliceLeft,
                isPercentSlice = ns.isPercentSlice,
                borderTopDp = ns.borderTopDp,
                borderRightDp = ns.borderRightDp,
                borderBottomDp = ns.borderBottomDp,
                borderLeftDp = ns.borderLeftDp,
                outsetTopDp = ns.outsetTopDp,
                outsetRightDp = ns.outsetRightDp,
                outsetBottomDp = ns.outsetBottomDp,
                outsetLeftDp = ns.outsetLeftDp,
            )
        }
        val mappedTimeline = if (includeTimeline && layer.timelineFrames.isNotEmpty() && layer.timelinePeriodMs > 0L) {
            val frames = layer.timelineFrames.mapNotNull { f ->
                mapSingleParsedPseudoLayer(f, cssPrefix, parentFallbackUrl, includeTimeline = false)
            }
            if (frames.isNotEmpty()) PseudoLayerTimeline(layer.timelinePeriodMs, frames) else null
        } else {
            null
        }
        if (!hasShape && mappedBgs.isEmpty() && mappedNs == null && mappedTimeline == null) return null
        return BubblePseudoLayer(
            topDp = layer.topDp,
            rightDp = layer.rightDp,
            bottomDp = layer.bottomDp,
            leftDp = layer.leftDp,
            widthDp = layer.widthDp,
            heightDp = layer.heightDp,
            heightCapToParent = layer.heightCapToParent,
            aspectRatio = layer.aspectRatio,
            translateSelfX = layer.translateSelfX,
            translateSelfY = layer.translateSelfY,
            centerX = layer.centerX,
            centerY = layer.centerY,
            offsetXDp = layer.offsetXDp,
            offsetYDp = layer.offsetYDp,
            shapeFillColor = layer.shapeFillColor,
            shapeBorderColor = layer.shapeBorderColor,
            shapeBorderWidthDp = layer.shapeBorderWidthDp,
            isCircle = layer.isCircle,
            shadowDot = layer.shadowDot,
            bgImagesBottomToTop = mappedBgs,
            nineSliceSpec = mappedNs,
            opacity = layer.opacity,
            scaleX = layer.scaleX,
            scaleY = layer.scaleY,
            rotateDeg = layer.rotateDeg,
            visualAnim = layer.visualAnim,
            pseudoTimeline = mappedTimeline,
        )
    }

    private fun mapParsedPseudoLayers(
        layers: List<ParsedPseudoLayer>,
        cssPrefix: String?,
        parentFallbackUrl: String? = null,
    ): List<BubblePseudoLayer> = layers.mapNotNull { layer ->
        mapSingleParsedPseudoLayer(layer, cssPrefix, parentFallbackUrl, includeTimeline = true)
    }

    private fun buildTitleFrameParts(
        id: String,
        titleImg: String?,
        titleCssSha: String?,
        dynTitle: ParsedCommunityTitleCss,
        isDarkTheme: Boolean,
        legacyDarkFallback: Boolean,
        includeTimeline: Boolean,
    ): TitleTimelineFrame {
        val effBgHex = if (isDarkTheme) (dynTitle.darkBgHex ?: dynTitle.bgHex) else dynTitle.bgHex
        val effSecHex = if (isDarkTheme) (dynTitle.darkSecondaryBgHex ?: dynTitle.secondaryBgHex ?: effBgHex) else (dynTitle.secondaryBgHex ?: effBgHex)
        val effTextHex = if (isDarkTheme) (dynTitle.darkTextHex ?: dynTitle.textHex) else dynTitle.textHex
        val effBorderHex = if (isDarkTheme) (dynTitle.darkBorderHex ?: dynTitle.borderHex) else dynTitle.borderHex
        val baseSrc = when {
            !dynTitle.hasNineSlice -> null
            legacyDarkFallback -> "community-css-dark:$id:title:$titleCssSha:${dynTitle.baseUrlIdx}"
            isDarkTheme && dynTitle.darkHasImageVariant -> "community-css:$id:title:$titleCssSha:${dynTitle.baseUrlIdx}"
            titleImg != null && dynTitle.baseUrlIdx == 0 -> titleImg
            else -> "community-css:$id:title:$titleCssSha:${dynTitle.baseUrlIdx}"
        }
        val nineSlice = if (dynTitle.hasNineSlice && baseSrc != null) {
            NineSliceSpec(
                sourceUrl = baseSrc,
                sliceTop = dynTitle.sliceTop,
                sliceRight = dynTitle.sliceRight,
                sliceBottom = dynTitle.sliceBottom,
                sliceLeft = dynTitle.sliceLeft,
                isPercentSlice = dynTitle.isPercentSlice,
                borderTopDp = dynTitle.borderImgTopDp,
                borderRightDp = dynTitle.borderImgRightDp,
                borderBottomDp = dynTitle.borderImgBottomDp,
                borderLeftDp = dynTitle.borderImgLeftDp,
                outsetTopDp = dynTitle.outsetTopDp,
                outsetRightDp = dynTitle.outsetRightDp,
                outsetBottomDp = dynTitle.outsetBottomDp,
                outsetLeftDp = dynTitle.outsetLeftDp,
            )
        } else {
            null
        }
        val lightCssPrefix = titleCssSha?.let { "community-css:$id:title:$it" }
        val parentFallbackUrl = titleImg ?: if (dynTitle.hasBaseImageUrl && lightCssPrefix != null) {
            "$lightCssPrefix:${dynTitle.baseUrlIdx}"
        } else {
            null
        }
        val mappedPseudo = dynTitle.pseudoLayers.mapNotNull { layer ->
            mapSingleParsedPseudoLayer(layer, lightCssPrefix, parentFallbackUrl, includeTimeline = includeTimeline)
        }
        val bgColorsList = if (dynTitle.hasNineSlice) {
            emptyList()
        } else if (dynTitle.fillMode == "grad") {
            listOf(parseHexColorPure(effBgHex), parseHexColorPure(effSecHex))
        } else {
            val c = parseHexColorPure(effBgHex)
            if (c != Color.Transparent) listOf(c) else emptyList()
        }
        val borderGrad = if (dynTitle.hasNineSlice) null else dynTitle.borderGradientColors
        val effBorderCol = when {
            dynTitle.hasNineSlice || borderGrad != null -> Color.Transparent
            else -> parseHexColorPure(effBorderHex)
        }
        return TitleTimelineFrame(
            bgColors = bgColorsList,
            bgAngleDeg = dynTitle.bgAngleDeg,
            borderColor = effBorderCol,
            borderGradientColors = borderGrad,
            textColor = parseHexColorPure(effTextHex, Color(0xFF1E293B)),
            nineSliceSpec = nineSlice,
            pseudoLayers = mappedPseudo,
            leadingIconUrl = dynTitle.leadingIconIdx?.let { "community-css:$id:title:$titleCssSha:$it" },
            trailingIconUrl = dynTitle.trailingIconIdx?.let { "community-css:$id:title:$titleCssSha:$it" },
        )
    }

    internal fun resolveCommunityTitleStyle(
        id: String,
        skin: CommunitySkinCatalogItem?,
        isDarkTheme: Boolean = false,
    ): ResolvedTitleChip {
        val label = skin?.name ?: id
        val titleImg = skin?.titleImgSha?.let { "$ORIGIN/api/community-skins/image/$it" }
        val titleCssSha = skin?.titleCssSha
        val themeKey = if (isDarkTheme) "dark" else "light"
        val dynTitle = titleCssSha?.takeIf { it.isNotBlank() }?.let { sha ->
            dynamicParsedTitles["$sha:$themeKey"] ?: dynamicParsedTitles[sha]
        }
        if (dynTitle != null) {
            val legacyDarkFallback = isDarkTheme &&
                dynTitle.darkHasImageVariant &&
                !dynamicParsedTitles.containsKey("$titleCssSha:dark")
            val effBgHex = if (isDarkTheme) (dynTitle.darkBgHex ?: dynTitle.bgHex) else dynTitle.bgHex
            val effSecHex = if (isDarkTheme) (dynTitle.darkSecondaryBgHex ?: dynTitle.secondaryBgHex ?: effBgHex) else (dynTitle.secondaryBgHex ?: effBgHex)
            val effTextHex = if (isDarkTheme) (dynTitle.darkTextHex ?: dynTitle.textHex) else dynTitle.textHex
            val effBorderHex = if (isDarkTheme) (dynTitle.darkBorderHex ?: dynTitle.borderHex) else dynTitle.borderHex
            val baseFrame = buildTitleFrameParts(
                id = id,
                titleImg = titleImg,
                titleCssSha = titleCssSha,
                dynTitle = dynTitle,
                isDarkTheme = isDarkTheme,
                legacyDarkFallback = legacyDarkFallback,
                includeTimeline = true,
            )
            var topRes = 0f
            var endRes = 0f
            var botRes = 0f
            var startRes = 0f
            for (layer in baseFrame.pseudoLayers) {
                layer.leftDp?.takeIf { it < 0f }?.let { startRes = max(startRes, -it) }
                layer.rightDp?.takeIf { it < 0f }?.let { endRes = max(endRes, -it) }
                layer.topDp?.takeIf { it < 0f }?.let { topRes = max(topRes, -it) }
                layer.bottomDp?.takeIf { it < 0f }?.let { botRes = max(botRes, -it) }
                layer.nineSliceSpec?.let { ns ->
                    startRes = max(startRes, ns.outsetLeftDp)
                    endRes = max(endRes, ns.outsetRightDp)
                    topRes = max(topRes, ns.outsetTopDp)
                    botRes = max(botRes, ns.outsetBottomDp)
                }
            }
            val mappedTimeline = if (dynTitle.timelineFrames.isNotEmpty() && dynTitle.timelinePeriodMs > 0L) {
                CommunityTitleTimeline(
                    periodMs = dynTitle.timelinePeriodMs,
                    frames = dynTitle.timelineFrames.map { f ->
                        buildTitleFrameParts(
                            id = id,
                            titleImg = titleImg,
                            titleCssSha = titleCssSha,
                            dynTitle = f,
                            isDarkTheme = isDarkTheme,
                            legacyDarkFallback = legacyDarkFallback,
                            includeTimeline = false,
                        )
                    },
                )
            } else {
                null
            }
            val renderSpec = CommunityTitleRenderSpec(
                fixedHeightDp = dynTitle.fixedHeightDp,
                minHeightDp = dynTitle.minHeightDp,
                padTopDp = dynTitle.padTopDp,
                padRightDp = dynTitle.padRightDp,
                padBottomDp = dynTitle.padBottomDp,
                padLeftDp = dynTitle.padLeftDp,
                startOverflowReserveDp = startRes,
                endOverflowReserveDp = endRes,
                topOverflowReserveDp = topRes,
                bottomOverflowReserveDp = botRes,
                bgColors = baseFrame.bgColors,
                bgAngleDeg = baseFrame.bgAngleDeg,
                borderColor = baseFrame.borderColor,
                borderWidthDp = when {
                    dynTitle.hasNineSlice -> 0f
                    baseFrame.borderGradientColors != null -> dynTitle.borderWidthDp.coerceAtLeast(1.2f)
                    else -> dynTitle.borderWidthDp
                },
                cornerRadiiDp = dynTitle.cornerRadiiDp ?: listOf(999f, 999f, 999f, 999f),
                textColor = baseFrame.textColor,
                fontWeight = dynTitle.fontWeight,
                letterSpacingSp = dynTitle.letterSpacingSp,
                textShadowColor = if (isDarkTheme && dynTitle.hasNineSlice) Color(0xC818081E) else null,
                nineSliceSpec = baseFrame.nineSliceSpec,
                pseudoLayers = baseFrame.pseudoLayers,
                borderGradientColors = baseFrame.borderGradientColors,
                visualAnim = dynTitle.visualAnim,
                animationTimeline = mappedTimeline,
            )
            return ResolvedTitleChip(
                key = "community:$id",
                label = dynTitle.overrideLabel?.takeIf { it.isNotBlank() } ?: label,
                rarity = "normal",
                bgHex = effBgHex,
                textHex = effTextHex,
                borderHex = effBorderHex,
                fillMode = dynTitle.fillMode,
                shape = "pill",
                secondaryBgHex = effSecHex,
                effect = "none",
                fontKey = "",
                bgImageUrl = titleImg,
                specialKind = "community",
                customCss = dynTitle.cornerRadiiDp?.takeIf { it.size == 4 }?.let { ParsedSafeCss(cornerRadiiDp = it) },
                leadingIconUrl = baseFrame.leadingIconUrl,
                trailingIconUrl = baseFrame.trailingIconUrl,
                dynamicCssSpec = null,
                communitySpec = renderSpec,
            )
        }

        val missingDynamicCss = when {
            skin == null -> if (id != "aurora-preset" && BadgeAndCosmeticRegistry.isValidCommunitySlug(id)) "catalog:$id" else null
            !titleCssSha.isNullOrBlank() -> "$id:title:$titleCssSha"
            else -> null
        }

        val titleSlice = skin?.titleSlice
        if (titleImg != null && titleSlice != null) {
            val sTop = titleSlice.getOrNull(0)?.toFloat() ?: 18f
            val sRight = titleSlice.getOrNull(1)?.toFloat() ?: 24f
            val sBottom = titleSlice.getOrNull(2)?.toFloat() ?: 18f
            val sLeft = titleSlice.getOrNull(3)?.toFloat() ?: 24f
            val bTop = (sTop * 0.5f).coerceIn(4f, 12f)
            val bRight = (sRight * 0.5f).coerceIn(8f, 20f)
            val bBottom = (sBottom * 0.5f).coerceIn(4f, 12f)
            val bLeft = (sLeft * 0.5f).coerceIn(8f, 20f)
            val bgHex = if (isDarkTheme) "#273244" else "#EEF2F6"
            val fgHex = if (isDarkTheme) "#F1F5F9" else "#334155"
            val renderSpec = CommunityTitleRenderSpec(
                minHeightDp = 22f,
                padTopDp = bTop.coerceIn(2f, 6f),
                padRightDp = bRight.coerceIn(8f, 16f),
                padBottomDp = bBottom.coerceIn(2f, 6f),
                padLeftDp = bLeft.coerceIn(8f, 16f),
                textColor = parseHexColorPure(fgHex),
                nineSliceSpec = NineSliceSpec(
                    sourceUrl = titleImg,
                    sliceTop = sTop,
                    sliceRight = sRight,
                    sliceBottom = sBottom,
                    sliceLeft = sLeft,
                    borderTopDp = bTop,
                    borderRightDp = bRight,
                    borderBottomDp = bBottom,
                    borderLeftDp = bLeft,
                ),
            )
            return ResolvedTitleChip(
                key = "community:$id",
                label = label,
                rarity = "normal",
                bgHex = bgHex,
                textHex = fgHex,
                borderHex = if (isDarkTheme) "#475569" else "#CBD5E1",
                fillMode = "solid",
                shape = "pill",
                bgImageUrl = titleImg,
                specialKind = "community",
                dynamicCssSpec = missingDynamicCss,
                communitySpec = renderSpec,
            )
        }

        return ResolvedTitleChip(
            key = "community:$id",
            label = label,
            rarity = "normal",
            bgHex = if (isDarkTheme) "#273244" else "#EEF2F6",
            textHex = if (isDarkTheme) "#F1F5F9" else "#334155",
            borderHex = if (isDarkTheme) "#475569" else "#CBD5E1",
            fillMode = "solid",
            shape = "pill",
            bgImageUrl = titleImg,
            specialKind = "community",
            dynamicCssSpec = missingDynamicCss,
        )
    }

    fun resolveCommunityBadgeSpec(
        id: String,
        imgSha: String?,
        cssSha: String?,
        isDarkTheme: Boolean = false,
    ): CommunityBadgeRenderSpec? {
        if (cssSha.isNullOrBlank()) return null
        val themeKey = if (isDarkTheme) "dark" else "light"
        val parsed = dynamicParsedBadges["$cssSha:$themeKey"] ?: dynamicParsedBadges[cssSha] ?: return null
        val lightPrefix = "community-css:$id:badge:$cssSha"
        val parentFallback = imgSha?.let { "$ORIGIN/api/community-skins/image/$it" }
            ?: if (parsed.hasBaseImage) "$lightPrefix:${parsed.baseUrlIdx}" else null
        val mappedPseudo = mapParsedPseudoLayers(parsed.pseudoLayers, lightPrefix, parentFallbackUrl = parentFallback)
        var topRes = 0f
        var rightRes = 0f
        var bottomRes = 0f
        var leftRes = 0f
        for (layer in mappedPseudo) {
            layer.topDp?.takeIf { it < 0f }?.let { topRes = max(topRes, -it) }
            layer.rightDp?.takeIf { it < 0f }?.let { rightRes = max(rightRes, -it) }
            layer.bottomDp?.takeIf { it < 0f }?.let { bottomRes = max(bottomRes, -it) }
            layer.leftDp?.takeIf { it < 0f }?.let { leftRes = max(leftRes, -it) }
        }
        val mappedTimeline = if (parsed.timelineFrames.isNotEmpty() && parsed.timelinePeriodMs > 0L) {
            CommunityBadgeTimeline(
                periodMs = parsed.timelinePeriodMs,
                frames = parsed.timelineFrames.map { f ->
                    val fFallback = imgSha?.let { "$ORIGIN/api/community-skins/image/$it" }
                        ?: if (f.hasBaseImage) "$lightPrefix:${f.baseUrlIdx}" else null
                    BadgeTimelineFrame(
                        baseUrlIdx = f.baseUrlIdx,
                        spriteCol = f.spriteCol,
                        spriteRow = f.spriteRow,
                        pseudoLayers = f.pseudoLayers.mapNotNull { l ->
                            mapSingleParsedPseudoLayer(l, lightPrefix, fFallback, includeTimeline = false)
                        },
                        opacity = f.opacity,
                    )
                },
            )
        } else {
            null
        }
        return CommunityBadgeRenderSpec(
            widthDp = parsed.widthDp,
            heightDp = parsed.heightDp,
            hasImage = parsed.hasImage,
            hasBaseImage = parsed.hasBaseImage,
            hasDarkImageVariant = parsed.hasDarkImageVariant,
            baseUrlIdx = parsed.baseUrlIdx,
            spriteCols = parsed.spriteCols,
            spriteRows = parsed.spriteRows,
            spriteCol = parsed.spriteCol,
            spriteRow = parsed.spriteRow,
            topReserveDp = topRes,
            rightReserveDp = rightRes,
            bottomReserveDp = bottomRes,
            leftReserveDp = leftRes,
            pseudoLayers = mappedPseudo,
            visualAnim = parsed.visualAnim,
            animationTimeline = mappedTimeline,
        )
    }

    private val CSS_URL_TOKEN_RE = Regex(
        """url\(\s*(?:"([^"]+)"|'([^']+)'|([^'")\s]+))\s*\)""",
    )

    data class ParsedPseudoBgImage(
        val urlIdx: Int,
        val widthDp: Float? = null,
        val widthFraction: Float? = null,
        val widthSubFromFullDp: Float? = null,
        val heightDp: Float? = null,
        val heightFraction: Float? = null,
        val alignCenterX: Boolean = false,
        val offsetXDp: Float = 0f,
        val offsetRightDp: Float? = null,
        val alignCenterY: Boolean = false,
        val offsetYDp: Float = 0f,
        val offsetBottomDp: Float? = null,
        val scaleMode: BgScaleMode = BgScaleMode.STRETCH,
    )

    data class ParsedPseudoNineSlice(
        val urlIdx: Int,
        val sliceTop: Float,
        val sliceRight: Float,
        val sliceBottom: Float,
        val sliceLeft: Float,
        val isPercentSlice: Boolean = false,
        val borderTopDp: Float = 0f,
        val borderRightDp: Float = 0f,
        val borderBottomDp: Float = 0f,
        val borderLeftDp: Float = 0f,
        val outsetTopDp: Float = 0f,
        val outsetRightDp: Float = 0f,
        val outsetBottomDp: Float = 0f,
        val outsetLeftDp: Float = 0f,
    )

    data class ParsedPseudoLayer(
        val topDp: Float? = null,
        val rightDp: Float? = null,
        val bottomDp: Float? = null,
        val leftDp: Float? = null,
        val widthDp: Float? = null,
        val heightDp: Float? = null,
        val heightCapToParent: Boolean = false,
        val aspectRatio: Float? = null,
        val translateSelfX: Float = 0f,
        val translateSelfY: Float = 0f,
        val centerX: Boolean = false,
        val centerY: Boolean = false,
        val offsetXDp: Float = 0f,
        val offsetYDp: Float = 0f,
        val shapeFillColor: Color? = null,
        val shapeBorderColor: Color? = null,
        val shapeBorderWidthDp: Float = 0f,
        val isCircle: Boolean = false,
        val shadowDot: PseudoBoxShadowDot? = null,
        val bgImagesBottomToTop: List<ParsedPseudoBgImage> = emptyList(),
        val nineSlice: ParsedPseudoNineSlice? = null,
        val opacity: Float = 1f,
        val scaleX: Float = 1f,
        val scaleY: Float = 1f,
        val rotateDeg: Float = 0f,
        val visualAnim: List<CssVisualAnimTrack> = emptyList(),
        val timelineFrames: List<ParsedPseudoLayer> = emptyList(),
        val timelinePeriodMs: Long = 0L,
    )

    data class ParsedCommunityBubbleCss(
        val hasImage: Boolean,
        val hasDarkImageVariant: Boolean,
        val baseUrlIdx: Int = 0,
        val hasBaseImageUrl: Boolean = false,
        val sliceTop: Float,
        val sliceRight: Float,
        val sliceBottom: Float,
        val sliceLeft: Float,
        val isPercentSlice: Boolean,
        val borderTopDp: Float,
        val borderRightDp: Float,
        val borderBottomDp: Float,
        val borderLeftDp: Float,
        val outsetTopDp: Float = 0f,
        val outsetRightDp: Float = 0f,
        val outsetBottomDp: Float = 0f,
        val outsetLeftDp: Float = 0f,
        val startOverflowReserveDp: Float = 0f,
        val endOverflowReserveDp: Float = 0f,
        val topOverflowReserveDp: Float = 0f,
        val bottomOverflowReserveDp: Float = 0f,
        val padTopDp: Float,
        val padRightDp: Float,
        val padBottomDp: Float,
        val padLeftDp: Float,
        val minWidthDp: Float,
        val textColorLight: Color?,
        val textColorDark: Color?,
        val safeCssLight: ParsedSafeCss?,
        val pseudoLayers: List<ParsedPseudoLayer> = emptyList(),
        val isNineSlice: Boolean = false,
        val bgColorLight: Color? = null,
        val bgGradientColorsLight: List<Color>? = null,
        val bgGradientAngleDeg: Float? = null,
        val borderColorLight: Color? = null,
        val borderWidthDp: Float? = null,
        val borderGradientColors: List<Color>? = null,
        val nonNineSliceRadiiDp: List<Float>? = null,
        val minHeightDp: Float = 0f,
        val darkBgColor: Color? = null,
        val darkBgGradientColors: List<Color>? = null,
        val darkBorderColor: Color? = null,
        val visualAnim: List<CssVisualAnimTrack> = emptyList(),
        val timelineFrames: List<ParsedCommunityBubbleCss> = emptyList(),
        val timelinePeriodMs: Long = 0L,
    )

    data class ParsedCommunityTitleCss(
        val bgHex: String,
        val secondaryBgHex: String? = null,
        val textHex: String,
        val borderHex: String,
        val darkBorderHex: String? = null,
        val fillMode: String = "grad",
        val cornerRadiiDp: List<Float>? = null,
        val leadingIconIdx: Int? = null,
        val trailingIconIdx: Int? = null,
        val overrideLabel: String? = null,
        val bgAngleDeg: Float? = null,
        val darkBgHex: String? = null,
        val darkSecondaryBgHex: String? = null,
        val darkTextHex: String? = null,
        val darkHasImageVariant: Boolean = false,
        val hasNineSlice: Boolean = false,
        val baseUrlIdx: Int = 0,
        val hasBaseImageUrl: Boolean = false,
        val borderGradientColors: List<Color>? = null,
        val sliceTop: Float = 0f,
        val sliceRight: Float = 0f,
        val sliceBottom: Float = 0f,
        val sliceLeft: Float = 0f,
        val isPercentSlice: Boolean = false,
        val borderImgTopDp: Float = 0f,
        val borderImgRightDp: Float = 0f,
        val borderImgBottomDp: Float = 0f,
        val borderImgLeftDp: Float = 0f,
        val outsetTopDp: Float = 0f,
        val outsetRightDp: Float = 0f,
        val outsetBottomDp: Float = 0f,
        val outsetLeftDp: Float = 0f,
        val fixedHeightDp: Float? = null,
        val minHeightDp: Float = 22f,
        val padTopDp: Float = 2f,
        val padRightDp: Float = 8f,
        val padBottomDp: Float = 2f,
        val padLeftDp: Float = 8f,
        val borderWidthDp: Float = 1f,
        val letterSpacingSp: Float? = null,
        val fontWeight: FontWeight = FontWeight.Bold,
        val hasImage: Boolean = false,
        val pseudoLayers: List<ParsedPseudoLayer> = emptyList(),
        val visualAnim: List<CssVisualAnimTrack> = emptyList(),
        val timelineFrames: List<ParsedCommunityTitleCss> = emptyList(),
        val timelinePeriodMs: Long = 0L,
    )

    data class ParsedCommunityBadgeCss(
        val widthDp: Float = 24f,
        val heightDp: Float = 24f,
        val hasImage: Boolean = false,
        val hasBaseImage: Boolean = false,
        val hasDarkImageVariant: Boolean = false,
        val baseUrlIdx: Int = 0,
        val spriteCols: Int = 1,
        val spriteRows: Int = 1,
        val spriteCol: Int = 0,
        val spriteRow: Int = 0,
        val pseudoLayers: List<ParsedPseudoLayer> = emptyList(),
        val opacity: Float = 1f,
        val visualAnim: List<CssVisualAnimTrack> = emptyList(),
        val timelineFrames: List<ParsedCommunityBadgeCss> = emptyList(),
        val timelinePeriodMs: Long = 0L,
    )

    private val dynamicParsedBubbles = ConcurrentHashMap<String, ParsedCommunityBubbleCss>()
    private val dynamicParsedTitles = ConcurrentHashMap<String, ParsedCommunityTitleCss>()
    private val dynamicParsedBadges = ConcurrentHashMap<String, ParsedCommunityBadgeCss>()

    private fun isValidCssImageUrl(raw: String): Boolean =
        raw.startsWith("data:image/", ignoreCase = true) ||
            raw.startsWith("https://", ignoreCase = true) ||
            raw.startsWith("http://", ignoreCase = true) ||
            raw.startsWith("/api/community-skins/", ignoreCase = true)

    /**
     * Strips `@... { ... }` at-blocks (`@keyframes`, `@property`, `@media`) while extracting `@property`
     * `initial-value` custom properties and partitioning top-level rules into `(selector, body)` pairs.
     */
    private data class CssTopLevelAst(
        val rules: List<Pair<String, String>>,
        val initialVars: Map<String, String>,
        val keyframeMidVars: Map<String, Map<String, String>>,
        val keyframeStops: Map<String, List<Pair<Float, String>>> = emptyMap(),
    )

    private fun splitCssDeclarations(body: String): List<String> {
        val out = ArrayList<String>()
        val cur = StringBuilder()
        var quote: Char? = null
        var depth = 0
        var i = 0
        while (i < body.length) {
            val ch = body[i]
            if (quote != null) {
                cur.append(ch)
                if (ch == '\\' && i + 1 < body.length) {
                    cur.append(body[i + 1])
                    i += 2
                    continue
                }
                if (ch == quote) quote = null
            } else {
                when (ch) {
                    '"', '\'' -> {
                        quote = ch
                        cur.append(ch)
                    }
                    '(' -> {
                        depth++
                        cur.append(ch)
                    }
                    ')' -> {
                        if (depth > 0) depth--
                        cur.append(ch)
                    }
                    ';' -> {
                        if (depth == 0) {
                            val d = cur.toString().trim()
                            if (d.isNotEmpty()) out.add(d)
                            cur.clear()
                        } else {
                            cur.append(ch)
                        }
                    }
                    else -> cur.append(ch)
                }
            }
            i++
        }
        val tail = cur.toString().trim()
        if (tail.isNotEmpty()) out.add(tail)
        return out
    }

    private fun extractCssCustomProps(body: String): Map<String, String> {
        val vars = LinkedHashMap<String, String>()
        for (decl in splitCssDeclarations(body)) {
            val colon = decl.indexOf(':')
            if (colon <= 0) continue
            val k = decl.substring(0, colon).trim()
            val v = decl.substring(colon + 1).trim()
            if (k.startsWith("--") && v.isNotEmpty()) {
                vars[k] = v
            }
        }
        return vars
    }

    /**
     * Parses CSS declarations/blocks into top-level style rules, `@property` initial values,
     * and `@keyframes` midpoint custom-property samples (prioritizing stops in 49%..51%, then 44%..56%).
     * Unwraps `@media` unless `prefers-reduced-motion: reduce` or desktop-only `min-width > 600px`.
     */
    private fun stripCssAtBlocks(noComments: String): CssTopLevelAst {
        val rules = ArrayList<Pair<String, String>>()
        val initialVars = LinkedHashMap<String, String>()
        val keyframeMidVars = LinkedHashMap<String, Map<String, String>>()
        val keyframeStops = LinkedHashMap<String, List<Pair<Float, String>>>()

        fun parseStopPcts(stopSel: String): List<Float> {
            val out = ArrayList<Float>()
            for (part in stopSel.split(',')) {
                val p = part.trim().lowercase()
                when {
                    p == "from" -> out.add(0f)
                    p == "to" -> out.add(100f)
                    p.endsWith("%") -> p.removeSuffix("%").trim().toFloatOrNull()?.let { out.add(it) }
                }
            }
            return out
        }

        fun scanBlocks(src: String, onBlock: (String, String) -> Unit) {
            val clean = src.trimStart('\uFEFF')
            val len = clean.length
            var i = 0
            while (i < len) {
                while (i < len && (clean[i].isWhitespace() || clean[i] == ';')) i++
                if (i >= len) break
                val headerStart = i
                var quote: Char? = null
                var hitBrace = false
                while (i < len) {
                    val ch = clean[i]
                    if (quote != null) {
                        if (ch == '\\' && i + 1 < len) {
                            i += 2
                            continue
                        }
                        if (ch == quote) quote = null
                    } else {
                        when (ch) {
                            '"', '\'' -> quote = ch
                            '{' -> {
                                hitBrace = true
                                break
                            }
                            ';' -> break
                        }
                    }
                    i++
                }
                if (!hitBrace) {
                    i++
                    continue
                }
                val selector = clean.substring(headerStart, i).trim()
                val bodyStart = i + 1
                i = bodyStart
                var depth = 1
                quote = null
                while (i < len && depth > 0) {
                    val ch = clean[i]
                    if (quote != null) {
                        if (ch == '\\' && i + 1 < len) {
                            i += 2
                            continue
                        }
                        if (ch == quote) quote = null
                    } else {
                        when (ch) {
                            '"', '\'' -> quote = ch
                            '{' -> depth++
                            '}' -> depth--
                        }
                    }
                    i++
                }
                val bodyEnd = if (depth == 0) i - 1 else len
                val body = clean.substring(bodyStart, bodyEnd)
                if (selector.isNotEmpty()) {
                    onBlock(selector, body)
                }
            }
        }

        fun shouldIncludeMedia(mediaHeader: String): Boolean {
            val low = mediaHeader.lowercase()
            if (low.contains("prefers-reduced-motion") && low.contains("reduce")) return false
            val minWidthPx = Regex("""min-width\s*:\s*(\d+(?:\.\d+)?)\s*(px|r?em)?""")
                .findAll(low)
                .mapNotNull { m ->
                    val v = m.groupValues[1].toFloatOrNull() ?: return@mapNotNull null
                    when (m.groupValues[2].lowercase()) {
                        "rem" -> v * 16f
                        "em" -> v * 15f
                        else -> v
                    }
                }
                .maxOrNull()
            if (minWidthPx != null && minWidthPx > 600f) return false
            return true
        }

        fun parseKeyframesBody(kfName: String, kfBody: String) {
            val tightMap = LinkedHashMap<String, Pair<Float, String>>()
            val broadMap = LinkedHashMap<String, Pair<Float, String>>()
            val fullStops = ArrayList<Pair<Float, String>>()
            scanBlocks(kfBody) { stopSel, stopBody ->
                val pcts = parseStopPcts(stopSel)
                if (pcts.isEmpty()) return@scanBlocks
                for (pct in pcts) {
                    fullStops.add((pct / 100f).coerceIn(0f, 1f) to stopBody)
                }
                val props = extractCssCustomProps(stopBody)
                if (props.isEmpty()) return@scanBlocks
                val tightDist = pcts.filter { it in 49f..51f }.minOfOrNull { kotlin.math.abs(it - 50f) }
                val broadDist = pcts.filter { it in 44f..56f }.minOfOrNull { kotlin.math.abs(it - 50f) }
                if (tightDist != null) {
                    for ((k, v) in props) {
                        val prev = tightMap[k]
                        if (prev == null || tightDist <= prev.first) {
                            tightMap[k] = tightDist to v
                        }
                    }
                }
                if (broadDist != null) {
                    for ((k, v) in props) {
                        val prev = broadMap[k]
                        if (prev == null || broadDist <= prev.first) {
                            broadMap[k] = broadDist to v
                        }
                    }
                }
            }
            if (fullStops.isNotEmpty()) {
                fullStops.sortBy { it.first }
                keyframeStops[kfName] = fullStops
            }
            val merged = LinkedHashMap<String, String>()
            for ((k, pair) in broadMap) merged[k] = pair.second
            for ((k, pair) in tightMap) merged[k] = pair.second
            if (merged.isNotEmpty()) {
                keyframeMidVars[kfName] = merged
            }
        }

        fun processChunk(src: String) {
            scanBlocks(src) { selector, body ->
                if (selector.startsWith("@")) {
                    val low = selector.lowercase()
                    when {
                        low.startsWith("@property") -> {
                            val propName = Regex("""--[a-zA-Z0-9_-]+""").find(selector)?.value
                            val initVal = Regex("""initial-value\s*:\s*([^;}]+)""", RegexOption.IGNORE_CASE)
                                .find(body)?.groupValues?.get(1)?.trim()
                            if (!propName.isNullOrBlank() && !initVal.isNullOrBlank()) {
                                initialVars[propName] = initVal
                            }
                        }
                        low.startsWith("@media") -> {
                            if (shouldIncludeMedia(selector)) {
                                processChunk(body)
                            }
                        }
                        low.startsWith("@keyframes") || low.startsWith("@-webkit-keyframes") -> {
                            val kfName = selector
                                .replace(Regex("""^@(?:-\w+-)?keyframes\s+""", RegexOption.IGNORE_CASE), "")
                                .trim()
                            if (kfName.isNotEmpty()) {
                                parseKeyframesBody(kfName, body)
                            }
                        }
                    }
                } else {
                    rules.add(selector to body)
                }
            }
        }

        processChunk(noComments)
        return CssTopLevelAst(rules, initialVars, keyframeMidVars, keyframeStops)
    }

    /**
     * Extracts all valid image URLs across active style rules (first) and `@keyframes` blocks (second)
     * in exact CSS document order matching the `__URL_k__` placeholder indices produced by [cascadeCommunityCss].
     */
    fun extractAllCssImageUrls(cssText: String): List<String> {
        val cleaned = cssText.replace(CSS_COMMENT_RE, "")
        val ast = stripCssAtBlocks(cleaned)
        val out = ArrayList<String>()
        for ((_, body) in ast.rules) {
            for (m in CSS_URL_TOKEN_RE.findAll(body)) {
                val raw = (m.groupValues[1].ifEmpty { m.groupValues[2].ifEmpty { m.groupValues[3] } }).trim()
                if (isValidCssImageUrl(raw)) out.add(raw)
            }
        }
        for (stops in ast.keyframeStops.values) {
            for ((_, stopBody) in stops) {
                for (m in CSS_URL_TOKEN_RE.findAll(stopBody)) {
                    val raw = (m.groupValues[1].ifEmpty { m.groupValues[2].ifEmpty { m.groupValues[3] } }).trim()
                    if (isValidCssImageUrl(raw)) out.add(raw)
                }
            }
        }
        return out
    }

    /**
     * Extracts image URLs from community skin CSS (`url("data:image/...")` or `/api/community-skins/image/...`),
     * handling quoted `data:image/svg+xml,...` payloads that contain nested parentheses like `rotate(...)`
     * and `fill="url(%23id)"`, preferring active rule blocks (`::before` / `::after` included) and falling
     * back to `@keyframes` if no rule URLs exist.
     */
    fun extractCssImageUrls(cssText: String, preferDark: Boolean = false): List<String> {
        val cleaned = cssText.replace(CSS_COMMENT_RE, "")
        val ast = stripCssAtBlocks(cleaned)
        fun collectFromRules(target: List<Pair<String, String>>): List<String> {
            val out = ArrayList<String>()
            for ((_, body) in target) {
                for (m in CSS_URL_TOKEN_RE.findAll(body)) {
                    val raw = (m.groupValues[1].ifEmpty { m.groupValues[2].ifEmpty { m.groupValues[3] } }).trim()
                    if (isValidCssImageUrl(raw)) out.add(raw)
                }
            }
            return out
        }
        val darkSelRe = Regex("""data-theme\s*=\s*["']?dark["']?""", RegexOption.IGNORE_CASE)
        val darkRules = ast.rules.filter { (sel, _) ->
            val low = sel.lowercase()
            !low.contains(":not([data-theme") && darkSelRe.containsMatchIn(low)
        }
        val lightRules = ast.rules.filter { (sel, _) ->
            val low = sel.lowercase()
            low.contains(":not([data-theme") || !darkSelRe.containsMatchIn(low)
        }
        val primary = if (preferDark && darkRules.isNotEmpty()) collectFromRules(darkRules) else collectFromRules(lightRules)
        if (primary.isNotEmpty()) return primary
        if (preferDark) {
            val fallbackRules = collectFromRules(lightRules)
            if (fallbackRules.isNotEmpty()) return fallbackRules
        }
        val kfOut = ArrayList<String>()
        for (stops in ast.keyframeStops.values) {
            for ((_, stopBody) in stops) {
                for (m in CSS_URL_TOKEN_RE.findAll(stopBody)) {
                    val raw = (m.groupValues[1].ifEmpty { m.groupValues[2].ifEmpty { m.groupValues[3] } }).trim()
                    if (isValidCssImageUrl(raw)) kfOut.add(raw)
                }
            }
        }
        return kfOut
    }

    private fun resolveCssVars(raw: String, vars: Map<String, String>): String {
        if (!raw.contains("var(", ignoreCase = true) || vars.isEmpty()) return raw
        var cur = raw
        val varPropRe = Regex("""^--[a-zA-Z0-9_-]+$""")
        repeat(10) {
            val lower = cur.lowercase()
            val sb = StringBuilder(cur.length)
            var i = 0
            var changed = false
            while (i < cur.length) {
                val idx = lower.indexOf("var(", i)
                if (idx < 0) {
                    sb.append(cur, i, cur.length)
                    break
                }
                if (idx > 0 && (cur[idx - 1].isLetterOrDigit() || cur[idx - 1] == '-' || cur[idx - 1] == '_')) {
                    sb.append(cur, i, idx + 4)
                    i = idx + 4
                    continue
                }
                sb.append(cur, i, idx)
                var j = idx + 4
                var depth = 1
                var quote: Char? = null
                var commaPos = -1
                while (j < cur.length && depth > 0) {
                    val ch = cur[j]
                    if (quote != null) {
                        if (ch == '\\' && j + 1 < cur.length) {
                            j += 2
                            continue
                        }
                        if (ch == quote) quote = null
                    } else {
                        when (ch) {
                            '"', '\'' -> quote = ch
                            '(' -> depth++
                            ')' -> depth--
                            ',' -> if (depth == 1 && commaPos < 0) commaPos = j
                        }
                    }
                    j++
                }
                if (depth != 0) {
                    sb.append(cur, idx, cur.length)
                    break
                }
                val innerEnd = j - 1
                val propName = (if (commaPos >= 0) cur.substring(idx + 4, commaPos) else cur.substring(idx + 4, innerEnd)).trim()
                val fallback = if (commaPos >= 0) cur.substring(commaPos + 1, innerEnd).trim() else null
                val resolvedVal = if (varPropRe.matches(propName)) vars[propName] else null
                val replacement = when {
                    !resolvedVal.isNullOrBlank() &&
                        !resolvedVal.equals("initial", ignoreCase = true) &&
                        !resolvedVal.equals("unset", ignoreCase = true) -> resolvedVal
                    !fallback.isNullOrEmpty() -> fallback
                    else -> null
                }
                if (replacement != null && replacement != cur.substring(idx, j)) {
                    sb.append(replacement)
                    changed = true
                } else {
                    sb.append(cur, idx, j)
                }
                i = j
            }
            cur = sb.toString()
            if (!changed) return cur
        }
        return cur
    }

    private fun buildResolvedVars(
        baseVars: Map<String, String>,
        body: String,
        overrideVars: Map<String, String> = emptyMap(),
    ): Map<String, String> {
        val combined = LinkedHashMap(baseVars)
        combined.putAll(extractCssCustomProps(body))
        combined.putAll(overrideVars)
        val resolved = LinkedHashMap(combined)
        repeat(10) {
            var changed = false
            for ((k, v) in combined) {
                val expanded = simplifyColorMix(evalCssMathTokens(resolveCssVars(v, resolved)))
                if (resolved[k] != expanded) {
                    resolved[k] = expanded
                    changed = true
                }
            }
            if (!changed) return resolved
        }
        return resolved
    }

    private enum class CssMathUnit { NONE, PX, PCT }
    private data class CssMathVal(val value: Float, val unit: CssMathUnit)

    private fun formatCssMathNumber(v: Float): String {
        val clean = if (kotlin.math.abs(v) < 1e-4f) 0f else v
        val rounded = kotlin.math.round(clean)
        return if (kotlin.math.abs(clean - rounded) < 1e-3f) {
            rounded.toInt().toString()
        } else {
            String.format(java.util.Locale.US, "%.4f", clean).trimEnd('0').trimEnd('.')
        }
    }

    private fun evalCssMathTokens(raw: String, emPx: Float = 15f): String {
        val lowerCheck = raw.lowercase()
        if (!lowerCheck.contains("calc(") &&
            !lowerCheck.contains("clamp(") &&
            !lowerCheck.contains("min(") &&
            !lowerCheck.contains("max(") &&
            !lowerCheck.contains("round(")
        ) {
            return raw
        }

        fun evalSingleMathExpr(expr: String): CssMathVal? {
            val s = evalCssMathTokens(expr.trim(), emPx).trim()
            if (s.isEmpty() || s.contains("var(", ignoreCase = true)) return null
            var pos = 0
            fun skipWs() {
                while (pos < s.length && s[pos].isWhitespace()) pos++
            }

            var parseExpression: () -> CssMathVal? = { null }
            fun parsePrimary(): CssMathVal? {
                skipWs()
                if (pos >= s.length) return null
                if (s[pos] == '(') {
                    pos++
                    val v = parseExpression() ?: return null
                    skipWs()
                    if (pos >= s.length || s[pos] != ')') return null
                    pos++
                    return v
                }
                val start = pos
                while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
                if (pos == start) return null
                val num = s.substring(start, pos).toFloatOrNull() ?: return null
                val unitStart = pos
                while (pos < s.length && (s[pos].isLetter() || s[pos] == '%')) pos++
                val unitTok = s.substring(unitStart, pos).lowercase()
                return when (unitTok) {
                    "" -> CssMathVal(num, CssMathUnit.NONE)
                    "px" -> CssMathVal(num, CssMathUnit.PX)
                    "rem" -> CssMathVal(num * 16f, CssMathUnit.PX)
                    "em" -> CssMathVal(num * emPx, CssMathUnit.PX)
                    "vw" -> CssMathVal(num * 3.9f, CssMathUnit.PX)
                    "vh" -> CssMathVal(num * 8.4f, CssMathUnit.PX)
                    "%" -> CssMathVal(num, CssMathUnit.PCT)
                    else -> null
                }
            }

            fun parseUnary(): CssMathVal? {
                skipWs()
                if (pos < s.length && (s[pos] == '+' || s[pos] == '-')) {
                    val sign = if (s[pos] == '-') -1f else 1f
                    pos++
                    val inner = parseUnary() ?: return null
                    return CssMathVal(sign * inner.value, inner.unit)
                }
                return parsePrimary()
            }

            fun parseTerm(): CssMathVal? {
                var left = parseUnary() ?: return null
                while (true) {
                    skipWs()
                    if (pos >= s.length) break
                    val op = s[pos]
                    if (op != '*' && op != '/') break
                    pos++
                    val right = parseUnary() ?: return null
                    left = if (op == '*') {
                        when {
                            left.unit == CssMathUnit.NONE -> CssMathVal(left.value * right.value, right.unit)
                            right.unit == CssMathUnit.NONE -> CssMathVal(left.value * right.value, left.unit)
                            else -> return null
                        }
                    } else {
                        if (right.unit != CssMathUnit.NONE || kotlin.math.abs(right.value) < 1e-6f) return null
                        CssMathVal(left.value / right.value, left.unit)
                    }
                }
                return left
            }

            parseExpression = {
                var left = parseTerm()
                while (left != null) {
                    skipWs()
                    if (pos >= s.length) break
                    val op = s[pos]
                    if (op != '+' && op != '-') break
                    pos++
                    val right = parseTerm()
                    if (right == null) {
                        left = null
                        break
                    }
                    val signR = if (op == '-') -right.value else right.value
                    val resUnit = when {
                        left.unit == right.unit -> left.unit
                        left.unit == CssMathUnit.NONE && kotlin.math.abs(left.value) < 1e-5f -> right.unit
                        right.unit == CssMathUnit.NONE && kotlin.math.abs(right.value) < 1e-5f -> left.unit
                        else -> {
                            left = null
                            break
                        }
                    }
                    left = CssMathVal(left.value + signR, resUnit)
                }
                left
            }

            val res = parseExpression() ?: return null
            skipWs()
            return if (pos == s.length) res else null
        }

        fun formatMathVal(v: CssMathVal): String {
            val n = formatCssMathNumber(v.value)
            return when (v.unit) {
                CssMathUnit.NONE -> n
                CssMathUnit.PX -> "${n}px"
                CssMathUnit.PCT -> "$n%"
            }
        }

        val fnNames = listOf("clamp", "round", "calc", "min", "max")
        val lower = raw.lowercase()
        val sb = StringBuilder(raw.length)
        var i = 0
        while (i < raw.length) {
            var matchIdx = -1
            var matchedName = ""
            for (fn in fnNames) {
                var searchFrom = i
                while (true) {
                    val idx = lower.indexOf("$fn(", searchFrom)
                    if (idx < 0) break
                    val prevOk = idx == 0 || (!raw[idx - 1].isLetterOrDigit() && raw[idx - 1] != '-' && raw[idx - 1] != '_')
                    if (prevOk) {
                        if (matchIdx < 0 || idx < matchIdx) {
                            matchIdx = idx
                            matchedName = fn
                        }
                        break
                    }
                    searchFrom = idx + 1
                }
            }
            if (matchIdx < 0) {
                sb.append(raw, i, raw.length)
                break
            }
            sb.append(raw, i, matchIdx)
            val openParen = matchIdx + matchedName.length
            var j = openParen + 1
            var depth = 1
            var quote: Char? = null
            while (j < raw.length && depth > 0) {
                val ch = raw[j]
                if (quote != null) {
                    if (ch == '\\' && j + 1 < raw.length) {
                        j += 2
                        continue
                    }
                    if (ch == quote) quote = null
                } else {
                    when (ch) {
                        '"', '\'' -> quote = ch
                        '(' -> depth++
                        ')' -> depth--
                    }
                }
                j++
            }
            if (depth != 0) {
                sb.append(raw, matchIdx, raw.length)
                break
            }
            val inner = raw.substring(openParen + 1, j - 1)
            val evaluated: String? = when (matchedName) {
                "calc" -> evalSingleMathExpr(inner)?.let(::formatMathVal)
                "min", "max" -> {
                    val args = splitTopLevel(inner, onWhitespace = false).map { evalCssMathTokens(it, emPx) }
                    val vals = args.map { evalSingleMathExpr(it) }
                    if (vals.all { it != null } && vals.isNotEmpty()) {
                        val nonNull = vals.filterNotNull()
                        val targetUnit = nonNull.firstOrNull { it.unit != CssMathUnit.NONE }?.unit ?: CssMathUnit.NONE
                        val allCompatible = nonNull.all { it.unit == targetUnit || (it.unit == CssMathUnit.NONE && (targetUnit == CssMathUnit.NONE || kotlin.math.abs(it.value) < 1e-5f)) }
                        if (allCompatible) {
                            val agg = if (matchedName == "min") nonNull.minOf { it.value } else nonNull.maxOf { it.value }
                            formatMathVal(CssMathVal(agg, targetUnit))
                        } else {
                            "$matchedName(${args.joinToString(", ")})"
                        }
                    } else {
                        "$matchedName(${args.joinToString(", ")})"
                    }
                }
                "clamp" -> {
                    val args = splitTopLevel(inner, onWhitespace = false).map { evalCssMathTokens(it, emPx) }
                    val vals = args.map { evalSingleMathExpr(it) }
                    if (vals.size == 3 && vals.all { it != null }) {
                        val nonNull = vals.filterNotNull()
                        val targetUnit = nonNull.firstOrNull { it.unit != CssMathUnit.NONE }?.unit ?: CssMathUnit.NONE
                        val allCompatible = nonNull.all { it.unit == targetUnit || (it.unit == CssMathUnit.NONE && (targetUnit == CssMathUnit.NONE || kotlin.math.abs(it.value) < 1e-5f)) }
                        if (allCompatible) {
                            val clamped = kotlin.math.max(nonNull[0].value, kotlin.math.min(nonNull[1].value, nonNull[2].value))
                            formatMathVal(CssMathVal(clamped, targetUnit))
                        } else {
                            "clamp(${args.joinToString(", ")})"
                        }
                    } else {
                        "clamp(${args.joinToString(", ")})"
                    }
                }
                "round" -> {
                    val rawArgs = splitTopLevel(inner, onWhitespace = false).map { evalCssMathTokens(it, emPx) }
                    val strategies = setOf("nearest", "up", "down", "to-zero")
                    val firstLower = rawArgs.firstOrNull()?.trim()?.lowercase()
                    val (strategy, valIdx) = if (firstLower in strategies && rawArgs.size >= 2) firstLower!! to 1 else "nearest" to 0
                    val valueExpr = rawArgs.getOrNull(valIdx)?.let(::evalSingleMathExpr)
                    val stepExpr = rawArgs.getOrNull(valIdx + 1)?.let(::evalSingleMathExpr)
                    if (valueExpr != null) {
                        val stepVal = stepExpr?.value ?: 1f
                        if (kotlin.math.abs(stepVal) > 1e-6f && (stepExpr == null || stepExpr.unit == CssMathUnit.NONE || stepExpr.unit == valueExpr.unit)) {
                            val q = valueExpr.value / stepVal
                            val roundedQ = when (strategy) {
                                "up" -> kotlin.math.ceil(q)
                                "down" -> kotlin.math.floor(q)
                                "to-zero" -> kotlin.math.truncate(q)
                                else -> kotlin.math.round(q)
                            }
                            formatMathVal(CssMathVal(roundedQ * stepVal, valueExpr.unit))
                        } else null
                    } else null
                }
                else -> null
            }
            sb.append(evaluated ?: raw.substring(matchIdx, j))
            i = j
        }
        return sb.toString()
    }

    private fun parseLengthWithParentCap(raw: String, emPx: Float = 15f): Pair<Float?, Boolean> {
        val evaluated = evalCssMathTokens(raw.replace("!important", "", ignoreCase = true).trim(), emPx)
        val minMatch = Regex("""^\s*min\((.*)\)\s*$""", RegexOption.IGNORE_CASE).matchEntire(evaluated)
        if (minMatch != null) {
            val parts = splitTopLevel(minMatch.groupValues[1], onWhitespace = false)
            if (parts.size == 2) {
                val p0 = parts[0].trim()
                val p1 = parts[1].trim()
                if (p0 == "100%") {
                    val len = parsePxLength(p1, emPx)
                    if (len != null) return len to true
                } else if (p1 == "100%") {
                    val len = parsePxLength(p0, emPx)
                    if (len != null) return len to true
                }
            }
        }
        return parsePxLength(evaluated, emPx) to false
    }

    private fun isMaskCompletelyTransparent(raw: String): Boolean {
        val low = raw.replace("!important", "", ignoreCase = true).trim().lowercase()
        if (low.isEmpty() || low == "none" || low == "initial" || low == "unset") return false
        if (!low.contains("gradient(")) return false
        val innerMatches = Regex("""gradient\((.*)\)""").find(raw) ?: return false
        val tokens = splitTopLevel(innerMatches.groupValues[1], onWhitespace = false)
        var parsedStopCount = 0
        for (tok in tokens) {
            val stopParts = splitTopLevel(tok.trim(), onWhitespace = true)
            for (sp in stopParts) {
                val col = parseCssColor(sp) ?: continue
                parsedStopCount++
                if (col.alpha > 0.02f) return false
            }
        }
        return parsedStopCount > 0
    }

    private data class ParsedBorderImageShorthand(
        val urlIdx: Int? = null,
        val inheritSource: Boolean = false,
        val clear: Boolean = false,
        val slice: List<Float>? = null,
        val fill: Boolean = false,
        val width: List<Float>? = null,
        val outset: List<Float>? = null,
    )

    private fun parseBorderImageShorthand(raw: String, emPx: Float = 15f): ParsedBorderImageShorthand {
        val cleaned = evalCssMathTokens(raw.replace("!important", "", ignoreCase = true).trim(), emPx)
        if (cleaned.equals("none", ignoreCase = true)) return ParsedBorderImageShorthand(clear = true)
        if (cleaned.equals("inherit", ignoreCase = true)) return ParsedBorderImageShorthand(inheritSource = true)
        val sections = splitTopLevelByChar(cleaned, '/')
        val sec0 = sections.getOrNull(0).orEmpty()
        val urlIdx = Regex("""__URL_(\d+)__""").find(sec0)?.groupValues?.get(1)?.toIntOrNull()
        val inheritSource = Regex("""\binherit\b""", RegexOption.IGNORE_CASE).containsMatchIn(sec0)
        val clear = urlIdx == null && !inheritSource && Regex("""\bnone\b""", RegexOption.IGNORE_CASE).containsMatchIn(sec0)
        val hasFill = Regex("""\bfill\b""", RegexOption.IGNORE_CASE).containsMatchIn(sec0)
        val sec0Clean = sec0
            .replace(Regex("""__URL_\d+__"""), " ")
            .replace(Regex("""\b(inherit|none|fill|stretch|repeat|round|space)\b""", RegexOption.IGNORE_CASE), " ")
        val sliceNums = expandBoxFour(parseBoxFourNumbers(sec0Clean, emPx))
        val widthNums = sections.getOrNull(1)?.let { s ->
            val stripped = s.replace(Regex("""\b(stretch|repeat|round|space)\b""", RegexOption.IGNORE_CASE), " ")
            expandBoxFour(parseBoxFourNumbers(stripped, emPx))
        }
        val outsetNums = sections.getOrNull(2)?.let { s ->
            val stripped = s.replace(Regex("""\b(stretch|repeat|round|space)\b""", RegexOption.IGNORE_CASE), " ")
            expandBoxFour(parseBoxFourNumbers(stripped, emPx))
        }
        return ParsedBorderImageShorthand(
            urlIdx = urlIdx,
            inheritSource = inheritSource,
            clear = clear,
            slice = sliceNums,
            fill = hasFill,
            width = widthNums,
            outset = outsetNums,
        )
    }

    private data class ParsedBackgroundLayer(
        val urlIdx: Int? = null,
        val gradientRaw: String? = null,
        val bgPosRaw: String? = null,
        val bgSizeRaw: String? = null,
        val repeatRaw: String? = null,
        val originClipBox: String? = null,
        val bgColorRaw: String? = null,
    )

    private fun parseBackgroundShorthand(raw: String, emPx: Float = 15f): List<ParsedBackgroundLayer> {
        val cleaned = simplifyColorMix(evalCssMathTokens(raw.replace("!important", "", ignoreCase = true).trim(), emPx))
        if (cleaned.isEmpty() || cleaned.equals("none", ignoreCase = true)) return emptyList()
        val layers = splitTopLevel(cleaned, onWhitespace = false)
        val repeatKeywords = setOf("no-repeat", "repeat", "repeat-x", "repeat-y", "space", "round")
        val boxKeywords = setOf("border-box", "padding-box", "content-box")
        val attachKeywords = setOf("scroll", "fixed", "local", "none")
        return layers.mapNotNull { layerStr ->
            val slashParts = splitTopLevelByChar(layerStr, '/')
            val preTokens = splitTopLevel(slashParts[0].trim(), onWhitespace = true)
            var urlIdx: Int? = null
            var gradientRaw: String? = null
            var repeatRaw: String? = null
            var boxRaw: String? = null
            var colorRaw: String? = null
            val posTokens = ArrayList<String>()
            val sizeTokens = ArrayList<String>()

            fun isColorToken(tok: String): Boolean {
                val low = tok.lowercase()
                if (low == "transparent" || low.startsWith("#") || low.startsWith("rgb(") ||
                    low.startsWith("rgba(") || low.startsWith("hsl(") || low.startsWith("hsla(")
                ) {
                    return parseCssColor(tok) != null
                }
                return false
            }

            for (tok in preTokens) {
                val low = tok.lowercase()
                val urlM = Regex("""^__URL_(\d+)__$""").matchEntire(tok)
                when {
                    urlM != null -> urlIdx = urlM.groupValues[1].toIntOrNull()
                    low.startsWith("linear-gradient(") || low.startsWith("radial-gradient(") ||
                        low.startsWith("conic-gradient(") || low.startsWith("repeating-linear-gradient(") ||
                        low.startsWith("repeating-radial-gradient(") -> gradientRaw = tok
                    low in repeatKeywords -> repeatRaw = low
                    low in boxKeywords -> boxRaw = low
                    low in attachKeywords -> Unit
                    isColorToken(tok) -> colorRaw = tok
                    else -> posTokens.add(tok)
                }
            }

            if (slashParts.size >= 2) {
                val postTokens = splitTopLevel(slashParts.drop(1).joinToString(" / ").trim(), onWhitespace = true)
                for (tok in postTokens) {
                    val low = tok.lowercase()
                    val urlM = Regex("""^__URL_(\d+)__$""").matchEntire(tok)
                    when {
                        urlM != null -> urlIdx = urlM.groupValues[1].toIntOrNull()
                        low.startsWith("linear-gradient(") || low.startsWith("radial-gradient(") ||
                            low.startsWith("conic-gradient(") || low.startsWith("repeating-linear-gradient(") ||
                            low.startsWith("repeating-radial-gradient(") -> gradientRaw = tok
                        low in repeatKeywords -> repeatRaw = low
                        low in boxKeywords -> boxRaw = low
                        low in attachKeywords -> Unit
                        isColorToken(tok) -> colorRaw = tok
                        sizeTokens.size < 2 -> sizeTokens.add(tok)
                    }
                }
            }

            if (urlIdx == null && gradientRaw == null && colorRaw == null && posTokens.isEmpty() && sizeTokens.isEmpty()) {
                null
            } else {
                ParsedBackgroundLayer(
                    urlIdx = urlIdx,
                    gradientRaw = gradientRaw,
                    bgPosRaw = posTokens.takeIf { it.isNotEmpty() }?.joinToString(" "),
                    bgSizeRaw = sizeTokens.takeIf { it.isNotEmpty() }?.joinToString(" "),
                    repeatRaw = repeatRaw,
                    originClipBox = boxRaw,
                    bgColorRaw = colorRaw,
                )
            }
        }
    }

    private fun splitTopLevelByChar(raw: String, sep: Char): List<String> {
        val out = ArrayList<String>()
        val cur = StringBuilder()
        var parenDepth = 0
        var quote: Char? = null
        var i = 0
        while (i < raw.length) {
            val ch = raw[i]
            if (quote != null) {
                cur.append(ch)
                if (ch == '\\' && i + 1 < raw.length) {
                    cur.append(raw[i + 1])
                    i += 2
                    continue
                }
                if (ch == quote) quote = null
            } else {
                when {
                    ch == '"' || ch == '\'' -> {
                        quote = ch
                        cur.append(ch)
                    }
                    ch == '(' -> {
                        parenDepth++
                        cur.append(ch)
                    }
                    ch == ')' -> {
                        if (parenDepth > 0) parenDepth--
                        cur.append(ch)
                    }
                    parenDepth == 0 && ch == sep -> {
                        out.add(cur.toString().trim())
                        cur.clear()
                    }
                    else -> cur.append(ch)
                }
            }
            i++
        }
        out.add(cur.toString().trim())
        return out
    }

    private fun splitTopLevel(raw: String, onWhitespace: Boolean): List<String> {
        val out = ArrayList<String>()
        val cur = StringBuilder()
        var parenDepth = 0
        var quote: Char? = null
        for (ch in raw) {
            if (quote != null) {
                cur.append(ch)
                if (ch == quote) quote = null
                continue
            }
            when {
                ch == '"' || ch == '\'' -> {
                    quote = ch
                    cur.append(ch)
                }
                ch == '(' -> {
                    parenDepth++
                    cur.append(ch)
                }
                ch == ')' -> {
                    if (parenDepth > 0) parenDepth--
                    cur.append(ch)
                }
                parenDepth == 0 && (if (onWhitespace) ch.isWhitespace() else ch == ',') -> {
                    val tok = cur.toString().trim()
                    if (tok.isNotEmpty()) out.add(tok)
                    cur.clear()
                }
                else -> cur.append(ch)
            }
        }
        val tail = cur.toString().trim()
        if (tail.isNotEmpty()) out.add(tail)
        return out
    }

    private fun parsePxLength(raw: String, emPx: Float = 15f): Float? {
        val cleaned = evalCssMathTokens(raw.replace("!important", "", ignoreCase = true).trim(), emPx)
        if (cleaned.isEmpty() || cleaned.equals("auto", ignoreCase = true)) return null
        val calcMatch = Regex("""^\s*calc\((.*)\)\s*$""", RegexOption.IGNORE_CASE).matchEntire(cleaned)
        if (calcMatch != null) {
            val expr = calcMatch.groupValues[1]
            if (expr.contains('%')) return null
            val normalized = expr.replace(Regex("""\+\s*-"""), "-").replace(Regex("""-\s*-"""), "+")
            val terms = Regex("""([+-]?)\s*(\d*\.?\d+)(px|r?em|vw|vh)?""").findAll(normalized).toList()
            if (terms.isEmpty()) return null
            var sum = 0f
            for (t in terms) {
                val sign = if (t.groupValues[1] == "-") -1f else 1f
                val n = t.groupValues[2].toFloatOrNull() ?: continue
                val mult = when (t.groupValues[3].lowercase()) {
                    "rem" -> 16f
                    "em" -> emPx
                    "vw" -> 3.9f
                    "vh" -> 8.4f
                    else -> 1f
                }
                sum += sign * n * mult
            }
            return sum
        }
        val m = Regex("""^(-?\d*\.?\d+)(px|r?em|vw|vh)?$""", RegexOption.IGNORE_CASE).matchEntire(cleaned) ?: return null
        val n = m.groupValues[1].toFloatOrNull() ?: return null
        return when (m.groupValues[2].lowercase()) {
            "rem" -> n * 16f
            "em" -> n * emPx
            "vw" -> n * 3.9f
            "vh" -> n * 8.4f
            else -> n
        }
    }

    private fun parseBoxFourNumbers(raw: String, emPx: Float = 15f): List<Float> {
        val evaluated = evalCssMathTokens(
            raw.replace("!important", "", ignoreCase = true)
                .replace("fill", "")
                .replace("stretch", "")
                .replace("round", ""),
            emPx,
        )
        return Regex("""(-?\d*\.?\d+)(%|px|r?em|vw|vh)?""").findAll(evaluated).mapNotNull { m ->
            val n = m.groupValues[1].toFloatOrNull() ?: return@mapNotNull null
            when (m.groupValues[2].lowercase()) {
                "rem" -> n * 16f
                "em" -> n * emPx
                "vw" -> n * 3.9f
                "vh" -> n * 8.4f
                else -> n
            }
        }.toList()
    }

    private fun expandBoxFour(vals: List<Float>): List<Float>? = when (vals.size) {
        0 -> null
        1 -> listOf(vals[0], vals[0], vals[0], vals[0])
        2 -> listOf(vals[0], vals[1], vals[0], vals[1])
        3 -> listOf(vals[0], vals[1], vals[2], vals[1])
        else -> listOf(vals[0], vals[1], vals[2], vals[3])
    }

    private fun simplifyColorMix(raw: String): String {
        if (!raw.contains("color-mix(", ignoreCase = true)) return raw
        val lower = raw.lowercase()
        val sb = StringBuilder(raw.length)
        var i = 0
        while (i < raw.length) {
            val idx = lower.indexOf("color-mix(", i)
            if (idx < 0) {
                sb.append(raw, i, raw.length)
                break
            }
            sb.append(raw, i, idx)
            var j = idx + "color-mix(".length
            var depth = 1
            while (j < raw.length && depth > 0) {
                when (raw[j]) {
                    '(' -> depth++
                    ')' -> depth--
                }
                j++
            }
            val inner = if (depth == 0) {
                raw.substring(idx + "color-mix(".length, j - 1)
            } else {
                raw.substring(idx + "color-mix(".length)
            }
            val parts = splitTopLevel(inner, onWhitespace = false)
            val resolved = if (parts.size >= 3) {
                fun parseMixArg(arg: String): Pair<Color?, Float?> {
                    val tokens = splitTopLevel(arg.trim(), onWhitespace = true)
                    var pct: Float? = null
                    val colTokens = ArrayList<String>(tokens.size)
                    for (t in tokens) {
                        val evalT = evalCssMathTokens(t)
                        val m = Regex("""^(\d*\.?\d+)%$""").matchEntire(evalT.trim())
                        if (m != null && pct == null) {
                            pct = m.groupValues[1].toFloatOrNull()?.div(100f)
                        } else {
                            colTokens.add(evalT)
                        }
                    }
                    val colStr = colTokens.joinToString(" ").trim()
                    val col = when {
                        colStr.equals("transparent", ignoreCase = true) -> Color.Transparent
                        else -> CSS_COLOR_TOKEN_RE.find(colStr.lowercase())?.let { parseCssColor(it.value) }
                            ?: parseCssColor(colStr)
                    }
                    return col to pct
                }
                val (c1, p1Raw) = parseMixArg(parts[1])
                val (c2, p2Raw) = parseMixArg(parts[2])
                if (c1 != null && c2 != null) {
                    val w1 = when {
                        p1Raw != null -> p1Raw.coerceIn(0f, 1f)
                        p2Raw != null -> (1f - p2Raw).coerceIn(0f, 1f)
                        else -> 0.5f
                    }
                    val w2 = if (p2Raw != null && p1Raw == null) p2Raw.coerceIn(0f, 1f) else (1f - w1).coerceIn(0f, 1f)
                    val totalW = (w1 + w2).coerceAtLeast(1e-4f)
                    val nw1 = w1 / totalW
                    val nw2 = w2 / totalW
                    val r = c1.red * c1.alpha * nw1 + c2.red * c2.alpha * nw2
                    val g = c1.green * c1.alpha * nw1 + c2.green * c2.alpha * nw2
                    val b = c1.blue * c1.alpha * nw1 + c2.blue * c2.alpha * nw2
                    val a = (c1.alpha * nw1 + c2.alpha * nw2).coerceIn(0f, 1f)
                    val (or, og, ob) = if (a > 0.004f) {
                        Triple(
                            (r / a * 255f).toInt().coerceIn(0, 255),
                            (g / a * 255f).toInt().coerceIn(0, 255),
                            (b / a * 255f).toInt().coerceIn(0, 255),
                        )
                    } else {
                        Triple(0, 0, 0)
                    }
                    "rgba($or,$og,$ob,${String.format(java.util.Locale.US, "%.3f", a)})"
                } else null
            } else null
            sb.append(resolved ?: raw.substring(idx, j))
            i = j
        }
        return sb.toString()
    }

    private data class CssBgSizeSpec(
        val widthDp: Float? = null,
        val widthFraction: Float? = null,
        val widthSubFromFullDp: Float? = null,
        val heightDp: Float? = null,
        val heightFraction: Float? = null,
        val scaleMode: BgScaleMode = BgScaleMode.STRETCH,
    )

    private data class CssBgPosSpec(
        val alignCenterX: Boolean = false,
        val offsetXDp: Float = 0f,
        val offsetRightDp: Float? = null,
        val alignCenterY: Boolean = false,
        val offsetYDp: Float = 0f,
        val offsetBottomDp: Float? = null,
    )

    private fun parseCssBgSizeLayer(rawLayer: String, emPx: Float = 15f): CssBgSizeSpec {
        val evaluated = evalCssMathTokens(rawLayer.trim(), emPx)
        val tokens = splitTopLevel(evaluated, onWhitespace = true)
        if (tokens.isEmpty()) return CssBgSizeSpec()
        var wDp: Float? = null
        var wFrac: Float? = null
        var wSub: Float? = null
        var hDp: Float? = null
        var hFrac: Float? = null
        var scaleMode = BgScaleMode.STRETCH

        val wTok = tokens[0]
        val wLow = wTok.lowercase()
        val calcSub = Regex("""^\s*calc\(\s*100%\s*([+-])\s*([^)]+)\)\s*$""", RegexOption.IGNORE_CASE).matchEntire(wTok)
        when {
            wLow == "contain" -> scaleMode = BgScaleMode.CONTAIN
            wLow == "cover" -> scaleMode = BgScaleMode.COVER
            wLow == "auto" -> Unit
            calcSub != null -> {
                val delta = parsePxLength(calcSub.groupValues[2], emPx) ?: 0f
                wSub = if (calcSub.groupValues[1] == "-") delta else -delta
            }
            wTok.endsWith("%") -> wFrac = (wTok.removeSuffix("%").toFloatOrNull() ?: 100f) / 100f
            else -> wDp = parsePxLength(wTok, emPx)
        }
        val hTok = tokens.getOrNull(1)
        if (hTok != null && scaleMode == BgScaleMode.STRETCH) {
            when {
                hTok.equals("auto", ignoreCase = true) -> Unit
                hTok.endsWith("%") -> hFrac = (hTok.removeSuffix("%").toFloatOrNull() ?: 100f) / 100f
                else -> hDp = parsePxLength(hTok, emPx)
            }
        }
        return CssBgSizeSpec(wDp, wFrac, wSub, hDp, hFrac, scaleMode)
    }

    private fun parseCssBgPosLayer(rawLayer: String, emPx: Float = 15f): CssBgPosSpec {
        val evaluated = evalCssMathTokens(rawLayer.trim(), emPx)
        val tokens = splitTopLevel(evaluated, onWhitespace = true)
        var centerX = false
        var xDp = 0f
        var rightDp: Float? = null
        var centerY = false
        var yDp = 0f
        var bottomDp: Float? = null
        var xSet = false
        var ySet = false
        var i = 0
        while (i < tokens.size) {
            val tok = tokens[i].lowercase()
            when (tok) {
                "left" -> {
                    val nextPx = tokens.getOrNull(i + 1)?.let { parsePxLength(it, emPx) }
                    xDp = nextPx ?: 0f
                    xSet = true
                    if (nextPx != null) i++
                }
                "right" -> {
                    val nextPx = tokens.getOrNull(i + 1)?.let { parsePxLength(it, emPx) }
                    rightDp = nextPx ?: 0f
                    xSet = true
                    if (nextPx != null) i++
                }
                "top" -> {
                    val nextPx = tokens.getOrNull(i + 1)?.let { parsePxLength(it, emPx) }
                    yDp = nextPx ?: 0f
                    ySet = true
                    if (nextPx != null) i++
                }
                "bottom" -> {
                    val nextPx = tokens.getOrNull(i + 1)?.let { parsePxLength(it, emPx) }
                    bottomDp = nextPx ?: 0f
                    ySet = true
                    if (nextPx != null) i++
                }
                "center" -> {
                    if (!xSet) {
                        centerX = true
                        xSet = true
                    } else if (!ySet) {
                        centerY = true
                        ySet = true
                    }
                }
                else -> {
                    val px = parsePxLength(tokens[i], emPx)
                    if (px != null) {
                        if (!xSet) {
                            xDp = px
                            xSet = true
                        } else if (!ySet) {
                            yDp = px
                            ySet = true
                        }
                    }
                }
            }
            i++
        }
        return CssBgPosSpec(centerX, xDp, rightDp, centerY, yDp, bottomDp)
    }

    private data class CascadedCommunityCss(
        val baseStr: String,
        val beforeStr: String,
        val afterStr: String,
        val darkStr: String,
        val urlCounter: Int,
        val darkImageCount: Int,
        val hasStandaloneDarkRule: Boolean,
        val baseVars: Map<String, String>,
        val beforeVars: Map<String, String>,
        val afterVars: Map<String, String>,
        val keyframesReplaced: Map<String, List<Pair<Float, String>>> = emptyMap(),
    )

    private data class ParsedCssTransform(
        val txSelf: Float = 0f,
        val txDp: Float = 0f,
        val tySelf: Float = 0f,
        val tyDp: Float = 0f,
        val scaleX: Float = 1f,
        val scaleY: Float = 1f,
        val rotateDeg: Float = 0f,
        val hasTx: Boolean = false,
        val hasTy: Boolean = false,
        val hasScaleX: Boolean = false,
        val hasScaleY: Boolean = false,
        val hasRotate: Boolean = false,
    )

    private fun parseTranslateComponent(arg: String, emPx: Float = 15f): Pair<Float, Float> {
        val evaluated = evalCssMathTokens(arg.trim(), emPx).trim()
        val calcPctLen = Regex("""^calc\(\s*(-?\d*\.?\d+)%\s*([+-])\s*([^)]+)\)$""", RegexOption.IGNORE_CASE)
            .matchEntire(evaluated)
        if (calcPctLen != null) {
            val pct = (calcPctLen.groupValues[1].toFloatOrNull() ?: 0f) / 100f
            val sign = if (calcPctLen.groupValues[2] == "-") -1f else 1f
            val px = parsePxLength(calcPctLen.groupValues[3], emPx) ?: 0f
            return pct to (sign * px)
        }
        return when {
            evaluated.endsWith("%") -> (evaluated.removeSuffix("%").trim().toFloatOrNull()?.div(100f) ?: 0f) to 0f
            else -> 0f to (parsePxLength(evaluated, emPx) ?: 0f)
        }
    }

    private fun parseRotateAngleDeg(arg: String): Float? {
        val clean = arg.trim().lowercase()
        val m = Regex("""^(-?\d*\.?\d+)(deg|turn|rad|grad)?$""").matchEntire(clean) ?: return null
        val n = m.groupValues[1].toFloatOrNull() ?: return null
        return when (m.groupValues[2]) {
            "turn" -> n * 360f
            "rad" -> (n * 180.0 / PI).toFloat()
            "grad" -> n * 0.9f
            else -> n
        }
    }

    private fun parseTransformDecls(raw: String, emPx: Float = 15f): ParsedCssTransform {
        val low = raw.lowercase()
        var txSelf = 0f
        var txDp = 0f
        var tySelf = 0f
        var tyDp = 0f
        var scaleX = 1f
        var scaleY = 1f
        var rotateDeg = 0f
        var hasTx = false
        var hasTy = false
        var hasScaleX = false
        var hasScaleY = false
        var hasRotate = false
        var idx = 0
        val fnRe = Regex("""\b(translate3d|translatex|translatey|translate|scale3d|scalex|scaley|scale|rotatez|rotate)\(""", RegexOption.IGNORE_CASE)
        while (idx < raw.length) {
            val m = fnRe.find(low, idx) ?: break
            val open = m.range.last
            var j = open + 1
            var depth = 1
            while (j < raw.length && depth > 0) {
                when (raw[j]) {
                    '(' -> depth++
                    ')' -> depth--
                }
                j++
            }
            if (depth == 0) {
                val args = splitTopLevel(raw.substring(open + 1, j - 1), onWhitespace = false)
                when (m.groupValues[1].lowercase()) {
                    "translate", "translate3d" -> {
                        args.getOrNull(0)?.let {
                            val (sx, px) = parseTranslateComponent(it, emPx)
                            txSelf += sx
                            txDp += px
                            hasTx = true
                        }
                        args.getOrNull(1)?.let {
                            val (sy, py) = parseTranslateComponent(it, emPx)
                            tySelf += sy
                            tyDp += py
                            hasTy = true
                        }
                    }
                    "translatex" -> args.getOrNull(0)?.let {
                        val (sx, px) = parseTranslateComponent(it, emPx)
                        txSelf += sx
                        txDp += px
                        hasTx = true
                    }
                    "translatey" -> args.getOrNull(0)?.let {
                        val (sy, py) = parseTranslateComponent(it, emPx)
                        tySelf += sy
                        tyDp += py
                        hasTy = true
                    }
                    "scale", "scale3d" -> {
                        val sx = args.getOrNull(0)?.toFloatOrNull()
                        val sy = args.getOrNull(1)?.toFloatOrNull() ?: sx
                        if (sx != null) {
                            scaleX *= sx
                            hasScaleX = true
                        }
                        if (sy != null) {
                            scaleY *= sy
                            hasScaleY = true
                        }
                    }
                    "scalex" -> args.getOrNull(0)?.toFloatOrNull()?.let {
                        scaleX *= it
                        hasScaleX = true
                    }
                    "scaley" -> args.getOrNull(0)?.toFloatOrNull()?.let {
                        scaleY *= it
                        hasScaleY = true
                    }
                    "rotate", "rotatez" -> args.getOrNull(0)?.let(::parseRotateAngleDeg)?.let {
                        rotateDeg += it
                        hasRotate = true
                    }
                }
            }
            idx = j
        }
        return ParsedCssTransform(txSelf, txDp, tySelf, tyDp, scaleX, scaleY, rotateDeg, hasTx, hasTy, hasScaleX, hasScaleY, hasRotate)
    }

    private fun parseDurationMs(rawTok: String): Long? {
        val m = Regex("""^(-?\d*\.?\d+)(ms|s)$""", RegexOption.IGNORE_CASE).matchEntire(rawTok.trim()) ?: return null
        val n = m.groupValues[1].toDoubleOrNull() ?: return null
        val mult = if (m.groupValues[2].equals("ms", ignoreCase = true)) 1.0 else 1000.0
        return (n * mult).roundToInt().toLong()
    }

    private fun parseTimingFn(rawTok: String): CssTimingFn? {
        val t = rawTok.trim().lowercase()
        return when {
            t == "linear" -> CssTimingFn.Linear
            t == "ease" -> CssTimingFn.CubicBezier.Ease
            t == "ease-in" -> CssTimingFn.CubicBezier.EaseIn
            t == "ease-out" -> CssTimingFn.CubicBezier.EaseOut
            t == "ease-in-out" -> CssTimingFn.CubicBezier.EaseInOut
            t == "step-start" -> CssTimingFn.Steps(1, jumpStart = true)
            t == "step-end" -> CssTimingFn.Steps(1, jumpStart = false)
            t.startsWith("steps(") && t.endsWith(")") -> {
                val args = splitTopLevel(t.removePrefix("steps(").removeSuffix(")"), onWhitespace = false)
                val count = args.firstOrNull()?.toIntOrNull()?.coerceAtLeast(1) ?: return null
                val pos = args.getOrNull(1)?.lowercase() ?: "end"
                CssTimingFn.Steps(
                    count = count,
                    jumpStart = pos == "start" || pos == "jump-start",
                    jumpNone = pos == "jump-none",
                )
            }
            t.startsWith("cubic-bezier(") && t.endsWith(")") -> {
                val nums = splitTopLevel(t.removePrefix("cubic-bezier(").removeSuffix(")"), onWhitespace = false)
                    .mapNotNull { it.toFloatOrNull() }
                if (nums.size >= 4) CssTimingFn.CubicBezier(nums[0], nums[1], nums[2], nums[3]) else null
            }
            else -> null
        }
    }

    private fun parseCssAnimations(body: String, vars: Map<String, String>): List<CssAnimSpec> {
        var names = mutableListOf<String>()
        var durations = mutableListOf<Long>()
        var delays = mutableListOf<Long>()
        var timings = mutableListOf<CssTimingFn>()
        var directions = mutableListOf<CssAnimDirection>()
        var iterations = mutableListOf<Float>()
        var playStates = mutableListOf<String>()

        val fillKeywords = setOf("none", "forwards", "backwards", "both", "running", "paused")
        fun parseDirection(tok: String): CssAnimDirection? = when (tok.lowercase()) {
            "normal" -> CssAnimDirection.NORMAL
            "reverse" -> CssAnimDirection.REVERSE
            "alternate" -> CssAnimDirection.ALTERNATE
            "alternate-reverse" -> CssAnimDirection.ALTERNATE_REVERSE
            else -> null
        }

        for (decl in splitCssDeclarations(body)) {
            val colon = decl.indexOf(':')
            if (colon <= 0) continue
            val prop = decl.substring(0, colon).trim().lowercase()
            if (!prop.startsWith("animation") && !prop.startsWith("-webkit-animation")) continue
            val rawVal = resolveCssVars(decl.substring(colon + 1).trim(), vars)
                .replace("!important", "", ignoreCase = true)
                .trim()
            if (rawVal.isEmpty()) continue
            val parts = splitTopLevel(rawVal, onWhitespace = false)
            when (prop) {
                "animation", "-webkit-animation" -> {
                    names = mutableListOf()
                    durations = mutableListOf()
                    delays = mutableListOf()
                    timings = mutableListOf()
                    directions = mutableListOf()
                    iterations = mutableListOf()
                    playStates = mutableListOf()
                    for (layer in parts) {
                        var dur: Long? = null
                        var del = 0L
                        var timing: CssTimingFn = CssTimingFn.CubicBezier.Ease
                        var dir = CssAnimDirection.NORMAL
                        var iter = Float.POSITIVE_INFINITY
                        var state = "running"
                        var name = "none"
                        for (tok in splitTopLevel(layer, onWhitespace = true)) {
                            val clean = tok.trim().trim('"', '\'')
                            val low = clean.lowercase()
                            val ms = parseDurationMs(low)
                            val tf = if (ms == null) parseTimingFn(low) else null
                            val d = if (ms == null && tf == null) parseDirection(low) else null
                            when {
                                ms != null -> if (dur == null) dur = ms else del = ms
                                tf != null -> timing = tf
                                d != null -> dir = d
                                low == "infinite" -> iter = Float.POSITIVE_INFINITY
                                low == "paused" || low == "running" -> state = low
                                low in fillKeywords -> if (low == "none" && name == "none") name = "none"
                                clean.toFloatOrNull() != null && dur != null -> iter = clean.toFloat().coerceAtLeast(0f)
                                !low.contains('(') -> name = clean
                            }
                        }
                        names.add(name)
                        durations.add(dur ?: 0L)
                        delays.add(del)
                        timings.add(timing)
                        directions.add(dir)
                        iterations.add(iter)
                        playStates.add(state)
                    }
                }
                "animation-name", "-webkit-animation-name" -> {
                    names = parts.map { it.trim().trim('"', '\'') }.toMutableList()
                }
                "animation-duration", "-webkit-animation-duration" -> {
                    durations = parts.map { parseDurationMs(it) ?: 0L }.toMutableList()
                }
                "animation-delay", "-webkit-animation-delay" -> {
                    delays = parts.map { parseDurationMs(it) ?: 0L }.toMutableList()
                }
                "animation-timing-function", "-webkit-animation-timing-function" -> {
                    timings = parts.map { parseTimingFn(it) ?: CssTimingFn.CubicBezier.Ease }.toMutableList()
                }
                "animation-direction", "-webkit-animation-direction" -> {
                    directions = parts.map { parseDirection(it) ?: CssAnimDirection.NORMAL }.toMutableList()
                }
                "animation-iteration-count", "-webkit-animation-iteration-count" -> {
                    iterations = parts.map {
                        if (it.equals("infinite", ignoreCase = true)) Float.POSITIVE_INFINITY
                        else it.toFloatOrNull()?.coerceAtLeast(0f) ?: 1f
                    }.toMutableList()
                }
                "animation-play-state", "-webkit-animation-play-state" -> {
                    playStates = parts.map { it.lowercase() }.toMutableList()
                }
            }
        }

        if (names.isEmpty()) return emptyList()
        val out = ArrayList<CssAnimSpec>(names.size)
        for (i in names.indices) {
            val n = names[i]
            if (n.isEmpty() || n.equals("none", ignoreCase = true)) continue
            val dur = durations.getOrNull(i % durations.size.coerceAtLeast(1)) ?: 0L
            if (dur <= 0L) continue
            val state = playStates.getOrNull(i % playStates.size.coerceAtLeast(1)) ?: "running"
            if (state == "paused") continue
            val iter = iterations.getOrNull(i % iterations.size.coerceAtLeast(1)) ?: Float.POSITIVE_INFINITY
            if (iter <= 0f) continue
            out.add(
                CssAnimSpec(
                    name = n,
                    durationMs = dur,
                    delayMs = delays.getOrNull(i % delays.size.coerceAtLeast(1)) ?: 0L,
                    timing = timings.getOrNull(i % timings.size.coerceAtLeast(1)) ?: CssTimingFn.CubicBezier.Ease,
                    direction = directions.getOrNull(i % directions.size.coerceAtLeast(1)) ?: CssAnimDirection.NORMAL,
                    iterationCount = iter,
                ),
            )
        }
        return out
    }

    private fun findKeyframeStops(
        keyframes: Map<String, List<Pair<Float, String>>>,
        animName: String,
    ): List<Pair<Float, String>>? =
        keyframes[animName] ?: keyframes.entries.firstOrNull { it.key.equals(animName, ignoreCase = true) }?.value

    private fun lcmPeriodMs(periods: List<Long>): Long {
        fun gcd(a: Long, b: Long): Long {
            var x = a
            var y = b
            while (y != 0L) {
                val t = x % y
                x = y
                y = t
            }
            return x.coerceAtLeast(1L)
        }
        var res = periods.firstOrNull()?.coerceAtLeast(100L) ?: return 0L
        for (i in 1 until periods.size) {
            val p = periods[i].coerceAtLeast(100L)
            val next = (res / gcd(res, p)) * p
            res = if (next in 1L..24_000L) next else max(res, p).coerceAtMost(24_000L)
        }
        return res.coerceIn(100L, 24_000L)
    }

    private fun interpolateCssValues(
        v0Raw: String,
        v1Raw: String,
        localT: Float,
        forceStepDiscrete: Boolean,
        emPx: Float,
    ): String {
        val v0 = simplifyColorMix(evalCssMathTokens(v0Raw.trim(), emPx))
        val v1 = simplifyColorMix(evalCssMathTokens(v1Raw.trim(), emPx))
        if (v0 == v1) return v0
        val urlRe = Regex("""__URL_\d+__""")
        if (urlRe.containsMatchIn(v0) || urlRe.containsMatchIn(v1)) {
            return if (forceStepDiscrete) {
                if (localT < 0.9999f) v0 else v1
            } else {
                if (localT < 0.5f) v0 else v1
            }
        }
        val c0 = parseCssColor(v0)
        val c1 = parseCssColor(v1)
        if (c0 != null && c1 != null) {
            val t = localT.coerceIn(0f, 1f)
            val r = ((c0.red + (c1.red - c0.red) * t) * 255f).roundToInt().coerceIn(0, 255)
            val g = ((c0.green + (c1.green - c0.green) * t) * 255f).roundToInt().coerceIn(0, 255)
            val b = ((c0.blue + (c1.blue - c0.blue) * t) * 255f).roundToInt().coerceIn(0, 255)
            val a = (c0.alpha + (c1.alpha - c0.alpha) * t).coerceIn(0f, 1f)
            return "rgba($r,$g,$b,${String.format(java.util.Locale.US, "%.3f", a)})"
        }
        val toks0 = splitTopLevel(v0, onWhitespace = true)
        val toks1 = splitTopLevel(v1, onWhitespace = true)
        if (toks0.size == toks1.size && toks0.isNotEmpty()) {
            val numUnitRe = Regex("""^(-?\d*\.?\d+)(%|px|deg|rem|em|vw|vh)?$""", RegexOption.IGNORE_CASE)
            val out = ArrayList<String>(toks0.size)
            var allNum = true
            for (k in toks0.indices) {
                val m0 = numUnitRe.matchEntire(toks0[k])
                val m1 = numUnitRe.matchEntire(toks1[k])
                if (m0 == null || m1 == null) {
                    allNum = false
                    break
                }
                val n0 = m0.groupValues[1].toFloatOrNull()
                val n1 = m1.groupValues[1].toFloatOrNull()
                val u0 = m0.groupValues[2].lowercase()
                val u1 = m1.groupValues[2].lowercase()
                val sameOrZero = u0 == u1 || (u0.isEmpty() && n0 == 0f) || (u1.isEmpty() && n1 == 0f)
                if (n0 == null || n1 == null || !sameOrZero) {
                    allNum = false
                    break
                }
                val unit = u1.ifEmpty { u0 }
                val n = n0 + (n1 - n0) * localT
                val numStr = formatCssMathNumber(n)
                out.add("$numStr$unit")
            }
            if (allNum) return out.joinToString(" ")
        }
        return if (forceStepDiscrete) {
            if (localT < 0.9999f) v0 else v1
        } else {
            if (localT < 0.5f) v0 else v1
        }
    }

    private data class ElementAnimAnalysis(
        val visualTracks: List<CssVisualAnimTrack> = emptyList(),
        val timelineAnims: List<CssAnimSpec> = emptyList(),
        val periodMs: Long = 0L,
        val sampleCount: Int = 0,
        val animatesOpacityToVisible: Boolean = false,
    )

    private fun extractVisualAnimTrack(
        anim: CssAnimSpec,
        stops: List<Pair<Float, String>>,
        baseTransform: ParsedCssTransform,
        baseOpacity: Float,
        vars: Map<String, String>,
        emPx: Float,
    ): CssVisualAnimTrack? {
        val rawStops = LinkedHashMap<Float, Pair<ParsedCssTransform?, Float?>>()
        var hasAnyTf = false
        var hasAnyOp = false
        for ((frac, stopBody) in stops) {
            var stopTf: ParsedCssTransform? = null
            var stopOp: Float? = null
            for (decl in splitCssDeclarations(stopBody)) {
                val colon = decl.indexOf(':')
                if (colon <= 0) continue
                val prop = decl.substring(0, colon).trim().lowercase()
                val rv = resolveCssVars(decl.substring(colon + 1).trim(), vars)
                    .replace("!important", "", ignoreCase = true)
                    .trim()
                if (rv.contains("var(", ignoreCase = true)) continue
                when (prop) {
                    "transform", "-webkit-transform" -> {
                        stopTf = parseTransformDecls(rv, emPx)
                        hasAnyTf = true
                    }
                    "opacity" -> {
                        rv.toFloatOrNull()?.let {
                            stopOp = it.coerceIn(0f, 1f)
                            hasAnyOp = true
                        }
                    }
                }
            }
            if (stopTf != null || stopOp != null) {
                val existing = rawStops[frac]
                rawStops[frac] = (stopTf ?: existing?.first) to (stopOp ?: existing?.second)
            }
        }
        if (!hasAnyTf && !hasAnyOp) return null

        val outStops = ArrayList<CssVisualTransformStop>()
        fun addStop(frac: Float, tf: ParsedCssTransform?, op: Float?, isSyntheticRest: Boolean = false) {
            outStops.add(
                CssVisualTransformStop(
                    fraction = frac,
                    txDpDelta = if (hasAnyTf) (if (isSyntheticRest) 0f else (tf?.let { it.txDp - baseTransform.txDp })) else null,
                    tyDpDelta = if (hasAnyTf) (if (isSyntheticRest) 0f else (tf?.let { it.tyDp - baseTransform.tyDp })) else null,
                    txSelfDelta = if (hasAnyTf) (if (isSyntheticRest) 0f else (tf?.let { it.txSelf - baseTransform.txSelf })) else null,
                    tySelfDelta = if (hasAnyTf) (if (isSyntheticRest) 0f else (tf?.let { it.tySelf - baseTransform.tySelf })) else null,
                    scaleX = if (hasAnyTf) (if (isSyntheticRest) 1f else (tf?.let { if (it.hasScaleX) it.scaleX / baseTransform.scaleX.coerceAtLeast(0.01f) else 1f })) else null,
                    scaleY = if (hasAnyTf) (if (isSyntheticRest) 1f else (tf?.let { if (it.hasScaleY) it.scaleY / baseTransform.scaleY.coerceAtLeast(0.01f) else 1f })) else null,
                    rotateDeg = if (hasAnyTf) (if (isSyntheticRest) 0f else (tf?.let { if (it.hasRotate) it.rotateDeg - baseTransform.rotateDeg else 0f })) else null,
                    opacity = if (hasAnyOp) (if (isSyntheticRest) baseOpacity else op) else null,
                ),
            )
        }

        val sortedFracs = rawStops.keys.sorted()
        if (sortedFracs.first() > 0.001f) {
            addStop(0f, null, null, isSyntheticRest = true)
        }
        for (f in sortedFracs) {
            val (tf, op) = rawStops.getValue(f)
            addStop(f, tf, op, isSyntheticRest = false)
        }
        if (sortedFracs.last() < 0.999f) {
            addStop(1f, null, null, isSyntheticRest = true)
        }
        return CssVisualAnimTrack(anim, outStops)
    }

    private fun analyzeElementAnimations(
        body: String,
        vars: Map<String, String>,
        keyframes: Map<String, List<Pair<Float, String>>>,
        baseTransform: ParsedCssTransform = ParsedCssTransform(),
        baseOpacity: Float = 1f,
        emPx: Float = 15f,
    ): ElementAnimAnalysis {
        if (keyframes.isEmpty() || body.isBlank()) return ElementAnimAnalysis()
        val anims = parseCssAnimations(body, vars)
        if (anims.isEmpty()) return ElementAnimAnalysis()
        val visualTracks = ArrayList<CssVisualAnimTrack>()
        val timelineAnims = ArrayList<CssAnimSpec>()
        var animatesOpacityToVisible = false

        for (anim in anims) {
            val stops = findKeyframeStops(keyframes, anim.name) ?: continue
            var hasCategoryB = false
            var usesDynamicVarInVisual = false
            for ((_, stopBody) in stops) {
                for (decl in splitCssDeclarations(stopBody)) {
                    val colon = decl.indexOf(':')
                    if (colon <= 0) continue
                    val prop = decl.substring(0, colon).trim().lowercase()
                    val vRaw = decl.substring(colon + 1).trim()
                    when {
                        prop == "opacity" -> {
                            val opVal = resolveCssVars(vRaw, vars).toFloatOrNull()
                            if (opVal != null && opVal > 0.01f) animatesOpacityToVisible = true
                            if (vRaw.contains("var(") && resolveCssVars(vRaw, vars).contains("var(")) usesDynamicVarInVisual = true
                        }
                        prop == "transform" || prop == "-webkit-transform" -> {
                            if (vRaw.contains("var(") && resolveCssVars(vRaw, vars).contains("var(")) usesDynamicVarInVisual = true
                        }
                        else -> hasCategoryB = true
                    }
                }
            }
            if (hasCategoryB || usesDynamicVarInVisual) {
                timelineAnims.add(anim)
            } else {
                extractVisualAnimTrack(anim, stops, baseTransform, baseOpacity, vars, emPx)?.let {
                    visualTracks.add(it)
                }
            }
        }
        if (timelineAnims.isEmpty()) {
            return ElementAnimAnalysis(
                visualTracks = visualTracks,
                animatesOpacityToVisible = animatesOpacityToVisible,
            )
        }
        val periodMs = lcmPeriodMs(timelineAnims.map { it.cyclePeriodMs.coerceAtLeast(100L) })
        val stepCount = timelineAnims.mapNotNull { (it.timing as? CssTimingFn.Steps)?.count }.maxOrNull()
        val sampleCount = when {
            timelineAnims.size == 1 && stepCount != null && stepCount in 2..64 && timelineAnims[0].durationMs == periodMs ->
                stepCount
            stepCount != null -> max(stepCount * 2, 24).coerceIn(16, 60)
            else -> ((periodMs / 150L).toInt().coerceIn(24, 48) / 2) * 2
        }
        return ElementAnimAnalysis(
            visualTracks = visualTracks,
            timelineAnims = timelineAnims,
            periodMs = periodMs,
            sampleCount = sampleCount.coerceAtLeast(2),
            animatesOpacityToVisible = animatesOpacityToVisible,
        )
    }

    private fun sampleKeyframeFrameOverrides(
        anims: List<CssAnimSpec>,
        keyframes: Map<String, List<Pair<Float, String>>>,
        baseBody: String,
        baseVars: Map<String, String>,
        elapsedMs: Long,
        emPx: Float,
    ): Pair<Map<String, String>, String> {
        val baseDecls = LinkedHashMap<String, String>()
        for (decl in splitCssDeclarations(baseBody)) {
            val colon = decl.indexOf(':')
            if (colon <= 0) continue
            val prop = decl.substring(0, colon).trim().lowercase()
            if (!prop.startsWith("--")) {
                baseDecls[prop] = decl.substring(colon + 1).replace("!important", "", ignoreCase = true).trim()
            }
        }
        val varOverrides = LinkedHashMap<String, String>()
        val declOverrides = LinkedHashMap<String, String>()

        fun evalPropAtFraction(
            prop: String,
            explicitStops: List<Pair<Float, String>>,
            directedFrac: Float,
            timing: CssTimingFn,
            resolvedVars: Map<String, String>,
        ): String? {
            val fallbackVal = if (prop.startsWith("--")) {
                resolvedVars[prop] ?: "0"
            } else {
                baseDecls[prop]?.let { resolveCssVars(it, resolvedVars) } ?: explicitStops.firstOrNull()?.second
            } ?: return null
            val fullStops = ArrayList<Pair<Float, String>>(explicitStops.size + 2)
            if (explicitStops.first().first > 0.001f) fullStops.add(0f to fallbackVal)
            fullStops.addAll(explicitStops)
            if (explicitStops.last().first < 0.999f) fullStops.add(1f to fallbackVal)

            val f = directedFrac.coerceIn(0f, 1f)
            var idx0 = 0
            for (k in 0 until fullStops.size - 1) {
                if (f >= fullStops[k].first) idx0 = k
            }
            val idx1 = (idx0 + 1).coerceAtMost(fullStops.lastIndex)
            val s0 = fullStops[idx0]
            val s1 = fullStops[idx1]
            val span = (s1.first - s0.first)
            val rawLocalT = if (span > 1e-5f) ((f - s0.first) / span).coerceIn(0f, 1f) else 0f
            val localT = timing.transform(rawLocalT)
            val v0 = resolveCssVars(s0.second, resolvedVars)
            val v1 = resolveCssVars(s1.second, resolvedVars)
            return interpolateCssValues(v0, v1, localT, forceStepDiscrete = timing is CssTimingFn.Steps, emPx = emPx)
        }

        // Pass 1: evaluate custom properties (`--*`) so Pass 2 regular declarations resolve updated `--vars`
        for (anim in anims) {
            val stops = findKeyframeStops(keyframes, anim.name) ?: continue
            val directed = anim.directedFracAt(elapsedMs)
            val byVar = LinkedHashMap<String, MutableList<Pair<Float, String>>>()
            for ((frac, stopBody) in stops) {
                for (decl in splitCssDeclarations(stopBody)) {
                    val colon = decl.indexOf(':')
                    if (colon <= 0) continue
                    val prop = decl.substring(0, colon).trim()
                    if (prop.startsWith("--")) {
                        byVar.getOrPut(prop) { ArrayList() }.add(frac to decl.substring(colon + 1).trim())
                    }
                }
            }
            for ((varName, propStops) in byVar) {
                propStops.sortBy { it.first }
                evalPropAtFraction(varName, propStops, directed, anim.timing, baseVars)?.let {
                    varOverrides[varName] = it
                }
            }
        }
        val frameVars = if (varOverrides.isEmpty()) baseVars else buildResolvedVars(baseVars, baseBody, varOverrides)

        // Pass 2: evaluate regular CSS declarations at the same frame
        for (anim in anims) {
            val stops = findKeyframeStops(keyframes, anim.name) ?: continue
            val directed = anim.directedFracAt(elapsedMs)
            val byProp = LinkedHashMap<String, MutableList<Pair<Float, String>>>()
            for ((frac, stopBody) in stops) {
                for (decl in splitCssDeclarations(stopBody)) {
                    val colon = decl.indexOf(':')
                    if (colon <= 0) continue
                    val prop = decl.substring(0, colon).trim().lowercase()
                    if (!prop.startsWith("--")) {
                        val cleanVal = decl.substring(colon + 1).replace("!important", "", ignoreCase = true).trim()
                        byProp.getOrPut(prop) { ArrayList() }.add(frac to cleanVal)
                    }
                }
            }
            for ((prop, propStops) in byProp) {
                propStops.sortBy { it.first }
                evalPropAtFraction(prop, propStops, directed, anim.timing, frameVars)?.let {
                    declOverrides[prop] = it
                }
            }
        }
        val declBuf = if (declOverrides.isEmpty()) "" else declOverrides.entries.joinToString("; ") { "${it.key}: ${it.value}" }
        return varOverrides to declBuf
    }

    fun evaluateVisualAnim(tracks: List<CssVisualAnimTrack>, elapsedMs: Long): EvaluatedVisualAnim {
        if (tracks.isEmpty()) return EvaluatedVisualAnim()
        var txDp = 0f
        var tyDp = 0f
        var txSelf = 0f
        var tySelf = 0f
        var sx = 1f
        var sy = 1f
        var rot = 0f
        var opacity: Float? = null

        fun interpScalar(stops: List<CssVisualTransformStop>, p: Float, selector: (CssVisualTransformStop) -> Float?): Float? {
            var prevFrac = -1f
            var prevVal: Float? = null
            var nextFrac = 2f
            var nextVal: Float? = null
            for (st in stops) {
                val v = selector(st) ?: continue
                if (st.fraction <= p && st.fraction >= prevFrac) {
                    prevFrac = st.fraction
                    prevVal = v
                }
                if (st.fraction >= p && st.fraction < nextFrac) {
                    nextFrac = st.fraction
                    nextVal = v
                }
            }
            if (prevVal == null) return nextVal
            if (nextVal == null || nextFrac - prevFrac < 1e-5f) return prevVal
            val localT = ((p - prevFrac) / (nextFrac - prevFrac)).coerceIn(0f, 1f)
            return prevVal + (nextVal - prevVal) * localT
        }

        for (track in tracks) {
            if (track.stops.isEmpty()) continue
            val p = track.spec.progressAt(elapsedMs)
            interpScalar(track.stops, p) { it.txDpDelta }?.let { txDp += it }
            interpScalar(track.stops, p) { it.tyDpDelta }?.let { tyDp += it }
            interpScalar(track.stops, p) { it.txSelfDelta }?.let { txSelf += it }
            interpScalar(track.stops, p) { it.tySelfDelta }?.let { tySelf += it }
            interpScalar(track.stops, p) { it.scaleX }?.let { sx *= it }
            interpScalar(track.stops, p) { it.scaleY }?.let { sy *= it }
            interpScalar(track.stops, p) { it.rotateDeg }?.let { rot += it }
            interpScalar(track.stops, p) { it.opacity }?.let { opacity = (opacity ?: 1f) * it }
        }
        return EvaluatedVisualAnim(txDp, tyDp, txSelf, tySelf, sx, sy, rot, opacity?.coerceIn(0f, 1f))
    }

    private fun extractAnimationNamesFromBody(body: String): List<String> {
        val skipKeywords = setOf(
            "none", "initial", "inherit", "unset", "linear", "ease", "ease-in", "ease-out", "ease-in-out",
            "step-start", "step-end", "infinite", "normal", "reverse", "alternate", "alternate-reverse",
            "forwards", "backwards", "both", "running", "paused",
        )
        val out = ArrayList<String>()
        for (decl in splitCssDeclarations(body)) {
            val colon = decl.indexOf(':')
            if (colon <= 0) continue
            val prop = decl.substring(0, colon).trim().lowercase()
            if (prop != "animation" && prop != "animation-name" &&
                prop != "-webkit-animation" && prop != "-webkit-animation-name"
            ) {
                continue
            }
            val value = decl.substring(colon + 1).trim()
            for (part in splitTopLevel(value, onWhitespace = false)) {
                for (tok in splitTopLevel(part, onWhitespace = true)) {
                    val t = tok.trim().trim('"', '\'')
                    val low = t.lowercase()
                    if (t.isEmpty() || low in skipKeywords) continue
                    if (low.contains('(')) continue
                    if (Regex("""^-?\d*\.?\d+(m?s)?$""").matches(low)) continue
                    out.add(t)
                }
            }
        }
        return out
    }

    private fun cascadeCommunityCss(cssText: String, isDarkTheme: Boolean): CascadedCommunityCss {
        val noComments = cssText.replace(CSS_COMMENT_RE, "")
        val ast = stripCssAtBlocks(noComments)
        var urlCounter = 0
        val neutralBase = StringBuilder()
        val themedBase = StringBuilder()
        val neutralBefore = StringBuilder()
        val themedBefore = StringBuilder()
        val neutralAfter = StringBuilder()
        val themedAfter = StringBuilder()
        val darkBodies = StringBuilder()
        var darkImageCount = 0
        var hasStandaloneDarkRule = false

        val darkSelRe = Regex("""data-theme\s*=\s*["']?dark["']?""", RegexOption.IGNORE_CASE)
        val lightSelRe = Regex("""data-theme\s*=\s*["']?light["']?""", RegexOption.IGNORE_CASE)
        val beforeSelRe = Regex("""(?<!:)::?before\b""", RegexOption.IGNORE_CASE)
        val afterSelRe = Regex("""(?<!:)::?after\b""", RegexOption.IGNORE_CASE)

        for ((selector, body) in ast.rules) {
            var ruleDarkImgs = 0
            val replaced = CSS_URL_TOKEN_RE.replace(body) { m ->
                val raw = (m.groupValues[1].ifEmpty { m.groupValues[2].ifEmpty { m.groupValues[3] } }).trim()
                if (isValidCssImageUrl(raw)) {
                    ruleDarkImgs++
                    "__URL_${urlCounter++}__"
                } else {
                    m.value
                }
            }
            val subSelectors = splitTopLevel(selector, onWhitespace = false)
            var ruleMatchedDarkSection = false
            for (subRaw in subSelectors) {
                val sub = subRaw.trim()
                if (sub.isEmpty()) continue
                val low = sub.lowercase()
                if (low.contains(":hover") || low.contains(":active") || low.contains(":focus")) continue
                val isNotDark = low.contains(":not([data-theme=\"dark\"])") || low.contains(":not([data-theme='dark'])")
                val isDarkSel = !isNotDark && darkSelRe.containsMatchIn(low)
                val isLightSel = isNotDark || lightSelRe.containsMatchIn(low)
                if (isDarkSel) {
                    hasStandaloneDarkRule = true
                    ruleMatchedDarkSection = true
                }
                val applies = when {
                    isDarkSel -> isDarkTheme
                    isLightSel -> !isDarkTheme
                    else -> true
                }
                if (!applies) continue
                val isThemedTier = isDarkSel || isLightSel
                when {
                    beforeSelRe.containsMatchIn(sub) ->
                        (if (isThemedTier) themedBefore else neutralBefore).append(replaced).append(';')
                    afterSelRe.containsMatchIn(sub) ->
                        (if (isThemedTier) themedAfter else neutralAfter).append(replaced).append(';')
                    else ->
                        (if (isThemedTier) themedBase else neutralBase).append(replaced).append(';')
                }
            }
            if (ruleMatchedDarkSection) {
                darkImageCount += ruleDarkImgs
                darkBodies.append(replaced).append(';')
            }
        }

        val baseStr = buildString {
            append(neutralBase)
            if (themedBase.isNotEmpty()) {
                if (isNotEmpty()) append(';')
                append(themedBase)
            }
        }
        val beforeStr = buildString {
            append(neutralBefore)
            if (themedBefore.isNotEmpty()) {
                if (isNotEmpty()) append(';')
                append(themedBefore)
            }
        }
        val afterStr = buildString {
            append(neutralAfter)
            if (themedAfter.isNotEmpty()) {
                if (isNotEmpty()) append(';')
                append(themedAfter)
            }
        }
        val darkStr = darkBodies.toString()

        val kfOverrideVars = LinkedHashMap<String, String>()
        if (isDarkTheme && !hasStandaloneDarkRule && ast.keyframeMidVars.isNotEmpty()) {
            val animNames = extractAnimationNamesFromBody("$baseStr;$beforeStr;$afterStr")
            for (animName in animNames) {
                val kfMap = ast.keyframeMidVars.entries
                    .firstOrNull { it.key.equals(animName, ignoreCase = true) }
                    ?.value
                if (kfMap != null) {
                    kfOverrideVars.putAll(kfMap)
                }
            }
        }

        val baseVars = buildResolvedVars(ast.initialVars, baseStr, kfOverrideVars)
        val beforeVars = buildResolvedVars(baseVars, beforeStr)
        val afterVars = buildResolvedVars(baseVars, afterStr)
        val keyframesReplaced = LinkedHashMap<String, List<Pair<Float, String>>>(ast.keyframeStops.size)
        for ((kfName, stops) in ast.keyframeStops) {
            val outStops = stops.map { (frac, stopBody) ->
                val replacedStop = CSS_URL_TOKEN_RE.replace(stopBody) { m ->
                    val raw = (m.groupValues[1].ifEmpty { m.groupValues[2].ifEmpty { m.groupValues[3] } }).trim()
                    if (isValidCssImageUrl(raw)) "__URL_${urlCounter++}__" else m.value
                }
                frac to replacedStop
            }
            keyframesReplaced[kfName] = outStops
        }
        return CascadedCommunityCss(
            baseStr = baseStr,
            beforeStr = beforeStr,
            afterStr = afterStr,
            darkStr = darkStr,
            urlCounter = urlCounter,
            darkImageCount = darkImageCount,
            hasStandaloneDarkRule = hasStandaloneDarkRule,
            baseVars = baseVars,
            beforeVars = beforeVars,
            afterVars = afterVars,
            keyframesReplaced = keyframesReplaced,
        )
    }

    private fun parsePseudoRule(
        body: String,
        vars: Map<String, String>,
        parentBorderW: List<Float> = listOf(0f, 0f, 0f, 0f),
        emPx: Float = 15f,
        allowZeroOpacity: Boolean = false,
    ): ParsedPseudoLayer? {
        if (body.isBlank()) return null
        val compactBody = body.replace(Regex("""\s+"""), " ").lowercase()
        val hasAbsPos = Regex("""position\s*:\s*(absolute|fixed)""").containsMatchIn(compactBody)
        val hasInsetCoord = Regex("""(?:^|[; ])(?:top|right|bottom|left|inset|inset-inline-start|inset-inline-end|inset-block-start|inset-block-end)\s*:""").containsMatchIn(compactBody)
        val hasBlockDisplay = Regex("""display\s*:\s*(?:inline-)?(?:block|flex)""").containsMatchIn(compactBody) ||
            compactBody.contains("flex:") ||
            compactBody.contains("flex-shrink:")
        if (hasBlockDisplay && !hasAbsPos && !hasInsetCoord) return null
        val urlTokenRe = Regex("""__URL_(\d+)__""")
        var topDp: Float? = null
        var rightDp: Float? = null
        var bottomDp: Float? = null
        var leftDp: Float? = null
        var widthDp: Float? = null
        var heightDp: Float? = null
        var heightCapToParent = false
        var aspectRatio: Float? = null
        var topPercent50 = false
        var leftPercent50 = false
        var calcTopCenterOffsetDp: Float? = null
        var calcLeftCenterOffsetDp: Float? = null
        var marginTopDp = 0f
        var marginRightDp = 0f
        var marginBottomDp = 0f
        var marginLeftDp = 0f
        var translateSelfX = 0f
        var translateSelfY = 0f
        var translatePxX = 0f
        var translatePxY = 0f
        var hasTranslateX = false
        var hasTranslateY = false
        var scaleX = 1f
        var scaleY = 1f
        var rotateDeg = 0f
        var opacityVal = 1f

        var isDisplayHidden = false
        var isMaskHidden = false
        var isOpaqueHidden = false

        var shapeFillColor: Color? = null
        var shapeBorderColor: Color? = null
        var shapeBorderWidthDp = 0f
        var isCircle = false
        var shadowDot: PseudoBoxShadowDot? = null

        var nsUrlIdx: Int? = null
        var nsSlice: List<Float>? = null
        var nsPercent = false
        var nsFill = false
        var nsBorderW: List<Float>? = null
        var nsOutset: List<Float>? = null

        var bgUrlIndices: List<Int> = emptyList()
        var bgSizes: List<CssBgSizeSpec> = emptyList()
        var bgPositions: List<CssBgPosSpec> = emptyList()

        for (decl in splitCssDeclarations(body)) {
            val colon = decl.indexOf(':')
            if (colon <= 0) continue
            val prop = decl.substring(0, colon).trim().lowercase()
            if (prop.startsWith("--")) continue
            val rawVal = simplifyColorMix(
                evalCssMathTokens(
                    resolveCssVars(decl.substring(colon + 1).trim(), vars)
                        .replace("!important", "", ignoreCase = true)
                        .trim(),
                    emPx,
                ),
            )
            when (prop) {
                "display" -> isDisplayHidden = rawVal.equals("none", ignoreCase = true)
                "visibility" -> isDisplayHidden = rawVal.equals("hidden", ignoreCase = true) || rawVal.equals("collapse", ignoreCase = true)
                "opacity" -> {
                    val op = rawVal.toFloatOrNull() ?: parsePxLength(rawVal, emPx)
                    if (op != null) {
                        opacityVal = op.coerceIn(0f, 1f)
                        isOpaqueHidden = !allowZeroOpacity && op <= 0.01f
                    }
                }
                "mask-image", "-webkit-mask-image", "mask", "-webkit-mask" -> {
                    isMaskHidden = isMaskCompletelyTransparent(rawVal)
                }
                "inset" -> {
                    if (!rawVal.contains('%')) {
                        expandBoxFour(parseBoxFourNumbers(rawVal, emPx))?.let { b ->
                            topDp = b[0]
                            rightDp = b[1]
                            bottomDp = b[2]
                            leftDp = b[3]
                        }
                    }
                }
                "top", "inset-block-start" -> {
                    val compactVal = rawVal.replace(" ", "").lowercase()
                    val calcMatch = Regex("""^calc\(50%([+-].+)\)$""").matchEntire(compactVal)
                    when {
                        compactVal == "50%" -> topPercent50 = true
                        calcMatch != null -> {
                            val delta = parsePxLength(calcMatch.groupValues[1], emPx)
                            if (delta != null) calcTopCenterOffsetDp = delta
                        }
                        else -> parsePxLength(rawVal, emPx)?.let { topDp = it }
                    }
                }
                "right", "inset-inline-end" -> parsePxLength(rawVal, emPx)?.let { rightDp = it }
                "bottom", "inset-block-end" -> parsePxLength(rawVal, emPx)?.let { bottomDp = it }
                "left", "inset-inline-start" -> {
                    val compactVal = rawVal.replace(" ", "").lowercase()
                    val calcMatch = Regex("""^calc\(50%([+-].+)\)$""").matchEntire(compactVal)
                    when {
                        compactVal == "50%" -> leftPercent50 = true
                        calcMatch != null -> {
                            val delta = parsePxLength(calcMatch.groupValues[1], emPx)
                            if (delta != null) calcLeftCenterOffsetDp = delta
                        }
                        else -> parsePxLength(rawVal, emPx)?.let { leftDp = it }
                    }
                }
                "margin-top", "margin-block-start" -> parsePxLength(rawVal, emPx)?.let { marginTopDp = it }
                "margin-right", "margin-inline-end" -> parsePxLength(rawVal, emPx)?.let { marginRightDp = it }
                "margin-bottom", "margin-block-end" -> parsePxLength(rawVal, emPx)?.let { marginBottomDp = it }
                "margin-left", "margin-inline-start" -> parsePxLength(rawVal, emPx)?.let { marginLeftDp = it }
                "margin" -> {
                    expandBoxFour(parseBoxFourNumbers(rawVal, emPx))?.let { m ->
                        marginTopDp = m[0]
                        marginRightDp = m[1]
                        marginBottomDp = m[2]
                        marginLeftDp = m[3]
                    }
                }
                "transform", "-webkit-transform" -> {
                    val tf = parseTransformDecls(rawVal, emPx)
                    if (tf.hasTx) {
                        translateSelfX = tf.txSelf
                        translatePxX = tf.txDp
                        hasTranslateX = true
                    }
                    if (tf.hasTy) {
                        translateSelfY = tf.tySelf
                        translatePxY = tf.tyDp
                        hasTranslateY = true
                    }
                    if (tf.hasScaleX) scaleX = tf.scaleX
                    if (tf.hasScaleY) scaleY = tf.scaleY
                    if (tf.hasRotate) rotateDeg = tf.rotateDeg
                }
                "width", "inline-size" -> parseLengthWithParentCap(rawVal, emPx).first?.let { widthDp = it }
                "height", "block-size" -> {
                    val (hVal, hCap) = parseLengthWithParentCap(rawVal, emPx)
                    if (hVal != null) {
                        heightDp = hVal
                        heightCapToParent = hCap
                    }
                }
                "aspect-ratio" -> {
                    val slashParts = rawVal.split('/').mapNotNull { it.trim().toFloatOrNull() }
                    aspectRatio = when {
                        slashParts.size >= 2 && slashParts[1] > 0f -> slashParts[0] / slashParts[1]
                        slashParts.size == 1 && slashParts[0] > 0f -> slashParts[0]
                        else -> aspectRatio
                    }
                }
                "border-radius" -> {
                    if (rawVal.contains("50%") || (parseBoxFourNumbers(rawVal, emPx).firstOrNull() ?: 0f) >= 20f) {
                        isCircle = true
                    }
                }
                "border", "border-color", "border-width" -> {
                    CSS_COLOR_TOKEN_RE.find(rawVal.lowercase())?.let { m ->
                        parseCssColor(m.value)?.takeIf { it != Color.Transparent }?.let { shapeBorderColor = it }
                    }
                    if (prop == "border" || prop == "border-width") {
                        val widthOnly = CSS_COLOR_TOKEN_RE.replace(rawVal.lowercase(), "")
                        parseBoxFourNumbers(widthOnly, emPx).firstOrNull()?.let { shapeBorderWidthDp = it }
                    }
                }
                "box-shadow" -> {
                    if (!rawVal.contains("inset", ignoreCase = true)) {
                        val m = Regex(
                            """(-?\d*\.?\d+)(?:px)?\s+(-?\d*\.?\d+)(?:px)?\s+(?:\d*\.?\d+(?:px)?\s+)?(-?\d*\.?\d+)(?:px)?\s+(#[0-9a-fA-F]{3,8}|(?:rgba?|hsla?)\([^)]+\))""",
                            RegexOption.IGNORE_CASE,
                        ).find(rawVal)
                        if (m != null) {
                            val dx = m.groupValues[1].toFloatOrNull() ?: 0f
                            val dy = m.groupValues[2].toFloatOrNull() ?: 0f
                            val spread = m.groupValues[3].toFloatOrNull() ?: 0f
                            parseCssColor(m.groupValues[4].lowercase())?.let { col ->
                                shadowDot = PseudoBoxShadowDot(dx, dy, spread, col)
                            }
                        }
                    }
                }
                "border-image" -> {
                    val bi = parseBorderImageShorthand(rawVal, emPx)
                    when {
                        bi.clear -> {
                            nsUrlIdx = null
                            nsSlice = null
                            nsBorderW = null
                            nsOutset = null
                            nsFill = false
                        }
                        else -> {
                            if (bi.inheritSource) nsUrlIdx = -1
                            else if (bi.urlIdx != null) nsUrlIdx = bi.urlIdx
                            if (bi.slice != null) {
                                nsSlice = bi.slice
                                nsPercent = rawVal.contains('%')
                            }
                            if (bi.fill) nsFill = true
                            if (bi.width != null) nsBorderW = bi.width
                            if (bi.outset != null) nsOutset = bi.outset
                        }
                    }
                }
                "border-image-source" -> {
                    when {
                        rawVal.equals("inherit", ignoreCase = true) -> nsUrlIdx = -1
                        rawVal.equals("none", ignoreCase = true) -> nsUrlIdx = null
                        else -> urlTokenRe.find(rawVal)?.groupValues?.get(1)?.toIntOrNull()?.let { nsUrlIdx = it }
                    }
                }
                "border-image-slice" -> {
                    nsPercent = rawVal.contains('%')
                    nsFill = rawVal.contains("fill", ignoreCase = true)
                    expandBoxFour(parseBoxFourNumbers(rawVal, emPx))?.let { nsSlice = it }
                }
                "border-image-width" -> expandBoxFour(parseBoxFourNumbers(rawVal, emPx))?.let { nsBorderW = it }
                "border-image-outset" -> expandBoxFour(parseBoxFourNumbers(rawVal, emPx))?.let { nsOutset = it }
                "background" -> {
                    val layers = parseBackgroundShorthand(rawVal, emPx)
                    val urlLayers = layers.filter { it.urlIdx != null }
                    if (urlLayers.isNotEmpty()) {
                        bgUrlIndices = urlLayers.mapNotNull { it.urlIdx }
                        if (urlLayers.any { !it.bgSizeRaw.isNullOrBlank() }) {
                            bgSizes = urlLayers.map { parseCssBgSizeLayer(it.bgSizeRaw.orEmpty(), emPx) }
                        }
                        if (urlLayers.any { !it.bgPosRaw.isNullOrBlank() }) {
                            bgPositions = urlLayers.map { parseCssBgPosLayer(it.bgPosRaw.orEmpty(), emPx) }
                        }
                    } else {
                        bgUrlIndices = emptyList()
                    }
                    layers.asReversed().firstOrNull { !it.bgColorRaw.isNullOrBlank() }?.bgColorRaw?.let { cStr ->
                        parseCssColor(cStr)?.takeIf { it != Color.Transparent }?.let { shapeFillColor = it }
                    }
                }
                "background-image" -> {
                    if (rawVal.equals("none", ignoreCase = true)) {
                        bgUrlIndices = emptyList()
                    } else {
                        val found = urlTokenRe.findAll(rawVal).mapNotNull { it.groupValues[1].toIntOrNull() }.toList()
                        if (found.isNotEmpty()) bgUrlIndices = found
                    }
                }
                "background-color" -> {
                    CSS_COLOR_TOKEN_RE.findAll(rawVal.lowercase()).lastOrNull()?.let { m ->
                        parseCssColor(m.value)?.takeIf { it != Color.Transparent }?.let { shapeFillColor = it }
                    } ?: parseCssColor(rawVal)?.takeIf { it != Color.Transparent }?.let { shapeFillColor = it }
                }
                "background-size" -> {
                    bgSizes = splitTopLevel(rawVal, onWhitespace = false).map { parseCssBgSizeLayer(it, emPx) }
                }
                "background-position" -> {
                    bgPositions = splitTopLevel(rawVal, onWhitespace = false).map { parseCssBgPosLayer(it, emPx) }
                }
            }
        }

        if (isDisplayHidden || isMaskHidden || isOpaqueHidden) return null

        // Derive missing dimension from aspect-ratio if only one side is specified
        val effAr = aspectRatio
        if (effAr != null && effAr > 0f) {
            when {
                heightDp != null && widthDp == null -> widthDp = heightDp!! * effAr
                widthDp != null && heightDp == null -> heightDp = widthDp!! / effAr
            }
        }

        // Convert CSS padding-box offsets into parent border-box coordinates and apply margins/translates
        val bwTop = parentBorderW.getOrElse(0) { 0f }
        val bwRight = parentBorderW.getOrElse(1) { 0f }
        val bwBottom = parentBorderW.getOrElse(2) { 0f }
        val bwLeft = parentBorderW.getOrElse(3) { 0f }
        if (topDp != null) topDp = topDp!! + bwTop + marginTopDp
        if (rightDp != null) rightDp = rightDp!! + bwRight + marginRightDp
        if (bottomDp != null) bottomDp = bottomDp!! + bwBottom + marginBottomDp
        if (leftDp != null) leftDp = leftDp!! + bwLeft + marginLeftDp

        var centerX = false
        var centerY = false
        var offsetXDp = (if (leftDp == null && rightDp == null) marginLeftDp else 0f) + translatePxX
        var offsetYDp = (if (topDp == null && bottomDp == null) marginTopDp else 0f) + translatePxY
        if (topPercent50) {
            centerY = true
            offsetYDp += marginTopDp
            if (!hasTranslateY) {
                offsetYDp += (heightDp ?: 0f) * 0.5f
            }
        } else if (calcTopCenterOffsetDp != null) {
            centerY = true
            offsetYDp += calcTopCenterOffsetDp!! + marginTopDp + (if (hasTranslateY) 0f else (heightDp ?: 0f) * 0.5f)
        }
        if (leftPercent50) {
            centerX = true
            offsetXDp += marginLeftDp
            if (!hasTranslateX) {
                offsetXDp += (widthDp ?: 0f) * 0.5f
            }
        } else if (calcLeftCenterOffsetDp != null) {
            centerX = true
            offsetXDp += calcLeftCenterOffsetDp!! + marginLeftDp + (if (hasTranslateX) 0f else (widthDp ?: 0f) * 0.5f)
        }

        val effNsSlice = nsSlice ?: if (nsUrlIdx != null && (nsFill || (nsBorderW != null && nsBorderW!!.all { it <= 0f }))) {
            listOf(0f, 0f, 0f, 0f)
        } else {
            null
        }
        val nineSlice = if (nsUrlIdx != null && effNsSlice != null &&
            ((nsBorderW == null || nsBorderW!!.any { it > 0f }) || nsFill || effNsSlice.all { it == 0f })
        ) {
            val bw = nsBorderW ?: listOf(0f, 0f, 0f, 0f)
            val out = nsOutset ?: listOf(0f, 0f, 0f, 0f)
            ParsedPseudoNineSlice(
                urlIdx = nsUrlIdx!!,
                sliceTop = effNsSlice[0], sliceRight = effNsSlice[1], sliceBottom = effNsSlice[2], sliceLeft = effNsSlice[3],
                isPercentSlice = nsPercent,
                borderTopDp = bw[0], borderRightDp = bw[1], borderBottomDp = bw[2], borderLeftDp = bw[3],
                outsetTopDp = out[0], outsetRightDp = out[1], outsetBottomDp = out[2], outsetLeftDp = out[3],
            )
        } else {
            null
        }

        val bgBottomToTop = if (bgUrlIndices.isNotEmpty()) {
            bgUrlIndices.mapIndexed { idx, uIdx ->
                val sz = if (bgSizes.isNotEmpty()) bgSizes[idx % bgSizes.size] else CssBgSizeSpec()
                val ps = if (bgPositions.isNotEmpty()) bgPositions[idx % bgPositions.size] else CssBgPosSpec()
                ParsedPseudoBgImage(
                    urlIdx = uIdx,
                    widthDp = sz.widthDp,
                    widthFraction = sz.widthFraction,
                    widthSubFromFullDp = sz.widthSubFromFullDp,
                    heightDp = sz.heightDp,
                    heightFraction = sz.heightFraction,
                    alignCenterX = ps.alignCenterX,
                    offsetXDp = ps.offsetXDp,
                    offsetRightDp = ps.offsetRightDp,
                    alignCenterY = ps.alignCenterY,
                    offsetYDp = ps.offsetYDp,
                    offsetBottomDp = ps.offsetBottomDp,
                    scaleMode = sz.scaleMode,
                )
            }.reversed()
        } else {
            emptyList()
        }

        val hasPureShape = shapeFillColor != null || (shapeBorderColor != null && shapeBorderWidthDp > 0f) || shadowDot != null
        if (nineSlice == null && bgBottomToTop.isEmpty() && !hasPureShape) return null
        return ParsedPseudoLayer(
            topDp = topDp,
            rightDp = rightDp,
            bottomDp = bottomDp,
            leftDp = leftDp,
            widthDp = widthDp,
            heightDp = heightDp,
            heightCapToParent = heightCapToParent,
            aspectRatio = aspectRatio,
            translateSelfX = translateSelfX,
            translateSelfY = translateSelfY,
            centerX = centerX,
            centerY = centerY,
            offsetXDp = offsetXDp,
            offsetYDp = offsetYDp,
            shapeFillColor = shapeFillColor,
            shapeBorderColor = shapeBorderColor,
            shapeBorderWidthDp = shapeBorderWidthDp,
            isCircle = isCircle,
            shadowDot = shadowDot,
            bgImagesBottomToTop = bgBottomToTop,
            nineSlice = nineSlice,
            opacity = opacityVal,
            scaleX = scaleX,
            scaleY = scaleY,
            rotateDeg = rotateDeg,
        )
    }

    private fun extractElementBaseTransformAndOpacity(
        body: String,
        vars: Map<String, String>,
        emPx: Float,
    ): Pair<ParsedCssTransform, Float> {
        var tf = ParsedCssTransform()
        var op = 1f
        for (decl in splitCssDeclarations(body)) {
            val colon = decl.indexOf(':')
            if (colon <= 0) continue
            val prop = decl.substring(0, colon).trim().lowercase()
            val rv = resolveCssVars(decl.substring(colon + 1).trim(), vars)
                .replace("!important", "", ignoreCase = true)
                .trim()
            when (prop) {
                "transform", "-webkit-transform" -> tf = parseTransformDecls(rv, emPx)
                "opacity" -> rv.toFloatOrNull()?.let { op = it.coerceIn(0f, 1f) }
            }
        }
        return tf to op
    }

    private fun parsePseudoRuleAnimated(
        body: String,
        vars: Map<String, String>,
        keyframes: Map<String, List<Pair<Float, String>>>,
        parentBorderW: List<Float>,
        emPx: Float,
        includeAnimations: Boolean = true,
    ): ParsedPseudoLayer? {
        val rest = parsePseudoRule(body, vars, parentBorderW, emPx, allowZeroOpacity = false)
        if (!includeAnimations || keyframes.isEmpty() || body.isBlank()) return rest
        val (baseTf, baseOp) = extractElementBaseTransformAndOpacity(body, vars, emPx)
        val analysis = analyzeElementAnimations(body, vars, keyframes, baseTf, baseOp, emPx)
        val frames = if (analysis.timelineAnims.isNotEmpty() && analysis.periodMs > 0L) {
            val out = ArrayList<ParsedPseudoLayer>(analysis.sampleCount)
            for (i in 0 until analysis.sampleCount) {
                val elapsed = ((i + 0.5) * analysis.periodMs / analysis.sampleCount).roundToInt().toLong()
                val (ovVars, ovDecls) = sampleKeyframeFrameOverrides(analysis.timelineAnims, keyframes, body, vars, elapsed, emPx)
                val fVars = if (ovVars.isEmpty()) vars else buildResolvedVars(vars, body, ovVars)
                val fBody = if (ovDecls.isBlank()) body else "$body;$ovDecls"
                val fLayer = parsePseudoRule(fBody, fVars, parentBorderW, emPx, allowZeroOpacity = false)
                    ?: ParsedPseudoLayer(opacity = 0f)
                out.add(fLayer)
            }
            if (out.any { it.opacity > 0.01f || it.nineSlice != null || it.bgImagesBottomToTop.isNotEmpty() || it.shapeFillColor != null || it.shadowDot != null }) {
                out
            } else {
                emptyList()
            }
        } else {
            emptyList()
        }
        if (rest == null && frames.isEmpty() && !analysis.animatesOpacityToVisible) return null
        val targetBase = rest
            ?: parsePseudoRule(body, vars, parentBorderW, emPx, allowZeroOpacity = true)
            ?: ParsedPseudoLayer(opacity = 0f)
        return targetBase.copy(
            visualAnim = analysis.visualTracks,
            timelineFrames = frames,
            timelinePeriodMs = if (frames.isNotEmpty()) analysis.periodMs else 0L,
        )
    }

    private fun applyPaddingDecl(prop: String, rawVal: String, curPad: MutableList<Float>): Boolean {
        return when (prop) {
            "padding" -> {
                expandBoxFour(parseBoxFourNumbers(rawVal))?.let {
                    for (i in 0..3) curPad[i] = it[i]
                }
                true
            }
            "padding-top", "padding-block-start" -> {
                parseBoxFourNumbers(rawVal).firstOrNull()?.let { curPad[0] = it }
                true
            }
            "padding-right", "padding-inline-end" -> {
                parseBoxFourNumbers(rawVal).firstOrNull()?.let { curPad[1] = it }
                true
            }
            "padding-bottom", "padding-block-end" -> {
                parseBoxFourNumbers(rawVal).firstOrNull()?.let { curPad[2] = it }
                true
            }
            "padding-left", "padding-inline-start" -> {
                parseBoxFourNumbers(rawVal).firstOrNull()?.let { curPad[3] = it }
                true
            }
            "padding-inline" -> {
                val nums = parseBoxFourNumbers(rawVal)
                if (nums.isNotEmpty()) {
                    curPad[3] = nums[0]
                    curPad[1] = nums.getOrElse(1) { nums[0] }
                }
                true
            }
            "padding-block" -> {
                val nums = parseBoxFourNumbers(rawVal)
                if (nums.isNotEmpty()) {
                    curPad[0] = nums[0]
                    curPad[2] = nums.getOrElse(1) { nums[0] }
                }
                true
            }
            else -> false
        }
    }

    fun parseCommunityBubbleCss(cssText: String, isDarkTheme: Boolean = false): ParsedCommunityBubbleCss? {
        if (cssText.isBlank()) return null
        val part = cascadeCommunityCss(cssText, isDarkTheme)
        val emPx = 15f
        val staticParsed = parseCommunityBubbleCssFrame(
            part = part,
            baseBody = part.baseStr,
            baseVars = part.baseVars,
            beforeVars = part.beforeVars,
            afterVars = part.afterVars,
            emPx = emPx,
            includePseudoAnimations = true,
        ) ?: return null
        val (baseTf, baseOp) = extractElementBaseTransformAndOpacity(part.baseStr, part.baseVars, emPx)
        val anim = analyzeElementAnimations(part.baseStr, part.baseVars, part.keyframesReplaced, baseTf, baseOp, emPx)
        val timelineFrames = if (anim.timelineAnims.isNotEmpty() && anim.periodMs > 0L) {
            val frames = ArrayList<ParsedCommunityBubbleCss>(anim.sampleCount)
            for (i in 0 until anim.sampleCount) {
                val elapsed = ((i + 0.5) * anim.periodMs / anim.sampleCount).roundToInt().toLong()
                val (ovVars, ovDecls) = sampleKeyframeFrameOverrides(anim.timelineAnims, part.keyframesReplaced, part.baseStr, part.baseVars, elapsed, emPx)
                val fBaseBody = if (ovDecls.isBlank()) part.baseStr else "${part.baseStr};$ovDecls"
                val fBaseVars = if (ovVars.isEmpty()) part.baseVars else buildResolvedVars(part.baseVars, part.baseStr, ovVars)
                val fBeforeVars = if (ovVars.isEmpty()) part.beforeVars else buildResolvedVars(fBaseVars, part.beforeStr)
                val fAfterVars = if (ovVars.isEmpty()) part.afterVars else buildResolvedVars(fBaseVars, part.afterStr)
                parseCommunityBubbleCssFrame(
                    part = part,
                    baseBody = fBaseBody,
                    baseVars = fBaseVars,
                    beforeVars = fBeforeVars,
                    afterVars = fAfterVars,
                    emPx = emPx,
                    includePseudoAnimations = false,
                )?.let { frames.add(it) }
            }
            frames
        } else {
            emptyList()
        }
        return staticParsed.copy(
            visualAnim = anim.visualTracks,
            timelineFrames = timelineFrames,
            timelinePeriodMs = if (timelineFrames.isNotEmpty()) anim.periodMs else 0L,
        )
    }

    private fun parseCommunityBubbleCssFrame(
        part: CascadedCommunityCss,
        baseBody: String,
        baseVars: Map<String, String>,
        beforeVars: Map<String, String>,
        afterVars: Map<String, String>,
        emPx: Float,
        includePseudoAnimations: Boolean,
    ): ParsedCommunityBubbleCss? {
        val urlTokenRe = Regex("""__URL_(\d+)__""")

        var baseUrlIdx = 0
        var hasBaseImageUrl = false
        var sliceArr: List<Float>? = null
        var slicePercent = false
        var sliceHasFill = false
        var borderImageW: List<Float>? = null
        var borderW: List<Float>? = null
        val curPad = mutableListOf(0f, 0f, 0f, 0f)
        var hasPad = false
        var minWidth = 72f
        var minHeightDp = 0f
        var startOverflowReserve = 0f
        var endOverflowReserve = 0f
        var topOverflowReserve = 0f
        var bottomOverflowReserve = 0f
        var textCol: Color? = null
        var bgCol: Color? = null
        var bgGrad: List<Color>? = null
        var translucentBgGrad: List<Color>? = null
        var bgAngleDeg: Float? = null
        var bdCol: Color? = null
        var borderGradCols: List<Color>? = null
        var radii: List<Float>? = null
        val baseBgImageLayers = ArrayList<ParsedPseudoBgImage>()
        val safeDeclBuf = StringBuilder()

        fun extractGradientColors(gradStr: String): Pair<List<Color>, Float?> {
            val cols = CSS_COLOR_TOKEN_RE.findAll(gradStr.lowercase()).mapNotNull { parseCssColor(it.value) }.toList()
            val angle = Regex("""(-?\d+(?:\.\d+)?)deg""").find(gradStr.lowercase())?.groupValues?.get(1)?.toFloatOrNull()
            return cols to angle
        }

        fun applyBackgroundDecl(prop: String, rawVal: String) {
            if (prop == "background-color") {
                val cleanedColor = rawVal.replace(urlTokenRe, "").trim()
                parseCssColor(cleanedColor)?.takeIf { it.alpha > 0.05f }?.let { bgCol = it }
                return
            }
            val layers = parseBackgroundShorthand(rawVal, emPx)
            val urlLayers = layers.filter { it.urlIdx != null }
            if (urlLayers.isNotEmpty()) {
                if (!hasBaseImageUrl) baseUrlIdx = urlLayers.first().urlIdx!!
                hasBaseImageUrl = true
                baseBgImageLayers.clear()
                urlLayers.forEach { l ->
                    val sz = parseCssBgSizeLayer(l.bgSizeRaw.orEmpty(), emPx)
                    val ps = parseCssBgPosLayer(l.bgPosRaw.orEmpty(), emPx)
                    baseBgImageLayers.add(
                        ParsedPseudoBgImage(
                            urlIdx = l.urlIdx!!,
                            widthDp = sz.widthDp,
                            widthFraction = sz.widthFraction,
                            widthSubFromFullDp = sz.widthSubFromFullDp,
                            heightDp = sz.heightDp,
                            heightFraction = sz.heightFraction,
                            alignCenterX = ps.alignCenterX,
                            offsetXDp = ps.offsetXDp,
                            offsetRightDp = ps.offsetRightDp,
                            alignCenterY = ps.alignCenterY,
                            offsetYDp = ps.offsetYDp,
                            offsetBottomDp = ps.offsetBottomDp,
                            scaleMode = sz.scaleMode,
                        ),
                    )
                }
            }
            for (layer in layers.asReversed()) {
                layer.bgColorRaw?.let { cStr ->
                    parseCssColor(cStr)?.takeIf { it.alpha > 0.05f }?.let { bgCol = it }
                }
                val grad = layer.gradientRaw ?: continue
                val (cols, angle) = extractGradientColors(grad)
                if (cols.size >= 2) {
                    if (layer.originClipBox == "border-box") {
                        borderGradCols = cols
                    } else {
                        val isOpaqueGrad = cols.count { it.alpha >= 0.8f } >= 2
                        val isSmallTrack = layer.bgSizeRaw != null && Regex("""\b(\d+(?:\.\d+)?)px\b""").findAll(layer.bgSizeRaw)
                            .any { (it.groupValues[1].toFloatOrNull() ?: 99f) < 12f }
                        if (isOpaqueGrad && !isSmallTrack) {
                            bgGrad = cols
                            bgAngleDeg = angle
                        } else if (translucentBgGrad == null && !isSmallTrack) {
                            translucentBgGrad = cols
                        }
                    }
                }
            }
            if (layers.isEmpty() && prop == "background-image" && rawVal.contains("gradient(", ignoreCase = true)) {
                val (cols, angle) = extractGradientColors(rawVal)
                if (cols.size >= 2) {
                    if (cols.count { it.alpha >= 0.8f } >= 2) {
                        bgGrad = cols
                        bgAngleDeg = angle
                    } else if (translucentBgGrad == null) {
                        translucentBgGrad = cols
                    }
                }
            }
        }

        for (decl in splitCssDeclarations(baseBody)) {
            val colon = decl.indexOf(':')
            if (colon <= 0) continue
            val prop = decl.substring(0, colon).trim().lowercase()
            if (prop.startsWith("--")) continue
            val rawVal = simplifyColorMix(
                evalCssMathTokens(
                    resolveCssVars(decl.substring(colon + 1).trim(), baseVars)
                        .replace("!important", "", ignoreCase = true)
                        .trim(),
                    emPx,
                ),
            )
            if (applyPaddingDecl(prop, rawVal, curPad)) {
                hasPad = true
                continue
            }
            when (prop) {
                "border-image" -> {
                    val bi = parseBorderImageShorthand(rawVal, emPx)
                    when {
                        bi.clear -> {
                            sliceArr = null
                            borderImageW = null
                            sliceHasFill = false
                        }
                        else -> {
                            bi.urlIdx?.let {
                                baseUrlIdx = it
                                hasBaseImageUrl = true
                            }
                            if (bi.slice != null) {
                                sliceArr = bi.slice
                                slicePercent = rawVal.contains('%')
                            }
                            if (bi.fill) sliceHasFill = true
                            if (bi.width != null) borderImageW = bi.width
                            if (bi.outset != null) {
                                safeDeclBuf.append("__OUTSET__=").append(bi.outset.joinToString(",")).append("; ")
                            }
                        }
                    }
                }
                "border-image-source" -> {
                    if (rawVal.equals("none", ignoreCase = true)) {
                        hasBaseImageUrl = false
                    } else {
                        urlTokenRe.find(rawVal)?.groupValues?.get(1)?.toIntOrNull()?.let {
                            baseUrlIdx = it
                            hasBaseImageUrl = true
                        }
                    }
                }
                "border-image-slice" -> {
                    slicePercent = rawVal.contains('%')
                    sliceHasFill = rawVal.contains("fill", ignoreCase = true)
                    expandBoxFour(parseBoxFourNumbers(rawVal, emPx))?.let { sliceArr = it }
                }
                "border-image-width" -> expandBoxFour(parseBoxFourNumbers(rawVal, emPx))?.let { borderImageW = it }
                "border-image-outset" -> expandBoxFour(parseBoxFourNumbers(rawVal, emPx))?.let { outsetW ->
                    safeDeclBuf.append("__OUTSET__=").append(outsetW.joinToString(",")).append("; ")
                }
                "border-width", "border", "border-color" -> {
                    if (prop != "border-width") {
                        CSS_COLOR_TOKEN_RE.find(rawVal.lowercase())?.let { m ->
                            parseCssColor(m.value)?.takeIf { it != Color.Transparent }?.let { bdCol = it }
                        } ?: parseCssColor(rawVal)?.takeIf { it != Color.Transparent }?.let { bdCol = it }
                    }
                    if (prop != "border-color") {
                        val widthOnly = CSS_COLOR_TOKEN_RE.replace(rawVal.lowercase(), "")
                        expandBoxFour(parseBoxFourNumbers(widthOnly, emPx))?.let { borderW = it }
                    }
                    safeDeclBuf.append(prop).append(": ").append(rawVal).append("; ")
                }
                "border-radius" -> {
                    expandBoxFour(parseBoxFourNumbers(rawVal, emPx))?.let { radii = it }
                    safeDeclBuf.append("border-radius: ").append(rawVal).append("; ")
                }
                "background", "background-image", "background-color" -> {
                    applyBackgroundDecl(prop, rawVal)
                    safeDeclBuf.append(prop).append(": ").append(rawVal).append("; ")
                }
                "max-width", "max-inline-size" -> {
                    Regex("""calc\(\s*100%\s*-\s*([^)]+)\)""", RegexOption.IGNORE_CASE).find(rawVal)?.let { m ->
                        parsePxLength(m.groupValues[1], emPx)?.let { sub ->
                            endOverflowReserve = max(endOverflowReserve, sub)
                        }
                    }
                }
                "min-height", "min-block-size" -> {
                    parsePxLength(rawVal, emPx)?.takeIf { it > 0f }?.let { minHeightDp = it.coerceIn(0f, 96f) }
                }
                "min-width", "min-inline-size" -> {
                    val nums = Regex("""(\d*\.?\d+)(%|px|r?em|vw|vh)?""").findAll(rawVal)
                        .filter { !it.groupValues[2].equals("%") && !rawVal.contains("max-content") }
                        .mapNotNull { m ->
                            val rawN = m.groupValues[1].toFloatOrNull() ?: return@mapNotNull null
                            val n = when (m.groupValues[2].lowercase()) {
                                "rem" -> rawN * 16f
                                "em" -> rawN * emPx
                                "vw" -> rawN * 3.9f
                                "vh" -> rawN * 8.4f
                                else -> rawN
                            }
                            if (n < 220f) n else null
                        }.toList()
                    if (nums.isNotEmpty()) minWidth = nums.last().coerceIn(48f, 220f)
                }
                "color" -> parseCssColor(rawVal)?.let {
                    textCol = it
                    safeDeclBuf.append("color: ").append(rawVal).append("; ")
                }
                else -> safeDeclBuf.append(prop).append(": ").append(rawVal).append("; ")
            }
        }

        val isBaseNineSliceActive = sliceArr != null &&
            ((borderImageW == null || borderImageW!!.any { it > 0f }) || (sliceHasFill && sliceArr!!.all { it == 0f }))

        val bw = borderW ?: listOf(0f, 0f, 0f, 0f)
        val pseudoLayers = ArrayList<ParsedPseudoLayer>(3)
        if (!isBaseNineSliceActive && baseBgImageLayers.isNotEmpty()) {
            pseudoLayers.add(
                ParsedPseudoLayer(
                    bgImagesBottomToTop = baseBgImageLayers.reversed(),
                ),
            )
        }
        listOf(part.beforeStr to beforeVars, part.afterStr to afterVars).forEach { (pStr, pVars) ->
            parsePseudoRuleAnimated(pStr, pVars, part.keyframesReplaced, bw, emPx, includePseudoAnimations)?.let { layer ->
                pseudoLayers.add(layer)
                layer.leftDp?.takeIf { it < 0f }?.let { startOverflowReserve = max(startOverflowReserve, -it) }
                layer.rightDp?.takeIf { it < 0f }?.let { endOverflowReserve = max(endOverflowReserve, -it) }
                layer.topDp?.takeIf { it < 0f }?.let { topOverflowReserve = max(topOverflowReserve, -it) }
                layer.bottomDp?.takeIf { it < 0f }?.let { bottomOverflowReserve = max(bottomOverflowReserve, -it) }
                layer.nineSlice?.let { ns ->
                    startOverflowReserve = max(startOverflowReserve, ns.outsetLeftDp)
                    endOverflowReserve = max(endOverflowReserve, ns.outsetRightDp)
                    topOverflowReserve = max(topOverflowReserve, ns.outsetTopDp)
                    bottomOverflowReserve = max(bottomOverflowReserve, ns.outsetBottomDp)
                }
            }
        }

        var darkTextCol: Color? = null
        var darkBgCol: Color? = null
        var darkBgGrad: List<Color>? = null
        var darkBdCol: Color? = null
        if (part.darkStr.isNotBlank()) {
            val darkVars = buildResolvedVars(baseVars, part.darkStr)
            for (decl in splitCssDeclarations(part.darkStr)) {
                val colon = decl.indexOf(':')
                if (colon <= 0) continue
                val prop = decl.substring(0, colon).trim().lowercase()
                val rawVal = simplifyColorMix(
                    evalCssMathTokens(
                        resolveCssVars(decl.substring(colon + 1).trim(), darkVars)
                            .replace("!important", "", ignoreCase = true)
                            .trim(),
                        emPx,
                    ),
                )
                when (prop) {
                    "color" -> parseCssColor(rawVal)?.let { darkTextCol = it }
                    "border-color", "border" -> {
                        CSS_COLOR_TOKEN_RE.find(rawVal.lowercase())?.let { m ->
                            parseCssColor(m.value)?.takeIf { it != Color.Transparent }?.let { darkBdCol = it }
                        } ?: parseCssColor(rawVal)?.takeIf { it != Color.Transparent }?.let { darkBdCol = it }
                    }
                    "background", "background-image", "background-color" -> {
                        if (prop == "background-color") {
                            val cleanedColor = rawVal.replace(urlTokenRe, "").trim()
                            parseCssColor(cleanedColor)?.takeIf { it.alpha > 0.05f }?.let { darkBgCol = it }
                        } else {
                            val layers = parseBackgroundShorthand(rawVal, emPx)
                            for (layer in layers.asReversed()) {
                                layer.bgColorRaw?.let { cStr ->
                                    parseCssColor(cStr)?.takeIf { it.alpha > 0.05f }?.let { darkBgCol = it }
                                }
                                layer.gradientRaw?.let { g ->
                                    if (layer.originClipBox != "border-box") {
                                        val (cols, _) = extractGradientColors(g)
                                        if (cols.count { it.alpha >= 0.8f } >= 2) darkBgGrad = cols
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val finalBgGrad = bgGrad ?: if (bgCol == null) translucentBgGrad else null
        val effSlice = if (isBaseNineSliceActive) sliceArr else null
        if (effSlice == null) {
            val p = if (hasPad) curPad else listOf(8f, 12f, 8f, 12f)
            val parsedSafe = parseSafeCss(safeDeclBuf.toString())
            val mergedSafe = ParsedSafeCss(
                textColor = textCol ?: parsedSafe?.textColor,
                bgColor = bgCol ?: parsedSafe?.bgColor,
                bgGradientColors = finalBgGrad ?: if (bgCol == null) parsedSafe?.bgGradientColors else null,
                bgGradientAngleDeg = bgAngleDeg ?: parsedSafe?.bgGradientAngleDeg,
                borderColor = bdCol ?: parsedSafe?.borderColor,
                borderWidthDp = borderW?.firstOrNull() ?: parsedSafe?.borderWidthDp,
                cornerRadiusDp = radii?.firstOrNull() ?: parsedSafe?.cornerRadiusDp,
                cornerRadiiDp = radii ?: parsedSafe?.cornerRadiiDp,
                fontWeight = parsedSafe?.fontWeight,
                fontStyle = parsedSafe?.fontStyle,
                letterSpacingSp = parsedSafe?.letterSpacingSp,
                opacity = parsedSafe?.opacity,
            )
            return ParsedCommunityBubbleCss(
                hasImage = part.urlCounter > 0,
                hasDarkImageVariant = part.darkImageCount > 0,
                baseUrlIdx = baseUrlIdx,
                hasBaseImageUrl = hasBaseImageUrl,
                sliceTop = 0f, sliceRight = 0f, sliceBottom = 0f, sliceLeft = 0f,
                isPercentSlice = false,
                borderTopDp = 0f, borderRightDp = 0f, borderBottomDp = 0f, borderLeftDp = 0f,
                startOverflowReserveDp = startOverflowReserve,
                endOverflowReserveDp = endOverflowReserve,
                topOverflowReserveDp = topOverflowReserve,
                bottomOverflowReserveDp = bottomOverflowReserve,
                padTopDp = (bw[0] + p[0]).coerceAtLeast(2f),
                padRightDp = (bw[1] + p[1]).coerceAtLeast(6f),
                padBottomDp = (bw[2] + p[2]).coerceAtLeast(2f),
                padLeftDp = (bw[3] + p[3]).coerceAtLeast(6f),
                minWidthDp = minWidth,
                textColorLight = textCol,
                textColorDark = darkTextCol,
                safeCssLight = mergedSafe,
                pseudoLayers = pseudoLayers,
                isNineSlice = false,
                bgColorLight = bgCol,
                bgGradientColorsLight = finalBgGrad,
                bgGradientAngleDeg = bgAngleDeg,
                borderColorLight = bdCol,
                borderWidthDp = borderW?.firstOrNull(),
                borderGradientColors = borderGradCols,
                nonNineSliceRadiiDp = radii,
                minHeightDp = minHeightDp,
                darkBgColor = darkBgCol,
                darkBgGradientColors = darkBgGrad,
                darkBorderColor = darkBdCol,
            )
        }
        val bImg = borderImageW ?: borderW ?: listOf(18f, 24f, 18f, 24f)
        val outMatch = Regex("""__OUTSET__=([0-9.,-]+)""").find(safeDeclBuf)
        val out = outMatch?.groupValues?.get(1)?.split(',')?.mapNotNull { it.toFloatOrNull() }?.takeIf { it.size == 4 }
            ?: listOf(0f, 0f, 0f, 0f)
        val p = if (hasPad) curPad else listOf(0f, 0f, 0f, 0f)
        return ParsedCommunityBubbleCss(
            hasImage = part.urlCounter > 0,
            hasDarkImageVariant = part.darkImageCount > 0,
            baseUrlIdx = baseUrlIdx,
            hasBaseImageUrl = hasBaseImageUrl,
            sliceTop = effSlice[0],
            sliceRight = effSlice[1],
            sliceBottom = effSlice[2],
            sliceLeft = effSlice[3],
            isPercentSlice = slicePercent,
            borderTopDp = bImg[0],
            borderRightDp = bImg[1],
            borderBottomDp = bImg[2],
            borderLeftDp = bImg[3],
            outsetTopDp = out[0],
            outsetRightDp = out[1],
            outsetBottomDp = out[2],
            outsetLeftDp = out[3],
            startOverflowReserveDp = startOverflowReserve,
            endOverflowReserveDp = endOverflowReserve,
            topOverflowReserveDp = topOverflowReserve,
            bottomOverflowReserveDp = bottomOverflowReserve,
            padTopDp = (bw[0] + p[0]).coerceAtLeast(2f),
            padRightDp = (bw[1] + p[1]).coerceAtLeast(6f),
            padBottomDp = (bw[2] + p[2]).coerceAtLeast(2f),
            padLeftDp = (bw[3] + p[3]).coerceAtLeast(6f),
            minWidthDp = minWidth,
            textColorLight = textCol,
            textColorDark = darkTextCol,
            safeCssLight = null,
            pseudoLayers = pseudoLayers,
            isNineSlice = true,
            borderGradientColors = borderGradCols,
            nonNineSliceRadiiDp = radii,
            minHeightDp = minHeightDp,
            darkBgColor = darkBgCol,
            darkBgGradientColors = darkBgGrad,
            darkBorderColor = darkBdCol,
        )
    }

    private fun colorToHex(color: Color): String {
        val a = (color.alpha * 255f).toInt().coerceIn(0, 255)
        val r = (color.red * 255f).toInt().coerceIn(0, 255)
        val g = (color.green * 255f).toInt().coerceIn(0, 255)
        val b = (color.blue * 255f).toInt().coerceIn(0, 255)
        return if (a == 255) "#%02x%02x%02x".format(r, g, b) else "#%02x%02x%02x%02x".format(a, r, g, b)
    }

    fun parseCommunityTitleCss(cssText: String, isDarkTheme: Boolean = false): ParsedCommunityTitleCss? {
        if (cssText.isBlank()) return null
        val part = cascadeCommunityCss(cssText, isDarkTheme)
        val allUrls = extractAllCssImageUrls(cssText)
        val emPx = 14f
        val staticParsed = parseCommunityTitleCssFrame(
            part = part,
            baseBody = part.baseStr,
            baseVars = part.baseVars,
            beforeVars = part.beforeVars,
            afterVars = part.afterVars,
            allUrlsSize = allUrls.size,
            emPx = emPx,
            includePseudoAnimations = true,
        ) ?: return null
        val (baseTf, baseOp) = extractElementBaseTransformAndOpacity(part.baseStr, part.baseVars, emPx)
        val anim = analyzeElementAnimations(part.baseStr, part.baseVars, part.keyframesReplaced, baseTf, baseOp, emPx)
        val timelineFrames = if (anim.timelineAnims.isNotEmpty() && anim.periodMs > 0L) {
            val frames = ArrayList<ParsedCommunityTitleCss>(anim.sampleCount)
            for (i in 0 until anim.sampleCount) {
                val elapsed = ((i + 0.5) * anim.periodMs / anim.sampleCount).roundToInt().toLong()
                val (ovVars, ovDecls) = sampleKeyframeFrameOverrides(anim.timelineAnims, part.keyframesReplaced, part.baseStr, part.baseVars, elapsed, emPx)
                val fBaseBody = if (ovDecls.isBlank()) part.baseStr else "${part.baseStr};$ovDecls"
                val fBaseVars = if (ovVars.isEmpty()) part.baseVars else buildResolvedVars(part.baseVars, part.baseStr, ovVars)
                val fBeforeVars = if (ovVars.isEmpty()) part.beforeVars else buildResolvedVars(fBaseVars, part.beforeStr)
                val fAfterVars = if (ovVars.isEmpty()) part.afterVars else buildResolvedVars(fBaseVars, part.afterStr)
                parseCommunityTitleCssFrame(
                    part = part,
                    baseBody = fBaseBody,
                    baseVars = fBaseVars,
                    beforeVars = fBeforeVars,
                    afterVars = fAfterVars,
                    allUrlsSize = allUrls.size,
                    emPx = emPx,
                    includePseudoAnimations = false,
                )?.let { frames.add(it) }
            }
            frames
        } else {
            emptyList()
        }
        return staticParsed.copy(
            visualAnim = anim.visualTracks,
            timelineFrames = timelineFrames,
            timelinePeriodMs = if (timelineFrames.isNotEmpty()) anim.periodMs else 0L,
        )
    }

    private fun parseCommunityTitleCssFrame(
        part: CascadedCommunityCss,
        baseBody: String,
        baseVars: Map<String, String>,
        beforeVars: Map<String, String>,
        afterVars: Map<String, String>,
        allUrlsSize: Int,
        emPx: Float,
        includePseudoAnimations: Boolean,
    ): ParsedCommunityTitleCss? {
        val urlTokenRe = Regex("""__URL_(\d+)__""")

        var gradColors: List<Color>? = null
        var translucentGradColors: List<Color>? = null
        var bgAngleDeg: Float? = null
        var solidBg: Color? = null
        var fgCol: Color? = null
        var bdCol: Color? = null
        var borderGradCols: List<Color>? = null
        var borderW: List<Float>? = null
        var radii: List<Float>? = null
        var overrideLabel: String? = null
        var fixedHeightDp: Float? = null
        var minHeightDp = 22f
        val curPad = mutableListOf(2f, 8f, 2f, 8f)
        var letterSpacingSp: Float? = null
        var fontWeight = FontWeight.Bold

        var baseUrlIdx = 0
        var hasBaseImageUrl = false
        var sliceArr: List<Float>? = null
        var slicePercent = false
        var sliceHasFill = false
        var borderImageW: List<Float>? = null
        var outsetW: List<Float>? = null

        var darkGradColors: List<Color>? = null
        var darkSolidBg: Color? = null
        var darkFgCol: Color? = null
        var darkBdCol: Color? = null

        var leadingIdx: Int? = null
        var trailingIdx: Int? = null

        fun inspectUrlPlacement(ruleSlice: String) {
            for (m in urlTokenRe.findAll(ruleSlice)) {
                val uIdx = m.groupValues[1].toIntOrNull() ?: continue
                val tail = ruleSlice.substring(m.range.last, (m.range.last + 120).coerceAtMost(ruleSlice.length)).lowercase()
                if (tail.contains("right")) trailingIdx = uIdx else if (leadingIdx == null) leadingIdx = uIdx
            }
        }

        fun extractGradientColors(gradStr: String): Pair<List<Color>, Float?> {
            val cols = CSS_COLOR_TOKEN_RE.findAll(gradStr.lowercase()).mapNotNull { parseCssColor(it.value) }.toList()
            val angle = Regex("""(-?\d+(?:\.\d+)?)deg""").find(gradStr.lowercase())?.groupValues?.get(1)?.toFloatOrNull()
            return cols to angle
        }

        for (decl in splitCssDeclarations(baseBody)) {
            val colon = decl.indexOf(':')
            if (colon <= 0) continue
            val prop = decl.substring(0, colon).trim().lowercase()
            if (prop.startsWith("--")) continue
            val rawVal = simplifyColorMix(
                evalCssMathTokens(
                    resolveCssVars(decl.substring(colon + 1).trim(), baseVars)
                        .replace("!important", "", ignoreCase = true)
                        .trim(),
                    emPx,
                ),
            )
            if (applyPaddingDecl(prop, rawVal, curPad)) continue
            when (prop) {
                "height", "block-size" -> parsePxLength(rawVal, emPx)?.let { fixedHeightDp = it }
                "min-height", "min-block-size" -> parsePxLength(rawVal, emPx)?.let { minHeightDp = it }
                "border-image" -> {
                    val bi = parseBorderImageShorthand(rawVal, emPx)
                    if (bi.clear) {
                        sliceArr = null
                        borderImageW = null
                        sliceHasFill = false
                    } else {
                        bi.urlIdx?.let {
                            baseUrlIdx = it
                            hasBaseImageUrl = true
                        }
                        if (bi.slice != null) {
                            sliceArr = bi.slice
                            slicePercent = rawVal.contains('%')
                        }
                        if (bi.fill) sliceHasFill = true
                        if (bi.width != null) borderImageW = bi.width
                        if (bi.outset != null) outsetW = bi.outset
                    }
                }
                "border-image-source" -> {
                    urlTokenRe.find(rawVal)?.groupValues?.get(1)?.toIntOrNull()?.let {
                        baseUrlIdx = it
                        hasBaseImageUrl = true
                    }
                }
                "border-image-slice" -> {
                    slicePercent = rawVal.contains('%')
                    sliceHasFill = rawVal.contains("fill", ignoreCase = true)
                    expandBoxFour(parseBoxFourNumbers(rawVal, emPx))?.let { sliceArr = it }
                }
                "border-image-width" -> expandBoxFour(parseBoxFourNumbers(rawVal, emPx))?.let { borderImageW = it }
                "border-image-outset" -> expandBoxFour(parseBoxFourNumbers(rawVal, emPx))?.let { outsetW = it }
                "border-width" -> expandBoxFour(parseBoxFourNumbers(rawVal, emPx))?.let { borderW = it }
                "border", "border-color" -> {
                    CSS_COLOR_TOKEN_RE.find(rawVal.lowercase())?.let { m -> parseCssColor(m.value)?.let { bdCol = it } }
                        ?: parseCssColor(rawVal)?.let { bdCol = it }
                    if (prop == "border") {
                        val widthOnly = CSS_COLOR_TOKEN_RE.replace(rawVal.lowercase(), "")
                        expandBoxFour(parseBoxFourNumbers(widthOnly, emPx))?.let { borderW = it }
                    }
                }
                "border-radius" -> expandBoxFour(parseBoxFourNumbers(rawVal, emPx))?.let { radii = it }
                "background", "background-image", "background-color" -> {
                    if (urlTokenRe.containsMatchIn(rawVal)) {
                        inspectUrlPlacement(rawVal)
                    }
                    if (prop == "background-color") {
                        val cleanedColor = rawVal.replace(urlTokenRe, "").trim()
                        parseCssColor(cleanedColor)?.let { if (it.alpha > 0.2f) solidBg = it }
                    } else {
                        val layers = parseBackgroundShorthand(rawVal, emPx)
                        for (layer in layers.asReversed()) {
                            layer.bgColorRaw?.let { cStr ->
                                parseCssColor(cStr)?.let { if (it.alpha > 0.2f) solidBg = it }
                            }
                            layer.gradientRaw?.let { g ->
                                val (cols, angle) = extractGradientColors(g)
                                if (cols.size >= 2) {
                                    if (layer.originClipBox == "border-box") {
                                        borderGradCols = cols
                                    } else if (cols.count { it.alpha >= 0.8f } >= 2) {
                                        gradColors = cols
                                        bgAngleDeg = angle
                                    } else if (translucentGradColors == null) {
                                        translucentGradColors = cols
                                    }
                                }
                            }
                        }
                        if (layers.isEmpty() && prop == "background-image" && rawVal.contains("gradient(", ignoreCase = true)) {
                            val (cols, angle) = extractGradientColors(rawVal)
                            if (cols.size >= 2) {
                                if (cols.count { it.alpha >= 0.8f } >= 2) {
                                    gradColors = cols
                                    bgAngleDeg = angle
                                } else if (translucentGradColors == null) {
                                    translucentGradColors = cols
                                }
                            }
                        }
                    }
                }
                "color" -> parseCssColor(rawVal)?.let { if (it.alpha > 0.2f) fgCol = it }
                "letter-spacing" -> parsePxOrEm(rawVal)?.let { letterSpacingSp = it }
                "font-weight" -> {
                    val fw = rawVal.toIntOrNull()
                    if (rawVal == "bold" || (fw != null && fw >= 700)) fontWeight = FontWeight.Bold
                    else if (fw != null && fw >= 600) fontWeight = FontWeight.SemiBold
                }
            }
        }

        if (part.afterStr.isNotBlank()) {
            Regex("""content\s*:\s*["']([^"']+)["']""").find(part.afterStr)?.let { m ->
                overrideLabel = m.groupValues[1].trim().takeIf { it.isNotEmpty() }
            }
            for (decl in splitCssDeclarations(part.afterStr)) {
                val colon = decl.indexOf(':')
                if (colon <= 0) continue
                val prop = decl.substring(0, colon).trim().lowercase()
                val rawVal = simplifyColorMix(
                    evalCssMathTokens(
                        resolveCssVars(decl.substring(colon + 1).trim(), afterVars)
                            .replace("!important", "", ignoreCase = true)
                            .trim(),
                        emPx,
                    ),
                )
                if (prop == "color") parseCssColor(rawVal)?.takeIf { it.alpha > 0.2f }?.let { fgCol = it }
            }
        }

        val hasNineSlice = sliceArr != null &&
            ((borderImageW == null || borderImageW!!.any { it > 0f }) || (sliceHasFill && sliceArr!!.all { it == 0f }))
        val bw = borderW ?: listOf(1f, 1f, 1f, 1f)
        val bwForPseudo = if (hasNineSlice) (borderW ?: listOf(0f, 0f, 0f, 0f)) else bw
        val pseudoLayers = ArrayList<ParsedPseudoLayer>(2)
        val beforePseudo = parsePseudoRuleAnimated(part.beforeStr, beforeVars, part.keyframesReplaced, bwForPseudo, emPx, includePseudoAnimations)
            ?.also { pseudoLayers.add(it) }
        val afterPseudo = parsePseudoRuleAnimated(part.afterStr, afterVars, part.keyframesReplaced, bwForPseudo, emPx, includePseudoAnimations)
            ?.also { pseudoLayers.add(it) }

        fun isPseudoRuleHidden(bodyStr: String, bodyVars: Map<String, String>): Boolean {
            var dispHidden = false
            var maskHidden = false
            var opHidden = false
            for (d in splitCssDeclarations(bodyStr)) {
                val c = d.indexOf(':')
                if (c <= 0) continue
                val p = d.substring(0, c).trim().lowercase()
                if (p.startsWith("--")) continue
                val rv = simplifyColorMix(
                    evalCssMathTokens(
                        resolveCssVars(d.substring(c + 1).trim(), bodyVars)
                            .replace("!important", "", ignoreCase = true)
                            .trim(),
                        emPx,
                    ),
                )
                when (p) {
                    "display" -> dispHidden = rv.equals("none", ignoreCase = true)
                    "visibility" -> dispHidden = rv.equals("hidden", ignoreCase = true) || rv.equals("collapse", ignoreCase = true)
                    "opacity" -> {
                        val op = rv.toFloatOrNull() ?: parsePxLength(rv, emPx)
                        opHidden = op != null && op <= 0.01f
                    }
                    "mask-image", "-webkit-mask-image", "mask", "-webkit-mask" -> maskHidden = isMaskCompletelyTransparent(rv)
                }
            }
            return dispHidden || maskHidden || opHidden
        }

        if (part.beforeStr.isNotBlank() && beforePseudo == null && !isPseudoRuleHidden(part.beforeStr, beforeVars)) {
            val bb = part.beforeStr.lowercase()
            urlTokenRe.find(part.beforeStr)?.groupValues?.get(1)?.toIntOrNull()?.let { idx ->
                if (bb.contains("inset-inline-end") || bb.contains("right:")) trailingIdx = trailingIdx ?: idx
                else leadingIdx = leadingIdx ?: idx
            }
        }
        if (allUrlsSize > 0 && !hasNineSlice && pseudoLayers.isEmpty() && leadingIdx == null && trailingIdx == null) {
            leadingIdx = 0
        }

        if (part.darkStr.isNotBlank()) {
            val darkVars = buildResolvedVars(baseVars, part.darkStr)
            for (decl in splitCssDeclarations(part.darkStr)) {
                val colon = decl.indexOf(':')
                if (colon <= 0) continue
                val prop = decl.substring(0, colon).trim().lowercase()
                val rawVal = simplifyColorMix(
                    evalCssMathTokens(
                        resolveCssVars(decl.substring(colon + 1).trim(), darkVars)
                            .replace("!important", "", ignoreCase = true)
                            .trim(),
                        emPx,
                    ),
                )
                when (prop) {
                    "color" -> parseCssColor(rawVal)?.takeIf { it.alpha > 0.2f }?.let { darkFgCol = it }
                    "border-color", "border" -> {
                        CSS_COLOR_TOKEN_RE.find(rawVal.lowercase())?.let { m -> parseCssColor(m.value)?.let { darkBdCol = it } }
                    }
                    "background", "background-image", "background-color" -> {
                        if (prop == "background-color") {
                            parseCssColor(rawVal)?.takeIf { it.alpha > 0.2f }?.let { darkSolidBg = it }
                        } else {
                            val layers = parseBackgroundShorthand(rawVal, emPx)
                            for (layer in layers.asReversed()) {
                                layer.bgColorRaw?.let { cStr ->
                                    parseCssColor(cStr)?.takeIf { it.alpha > 0.2f }?.let { darkSolidBg = it }
                                }
                                layer.gradientRaw?.let { g ->
                                    if (layer.originClipBox != "border-box") {
                                        val (cols, _) = extractGradientColors(g)
                                        if (cols.count { it.alpha >= 0.8f } >= 2) darkGradColors = cols
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val finalGradColors = gradColors ?: if (solidBg == null) translucentGradColors else null
        if (!hasNineSlice && finalGradColors == null && solidBg == null && borderGradCols == null && pseudoLayers.isEmpty()) return null
        val bg = finalGradColors?.firstOrNull() ?: solidBg ?: Color.Transparent
        val secBg = finalGradColors?.lastOrNull()
        val fg = fgCol ?: Color.White
        val bd = bdCol ?: bg
        val effSlice = if (hasNineSlice) sliceArr!! else listOf(0f, 0f, 0f, 0f)
        val effBImg = borderImageW ?: borderW ?: listOf(0f, 0f, 0f, 0f)
        val effOut = outsetW ?: listOf(0f, 0f, 0f, 0f)
        val padBase = if (hasNineSlice) {
            val pBw = borderW ?: listOf(0f, 0f, 0f, 0f)
            listOf(pBw[0] + curPad[0], pBw[1] + curPad[1], pBw[2] + curPad[2], pBw[3] + curPad[3])
        } else {
            listOf(bw[0] + curPad[0], bw[1] + curPad[1], bw[2] + curPad[2], bw[3] + curPad[3])
        }

        return ParsedCommunityTitleCss(
            bgHex = colorToHex(bg),
            secondaryBgHex = secBg?.let(::colorToHex),
            textHex = colorToHex(fg),
            borderHex = colorToHex(bd),
            darkBorderHex = darkBdCol?.let(::colorToHex),
            fillMode = if (finalGradColors != null) "grad" else "solid",
            cornerRadiiDp = radii,
            leadingIconIdx = leadingIdx?.takeIf { it < allUrlsSize },
            trailingIconIdx = trailingIdx?.takeIf { it < allUrlsSize },
            overrideLabel = overrideLabel,
            bgAngleDeg = bgAngleDeg,
            darkBgHex = (darkGradColors?.firstOrNull() ?: darkSolidBg)?.let(::colorToHex),
            darkSecondaryBgHex = darkGradColors?.lastOrNull()?.let(::colorToHex),
            darkTextHex = darkFgCol?.let(::colorToHex),
            darkHasImageVariant = part.darkImageCount > 0,
            hasNineSlice = hasNineSlice,
            baseUrlIdx = baseUrlIdx,
            hasBaseImageUrl = hasBaseImageUrl,
            borderGradientColors = borderGradCols,
            sliceTop = effSlice[0],
            sliceRight = effSlice[1],
            sliceBottom = effSlice[2],
            sliceLeft = effSlice[3],
            isPercentSlice = slicePercent,
            borderImgTopDp = effBImg[0],
            borderImgRightDp = effBImg[1],
            borderImgBottomDp = effBImg[2],
            borderImgLeftDp = effBImg[3],
            outsetTopDp = effOut[0],
            outsetRightDp = effOut[1],
            outsetBottomDp = effOut[2],
            outsetLeftDp = effOut[3],
            fixedHeightDp = fixedHeightDp,
            minHeightDp = minHeightDp,
            padTopDp = padBase[0],
            padRightDp = padBase[1],
            padBottomDp = padBase[2],
            padLeftDp = padBase[3],
            borderWidthDp = bw.firstOrNull() ?: 1f,
            letterSpacingSp = letterSpacingSp,
            fontWeight = fontWeight,
            hasImage = allUrlsSize > 0 || part.urlCounter > 0,
            pseudoLayers = pseudoLayers,
        )
    }

    fun parseCommunityBadgeCss(cssText: String, isDarkTheme: Boolean = false): ParsedCommunityBadgeCss? {
        if (cssText.isBlank()) return null
        val part = cascadeCommunityCss(cssText, isDarkTheme)
        val emPx = 14f
        val staticParsed = parseCommunityBadgeCssFrame(
            part = part,
            baseBody = part.baseStr,
            baseVars = part.baseVars,
            beforeVars = part.beforeVars,
            afterVars = part.afterVars,
            emPx = emPx,
            includePseudoAnimations = true,
        ) ?: return null
        val (baseTf, baseOp) = extractElementBaseTransformAndOpacity(part.baseStr, part.baseVars, emPx)
        val anim = analyzeElementAnimations(part.baseStr, part.baseVars, part.keyframesReplaced, baseTf, baseOp, emPx)
        val timelineFrames = if (anim.timelineAnims.isNotEmpty() && anim.periodMs > 0L) {
            val frames = ArrayList<ParsedCommunityBadgeCss>(anim.sampleCount)
            for (i in 0 until anim.sampleCount) {
                val elapsed = ((i + 0.5) * anim.periodMs / anim.sampleCount).roundToInt().toLong()
                val (ovVars, ovDecls) = sampleKeyframeFrameOverrides(anim.timelineAnims, part.keyframesReplaced, part.baseStr, part.baseVars, elapsed, emPx)
                val fBaseBody = if (ovDecls.isBlank()) part.baseStr else "${part.baseStr};$ovDecls"
                val fBaseVars = if (ovVars.isEmpty()) part.baseVars else buildResolvedVars(part.baseVars, part.baseStr, ovVars)
                val fBeforeVars = if (ovVars.isEmpty()) part.beforeVars else buildResolvedVars(fBaseVars, part.beforeStr)
                val fAfterVars = if (ovVars.isEmpty()) part.afterVars else buildResolvedVars(fBaseVars, part.afterStr)
                parseCommunityBadgeCssFrame(
                    part = part,
                    baseBody = fBaseBody,
                    baseVars = fBaseVars,
                    beforeVars = fBeforeVars,
                    afterVars = fAfterVars,
                    emPx = emPx,
                    includePseudoAnimations = false,
                )?.let { frames.add(it) }
            }
            frames
        } else {
            emptyList()
        }
        return staticParsed.copy(
            visualAnim = anim.visualTracks,
            timelineFrames = timelineFrames,
            timelinePeriodMs = if (timelineFrames.isNotEmpty()) anim.periodMs else 0L,
        )
    }

    private fun parseCommunityBadgeCssFrame(
        part: CascadedCommunityCss,
        baseBody: String,
        baseVars: Map<String, String>,
        beforeVars: Map<String, String>,
        afterVars: Map<String, String>,
        emPx: Float,
        includePseudoAnimations: Boolean,
    ): ParsedCommunityBadgeCss? {
        val urlTokenRe = Regex("""__URL_(\d+)__""")
        var widthDp = 24f
        var heightDp = 24f
        var baseUrlIdx = 0
        var hasBaseUrl = false
        var spriteCols = 1
        var spriteRows = 1
        var spriteCol = 0
        var spriteRow = 0
        var posXVal: Float? = null
        var posXUnit = "%"
        var posYVal: Float? = null
        var posYUnit = "%"
        var opacityVal = 1f

        val posTokRe = Regex("""(-?\d*\.?\d+)(%|px|r?em)?|left|center|right|top|bottom""", RegexOption.IGNORE_CASE)
        fun parseAxisPos(tok: String): Pair<Float, String>? {
            val low = tok.lowercase()
            return when (low) {
                "left", "top" -> 0f to "%"
                "center" -> 50f to "%"
                "right", "bottom" -> 100f to "%"
                else -> {
                    val m = Regex("""^(-?\d*\.?\d+)(%|px|r?em)?$""", RegexOption.IGNORE_CASE).matchEntire(low)
                        ?: return null
                    val n = m.groupValues[1].toFloatOrNull() ?: return null
                    n to m.groupValues[2]
                }
            }
        }

        for (decl in splitCssDeclarations(baseBody)) {
            val colon = decl.indexOf(':')
            if (colon <= 0) continue
            val prop = decl.substring(0, colon).trim().lowercase()
            if (prop.startsWith("--")) continue
            val rawVal = evalCssMathTokens(
                resolveCssVars(decl.substring(colon + 1).trim(), baseVars)
                    .replace("!important", "", ignoreCase = true)
                    .trim(),
                emPx,
            )
            when (prop) {
                "width", "inline-size" -> parsePxLength(rawVal, emPx)?.let { widthDp = it.coerceIn(18f, 40f) }
                "height", "block-size" -> parsePxLength(rawVal, emPx)?.let { heightDp = it.coerceIn(18f, 40f) }
                "opacity" -> rawVal.toFloatOrNull()?.let { opacityVal = it.coerceIn(0f, 1f) }
                "background", "background-image", "border-image-source", "border-image" -> {
                    urlTokenRe.find(rawVal)?.groupValues?.get(1)?.toIntOrNull()?.let {
                        baseUrlIdx = it
                        hasBaseUrl = true
                    }
                    if (prop == "background") {
                        parseBackgroundShorthand(rawVal, emPx).firstOrNull { it.urlIdx != null }?.let { layer ->
                            layer.bgSizeRaw?.let { sz ->
                                val pcts = Regex("""(\d+(?:\.\d+)?)%""").findAll(sz).mapNotNull { it.groupValues[1].toFloatOrNull() }.toList()
                                if (pcts.size >= 2) {
                                    spriteCols = (pcts[0] / 100f).toInt().coerceAtLeast(1)
                                    spriteRows = (pcts[1] / 100f).toInt().coerceAtLeast(1)
                                }
                            }
                            layer.bgPosRaw?.let { ps ->
                                val matches = posTokRe.findAll(ps).mapNotNull { parseAxisPos(it.value) }.toList()
                                matches.getOrNull(0)?.let {
                                    posXVal = it.first
                                    posXUnit = it.second
                                }
                                matches.getOrNull(1)?.let {
                                    posYVal = it.first
                                    posYUnit = it.second
                                }
                            }
                        }
                    }
                }
                "background-size" -> {
                    val pcts = Regex("""(\d+(?:\.\d+)?)%""").findAll(rawVal).mapNotNull { it.groupValues[1].toFloatOrNull() }.toList()
                    if (pcts.size >= 2) {
                        spriteCols = (pcts[0] / 100f).toInt().coerceAtLeast(1)
                        spriteRows = (pcts[1] / 100f).toInt().coerceAtLeast(1)
                    }
                }
                "background-position" -> {
                    val matches = posTokRe.findAll(rawVal).mapNotNull { parseAxisPos(it.value) }.toList()
                    matches.getOrNull(0)?.let {
                        posXVal = it.first
                        posXUnit = it.second
                    }
                    matches.getOrNull(1)?.let {
                        posYVal = it.first
                        posYUnit = it.second
                    }
                }
                "background-position-x" -> {
                    parseAxisPos(rawVal.trim())?.let {
                        posXVal = it.first
                        posXUnit = it.second
                    }
                }
                "background-position-y" -> {
                    parseAxisPos(rawVal.trim())?.let {
                        posYVal = it.first
                        posYUnit = it.second
                    }
                }
            }
        }
        fun resolveSpriteIndex(pos: Float, unit: String, cellCount: Int, cellDp: Float): Int {
            if (cellCount <= 1) return 0
            return when (unit) {
                "", "%" -> {
                    if (pos < 0f) {
                        val step = kotlin.math.round(-pos / 100f).toInt()
                        ((step % cellCount) + cellCount) % cellCount
                    } else {
                        kotlin.math.round((pos / 100f) * (cellCount - 1)).toInt().coerceIn(0, cellCount - 1)
                    }
                }
                else -> {
                    val mult = when (unit) {
                        "rem" -> 16f
                        "em" -> emPx
                        else -> 1f
                    }
                    val step = kotlin.math.round(abs(pos * mult) / cellDp.coerceAtLeast(1f)).toInt()
                    ((step % cellCount) + cellCount) % cellCount
                }
            }
        }
        posXVal?.let { spriteCol = resolveSpriteIndex(it, posXUnit, spriteCols, widthDp) }
        posYVal?.let { spriteRow = resolveSpriteIndex(it, posYUnit, spriteRows, heightDp) }

        val pseudoLayers = ArrayList<ParsedPseudoLayer>(2)
        parsePseudoRuleAnimated(part.beforeStr, beforeVars, part.keyframesReplaced, listOf(0f, 0f, 0f, 0f), emPx, includePseudoAnimations)
            ?.let { pseudoLayers.add(it) }
        parsePseudoRuleAnimated(part.afterStr, afterVars, part.keyframesReplaced, listOf(0f, 0f, 0f, 0f), emPx, includePseudoAnimations)
            ?.let { pseudoLayers.add(it) }

        return ParsedCommunityBadgeCss(
            widthDp = widthDp,
            heightDp = heightDp,
            hasImage = hasBaseUrl || pseudoLayers.isNotEmpty() || part.urlCounter > 0,
            hasBaseImage = hasBaseUrl,
            hasDarkImageVariant = part.darkImageCount > 0,
            baseUrlIdx = baseUrlIdx,
            spriteCols = spriteCols,
            spriteRows = spriteRows,
            spriteCol = spriteCol,
            spriteRow = spriteRow,
            pseudoLayers = pseudoLayers,
            opacity = opacityVal,
        )
    }

    fun registerParsedCommunityCss(kind: String, sha: String, cssText: String): Boolean {
        if (sha.isBlank() || cssText.isBlank()) return false
        val lightKey = "$sha:light"
        val darkKey = "$sha:dark"
        return when (kind) {
            "bubble", "reply" -> {
                if (dynamicParsedBubbles.containsKey(lightKey) && dynamicParsedBubbles.containsKey(darkKey)) return false
                val lightParsed = parseCommunityBubbleCss(cssText, isDarkTheme = false) ?: return false
                val darkParsed = parseCommunityBubbleCss(cssText, isDarkTheme = true) ?: lightParsed
                val prevLight = dynamicParsedBubbles.putIfAbsent(lightKey, lightParsed)
                val prevDark = dynamicParsedBubbles.putIfAbsent(darkKey, darkParsed)
                dynamicParsedBubbles.putIfAbsent(sha, lightParsed)
                if (prevLight == null || prevDark == null) {
                    BadgeAndCosmeticRegistry.notifyDynamicCosmeticParsed()
                    true
                } else {
                    false
                }
            }
            "title" -> {
                if (dynamicParsedTitles.containsKey(lightKey) && dynamicParsedTitles.containsKey(darkKey)) return false
                val lightParsed = parseCommunityTitleCss(cssText, isDarkTheme = false) ?: return false
                val darkParsed = parseCommunityTitleCss(cssText, isDarkTheme = true) ?: lightParsed
                val prevLight = dynamicParsedTitles.putIfAbsent(lightKey, lightParsed)
                val prevDark = dynamicParsedTitles.putIfAbsent(darkKey, darkParsed)
                dynamicParsedTitles.putIfAbsent(sha, lightParsed)
                if (prevLight == null || prevDark == null) {
                    BadgeAndCosmeticRegistry.notifyDynamicCosmeticParsed()
                    true
                } else {
                    false
                }
            }
            "badge" -> {
                if (dynamicParsedBadges.containsKey(lightKey) && dynamicParsedBadges.containsKey(darkKey)) return false
                val lightParsed = parseCommunityBadgeCss(cssText, isDarkTheme = false) ?: return false
                val darkParsed = parseCommunityBadgeCss(cssText, isDarkTheme = true) ?: lightParsed
                val prevLight = dynamicParsedBadges.putIfAbsent(lightKey, lightParsed)
                val prevDark = dynamicParsedBadges.putIfAbsent(darkKey, darkParsed)
                dynamicParsedBadges.putIfAbsent(sha, lightParsed)
                if (prevLight == null || prevDark == null) {
                    BadgeAndCosmeticRegistry.notifyDynamicCosmeticParsed()
                    true
                } else {
                    false
                }
            }
            else -> false
        }
    }

    // ---------- Safe CSS Cleaner & Parser (app.js:1767-1796 cleanCss) ----------

    private val CSS_PROP_RE = Regex(
        """^(color|background|background-color|background-image|box-shadow|text-shadow|font-weight|font-style|letter-spacing|padding|opacity|filter|border(-(top|right|bottom|left))?(-(color|width|style))?|border(-(top|bottom)-(left|right))?-radius)$""",
    )
    private val CSS_BAD_RE = Regex(
        """url|expression|script|import|behavior|binding|attr|var|env|image-set|element|calc|fixed|absolute|sticky|z-index|content|\d(vw|vh|vmin|vmax|cm|mm|in|pt|pc|q|ch|ex)\b""",
    )
    private val CSS_FN_COLOR = setOf("rgb", "rgba", "hsl", "hsla")
    private val CSS_FN_GRAD = setOf(
        "linear-gradient", "radial-gradient", "conic-gradient",
        "repeating-linear-gradient", "repeating-radial-gradient", "repeating-conic-gradient",
    )
    private val CSS_FN_FILTER = setOf("hue-rotate", "saturate", "brightness")
    private val CSS_COMMENT_RE = Regex("""/\*[\s\S]*?(\*/|$)""")
    private val CSS_VAL_ALLOWED_RE = Regex("""^[a-z0-9#%., ()/+-]+$""")
    private val CSS_FN_NAME_RE = Regex("""([a-z-]+)\(""")
    private val CSS_UNIT_LIMIT_RE = Regex("""(\d*\.?\d+)(px|r?em)""")
    private val CSS_FILTER_RE = Regex("""^((hue-rotate|saturate|brightness)\([0-9.]+(deg|%)?\) ?)+$""")
    private val CSS_COLOR_TOKEN_RE = Regex("""#[0-9a-f]{3,8}|(?:rgba?|hsla?)\([^)]+\)""")

    fun cleanCss(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val sanitized = raw.take(600)
            .replace(CSS_COMMENT_RE, "")
            .replace(Regex("""[{}<>\\@]"""), "")
        val out = ArrayList<String>(16)
        for (part in sanitized.split(';')) {
            val idx = part.indexOf(':')
            if (idx < 0) continue
            val prop = part.substring(0, idx).trim().lowercase()
            val value = part.substring(idx + 1).trim().replace(Regex("""\s+"""), " ").lowercase()
            if (!CSS_PROP_RE.matches(prop) || value.isEmpty() || value.length > 240) continue
            if (!CSS_VAL_ALLOWED_RE.matches(value) || CSS_BAD_RE.containsMatchIn(value)) continue
            val fnNames = CSS_FN_NAME_RE.findAll(value).map { it.groupValues[1] }.toList()
            val allowedFns = when (prop) {
                "filter" -> CSS_FN_FILTER
                "background", "background-image" -> CSS_FN_COLOR + CSS_FN_GRAD
                else -> CSS_FN_COLOR
            }
            if (!fnNames.all { it in allowedFns }) continue
            if (prop == "filter" && !CSS_FILTER_RE.matches(value)) continue
            if (prop == "background-image" && fnNames.none { it in CSS_FN_GRAD }) continue
            if (prop == "opacity") {
                val num = value.toDoubleOrNull() ?: continue
                if (num < 0.6 || num > 1.0) continue
            }
            if ((prop == "padding" || prop == "letter-spacing" || prop.endsWith("width")) && value.contains('%')) {
                continue
            }
            val unitOverflow = CSS_UNIT_LIMIT_RE.findAll(value).any { m ->
                val v = m.groupValues[1].toDoubleOrNull() ?: 0.0
                val limit = if (m.groupValues[2] == "px") 24.0 else 2.0
                v > limit
            }
            if (unitOverflow) continue
            out.add("$prop: $value")
            if (out.size >= 16) break
        }
        val joined = out.joinToString("; ")
        return if (joined.length <= 600) joined else ""
    }

    fun parseSafeCss(raw: String?): ParsedSafeCss? {
        val cleaned = cleanCss(raw)
        if (cleaned.isEmpty()) return null
        var textColor: Color? = null
        var bgColor: Color? = null
        var bgGradColors: List<Color>? = null
        var bgGradAngle: Float? = null
        var borderColor: Color? = null
        var borderWidthDp: Float? = null
        var cornerRadiusDp: Float? = null
        var cornerRadiiDp: List<Float>? = null
        var fontWeight: FontWeight? = null
        var fontStyle: FontStyle? = null
        var letterSpacingSp: Float? = null
        var opacity: Float? = null

        for (decl in cleaned.split(';')) {
            val idx = decl.indexOf(':')
            if (idx < 0) continue
            val p = decl.substring(0, idx).trim()
            val v = decl.substring(idx + 1).trim()
            when (p) {
                "color" -> parseCssColor(v)?.let { textColor = it }
                "background-color" -> parseCssColor(v)?.let {
                    bgColor = it
                    bgGradColors = null
                }
                "background", "background-image" -> {
                    if (v.contains("gradient(")) {
                        val gradColors = CSS_COLOR_TOKEN_RE.findAll(v).mapNotNull { parseCssColor(it.value) }.toList()
                        if (gradColors.size >= 2) {
                            bgGradColors = gradColors
                            bgColor = gradColors.first()
                            val deg = Regex("""(-?\d+(?:\.\d+)?)deg""").find(v)?.groupValues?.get(1)?.toFloatOrNull()
                            if (deg != null) bgGradAngle = deg
                        }
                    } else if (p == "background") {
                        parseCssColor(v)?.let {
                            bgColor = it
                            bgGradColors = null
                        }
                    }
                }
                "border-color" -> parseCssColor(v)?.let { borderColor = it }
                "border-width" -> parsePxOrEm(v)?.let { borderWidthDp = it }
                "border", "border-top", "border-right", "border-bottom", "border-left" -> {
                    CSS_COLOR_TOKEN_RE.find(v)?.let { m -> parseCssColor(m.value)?.let { borderColor = it } }
                    if (p == "border") parsePxOrEm(v)?.let { borderWidthDp = it }
                }
                "border-radius" -> {
                    val nums = Regex("""(\d*\.?\d+)(px|r?em)?""").findAll(v).mapNotNull { m ->
                        val n = m.groupValues[1].toFloatOrNull() ?: return@mapNotNull null
                        when (m.groupValues[2]) {
                            "em", "rem" -> n * 14f
                            else -> n
                        }
                    }.toList()
                    if (nums.size == 1) cornerRadiusDp = nums[0].coerceIn(0f, 24f)
                    else if (nums.size >= 4) cornerRadiiDp = nums.take(4).map { it.coerceIn(0f, 24f) }
                }
                "font-weight" -> {
                    val fw = v.toIntOrNull()
                    fontWeight = when {
                        v == "bold" || (fw != null && fw >= 700) -> FontWeight.Bold
                        v == "600" -> FontWeight.SemiBold
                        v == "500" -> FontWeight.Medium
                        else -> fontWeight
                    }
                }
                "font-style" -> if (v == "italic") fontStyle = FontStyle.Italic
                "letter-spacing" -> parsePxOrEm(v)?.let { letterSpacingSp = it.coerceIn(0f, 4f) }
                "opacity" -> v.toFloatOrNull()?.let { opacity = it.coerceIn(0.6f, 1f) }
            }
        }
        return ParsedSafeCss(
            textColor = textColor,
            bgColor = bgColor,
            bgGradientColors = bgGradColors,
            bgGradientAngleDeg = bgGradAngle,
            borderColor = borderColor,
            borderWidthDp = borderWidthDp,
            cornerRadiusDp = cornerRadiusDp,
            cornerRadiiDp = cornerRadiiDp,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            letterSpacingSp = letterSpacingSp,
            opacity = opacity,
        )
    }

    private fun parsePxOrEm(raw: String): Float? {
        val m = Regex("""(\d*\.?\d+)(px|r?em)?""").find(raw) ?: return null
        val n = m.groupValues[1].toFloatOrNull() ?: return null
        return when (m.groupValues[2]) {
            "em", "rem" -> (n * 14f).coerceAtMost(24f)
            else -> n.coerceAtMost(24f)
        }
    }

    fun parseCssColor(raw: String?): Color? {
        val s = raw?.trim()?.lowercase() ?: return null
        if (s.isEmpty() || s == "none") return null
        if (s == "transparent") return Color.Transparent
        if (s.startsWith("#")) return parseHexColorPure(s)
        if (s.startsWith("rgb")) {
            val nums = Regex("""-?[\d.]+%?""").findAll(s).toList()
            if (nums.size >= 3) {
                fun parseChan(tok: String, isAlpha: Boolean): Float {
                    val isPct = tok.endsWith("%")
                    val v = tok.removeSuffix("%").toFloatOrNull() ?: return 0f
                    return when {
                        isAlpha && isPct -> (v / 100f).coerceIn(0f, 1f)
                        isAlpha -> v.coerceIn(0f, 1f)
                        isPct -> (v / 100f).coerceIn(0f, 1f)
                        else -> (v / 255f).coerceIn(0f, 1f)
                    }
                }
                val r = parseChan(nums[0].value, isAlpha = false)
                val g = parseChan(nums[1].value, isAlpha = false)
                val b = parseChan(nums[2].value, isAlpha = false)
                val a = nums.getOrNull(3)?.let { parseChan(it.value, isAlpha = true) } ?: 1f
                return Color(r, g, b, a)
            }
        }
        if (s.startsWith("hsl")) {
            val nums = Regex("""[\d.]+""").findAll(s).mapNotNull { it.value.toFloatOrNull() }.toList()
            if (nums.size >= 3) {
                val a = nums.getOrNull(3)?.let { if (it > 1f) it / 100f else it }?.coerceIn(0f, 1f) ?: 1f
                return hslToColor(nums[0], nums[1], nums[2], a)
            }
        }
        return when (s) {
            "white" -> Color.White
            "black" -> Color.Black
            "red" -> Color.Red
            "green" -> Color(0xFF008000)
            "blue" -> Color.Blue
            "yellow" -> Color.Yellow
            "cyan", "aqua" -> Color.Cyan
            "magenta", "fuchsia" -> Color.Magenta
            "gray", "grey" -> Color.Gray
            else -> null
        }
    }

    fun parseHexColorPure(hex: String, fallback: Color = Color.Transparent): Color {
        val clean = hex.trim().removePrefix("#")
        val expanded = when (clean.length) {
            3 -> "${clean[0]}${clean[0]}${clean[1]}${clean[1]}${clean[2]}${clean[2]}"
            4 -> "${clean[3]}${clean[3]}${clean[0]}${clean[0]}${clean[1]}${clean[1]}${clean[2]}${clean[2]}"
            8 -> {
                // CSS #RRGGBBAA -> Android ARGB
                clean.substring(6, 8) + clean.substring(0, 6)
            }
            6 -> clean
            else -> return fallback
        }
        val parsed = expanded.toLongOrNull(16) ?: return fallback
        return if (expanded.length == 6) Color(0xFF000000L or parsed) else Color(parsed)
    }

    fun hslToColor(hDeg: Float, sPct: Float, lPct: Float, alpha: Float = 1f): Color {
        val h = ((hDeg % 360f) + 360f) % 360f
        val s = (sPct / 100f).coerceIn(0f, 1f)
        val l = (lPct / 100f).coerceIn(0f, 1f)
        val c = (1f - abs(2f * l - 1f)) * s
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = l - c / 2f
        val (r1, g1, b1) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return Color(
            red = (r1 + m).coerceIn(0f, 1f),
            green = (g1 + m).coerceIn(0f, 1f),
            blue = (b1 + m).coerceIn(0f, 1f),
            alpha = alpha.coerceIn(0f, 1f),
        )
    }

    fun contrastRatio(fg: Color, bg: Color): Float {
        val l1 = fg.luminance()
        val l2 = bg.luminance()
        return (max(l1, l2) + 0.05f) / (min(l1, l2) + 0.05f)
    }

    fun ensureReadableForeground(
        preferred: Color,
        background: Color,
        minContrast: Float = 4.5f,
    ): Color {
        if (background.alpha < 0.25f) return preferred
        if (contrastRatio(preferred, background) >= minContrast) return preferred
        val darkInk = Color(0xFF172D25)
        val lightInk = Color(0xFFF8FAFC)
        return if (contrastRatio(lightInk, background) >= contrastRatio(darkInk, background)) lightInk else darkInk
    }

    fun blendColors(a: Color, b: Color, ratioA: Float): Color {
        val r = ratioA.coerceIn(0f, 1f)
        val inv = 1f - r
        return Color(
            red = a.red * r + b.red * inv,
            green = a.green * r + b.green * inv,
            blue = a.blue * r + b.blue * inv,
            alpha = 1f,
        )
    }

    // ---------- ChickenGun Sizing (title-collection.js:23) ----------

    fun chickengunSizeDp(
        level: Int,
        requested: Int?,
        random: Boolean,
        seed: String = "preview",
    ): Float {
        val stage = BadgeAndCosmeticRegistry.raffleStage(level)
        val caps = intArrayOf(30, 32, 36, 40, 44, 48, 51, 54, 57, 60)
        val defaults = intArrayOf(30, 32, 34, 36, 40, 44, 48, 50, 52, 54)
        val cap = caps[(stage - 1).coerceIn(0, 9)]
        val size = if (requested != null && requested > 0) {
            requested.coerceIn(24, cap)
        } else {
            defaults[(stage - 1).coerceIn(0, 9)]
        }
        if (!random || size <= 24) return size.toFloat()
        var hash = 2166136261u
        for (ch in seed) {
            hash = (hash xor ch.code.toUInt()) * 16777619u
        }
        val span = (size - 24 + 1).toUInt()
        return (24 + (hash % span).toInt()).toFloat()
    }

    // ---------- Chat Bubble & Boost Frame Resolution ----------

    fun resolveChatBubbleStyle(
        user: ChatUser?,
        where: String = "chat",
        isDarkTheme: Boolean = false,
        hideBubble: Boolean = false,
        hideCss: Boolean = false,
        messageId: String = "preview",
        nowSec: Long = ServerClock.serverNowSec(),
    ): ResolvedChatBubbleStyle? {
        return resolveBubbleInternal(
            deco = user?.deco,
            cosmeticAccess = user?.cosmeticAccess,
            userKey = (user?.id?.takeIf { it > 0L }?.toString() ?: user?.username.orEmpty()),
            where = where,
            frame = false,
            isDarkTheme = isDarkTheme,
            hideBubble = hideBubble,
            hideCss = hideCss,
            messageId = messageId,
            nowSec = nowSec,
        )
    }

    fun resolveBoostFrameStyle(
        boost: ChatBoost,
        isDarkTheme: Boolean = false,
        hideBubble: Boolean = false,
        hideCss: Boolean = false,
        nowSec: Long = ServerClock.serverNowSec(),
    ): ResolvedChatBubbleStyle? {
        return resolveBubbleInternal(
            deco = boost.deco,
            cosmeticAccess = boost.cosmeticAccess,
            userKey = (boost.userId.takeIf { it > 0L }?.toString() ?: boost.username),
            where = "chat",
            frame = true,
            isDarkTheme = isDarkTheme,
            hideBubble = hideBubble,
            hideCss = hideCss,
            messageId = "boost:${boost.at}",
            nowSec = nowSec,
        )
    }

    fun resolveBubbleFromDeco(
        deco: JsonElement?,
        cosmeticAccess: String? = null,
        userKey: String = "preview",
        where: String = "chat",
        frame: Boolean = false,
        isDarkTheme: Boolean = false,
        hideBubble: Boolean = false,
        hideCss: Boolean = false,
        messageId: String = "preview",
        nowSec: Long = ServerClock.serverNowSec(),
    ): ResolvedChatBubbleStyle? = resolveBubbleInternal(
        deco = deco,
        cosmeticAccess = cosmeticAccess,
        userKey = userKey,
        where = where,
        frame = frame,
        isDarkTheme = isDarkTheme,
        hideBubble = hideBubble,
        hideCss = hideCss,
        messageId = messageId,
        nowSec = nowSec,
    )

    private fun resolveBubbleInternal(
        deco: JsonElement?,
        cosmeticAccess: String?,
        userKey: String,
        where: String,
        frame: Boolean,
        isDarkTheme: Boolean,
        hideBubble: Boolean,
        hideCss: Boolean,
        messageId: String,
        nowSec: Long,
    ): ResolvedChatBubbleStyle? {
        if (hideBubble) return null
        val obj = runCatching { deco?.jsonObject }.getOrNull() ?: return null
        val rawSc = obj["sc"]?.jsonPrimitive?.contentOrNull ?: obj["decoScope"]?.jsonPrimitive?.contentOrNull ?: "all"
        val sc = if (rawSc == "wall") "post" else rawSc
        val effWhere = if (where == "wall") "post" else where
        val scMatches = effWhere == "card" || sc == "all" || sc == effWhere

        val ptId = if (scMatches) {
            obj["pt"]?.jsonPrimitive?.contentOrNull ?: obj["equippedPersonalId"]?.jsonPrimitive?.contentOrNull
        } else {
            null
        }
        // 1. owner52 suit
        if (scMatches && BadgeAndCosmeticRegistry.isOwner52Suit(ptId)) {
            return resolveOwner52Bubble(ptId!!, userKey, messageId, frame, isDarkTheme)
        }

        // 2. Community bubble / reply skin (`meme-hall-skins`)
        if (scMatches) {
            val commObj = runCatching { obj["community"]?.jsonObject }.getOrNull()
            val commKey = if (frame) "reply" else "bubble"
            val commId = commObj?.get(commKey)?.jsonPrimitive?.contentOrNull
                ?: if (!frame) obj["communityBubbleId"]?.jsonPrimitive?.contentOrNull else null
            if (!commId.isNullOrBlank()) {
                val resolvedComm = resolveCommunityBubble(commId, frame, isDarkTheme)
                if (resolvedComm != null) return resolvedComm
            }
        }

        // 3. aurora preset (`!frame && bb == 'aurora'`)
        val bb = if (scMatches) (obj["bb"]?.jsonPrimitive?.contentOrNull ?: obj["bubbleSkin"]?.jsonPrimitive?.contentOrNull).orEmpty() else ""
        if (scMatches && !frame && bb == "aurora") {
            return resolvePresetBubble("aurora", isDarkTheme)
        }

        // 4. Growth / Luck / Rank bubble (`gr`)
        val grId = if (scMatches) obj["gr"]?.jsonPrimitive?.contentOrNull ?: obj["equippedGrowthId"]?.jsonPrimitive?.contentOrNull else null
        if (scMatches && !grId.isNullOrBlank()) {
            BadgeAndCosmeticRegistry.resolveGrowthRewardMeta(grId)?.let { meta ->
                return resolveGrowthBubble(meta, frame, isDarkTheme)
            }
        }

        // 5. Personal suit bubble (`pt`)
        if (scMatches && !ptId.isNullOrBlank() && BadgeAndCosmeticRegistry.resolvePersonalTitle(ptId) != null) {
            return resolvePersonalBubble(ptId, frame, isDarkTheme)
        }

        // 6. Raffle character suit bubble (`ft`) — active raffle is global and ignores `sc` (`app.js`)
        val activeRaffle = BadgeAndCosmeticRegistry.resolveActiveRaffle(obj, cosmeticAccess, nowSec)
        if (activeRaffle != null) {
            return resolveRaffleBubble(obj, activeRaffle, userKey, messageId, frame, isDarkTheme)
        }

        // 7. Official title bubble (`ot`)
        val otId = if (scMatches) obj["ot"]?.jsonPrimitive?.contentOrNull ?: obj["equippedOfficialId"]?.jsonPrimitive?.contentOrNull else null
        if (scMatches && !otId.isNullOrBlank() && BadgeAndCosmeticRegistry.resolveOfficialTitleName(otId) != null && OFFICIAL_PALETTES.containsKey(otId)) {
            return resolveOfficialBubble(otId, frame, isDarkTheme)
        }

        if (frame || !scMatches) return null

        // 8. Preset bubble (`bb`) + 9. Custom CSS bubble (`cssb`)
        val basePreset = if (bb.isNotBlank() && bb != "default") resolvePresetBubble(bb, isDarkTheme) else null
        val cssRaw = if (!hideCss) obj["cssb"]?.jsonPrimitive?.contentOrNull else null
        val parsedCss = parseSafeCss(cssRaw)
        return when {
            basePreset != null && parsedCss != null -> applyCustomCssOverride(basePreset, parsedCss)
            basePreset != null -> basePreset
            parsedCss != null -> buildStandaloneCustomCssBubble(parsedCss, isDarkTheme)
            else -> null
        }
    }

    private fun resolveOwner52Bubble(
        ptId: String,
        userKey: String,
        messageId: String,
        frame: Boolean,
        isDarkTheme: Boolean,
    ): ResolvedChatBubbleStyle {
        val seed = (userKey + ":" + messageId).hashCode()
        val idx = ((seed % 6) + 6) % 6
        val softHex = listOf("#f5e8df", "#f0eaf8", "#f7e6f0", "#f7eedf", "#eaf6d3", "#e8f0fa")[idx]
        val darkHex = listOf("#342925", "#2c2839", "#362537", "#342c25", "#2b3e20", "#263549")[idx]
        val mainHex = listOf("#bd5549", "#9d79cd", "#c5689e", "#bd8a48", "#83bd32", "#6d98c7")[idx]
        val accentHex = listOf("#e6c36c", "#75c9b1", "#cca1ef", "#e3b783", "#c2e66b", "#c8ddeb")[idx]
        val bgStart = if (isDarkTheme) parseHexColorPure(darkHex) else parseHexColorPure(softHex)
        val bgEnd = if (isDarkTheme) blendColors(parseHexColorPure(darkHex), parseHexColorPure(accentHex), 0.72f) else blendColors(parseHexColorPure(softHex), parseHexColorPure(accentHex), 0.75f)
        val fg = if (isDarkTheme) Color(0xFFF5F7F1) else Color(0xFF1F2937)
        val border = parseHexColorPure(mainHex)
        val corners = listOf(
            BubbleShapeKind.Rounded(12f, 5f, 15f, 10f),
            BubbleShapeKind.Rounded(15f, 15f, 5f, 15f),
            BubbleShapeKind.Rounded(5f, 13f, 5f, 13f),
            BubbleShapeKind.Rounded(10f, 14f, 13f, 5f),
            BubbleShapeKind.Rounded(5f, 15f, 15f, 12f),
            BubbleShapeKind.Rounded(14f, 5f, 14f, 5f),
        )[idx]
        val ownerBorderGrad = listOf(border, parseHexColorPure(accentHex), border, border.copy(alpha = 0.75f), border)
        return ResolvedChatBubbleStyle(
            key = "personal:$ptId",
            fillColor = bgStart,
            fillBrush = Brush.linearGradient(listOf(bgStart, bgEnd)),
            textColor = ensureReadableForeground(fg, bgStart),
            linkColor = if (isDarkTheme) Color(0xFFBAE6FD) else border,
            mutedColor = ensureReadableForeground(fg.copy(alpha = 0.72f), bgStart, 3.2f),
            borderColor = border,
            borderBrush = Brush.sweepGradient(ownerBorderGrad),
            borderWidthDp = if (frame) 1.2f else 1.5f,
            shapeKind = if (frame) BubbleShapeKind.Rounded(999f, 999f, 999f, 999f) else corners,
            shadowColor = border.copy(alpha = 0.25f),
            contentPadding = if (frame) PaddingValues(horizontal = 8.dp, vertical = 3.dp) else PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ornament = if (frame) BubbleOrnament.None else BubbleOrnament.Owner52(idx, isBVariant = ptId.contains("52b", ignoreCase = true)),
            borderGradientColors = ownerBorderGrad,
        )
    }

    private fun buildCommunityBubbleTimelineFrame(
        id: String,
        frame: Boolean,
        isDarkTheme: Boolean,
        dynCss: ParsedCommunityBubbleCss,
        legacyDarkFallback: Boolean,
        lightCssPrefix: String?,
        cssUrlPrefix: String?,
        imgSha: String?,
    ): BubbleTimelineFrame {
        if (!dynCss.isNineSlice) {
            val parentFallback = imgSha?.takeIf { dynCss.baseUrlIdx == 0 }?.let { "$ORIGIN/api/community-skins/image/$it" }
                ?: if (dynCss.hasBaseImageUrl && lightCssPrefix != null) "$lightCssPrefix:${dynCss.baseUrlIdx}" else null
            val mappedPseudo = dynCss.pseudoLayers.mapNotNull { l ->
                mapSingleParsedPseudoLayer(l, lightCssPrefix, parentFallback, includeTimeline = false)
            }
            val base = dynCss.safeCssLight?.let { buildStandaloneCustomCssBubble(it, isDarkTheme) }
            val gradCols = if (isDarkTheme) {
                dynCss.darkBgGradientColors ?: dynCss.bgGradientColorsLight
            } else {
                dynCss.bgGradientColorsLight
            }
            val fillCol = if (isDarkTheme) {
                dynCss.darkBgColor ?: dynCss.bgColorLight ?: gradCols?.firstOrNull() ?: base?.fillColor ?: Color(0xFF252F3D)
            } else {
                dynCss.bgColorLight ?: gradCols?.firstOrNull() ?: base?.fillColor ?: Color(0xFFF1F5F9)
            }
            val borderGrad = dynCss.borderGradientColors?.takeIf { it.size >= 2 }
            val bdCol = when {
                borderGrad != null -> Color.Transparent
                isDarkTheme -> dynCss.darkBorderColor ?: dynCss.borderColorLight ?: base?.borderColor ?: Color(0xFF475569)
                else -> dynCss.borderColorLight ?: base?.borderColor ?: Color(0xFFCBD5E1)
            }
            val rawFg = if (isDarkTheme) {
                dynCss.textColorDark ?: dynCss.textColorLight ?: dynCss.safeCssLight?.textColor ?: Color(0xFFF1F5F9)
            } else {
                dynCss.textColorLight ?: dynCss.safeCssLight?.textColor ?: Color(0xFF1E293B)
            }
            val safeFg = ensureReadableForeground(rawFg, fillCol)
            return BubbleTimelineFrame(
                fillColor = fillCol,
                fillGradColors = gradCols,
                fillGradAngleDeg = dynCss.bgGradientAngleDeg,
                textColor = safeFg,
                linkColor = if (safeFg.luminance() > 0.5f) Color(0xFF93C5FD) else Color(0xFF2563EB),
                mutedColor = safeFg.copy(alpha = 0.72f),
                borderColor = bdCol,
                borderGradColors = borderGrad,
                nineSliceSpec = null,
                pseudoLayers = mappedPseudo,
                opacity = dynCss.safeCssLight?.opacity ?: 1f,
            )
        }
        val fg = if (isDarkTheme) (dynCss.textColorDark ?: dynCss.textColorLight ?: Color(0xFFF1F5F9)) else (dynCss.textColorLight ?: Color(0xFF1E293B))
        val fallbackBg = if (fg.luminance() > 0.55f) Color(0xFF252F3D) else Color(0xFFF1F5F9)
        val baseSrc = when {
            legacyDarkFallback && cssUrlPrefix != null -> "$cssUrlPrefix:${dynCss.baseUrlIdx}"
            isDarkTheme && dynCss.hasDarkImageVariant && cssUrlPrefix != null -> "$cssUrlPrefix:${dynCss.baseUrlIdx}"
            imgSha != null && dynCss.baseUrlIdx == 0 -> "$ORIGIN/api/community-skins/image/$imgSha"
            else -> "${cssUrlPrefix ?: lightCssPrefix ?: ""}:${dynCss.baseUrlIdx}"
        }
        val mappedPseudo = dynCss.pseudoLayers.mapNotNull { l ->
            mapSingleParsedPseudoLayer(l, lightCssPrefix, baseSrc, includeTimeline = false)
        }
        return BubbleTimelineFrame(
            fillColor = fallbackBg,
            textColor = fg,
            linkColor = if (fg.luminance() > 0.5f) Color(0xFFBAE6FD) else Color(0xFF1E40AF),
            mutedColor = fg.copy(alpha = 0.72f),
            borderColor = Color.Transparent,
            nineSliceSpec = if (baseSrc.isBlank()) null else NineSliceSpec(
                sourceUrl = baseSrc,
                sliceTop = dynCss.sliceTop,
                sliceRight = dynCss.sliceRight,
                sliceBottom = dynCss.sliceBottom,
                sliceLeft = dynCss.sliceLeft,
                isPercentSlice = dynCss.isPercentSlice,
                borderTopDp = dynCss.borderTopDp,
                borderRightDp = dynCss.borderRightDp,
                borderBottomDp = dynCss.borderBottomDp,
                borderLeftDp = dynCss.borderLeftDp,
                outsetTopDp = dynCss.outsetTopDp,
                outsetRightDp = dynCss.outsetRightDp,
                outsetBottomDp = dynCss.outsetBottomDp,
                outsetLeftDp = dynCss.outsetLeftDp,
            ),
            pseudoLayers = mappedPseudo,
            opacity = if (frame) 1f else 1f,
        )
    }

    private fun resolveCommunityBubble(
        id: String,
        frame: Boolean,
        isDarkTheme: Boolean,
    ): ResolvedChatBubbleStyle? {
        if (id == "aurora-preset") {
            return if (frame) {
                val bg = if (isDarkTheme) Color(0xFF161E38) else Color(0xFFEEF4FF)
                val fg = if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF1E293B)
                val borderCols = listOf(Color(0xFF00F0FF), Color(0xFF8B5CF6), Color(0xFFFF007F), Color(0xFF00F0FF))
                ResolvedChatBubbleStyle(
                    key = "community:$id",
                    fillColor = bg,
                    textColor = fg,
                    linkColor = Color(0xFF38BDF8),
                    mutedColor = fg.copy(alpha = 0.75f),
                    borderBrush = Brush.linearGradient(borderCols),
                    borderColor = Color(0xFF8B5CF6),
                    borderWidthDp = 1.4f,
                    shapeKind = BubbleShapeKind.Rounded(4f, 12f, 12f, 4f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp),
                    borderGradientColors = borderCols,
                )
            } else {
                resolvePresetBubble("aurora", isDarkTheme).copy(key = "community:$id")
            }
        }

        val skin = BadgeAndCosmeticRegistry.resolveCommunitySkinMeta(id)
        if (skin == null) {
            if (!BadgeAndCosmeticRegistry.isValidCommunitySlug(id)) return null
            val bg = if (isDarkTheme) Color(0xFF252F3D) else Color(0xFFF1F5F9)
            val fg = if (isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF1E293B)
            return ResolvedChatBubbleStyle(
                key = "community:$id",
                fillColor = bg,
                textColor = fg,
                linkColor = if (isDarkTheme) Color(0xFF93C5FD) else Color(0xFF2563EB),
                mutedColor = fg.copy(alpha = 0.7f),
                borderColor = if (isDarkTheme) Color(0xFF475569) else Color(0xFFCBD5E1),
                borderWidthDp = 1.2f,
                shapeKind = BubbleShapeKind.Rounded(14f, 14f, 14f, 14f),
                contentPadding = if (frame) PaddingValues(horizontal = 8.dp, vertical = 3.dp) else PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                dynamicCssSpec = "catalog:$id",
            )
        }

        val allowed = if (frame) skin.hasReply else skin.hasBubble
        if (!allowed) return null

        val commKey = if (frame) "reply" else "bubble"
        val imgSha = if (frame) skin.replyImgSha else skin.bubbleImgSha
        val cssSha = if (frame) skin.replyCssSha else skin.bubbleCssSha
        val sliceArr = if (frame) skin.replySlice else skin.bubbleSlice
        val themeKey = if (isDarkTheme) "dark" else "light"
        val dynCss = cssSha?.takeIf { it.isNotBlank() }?.let { sha ->
            dynamicParsedBubbles["$sha:$themeKey"] ?: dynamicParsedBubbles[sha]
        }
        val hasThemeEntry = cssSha.isNullOrBlank() || dynamicParsedBubbles.containsKey("$cssSha:$themeKey")
        val missingDynamicCss = if (!cssSha.isNullOrBlank() && !hasThemeEntry && dynCss == null) "$id:$commKey:$cssSha" else null
        val lightCssPrefix = cssSha?.let { "community-css:$id:$commKey:$it" }

        val legacyDarkFallback = isDarkTheme &&
            cssSha != null &&
            dynCss?.hasDarkImageVariant == true &&
            !dynamicParsedBubbles.containsKey("$cssSha:dark")
        val cssUrlPrefix = cssSha?.let { sha ->
            if (legacyDarkFallback) "community-css-dark:$id:$commKey:$sha" else "community-css:$id:$commKey:$sha"
        }
        val mappedBubbleTimeline = if (dynCss != null && dynCss.timelineFrames.isNotEmpty() && dynCss.timelinePeriodMs > 0L) {
            CommunityBubbleTimeline(
                periodMs = dynCss.timelinePeriodMs,
                frames = dynCss.timelineFrames.map { f ->
                    buildCommunityBubbleTimelineFrame(
                        id = id,
                        frame = frame,
                        isDarkTheme = isDarkTheme,
                        dynCss = f,
                        legacyDarkFallback = legacyDarkFallback,
                        lightCssPrefix = lightCssPrefix,
                        cssUrlPrefix = cssUrlPrefix,
                        imgSha = imgSha,
                    )
                },
            )
        } else {
            null
        }

        if (dynCss != null && !dynCss.isNineSlice) {
            val parentFallback = imgSha?.let { "$ORIGIN/api/community-skins/image/$it" }
                ?: if (dynCss.hasBaseImageUrl && lightCssPrefix != null) "$lightCssPrefix:${dynCss.baseUrlIdx}" else null
            val mappedPseudo = mapParsedPseudoLayers(dynCss.pseudoLayers, lightCssPrefix, parentFallbackUrl = parentFallback)
            val base = dynCss.safeCssLight?.let { buildStandaloneCustomCssBubble(it, isDarkTheme) }
            val gradCols = if (isDarkTheme) {
                dynCss.darkBgGradientColors ?: dynCss.bgGradientColorsLight
            } else {
                dynCss.bgGradientColorsLight
            }
            val fillCol = if (isDarkTheme) {
                dynCss.darkBgColor ?: dynCss.bgColorLight ?: gradCols?.firstOrNull() ?: base?.fillColor ?: Color(0xFF252F3D)
            } else {
                dynCss.bgColorLight ?: gradCols?.firstOrNull() ?: base?.fillColor ?: Color(0xFFF1F5F9)
            }
            val fillBrush = when {
                gradCols != null && gradCols.size >= 2 -> Brush.linearGradient(gradCols)
                dynCss.bgColorLight != null || (isDarkTheme && dynCss.darkBgColor != null) -> null
                else -> base?.fillBrush
            }
            val borderGrad = dynCss.borderGradientColors?.takeIf { it.size >= 2 }
            val bdCol = when {
                borderGrad != null -> Color.Transparent
                isDarkTheme -> dynCss.darkBorderColor ?: dynCss.borderColorLight ?: base?.borderColor ?: Color(0xFF475569)
                else -> dynCss.borderColorLight ?: base?.borderColor ?: Color(0xFFCBD5E1)
            }
            val borderBrush = borderGrad?.let { Brush.sweepGradient(it) } ?: base?.borderBrush
            val borderW = if (borderGrad != null) (dynCss.borderWidthDp ?: base?.borderWidthDp ?: 2f).coerceAtLeast(1.2f) else (dynCss.borderWidthDp ?: base?.borderWidthDp ?: 1.2f)
            val rawFg = if (isDarkTheme) {
                dynCss.textColorDark ?: dynCss.textColorLight ?: dynCss.safeCssLight?.textColor ?: Color(0xFFF1F5F9)
            } else {
                dynCss.textColorLight ?: dynCss.safeCssLight?.textColor ?: Color(0xFF1E293B)
            }
            val safeFg = ensureReadableForeground(rawFg, fillCol)
            val radii = dynCss.nonNineSliceRadiiDp ?: dynCss.safeCssLight?.cornerRadiiDp
            val shape = if (radii != null && radii.size == 4) {
                BubbleShapeKind.Rounded(radii[0], radii[1], radii[2], radii[3])
            } else {
                base?.shapeKind ?: BubbleShapeKind.Rounded(14f, 14f, 14f, 14f)
            }
            return (base ?: ResolvedChatBubbleStyle(
                key = "community:$id",
                fillColor = fillCol,
                textColor = safeFg,
                linkColor = if (safeFg.luminance() > 0.5f) Color(0xFF93C5FD) else Color(0xFF2563EB),
                mutedColor = safeFg.copy(alpha = 0.72f),
                borderColor = bdCol,
            )).copy(
                key = "community:$id",
                fillColor = fillCol,
                fillBrush = fillBrush,
                textColor = safeFg,
                linkColor = if (safeFg.luminance() > 0.5f) Color(0xFF93C5FD) else Color(0xFF2563EB),
                mutedColor = safeFg.copy(alpha = 0.72f),
                borderColor = bdCol,
                borderBrush = borderBrush,
                borderWidthDp = borderW,
                shapeKind = shape,
                minWidthDp = if (frame) 0f else dynCss.minWidthDp,
                minHeightDp = if (frame) 0f else dynCss.minHeightDp,
                startOverflowReserveDp = dynCss.startOverflowReserveDp,
                endOverflowReserveDp = dynCss.endOverflowReserveDp,
                topOverflowReserveDp = dynCss.topOverflowReserveDp,
                bottomOverflowReserveDp = dynCss.bottomOverflowReserveDp,
                pseudoLayers = mappedPseudo,
                contentPadding = PaddingValues(
                    start = dynCss.padLeftDp.coerceAtLeast(if (frame) 6f else 10f).dp,
                    end = dynCss.padRightDp.coerceAtLeast(if (frame) 6f else 10f).dp,
                    top = dynCss.padTopDp.coerceAtLeast(if (frame) 2f else 6f).dp,
                    bottom = dynCss.padBottomDp.coerceAtLeast(if (frame) 2f else 6f).dp,
                ),
                borderGradientColors = borderGrad,
                visualAnim = dynCss.visualAnim,
                animationTimeline = mappedBubbleTimeline,
            )
        }

        val sourceUrl = when {
            legacyDarkFallback && cssUrlPrefix != null -> cssUrlPrefix
            isDarkTheme && dynCss?.hasDarkImageVariant == true && cssUrlPrefix != null -> cssUrlPrefix
            imgSha != null -> "$ORIGIN/api/community-skins/image/$imgSha"
            cssUrlPrefix != null -> cssUrlPrefix
            else -> null
        }

        if (dynCss != null && dynCss.isNineSlice && sourceUrl != null) {
            val fg = if (isDarkTheme) (dynCss.textColorDark ?: dynCss.textColorLight ?: Color(0xFFF1F5F9)) else (dynCss.textColorLight ?: Color(0xFF1E293B))
            val fallbackBg = if (fg.luminance() > 0.55f) Color(0xFF252F3D) else Color(0xFFF1F5F9)
            val baseSrc = when {
                legacyDarkFallback -> "${cssUrlPrefix ?: sourceUrl}:${dynCss.baseUrlIdx}"
                isDarkTheme && dynCss.hasDarkImageVariant && cssUrlPrefix != null -> "$cssUrlPrefix:${dynCss.baseUrlIdx}"
                imgSha != null -> "$ORIGIN/api/community-skins/image/$imgSha"
                else -> "${cssUrlPrefix ?: sourceUrl}:${dynCss.baseUrlIdx}"
            }
            val mappedPseudo = mapParsedPseudoLayers(dynCss.pseudoLayers, lightCssPrefix, parentFallbackUrl = baseSrc)
            return ResolvedChatBubbleStyle(
                key = "community:$id",
                fillColor = fallbackBg,
                textColor = fg,
                linkColor = if (fg.luminance() > 0.5f) Color(0xFFBAE6FD) else Color(0xFF1E40AF),
                mutedColor = fg.copy(alpha = 0.72f),
                borderColor = Color.Transparent,
                shapeKind = BubbleShapeKind.Rounded(0f, 0f, 0f, 0f),
                minWidthDp = if (frame) 0f else dynCss.minWidthDp,
                minHeightDp = if (frame) 0f else dynCss.minHeightDp,
                startOverflowReserveDp = dynCss.startOverflowReserveDp,
                endOverflowReserveDp = dynCss.endOverflowReserveDp,
                topOverflowReserveDp = dynCss.topOverflowReserveDp,
                bottomOverflowReserveDp = dynCss.bottomOverflowReserveDp,
                contentPadding = PaddingValues(
                    start = dynCss.padLeftDp.coerceAtLeast(if (frame) 6f else 12f).dp,
                    end = dynCss.padRightDp.coerceAtLeast(if (frame) 6f else 12f).dp,
                    top = dynCss.padTopDp.coerceAtLeast(if (frame) 2f else 8f).dp,
                    bottom = dynCss.padBottomDp.coerceAtLeast(if (frame) 2f else 8f).dp,
                ),
                nineSliceSpec = NineSliceSpec(
                    sourceUrl = baseSrc,
                    sliceTop = dynCss.sliceTop,
                    sliceRight = dynCss.sliceRight,
                    sliceBottom = dynCss.sliceBottom,
                    sliceLeft = dynCss.sliceLeft,
                    isPercentSlice = dynCss.isPercentSlice,
                    borderTopDp = dynCss.borderTopDp,
                    borderRightDp = dynCss.borderRightDp,
                    borderBottomDp = dynCss.borderBottomDp,
                    borderLeftDp = dynCss.borderLeftDp,
                    outsetTopDp = dynCss.outsetTopDp,
                    outsetRightDp = dynCss.outsetRightDp,
                    outsetBottomDp = dynCss.outsetBottomDp,
                    outsetLeftDp = dynCss.outsetLeftDp,
                ),
                pseudoLayers = mappedPseudo,
                visualAnim = dynCss.visualAnim,
                animationTimeline = mappedBubbleTimeline,
            )
        }

        if (dynCss == null && sourceUrl != null && (sliceArr != null || imgSha != null)) {
            val sTop = sliceArr?.getOrNull(0)?.toFloat() ?: 36f
            val sRight = sliceArr?.getOrNull(1)?.toFloat() ?: 36f
            val sBottom = sliceArr?.getOrNull(2)?.toFloat() ?: 36f
            val sLeft = sliceArr?.getOrNull(3)?.toFloat() ?: 36f
            val bTop = if (frame) (sTop * 0.25f).coerceIn(6f, 14f) else (sTop * 0.5f).coerceIn(10f, 36f)
            val bRight = if (frame) (sRight * 0.25f).coerceIn(8f, 18f) else (sRight * 0.5f).coerceIn(12f, 40f)
            val bBottom = if (frame) (sBottom * 0.25f).coerceIn(6f, 14f) else (sBottom * 0.5f).coerceIn(10f, 36f)
            val bLeft = if (frame) (sLeft * 0.25f).coerceIn(8f, 18f) else (sLeft * 0.5f).coerceIn(12f, 40f)
            val bg = if (isDarkTheme) Color(0xFF252F3D) else Color(0xFFF1F5F9)
            val fg = if (isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF1E293B)
            return ResolvedChatBubbleStyle(
                key = "community:$id",
                fillColor = bg,
                textColor = fg,
                linkColor = if (isDarkTheme) Color(0xFF93C5FD) else Color(0xFF2563EB),
                mutedColor = fg.copy(alpha = 0.72f),
                borderColor = Color.Transparent,
                shapeKind = BubbleShapeKind.Rounded(0f, 0f, 0f, 0f),
                minWidthDp = if (frame) 0f else 72f,
                contentPadding = PaddingValues(
                    start = (if (frame) 12f else (bLeft * 0.85f).coerceIn(14f, 32f)).dp,
                    end = (if (frame) 12f else (bRight * 0.85f).coerceIn(14f, 32f)).dp,
                    top = (if (frame) 4f else (bTop * 0.65f).coerceIn(8f, 24f)).dp,
                    bottom = (if (frame) 4f else (bBottom * 0.65f).coerceIn(8f, 24f)).dp,
                ),
                nineSliceSpec = NineSliceSpec(
                    sourceUrl = sourceUrl,
                    sliceTop = sTop,
                    sliceRight = sRight,
                    sliceBottom = sBottom,
                    sliceLeft = sLeft,
                    borderTopDp = bTop,
                    borderRightDp = bRight,
                    borderBottomDp = bBottom,
                    borderLeftDp = bLeft,
                ),
                dynamicCssSpec = missingDynamicCss,
            )
        }

        val bg = if (isDarkTheme) Color(0xFF252F3D) else Color(0xFFF1F5F9)
        val fg = if (isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF1E293B)
        return ResolvedChatBubbleStyle(
            key = "community:$id",
            fillColor = bg,
            textColor = fg,
            linkColor = if (isDarkTheme) Color(0xFF93C5FD) else Color(0xFF2563EB),
            mutedColor = fg.copy(alpha = 0.7f),
            borderColor = if (isDarkTheme) Color(0xFF475569) else Color(0xFFCBD5E1),
            borderWidthDp = 1.2f,
            shapeKind = BubbleShapeKind.Rounded(14f, 14f, 14f, 14f),
            contentPadding = if (frame) PaddingValues(horizontal = 8.dp, vertical = 3.dp) else PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            dynamicCssSpec = missingDynamicCss,
        )
    }

    private fun resolveGrowthBubble(
        meta: com.jinhuan.forhappy.core.model.GrowthRewardMeta,
        frame: Boolean,
        isDarkTheme: Boolean,
    ): ResolvedChatBubbleStyle {
        val series = meta.series ?: "guess"
        val lv = meta.level.coerceIn(1, 5)
        val board = meta.board
        val isAll = meta.period == "all"

        val (bgStart, bgEnd, fg, edge, ringGold) = when {
            board != null -> {
                val hue = when (board) {
                    "sender_paid" -> 38f
                    "sender_count" -> 28f
                    "chat_count" -> 210f
                    "chat_early" -> 45f
                    "chat_late" -> 275f
                    "guess_winrate" -> 185f
                    else -> 170f
                }
                if (isAll) {
                    val fill = if (isDarkTheme) Color(0xFF252B2C) else Color(0xFFFFFDF8)
                    val end = if (isDarkTheme) hslToColor(hue + 20f, 30f, 22f) else hslToColor(hue, 60f, 95f)
                    val ink = if (isDarkTheme) Color(0xFFEBEEEA) else Color(0xFF35413F)
                    val gem = hslToColor(hue, 48f, if (isDarkTheme) 68f else 43f)
                    listOf(fill, end, ink, gem, Color(0xFFB69A62))
                } else {
                    val fill = hslToColor(hue, if (isDarkTheme) 22f else 60f, if (isDarkTheme) 19f else 96f)
                    val ink = hslToColor(hue, if (isDarkTheme) 72f else 55f, if (isDarkTheme) 90f else 24f)
                    val e = hslToColor(hue, if (isDarkTheme) 40f else 42f, if (isDarkTheme) 52f else 65f)
                    val m = hslToColor(hue, if (isDarkTheme) 60f else 48f, if (isDarkTheme) 70f else 40f)
                    listOf(fill, fill, ink, e, m)
                }
            }
            series == "luck_king" -> if (isDarkTheme) {
                listOf(Color(0xFF39272F), Color(0xFF422C36), Color(0xFFFFDBE0), Color(0xFFB16B79), Color(0xFFE49AA5))
            } else {
                listOf(Color(0xFFFFF5F5), Color(0xFFFFEFEF), Color(0xFF7B3041), Color(0xFFD68B94), Color(0xFFC45867))
            }
            series == "luck_last" -> if (isDarkTheme) {
                listOf(Color(0xFF30283E), Color(0xFF392F4B), Color(0xFFEEE0FF), Color(0xFF9C80BA), Color(0xFFBD9AE6))
            } else {
                listOf(Color(0xFFF8F4FF), Color(0xFFF3ECFF), Color(0xFF583880), Color(0xFFB49BD6), Color(0xFF8962BC))
            }
            else -> if (isDarkTheme) {
                listOf(Color(0xFF303223), Color(0xFF383A28), Color(0xFFF8E6B9), Color(0xFF9A824F), Color(0xFFF59E0B))
            } else {
                listOf(Color(0xFFFFFAF0), Color(0xFFFFF6E0), Color(0xFF664D22), Color(0xFFD4BD91), Color(0xFFBA9656))
            }
        }

        val borderWidth = when (lv) {
            1 -> 1.0f
            2 -> 1.3f
            3 -> 1.6f
            4 -> 1.9f
            else -> 2.2f
        }
        val safeFg = ensureReadableForeground(fg, bgStart)
        val growthBorderGrad = if (lv >= 3 || isAll) listOf(ringGold, Color(0xFFFFF1C2), ringGold, edge, ringGold) else null
        return ResolvedChatBubbleStyle(
            key = "$series:$lv",
            fillColor = bgStart,
            fillBrush = Brush.linearGradient(listOf(bgStart, bgEnd)),
            textColor = safeFg,
            linkColor = if (safeFg.luminance() > 0.5f) ringGold else edge,
            mutedColor = safeFg.copy(alpha = 0.72f),
            borderColor = edge,
            borderBrush = growthBorderGrad?.let { Brush.sweepGradient(it) },
            borderWidthDp = borderWidth,
            shapeKind = when {
                frame -> BubbleShapeKind.Rounded(7f, 7f, 7f, 7f)
                isAll -> BubbleShapeKind.Rounded(4f, 13f, 5f, 13f)
                else -> BubbleShapeKind.Rounded(5f, 12f, 12f, 12f)
            },
            shadowColor = if (lv >= 4) ringGold.copy(alpha = 0.25f) else null,
            contentPadding = if (frame) PaddingValues(horizontal = 8.dp, vertical = 3.dp) else PaddingValues(horizontal = 12.dp, vertical = 7.dp),
            borderGradientColors = growthBorderGrad,
        )
    }

    private fun resolvePersonalBubble(
        ptId: String,
        frame: Boolean,
        isDarkTheme: Boolean,
    ): ResolvedChatBubbleStyle {
        val item = BadgeAndCosmeticRegistry.resolvePersonalTitle(ptId)
        val isV2 = item?.art == "v2"
        if (isV2) {
            // `personal-v2.css`: frame 9-slice (`30% 18% 30% 18% fill`) + right mascot (`pt-character.webp`)
            // Content text is ALWAYS `#304052` over the light v2 frame artwork.
            val bg = if (frame && isDarkTheme) Color(0xFF283442) else Color(0xFFF5F8FB)
            val fg = if (frame && isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF304052)
            return ResolvedChatBubbleStyle(
                key = "personal:$ptId",
                fillColor = bg,
                textColor = ensureReadableForeground(fg, bg),
                linkColor = if (frame && isDarkTheme) Color(0xFF93C5FD) else Color(0xFF2563EB),
                mutedColor = ensureReadableForeground(fg.copy(alpha = 0.72f), bg, 3.2f),
                borderColor = if (frame) Color(0xFFA4B5C7) else Color.Transparent,
                borderWidthDp = if (frame) 1.2f else 0f,
                shapeKind = BubbleShapeKind.Rounded(0f, 0f, 0f, 0f),
                contentPadding = if (frame) {
                    PaddingValues(horizontal = 8.dp, vertical = 3.dp)
                } else {
                    PaddingValues(start = 16.dp, end = 16.dp, top = 11.dp, bottom = 11.dp)
                },
                nineSliceSpec = if (frame) null else NineSliceSpec(
                    sourceUrl = "$ORIGIN/assets/personal-v2/$ptId-frame.webp",
                    sliceTop = 0.30f,
                    sliceRight = 0.18f,
                    sliceBottom = 0.30f,
                    sliceLeft = 0.18f,
                    isPercentSlice = true,
                    fillCenter = true,
                    borderTopDp = 8f,
                    borderRightDp = 12f,
                    borderBottomDp = 8f,
                    borderLeftDp = 12f,
                ),
                ornament = if (frame) BubbleOrnament.None else BubbleOrnament.PersonalV2Mascot("$ORIGIN/assets/personal-v2/$ptId-character.webp"),
            )
        }

        // Personal v1 (`p001`..`p059`, `personal-palettes.css` + `personal.css`)
        val pal = resolvePersonalV1Palette(ptId)
        val ink = parseHexColorPure(pal.inkHex)
        val paper = parseHexColorPure(pal.paperHex)
        val accent = parseHexColorPure(pal.accentHex)
        // Even in dark mode, personal v1 uses its fixed pastel paper + `#293b4b` dark ink on web (`personal.css:5`)
        val fg = Color(0xFF293B4B)
        return ResolvedChatBubbleStyle(
            key = "personal:$ptId",
            fillColor = paper,
            textColor = ensureReadableForeground(fg, paper),
            linkColor = ink,
            mutedColor = ensureReadableForeground(fg.copy(alpha = 0.72f), paper, 3.2f),
            borderColor = ink,
            borderWidthDp = if (frame) 1f else 1.5f,
            shapeKind = if (frame) BubbleShapeKind.Rounded(10f, 10f, 10f, 10f) else BubbleShapeKind.Rounded(12f, 15f, 11f, 4f),
            shadowColor = accent.copy(alpha = 0.50f),
            shadowOffsetDp = if (frame) (0f to 0f) else (2f to 2f),
            contentPadding = if (frame) PaddingValues(horizontal = 8.dp, vertical = 3.dp) else PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ornament = if (frame) BubbleOrnament.None else BubbleOrnament.PersonalV1Mascot("$ORIGIN/assets/personal/$ptId.svg"),
        )
    }

    private fun resolveRaffleBubble(
        decoObj: JsonObject,
        raffle: BadgeAndCosmeticRegistry.ActiveRaffleInfo,
        userKey: String,
        messageId: String,
        frame: Boolean,
        isDarkTheme: Boolean,
    ): ResolvedChatBubbleStyle {
        val id = raffle.catalog.id
        val rtColor = parseHexColorPure(raffle.catalog.colorHex, Color(0xFF488CAF))
        val rtAccent = parseHexColorPure(raffle.catalog.accentHex, Color(0xFFB6DCEC))
        val stage = raffle.stage

        // Boost pill / frame (`rt-frame`)
        if (frame) {
            val bg = if (isDarkTheme) blendColors(Color(0xFF1F2A33), rtColor, 0.78f) else blendColors(Color.White, rtAccent, 0.74f)
            val fg = if (isDarkTheme) Color(0xFFEDF1F3) else blendColors(Color(0xFF212E40), rtColor, 0.45f)
            return ResolvedChatBubbleStyle(
                key = "raffle:$id",
                fillColor = bg,
                textColor = ensureReadableForeground(fg, bg),
                linkColor = rtColor,
                mutedColor = ensureReadableForeground(fg.copy(alpha = 0.72f), bg, 3.2f),
                borderColor = rtColor.copy(alpha = 0.7f),
                borderWidthDp = 1.1f,
                shapeKind = BubbleShapeKind.Rounded(8f, 12f, 12f, 4f),
                shadowColor = rtAccent.copy(alpha = 0.4f),
                shadowOffsetDp = 1.5f to 1.5f,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp),
            )
        }

        // Special Shiguang leather book (`claude`)
        if (id == "claude") {
            val fg = Color(0xFFFBF5EA)
            return ResolvedChatBubbleStyle(
                key = "raffle:claude",
                fillColor = Color(0xFF6E321B),
                fillBrush = Brush.linearGradient(listOf(Color(0xFF94492A), Color(0xFF6E321B), Color(0xFF4C2012))),
                textColor = fg,
                linkColor = Color(0xFFFFE0A1),
                mutedColor = Color(0xFFF8E3AE).copy(alpha = 0.85f),
                borderColor = if (stage >= 2) Color(0xFFF2D08E) else Color(0xFF3A180B),
                borderWidthDp = 1.3f,
                shapeKind = BubbleShapeKind.Rounded(6f, 14f, 14f, 14f),
                fontFamily = FontFamily.Serif,
                contentPadding = PaddingValues(start = 15.dp, end = 22.dp, top = 12.dp, bottom = 11.dp),
                ornament = BubbleOrnament.ShiguangBook(raffle.displayLevel),
            )
        }

        // OldFarmer commemorative paper
        if (id == "oldfarmer") {
            val bg = if (isDarkTheme) Color(0xFF2D271E) else Color(0xFFFFF8EC)
            val fg = if (isDarkTheme) Color(0xFFF5EBDD) else Color(0xFF4A3519)
            return ResolvedChatBubbleStyle(
                key = "raffle:oldfarmer",
                fillColor = bg,
                fillBrush = Brush.linearGradient(listOf(bg, blendColors(bg, rtAccent, 0.85f))),
                textColor = ensureReadableForeground(fg, bg),
                linkColor = rtColor,
                mutedColor = ensureReadableForeground(fg.copy(alpha = 0.72f), bg, 3.2f),
                borderColor = rtColor,
                borderWidthDp = 1.4f,
                shapeKind = BubbleShapeKind.Rounded(6f, 14f, 14f, 14f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            )
        }

        val slice = RAFFLE_BUBBLE_SLICES[id]
        val isCollectionBanner = slice?.isCollectionStyle == true && id != "whalebuddy"
        // Compute readable gradient paper (`title-growth.css:4, 128`, `collection.css:119`)
        val endAccentWeight = (0.10f + (stage - 1).coerceIn(0, 9) * 0.042f).coerceIn(0.10f, 0.48f)
        val (bgStart, bgEnd, rawFg) = if (id == "whalebuddy") {
            // `whalebuddy-frame.webp` fills a fixed bright sky surface even in dark theme (`collection.css:119`)
            Triple(Color(0xFFE3F4FC), Color(0xFFD0EEFB), Color(0xFF163F63))
        } else if (isCollectionBanner) {
            Triple(Color(0xFFFDFBFA), Color(0xFFF7F4FA), parseHexColorPure(slice?.inkHex ?: "#29466c"))
        } else if (id == "chickengun") {
            if (isDarkTheme) {
                Triple(Color(0xFF27313B), blendColors(Color(0xFF27313B), rtAccent, 0.24f), Color(0xFFEDF1F3))
            } else {
                Triple(Color.White, if (stage >= 7) Color(0xFFEFD4BA) else Color(0xFFFFF8F1), Color(0xFF3D2420))
            }
        } else if (isDarkTheme) {
            Triple(
                Color(0xFF25323A),
                blendColors(Color(0xFF27313B), rtAccent, 0.26f),
                Color(0xFFEDF1F3),
            )
        } else {
            Triple(
                blendColors(Color(0xFFFFFDF7), rtColor, 0.94f),
                blendColors(Color(0xFFFFFDF8), rtAccent, 1f - endAccentWeight),
                Color(0xFF1F2D27),
            )
        }

        val shape = when (id) {
            "whalebuddy" -> BubbleShapeKind.Rounded(0f, 0f, 0f, 0f)
            "gpt", "whitedragon" -> BubbleShapeKind.Rounded(20f, 14f, 18f, 16f)
            "flower", "mona" -> BubbleShapeKind.Rounded(18f, 22f, 18f, 20f)
            "deepseek", "deepseekupgrade", "maid" -> BubbleShapeKind.Rounded(22f, 16f, 22f, 16f)
            "lace", "blacksilk" -> BubbleShapeKind.Rounded(18f, 10f, 18f, 10f)
            "giant", "worship" -> BubbleShapeKind.Rounded(22f, 26f, 22f, 26f)
            "blade", "sword", "halberd", "spear" -> BubbleShapeKind.Rounded(12f, 20f, 12f, 18f)
            "cloud", "snow", "moon", "loli" -> BubbleShapeKind.Rounded(24f, 24f, 24f, 24f)
            else -> BubbleShapeKind.Rounded(4f, 12f, 12f, 12f)
        }

        val nineSlice = slice?.let { s ->
            NineSliceSpec(
                sourceUrl = "$ORIGIN${s.framePath}",
                sliceTop = s.sliceTop.toFloat(),
                sliceRight = s.sliceRight.toFloat(),
                sliceBottom = s.sliceBottom.toFloat(),
                sliceLeft = s.sliceLeft.toFloat(),
                borderTopDp = when {
                    id == "whalebuddy" -> 22f
                    s.borderTopDp != null -> s.borderTopDp
                    s.isCollectionStyle -> 22f
                    else -> 14f * s.topRatio
                },
                borderRightDp = when {
                    id == "whalebuddy" -> 44.2f
                    s.borderRightDp != null -> s.borderRightDp
                    s.isCollectionStyle -> 26f
                    else -> 14f
                },
                borderBottomDp = when {
                    id == "whalebuddy" -> 8f
                    s.borderBottomDp != null -> s.borderBottomDp
                    s.isCollectionStyle -> 10f
                    else -> 14f * s.bottomRatio
                },
                borderLeftDp = when {
                    id == "whalebuddy" -> 10.2f
                    s.borderLeftDp != null -> s.borderLeftDp
                    s.isCollectionStyle -> 40f
                    else -> 14f
                },
                isEdgeTrimOnly = !s.isCollectionStyle && id !in setOf("gpt", "gemini", "deepseek", "deepseekupgrade", "maid", "nightdevil", "whitedragon"),
            )
        }

        val ornament = when (id) {
            "whalebuddy" -> {
                val personAsset = when {
                    stage >= 8 -> "swim.webp"
                    stage >= 2 -> "greet.webp"
                    else -> "blink.webp"
                }
                BubbleOrnament.RaffleWhalebuddy("$ORIGIN/assets/raffle/whale-motion/$personAsset", stage)
            }
            "chickengun" -> {
                val cgSizeJson = decoObj["cg_size"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                val cgRandom = decoObj["cg_random"]?.jsonPrimitive?.booleanOrNull == true
                val cgMotion = decoObj["cg_motion"]?.jsonPrimitive?.booleanOrNull
                    ?: decoObj["cgMotion"]?.jsonPrimitive?.booleanOrNull
                    ?: true
                val cgFire = decoObj["cg_fire"]?.jsonPrimitive?.booleanOrNull
                    ?: decoObj["cgFire"]?.jsonPrimitive?.booleanOrNull
                    ?: true
                val sz = chickengunSizeDp(
                    level = raffle.displayLevel,
                    requested = cgSizeJson,
                    random = raffle.earnedLevel >= 5200 && cgRandom,
                    seed = "$userKey:$messageId",
                )
                BubbleOrnament.RaffleChickengun(
                    webpUrl = "$ORIGIN/stickers/heartgif/thumbs/heartgif_013.webp",
                    sizeDp = sz,
                    cgMotion = cgMotion,
                    cgFire = cgFire,
                    stage = stage,
                )
            }
            "railcannon" -> BubbleOrnament.RaffleRailcannon("$ORIGIN/assets/raffle/bubble-reading-v10/cannon.webp", stage)
            else -> BubbleOrnament.None
        }

        val safeFg = if (isCollectionBanner) rawFg else ensureReadableForeground(rawFg, bgStart)
        val hasOpaqueSlice = slice?.isCollectionStyle == true
        val raffleBorderGrad = if (!hasOpaqueSlice && stage >= 5) listOf(rtColor, rtAccent, Color(0xFFFFF4DA), rtColor) else null
        return ResolvedChatBubbleStyle(
            key = "raffle:$id",
            fillColor = bgStart,
            fillBrush = Brush.linearGradient(listOf(bgStart, bgEnd)),
            textColor = safeFg,
            linkColor = when {
                id == "whalebuddy" -> Color(0xFF1D4ED8)
                isCollectionBanner -> safeFg
                isDarkTheme -> rtAccent
                else -> blendColors(rtColor, Color.Black, 0.72f)
            },
            mutedColor = safeFg.copy(alpha = 0.72f),
            borderColor = if (hasOpaqueSlice) Color.Transparent else rtColor.copy(alpha = if (stage >= 5) 0.85f else 0.55f),
            borderBrush = raffleBorderGrad?.let { Brush.linearGradient(it) },
            borderWidthDp = if (hasOpaqueSlice) 0f else if (stage >= 7) 1.8f else 1.3f,
            shapeKind = shape,
            minWidthDp = if (id == "whalebuddy") 105f else 0f,
            contentPadding = when {
                id == "whalebuddy" -> PaddingValues(start = 17.dp, end = 24.dp, top = 25.dp, bottom = 11.dp)
                id == "chickengun" -> PaddingValues(start = 18.dp, end = 12.dp, top = 8.dp, bottom = 10.dp)
                isCollectionBanner -> PaddingValues(
                    start = (slice?.borderLeftDp ?: 22f).coerceAtLeast(14f).dp,
                    end = (slice?.borderRightDp ?: 20f).coerceAtLeast(14f).dp,
                    top = (slice?.borderTopDp ?: 10f).coerceAtLeast(8f).dp,
                    bottom = (slice?.borderBottomDp ?: 10f).coerceAtLeast(8f).dp,
                )
                else -> PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            },
            nineSliceSpec = if (id == "chickengun" || slice?.isCollectionStyle == true) nineSlice else null,
            ornament = ornament,
            borderGradientColors = raffleBorderGrad,
        )
    }

    private fun resolveOfficialBubble(
        otId: String,
        frame: Boolean,
        isDarkTheme: Boolean,
    ): ResolvedChatBubbleStyle {
        val p = OFFICIAL_PALETTES[otId] ?: OFFICIAL_PALETTES.getValue("serene")
        val ink = parseHexColorPure(p.inkHex)
        val soft = parseHexColorPure(p.softHex)
        val edge = parseHexColorPure(p.edgeHex)
        val accent = parseHexColorPure(p.accentHex)
        val safeFg = ensureReadableForeground(ink, soft)
        return ResolvedChatBubbleStyle(
            key = "official:$otId",
            fillColor = soft,
            textColor = safeFg,
            linkColor = safeFg,
            mutedColor = safeFg.copy(alpha = 0.72f),
            borderColor = edge,
            borderWidthDp = if (frame) 1.4f else 1.6f,
            dashedBorder = otId == "gentleman" && !frame,
            doubleBorder = otId == "peach" && !frame,
            leftAccentBarColor = if (!frame && (otId == "coder" || otId == "oracle")) edge else null,
            leftAccentBarWidthDp = 4f,
            bottomAccentBarColor = if (!frame && otId in setOf("serene", "banana", "whale", "diver")) accent else null,
            shapeKind = BubbleShapeKind.Rounded(p.cornersDp[0], p.cornersDp[1], p.cornersDp[2], p.cornersDp[3]),
            shadowColor = accent,
            shadowOffsetDp = if (frame) (1f to 2f) else (2f to 3f),
            fontFamily = if (otId == "coder") FontFamily.Monospace else null,
            contentPadding = if (frame) PaddingValues(horizontal = 8.dp, vertical = 3.dp) else PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        )
    }

    fun resolvePresetBubble(
        id: String,
        isDarkTheme: Boolean = false,
    ): ResolvedChatBubbleStyle {
        return when (id) {
            "aurora" -> {
                val auroraCols = listOf(
                    Color(0xFF00F0FF), Color(0xFF3B82F6), Color(0xFF8B5CF6), Color(0xFFD946EF),
                    Color(0xFFFF007F), Color(0xFFFF7700), Color(0xFFFFEA00), Color(0xFF00FF88), Color(0xFF00F0FF),
                )
                ResolvedChatBubbleStyle(
                    key = "preset:aurora",
                    fillColor = Color(0xFF131A34),
                    fillBrush = Brush.linearGradient(listOf(Color(0xFF0D1224), Color(0xFF131A34), Color(0xFF191436))),
                    textColor = Color(0xFFF8FAFC),
                    linkColor = Color(0xFF7DD3FC),
                    mutedColor = Color(0xFFCBD5E1),
                    borderColor = Color(0xFF38BDF8),
                    borderBrush = Brush.sweepGradient(auroraCols),
                    borderWidthDp = 3f,
                    shapeKind = BubbleShapeKind.Rounded(4f, 16f, 16f, 16f),
                    shadowColor = Color(0x55F43F5E),
                    shadowOffsetDp = 0f to 3f,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
                    ornament = BubbleOrnament.PresetAurora,
                    borderGradientColors = auroraCols,
                )
            }
            "jelly" -> ResolvedChatBubbleStyle(
                key = "preset:jelly",
                fillColor = Color(0xFFFFD4E8),
                fillBrush = Brush.verticalGradient(listOf(Color(0xFFFFE4F1), Color(0xFFFFC4DE))),
                textColor = Color(0xFF831843),
                linkColor = Color(0xFFBE185D),
                mutedColor = Color(0xFF9D174D),
                borderColor = Color(0xFFF9A8D4),
                borderWidthDp = 1.2f,
                shapeKind = BubbleShapeKind.Rounded(18f, 18f, 18f, 18f),
                shadowColor = Color(0xFFF9A8D4),
                shadowOffsetDp = 0f to 3f,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
                ornament = BubbleOrnament.PresetJelly,
            )
            "puff" -> ResolvedChatBubbleStyle(
                key = "preset:puff",
                fillColor = Color.White,
                textColor = Color(0xFF1E3A8A),
                linkColor = Color(0xFF1D4ED8),
                mutedColor = Color(0xFF3B82F6),
                borderColor = Color(0xFFBFDBFE),
                borderWidthDp = 2f,
                shapeKind = BubbleShapeKind.Rounded(22f, 22f, 22f, 22f),
                shadowColor = Color(0xFFDBEAFE),
                shadowOffsetDp = 0f to 3f,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
                ornament = BubbleOrnament.PresetPuff,
            )
            "paw" -> ResolvedChatBubbleStyle(
                key = "preset:paw",
                fillColor = Color(0xFFFFF7ED),
                textColor = Color(0xFF7C2D12),
                linkColor = Color(0xFFC2410C),
                mutedColor = Color(0xFF9A3412),
                borderColor = Color(0xFFFDBA74),
                borderWidthDp = 2f,
                shapeKind = BubbleShapeKind.Rounded(6f, 18f, 18f, 18f),
                shadowColor = Color(0xFFFED7AA),
                shadowOffsetDp = 0f to 3f,
                contentPadding = PaddingValues(start = 13.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                ornament = BubbleOrnament.PresetPaw,
            )
            "galaxy" -> ResolvedChatBubbleStyle(
                key = "preset:galaxy",
                fillColor = Color(0xFF312E81),
                fillBrush = Brush.linearGradient(listOf(Color(0xFF1E1B4B), Color(0xFF312E81), Color(0xFF4C1D95))),
                textColor = Color(0xFFEEF2FF),
                linkColor = Color(0xFFFDE68A),
                mutedColor = Color(0xFFC7D2FE),
                borderColor = Color(0xFF6366F1),
                borderWidthDp = 1.5f,
                shapeKind = BubbleShapeKind.Rounded(16f, 16f, 16f, 16f),
                shadowColor = Color(0x55312E81),
                shadowOffsetDp = 0f to 3f,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ornament = BubbleOrnament.PresetGalaxy,
            )
            "bit" -> ResolvedChatBubbleStyle(
                key = "preset:bit",
                fillColor = Color(0xFFC7F0A8),
                textColor = Color(0xFF14361A),
                linkColor = Color(0xFF166534),
                mutedColor = Color(0xFF1E4620),
                borderColor = Color(0xFF14361A),
                borderWidthDp = 2.5f,
                shapeKind = BubbleShapeKind.Rounded(0f, 0f, 0f, 0f),
                shadowColor = Color(0x5514361A),
                shadowOffsetDp = 3f to 4f,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                contentPadding = PaddingValues(horizontal = 13.dp, vertical = 7.dp),
                ornament = BubbleOrnament.PresetBit,
            )
            "petal" -> ResolvedChatBubbleStyle(
                key = "preset:petal",
                fillColor = Color(0xFFFFE7F0),
                fillBrush = Brush.linearGradient(listOf(Color(0xFFFFF1F6), Color(0xFFFFE1EC))),
                textColor = Color(0xFF881337),
                linkColor = Color(0xFFBE123C),
                mutedColor = Color(0xFF9F1239),
                borderColor = Color(0xFFFBCFE8),
                borderWidthDp = 1.5f,
                shapeKind = BubbleShapeKind.Rounded(18f, 18f, 18f, 6f),
                shadowColor = Color(0x33F472B6),
                shadowOffsetDp = 0f to 3f,
                contentPadding = PaddingValues(start = 14.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                ornament = BubbleOrnament.PresetPetal,
            )
            "boba" -> ResolvedChatBubbleStyle(
                key = "preset:boba",
                fillColor = Color(0xFFEFDBC2),
                fillBrush = Brush.verticalGradient(listOf(Color(0xFFF5E6D3), Color(0xFFEAD2B4))),
                textColor = Color(0xFF3F2A1D),
                linkColor = Color(0xFF78350F),
                mutedColor = Color(0xFF5C4030),
                borderColor = Color(0xFFC8A27A),
                borderWidthDp = 2f,
                shapeKind = BubbleShapeKind.Rounded(16f, 16f, 12f, 12f),
                shadowColor = Color(0xFFD6B58F),
                shadowOffsetDp = 0f to 3f,
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 12.dp),
                ornament = BubbleOrnament.PresetBoba,
            )
            "cyber" -> ResolvedChatBubbleStyle(
                key = "preset:cyber",
                fillColor = Color(0xFF0E1422),
                fillBrush = Brush.linearGradient(listOf(Color(0xFF0B0F1A), Color(0xFF111827))),
                textColor = Color(0xFFA5F3FC),
                linkColor = Color(0xFFF9A8D4),
                mutedColor = Color(0xFF67E8F9),
                borderColor = Color(0xFF22D3EE),
                borderWidthDp = 1.5f,
                leftAccentBarColor = Color(0xFFF472B6),
                leftAccentBarWidthDp = 3f,
                shapeKind = BubbleShapeKind.CutCyber(10f),
                fontFamily = FontFamily.Monospace,
                letterSpacingSp = 0.2f,
                contentPadding = PaddingValues(horizontal = 15.dp, vertical = 8.dp),
                ornament = BubbleOrnament.PresetCyber,
            )
            "prism" -> {
                val prismCols = listOf(Color(0xFFF472B6), Color(0xFFFBBF24), Color(0xFF34D399), Color(0xFF60A5FA), Color(0xFFA78BFA), Color(0xFFF472B6))
                ResolvedChatBubbleStyle(
                    key = "preset:prism",
                    fillColor = Color.White,
                    textColor = Color(0xFF1F2937),
                    linkColor = Color(0xFF6D28D9),
                    mutedColor = Color(0xFF4B5563),
                    borderColor = Color(0xFFA78BFA),
                    borderBrush = Brush.linearGradient(prismCols),
                    borderWidthDp = 2.4f,
                    shapeKind = BubbleShapeKind.Rounded(16f, 16f, 16f, 16f),
                    shadowColor = Color(0x38A78BFA),
                    shadowOffsetDp = 0f to 3f,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                    ornament = BubbleOrnament.PresetPrism,
                    borderGradientColors = prismCols,
                )
            }
            "mint" -> ResolvedChatBubbleStyle(
                key = "preset:mint",
                fillColor = Color(0xFFDDFBEE),
                fillBrush = Brush.linearGradient(listOf(Color(0xFFECFDF5), Color(0xFFD1FAE5))),
                textColor = Color(0xFF064E3B),
                linkColor = Color(0xFF047857),
                mutedColor = Color(0xFF065F46),
                borderColor = Color(0xFF6EE7B7),
                borderWidthDp = 2f,
                shapeKind = BubbleShapeKind.Rounded(18f, 6f, 18f, 18f),
                shadowColor = Color(0xFFA7F3D0),
                shadowOffsetDp = 0f to 3f,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ornament = BubbleOrnament.PresetMint,
            )
            "bunny" -> ResolvedChatBubbleStyle(
                key = "preset:bunny",
                fillColor = Color(0xFFFDFAFF),
                textColor = Color(0xFF4C1D95),
                linkColor = Color(0xFF6D28D9),
                mutedColor = Color(0xFF5B21B6),
                borderColor = Color(0xFFDDD6FE),
                borderWidthDp = 2f,
                shapeKind = BubbleShapeKind.Rounded(18f, 18f, 18f, 18f),
                shadowColor = Color(0xFFEDE9FE),
                shadowOffsetDp = 0f to 3f,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ornament = BubbleOrnament.PresetBunny,
            )
            "lemon" -> ResolvedChatBubbleStyle(
                key = "preset:lemon",
                fillColor = Color(0xFFFEF0A6),
                fillBrush = Brush.linearGradient(listOf(Color(0xFFFEF9C3), Color(0xFFFDE68A))),
                textColor = Color(0xFF713F12),
                linkColor = Color(0xFF854D0E),
                mutedColor = Color(0xFF854D0E),
                borderColor = Color(0xFFFACC15),
                borderWidthDp = 2f,
                shapeKind = BubbleShapeKind.Rounded(16f, 16f, 16f, 16f),
                shadowColor = Color(0xFFFCD34D),
                shadowOffsetDp = 0f to 3f,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ornament = BubbleOrnament.PresetLemon,
            )
            "laowu" -> ResolvedChatBubbleStyle(
                key = "preset:laowu",
                fillColor = Color(0xFFFFEBA8),
                fillBrush = Brush.linearGradient(listOf(Color(0xFFFFF6D6), Color(0xFFFFE08A))),
                textColor = Color(0xFF5A3B00),
                linkColor = Color(0xFFB45309),
                mutedColor = Color(0xFF78350F),
                borderColor = Color(0xFFE0A800),
                borderWidthDp = 2f,
                shapeKind = BubbleShapeKind.Rounded(4f, 16f, 16f, 16f),
                shadowColor = Color(0x55E0A800),
                shadowOffsetDp = 3f to 3f,
                fontWeight = FontWeight.Medium,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ornament = BubbleOrnament.PresetLaowu,
            )
            "cloud" -> ResolvedChatBubbleStyle(
                key = "preset:cloud",
                fillColor = Color(0xFFEEF6FF),
                textColor = Color(0xFF1D4E89),
                linkColor = Color(0xFF1E40AF),
                mutedColor = Color(0xFF3B6EA8),
                borderColor = Color(0xFFC9E0FF),
                borderWidthDp = 1.2f,
                shapeKind = BubbleShapeKind.Rounded(20f, 20f, 20f, 20f),
                shadowColor = Color(0xFFC9E0FF),
                shadowOffsetDp = 0f to 2f,
                contentPadding = PaddingValues(horizontal = 13.dp, vertical = 8.dp),
            )
            "candy" -> ResolvedChatBubbleStyle(
                key = "preset:candy",
                fillColor = Color(0xFFFFEBF4),
                textColor = Color(0xFFB0306A),
                linkColor = Color(0xFF9D174D),
                mutedColor = Color(0xFF9D174D),
                borderColor = Color(0xFFFF9AC4),
                borderWidthDp = 1.5f,
                shapeKind = BubbleShapeKind.Rounded(16f, 16f, 16f, 16f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ornament = BubbleOrnament.PresetCandyStripes,
            )
            "neon" -> ResolvedChatBubbleStyle(
                key = "preset:neon",
                fillColor = Color(0xFF0D0F1A),
                textColor = Color(0xFF9FF7FF),
                linkColor = Color(0xFF67E8F9),
                mutedColor = Color(0xFF67E8F9),
                borderColor = Color(0xFF37E8FF),
                borderWidthDp = 1.5f,
                shapeKind = BubbleShapeKind.Rounded(10f, 10f, 10f, 10f),
                shadowColor = Color(0x9937E8FF),
                shadowOffsetDp = 0f to 2f,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            )
            "paper" -> ResolvedChatBubbleStyle(
                key = "preset:paper",
                fillColor = Color(0xFFFFFBE6),
                textColor = Color(0xFF4A4030),
                linkColor = Color(0xFF78350F),
                mutedColor = Color(0xFF6B5E48),
                borderColor = Color(0xFFE9DFB8),
                borderWidthDp = 1f,
                shapeKind = BubbleShapeKind.Rounded(2f, 2f, 2f, 2f),
                shadowColor = Color(0x20000000),
                shadowOffsetDp = 2f to 3f,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ornament = BubbleOrnament.PresetPaperLines,
            )
            "ink" -> ResolvedChatBubbleStyle(
                key = "preset:ink",
                fillColor = Color(0xFFF6F3EE),
                textColor = Color(0xFF222222),
                linkColor = Color(0xFF1E293B),
                mutedColor = Color(0xFF4B5563),
                borderColor = Color(0xFFD6D3D1),
                borderWidthDp = 0.8f,
                leftAccentBarColor = Color(0xFF222222),
                leftAccentBarWidthDp = 4f,
                shapeKind = BubbleShapeKind.Rounded(3f, 14f, 3f, 14f),
                fontFamily = FontFamily.Serif,
                contentPadding = PaddingValues(start = 14.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            )
            "pixel" -> ResolvedChatBubbleStyle(
                key = "preset:pixel",
                fillColor = Color.White,
                textColor = Color(0xFF222222),
                linkColor = Color(0xFF1D4ED8),
                mutedColor = Color(0xFF4B5563),
                borderColor = Color(0xFF222222),
                borderWidthDp = 2.5f,
                shapeKind = BubbleShapeKind.Rounded(0f, 0f, 0f, 0f),
                shadowColor = Color(0xFF222222),
                shadowOffsetDp = 3.5f to 3.5f,
                fontFamily = FontFamily.Monospace,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
            )
            "sakura" -> ResolvedChatBubbleStyle(
                key = "preset:sakura",
                fillColor = Color(0xFFFFE3EE),
                fillBrush = Brush.linearGradient(listOf(Color(0xFFFFF0F5), Color(0xFFFFD6E7))),
                textColor = Color(0xFF9B2C5A),
                linkColor = Color(0xFFBE185D),
                mutedColor = Color(0xFF9D174D),
                borderColor = Color(0xFFFFB6D1),
                borderWidthDp = 1.2f,
                shapeKind = BubbleShapeKind.Rounded(16f, 16f, 16f, 4f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ornament = BubbleOrnament.PresetSakura,
            )
            "gold" -> ResolvedChatBubbleStyle(
                key = "preset:gold",
                fillColor = Color(0xFF533C00),
                fillBrush = Brush.linearGradient(listOf(Color(0xFF3A2A00), Color(0xFF6B4E00), Color(0xFF3A2A00))),
                textColor = Color(0xFFFFE27A),
                linkColor = Color(0xFFFEF08A),
                mutedColor = Color(0xFFFCD34D),
                borderColor = Color(0xFFD4A017),
                borderWidthDp = 1.5f,
                shapeKind = BubbleShapeKind.Rounded(12f, 12f, 12f, 12f),
                shadowColor = Color(0x55D4A017),
                shadowOffsetDp = 0f to 2f,
                fontWeight = FontWeight.SemiBold,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            )
            "night" -> ResolvedChatBubbleStyle(
                key = "preset:night",
                fillColor = Color(0xFF2B245B),
                fillBrush = Brush.linearGradient(listOf(Color(0xFF1B1E4B), Color(0xFF3B2A6B))),
                textColor = Color(0xFFE8E6FF),
                linkColor = Color(0xFFC4B5FD),
                mutedColor = Color(0xFFC7D2FE),
                borderColor = Color(0xFF5B4899),
                borderWidthDp = 1f,
                shapeKind = BubbleShapeKind.Rounded(14f, 14f, 14f, 14f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ornament = BubbleOrnament.PresetNightStars,
            )
            "yinlu" -> {
                val bg = if (isDarkTheme) Color(0xFF273E2D) else Color(0xFFF1F8DC)
                val fg = if (isDarkTheme) Color(0xFFD6EDB3) else Color(0xFF31482D)
                val sh = if (isDarkTheme) Color(0xFF577748) else Color(0xFFBDD595)
                ResolvedChatBubbleStyle(
                    key = "preset:yinlu",
                    fillColor = bg,
                    textColor = fg,
                    linkColor = fg,
                    mutedColor = fg.copy(alpha = 0.72f),
                    borderColor = Color(0xFF94B669),
                    borderWidthDp = 1.2f,
                    dashedBorder = true,
                    shapeKind = BubbleShapeKind.Rounded(16f, 16f, 16f, 5f),
                    shadowColor = sh,
                    shadowOffsetDp = 3f to 3f,
                    contentPadding = PaddingValues(start = 12.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
                    ornament = BubbleOrnament.PresetYinlu,
                )
            }
            else -> ResolvedChatBubbleStyle(
                key = "preset:$id",
                fillColor = if (isDarkTheme) Color(0xFF1C3028) else Color(0xFFFAF9F5),
                textColor = if (isDarkTheme) Color(0xFFECFDF5) else Color(0xFF172D25),
                linkColor = Color(0xFF2D6A4F),
                mutedColor = if (isDarkTheme) Color(0xFFA7F3D0) else Color(0xFF52796F),
                borderColor = if (isDarkTheme) Color(0xFF2D4A3E) else Color(0xFFD9DFD5),
                borderWidthDp = 1f,
            )
        }
    }

    private fun applyCustomCssOverride(
        base: ResolvedChatBubbleStyle,
        css: ParsedSafeCss,
    ): ResolvedChatBubbleStyle {
        val nextBg = css.bgColor ?: base.fillColor
        val nextBrush = css.bgGradientColors?.let { Brush.linearGradient(it) } ?: if (css.bgColor != null) null else base.fillBrush
        val preferredFg = css.textColor ?: base.textColor
        val safeFg = ensureReadableForeground(preferredFg, nextBg)
        val nextShape = when {
            css.cornerRadiiDp != null && css.cornerRadiiDp.size >= 4 -> BubbleShapeKind.Rounded(
                css.cornerRadiiDp[0], css.cornerRadiiDp[1], css.cornerRadiiDp[2], css.cornerRadiiDp[3],
            )
            css.cornerRadiusDp != null -> BubbleShapeKind.Rounded(
                css.cornerRadiusDp, css.cornerRadiusDp, css.cornerRadiusDp, css.cornerRadiusDp,
            )
            else -> base.shapeKind
        }
        return base.copy(
            key = "custom",
            fillColor = nextBg,
            fillBrush = nextBrush,
            textColor = safeFg,
            linkColor = if (css.bgColor != null || css.textColor != null) safeFg else base.linkColor,
            mutedColor = ensureReadableForeground(safeFg.copy(alpha = 0.72f), nextBg, 3.2f),
            borderColor = css.borderColor ?: base.borderColor,
            borderBrush = if (css.borderColor != null) null else base.borderBrush,
            borderWidthDp = css.borderWidthDp ?: base.borderWidthDp,
            shapeKind = nextShape,
            fontWeight = css.fontWeight ?: base.fontWeight,
            fontStyle = css.fontStyle ?: base.fontStyle,
            letterSpacingSp = css.letterSpacingSp ?: base.letterSpacingSp,
            opacity = css.opacity ?: base.opacity,
        )
    }

    private fun buildStandaloneCustomCssBubble(
        css: ParsedSafeCss,
        isDarkTheme: Boolean,
    ): ResolvedChatBubbleStyle {
        val defaultBg = if (isDarkTheme) Color(0xFF1D3229) else Color(0xFFFFFFFF)
        val defaultFg = if (isDarkTheme) Color(0xFFECFDF5) else Color(0xFF172D25)
        val defaultBorder = if (isDarkTheme) Color(0xFF2D4A3E) else Color(0xFFD9DFD5)
        val bg = css.bgColor ?: defaultBg
        val brush = css.bgGradientColors?.let { Brush.linearGradient(it) }
        val fg = ensureReadableForeground(css.textColor ?: defaultFg, bg)
        val shape = when {
            css.cornerRadiiDp != null && css.cornerRadiiDp.size >= 4 -> BubbleShapeKind.Rounded(
                css.cornerRadiiDp[0], css.cornerRadiiDp[1], css.cornerRadiiDp[2], css.cornerRadiiDp[3],
            )
            css.cornerRadiusDp != null -> BubbleShapeKind.Rounded(
                css.cornerRadiusDp, css.cornerRadiusDp, css.cornerRadiusDp, css.cornerRadiusDp,
            )
            else -> BubbleShapeKind.Rounded(15f, 15f, 15f, 15f)
        }
        return ResolvedChatBubbleStyle(
            key = "custom",
            fillColor = bg,
            fillBrush = brush,
            textColor = fg,
            linkColor = fg,
            mutedColor = ensureReadableForeground(fg.copy(alpha = 0.72f), bg, 3.2f),
            borderColor = css.borderColor ?: defaultBorder,
            borderWidthDp = css.borderWidthDp ?: 1f,
            shapeKind = shape,
            fontWeight = css.fontWeight,
            fontStyle = css.fontStyle,
            letterSpacingSp = css.letterSpacingSp,
            opacity = css.opacity ?: 1f,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}
