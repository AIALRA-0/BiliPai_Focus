package com.android.purebilibili.core.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.store.WallpaperPaletteOverrides
import com.android.purebilibili.core.store.WallpaperPaletteRole
import com.android.purebilibili.feature.settings.Md3ColorSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SeedTextContrastPolicyTest {
    private val seed = Color(0xFF5CB67B)

    @Test
    fun candyWhiteTextIsCorrectedWithoutChangingItsColorfulBackground() {
        val raw = createRawSeedColorScheme(seed, false, SeedPaletteStyle.CandyPop)
        val readable = createSeedColorScheme(seed, false, SeedPaletteStyle.CandyPop)
        assertTrue(calculateContrastRatio(raw.onSecondary, raw.secondary) < ACCESSIBLE_TEXT_MIN_CONTRAST)
        assertNotEquals(raw.onSecondary, readable.onSecondary)
        assertTrue(calculateContrastRatio(readable.onSecondary, readable.secondary) >= ACCESSIBLE_TEXT_MIN_CONTRAST)
        assertEquals(paletteAndSurfaceColors(raw), paletteAndSurfaceColors(readable))
    }

    @Test
    fun presetMatrixKeepsBackgroundsAndMakesSharedForegroundPairsReadable() {
        val seeds = listOf(seed, Color(0xFFFF7299), Color(0xFF6750A4), Color.Yellow, Color.Black, Color.White)
        for (style in SeedPaletteStyle.entries.filter { it.enabled }) {
            for (seed in seeds) {
                for (dark in listOf(false, true)) {
                    for (pureBlack in listOf(false, true)) {
                        val raw = createRawSeedColorScheme(seed, dark, style, pureBlack)
                        val result = createSeedColorScheme(seed, dark, style, pureBlack)
                        val message = "$style seed=$seed dark=$dark pureBlack=$pureBlack"
                        assertEquals(paletteAndSurfaceColors(raw), paletteAndSurfaceColors(result), message)
                        assertReadablePairs(result, message)
                        assertSame(result, enforceSeedTextContrast(result), "Must be idempotent: $message")
                    }
                }
            }
        }
    }

    @Test
    fun alreadyReadableForegroundIsKeptExactly() {
        val candidate = Color(0xFF122A1D)
        assertEquals(candidate, resolveSeedReadableForeground(candidate, seed, listOf(Color.White)))
    }

    @Test
    fun wallpaperEditedRolesRemainReadableAndDoNotDependOnGlass() {
        val base = createSeedColorScheme(seed, false, SeedPaletteStyle.CandyPop)
        val overrides = WallpaperPaletteOverrides(
            light = mapOf(WallpaperPaletteRole.SECONDARY to "#81DDB0"),
        )
        fun endpoint(glass: Boolean) = resolveAppThemeEndpointPalette(
            base = base,
            uiStyle = AppUiStyle.MATERIAL3,
            source = Md3ColorSource.WALLPAPER_RETONED,
            darkTheme = false,
            wallpaperOverrides = overrides,
            liquidGlassEnabled = glass,
            preserveGeneratedPalette = true,
        ).material
        val plain = endpoint(false)
        val glass = endpoint(true)
        assertEquals(paletteAndSurfaceColors(plain), paletteAndSurfaceColors(glass))
        assertEquals(plain.onSecondary, glass.onSecondary)
        assertEquals(Color(0xFF81DDB0), plain.secondary)
        assertReadablePairs(plain, "wallpaper edit")
    }

    @Test
    fun pureBlackSystemPaletteIsRecheckedAfterDarkening() {
        val base = createRawSeedColorScheme(seed, true, SeedPaletteStyle.CandyPop)
        val result = createBiliPaiStyleColorScheme(
            seedColor = seed,
            darkTheme = true,
            amoledDarkTheme = true,
            paletteStyle = com.materialkolor.PaletteStyle.TonalSpot,
            colorSpec = com.materialkolor.dynamiccolor.ColorSpec.SpecVersion.SPEC_2025,
            dynamicBaseScheme = base,
            seedPaletteStyle = SeedPaletteStyle.CandyPop,
        )
        assertEquals(paletteAndSurfaceColors(darkenSeedColorScheme(base)), paletteAndSurfaceColors(result))
        assertReadablePairs(result, "system pure black")
    }

    private fun assertReadablePairs(scheme: ColorScheme, message: String) {
        val surfaces = listOf(
            scheme.surface, scheme.background, scheme.surfaceVariant, scheme.surfaceDim,
            scheme.surfaceBright, scheme.surfaceContainerLowest, scheme.surfaceContainerLow,
            scheme.surfaceContainer, scheme.surfaceContainerHigh, scheme.surfaceContainerHighest,
        )
        val pairs = listOf(
            scheme.onPrimary to listOf(scheme.primary),
            scheme.onPrimaryContainer to listOf(scheme.primaryContainer),
            scheme.onSecondary to listOf(scheme.secondary),
            scheme.onSecondaryContainer to listOf(scheme.secondaryContainer),
            scheme.onTertiary to listOf(scheme.tertiary),
            scheme.onTertiaryContainer to listOf(scheme.tertiaryContainer),
            scheme.onError to listOf(scheme.error),
            scheme.onErrorContainer to listOf(scheme.errorContainer),
            scheme.onBackground to listOf(scheme.background),
            scheme.onSurface to surfaces,
            scheme.onSurfaceVariant to surfaces,
            scheme.inverseOnSurface to listOf(scheme.inverseSurface),
            scheme.onPrimaryFixed to listOf(scheme.primaryFixed, scheme.primaryFixedDim),
            scheme.onPrimaryFixedVariant to listOf(scheme.primaryFixed, scheme.primaryFixedDim),
            scheme.onSecondaryFixed to listOf(scheme.secondaryFixed, scheme.secondaryFixedDim),
            scheme.onSecondaryFixedVariant to listOf(scheme.secondaryFixed, scheme.secondaryFixedDim),
            scheme.onTertiaryFixed to listOf(scheme.tertiaryFixed, scheme.tertiaryFixedDim),
            scheme.onTertiaryFixedVariant to listOf(scheme.tertiaryFixed, scheme.tertiaryFixedDim),
        )
        pairs.forEach { (foreground, backgrounds) ->
            backgrounds.forEach { background ->
                assertTrue(
                    calculateContrastRatio(foreground, background) >= ACCESSIBLE_TEXT_MIN_CONTRAST,
                    "$message: $foreground on $background",
                )
            }
        }
    }

    private fun paletteAndSurfaceColors(scheme: ColorScheme): List<Color> = listOf(
        scheme.primary, scheme.primaryContainer, scheme.primaryFixed, scheme.primaryFixedDim,
        scheme.secondary, scheme.secondaryContainer, scheme.secondaryFixed, scheme.secondaryFixedDim,
        scheme.tertiary, scheme.tertiaryContainer, scheme.tertiaryFixed, scheme.tertiaryFixedDim,
        scheme.error, scheme.errorContainer, scheme.background, scheme.surface, scheme.surfaceVariant,
        scheme.surfaceDim, scheme.surfaceBright, scheme.surfaceContainerLowest, scheme.surfaceContainerLow,
        scheme.surfaceContainer, scheme.surfaceContainerHigh, scheme.surfaceContainerHighest,
        scheme.surfaceTint, scheme.inversePrimary, scheme.inverseSurface, scheme.outline, scheme.outlineVariant,
        scheme.scrim,
    )
}
