package com.jinhuan.forhappy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jinhuan.forhappy.core.model.AppDisguiseMode
import com.jinhuan.forhappy.core.model.AppThemeMode
import com.jinhuan.forhappy.core.model.PrivacyStealthConfig
import com.jinhuan.forhappy.core.seen.LocalSeenTracker
import com.jinhuan.forhappy.core.seen.SeenTracker

const val TABULAR_FONT_FEATURE = "tnum"

@Immutable
object ForHappyDimens {
    val space1 = 4.dp
    val space2 = 8.dp
    val space3 = 12.dp
    val space4 = 16.dp
    val space5 = 24.dp
    val space6 = 32.dp

    val radiusS = 6.dp
    val radiusM = 12.dp
    val radiusL = 16.dp
    val radiusXL = 24.dp

    val topBarHeight = 48.dp
    val bottomBarHeight = 56.dp
    val inlineBannerHeight = 34.dp
    val minTouchTarget = 48.dp
    val avatarChat = 36.dp
    val avatarConversation = 48.dp
    val avatarProfile = 64.dp
}

val ForHappyTypography = Typography(
    displaySmall = TextStyle(
        fontSize = 28.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.SemiBold,
        fontFeatureSettings = TABULAR_FONT_FEATURE,
    ),
    titleLarge = TextStyle(
        fontSize = 20.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleMedium = TextStyle(
        fontSize = 18.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleSmall = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium,
    ),
    bodyLarge = TextStyle(
        fontSize = 15.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
    ),
    labelLarge = TextStyle(
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium,
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Normal,
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium,
    ),
)

@Immutable
data class ForHappyPalette(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val text: Color,
    val muted: Color,
    val primary: Color,
    val onPrimary: Color,
    val accent: Color,
    val gold: Color,
    val line: Color,
    val danger: Color,
    val isDark: Boolean,
    val appDisplayName: String,
    val surfaceRaised: Color = surface,
    val textDisabled: Color = muted.copy(alpha = 0.6f),
    val primaryContainer: Color = primary.copy(alpha = 0.14f),
    val redpacket: Color = Color(0xFFD9433B),
    val like: Color = Color(0xFFE0475B),
    val badge: Color = Color(0xFFE5484D),
    val highlight: Color = Color(0xFFFFF1B8),
    val scrim: Color = Color.Black.copy(alpha = if (isDark) 0.56f else 0.40f),
    val blurOverlay: Color = Color.Black.copy(alpha = if (isDark) 0.24f else 0.12f),
    val bubbleSelf: Color = primaryContainer,
    val success: Color = primary,
) {
    val surfaceVariant: Color get() = surface2
    val outline: Color get() = line
    val textMuted: Color get() = muted
    val coin: Color get() = gold
}

@Immutable
data class BossDocPalette(
    val bg: Color,
    val barBg: Color,
    val text: Color,
    val subText: Color,
    val textMuted: Color,
    val tableHeader: Color,
    val tableBorder: Color,
    val calloutBg: Color,
    val link: Color,
    val okBg: Color,
    val okFg: Color,
    val warnBg: Color,
    val warnFg: Color,
    val isDark: Boolean,
)

val BossDocLightPalette = BossDocPalette(
    bg = Color(0xFFFFFFFF),
    barBg = Color(0xFFFAFAFB),
    text = Color(0xFF1F2329),
    subText = Color(0xFF646A73),
    textMuted = Color(0xFF8F959E),
    tableHeader = Color(0xFFF5F6F7),
    tableBorder = Color(0xFFDEE0E3),
    calloutBg = Color(0xFFFFF5EB),
    link = Color(0xFF3370FF),
    okBg = Color(0xFFE8F7EE),
    okFg = Color(0xFF14866D),
    warnBg = Color(0xFFFFF3E0),
    warnFg = Color(0xFFD97706),
    isDark = false,
)

val BossDocDarkPalette = BossDocPalette(
    bg = Color(0xFF1A1A1A),
    barBg = Color(0xFF222326),
    text = Color(0xFFE6E6E6),
    subText = Color(0xFFB3B8C0),
    textMuted = Color(0xFF8A8A8A),
    tableHeader = Color(0xFF262626),
    tableBorder = Color(0xFF3A3A3A),
    calloutBg = Color(0xFF3A2C1E),
    link = Color(0xFF4C88FF),
    okBg = Color(0xFF163628),
    okFg = Color(0xFF51BA43),
    warnBg = Color(0xFF3B2C16),
    warnFg = Color(0xFFF59E0B),
    isDark = true,
)

val WarmPaperLightPalette = ForHappyPalette(
    bg = Color(0xFFF7F6F0),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFEEF0E6),
    text = Color(0xFF172D25),
    muted = Color(0xFF5A6F65),
    primary = Color(0xFF1F6B4F),
    onPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFFB7CE78),
    gold = Color(0xFFB7791F),
    line = Color(0xFFDCE0D2),
    danger = Color(0xFFC2410C),
    isDark = false,
    appDisplayName = "尽欢",
    surfaceRaised = Color(0xFFFFFFFF),
    textDisabled = Color(0xFF9AA89F),
    primaryContainer = Color(0xFFDCEBD9),
    redpacket = Color(0xFFD9433B),
    like = Color(0xFFE0475B),
    badge = Color(0xFFE5484D),
    highlight = Color(0xFFFFF1B8),
    scrim = Color.Black.copy(alpha = 0.40f),
    blurOverlay = Color.Black.copy(alpha = 0.12f),
    bubbleSelf = Color(0xFFDCEBD9),
    success = Color(0xFF1F6B4F),
)

