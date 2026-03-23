package com.android.purebilibili.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppLanguageLocalePolicyTest {

    @Test
    fun followSystem_resolvesToEmptyLocaleList() {
        val locales = resolveAppLanguageLocaleList(AppLanguage.FOLLOW_SYSTEM)

        assertTrue(locales.isEmpty)
        assertEquals("", locales.toLanguageTags())
    }

    @Test
    fun explicitLanguages_resolveToExpectedLocaleList() {
        assertEquals(
            "zh-CN",
            resolveAppLanguageLocaleList(AppLanguage.SIMPLIFIED_CHINESE).toLanguageTags()
        )
        assertEquals(
            "zh-TW",
            resolveAppLanguageLocaleList(AppLanguage.TRADITIONAL_CHINESE_TAIWAN).toLanguageTags()
        )
        assertEquals(
            "en",
            resolveAppLanguageLocaleList(AppLanguage.ENGLISH).toLanguageTags()
        )
    }
}
