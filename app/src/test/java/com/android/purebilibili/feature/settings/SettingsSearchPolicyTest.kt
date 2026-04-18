package com.android.purebilibili.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsSearchPolicyTest {

    @Test
    fun blankQuery_returnsEmptyList() {
        val results = resolveSettingsSearchResults("   ")

        assertTrue(results.isEmpty())
    }

    @Test
    fun queryByChineseKeyword_hitsExpectedSetting() {
        val results = resolveSettingsSearchResults("缓存")

        assertTrue(results.any { it.target == SettingsSearchTarget.CLEAR_CACHE })
    }

    @Test
    fun queryByEnglishAlias_isCaseInsensitive() {
        val results = resolveSettingsSearchResults("gItHuB")

        assertTrue(results.any { it.target == SettingsSearchTarget.OPEN_SOURCE_HOME })
    }

    @Test
    fun prefixMatch_ranksBeforeGenericContains() {
        val results = resolveSettingsSearchResults("检查")

        assertEquals(SettingsSearchTarget.CHECK_UPDATE, results.firstOrNull()?.target)
    }

    @Test
    fun limit_isRespected() {
        val results = resolveSettingsSearchResults("设", maxResults = 3)

        assertEquals(3, results.size)
    }

    @Test
    fun queryByShareKeyword_hitsSettingsShareEntry() {
        val results = resolveSettingsSearchResults("导入")

        assertTrue(results.any { it.target == SettingsSearchTarget.SETTINGS_SHARE })
    }

    @Test
    fun queryByGlassKeyword_hitsAppearanceEntry() {
        val results = resolveSettingsSearchResults("玻璃")

        assertTrue(results.any { it.target == SettingsSearchTarget.APPEARANCE })
    }

    @Test
    fun queryByUpBadgeKeyword_hitsAppearanceEntry() {
        val results = resolveSettingsSearchResults("UP主标识")

        assertTrue(results.any { it.target == SettingsSearchTarget.APPEARANCE })
    }

    @Test
    fun queryByMd3Alias_hitsAppearanceEntry() {
        val results = resolveSettingsSearchResults("md3")

        assertTrue(results.any { it.target == SettingsSearchTarget.APPEARANCE })
    }

    @Test
    fun queryByPinyin_hitsChineseAlias() {
        val results = resolveSettingsSearchResults("waiguan")

        assertTrue(results.any { it.target == SettingsSearchTarget.APPEARANCE })
    }

    @Test
    fun queryByPredictiveBack_hitsAppearanceEntry() {
        val results = resolveSettingsSearchResults("预测性返回")

        assertTrue(results.any { it.target == SettingsSearchTarget.APPEARANCE })
    }

    @Test
    fun queryByPictureInPicture_hitsPlaybackEntry() {
        val results = resolveSettingsSearchResults("画中画")

        assertTrue(results.any { it.target == SettingsSearchTarget.PLAYBACK })
    }

    @Test
    fun queryByAutoRotate_hitsPlaybackEntry() {
        val results = resolveSettingsSearchResults("自动横竖屏")

        assertTrue(results.any { it.target == SettingsSearchTarget.PLAYBACK })
    }

    @Test
    fun queryByAutoCheckUpdate_hitsCheckUpdateEntry() {
        val results = resolveSettingsSearchResults("自动检查更新")

        assertTrue(results.any { it.target == SettingsSearchTarget.CHECK_UPDATE })
    }

    @Test
    fun queryByBottomBar_surfacesTopTabDiscoverabilityInSubtitle() {
        val result = resolveSettingsSearchResults("底栏").firstOrNull {
            it.target == SettingsSearchTarget.BOTTOM_BAR && it.title == "底栏设置"
        }

        assertEquals("自定义底栏和顶部标签", result?.subtitle)
    }
}
