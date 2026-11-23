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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

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

data class RaffleBubbleSliceSpec(
    val framePath: String,
    val sliceTop: Int,
    val sliceRight: Int,
    val sliceBottom: Int,
    val sliceLeft: Int,
    val topRatio: Float,
    val bottomRatio: Float,
    val isCollectionStyle: Boolean = false,
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
    data object LantingInk : BubbleOrnament
    data class P3rZeroHour(val spriteSheetUrl: String) : BubbleOrnament
    data class ShiguangBook(val level: Int) : BubbleOrnament
    data class PersonalV1Mascot(val svgUrl: String) : BubbleOrnament
    data class PersonalV2Mascot(val webpUrl: String) : BubbleOrnament
    data class RaffleChickengun(val webpUrl: String, val sizeDp: Float) : BubbleOrnament
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
    val contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    val nineSliceSpec: NineSliceSpec? = null,
    val ornament: BubbleOrnament = BubbleOrnament.None,
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

    val RAFFLE_BUBBLE_SLICES: Map<String, RaffleBubbleSliceSpec> = mapOf(
        "gpt" to RaffleBubbleSliceSpec("/assets/raffle/bubble-anime-v9/gpt.webp", 35, 140, 20, 140, 0.2500f, 0.1429f),
        "claude" to RaffleBubbleSliceSpec("/assets/raffle/bubble-anime-v9/claude.webp", 35, 140, 20, 140, 0.2500f, 0.1429f),
        "gemini" to RaffleBubbleSliceSpec("/assets/raffle/bubble-anime-v9/gemini.webp", 35, 140, 20, 140, 0.2500f, 0.1429f),
        "deepseek" to RaffleBubbleSliceSpec("/assets/raffle/bubble-anime-v9/deepseek.webp", 35, 140, 20, 140, 0.2500f, 0.1429f),
        "deepseekupgrade" to RaffleBubbleSliceSpec("/assets/raffle/bubble-anime-v9/deepseekupgrade.webp", 35, 140, 20, 140, 0.2500f, 0.1429f),
        "maid" to RaffleBubbleSliceSpec("/assets/raffle/bubble-anime-v9/maid.webp", 35, 140, 20, 140, 0.2500f, 0.1429f),
        "nightdevil" to RaffleBubbleSliceSpec("/assets/raffle/bubble-anime-v9/nightdevil.webp", 35, 140, 20, 140, 0.2500f, 0.1429f),
        "whitedragon" to RaffleBubbleSliceSpec("/assets/raffle/bubble-anime-v9/whitedragon.webp", 35, 140, 20, 140, 0.2500f, 0.1429f),
        "newcomer" to RaffleBubbleSliceSpec("/assets/raffle/bubble-growth-v8/newcomer.webp", 64, 128, 61, 128, 0.5000f, 0.4766f),
        "mona" to RaffleBubbleSliceSpec("/assets/raffle/bubble-growth-v8/mona.webp", 80, 128, 46, 128, 0.6250f, 0.3594f),
        "lace" to RaffleBubbleSliceSpec("/assets/raffle/bubble-growth-v8/lace.webp", 69, 128, 56, 128, 0.5391f, 0.4375f),
        "giant" to RaffleBubbleSliceSpec("/assets/raffle/bubble-growth-v8/giant.webp", 55, 128, 70, 128, 0.4297f, 0.5469f),
        "worship" to RaffleBubbleSliceSpec("/assets/raffle/bubble-growth-v8/worship.webp", 57, 128, 69, 128, 0.4453f, 0.5391f),
        "muscle" to RaffleBubbleSliceSpec("/assets/raffle/bubble-growth-v8/muscle.webp", 53, 128, 72, 128, 0.4141f, 0.5625f),
        "whalepunch" to RaffleBubbleSliceSpec("/assets/raffle/bubble-growth-v8/whalepunch.webp", 60, 128, 65, 128, 0.4688f, 0.5078f),
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

    private data class CommunityTitlePalette(
        val bgHex: String,
        val secondaryBgHex: String? = null,
        val textHex: String,
        val borderHex: String,
        val shape: String = "pill",
        val fillMode: String = "grad",
        val effect: String = "none",
        val fontKey: String = "",
        val bgImageUrl: String? = null,
    )

    private val COMMUNITY_TITLE_PALETTES: Map<String, CommunityTitlePalette> = mapOf(
        "aurora-preset" to CommunityTitlePalette("#2e1065", "#0c4a6e", "#fdf4ff", "#00f0ff", "ribbon", "grad", "aurora"),
        "lanting-ink" to CommunityTitlePalette("#fbf8ef", "#ebe0c7", "#1c1917", "#b48327", "tag", "grad", "flow", "xing"),
        "laoda-purple" to CommunityTitlePalette("#665391", "#473c75", "#fff3db", "#8976ad", "pill", "grad"),
        "laowu-dispatch" to CommunityTitlePalette("#fff1d9", "#ffdfad", "#67371f", "#be7542", "rect", "grad"),
        "laowu-dispatch-all-in" to CommunityTitlePalette("#e5a254", "#3d5e7a", "#fff3d3", "#a47b43", "rect", "grad"),
        "laowu-dispatch-black" to CommunityTitlePalette("#39372b", "#24231e", "#fff0c7", "#a27a42", "rect", "grad"),
        "laowu-dispatch-calico" to CommunityTitlePalette("#fffaf0", "#f5e6d3", "#352921", "#926747", "rect", "grad"),
        "laowu-dispatch-gray" to CommunityTitlePalette("#e7e7ec", "#c6c7d1", "#2d2739", "#858597", "rect", "grad"),
        "laowu-dispatch-siamese" to CommunityTitlePalette("#fcf2df", "#d7e9f0", "#354856", "#86a4b3", "rect", "grad"),
        "laowu-dispatch-white" to CommunityTitlePalette("#fffefa", "#e5e6eb", "#292b34", "#626574", "rect", "grad"),
        "oiia-loaf" to CommunityTitlePalette("#293536", "#151e21", "#deffb6", "#567b62", "tag", "grad"),
        "orange-flick" to CommunityTitlePalette("#fff0da", "#ffe4c2", "#733e23", "#dfa16f", "tag", "soft"),
        "p3r-zero-hour-fool" to CommunityTitlePalette("#0734b8", "#031a5c", "#ffffff", "#52d6ff", "ribbon", "grad", "shine"),
        "round-maodie" to CommunityTitlePalette("#fff1d2", "#e8c491", "#512f1e", "#9b6a3d", "pill", "grad"),
        "shadow-dao" to CommunityTitlePalette("#f3efe6", "#d8c8a8", "#141418", "#8c703b", "ribbon", "grad"),
        "shadow-path" to CommunityTitlePalette("#eef4f7", "#cbdbe2", "#0f1418", "#5b7c8a", "ribbon", "grad"),
        "waibi-babu" to CommunityTitlePalette("#fff0a9", "#ffcf62", "#543510", "#bc8331", "pill", "grad"),
    )

    internal fun resolveCommunityTitleStyle(
        id: String,
        skin: CommunitySkinCatalogItem?,
        isDarkTheme: Boolean = false,
    ): ResolvedTitleChip {
        val label = skin?.name ?: id
        val p = COMMUNITY_TITLE_PALETTES[id]
        val titleImg = skin?.titleImgSha?.let { "$ORIGIN/api/community-skins/image/$it" }
        return if (p != null) {
            val (bg, secBg, fg, border) = if (isDarkTheme && id == "lanting-ink") {
                listOf("#18191e", "#201e24", "#fef3c7", "#d4af37")
            } else if (isDarkTheme && id == "orange-flick") {
                listOf("#553121", "#442619", "#ffeddb", "#bd805e")
            } else {
                listOf(p.bgHex, p.secondaryBgHex ?: p.bgHex, p.textHex, p.borderHex)
            }
            ResolvedTitleChip(
                key = "community:$id",
                label = label,
                rarity = "normal",
                bgHex = bg,
                textHex = fg,
                borderHex = border,
                fillMode = p.fillMode,
                shape = p.shape,
                secondaryBgHex = secBg,
                effect = p.effect,
                fontKey = p.fontKey,
                bgImageUrl = titleImg,
                specialKind = "community",
            )
        } else {
            ResolvedTitleChip(
                key = "community:$id",
                label = label,
                rarity = "normal",
                bgHex = "#2D6A4F",
                textHex = "#FFFFFF",
                borderHex = "#52B788",
                fillMode = "solid",
                shape = "pill",
                bgImageUrl = titleImg,
                specialKind = "community",
            )
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
            val nums = Regex("""[\d.]+""").findAll(s).mapNotNull { it.value.toFloatOrNull() }.toList()
            if (nums.size >= 3) {
                val r = (nums[0] / 255f).coerceIn(0f, 1f)
                val g = (nums[1] / 255f).coerceIn(0f, 1f)
                val b = (nums[2] / 255f).coerceIn(0f, 1f)
                val a = nums.getOrNull(3)?.coerceIn(0f, 1f) ?: 1f
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
        val sc = obj["sc"]?.jsonPrimitive?.contentOrNull ?: obj["decoScope"]?.jsonPrimitive?.contentOrNull ?: "all"
        if (where != "card" && sc != "all" && sc != where) return null

        val ptId = obj["pt"]?.jsonPrimitive?.contentOrNull ?: obj["equippedPersonalId"]?.jsonPrimitive?.contentOrNull
        // 1. owner52 suit
        if (BadgeAndCosmeticRegistry.isOwner52Suit(ptId)) {
            return resolveOwner52Bubble(ptId!!, userKey, messageId, frame, isDarkTheme)
        }

        // 2. Community bubble / reply skin (`meme-hall-skins`)
        val commObj = runCatching { obj["community"]?.jsonObject }.getOrNull()
        val commKey = if (frame) "reply" else "bubble"
        val commId = commObj?.get(commKey)?.jsonPrimitive?.contentOrNull
            ?: if (!frame) obj["communityBubbleId"]?.jsonPrimitive?.contentOrNull else null
        if (!commId.isNullOrBlank()) {
            val resolvedComm = resolveCommunityBubble(commId, frame, isDarkTheme)
            if (resolvedComm != null) return resolvedComm
        }

        // 3. aurora preset (`!frame && bb == 'aurora'`)
        val bb = (obj["bb"]?.jsonPrimitive?.contentOrNull ?: obj["bubbleSkin"]?.jsonPrimitive?.contentOrNull).orEmpty()
        if (!frame && bb == "aurora") {
            return resolvePresetBubble("aurora", isDarkTheme)
        }

        // 4. Growth / Luck / Rank bubble (`gr`)
        val grId = obj["gr"]?.jsonPrimitive?.contentOrNull ?: obj["equippedGrowthId"]?.jsonPrimitive?.contentOrNull
        if (!grId.isNullOrBlank()) {
            BadgeAndCosmeticRegistry.resolveGrowthRewardMeta(grId)?.let { meta ->
                return resolveGrowthBubble(meta, frame, isDarkTheme)
            }
        }

        // 5. Personal suit bubble (`pt`)
        if (!ptId.isNullOrBlank() && BadgeAndCosmeticRegistry.PERSONAL_TITLES[ptId] != null) {
            return resolvePersonalBubble(ptId, frame, isDarkTheme)
        }

        // 6. Raffle character suit bubble (`ft`)
        val activeRaffle = BadgeAndCosmeticRegistry.resolveActiveRaffle(obj, cosmeticAccess, nowSec)
        if (activeRaffle != null) {
            return resolveRaffleBubble(obj, activeRaffle, userKey, messageId, frame, isDarkTheme)
        }

        // 7. Official title bubble (`ot`)
        val otId = obj["ot"]?.jsonPrimitive?.contentOrNull ?: obj["equippedOfficialId"]?.jsonPrimitive?.contentOrNull
        if (!otId.isNullOrBlank() && BadgeAndCosmeticRegistry.resolveOfficialTitleName(otId) != null && OFFICIAL_PALETTES.containsKey(otId)) {
            return resolveOfficialBubble(otId, frame, isDarkTheme)
        }

        if (frame) return null

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
        return ResolvedChatBubbleStyle(
            key = "personal:$ptId",
            fillColor = bgStart,
            fillBrush = Brush.linearGradient(listOf(bgStart, bgEnd)),
            textColor = ensureReadableForeground(fg, bgStart),
            linkColor = if (isDarkTheme) Color(0xFFBAE6FD) else border,
            mutedColor = ensureReadableForeground(fg.copy(alpha = 0.72f), bgStart, 3.2f),
            borderColor = border,
            borderBrush = Brush.sweepGradient(listOf(border, parseHexColorPure(accentHex), border, border.copy(alpha = 0.75f), border)),
            borderWidthDp = if (frame) 1.2f else 1.5f,
            shapeKind = if (frame) BubbleShapeKind.Rounded(999f, 999f, 999f, 999f) else corners,
            shadowColor = border.copy(alpha = 0.25f),
            contentPadding = if (frame) PaddingValues(horizontal = 8.dp, vertical = 3.dp) else PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ornament = if (frame) BubbleOrnament.None else BubbleOrnament.Owner52(idx, isBVariant = ptId.contains("52b", ignoreCase = true)),
        )
    }

    private fun resolveCommunityBubble(
        id: String,
        frame: Boolean,
        isDarkTheme: Boolean,
    ): ResolvedChatBubbleStyle? {
        val skin = BadgeAndCosmeticRegistry.resolveCommunitySkinMeta(id)
        if (id == "aurora-preset") {
            return if (frame) {
                val bg = if (isDarkTheme) Color(0xFF161E38) else Color(0xFFEEF4FF)
                val fg = if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF1E293B)
                ResolvedChatBubbleStyle(
                    key = "community:$id",
                    fillColor = bg,
                    textColor = fg,
                    linkColor = Color(0xFF38BDF8),
                    mutedColor = fg.copy(alpha = 0.75f),
                    borderBrush = Brush.linearGradient(listOf(Color(0xFF00F0FF), Color(0xFF8B5CF6), Color(0xFFFF007F))),
                    borderColor = Color(0xFF8B5CF6),
                    borderWidthDp = 1.4f,
                    shapeKind = BubbleShapeKind.Rounded(4f, 12f, 12f, 4f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp),
                )
            } else {
                resolvePresetBubble("aurora", isDarkTheme).copy(key = "community:$id")
            }
        }
        if (id == "lanting-ink") {
            val bg1 = if (isDarkTheme) Color(0xFF18191E) else Color(0xFFFBF8EF)
            val bg2 = if (isDarkTheme) Color(0xFF201E24) else Color(0xFFEBE0C7)
            val fg = if (isDarkTheme) Color(0xFFFEF3C7) else Color(0xFF1C1917)
            val gold = if (isDarkTheme) Color(0xFFD4AF37) else Color(0xFFB48327)
            return ResolvedChatBubbleStyle(
                key = "community:$id",
                fillColor = bg1,
                fillBrush = Brush.linearGradient(listOf(bg1, bg2)),
                textColor = fg,
                linkColor = gold,
                mutedColor = fg.copy(alpha = 0.72f),
                borderColor = gold,
                borderBrush = Brush.linearGradient(listOf(Color(0xFF111827), gold, Color(0xFF991B1B), gold)),
                borderWidthDp = if (frame) 1.6f else 2f,
                leftAccentBarColor = Color(0xFF111827),
                leftAccentBarWidthDp = 4f,
                shapeKind = BubbleShapeKind.Rounded(4f, 16f, 6f, 16f),
                fontFamily = FontFamily.Serif,
                contentPadding = if (frame) PaddingValues(horizontal = 9.dp, vertical = 3.dp) else PaddingValues(start = 15.dp, end = 20.dp, top = 10.dp, bottom = 10.dp),
                ornament = if (frame) BubbleOrnament.None else BubbleOrnament.LantingInk,
            )
        }
        if (id == "orange-flick") {
            val bg = if (isDarkTheme) Color(0xFF553121) else Color(0xFFFFF0DA)
            val fg = if (isDarkTheme) Color(0xFFFFEDDB) else Color(0xFF733E23)
            val edge = if (isDarkTheme) Color(0xFFBD805E) else Color(0xFFDFA16F)
            val pink = if (isDarkTheme) Color(0xFFCB6B93) else Color(0xFFF1A5C5)
            return ResolvedChatBubbleStyle(
                key = "community:$id",
                fillColor = bg,
                textColor = fg,
                linkColor = if (isDarkTheme) Color(0xFFFED7AA) else Color(0xFF9A3412),
                mutedColor = fg.copy(alpha = 0.75f),
                borderColor = edge,
                borderWidthDp = 1.2f,
                bottomAccentBarColor = pink,
                shapeKind = BubbleShapeKind.Rounded(7f, 18f, 18f, 7f),
                shadowColor = pink,
                shadowOffsetDp = 1.5f to 2.5f,
                contentPadding = if (frame) PaddingValues(horizontal = 8.dp, vertical = 3.dp) else PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            )
        }
        if (id == "p3r-zero-hour-fool" && !frame) {
            val imgUrl = skin?.bubbleImgSha?.let { "$ORIGIN/api/community-skins/image/$it" }
                ?: "$ORIGIN/api/community-skins/image/0a4fbf591d0059052bccc9baef213b050426ea90d02c154afe011e407e227245"
            val bg = Color(0xFF0734B8)
            val fg = Color(0xFFF6FDFF)
            return ResolvedChatBubbleStyle(
                key = "community:$id",
                fillColor = bg,
                fillBrush = Brush.linearGradient(listOf(Color(0xFF0734B8), Color(0xFF052794))),
                textColor = fg,
                linkColor = Color(0xFF93E9FF),
                mutedColor = Color(0xFFBAE6FD),
                borderColor = Color(0xFF52D6FF),
                borderWidthDp = 2f,
                shapeKind = BubbleShapeKind.Rounded(4f, 14f, 14f, 14f),
                contentPadding = PaddingValues(start = 42.dp, end = 14.dp, top = 9.dp, bottom = 9.dp),
                ornament = BubbleOrnament.P3rZeroHour(imgUrl),
            )
        }

        // Image-based 9-slice community skins (shadow-dao, shadow-path) or CSS-embedded data-URI 9-slice skins (laowu*, laoda, oiia, round-maodie, waibi-babu)
        if (skin != null) {
            val commKey = if (frame) "reply" else "bubble"
            val imgSha = if (frame) skin.replyImgSha else skin.bubbleImgSha
            val cssSha = if (frame) skin.replyCssSha else skin.bubbleCssSha
            val sliceArr = if (frame) skin.replySlice else skin.bubbleSlice
            val isDarkSkinVariant = isDarkTheme && (id == "shadow-dao" || id == "shadow-path") && cssSha != null
            val sourceUrl = when {
                isDarkSkinVariant -> "community-css-dark:$id:$commKey:$cssSha"
                imgSha != null -> "$ORIGIN/api/community-skins/image/$imgSha"
                cssSha != null -> "community-css:$id:$commKey:$cssSha"
                else -> null
            }
            if (sourceUrl != null) {
                val m = communityNineSliceMetrics(id, frame, sliceArr, isDarkTheme)
                val font = if (id == "shadow-dao" || id == "shadow-path") FontFamily.SansSerif else null
                return ResolvedChatBubbleStyle(
                    key = "community:$id",
                    fillColor = m.fallbackBg,
                    textColor = m.textColor,
                    linkColor = if (m.textColor.luminance() > 0.5f) Color(0xFFBAE6FD) else Color(0xFF1E40AF),
                    mutedColor = m.textColor.copy(alpha = 0.72f),
                    borderColor = Color.Transparent,
                    shapeKind = BubbleShapeKind.Rounded(0f, 0f, 0f, 0f),
                    fontFamily = font,
                    minWidthDp = if (frame) 0f else 72f,
                    contentPadding = PaddingValues(
                        start = m.pLeft.dp,
                        end = m.pRight.dp,
                        top = m.pTop.dp,
                        bottom = m.pBottom.dp,
                    ),
                    nineSliceSpec = NineSliceSpec(
                        sourceUrl = sourceUrl,
                        sliceTop = m.sTop.toFloat(),
                        sliceRight = m.sRight.toFloat(),
                        sliceBottom = m.sBottom.toFloat(),
                        sliceLeft = m.sLeft.toFloat(),
                        borderTopDp = m.bTop,
                        borderRightDp = m.bRight,
                        borderBottomDp = m.bBottom,
                        borderLeftDp = m.bLeft,
                    ),
                )
            }
        }

        // Fallback tinted pill for unrecognized dynamic /api/community-skins entries
        if (skin != null) {
            val bg = if (isDarkTheme) Color(0xFF233029) else Color(0xFFECFDF5)
            val fg = if (isDarkTheme) Color(0xFFECFDF5) else Color(0xFF064E3B)
            return ResolvedChatBubbleStyle(
                key = "community:$id",
                fillColor = bg,
                textColor = fg,
                linkColor = Color(0xFF10B981),
                mutedColor = fg.copy(alpha = 0.7f),
                borderColor = Color(0xFF52B788),
                borderWidthDp = 1.2f,
                shapeKind = BubbleShapeKind.Rounded(12f, 12f, 12f, 12f),
                contentPadding = if (frame) PaddingValues(horizontal = 8.dp, vertical = 3.dp) else PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            )
        }
        return null
    }

    private data class CommunityNineSliceTuple(
        val sTop: Int,
        val sRight: Int,
        val sBottom: Int,
        val sLeft: Int,
        val bTop: Float,
        val bRight: Float,
        val bBottom: Float,
        val bLeft: Float,
        val pTop: Float,
        val pRight: Float,
        val pBottom: Float,
        val pLeft: Float,
        val fallbackBg: Color,
        val textColor: Color,
    )

    private fun communityNineSliceMetrics(
        id: String,
        frame: Boolean,
        sliceArr: IntArray?,
        isDarkTheme: Boolean,
    ): CommunityNineSliceTuple = when (id) {
        "laoda-purple" -> if (frame) {
            CommunityNineSliceTuple(103, 95, 56, 51, 10f, 14f, 8f, 12f, 4f, 12f, 4f, 12f, Color(0xFF534082), Color(0xFFFFFDF5))
        } else {
            CommunityNineSliceTuple(103, 95, 56, 51, 24f, 22f, 13f, 14f, 12f, 25f, 13f, 17f, Color(0xFF534082), Color(0xFFFFFDF5))
        }
        "laowu-dispatch" -> if (frame) {
            CommunityNineSliceTuple(75, 70, 47, 64, 12f, 16f, 8f, 14f, 4f, 12f, 4f, 12f, Color(0xFFFFECD1), Color(0xFF332215))
        } else {
            CommunityNineSliceTuple(75, 70, 47, 64, 30f, 28f, 19f, 26f, 11f, 31f, 17f, 29f, Color(0xFFFFECD1), Color(0xFF332215))
        }
        "laowu-dispatch-all-in" -> if (frame) {
            CommunityNineSliceTuple(53, 49, 33, 45, 12f, 16f, 8f, 14f, 4f, 12f, 4f, 12f, Color(0xFFFFECD1), Color(0xFF332215))
        } else {
            CommunityNineSliceTuple(53, 49, 33, 45, 30f, 28f, 19f, 26f, 11f, 31f, 17f, 29f, Color(0xFFFFECD1), Color(0xFF332215))
        }
        "laowu-dispatch-black" -> if (frame) {
            CommunityNineSliceTuple(101, 79, 54, 68, 12f, 14f, 8f, 12f, 4f, 12f, 4f, 12f, Color(0xFF28251F), Color(0xFFFFF2CF))
        } else {
            CommunityNineSliceTuple(101, 79, 54, 68, 28f, 22f, 15f, 19f, 12f, 25f, 17f, 22f, Color(0xFF28251F), Color(0xFFFFF2CF))
        }
        "laowu-dispatch-calico" -> if (frame) {
            CommunityNineSliceTuple(112, 99, 66, 64, 12f, 16f, 8f, 12f, 4f, 12f, 4f, 12f, Color(0xFFF7EBD9), Color(0xFF211F24))
        } else {
            CommunityNineSliceTuple(112, 99, 66, 64, 33f, 30f, 16f, 18f, 17f, 33f, 14f, 21f, Color(0xFFF7EBD9), Color(0xFF211F24))
        }
        "laowu-dispatch-gray" -> if (frame) {
            CommunityNineSliceTuple(59, 58, 86, 92, 10f, 14f, 10f, 16f, 4f, 12f, 4f, 12f, Color(0xFFE7E7EC), Color(0xFF101219))
        } else {
            CommunityNineSliceTuple(59, 58, 86, 92, 16f, 19f, 27f, 29f, 7f, 22f, 19f, 32f, Color(0xFFE7E7EC), Color(0xFF101219))
        }
        "laowu-dispatch-siamese" -> if (frame) {
            CommunityNineSliceTuple(85, 71, 61, 62, 12f, 14f, 9f, 12f, 4f, 12f, 4f, 12f, Color(0xFFF5E7CF), Color(0xFF302318))
        } else {
            CommunityNineSliceTuple(85, 71, 61, 62, 27f, 23f, 20f, 20f, 8f, 26f, 21f, 23f, Color(0xFFF5E7CF), Color(0xFF302318))
        }
        "laowu-dispatch-white" -> if (frame) {
            CommunityNineSliceTuple(92, 74, 50, 84, 10f, 14f, 8f, 14f, 4f, 12f, 4f, 12f, Color(0xFFFAFBFE), Color(0xFF22232B))
        } else {
            CommunityNineSliceTuple(92, 74, 50, 84, 25f, 22f, 15f, 25f, 7f, 25f, 17f, 28f, Color(0xFFFAFBFE), Color(0xFF22232B))
        }
        "oiia-loaf" -> if (frame) {
            CommunityNineSliceTuple(68, 78, 44, 47, 10f, 14f, 8f, 10f, 4f, 12f, 4f, 12f, Color(0xFF1D292A), Color(0xFFFFF5D9))
        } else {
            CommunityNineSliceTuple(68, 78, 44, 47, 19f, 22f, 12f, 14f, 11f, 25f, 12f, 17f, Color(0xFF1D292A), Color(0xFFFFF5D9))
        }
        "round-maodie" -> if (frame) {
            CommunityNineSliceTuple(75, 80, 67, 77, 10f, 14f, 8f, 12f, 4f, 12f, 4f, 12f, Color(0xFFF9E5BC), Color(0xFF23160E))
        } else {
            CommunityNineSliceTuple(75, 80, 67, 77, 18f, 22f, 16f, 20f, 12f, 25f, 15f, 23f, Color(0xFFF9E5BC), Color(0xFF23160E))
        }
        "waibi-babu" -> if (frame) {
            CommunityNineSliceTuple(88, 94, 44, 61, 12f, 16f, 8f, 12f, 4f, 12f, 4f, 12f, Color(0xFFFFE599), Color(0xFF39250E))
        } else {
            CommunityNineSliceTuple(88, 94, 44, 61, 27f, 32f, 13f, 20f, 15f, 35f, 8f, 23f, Color(0xFFFFE599), Color(0xFF39250E))
        }
        "shadow-dao" -> if (frame) {
            CommunityNineSliceTuple(
                sliceArr?.getOrNull(0) ?: 18, sliceArr?.getOrNull(1) ?: 50, sliceArr?.getOrNull(2) ?: 20, sliceArr?.getOrNull(3) ?: 56,
                8f, 20f, 9f, 22f, 4f, 14f, 4f, 14f,
                if (isDarkTheme) Color(0xFF1B1A1F) else Color(0xFFF3EFE6),
                if (isDarkTheme) Color(0xFFF3EFE6) else Color(0xFF141418),
            )
        } else {
            CommunityNineSliceTuple(
                sliceArr?.getOrNull(0) ?: 42, sliceArr?.getOrNull(1) ?: 60, sliceArr?.getOrNull(2) ?: 48, sliceArr?.getOrNull(3) ?: 64,
                22f, 34f, 24f, 36f, 16f, 26f, 18f, 26f,
                if (isDarkTheme) Color(0xFF1B1A1F) else Color(0xFFF3EFE6),
                if (isDarkTheme) Color(0xFFF3EFE6) else Color(0xFF141418),
            )
        }
        "shadow-path" -> if (frame) {
            CommunityNineSliceTuple(
                sliceArr?.getOrNull(0) ?: 18, sliceArr?.getOrNull(1) ?: 56, sliceArr?.getOrNull(2) ?: 18, sliceArr?.getOrNull(3) ?: 56,
                9f, 22f, 8f, 22f, 4f, 14f, 4f, 14f,
                if (isDarkTheme) Color(0xFF141C21) else Color(0xFFEEF4F7),
                if (isDarkTheme) Color(0xFFF2F7F8) else Color(0xFF0F1418),
            )
        } else {
            CommunityNineSliceTuple(
                sliceArr?.getOrNull(0) ?: 42, sliceArr?.getOrNull(1) ?: 64, sliceArr?.getOrNull(2) ?: 44, sliceArr?.getOrNull(3) ?: 62,
                22f, 36f, 24f, 34f, 16f, 26f, 18f, 26f,
                if (isDarkTheme) Color(0xFF141C21) else Color(0xFFEEF4F7),
                if (isDarkTheme) Color(0xFFF2F7F8) else Color(0xFF0F1418),
            )
        }
        else -> CommunityNineSliceTuple(
            sliceArr?.getOrNull(0) ?: 36, sliceArr?.getOrNull(1) ?: 36, sliceArr?.getOrNull(2) ?: 36, sliceArr?.getOrNull(3) ?: 36,
            14f, 16f, 14f, 16f, 10f, 18f, 10f, 18f,
            if (isDarkTheme) Color(0xFF233029) else Color(0xFFECFDF5),
            if (isDarkTheme) Color(0xFFECFDF5) else Color(0xFF064E3B),
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
        return ResolvedChatBubbleStyle(
            key = "$series:$lv",
            fillColor = bgStart,
            fillBrush = Brush.linearGradient(listOf(bgStart, bgEnd)),
            textColor = safeFg,
            linkColor = if (safeFg.luminance() > 0.5f) ringGold else edge,
            mutedColor = safeFg.copy(alpha = 0.72f),
            borderColor = edge,
            borderBrush = if (lv >= 3 || isAll) Brush.sweepGradient(listOf(ringGold, Color(0xFFFFF1C2), ringGold, edge, ringGold)) else null,
            borderWidthDp = borderWidth,
            shapeKind = when {
                frame -> BubbleShapeKind.Rounded(7f, 7f, 7f, 7f)
                isAll -> BubbleShapeKind.Rounded(4f, 13f, 5f, 13f)
                else -> BubbleShapeKind.Rounded(5f, 12f, 12f, 12f)
            },
            shadowColor = if (lv >= 4) ringGold.copy(alpha = 0.25f) else null,
            contentPadding = if (frame) PaddingValues(horizontal = 8.dp, vertical = 3.dp) else PaddingValues(horizontal = 12.dp, vertical = 7.dp),
        )
    }

    private fun resolvePersonalBubble(
        ptId: String,
        frame: Boolean,
        isDarkTheme: Boolean,
    ): ResolvedChatBubbleStyle {
        val item = BadgeAndCosmeticRegistry.PERSONAL_TITLES[ptId]
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
                shapeKind = BubbleShapeKind.Rounded(12f, 14f, 12f, 6f),
                contentPadding = if (frame) {
                    PaddingValues(horizontal = 8.dp, vertical = 3.dp)
                } else {
                    PaddingValues(start = 18.dp, end = 18.dp, top = 11.dp, bottom = 11.dp)
                },
                nineSliceSpec = if (frame) null else NineSliceSpec(
                    sourceUrl = "$ORIGIN/assets/personal-v2/$ptId-frame.webp",
                    sliceTop = 0.30f,
                    sliceRight = 0.18f,
                    sliceBottom = 0.30f,
                    sliceLeft = 0.18f,
                    isPercentSlice = true,
                    fillCenter = true,
                    borderTopDp = 10f,
                    borderRightDp = 16f,
                    borderBottomDp = 10f,
                    borderLeftDp = 16f,
                ),
                ornament = if (frame) BubbleOrnament.None else BubbleOrnament.PersonalV2Mascot("$ORIGIN/assets/personal-v2/$ptId-character.webp"),
            )
        }

        // Personal v1 (`p001`..`p059`, `personal-palettes.css` + `personal.css`)
        val pal = PERSONAL_V1_PALETTES[ptId] ?: PersonalV1PaletteSpec("#367a78", "#e0f2e9", "#f2bc72")
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

        // Compute readable gradient paper (`title-growth.css:4, 128`, `collection.css:119`)
        val endAccentWeight = (0.10f + (stage - 1).coerceIn(0, 9) * 0.042f).coerceIn(0.10f, 0.48f)
        val (bgStart, bgEnd, rawFg) = if (id == "whalebuddy") {
            // `whalebuddy-frame.webp` fills a fixed bright sky surface even in dark theme (`collection.css:119`)
            Triple(Color(0xFFE3F4FC), Color(0xFFD0EEFB), Color(0xFF163F63))
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

        val slice = RAFFLE_BUBBLE_SLICES[id]
        val nineSlice = slice?.let { s ->
            NineSliceSpec(
                sourceUrl = "$ORIGIN${s.framePath}",
                sliceTop = s.sliceTop.toFloat(),
                sliceRight = s.sliceRight.toFloat(),
                sliceBottom = s.sliceBottom.toFloat(),
                sliceLeft = s.sliceLeft.toFloat(),
                borderTopDp = when {
                    id == "whalebuddy" -> 16f
                    s.isCollectionStyle -> 22f
                    else -> 14f * s.topRatio
                },
                borderRightDp = when {
                    id == "whalebuddy" -> 32f
                    s.isCollectionStyle -> 26f
                    else -> 14f
                },
                borderBottomDp = when {
                    id == "whalebuddy" -> 6f
                    s.isCollectionStyle -> 10f
                    else -> 14f * s.bottomRatio
                },
                borderLeftDp = when {
                    id == "whalebuddy" -> 8f
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
                val sz = chickengunSizeDp(
                    level = raffle.displayLevel,
                    requested = cgSizeJson,
                    random = raffle.earnedLevel >= 5200 && cgRandom,
                    seed = "$userKey:$messageId",
                )
                BubbleOrnament.RaffleChickengun("$ORIGIN/stickers/heartgif/thumbs/heartgif_013.webp", sz)
            }
            "railcannon" -> BubbleOrnament.RaffleRailcannon("$ORIGIN/assets/raffle/bubble-reading-v10/cannon.webp", stage)
            else -> BubbleOrnament.None
        }

        val safeFg = ensureReadableForeground(rawFg, bgStart)
        val hasOpaqueSlice = id == "whalebuddy"
        return ResolvedChatBubbleStyle(
            key = "raffle:$id",
            fillColor = bgStart,
            fillBrush = Brush.linearGradient(listOf(bgStart, bgEnd)),
            textColor = safeFg,
            linkColor = when {
                hasOpaqueSlice -> Color(0xFF1D4ED8)
                isDarkTheme -> rtAccent
                else -> blendColors(rtColor, Color.Black, 0.72f)
            },
            mutedColor = safeFg.copy(alpha = 0.72f),
            borderColor = if (hasOpaqueSlice) Color.Transparent else rtColor.copy(alpha = if (stage >= 5) 0.85f else 0.55f),
            borderBrush = if (!hasOpaqueSlice && stage >= 5) Brush.linearGradient(listOf(rtColor, rtAccent, Color(0xFFFFF4DA), rtColor)) else null,
            borderWidthDp = if (hasOpaqueSlice) 0f else if (stage >= 7) 1.8f else 1.3f,
            shapeKind = shape,
            minWidthDp = if (id == "whalebuddy") 105f else 0f,
            contentPadding = when {
                id == "whalebuddy" -> PaddingValues(start = 15.dp, end = 26.dp, top = 11.dp, bottom = 9.dp)
                id == "chickengun" -> PaddingValues(start = 18.dp, end = 12.dp, top = 8.dp, bottom = 10.dp)
                else -> PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            },
            nineSliceSpec = if (id == "chickengun" || id == "whalebuddy") nineSlice else null,
            ornament = ornament,
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
            "aurora" -> ResolvedChatBubbleStyle(
                key = "preset:aurora",
                fillColor = Color(0xFF131A34),
                fillBrush = Brush.linearGradient(listOf(Color(0xFF0D1224), Color(0xFF131A34), Color(0xFF191436))),
                textColor = Color(0xFFF8FAFC),
                linkColor = Color(0xFF7DD3FC),
                mutedColor = Color(0xFFCBD5E1),
                borderColor = Color(0xFF38BDF8),
                borderBrush = Brush.sweepGradient(
                    listOf(
                        Color(0xFF00F0FF), Color(0xFF3B82F6), Color(0xFF8B5CF6), Color(0xFFD946EF),
                        Color(0xFFFF007F), Color(0xFFFF7700), Color(0xFFFFEA00), Color(0xFF00FF88), Color(0xFF00F0FF),
                    ),
                ),
                borderWidthDp = 3f,
                shapeKind = BubbleShapeKind.Rounded(4f, 16f, 16f, 16f),
                shadowColor = Color(0x55F43F5E),
                shadowOffsetDp = 0f to 3f,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
                ornament = BubbleOrnament.PresetAurora,
            )
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
            "prism" -> ResolvedChatBubbleStyle(
                key = "preset:prism",
                fillColor = Color.White,
                textColor = Color(0xFF1F2937),
                linkColor = Color(0xFF6D28D9),
                mutedColor = Color(0xFF4B5563),
                borderColor = Color(0xFFA78BFA),
                borderBrush = Brush.linearGradient(
                    listOf(Color(0xFFF472B6), Color(0xFFFBBF24), Color(0xFF34D399), Color(0xFF60A5FA), Color(0xFFA78BFA)),
                ),
                borderWidthDp = 2.4f,
                shapeKind = BubbleShapeKind.Rounded(16f, 16f, 16f, 16f),
                shadowColor = Color(0x38A78BFA),
                shadowOffsetDp = 0f to 3f,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                ornament = BubbleOrnament.PresetPrism,
            )
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
