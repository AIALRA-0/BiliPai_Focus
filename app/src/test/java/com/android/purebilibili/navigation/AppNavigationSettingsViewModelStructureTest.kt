package com.android.purebilibili.navigation

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class AppNavigationSettingsViewModelStructureTest {

    @Test
    fun appNavigationProvidesApplicationBackedSettingsViewModelToNavigation3SettingsPages() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt")

        assertTrue(source.contains("val settingsViewModel: SettingsViewModel = viewModel("))
        assertTrue(source.contains("SettingsViewModelFactory(application)"))
        listOf(
            "SettingsScreen",
            "AppearanceSettingsScreen",
            "IconSettingsScreen",
            "AnimationSettingsScreen",
            "PlaybackSettingsScreen",
        ).forEach { screen ->
            assertSettingsScreenUsesSharedViewModel(source, screen)
        }
        assertTrue(source.contains("SettingsShareViewModelFactory(application)"))
        assertTrue(source.contains("viewModel = settingsShareViewModel"))
        assertTrue(source.contains("WebDavBackupViewModelFactory(application)"))
        assertTrue(source.contains("viewModel = webDavBackupViewModel"))
    }

    private fun loadSource(path: String): String {
        val candidates = listOf(
            File(path),
            File("app", path.removePrefix("app/")),
            File(path.removePrefix("app/")),
            File("..", path)
        )
        return candidates.first { it.exists() }.readText()
    }

    private fun assertSettingsScreenUsesSharedViewModel(source: String, screen: String) {
        val callSource = source.substringAfter("$screen(").substringBefore(")")
        assertTrue(callSource.contains("viewModel = settingsViewModel"))
    }
}
