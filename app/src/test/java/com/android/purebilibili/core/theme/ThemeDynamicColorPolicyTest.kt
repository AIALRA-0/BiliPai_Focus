package com.android.purebilibili.core.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.android.purebilibili.feature.settings.AppThemeMode
import com.android.purebilibili.feature.settings.Md3ColorSource
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import top.yukonga.miuix.kmp.theme.ColorSchemeMode

class ThemeDynamicColorPolicyTest {

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
    fun `static palette keeps selected theme color as light primary even when generated palette drifts`() {
        val selectedThemeColor = Color(0xFF007AFF)
        val generatedScheme = lightColorScheme(
            primary = Color(0xFF2E7D32),
            primaryContainer = Color(0xFFC8E6C9),
            background = Color(0xFFF8FBFF)
        )

        val scheme = alignStaticColorSchemeWithThemePrimary(
            scheme = generatedScheme,
            themePrimaryColor = selectedThemeColor,
            darkTheme = false
        )

        assertEquals(selectedThemeColor, scheme.primary)
        assertNotEquals(generatedScheme.primary, scheme.primary)
        assertNotEquals(generatedScheme.primaryContainer, scheme.primaryContainer)
        assertTrue(calculateContrastRatio(scheme.onPrimary, scheme.primary) >= 4.5f)
        assertTrue(calculateContrastRatio(scheme.onPrimaryContainer, scheme.primaryContainer) >= 4.5f)
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
}
