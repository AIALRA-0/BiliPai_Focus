package com.android.purebilibili.feature.settings

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppearanceSettingsNavigationPolicyTest {

    @Test
    fun appearanceSettings_noLongerHostsNavigationManagementShortcuts() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/settings/screen/AppearanceSettingsScreen.kt")

        assertFalse(source.contains("openTopTabManagement("))
        assertFalse(source.contains("title = \"顶部标签页\""))
        assertFalse(source.contains("title = \"顶部栏自动收缩\""))
        assertFalse(source.contains("title = \"侧边导航栏\""))
    }

    @Test
    fun appearanceSettings_bottomBarSearchCopyUsesEntrySemantics() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/settings/screen/AppearanceSettingsScreen.kt")

        assertTrue(source.contains("title = \"底栏搜索入口\""))
        assertTrue(source.contains("subtitle = \"在悬浮底栏右侧显示搜索入口\""))
        assertFalse(source.contains("subtitle = \"在悬浮底栏右侧显示可展开搜索框\""))
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
