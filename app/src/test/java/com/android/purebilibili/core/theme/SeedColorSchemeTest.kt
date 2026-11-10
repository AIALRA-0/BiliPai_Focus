package com.android.purebilibili.core.theme

import androidx.compose.ui.graphics.Color
import com.android.purebilibili.feature.settings.Md3ColorSource
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

class SeedColorSchemeTest {
    private val seed = Color(0xFF6750A4)

    @Test
    fun storedStylesPreserveExistingSettingsAndUnknownValuesFallBack() {
        assertEquals(21, SeedPaletteStyle.entries.count { it.enabled })
        assertEquals(SeedPaletteStyle.Standard, SeedPaletteStyle.fromStored(null))
        assertEquals(SeedPaletteStyle.Standard, SeedPaletteStyle.fromStored("future_style"))
        SeedPaletteStyle.entries.forEach { assertEquals(it, SeedPaletteStyle.fromStored(it.name)) }
    }

    @Test
    fun dynamicPresetPrimaryRolesMatchUpstream501Fixtures() {
        // flex_seed_scheme 5.0.1/test/flex_seed_scheme_test.dart FCS7.017–023.
        // The fixtures provide additional seeds; only primary roles are asserted
        // because their primary seed is 0xFF6750A4 and independent of those inputs.
        val expected = listOf(
            Triple(SeedPaletteStyle.Fidelity, 0xFF4F378A, 0xFF6750A4),
            Triple(SeedPaletteStyle.Monochrome, 0xFF000000, 0xFF3B3B3B),
            Triple(SeedPaletteStyle.Neutral, 0xFF615C6B, 0xFFE7DFF2),
            Triple(SeedPaletteStyle.Vibrant, 0xFF6F19FF, 0xFFE9DDFF),
            Triple(SeedPaletteStyle.Expressive, 0xFF006B5A, 0xFF95F4DC),
            Triple(SeedPaletteStyle.Content, 0xFF4F378A, 0xFF6750A4),
            Triple(SeedPaletteStyle.Rainbow, 0xFF6750A4, 0xFFE9DDFF),
            Triple(SeedPaletteStyle.FruitSalad, 0xFF00639C, 0xFFCEE5FF),
        )
        expected.forEach { (style, primary, container) ->
            val scheme = createRawSeedColorScheme(seed, false, style)
            assertEquals(Color(primary), scheme.primary, "${style.name}.primary")
            assertEquals(Color(container), scheme.primaryContainer, "${style.name}.primaryContainer")
        }
    }

    @Test
    fun generatedRolesSurviveBothGlassBranchesAndMatchPresetPreview() {
        for (style in SeedPaletteStyle.entries.filter { it.enabled }) {
            for (dark in listOf(false, true)) {
                val generated = createSeedColorScheme(seed, dark, style)
                val swatch = resolveThemePresetPaletteSwatch(seed, dark, colorSpec = ColorSpec.SpecVersion.SPEC_2021, seedPaletteStyle = style)
                for (glass in listOf(false, true)) {
                    val result = resolveAppThemeEndpointPalette(
                        base = generated,
                        uiStyle = AppUiStyle.MATERIAL3,
                        source = Md3ColorSource.CUSTOM,
                        darkTheme = dark,
                        customSeed = Color.Red,
                        liquidGlassEnabled = glass,
                        preserveGeneratedPalette = true,
                    ).material
                    assertEquals(generated, result, "${style.name}, dark=$dark, glass=$glass")
                    assertEquals(result.primary, swatch.primary)
                    assertEquals(result.tertiary, swatch.tertiary)
                    assertEquals(result.primaryContainer, swatch.primaryContainer)
                }
            }
        }
    }

    @Test
    fun nativeMiuixKeepsGeneratedAccentRolesWhenGlassIsToggled() {
        val base = createSeedColorScheme(seed, false, SeedPaletteStyle.Jolly)
        fun endpoint(glass: Boolean) = resolveAppThemeEndpointPalette(
            base = base,
            uiStyle = AppUiStyle.MIUIX,
            source = Md3ColorSource.CUSTOM,
            darkTheme = false,
            customSeed = Color.Red,
            liquidGlassEnabled = glass,
            preserveGeneratedPalette = true,
        )
        for (glass in listOf(false, true)) {
            assertEquals(base.primary, endpoint(glass).miuix.primary)
            assertEquals(base.onPrimary, endpoint(glass).miuix.onPrimary)
            assertEquals(base.primaryContainer, endpoint(glass).miuix.primaryContainer)
            assertEquals(base.onPrimaryContainer, endpoint(glass).miuix.onPrimaryContainer)
            assertEquals(base.primary, endpoint(glass).material.primary)
        }
        assertEquals(base.surface, endpoint(false).miuix.surface)
        assertEquals(base.surfaceContainer, endpoint(false).miuix.surfaceContainer)
    }

    @Test
    fun legacyOnlyStylesKeepTheirOwnRulesRegardlessOfStandardColorSpec() {
        val actual = createBiliPaiStyleColorScheme(
            seed, false, false, PaletteStyle.Monochrome, ColorSpec.SpecVersion.SPEC_2025,
            seedPaletteStyle = SeedPaletteStyle.CandyPop,
        )
        assertEquals(createSeedColorScheme(seed, false, SeedPaletteStyle.CandyPop), actual)
        assertNotEquals(actual.primary, seed)
    }

    @Test
    fun pureBlackAdjustmentsRetainUnchangedTertiaryAndFixedRoles() {
        val base = createSeedColorScheme(seed, true, SeedPaletteStyle.CandyPop)
        val dark = createSeedColorScheme(seed, true, SeedPaletteStyle.CandyPop, pureBlack = true)
        assertEquals(Color.Black, dark.surface)
        assertEquals(Color.Black, dark.background)
        assertNotEquals(base.primary, dark.primary)
        assertEquals(base.tertiary, dark.tertiary)
        assertEquals(base.primaryFixed, dark.primaryFixed)
        assertEquals(base.surfaceContainerLow, dark.surfaceContainerLow)
    }

    @Test
    fun fixedStyleTablesKeepTheirDistinctChromaAndToneRules() {
        assertFalse(SeedPaletteStyle.Material3Legacy.isDynamic)
        assertEquals(99, SeedTones.forStyle(SeedPaletteStyle.Material3Legacy, false).surfaceTone)
        assertEquals(10, SeedTones.forStyle(SeedPaletteStyle.Material3Legacy, true).surfaceTone)
        assertEquals(0.0, SeedTones.forStyle(SeedPaletteStyle.OneHue, true).tertiaryHueRotation)
        assertEquals(70.0, SeedTones.forStyle(SeedPaletteStyle.UltraContrast, false).secondaryMinChroma)
        assertEquals(60, SeedTones.forStyle(SeedPaletteStyle.CandyPop, false).secondaryTone)
        assertEquals(50, SeedTones.forStyle(SeedPaletteStyle.Chroma, false).secondaryTone)
    }
}
