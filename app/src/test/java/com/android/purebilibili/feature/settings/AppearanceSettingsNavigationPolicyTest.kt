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
    fun bottomBarSearchBelongsToNavigationSettings() {
        val appearanceSource = loadSource("app/src/main/java/com/android/purebilibili/feature/settings/screen/AppearanceSettingsScreen.kt")
        val navigationSource = loadSource("app/src/main/java/com/android/purebilibili/feature/settings/screen/BottomBarSettingsScreen.kt")

        assertFalse(appearanceSource.contains("title = \"底栏搜索联动\""))
        assertFalse(appearanceSource.contains("setBottomBarSearchLayoutMode"))
        assertTrue(navigationSource.contains("title = \"底栏搜索联动\""))
        assertTrue(navigationSource.contains("视频小横条随滚动自然收拢或展开"))
        assertFalse(navigationSource.contains("title = \"底栏搜索布局\""))
        assertFalse(navigationSource.contains("title = \"搜索框自动展开\""))
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
