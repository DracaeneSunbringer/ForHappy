package com.jinhuan.forhappy.ui.theme

import android.app.Activity
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.jinhuan.forhappy.core.model.AppDisguiseMode
import com.jinhuan.forhappy.core.model.AppThemeMode
import com.jinhuan.forhappy.core.model.AppUiStyle
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
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.3).sp,
        fontFeatureSettings = TABULAR_FONT_FEATURE,
    ),
    titleLarge = TextStyle(
        fontSize = 20.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.3).sp,
    ),
    titleMedium = TextStyle(
        fontSize = 18.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.3).sp,
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
    val isModernUi: Boolean = false,
    val glassSurface: Color = surface.copy(alpha = if (isDark) 0.86f else 0.90f),
    val glassBorder: Color = line,
    val cardBorder: Color = line.copy(alpha = 0.7f),
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
    bg = Color(0xFFF2F3F5),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFE8EAEE),
    text = Color(0xFF1F2329),
    muted = Color(0xFF8F959E),
    primary = Color(0xFF3370FF),
    onPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFFE1EAFF),
    gold = Color(0xFFD97706),
    line = Color(0xFFE5E6EB),
    danger = Color(0xFFF54A45),
    isDark = false,
    appDisplayName = "飞书",
    surfaceRaised = Color(0xFFFFFFFF),
    textDisabled = Color(0xFF9AA0A8),
    primaryContainer = Color(0xFFD8E6FF),
    redpacket = Color(0xFFF54A45),
    like = Color(0xFFF54A45),
    badge = Color(0xFFF54A45),
    highlight = Color(0xFFFFF1B8),
    scrim = Color.Black.copy(alpha = 0.40f),
    blurOverlay = Color.Black.copy(alpha = 0.12f),
    bubbleSelf = Color(0xFFD8E6FF),
    success = Color(0xFF00B42A),
)

val FeishuWorkDarkPalette = ForHappyPalette(
    bg = Color(0xFF111215),
    surface = Color(0xFF24272E),
    surface2 = Color(0xFF2B2F37),
    text = Color(0xFFEFF0F2),
    muted = Color(0xFF8E95A0),
    primary = Color(0xFF4C88FF),
    onPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFF1E355E),
    gold = Color(0xFFF59E0B),
    line = Color(0xFF2B2E36),
    danger = Color(0xFFF76964),
    isDark = true,
    appDisplayName = "飞书",
    surfaceRaised = Color(0xFF1A1C20),
    textDisabled = Color(0xFF666A70),
    primaryContainer = Color(0xFF1D3766),
    redpacket = Color(0xFFE25B50),
    like = Color(0xFFF76964),
    badge = Color(0xFFF76964),
    highlight = Color(0xFF5A4A12),
    scrim = Color.Black.copy(alpha = 0.56f),
    blurOverlay = Color.Black.copy(alpha = 0.24f),
    bubbleSelf = Color(0xFF1D3766),
    success = Color(0xFF27C346),
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

val ModernAiryLightPalette = ForHappyPalette(
    bg = Color(0xFFF3F5FA),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFE9EDF5),
    text = Color(0xFF0F172A),
    muted = Color(0xFF64748B),
    primary = Color(0xFFFF5A36),
    onPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFF0EA5E9),
    gold = Color(0xFFD97706),
    line = Color(0x120F172A),
    danger = Color(0xFFE11D48),
    isDark = false,
    appDisplayName = "尽欢",
    surfaceRaised = Color(0xFFFFFFFF),
    textDisabled = Color(0xFF94A3B8),
    primaryContainer = Color(0xFFFFEAE4),
    redpacket = Color(0xFFE53935),
    like = Color(0xFFFF3B5C),
    badge = Color(0xFFFF4757),
    highlight = Color(0xFFFFF3C4),
    scrim = Color.Black.copy(alpha = 0.40f),
    blurOverlay = Color.Black.copy(alpha = 0.12f),
    bubbleSelf = Color(0xFFFFE8E1),
    success = Color(0xFF10B981),
    isModernUi = true,
    glassSurface = Color(0xF5F8FAFE),
    glassBorder = Color(0x140F172A),
    cardBorder = Color.Transparent,
)

