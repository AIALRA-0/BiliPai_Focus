package com.android.purebilibili.core.store

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FocusSettingsMappingPolicyTest {

    @Test
    fun emptyPreferences_useExpectedFocusDefaults() {
        val result = SettingsManager.mapFocusSettingsFromPreferences(mutablePreferencesOf())

        assertTrue(result.enableFollowGroupFiltering)
        assertFalse(result.showVideoRelatedVideosSection)
        assertTrue(result.showHistoryClearAllAction)
        assertFalse(result.showSearchHistorySection)
    }

    @Test
    fun populatedPreferences_mapVideoRelatedSectionVisibility() {
        val prefs = mutablePreferencesOf(
            booleanPreferencesKey("focus_video_related_videos_section_visible") to true,
            booleanPreferencesKey("focus_follow_group_filtering_enabled") to false,
            booleanPreferencesKey("focus_history_clear_all_action_enabled") to false,
            booleanPreferencesKey("search_history_section_enabled") to true
        )

        val result = SettingsManager.mapFocusSettingsFromPreferences(prefs)

        assertFalse(result.enableFollowGroupFiltering)
        assertTrue(result.showVideoRelatedVideosSection)
        assertFalse(result.showHistoryClearAllAction)
        assertTrue(result.showSearchHistorySection)
    }

    @Test
    fun legacyTopTabVisibility_mapsForOneTimeMigrationOnly() {
        val prefs = mutablePreferencesOf(
            booleanPreferencesKey("focus_home_follow_tab_visible") to false,
            booleanPreferencesKey("focus_home_anime_tab_visible") to false,
            booleanPreferencesKey("focus_home_knowledge_tab_visible") to true,
            booleanPreferencesKey("focus_home_partition_button_visible") to true,
        )

        val result = FocusSettingsStore.mapLegacyHomeTabVisibilityFromPreferences(prefs)

        assertFalse(result.showFollow)
        assertFalse(result.showAnime)
        assertTrue(result.showKnowledge)
        assertTrue(result.showPartition)
    }
}
