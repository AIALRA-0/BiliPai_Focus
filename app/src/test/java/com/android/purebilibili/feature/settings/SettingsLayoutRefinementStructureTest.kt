package com.android.purebilibili.feature.settings

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsLayoutRefinementStructureTest {
    private fun source(path: String): String = listOf(File("app/$path"), File(path))
        .first { it.exists() }.readText()

    @Test
    fun settingsRootReturnsToFlatCategoryCardAndKeepsCurrentCopy() {
        val sections = source("src/main/java/com/android/purebilibili/feature/settings/ui/SettingsSections.kt")
        val rootList = sections.substringAfter("internal fun SettingsRootCategoryListSection(")
            .substringBefore("@Composable\nprivate fun SettingsRootCategoryRow")
        assertFalse(rootList.contains("resolveSettingsRootGroups(categories)"))
        assertTrue(rootList.contains("SettingsCardGroup"))
        assertTrue(rootList.contains("settingsDestinationCopy(SettingsSearchTarget.DONATE)"))
        assertTrue(sections.contains("SettingsSectionTitle(title = \"来源与验证\")"))
        assertTrue(sections.contains("SettingsSectionTitle(title = \"使用与反馈\")"))
    }

    @Test
    fun categoryAndTabletShellUseThePreviousSettingsLayout() {
        val category = source("src/main/java/com/android/purebilibili/feature/settings/screen/SettingsCategoryScreen.kt")
        val tablet = source("src/main/java/com/android/purebilibili/feature/settings/screen/SettingsTabletShell.kt")
        assertFalse(category.contains("PlaybackSettingsPage"))
        assertTrue(tablet.contains("ThreePaneScaffoldValue("))
        assertTrue(tablet.contains("extraPane = if (useThreePaneLayout && !isSearchActive) detailPane else null"))
    }

    @Test
    fun mergedPlaybackPageRendersCommentsAndFocusOrderMatchesTheList() {
        val playback = source("src/main/java/com/android/purebilibili/feature/settings/screen/PlaybackSettingsScreen.kt")
        val commentSection = playback.substringAfter(
            "if (page == PlaybackSettingsPage.PLAYBACK || page == PlaybackSettingsPage.COMMENTS) {"
        ).substringBefore("if (page == PlaybackSettingsPage.PLAYBACK || page == PlaybackSettingsPage.FULLSCREEN)")
        assertTrue(commentSection.contains("SettingsSearchFocusIds.PLAYBACK_COMMENTS"))
        assertTrue(commentSection.contains("playbackControls = false"))
        val content = playback.substringAfter("    LazyColumn(")
        val sectionKeys = Regex("item\\(key = SettingsSearchFocusIds\\.(PLAYBACK_[A-Z_]+)\\)")
            .findAll(content).map { match -> match.groupValues[1] }.toList()
        val focusSource = source("src/main/java/com/android/purebilibili/feature/settings/SettingsSearchFocusPolicy.kt")
        val focusKeys = Regex("SettingsSearchFocusIds\\.(PLAYBACK_[A-Z_]+)")
            .findAll(focusSource.substringAfter("internal val playbackSettingsSectionKeys = listOf(")
                .substringBefore(")")).map { it.groupValues[1] }.toList()
        kotlin.test.assertEquals(focusKeys, sectionKeys)
    }

    @Test
    fun playbackAndGlassControlsRemainOnTheirOriginalSettingsPages() {
        val playback = source("src/main/java/com/android/purebilibili/feature/settings/screen/PlaybackSettingsScreen.kt")
        val animation = source("src/main/java/com/android/purebilibili/feature/settings/screen/AnimationSettingsScreen.kt")
        assertTrue(playback.contains("page == PlaybackSettingsPage.PLAYBACK || page == PlaybackSettingsPage.DECODER"))
        assertTrue(playback.contains("page == PlaybackSettingsPage.PLAYBACK || page == PlaybackSettingsPage.FULLSCREEN"))
        assertFalse(playback.contains("item(key = \"playback_advanced\")"))
        assertTrue(animation.contains("LiquidGlassAdjustmentPanel("))
        assertFalse(animation.contains("AppPreference(title = \"玻璃高级调节\""))
    }
}
