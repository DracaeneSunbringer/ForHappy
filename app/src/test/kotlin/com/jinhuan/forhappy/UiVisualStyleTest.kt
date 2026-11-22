package com.jinhuan.forhappy

import com.jinhuan.forhappy.core.model.AppDisguiseMode
import com.jinhuan.forhappy.core.model.AppThemeMode
import com.jinhuan.forhappy.core.model.AppUiStyle
import com.jinhuan.forhappy.core.model.PrivacyStealthConfig
import com.jinhuan.forhappy.ui.theme.FeishuWorkDarkPalette
import com.jinhuan.forhappy.ui.theme.FeishuWorkPalette
import com.jinhuan.forhappy.ui.theme.LinuxDoDarkPalette
import com.jinhuan.forhappy.ui.theme.LinuxDoPalette
import com.jinhuan.forhappy.ui.theme.ModernAiryLightPalette
import com.jinhuan.forhappy.ui.theme.ModernObsidianDarkPalette
import com.jinhuan.forhappy.ui.theme.PineNightDarkPalette
import com.jinhuan.forhappy.ui.theme.WarmPaperLightPalette
import com.jinhuan.forhappy.ui.theme.resolveForHappyPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiVisualStyleTest {

    @Test
    fun `AppUiStyle defaults to MODERN on null blank or unknown keys`() {
        assertEquals(AppUiStyle.MODERN, AppUiStyle.fromKey(null))
        assertEquals(AppUiStyle.MODERN, AppUiStyle.fromKey(""))
        assertEquals(AppUiStyle.MODERN, AppUiStyle.fromKey("unknown_style"))
        assertEquals(AppUiStyle.MODERN, AppUiStyle.fromKey("modern"))
        assertEquals(AppUiStyle.CLASSIC_WEB, AppUiStyle.fromKey("classic_web"))
        assertEquals(AppUiStyle.CLASSIC_WEB, AppUiStyle.fromKey("CLASSIC_WEB"))
    }

    @Test
    fun `resolveForHappyPalette returns modern palettes when not disguised and MODERN selected`() {
        val light = resolveForHappyPalette(
            mode = AppThemeMode.FOLLOW_SYSTEM,
            uiStyle = AppUiStyle.MODERN,
            privacyConfig = PrivacyStealthConfig(),
            systemDark = false,
        )
        assertEquals(ModernAiryLightPalette, light)
        assertTrue(light.isModernUi)
        assertFalse(light.isDark)

        val dark = resolveForHappyPalette(
            mode = AppThemeMode.FOLLOW_SYSTEM,
            uiStyle = AppUiStyle.MODERN,
            privacyConfig = PrivacyStealthConfig(),
            systemDark = true,
        )
        assertEquals(ModernObsidianDarkPalette, dark)
        assertTrue(dark.isModernUi)
        assertTrue(dark.isDark)

        val explicitDark = resolveForHappyPalette(
            mode = AppThemeMode.PINE_NIGHT_DARK,
            uiStyle = AppUiStyle.MODERN,
            privacyConfig = PrivacyStealthConfig(),
            systemDark = false,
        )
        assertEquals(ModernObsidianDarkPalette, explicitDark)
    }

    @Test
    fun `resolveForHappyPalette supports classic web fallback and preserves disguise isolation`() {
        val classicLight = resolveForHappyPalette(
            mode = AppThemeMode.WARM_PAPER_LIGHT,
            uiStyle = AppUiStyle.CLASSIC_WEB,
            privacyConfig = PrivacyStealthConfig(),
            systemDark = false,
        )
        assertEquals(WarmPaperLightPalette, classicLight)
        assertFalse(classicLight.isModernUi)

        val classicDark = resolveForHappyPalette(
            mode = AppThemeMode.PINE_NIGHT_DARK,
            uiStyle = AppUiStyle.CLASSIC_WEB,
            privacyConfig = PrivacyStealthConfig(),
            systemDark = false,
        )
        assertEquals(PineNightDarkPalette, classicDark)
        assertFalse(classicDark.isModernUi)

        val feishu = resolveForHappyPalette(
            mode = AppThemeMode.FOLLOW_SYSTEM,
            uiStyle = AppUiStyle.MODERN,
            privacyConfig = PrivacyStealthConfig(disguiseMode = AppDisguiseMode.FEISHU),
            systemDark = false,
        )
        assertEquals(FeishuWorkPalette, feishu)
        assertFalse(feishu.isModernUi)

        val feishuDark = resolveForHappyPalette(
            mode = AppThemeMode.PINE_NIGHT_DARK,
            uiStyle = AppUiStyle.MODERN,
            privacyConfig = PrivacyStealthConfig(disguiseMode = AppDisguiseMode.FEISHU),
            systemDark = false,
        )
        assertEquals(FeishuWorkDarkPalette, feishuDark)
        assertFalse(feishuDark.isModernUi)

        val linuxDo = resolveForHappyPalette(
            mode = AppThemeMode.FOLLOW_SYSTEM,
            uiStyle = AppUiStyle.MODERN,
            privacyConfig = PrivacyStealthConfig(disguiseMode = AppDisguiseMode.LINUX_DO),
            systemDark = false,
        )
        assertEquals(LinuxDoPalette, linuxDo)
        assertFalse(linuxDo.isModernUi)

        val linuxDoDark = resolveForHappyPalette(
            mode = AppThemeMode.FOLLOW_SYSTEM,
            uiStyle = AppUiStyle.MODERN,
            privacyConfig = PrivacyStealthConfig(disguiseMode = AppDisguiseMode.LINUX_DO),
            systemDark = true,
        )
        assertEquals(LinuxDoDarkPalette, linuxDoDark)
        assertFalse(linuxDoDark.isModernUi)
    }

    @Test
    fun `AppThemeMode displayLabel adapts to active UI style`() {
        assertEquals("跟随系统（雾白/曜石）", AppThemeMode.FOLLOW_SYSTEM.displayLabel(AppUiStyle.MODERN))
        assertEquals("浅色 · 冷雾白", AppThemeMode.WARM_PAPER_LIGHT.displayLabel(AppUiStyle.MODERN))
        assertEquals("深色 · 深曜石", AppThemeMode.PINE_NIGHT_DARK.displayLabel(AppUiStyle.MODERN))

        assertEquals("跟随系统（暖纸/深松）", AppThemeMode.FOLLOW_SYSTEM.displayLabel(AppUiStyle.CLASSIC_WEB))
        assertEquals("暖纸松绿（日间）", AppThemeMode.WARM_PAPER_LIGHT.displayLabel(AppUiStyle.CLASSIC_WEB))
        assertEquals("深松暗夜（夜间）", AppThemeMode.PINE_NIGHT_DARK.displayLabel(AppUiStyle.CLASSIC_WEB))
    }
}
