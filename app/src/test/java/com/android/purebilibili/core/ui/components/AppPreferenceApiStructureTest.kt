package com.android.purebilibili.core.ui.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppPreferenceApiStructureTest {

    @Test
    fun neutralPreferenceApi_delegatesToExistingAdaptiveRenderers() {
        val source = loadSource(
            "design-system/src/main/java/com/android/purebilibili/core/ui/components/AppPreferenceComponents.kt"
        )

        assertTrue(source.contains("fun AppPreference("))
        assertTrue(source.contains("fun AppSwitchPreference("))
        assertTrue(source.contains("fun AppSliderPreference("))
        assertTrue(source.contains("fun AppPreferenceGroup("))
        assertTrue(source.contains("AppPreferenceGroupPresentation"))
        assertTrue(source.contains("fun AppPreferenceSectionTitle("))
        assertTrue(source.contains("fun AppPreferenceDivider("))
        assertTrue(source.contains("fun AppTextField("))
        assertTrue(source.contains("fun AppSearchField("))
        assertTrue(source.contains(") = AdaptiveSearchFieldRenderer("))
        assertTrue(source.contains(") = AdaptivePreferenceContent("))
        assertTrue(source.contains(") = AdaptiveSwitchPreferenceContent("))
        assertTrue(source.contains(") = AdaptiveSliderPreferenceRenderer("))
        assertTrue(source.contains(") = AdaptivePreferenceGroupRenderer("))

        val dialogSource = loadSource(
            "design-system/src/main/java/com/android/purebilibili/core/ui/AppDialogComponents.kt"
        )
        assertTrue(dialogSource.contains("fun AppAlertDialog("))
        assertTrue(dialogSource.contains(") = AdaptiveAlertDialog("))
        assertTrue(dialogSource.contains("icon: @Composable (() -> Unit)? = null"))
        assertTrue(dialogSource.contains("shape: Shape? = null"))
        assertTrue(dialogSource.contains("containerColor: Color? = null"))
        assertTrue(dialogSource.contains("tonalElevation: Dp? = null"))
        assertTrue(dialogSource.contains("fun AppDialogAction("))
        assertTrue(dialogSource.contains(") = AdaptiveDialogAction("))

        val listSource = loadSource(
            "design-system/src/main/java/com/android/purebilibili/core/ui/components/AdaptivePreferenceComponents.kt"
        )
        assertTrue(listSource.contains("fun AppSearchEntry("))
    }

    @Test
    fun phaseOneSettingsPilot_usesNeutralPreferenceNames() {
        val pilotPaths = listOf(
            "app/src/main/java/com/android/purebilibili/feature/settings/ui/SettingsSections.kt",
            "app/src/main/java/com/android/purebilibili/feature/settings/screen/AppearanceSettingsScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/settings/screen/PlaybackSettingsScreen.kt",
            "app/src/main/java/com/android/purebilibili/feature/settings/screen/PluginsScreen.kt",
        )
        val legacyCall = Regex(
            """\b(IOSSectionTitle|IOSGroup|IOSSwitchItem|IOSSliderPreference|IOSClickableItem|IOSDivider|IOSAdaptiveTextField|IOSSlidingSegmentedControl|IOSSlidingSegmentedSetting|IOSAlertDialog|IOSDialogAction)\b"""
        )

        val requiredNeutralCalls = mapOf(
            pilotPaths[0] to listOf("AppPreferenceGroup", "SettingsSingleChoicePreference", "AppAlertDialog"),
            pilotPaths[1] to listOf("AppPreferenceGroup", "SettingsSingleChoicePreference", "AppTextField"),
            pilotPaths[2] to listOf("AppPreferenceGroup", "AppSliderDialogPreference", "AppAlertDialog"),
            pilotPaths[3] to listOf("AppTextField"),
        )

        pilotPaths.forEach { path ->
            val source = loadSource(path)
            assertFalse(legacyCall.containsMatchIn(source), "Legacy preference call remains in $path")
            requiredNeutralCalls.getValue(path).forEach { neutralCall ->
                assertTrue(source.contains(neutralCall), "$neutralCall is missing from $path")
            }
        }
    }

    @Test
    fun settingsSelectionApi_usesDialogPreferencesWithoutInlineSegments() {
        val settingsRoot = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/settings"),
            File("src/main/java/com/android/purebilibili/feature/settings"),
        ).first(File::isDirectory)
        val pluginRoot = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/plugin"),
            File("src/main/java/com/android/purebilibili/feature/plugin"),
        ).first(File::isDirectory)
        val selectionSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/settings/SettingsSelectionComponents.kt"
        )
        val appearanceSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/settings/screen/AppearanceSettingsScreen.kt"
        )
        val settingsComponentsSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/settings/ui/SettingsComponents.kt"
        )
        val screenSources = settingsRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name.endsWith("SettingsScreen.kt") }
            .joinToString("\n") { it.readText() }
        val pluginSources = pluginRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .joinToString("\n") { it.readText() }

        assertTrue(selectionSource.contains("AppSingleChoicePreference("))
        assertFalse(screenSources.contains("AppSegmentedPreference("))
        assertFalse(screenSources.contains("AppSegmentedControl("))
        assertFalse(pluginSources.contains("AppNativeSegmentedControl("))
        assertFalse(screenSources.contains("AppSlider("))
        assertTrue(screenSources.contains("AppSliderDialogPreference("))
        assertFalse(appearanceSource.contains("ThemePresetDropdownSetting("))
        assertFalse(appearanceSource.contains("AppDropdownMenu("))
        assertTrue(settingsComponentsSource.contains("SettingsSingleChoicePreference("))
    }

    @Test
    fun settingsFeatureCallers_useNeutralComponentNames() {
        val settingsRoot = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/settings"),
            File("src/main/java/com/android/purebilibili/feature/settings"),
        ).firstOrNull(File::isDirectory)
            ?: error("Cannot locate settings production sources from ${File(".").absolutePath}")
        val legacyCall = Regex(
            """\b(IOSSectionTitle|IOSGroup|IOSSwitchItem|IOSSliderPreference|IOSClickableItem|IOSDivider|IOSAdaptiveTextField|IOSSlidingSegmentedControl|IOSSlidingSegmentedSetting|IOSAlertDialog|IOSDialogAction)\b"""
        )

        settingsRoot.walkTopDown()
            .filter { file ->
                file.isFile && file.extension == "kt"
            }
            .forEach { file ->
                val source = file.readText().replace("\r\n", "\n")
                assertFalse(
                    legacyCall.containsMatchIn(source),
                    "Legacy component call remains in ${file.relativeTo(settingsRoot)}",
                )
            }
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        return listOf(File(path), File(normalizedPath), File("../$path"))
            .firstOrNull(File::exists)
            ?.readText()
            ?.replace("\r\n", "\n")
            ?: error("Cannot locate $path from ${File(".").absolutePath}")
    }
}
