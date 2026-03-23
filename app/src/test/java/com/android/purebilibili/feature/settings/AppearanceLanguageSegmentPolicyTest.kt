package com.android.purebilibili.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class AppearanceLanguageSegmentPolicyTest {

    @Test
    fun languageSegmentOptions_exposeAllLanguageModesInStableOrder_andUseProvidedLabels() {
        val options = resolveAppLanguageSegmentOptions(
            followSystemLabel = "Follow System",
            simplifiedChineseLabel = "Simplified",
            traditionalChineseLabel = "Traditional",
            englishLabel = "English"
        )

        assertEquals(
            listOf(
                AppLanguage.FOLLOW_SYSTEM,
                AppLanguage.SIMPLIFIED_CHINESE,
                AppLanguage.TRADITIONAL_CHINESE_TAIWAN,
                AppLanguage.ENGLISH
            ),
            options.map { it.value }
        )
        assertEquals(
            listOf("Follow System", "Simplified", "Traditional", "English"),
            options.map { it.label }
        )
    }
}
