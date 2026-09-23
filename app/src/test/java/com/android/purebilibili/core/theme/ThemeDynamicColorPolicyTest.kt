package com.android.purebilibili.core.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.android.purebilibili.feature.settings.AppThemeMode
import com.android.purebilibili.feature.settings.Md3ColorSource
import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import com.android.purebilibili.core.theme.iOSSystemGray6

class ThemeDynamicColorPolicyTest {

    @Test
    fun `all palette specs keep material and miuix roles accessible`() {
        val seeds = listOf(
            Color(0xFF007AFF),
            Color(0xFFFF5722),
            Color(0xFF34C759),
        )
        val modes = listOf(
            Triple(false, false, "light"),
            Triple(true, false, "dark"),
            Triple(true, true, "amoled"),
        )

        PaletteStyle.entries.forEach { style ->
            ColorSpec.SpecVersion.entries.forEach { spec ->
                seeds.forEach { seed ->
                    modes.forEach { (dark, amoled, mode) ->
                        val label = "$style/$spec/$mode/${seed.value}"
                        val scheme = createBiliPaiStyleColorScheme(
                            seedColor = seed,
                            darkTheme = dark,
                            amoledDarkTheme = amoled,
                            paletteStyle = style,
                            colorSpec = spec,
                        )

                        assertTextContrast(scheme.onBackground, scheme.background, "$label background")
                        assertTextContrast(scheme.onSurface, scheme.surface, "$label surface")
                        assertTextContrast(scheme.onSurfaceVariant, scheme.surfaceVariant, "$label surfaceVariant")
                        assertTextContrast(scheme.onPrimary, scheme.primary, "$label primary")
                        assertTextContrast(scheme.onPrimaryContainer, scheme.primaryContainer, "$label primaryContainer")
                        assertTextContrast(scheme.onSecondary, scheme.secondary, "$label secondary")
                        assertTextContrast(scheme.onSecondaryContainer, scheme.secondaryContainer, "$label secondaryContainer")
                        assertTextContrast(scheme.onTertiary, scheme.tertiary, "$label tertiary")
                        assertTextContrast(scheme.onTertiaryContainer, scheme.tertiaryContainer, "$label tertiaryContainer")
                        assertTextContrast(scheme.onError, scheme.error, "$label error")
                        assertTextContrast(scheme.onErrorContainer, scheme.errorContainer, "$label errorContainer")
                        assertTrue(
                            calculateContrastRatio(scheme.primary, scheme.surface) >= 3f,
                            "$label primary control is below 3:1",
                        )

                        val miuix = resolveMiuixColorsFromMaterialBridge(
                            bridge = createMiuixMaterialBridge(scheme),
                            darkTheme = dark,
                        )
                        assertTextContrast(miuix.onSurface, miuix.surface, "$label miuix surface")
                        assertTextContrast(miuix.onPrimary, miuix.primary, "$label miuix primary")
                        assertTextContrast(
                            miuix.onPrimaryContainer,
                            miuix.primaryContainer,
                            "$label miuix primaryContainer",
                        )
                        assertTrue(
                            calculateContrastRatio(miuix.onSecondary, miuix.secondary) >= 3f,
                            "$label miuix switch roles are below 3:1",
                        )
                        assertTrue(
                            calculateContrastRatio(miuix.sliderKeyPoint, miuix.surface) >= 3f,
                            "$label miuix slider is below 3:1",
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `dynamic color keeps miuix bridge on explicit resolved colors`() {
        assertEquals(
            ColorSchemeMode.System,
            resolveMiuixColorSchemeMode(
                themeMode = AppThemeMode.FOLLOW_SYSTEM,
                dynamicColorEnabled = true
            )
        )
        assertEquals(
            ColorSchemeMode.Light,
            resolveMiuixColorSchemeMode(
                themeMode = AppThemeMode.LIGHT,
                dynamicColorEnabled = true
            )
        )
        assertEquals(
            ColorSchemeMode.Dark,
            resolveMiuixColorSchemeMode(
                themeMode = AppThemeMode.DARK,
                dynamicColorEnabled = true
            )
        )
    }

    @Test
    fun `system wallpaper observer only runs when monet dynamic color is active`() {
        assertEquals(
            true,
            shouldObserveSystemWallpaperForDynamicColor(
                dynamicColorActive = true,
                sdkInt = android.os.Build.VERSION_CODES.S
            )
        )
        assertEquals(
            false,
            shouldObserveSystemWallpaperForDynamicColor(
                dynamicColorActive = false,
                sdkInt = android.os.Build.VERSION_CODES.S
            )
        )
        assertEquals(
            false,
            shouldObserveSystemWallpaperForDynamicColor(
                dynamicColorActive = true,
                sdkInt = android.os.Build.VERSION_CODES.R
            )
        )
    }

    @Test
    fun `wallpaper changes refresh AndroidX dynamic schemes immediately and on resume`() {
        val source = File(
            "app/src/main/java/com/android/purebilibili/core/theme/Theme.kt"
        ).readText()
        val observer = source
            .substringAfter("private fun rememberSystemWallpaperRefreshToken(")
            .substringBefore("private const val SYSTEM_WALLPAPER_PALETTE_SETTLE_DELAY_MS")

        assertTrue(observer.contains("WallpaperManager.OnColorsChangedListener"))
        assertTrue(observer.contains("Intent.ACTION_WALLPAPER_CHANGED"))
        assertTrue(observer.contains("androidx.lifecycle.Lifecycle.Event.ON_RESUME"))
        assertTrue(observer.contains("wallpaperManager.removeOnColorsChangedListener(listener)"))
        assertTrue(observer.contains("lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)"))
    }

    @Test
    fun `md3 color source maps wallpaper to monet and custom to static seed`() {
        assertTrue(
            resolveMd3DynamicColorEnabled(
                source = Md3ColorSource.FOLLOW_WALLPAPER,
                sdkInt = android.os.Build.VERSION_CODES.S
            )
        )
        assertEquals(
            false,
            resolveMd3DynamicColorEnabled(
                source = Md3ColorSource.CUSTOM,
                sdkInt = android.os.Build.VERSION_CODES.S
            )
        )
        assertEquals(
            Color(0xFFFF5722),
            resolveMd3ThemeSeedColor(
                source = Md3ColorSource.CUSTOM,
                customColorHex = "#FF5722",
                themeColorIndex = 0
            )
        )
        assertEquals(
            Color(0xFF007AFF),
            resolveMd3ThemeSeedColor(
                source = Md3ColorSource.FOLLOW_WALLPAPER,
                customColorHex = "#FF5722",
                themeColorIndex = 0
            )
        )
    }

    @Test
    fun `wallpaper color scheme remains the exact AndroidX resolved scheme`() {
        val wallpaperScheme = lightColorScheme(
            primary = Color(0xFF246A73),
            secondary = Color(0xFF4F6367),
            tertiary = Color(0xFF526A92),
            surface = Color(0xFFF5FAF8)
        )

        val result = createBiliPaiStyleColorScheme(
            seedColor = Color.Red,
            darkTheme = false,
            amoledDarkTheme = true,
            paletteStyle = PaletteStyle.Expressive,
            colorSpec = ColorSpec.SpecVersion.SPEC_2025,
            dynamicBaseScheme = wallpaperScheme
        )

        assertEquals(wallpaperScheme, result)
    }

    @Test
    fun `static color modes map to plain miuix color scheme modes`() {
        assertEquals(
            ColorSchemeMode.System,
            resolveMiuixColorSchemeMode(
                themeMode = AppThemeMode.FOLLOW_SYSTEM,
                dynamicColorEnabled = false
            )
        )
        assertEquals(
            ColorSchemeMode.Light,
            resolveMiuixColorSchemeMode(
                themeMode = AppThemeMode.LIGHT,
                dynamicColorEnabled = false
            )
        )
        assertEquals(
            ColorSchemeMode.Dark,
            resolveMiuixColorSchemeMode(
                themeMode = AppThemeMode.DARK,
                dynamicColorEnabled = false
            )
        )
    }

    @Test
    fun `color style preference defaults to tonal spot and rejects invalid values`() {
        assertEquals(PaletteStyle.TonalSpot, resolvePaletteStylePreference(null))
        assertEquals(PaletteStyle.TonalSpot, resolvePaletteStylePreference("not-a-style"))
        assertEquals(PaletteStyle.Vibrant, resolvePaletteStylePreference(PaletteStyle.Vibrant.name))
    }

    @Test
    fun `color spec preference defaults to spec 2021 and rejects invalid values`() {
        assertEquals(ColorSpec.SpecVersion.SPEC_2021, resolveColorSpecPreference(null))
        assertEquals(ColorSpec.SpecVersion.SPEC_2021, resolveColorSpecPreference("not-a-spec"))
        assertEquals(
            ColorSpec.SpecVersion.SPEC_2025,
            resolveColorSpecPreference(ColorSpec.SpecVersion.SPEC_2025.name)
        )
    }

    @Test
    fun `amoled overrides keep monet accents while forcing black surfaces`() {
        val monetScheme = darkColorScheme(
            primary = Color(0xFF84F2A4),
            secondary = Color(0xFF79D7FF),
            tertiary = Color(0xFFFFB3C1),
            background = Color(0xFF101414),
            surface = Color(0xFF161B1A),
            surfaceVariant = Color(0xFF29312E),
            surfaceContainer = Color(0xFF1E2523),
            outline = Color(0xFF6F7975),
            outlineVariant = Color(0xFF414946)
        )

        val result = applyAmoledSurfaceOverrides(monetScheme)

        assertEquals(monetScheme.primary, result.primary)
        assertEquals(monetScheme.secondary, result.secondary)
        assertEquals(monetScheme.tertiary, result.tertiary)
        assertEquals(Color.Black, result.background)
        assertEquals(Color.Black, result.surface)
        assertEquals(Color(0xFF050505), result.surfaceVariant)
        assertEquals(Color(0xFF090909), result.surfaceContainer)
    }

    @Test
    fun `static md3 light scheme derives distinct secondary and tertiary roles from source color`() {
        val scheme = createStaticMd3ColorScheme(
            primaryColor = Color(0xFF6750A4),
            darkTheme = false,
            amoledDarkTheme = false
        )

        assertNotEquals(scheme.primary, scheme.secondary)
        assertNotEquals(scheme.primary, scheme.tertiary)
        assertNotEquals(scheme.primaryContainer, scheme.secondaryContainer)
        assertNotEquals(scheme.primaryContainer, scheme.tertiaryContainer)
        assertTrue(calculateContrastRatio(scheme.onPrimaryContainer, scheme.primaryContainer) >= 4.5f)
        assertTrue(calculateContrastRatio(scheme.onSecondaryContainer, scheme.secondaryContainer) >= 4.5f)
        assertTrue(calculateContrastRatio(scheme.onTertiaryContainer, scheme.tertiaryContainer) >= 4.5f)
    }

    @Test
    fun `static palette keeps MaterialKolor primary roles and only tints with seed`() {
        val selectedThemeColor = Color(0xFF007AFF)
        val generatedScheme = lightColorScheme(
            primary = Color(0xFF005BBC),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFD6E3FF),
            onPrimaryContainer = Color(0xFF001B3E),
            background = Color(0xFFF8FBFF),
            surface = Color(0xFFFFFBFE),
        )

        val scheme = alignStaticColorSchemeWithThemePrimary(
            scheme = generatedScheme,
            themePrimaryColor = selectedThemeColor,
            darkTheme = false
        )

        // Do not overwrite HCT-mapped control roles with the raw seed hex.
        assertEquals(generatedScheme.primary, scheme.primary)
        assertEquals(generatedScheme.onPrimary, scheme.onPrimary)
        assertEquals(generatedScheme.primaryContainer, scheme.primaryContainer)
        assertEquals(selectedThemeColor, scheme.surfaceTint)
    }

    @Test
    fun `static palette does not force neon bright seed as light primary`() {
        val neonOrange = Color(0xFFFF6A00)
        val generatedScheme = lightColorScheme(
            primary = Color(0xFFA33B00),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFDBCB),
            onPrimaryContainer = Color(0xFF3A1600),
            surface = Color.White,
            background = Color.White,
        )

        val scheme = alignStaticColorSchemeWithThemePrimary(
            scheme = generatedScheme,
            themePrimaryColor = neonOrange,
            darkTheme = false,
        )

        assertEquals(generatedScheme.primary, scheme.primary)
        assertEquals(generatedScheme.primaryContainer, scheme.primaryContainer)
        assertEquals(neonOrange, scheme.surfaceTint)
        assertTrue(calculateContrastRatio(scheme.onPrimary, scheme.primary) >= 4.5f)
    }

    @Test
    fun `static palette falls back from a source primary that cannot identify controls`() {
        val lowContrastGreen = Color(0xFF34C759)
        val generatedScheme = lightColorScheme(
            primary = Color(0xFF006E2C),
            surface = Color.White,
            background = Color.White,
        )

        val scheme = alignStaticColorSchemeWithThemePrimary(
            scheme = generatedScheme,
            themePrimaryColor = lowContrastGreen,
            darkTheme = false,
        )

        // Seed is not forced; MaterialKolor primary is preserved.
        assertEquals(generatedScheme.primary, scheme.primary)
        assertEquals(lowContrastGreen, scheme.surfaceTint)
        assertTrue(calculateContrastRatio(scheme.primary, scheme.surface) >= 3f)
    }

    @Test
    fun `static md3 surfaces should respond to different source colors instead of staying fixed`() {
        val blueScheme = createStaticMd3ColorScheme(
            primaryColor = Color(0xFF007AFF),
            darkTheme = false,
            amoledDarkTheme = false
        )
        val orangeScheme = createStaticMd3ColorScheme(
            primaryColor = Color(0xFFFF5722),
            darkTheme = false,
            amoledDarkTheme = false
        )

        assertNotEquals(blueScheme.background, orangeScheme.background)
        assertNotEquals(blueScheme.surfaceVariant, orangeScheme.surfaceVariant)
        assertNotEquals(blueScheme.outlineVariant, orangeScheme.outlineVariant)
    }

    @Test
    fun `static md3 dark scheme keeps readable accents and source tinted surfaces`() {
        val scheme = createStaticMd3ColorScheme(
            primaryColor = Color(0xFF34C759),
            darkTheme = true,
            amoledDarkTheme = false
        )

        assertNotEquals(scheme.primary, scheme.secondary)
        assertNotEquals(scheme.primary, scheme.tertiary)
        assertTrue(calculateContrastRatio(scheme.onPrimary, scheme.primary) >= 4.5f)
        assertTrue(calculateContrastRatio(scheme.onSecondary, scheme.secondary) >= 4.5f)
        assertTrue(calculateContrastRatio(scheme.onTertiary, scheme.tertiary) >= 4.5f)
        assertNotEquals(Color(0xFF121212), scheme.background)
        assertNotEquals(Color(0xFF1E1E1E), scheme.surface)
    }

    @Test
    fun `static md3 dark scheme preserves selected theme color as primary`() {
        val selectedThemeColor = Color(0xFF007AFF)

        val scheme = createStaticMd3ColorScheme(
            primaryColor = selectedThemeColor,
            darkTheme = true,
            amoledDarkTheme = false
        )

        assertEquals(selectedThemeColor, scheme.primary)
        assertTrue(calculateContrastRatio(scheme.onPrimary, scheme.primary) >= 4.5f)
    }

    @Test
    fun `ios light scheme keeps grouped list gray background and white cards`() {
        val scheme = createIosColorScheme(
            primaryColor = Color(0xFF007AFF),
            darkTheme = false,
            amoledDarkTheme = false
        )

        assertEquals(iOSSystemGray6, scheme.background)
        assertEquals(Color.White, scheme.surface)
        assertEquals(Color(0xFF007AFF), scheme.primary)
    }

    @Test
    fun `ios dark scheme keeps ios neutral surfaces instead of md3 tinted neutrals`() {
        val iosScheme = createIosColorScheme(
            primaryColor = Color(0xFF34C759),
            darkTheme = true,
            amoledDarkTheme = false
        )
        val md3Scheme = createStaticMd3ColorScheme(
            primaryColor = Color(0xFF34C759),
            darkTheme = true,
            amoledDarkTheme = false
        )

        assertNotEquals(md3Scheme.background, iosScheme.background)
        assertNotEquals(md3Scheme.surface, iosScheme.surface)
        assertEquals(Color(0xFF34C759), iosScheme.primary)
    }

    @Test
    fun `ios dynamic accent merge keeps ios surfaces while adopting monet accents`() {
        val base = createIosColorScheme(
            primaryColor = Color(0xFF007AFF),
            darkTheme = false,
            amoledDarkTheme = false
        )
        val dynamicAccent = lightColorScheme(
            primary = Color(0xFF6750A4),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFEADDFF),
            onPrimaryContainer = Color(0xFF21005D),
            secondary = Color(0xFF625B71),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFE8DEF8),
            onSecondaryContainer = Color(0xFF1D192B),
            tertiary = Color(0xFF7D5260),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFFFD8E4),
            onTertiaryContainer = Color(0xFF31111D),
            background = Color(0xFFFFFBFE),
            surface = Color(0xFFFFFBFE)
        )

        val merged = alignIosColorSchemeWithDynamicAccent(
            baseScheme = base,
            dynamicAccentScheme = dynamicAccent
        )

        assertEquals(base.background, merged.background)
        assertEquals(base.surface, merged.surface)
        assertEquals(dynamicAccent.primary, merged.primary)
        assertEquals(dynamicAccent.secondary, merged.secondary)
        assertEquals(dynamicAccent.tertiary, merged.tertiary)
    }

    @Test
    fun `ios amoled scheme forces black surfaces`() {
        val scheme = createIosColorScheme(
            primaryColor = Color(0xFF007AFF),
            darkTheme = true,
            amoledDarkTheme = true
        )

        assertEquals(Color.Black, scheme.background)
        assertEquals(Color.Black, scheme.surface)
        assertEquals(Color(0xFF007AFF), scheme.primary)
    }

    private fun assertTextContrast(foreground: Color, background: Color, label: String) {
        assertTrue(
            calculateContrastRatio(foreground, background) >= 4.5f,
            "$label text is below 4.5:1",
        )
    }
}
