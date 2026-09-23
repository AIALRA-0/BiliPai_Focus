package com.android.purebilibili.feature.settings

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsDiagnosticsPolicyTest {

    @Test
    fun `diagnostics only starts when no load is active or cached`() {
        assertTrue(
            shouldStartSettingsDiagnostics(
                loadState = SettingsDiagnosticsLoadState.NOT_LOADED,
                jobActive = false,
            )
        )
        assertFalse(
            shouldStartSettingsDiagnostics(
                loadState = SettingsDiagnosticsLoadState.LOADING,
                jobActive = true,
            )
        )
        assertFalse(
            shouldStartSettingsDiagnostics(
                loadState = SettingsDiagnosticsLoadState.LOADED,
                jobActive = false,
            )
        )
    }

    @Test
    fun `settings root delegates diagnostics to view model without rescanning on entry`() {
        val screenSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/settings/screen/SettingsScreen.kt"
        )
        val viewModelSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/settings/SettingsViewModel.kt"
        )
        val screenEffect = screenSource
            .substringAfter("LaunchedEffect(viewModel)")
            .substringBefore("//  Transparent Navigation Bar")

        assertTrue(screenEffect.contains("viewModel.ensureDiagnosticsLoaded()"))
        assertFalse(screenEffect.contains("viewModel.refreshCacheSize()"))
        assertFalse(screenEffect.contains("calculateInstalledApkSha256(context)"))
        assertFalse(screenEffect.contains("AppUpdateChecker.check"))
        assertTrue(viewModelSource.contains("private var diagnosticsLoadJob: Job? = null"))
        assertTrue(viewModelSource.contains("diagnosticsLoadJob?.isActive == true"))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        return listOf(File(path), File(normalizedPath))
            .first { it.exists() }
            .readText()
    }
}
