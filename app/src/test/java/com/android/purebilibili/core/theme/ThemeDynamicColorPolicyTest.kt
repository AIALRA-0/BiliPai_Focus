package com.android.purebilibili.core.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import com.android.purebilibili.feature.settings.AppThemeMode
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import top.yukonga.miuix.kmp.theme.ColorSchemeMode

class ThemeDynamicColorPolicyTest {

    @Test
    fun `dynamic color follows miuix monet modes for each app theme mode`() {
        assertEquals(
            ColorSchemeMode.MonetSystem,
            resolveMiuixColorSchemeMode(
                themeMode = AppThemeMode.FOLLOW_SYSTEM,
                dynamicColorEnabled = true
            )
        )
        assertEquals(
            ColorSchemeMode.MonetLight,
            resolveMiuixColorSchemeMode(
                themeMode = AppThemeMode.LIGHT,
                dynamicColorEnabled = true
            )
        )
        assertEquals(
            ColorSchemeMode.MonetDark,
            resolveMiuixColorSchemeMode(
                themeMode = AppThemeMode.DARK,
                dynamicColorEnabled = true
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
}
