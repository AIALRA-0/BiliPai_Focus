package com.android.purebilibili.feature.settings

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class SettingsEntranceMotionStructureTest {

    @Test
    fun bottomPagerHostBypassesSettingsRootEntrance() {
        val navigationSource = source("app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt")

        assertTrue(navigationSource.contains("isBottomPagerHosted = true"))
        assertTrue(navigationSource.contains("rootEntranceEnabled = !isBottomPagerHosted"))
    }

    @Test
    fun settingsRootWaitsForNavigationAndBypassesGroupWhenDisabled() {
        val settingsSource = source(
            "app/src/main/java/com/android/purebilibili/feature/settings/screen/SettingsScreen.kt"
        )

        assertTrue(settingsSource.contains("LocalAnimatedVisibilityScope.current?.transition?.isRunning == true"))
        assertTrue(settingsSource.contains("EntranceGroup(startWhen = rootEntranceStartWhen)"))
        assertTrue(settingsSource.contains("if (rootEntranceEnabled)"))
        assertTrue(settingsSource.contains("else {\n            SettingsRootContent()"))
    }

    @Test
    fun settingsSearchUsesSameNavigationGate() {
        val searchSource = source(
            "app/src/main/java/com/android/purebilibili/feature/settings/screen/SettingsSearchScreen.kt"
        )

        assertTrue(searchSource.contains("LocalAnimatedVisibilityScope.current?.transition?.isRunning == true"))
        assertTrue(searchSource.contains("shouldStartSettingsEntrance("))
        assertTrue(searchSource.contains("EntranceGroup(startWhen = rootEntranceStartWhen)"))
    }

    private fun source(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        // 统一换行为 \n：Windows CRLF 检出会让多行子串匹配失效。
        return listOf(File(path), File(normalizedPath)).first { it.exists() }
            .readText()
            .replace("\r\n", "\n")
    }
}
