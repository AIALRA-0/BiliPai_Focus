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

        assertFalse(result.showHomeRecommendTab)
        assertFalse(result.showHomePopularTab)
        assertFalse(result.showHomeLiveTab)
        assertFalse(result.showHomeGameTab)
        assertFalse(result.showHomePartitionButton)
        assertTrue(result.enableFollowGroupFiltering)
        assertFalse(result.showVideoRelatedVideosSection)
        assertTrue(result.showHistoryClearAllAction)
        assertFalse(result.showSearchHotSection)
    }

    @Test
    fun populatedPreferences_mapVideoRelatedSectionVisibility() {
        val prefs = mutablePreferencesOf(
            booleanPreferencesKey("focus_video_related_videos_section_visible") to true,
            booleanPreferencesKey("focus_follow_group_filtering_enabled") to false,
            booleanPreferencesKey("focus_history_clear_all_action_enabled") to false,
            booleanPreferencesKey("search_hot_section_enabled") to true
        )

        val result = SettingsManager.mapFocusSettingsFromPreferences(prefs)

        assertFalse(result.enableFollowGroupFiltering)
        assertTrue(result.showVideoRelatedVideosSection)
        assertFalse(result.showHistoryClearAllAction)
        assertTrue(result.showSearchHotSection)
    }
}

