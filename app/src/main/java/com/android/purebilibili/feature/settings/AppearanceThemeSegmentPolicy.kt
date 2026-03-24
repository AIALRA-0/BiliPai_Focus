package com.android.purebilibili.feature.settings

internal fun resolveThemeModeSegmentOptions(
    followSystemLabel: String = AppThemeMode.FOLLOW_SYSTEM.label,
    lightLabel: String = AppThemeMode.LIGHT.label,
    darkLabel: String = AppThemeMode.DARK.label
): List<PlaybackSegmentOption<AppThemeMode>> {
    return listOf(
        PlaybackSegmentOption(AppThemeMode.FOLLOW_SYSTEM, followSystemLabel),
        PlaybackSegmentOption(AppThemeMode.LIGHT, lightLabel),
        PlaybackSegmentOption(AppThemeMode.DARK, darkLabel)
    )
}

internal fun resolveDarkThemeStyleSegmentOptions(
    defaultLabel: String = DarkThemeStyle.DEFAULT.label,
    amoledLabel: String = DarkThemeStyle.AMOLED.label
): List<PlaybackSegmentOption<DarkThemeStyle>> {
    return listOf(
        PlaybackSegmentOption(DarkThemeStyle.DEFAULT, defaultLabel),
        PlaybackSegmentOption(DarkThemeStyle.AMOLED, amoledLabel)
    )
}

internal fun resolveAppLanguageSegmentOptions(
    followSystemLabel: String = "跟随系统",
    simplifiedChineseLabel: String = "简体中文",
    traditionalChineseLabel: String = "繁體中文",
    englishLabel: String = "英语"
): List<PlaybackSegmentOption<AppLanguage>> {
    return listOf(
        PlaybackSegmentOption(AppLanguage.FOLLOW_SYSTEM, followSystemLabel),
        PlaybackSegmentOption(AppLanguage.SIMPLIFIED_CHINESE, simplifiedChineseLabel),
        PlaybackSegmentOption(AppLanguage.TRADITIONAL_CHINESE_TAIWAN, traditionalChineseLabel),
        PlaybackSegmentOption(AppLanguage.ENGLISH, englishLabel)
    )
}
