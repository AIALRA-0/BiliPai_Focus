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
        assertTrue(source.contains("SettingsScreen(\n                                viewModel = settingsViewModel"))
        assertTrue(source.contains("AppearanceSettingsScreen(\n                                viewModel = settingsViewModel"))
        assertTrue(source.contains("IconSettingsScreen(\n                                viewModel = settingsViewModel"))
        assertTrue(source.contains("AnimationSettingsScreen(\n                                viewModel = settingsViewModel"))
        assertTrue(source.contains("PlaybackSettingsScreen(\n                                viewModel = settingsViewModel"))
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
}
