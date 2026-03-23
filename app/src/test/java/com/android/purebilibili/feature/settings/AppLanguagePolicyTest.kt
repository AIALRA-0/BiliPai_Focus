package com.android.purebilibili.feature.settings

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppLanguagePolicyTest {

    @Test
    fun invalidRawValue_fallsBackToFollowSystem() {
        assertEquals(
            AppLanguage.FOLLOW_SYSTEM,
            resolveAppLanguagePreference(rawValue = 999)
        )
    }

    @Test
    fun explicitLanguageValues_mapToExpectedOptions() {
        assertEquals(
            AppLanguage.SIMPLIFIED_CHINESE,
            resolveAppLanguagePreference(rawValue = AppLanguage.SIMPLIFIED_CHINESE.value)
        )
        assertEquals(
            AppLanguage.TRADITIONAL_CHINESE_TAIWAN,
            resolveAppLanguagePreference(rawValue = AppLanguage.TRADITIONAL_CHINESE_TAIWAN.value)
        )
        assertEquals(
            AppLanguage.ENGLISH,
            resolveAppLanguagePreference(rawValue = AppLanguage.ENGLISH.value)
        )
    }

    @Test
    fun followSystem_usesEmptyLocaleTags() {
        assertTrue(resolveAppLanguageLocaleTags(AppLanguage.FOLLOW_SYSTEM).isEmpty())
    }

    @Test
    fun explicitLanguages_mapToExpectedLocaleTags() {
        assertEquals(
            listOf("zh-CN"),
            resolveAppLanguageLocaleTags(AppLanguage.SIMPLIFIED_CHINESE)
        )
        assertEquals(
            listOf("zh-TW"),
            resolveAppLanguageLocaleTags(AppLanguage.TRADITIONAL_CHINESE_TAIWAN)
        )
        assertEquals(
            listOf("en"),
            resolveAppLanguageLocaleTags(AppLanguage.ENGLISH)
        )
    }

    @Test
    fun restartPrompt_onlyAppearsWhenLanguageActuallyChanges() {
        assertFalse(shouldPromptAppRestartForLanguageChange(AppLanguage.ENGLISH, AppLanguage.ENGLISH))
        assertTrue(shouldPromptAppRestartForLanguageChange(AppLanguage.FOLLOW_SYSTEM, AppLanguage.ENGLISH))
        assertTrue(
            shouldPromptAppRestartForLanguageChange(
                AppLanguage.TRADITIONAL_CHINESE_TAIWAN,
                AppLanguage.SIMPLIFIED_CHINESE
            )
        )
    }

    @Test
    fun languageRestart_persistsBeforeApplyAndRestart() = runTest {
        val events = mutableListOf<String>()

        persistAndApplyAppLanguageBeforeRestart(
            appLanguage = AppLanguage.TRADITIONAL_CHINESE_TAIWAN,
            persist = { events += "persist:${it.name}" },
            apply = { events += "apply:${it.name}" },
            restart = { events += "restart" }
        )

        assertEquals(
            listOf(
                "persist:TRADITIONAL_CHINESE_TAIWAN",
                "apply:TRADITIONAL_CHINESE_TAIWAN",
                "restart"
            ),
            events
        )
    }
}
