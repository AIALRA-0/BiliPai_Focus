package com.android.purebilibili.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThemePreferencePolicyTest {

    @Test
    fun legacyAmoledThemeMode_migratesToDarkModeAndAmoledStyle() {
        val themeMode = resolveThemeModePreference(themeModeValue = 3)
        val darkThemeStyle = resolveDarkThemeStylePreference(
            darkThemeStyleValue = null,
            legacyThemeModeValue = 3
        )

        assertEquals(AppThemeMode.DARK, themeMode)
        assertEquals(DarkThemeStyle.AMOLED, darkThemeStyle)
    }

    @Test
    fun explicitDarkThemeStyle_preventsLegacyFallbackFromOverridingSelection() {
        val darkThemeStyle = resolveDarkThemeStylePreference(
            darkThemeStyleValue = DarkThemeStyle.DEFAULT.value,
            legacyThemeModeValue = 3
        )

        assertEquals(DarkThemeStyle.DEFAULT, darkThemeStyle)
    }

    @Test
    fun followSystem_withAmoledStyle_usesPureBlackOnlyWhenSystemIsDark() {
        val darkState = resolveThemePreferenceState(
            themeMode = AppThemeMode.FOLLOW_SYSTEM,
            darkThemeStyle = DarkThemeStyle.AMOLED,
            systemInDark = true
        )
        val lightState = resolveThemePreferenceState(
            themeMode = AppThemeMode.FOLLOW_SYSTEM,
            darkThemeStyle = DarkThemeStyle.AMOLED,
            systemInDark = false
        )

        assertTrue(darkState.useDarkTheme)
        assertTrue(darkState.useAmoledDarkTheme)
        assertFalse(lightState.useDarkTheme)
        assertFalse(lightState.useAmoledDarkTheme)
    }

    @Test
    fun lightMode_disablesAmoledEvenWhenAmoledStyleIsSelected() {
        val state = resolveThemePreferenceState(
            themeMode = AppThemeMode.LIGHT,
            darkThemeStyle = DarkThemeStyle.AMOLED,
            systemInDark = true
        )

        assertFalse(state.useDarkTheme)
        assertFalse(state.useAmoledDarkTheme)
    }

    @Test
    fun md3ColorSource_defaultsToWallpaperAndUsesLegacyDynamicColorWhenMissing() {
        assertEquals(
            Md3ColorSource.FOLLOW_WALLPAPER,
            resolveMd3ColorSourcePreference(
                sourceValue = null,
                legacyDynamicColorEnabled = null
            )
        )
        assertEquals(
            Md3ColorSource.FOLLOW_WALLPAPER,
            resolveMd3ColorSourcePreference(
                sourceValue = null,
                legacyDynamicColorEnabled = true
            )
        )
        assertEquals(
            Md3ColorSource.CUSTOM,
            resolveMd3ColorSourcePreference(
                sourceValue = null,
                legacyDynamicColorEnabled = false
            )
        )
    }

    @Test
    fun explicitMd3ColorSourceWinsOverLegacyDynamicColor() {
        assertEquals(
            Md3ColorSource.CUSTOM,
            resolveMd3ColorSourcePreference(
                sourceValue = Md3ColorSource.CUSTOM.name,
                legacyDynamicColorEnabled = true
            )
        )
        assertEquals(
            Md3ColorSource.FOLLOW_WALLPAPER,
            resolveMd3ColorSourcePreference(
                sourceValue = Md3ColorSource.FOLLOW_WALLPAPER.name,
                legacyDynamicColorEnabled = false
            )
        )
    }

    @Test
    fun md3CustomColorHex_normalizesRgbHexAndRejectsInvalidValues() {
        assertEquals("#FA7298", normalizeMd3CustomColorHex("fa7298"))
        assertEquals("#007AFF", normalizeMd3CustomColorHex("#007aff"))
        assertEquals("#007AFF", normalizeMd3CustomColorHex("not-a-color"))
        assertEquals("#007AFF", normalizeMd3CustomColorHex("#12345"))
        assertEquals("#007AFF", normalizeMd3CustomColorHex("#FF007AFF"))
    }
}
