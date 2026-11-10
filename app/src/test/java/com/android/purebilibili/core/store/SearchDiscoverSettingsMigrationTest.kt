package com.android.purebilibili.core.store

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchDiscoverSettingsMigrationTest {
    @Test
    fun oldPersonalizationOptOutDoesNotHideSearchDiscover() {
        val saved = mutablePreferencesOf(booleanPreferencesKey("search_suggestions_enabled") to false)

        assertTrue(SettingsManager.resolveSearchDiscoverSectionEnabled(saved))
        assertFalse(SettingsManager.resolveSearchSuggestionsEnabled(saved))
    }

    @Test
    fun existingVisibilitySettingRemainsAuthoritative() {
        val oldPersonalization = booleanPreferencesKey("search_suggestions_enabled")
        val visibility = booleanPreferencesKey("search_discover_section_enabled")

        assertFalse(SettingsManager.resolveSearchDiscoverSectionEnabled(
            mutablePreferencesOf(oldPersonalization to true, visibility to false)
        ))
        assertTrue(SettingsManager.resolveSearchDiscoverSectionEnabled(
            mutablePreferencesOf(oldPersonalization to false, visibility to true)
        ))
    }

    @Test
    fun togglingVisibilityPreservesTheOldPersonalizationOptOut() {
        val saved = mutablePreferencesOf(booleanPreferencesKey("search_suggestions_enabled") to false)
        val visibility = booleanPreferencesKey("search_discover_section_enabled")

        saved[visibility] = false
        assertFalse(SettingsManager.resolveSearchDiscoverSectionEnabled(saved))
        assertFalse(SettingsManager.resolveSearchSuggestionsEnabled(saved))

        saved[visibility] = true
        assertTrue(SettingsManager.resolveSearchDiscoverSectionEnabled(saved))
        assertFalse(SettingsManager.resolveSearchSuggestionsEnabled(saved))
    }

    @Test
    fun changingPersonalizationDoesNotShowAHiddenSection() {
        val personalized = booleanPreferencesKey("search_suggestions_enabled")
        val saved = mutablePreferencesOf(
            booleanPreferencesKey("search_discover_section_enabled") to false,
            personalized to false
        )
        assertFalse(SettingsManager.resolveSearchSuggestionsEnabled(saved))

        saved[personalized] = true
        assertFalse(SettingsManager.resolveSearchDiscoverSectionEnabled(saved))
        assertTrue(SettingsManager.resolveSearchSuggestionsEnabled(saved))
    }
}
