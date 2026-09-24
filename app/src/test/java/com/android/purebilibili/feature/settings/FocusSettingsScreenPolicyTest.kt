package com.android.purebilibili.feature.settings

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.android.purebilibili.navigation3.BiliPaiNavKey

class FocusSettingsScreenPolicyTest {

    @Test
    fun `search hot and discover switches live outside Focus while history remains Focus-only`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/settings/screen/FocusSettingsScreen.kt")
            .replace(Regex("\\s+"), " ")

        assertFalse(source.contains("settings.showSearchHotSection"))
        assertFalse(source.contains("settings.showSearchDiscoverSection"))
        assertFalse(source.contains("setSearchHotSectionEnabled"))
        assertFalse(source.contains("setSearchDiscoverSectionEnabled"))

        assertTrue(source.contains("title = \"显示搜索历史\""))
        assertTrue(source.contains("checked = settings.showSearchHistorySection"))
        assertTrue(source.contains("SettingsManager.setSearchHistorySectionEnabled(context, enabled)"))

        assertFalse(source.contains("title = \"隐藏大家都在搜\""))
        assertFalse(source.contains("title = \"隐藏搜索发现\""))
        assertFalse(source.contains("title = \"隐藏搜索历史\""))
        assertFalse(source.contains("checked = !settings.showSearchHotSection"))
        assertFalse(source.contains("checked = !settings.showSearchDiscoverSection"))
        assertFalse(source.contains("checked = !settings.showSearchHistorySection"))
    }

    @Test
    fun `focus settings remains reachable from the upstream home category`() {
        val sections = loadSource("app/src/main/java/com/android/purebilibili/feature/settings/ui/SettingsSections.kt")
        val settingsScreen = loadSource("app/src/main/java/com/android/purebilibili/feature/settings/screen/SettingsScreen.kt")
            .replace(Regex("\\s+"), " ")

        assertTrue(sections.contains("val onFocusSettingsClick: () -> Unit"))
        val homeCategory = sections
            .substringAfter("SettingsRootCategory.HOME_RECOMMENDATION -> {")
            .substringBefore("SettingsRootCategory.NAVIGATION_INTERACTION -> {")
        val rootCategoryList = sections.substringAfter("fun SettingsRootCategoryListSection(")
        assertTrue(rootCategoryList.contains("title = \"Focus 专属\""))
        assertTrue(rootCategoryList.contains("onClick = onFocusSettingsClick"))
        assertFalse(homeCategory.contains("onFocusSettingsClick"))
        assertTrue(settingsScreen.contains("onFocusSettingsClick: () -> Unit = {}"))
        assertTrue(settingsScreen.contains("onFocusSettingsClick = onFocusSettingsClick"))
        assertTrue(settingsScreen.contains("onFocusSettingsClick: () -> Unit,"))
    }

    @Test
    fun `focus screen uses adaptive controls only for Focus-owned behavior`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/settings/screen/FocusSettingsScreen.kt")
            .replace(Regex("\\s+"), " ")

        assertTrue(source.contains("SettingsPageScaffold("))
        assertTrue(source.contains("AppPreferenceSectionTitle(title)"))
        assertTrue(source.contains("AppPreferenceGroup(content = content)"))
        assertTrue(source.contains("AppSwitchPreference("))
        assertFalse(source.contains("IOSSectionTitle"))
        assertFalse(source.contains("IOSGroup"))
        assertFalse(source.contains("IOSSwitchItem"))
        assertFalse(source.contains("IOSDivider"))
        assertFalse(source.contains("setFocusHome"))
        assertTrue(source.contains("关注分组过滤"))
        assertTrue(source.contains("搜索历史"))
        assertTrue(source.contains("SettingsManager.setFocusHistoryClearAllActionEnabled"))
    }

    @Test
    fun `search can open Focus settings directly`() {
        val results = resolveSettingsSearchResults("Focus 专属")
        val focusResult = results.first { it.target == SettingsSearchTarget.FOCUS_SETTINGS }

        assertEquals(BiliPaiNavKey.FocusSettings, resolveSettingsSearchNavigation(focusResult))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(
            File(path),
            File(normalizedPath)
        ).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