val ModernObsidianDarkPalette = ForHappyPalette(
    bg = Color(0xFF0A0C10),
    surface = Color(0xFF131720),
    surface2 = Color(0xFF1C2230),
    text = Color(0xFFF1F5F9),
    muted = Color(0xFF8B98AF),
    primary = Color(0xFFFF6642),
    onPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFF38BDF8),
    gold = Color(0xFFFBBF24),
    line = Color(0x14FFFFFF),
    danger = Color(0xFFFB7185),
    isDark = true,
    appDisplayName = "尽欢",
    surfaceRaised = Color(0xFF212838),
    textDisabled = Color(0xFF525E73),
    primaryContainer = Color(0xFF331C19),
    redpacket = Color(0xFFEF5350),
    like = Color(0xFFFF5470),
    badge = Color(0xFFFF5263),
    highlight = Color(0xFF4A3B10),
    scrim = Color.Black.copy(alpha = 0.62f),
    blurOverlay = Color.Black.copy(alpha = 0.26f),
    bubbleSelf = Color(0xFF2E1D1B),
    success = Color(0xFF34D399),
    isModernUi = true,
    glassSurface = Color(0xF5141923),
    glassBorder = Color(0x22FFFFFF),
    cardBorder = Color(0x0FFFFFFF),
)

val LocalForHappyPalette = staticCompositionLocalOf { ModernAiryLightPalette }
val LocalAppUiStyle = staticCompositionLocalOf { AppUiStyle.MODERN }
val LocalPrivacyStealthConfig = staticCompositionLocalOf { PrivacyStealthConfig() }
val LocalBossKeyActive = staticCompositionLocalOf { false }

private val ForHappyShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

fun resolveForHappyPalette(
    mode: AppThemeMode,
    uiStyle: AppUiStyle = AppUiStyle.MODERN,
    privacyConfig: PrivacyStealthConfig = PrivacyStealthConfig(),
    systemDark: Boolean = false,
): ForHappyPalette {
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
    return when (effectiveDisguise) {
        AppDisguiseMode.FEISHU -> if (useDark) FeishuWorkDarkPalette else FeishuWorkPalette
        AppDisguiseMode.LINUX_DO -> if (useDark) LinuxDoDarkPalette else LinuxDoPalette
        AppDisguiseMode.NONE -> when (uiStyle) {
            AppUiStyle.MODERN -> if (useDark) ModernObsidianDarkPalette else ModernAiryLightPalette
            AppUiStyle.CLASSIC_WEB -> if (useDark) PineNightDarkPalette else WarmPaperLightPalette
        }
    }
}

@Composable
fun ForHappyTheme(
    mode: AppThemeMode,
    uiStyle: AppUiStyle = AppUiStyle.MODERN,
    privacyConfig: PrivacyStealthConfig = PrivacyStealthConfig(),
    seenTracker: SeenTracker? = null,
    bossKeyActive: Boolean = false,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val palette = resolveForHappyPalette(
        mode = mode,
        uiStyle = uiStyle,
        privacyConfig = privacyConfig,
        systemDark = systemDark,
    )
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            var ctx = view.context
            while (ctx is android.content.ContextWrapper && ctx !is Activity) {
                ctx = ctx.baseContext
            }
            val window = (ctx as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !palette.isDark
            controller.isAppearanceLightNavigationBars = !palette.isDark
        }
    }

    val colorScheme = if (palette.isDark) {
        darkColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            primaryContainer = palette.primaryContainer,
            onPrimaryContainer = palette.primary,
            secondary = palette.accent,
            onSecondary = palette.onPrimary,
            secondaryContainer = palette.primaryContainer,
            onSecondaryContainer = palette.primary,
            background = palette.bg,
            onBackground = palette.text,
            surface = palette.surface,
            onSurface = palette.text,
            surfaceVariant = palette.surface2,
            onSurfaceVariant = palette.muted,
            surfaceContainer = palette.surface,
            surfaceContainerHigh = palette.surfaceRaised,
            surfaceContainerHighest = palette.surface2,
            outline = palette.line,
            outlineVariant = palette.line,
            error = palette.danger,
        )
    } else {
        lightColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            primaryContainer = palette.primaryContainer,
            onPrimaryContainer = palette.primary,
            secondary = palette.accent,
            onSecondary = palette.text,
            secondaryContainer = palette.primaryContainer,
            onSecondaryContainer = palette.primary,
            background = palette.bg,
            onBackground = palette.text,
            surface = palette.surface,
            onSurface = palette.text,
            surfaceVariant = palette.surface2,
            onSurfaceVariant = palette.muted,
            surfaceContainer = palette.surface,
            surfaceContainerHigh = palette.surfaceRaised,
            surfaceContainerHighest = palette.surface2,
            outline = palette.line,
            outlineVariant = palette.line,
            error = palette.danger,
        )
    }

    CompositionLocalProvider(
        LocalForHappyPalette provides palette,
        LocalAppUiStyle provides uiStyle,
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
