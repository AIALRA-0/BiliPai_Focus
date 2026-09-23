package com.android.purebilibili.feature.settings

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsLocalBackHandlerStructureTest {

    @Test
    fun settingsLocalBackHandlerUsesMiuixCompletionOnlyApi() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/settings/SettingsLocalBackHandler.kt"
        )

        assertTrue(source.contains("NavigationBackHandler("))
        assertTrue(source.contains("rememberNavigationEventState(NavigationEventInfo.None)"))
        assertTrue(source.contains("isBackEnabled = enabled"))
        assertTrue(source.contains("onBackCompleted = onBackCompleted"))
        assertFalse(source.contains("reportPredictiveProgress"))
    }

    @Test
    fun settingsScreens_avoidClassicBackHandler() {
        val paths = listOf(
            "app/src/main/java/com/android/purebilibili/feature/settings/screen/SettingsScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/settings/screen/PluginsScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/settings/screen/SettingsTabletShell.kt"
        )

        val offenders = paths.filter { path ->
            val source = loadSource(path)
            source.contains("BackHandler(") && !source.contains("NavigationBackHandler(") &&
                !source.contains("SettingsLocalBackHandler(")
        }

        assertTrue(
            offenders.isEmpty(),
            offenders.joinToString(prefix = "Classic BackHandler still used in:\n")
        )
    }

    @Test
    fun settingsScreen_registersNav3CategoryNavigation() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/settings/screen/SettingsScreen.kt"
        )

        assertTrue(source.contains("MobileSettingsNavLayout("))
        assertTrue(source.contains("onCategoryClick"))
        assertTrue(source.contains("onSearchOpen"))
        assertFalse(source.contains("SettingsRootDrillDownNavigator("))
    }

    @Test
    fun settingsScreen_registersOneCoordinatedLocalBackHandler() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/settings/screen/SettingsScreen.kt"
        )

        assertTrue(source.contains("val settingsBackTarget = resolveSettingsBackTarget("))
        assertTrue(source.contains("SettingsLocalBackHandler(enabled = settingsBackTarget != SettingsBackTarget.NONE)"))
        assertTrue(source.indexOf("SettingsLocalBackHandler(") == source.lastIndexOf("SettingsLocalBackHandler("))
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
