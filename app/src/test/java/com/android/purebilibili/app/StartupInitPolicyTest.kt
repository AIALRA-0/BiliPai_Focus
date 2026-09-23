package com.android.purebilibili.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupInitPolicyTest {

    @Test
    fun homeVisualDefaultsMigrationDoesNotBlockColdStart() {
        assertFalse(PureApplicationRuntimeConfig.shouldBlockStartupForHomeVisualDefaultsMigration())
    }

    @Test
    fun bundledDefaultsAreScheduledOffMainBeforeLaunchThemeSetup() {
        val source = loadProjectFile(
            "app/src/main/java/com/android/purebilibili/app/PureApplication.kt"
        )
        val onCreate = source
            .substringAfter("override fun onCreate()")
            .substringBefore("internal fun retryStartupFromRecovery()")
        val initializer = source
            .substringAfter("private fun applyBundledDefaultSettingsAsync()")
            .substringBefore("private fun installStrictModeForDebugBuilds()")

        assertFalse(source.contains("runBlocking("))
        assertTrue(initializer.contains("AppScope.ioScope.launch"))
        assertTrue(initializer.contains("applyBundledDefaultIfNeeded()"))
        assertTrue(initializer.indexOf("applyBundledDefaultIfNeeded()") <
            initializer.indexOf("ensureHomeVisualDefaults("))
        assertTrue(onCreate.indexOf("\n        applyBundledDefaultSettingsAsync()") <
            onCreate.indexOf("\n        applyThemePreference()"))
        assertTrue(onCreate.indexOf("\n        applyThemePreference()") <
            onCreate.indexOf("\n        super.onCreate()"))
    }

    @Test
    fun bundledDefaultsDoNotOverrideSynchronousLaunchModeOrLanguage() {
        val profile = loadProjectFile("app/src/main/assets/default_settings_profile.json")
        val launchPreferenceKey = Regex("\"(theme_mode(_v2)?|app_language(_v1)?)\"\\s*:")

        assertFalse(launchPreferenceKey.containsMatchIn(profile))
        assertTrue(profile.contains("\"theme_selection_v1\": \"MATERIAL3\""))
        assertTrue(profile.contains("\"md3_color_source\": \"FOLLOW_WALLPAPER\""))
    }

    @Test
    fun defersPlaylistRestoreAtStartup() {
        assertTrue(PureApplicationRuntimeConfig.shouldDeferPlaylistRestoreAtStartup())
    }

    @Test
    fun defersTelemetryInitializationAtStartup() {
        assertTrue(PureApplicationRuntimeConfig.shouldDeferTelemetryInitAtStartup())
    }

    @Test
    fun usesStableDeferredStartupDelayWindow() {
        assertEquals(900L, PureApplicationRuntimeConfig.deferredNonCriticalStartupDelayMs())
    }

    private fun loadProjectFile(path: String): String {
        val sourceFile = listOf(File(path), File(path.removePrefix("app/")))
            .firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
