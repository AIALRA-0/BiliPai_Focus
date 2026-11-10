package com.android.purebilibili.core.theme

import androidx.compose.ui.graphics.Color
import com.materialkolor.Contrast
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.material3.dynamicColorScheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ColorSpec2026PolicyTest {
    @Test
    fun cmfUses2026AndOtherStylesFollowUpstreamFallbacks() {
        for (style in PaletteStyle.KnownStyles) {
            val expected = when {
                style is PaletteStyle.Cmf -> ColorSpec.SpecVersion.SPEC_2026
                supportsMaterialSpec2025(style) -> ColorSpec.SpecVersion.SPEC_2025
                else -> ColorSpec.SpecVersion.SPEC_2021
            }
            assertEquals(expected, resolveMaterialPaletteSpec(style, ColorSpec.SpecVersion.SPEC_2026))
        }
        for (requested in ColorSpec.SpecVersion.entries) {
            assertEquals(ColorSpec.SpecVersion.SPEC_2026, resolveMaterialPaletteSpec(PaletteStyle.Cmf(), requested))
        }
    }

    @Test
    fun cmfPreferenceRoundTripsAndExistingStyleNamesStillLoad() {
        for (style in PaletteStyle.KnownStyles + PaletteStyle.Cmf(Color(0xFF3498DB))) {
            assertEquals(style, resolvePaletteStylePreference(style.toString()))
        }
        assertEquals(ColorSpec.SpecVersion.SPEC_2026, resolveColorSpecPreference("SPEC_2026"))
        assertEquals(PaletteStyle.TonalSpot, resolvePaletteStylePreference(null))
        assertEquals(ColorSpec.SpecVersion.SPEC_2025, resolveColorSpecPreference(null))
    }

    @Test
    fun cmfMatchesPinnedUpstreamPhoneGolden() {
        // Google MCU 5b3618b / MaterialKolor upstreamRoleGoldens:
        // Cmf|SPEC_2026|false|0.0|PHONE|ff6750a4
        val scheme = createBiliPaiStyleColorScheme(
            Color(0xFF6750A4), false, false, PaletteStyle.Cmf(), ColorSpec.SpecVersion.SPEC_2026,
        )
        assertEquals(Color(0xFF5B4497), scheme.primary)
        assertEquals(Color(0xFF6750A4), scheme.primaryContainer)
        assertEquals(Color(0xFFDED1FA), scheme.secondaryContainer)
        assertEquals(Color(0xFF594982), scheme.tertiary)
        assertEquals(Color(0xFFFDF7FF), scheme.surface)
        assertEquals(Color(0xFFE8DEF9), scheme.surfaceContainerHighest)
    }

    @Test
    fun cmfUsesNativeRolesAndPreviewMatchesBothModesAndContrastLevels() {
        val style = PaletteStyle.Cmf()
        for (seed in listOf(Color(0xFF34C759), Color(0xFF007AFF), Color(0xFFFFCC00))) {
            for (dark in listOf(false, true)) {
                for (contrast in listOf(Contrast.Default, Contrast.Medium, Contrast.High)) {
                    val native = dynamicColorScheme(
                        seedColor = seed, isDark = dark, style = style,
                        specVersion = ColorSpec.SpecVersion.SPEC_2026, contrastLevel = contrast.value,
                    )
                    val actual = createBiliPaiStyleColorScheme(
                        seed, dark, false, style, ColorSpec.SpecVersion.SPEC_2026,
                        colorContrast = contrast,
                    )
                    val preview = resolveThemePresetPaletteSwatch(
                        seed, dark, style, ColorSpec.SpecVersion.SPEC_2026, contrast,
                    )
                    assertEquals(native.primary, actual.primary)
                    assertEquals(native.secondaryContainer, actual.secondaryContainer)
                    assertEquals(native.tertiary, actual.tertiary)
                    assertEquals(native.surface, actual.surface)
                    assertEquals(native.surfaceContainerHighest, actual.surfaceContainerHighest)
                    assertEquals(actual.primary, preview.primary)
                    assertEquals(actual.primaryContainer, preview.primaryContainer)
                    assertEquals(actual.tertiary, preview.tertiary)
                    val pairs = listOf(
                        actual.onSurface to actual.surface,
                        actual.onBackground to actual.background,
                        actual.onPrimary to actual.primary,
                        actual.onPrimaryContainer to actual.primaryContainer,
                        actual.onSecondaryContainer to actual.secondaryContainer,
                        actual.onTertiaryContainer to actual.tertiaryContainer,
                    )
                    pairs.forEach { (foreground, background) ->
                        assertTrue(calculateContrastRatio(foreground, background) >= 4.5f,
                            "CMF dark=$dark contrast=$contrast seed=$seed")
                    }
                }
            }
        }
    }
}
