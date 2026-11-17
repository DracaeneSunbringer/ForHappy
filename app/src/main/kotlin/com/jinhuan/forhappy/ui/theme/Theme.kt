package com.jinhuan.forhappy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jinhuan.forhappy.core.model.AppDisguiseMode
import com.jinhuan.forhappy.core.model.AppThemeMode
import com.jinhuan.forhappy.core.model.PrivacyStealthConfig
import com.jinhuan.forhappy.core.seen.LocalSeenTracker
import com.jinhuan.forhappy.core.seen.SeenTracker

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
    line = Color(0xFFD9DFD5),
    danger = Color(0xFFC2410C),
    isDark = false,
    appDisplayName = "尽欢",
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
    line = Color(0xFF2F4C3E),
    danger = Color(0xFFF97316),
    isDark = true,
    appDisplayName = "尽欢",
)

val FeishuWorkPalette = ForHappyPalette(
    bg = Color(0xFFF5F6F7),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFEFF0F1),
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
)

val FeishuWorkDarkPalette = ForHappyPalette(
    bg = Color(0xFF191919),
    surface = Color(0xFF222326),
    surface2 = Color(0xFF2B2C30),
    text = Color(0xFFEFF0F1),
    muted = Color(0xFF8F959E),
    primary = Color(0xFF4C88FF),
    onPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFF27395E),
    gold = Color(0xFFF59E0B),
    line = Color(0xFF33353A),
    danger = Color(0xFFF54A45),
    isDark = true,
    appDisplayName = "飞书",
)

val LinuxDoPalette = ForHappyPalette(
    bg = Color(0xFFF7F8FA),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFECEFF4),
    text = Color(0xFF222222),
    muted = Color(0xFF6B7280),
    primary = Color(0xFF0088CC),
    onPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFF93C5FD),
    gold = Color(0xFFB45309),
    line = Color(0xFFE2E8F0),
    danger = Color(0xFFDC2626),
    isDark = false,
    appDisplayName = "LINUX DO",
)

val LinuxDoDarkPalette = ForHappyPalette(
    bg = Color(0xFF1D1F21),
    surface = Color(0xFF25282A),
    surface2 = Color(0xFF2F3336),
    text = Color(0xFFDDDDDD),
    muted = Color(0xFF91979E),
    primary = Color(0xFF1A95E0),
    onPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFF274863),
    gold = Color(0xFFF59E0B),
    line = Color(0xFF393D40),
    danger = Color(0xFFEF4444),
    isDark = true,
    appDisplayName = "LINUX DO",
)

val LocalForHappyPalette = staticCompositionLocalOf { WarmPaperLightPalette }
val LocalPrivacyStealthConfig = staticCompositionLocalOf { PrivacyStealthConfig() }

private val ForHappyShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun ForHappyTheme(
    mode: AppThemeMode,
    privacyConfig: PrivacyStealthConfig = PrivacyStealthConfig(),
    seenTracker: SeenTracker? = null,
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
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = ForHappyShapes,
            content = content,
        )
    }
}
