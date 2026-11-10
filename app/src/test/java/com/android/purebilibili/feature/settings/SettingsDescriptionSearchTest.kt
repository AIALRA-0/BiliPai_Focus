package com.android.purebilibili.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsDescriptionSearchTest {
    @Test
    fun findsConcreteSettingByItsVisibleDescription() {
        val result = resolveSettingsSearchResults("绿屏").first()
        assertEquals("playback.hw_decode", result.settingId)
        assertTrue(result.subtitle.contains("绿屏"))
        assertEquals(SettingsSearchFocusIds.PLAYBACK_DECODER, result.focusId)
    }

    @Test
    fun searchesAlternateStateDescriptionsAndPrefersExactTitle() {
        assertTrue(resolveSettingsSearchResults("关闭后仅手动检查").any { it.settingId == "help.auto_check_update_enabled" })
        assertEquals("playback.hw_decode", resolveSettingsSearchResults("启用硬件解码").first().settingId)
    }

    @Test
    fun snippetShowsMatchingStateInsteadOfTheFirstState() {
        assertEquals("关闭后仅手动检查", resolveSettingsSearchDescriptionSnippet("进入应用时自动检查新版本；关闭后仅手动检查", "手动检查"))
    }

    @Test
    fun navigationRequestKeepsSearchQueryAndSettingIdentity() {
        SettingsSearchFocusController.submit(SettingsSearchTarget.PLAYBACK, SettingsSearchFocusIds.PLAYBACK_DECODER, "playback.hw_decode", query = " 绿屏 ")
        try {
            assertEquals("绿屏", SettingsSearchFocusController.request.value?.query)
            assertEquals("playback.hw_decode", SettingsSearchFocusController.request.value?.settingId)
        } finally {
            SettingsSearchFocusController.clear()
        }
    }
}