val PineNightDarkPalette = ForHappyPalette(
    bg = Color(0xFF14261F),
    surface = Color(0xFF1D3329),
    surface2 = Color(0xFF243D31),
    text = Color(0xFFF2F4EC),
    muted = Color(0xFFA3B6AA),
    primary = Color(0xFFB7CE78),
    onPrimary = Color(0xFF14261F),
    accent = Color(0xFF5CA382),
    gold = Color(0xFFE9B949),
    line = Color(0xFF33503F),
    danger = Color(0xFFF97316),
    isDark = true,
    appDisplayName = "尽欢",
    surfaceRaised = Color(0xFF2A4538),
    textDisabled = Color(0xFF5E7468),
    primaryContainer = Color(0xFF2F4D3C),
    redpacket = Color(0xFFE25B50),
    like = Color(0xFFF06A7B),
    badge = Color(0xFFF2555A),
    highlight = Color(0xFF5A4A12),
    scrim = Color.Black.copy(alpha = 0.56f),
    blurOverlay = Color.Black.copy(alpha = 0.24f),
    bubbleSelf = Color(0xFF2F4D3C),
    success = Color(0xFF5CA382),
)

val FeishuWorkPalette = ForHappyPalette(
    bg = Color(0xFFF5F6F7),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFF2F3F5),
    text = Color(0xFF1F2329),
    muted = Color(0xFF646A73),
    primary = Color(0xFF3370FF),
    onPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFFE1EAFF),
    gold = Color(0xFFD97706),
    line = Color(0xFFDEE0E3),
    danger = Color(0xFFF54A45),
    isDark = false,
    appDisplayName = "飞书",
    surfaceRaised = Color(0xFFFFFFFF),
    textDisabled = Color(0xFF9AA0A8),
    primaryContainer = Color(0xFFE1EAFF),
    redpacket = Color(0xFFE5484D),
    like = Color(0xFFF54A45),
    badge = Color(0xFFF54A45),
    highlight = Color(0xFFFFF1B8),
    scrim = Color.Black.copy(alpha = 0.40f),
    blurOverlay = Color.Black.copy(alpha = 0.12f),
    bubbleSelf = Color(0xFFE1EAFF),
    success = Color(0xFF34C724),
)

val FeishuWorkDarkPalette = ForHappyPalette(
    bg = Color(0xFF191919),
    surface = Color(0xFF222326),
    surface2 = Color(0xFF2E2F33),
    text = Color(0xFFEBEBEB),
    muted = Color(0xFFA6A6A6),
    primary = Color(0xFF4C88FF),
    onPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFF27395E),
    gold = Color(0xFFF59E0B),
    line = Color(0xFF373737),
    danger = Color(0xFFF76964),
    isDark = true,
    appDisplayName = "飞书",
    surfaceRaised = Color(0xFF2B2C30),
    textDisabled = Color(0xFF666A70),
    primaryContainer = Color(0xFF1F3A6E),
    redpacket = Color(0xFFE25B50),
    like = Color(0xFFF76964),
    badge = Color(0xFFF76964),
    highlight = Color(0xFF5A4A12),
    scrim = Color.Black.copy(alpha = 0.56f),
    blurOverlay = Color.Black.copy(alpha = 0.24f),
    bubbleSelf = Color(0xFF1F3A6E),
    success = Color(0xFF51BA43),
)

