package com.android.purebilibili.core.theme

import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.material3.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import kotlin.test.Test
import kotlin.test.assertEquals

class PaletteSpecPolicyTest {
    @Test
    fun materialSpecSupportMatchesThePinnedLibrary() {
        val supported = setOf(PaletteStyle.TonalSpot, PaletteStyle.Neutral, PaletteStyle.Vibrant, PaletteStyle.Expressive)
        for (style in PaletteStyle.KnownStyles) {
            assertEquals(style in supported, supportsMaterialSpec2025(style))
            assertEquals(
                when {
                    style is PaletteStyle.Cmf -> ColorSpec.SpecVersion.SPEC_2026
                    style in supported -> ColorSpec.SpecVersion.SPEC_2025
                    else -> ColorSpec.SpecVersion.SPEC_2021
                },
                resolveMaterialPaletteSpec(style, ColorSpec.SpecVersion.SPEC_2025),
            )
        }
    }

    @Test
    fun modernSeedStylesUseNative2025PalettesAndRoleMappings() {
        for (style in SeedPaletteStyle.entries.filter { it.material2025Style() != null }) {
            for (dark in listOf(false, true)) {
                val seed = Color(0xFF5CB67B)
                val expected = dynamicColorScheme(
                    seedColor = seed,
                    isDark = dark,
                    style = requireNotNull(style.material2025Style()),
                    specVersion = ColorSpec.SpecVersion.SPEC_2025,
                    contrastLevel = 0.0,
                )
                val raw = createRawSeedColorScheme(seed, dark, style, colorSpec = ColorSpec.SpecVersion.SPEC_2025)
                assertEquals(expected.primary, raw.primary)
                assertEquals(expected.primaryContainer, raw.primaryContainer)
                assertEquals(expected.secondary, raw.secondary)
                assertEquals(expected.tertiary, raw.tertiary)
                assertEquals(expected.primaryFixed, raw.primaryFixed)
                assertEquals(expected.onPrimary, raw.onPrimary)
                assertEquals(expected.onPrimaryContainer, raw.onPrimaryContainer)
                assertEquals(expected.onSecondaryContainer, raw.onSecondaryContainer)
                assertEquals(expected.onTertiaryContainer, raw.onTertiaryContainer)
                assertEquals(expected.surface, raw.surface)
                assertEquals(expected.surfaceContainer, raw.surfaceContainer)
                assertEquals(expected.surfaceContainerHighest, raw.surfaceContainerHighest)
                val effective = createBiliPaiStyleColorScheme(
                    seed, dark, false, PaletteStyle.Monochrome, ColorSpec.SpecVersion.SPEC_2025,
                    seedPaletteStyle = style,
                )
                val preview = resolveThemePresetPaletteSwatch(
                    seed, dark, colorSpec = ColorSpec.SpecVersion.SPEC_2025, seedPaletteStyle = style,
                )
                assertEquals(expected.primary, effective.primary)
                assertEquals(effective.primary, preview.primary)
                assertEquals(effective.tertiary, preview.tertiary)
                assertEquals(effective.primaryContainer, preview.primaryContainer)
            }
        }
    }
}
