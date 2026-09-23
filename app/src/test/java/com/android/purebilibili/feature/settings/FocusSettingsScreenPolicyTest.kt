package com.android.purebilibili.feature.settings

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FocusSettingsScreenPolicyTest {

    @Test
    fun `search visibility switches use positive show semantics`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/settings/screen/FocusSettingsScreen.kt")
            .replace(Regex("\\s+"), " ")

        assertTrue(source.contains("title = \"显示大家都在搜\""))
        assertTrue(source.contains("checked = settings.showSearchHotSection"))
        assertTrue(source.contains("SettingsManager.setSearchHotSectionEnabled(context, enabled)"))

        assertTrue(source.contains("title = \"显示搜索发现\""))
        assertTrue(source.contains("checked = settings.showSearchDiscoverSection"))
        assertTrue(source.contains("SettingsManager.setSearchDiscoverSectionEnabled(context, enabled)"))

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
        assertTrue(homeCategory.contains("title = \"Focus 设置\""))
        assertTrue(homeCategory.contains("onClick = actions.onFocusSettingsClick"))
        assertTrue(settingsScreen.contains("onFocusSettingsClick: () -> Unit = {}"))
        assertTrue(settingsScreen.contains("onFocusSettingsClick = onFocusSettingsClick"))
        assertTrue(settingsScreen.contains("onFocusSettingsClick: () -> Unit,"))
    }

    @Test
    fun `focus screen uses adaptive settings components and exposes all stored home tabs`() {
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
        assertTrue(source.contains("SettingsManager.setFocusHomeAnimeTabVisible(context, enabled)"))
        assertTrue(source.contains("SettingsManager.setFocusHomeKnowledgeTabVisible(context, enabled)"))
        assertTrue(source.contains("SettingsManager.setFocusHomeTechTabVisible(context, enabled)"))
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
