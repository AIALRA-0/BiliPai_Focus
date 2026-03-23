package com.android.purebilibili.feature.settings

enum class AppLanguage(val value: Int) {
    FOLLOW_SYSTEM(0),
    SIMPLIFIED_CHINESE(1),
    TRADITIONAL_CHINESE_TAIWAN(2),
    ENGLISH(3);

    companion object {
        fun fromValue(value: Int): AppLanguage = resolveAppLanguagePreference(value)
    }
}

internal fun resolveAppLanguagePreference(rawValue: Int?): AppLanguage {
    return AppLanguage.entries.find { it.value == rawValue } ?: AppLanguage.FOLLOW_SYSTEM
}

internal fun resolveAppLanguageLocaleTags(appLanguage: AppLanguage): List<String> {
    return when (appLanguage) {
        AppLanguage.FOLLOW_SYSTEM -> emptyList()
        AppLanguage.SIMPLIFIED_CHINESE -> listOf("zh-CN")
        AppLanguage.TRADITIONAL_CHINESE_TAIWAN -> listOf("zh-TW")
        AppLanguage.ENGLISH -> listOf("en")
    }
}

internal fun shouldPromptAppRestartForLanguageChange(
    current: AppLanguage,
    requested: AppLanguage
): Boolean = current != requested

internal suspend fun persistAndApplyAppLanguageBeforeRestart(
    appLanguage: AppLanguage,
    persist: suspend (AppLanguage) -> Unit,
    apply: (AppLanguage) -> Unit = ::applyAppLanguage,
    restart: () -> Unit
) {
    persist(appLanguage)
    apply(appLanguage)
    restart()
}
