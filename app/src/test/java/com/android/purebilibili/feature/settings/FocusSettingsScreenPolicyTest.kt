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