val LinuxDoPalette = ForHappyPalette(
    bg = Color(0xFFF7F8FA),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFECEFF4),
    text = Color(0xFF222222),
    muted = Color(0xFF919191),
    primary = Color(0xFF0088CC),
    onPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFF93C5FD),
    gold = Color(0xFFB45309),
    line = Color(0xFFE9E9E9),
    danger = Color(0xFFDC2626),
    isDark = false,
    appDisplayName = "LINUX DO",
    surfaceRaised = Color(0xFFFFFFFF),
    textDisabled = Color(0xFFB5B5B5),
    primaryContainer = Color(0xFFE1F2FB),
    redpacket = Color(0xFFD9433B),
    like = Color(0xFFFA6C8D),
    badge = Color(0xFFFA6C8D),
    highlight = Color(0xFFFFFFE0),
    scrim = Color.Black.copy(alpha = 0.40f),
    blurOverlay = Color.Black.copy(alpha = 0.12f),
    bubbleSelf = Color(0xFFE1F2FB),
    success = Color(0xFF16A34A),
)

val LinuxDoDarkPalette = ForHappyPalette(
    bg = Color(0xFF1D1F21),
    surface = Color(0xFF25282A),
    surface2 = Color(0xFF2F3336),
    text = Color(0xFFDDDDDD),
    muted = Color(0xFF999999),
    primary = Color(0xFF1A95E0),
    onPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFF274863),
    gold = Color(0xFFF59E0B),
    line = Color(0xFF3A3D40),
    danger = Color(0xFFEF4444),
    isDark = true,
    appDisplayName = "LINUX DO",
    surfaceRaised = Color(0xFF33373B),
    textDisabled = Color(0xFF666666),
    primaryContainer = Color(0xFF274863),
    redpacket = Color(0xFFE25B50),
    like = Color(0xFFFA6C8D),
    badge = Color(0xFFFA6C8D),
    highlight = Color(0xFF3B3A24),
    scrim = Color.Black.copy(alpha = 0.56f),
    blurOverlay = Color.Black.copy(alpha = 0.24f),
    bubbleSelf = Color(0xFF274863),
    success = Color(0xFF22C55E),
)

val LocalForHappyPalette = staticCompositionLocalOf { WarmPaperLightPalette }
val LocalPrivacyStealthConfig = staticCompositionLocalOf { PrivacyStealthConfig() }
val LocalBossKeyActive = staticCompositionLocalOf { false }

private val ForHappyShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun ForHappyTheme(
    mode: AppThemeMode,
    privacyConfig: PrivacyStealthConfig = PrivacyStealthConfig(),
    seenTracker: SeenTracker? = null,
    bossKeyActive: Boolean = false,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (mode) {
        AppThemeMode.WARM_PAPER_LIGHT -> false
        AppThemeMode.PINE_NIGHT_DARK -> true
        AppThemeMode.FOLLOW_SYSTEM -> systemDark
        AppThemeMode.FEISHU_WORK -> false
        AppThemeMode.LINUX_DO -> false
    }
    val effectiveDisguise = when {
        privacyConfig.disguiseMode != AppDisguiseMode.NONE -> privacyConfig.disguiseMode
        mode == AppThemeMode.FEISHU_WORK -> AppDisguiseMode.FEISHU
        mode == AppThemeMode.LINUX_DO -> AppDisguiseMode.LINUX_DO
        else -> AppDisguiseMode.NONE
    }
    val palette = when (effectiveDisguise) {
        AppDisguiseMode.FEISHU -> if (useDark) FeishuWorkDarkPalette else FeishuWorkPalette
        AppDisguiseMode.LINUX_DO -> if (useDark) LinuxDoDarkPalette else LinuxDoPalette
        AppDisguiseMode.NONE -> if (useDark) PineNightDarkPalette else WarmPaperLightPalette
    }

    val colorScheme = if (palette.isDark) {
        darkColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            primaryContainer = palette.primaryContainer,
            secondary = palette.accent,
            onSecondary = palette.onPrimary,
            background = palette.bg,
            onBackground = palette.text,
            surface = palette.surface,
            onSurface = palette.text,
            surfaceVariant = palette.surface2,
            onSurfaceVariant = palette.muted,
            outline = palette.line,
            error = palette.danger,
        )
    } else {
        lightColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            primaryContainer = palette.primaryContainer,
            secondary = palette.accent,
            onSecondary = palette.text,
            background = palette.bg,
            onBackground = palette.text,
            surface = palette.surface,
            onSurface = palette.text,
            surfaceVariant = palette.surface2,
            onSurfaceVariant = palette.muted,
            outline = palette.line,
            error = palette.danger,
        )
    }

    CompositionLocalProvider(
        LocalForHappyPalette provides palette,
        LocalPrivacyStealthConfig provides privacyConfig,
        LocalSeenTracker provides seenTracker,
        LocalBossKeyActive provides bossKeyActive,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ForHappyTypography,
            shapes = ForHappyShapes,
            content = content,
        )
    }
}
