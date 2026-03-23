package com.android.purebilibili.feature.settings

internal const val LEGACY_THEME_MODE_AMOLED = 3

internal data class ThemePreferenceState(
    val useDarkTheme: Boolean,
    val useAmoledDarkTheme: Boolean
)

internal fun resolveThemeModePreference(
    themeModeValue: Int
): AppThemeMode {
    return when (themeModeValue) {
        AppThemeMode.FOLLOW_SYSTEM.value -> AppThemeMode.FOLLOW_SYSTEM
        AppThemeMode.LIGHT.value -> AppThemeMode.LIGHT
        AppThemeMode.DARK.value,
        LEGACY_THEME_MODE_AMOLED -> AppThemeMode.DARK
        else -> AppThemeMode.FOLLOW_SYSTEM
    }
}

internal fun resolveDarkThemeStylePreference(
    darkThemeStyleValue: Int?,
    legacyThemeModeValue: Int?
): DarkThemeStyle {
    return when {
        darkThemeStyleValue != null -> DarkThemeStyle.fromValue(darkThemeStyleValue)
        legacyThemeModeValue == LEGACY_THEME_MODE_AMOLED -> DarkThemeStyle.AMOLED
        else -> DarkThemeStyle.DEFAULT
    }
}

internal fun resolveThemePreferenceState(
    themeMode: AppThemeMode,
    darkThemeStyle: DarkThemeStyle,
    systemInDark: Boolean
): ThemePreferenceState {
    val useDarkTheme = when (themeMode) {
        AppThemeMode.FOLLOW_SYSTEM -> systemInDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
    return ThemePreferenceState(
        useDarkTheme = useDarkTheme,
        useAmoledDarkTheme = useDarkTheme && darkThemeStyle == DarkThemeStyle.AMOLED
    )
}
