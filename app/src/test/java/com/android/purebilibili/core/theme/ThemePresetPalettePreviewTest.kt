package com.android.purebilibili.core.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.materialkolor.Contrast
import com.materialkolor.PaletteStyle
import com.materialkolor.material3.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * 预设色板预览守卫:格子上的三色必须与"选中该颜色后真正生效"的方案一致,
 * 且明暗、色彩风格切换时预览要跟着变。
 */
class ThemePresetPalettePreviewTest {

    private val seed = Color(0xFF00A1D6)

    @Test
    fun swatch_rolesMatchTheEffectiveMaterialKolorScheme() {
        for (isDark in listOf(false, true)) {
            val expected = dynamicColorScheme(
                seedColor = seed,
                isDark = isDark,
                style = PaletteStyle.TonalSpot,
                specVersion = ColorSpec.SpecVersion.SPEC_2025
            )
            val swatch = resolveThemePresetPaletteSwatch(seedColor = seed, isDark = isDark)

            assertEquals(expected.primary, swatch.primary)
            assertEquals(expected.tertiary, swatch.tertiary)
            assertEquals(expected.primaryContainer, swatch.primaryContainer)
        }
    }

    @Test
    fun swatch_followsDarkModeAndPaletteStyle() {
        val light = resolveThemePresetPaletteSwatch(seedColor = seed, isDark = false)
        val dark = resolveThemePresetPaletteSwatch(seedColor = seed, isDark = true)
        val rainbow = resolveThemePresetPaletteSwatch(
            seedColor = seed,
            isDark = false,
            paletteStyle = PaletteStyle.Rainbow
        )

        assertNotEquals(light.primary, dark.primary)
        assertNotEquals(light, rainbow, "切换色彩风格后预览三色应随之变化")
    }

    @Test
    fun swatch_followsContrastLevel() {
        val standard = resolveThemePresetPaletteSwatch(seedColor = seed, isDark = false)
        val high = resolveThemePresetPaletteSwatch(
            seedColor = seed,
            isDark = false,
            colorContrast = Contrast.High,
        )

        assertNotEquals(standard, high, "切换对比度后预览三色应随之变化")
    }

    @Test
    fun everyPresetSeedProducesOpaqueSwatch() {
        for (preset in ThemeColors) {
            val swatch = resolveThemePresetPaletteSwatch(seedColor = preset, isDark = true)

            assertTrue(swatch.primary.alpha > 0f, "primary 不应为透明色")
            assertTrue(swatch.tertiary.alpha > 0f, "tertiary 不应为透明色")
            assertTrue(swatch.primaryContainer.alpha > 0f, "primaryContainer 不应为透明色")
        }
    }

    @Test
    fun colorSchemeIsDark_followsBackgroundLuminance() {
        assertFalse(resolveColorSchemeIsDark(lightColorScheme()))
        assertTrue(resolveColorSchemeIsDark(darkColorScheme()))
    }
}
