package com.android.purebilibili.feature.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

internal fun resolveAppLanguageLocaleList(appLanguage: AppLanguage): LocaleListCompat {
    val languageTags = resolveAppLanguageLocaleTags(appLanguage)
    return if (languageTags.isEmpty()) {
        LocaleListCompat.getEmptyLocaleList()
    } else {
        LocaleListCompat.forLanguageTags(languageTags.joinToString(","))
    }
}

internal fun applyAppLanguage(appLanguage: AppLanguage) {
    AppCompatDelegate.setApplicationLocales(resolveAppLanguageLocaleList(appLanguage))
}
