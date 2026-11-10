package com.android.purebilibili.core.store

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FocusSearchLandingSectionMigrationPolicyTest {
    private val visibleSectionKeys = listOf(
        KEY_SEARCH_HOT_CONTROL_VISIBLE,
        KEY_SEARCH_HISTORY_CONTROL_VISIBLE,
        KEY_SEARCH_DISCOVER_CONTROL_VISIBLE,
    )

    @Test
    fun newFocusInstall_defaultsAllLandingSectionsToHidden() {
        val preferences = mutablePreferencesOf()

        assertTrue(migrateFocusSearchLandingSections(preferences))
        assertEquals(listOf(false, false, false), visibleSectionKeys.map { preferences[it] })
        assertEquals(1, preferences[KEY_FOCUS_SEARCH_LANDING_SECTION_MIGRATION_VERSION])
        assertEquals(true, preferences[booleanPreferencesKey("search_hot_section_enabled")])
        assertEquals(true, preferences[booleanPreferencesKey("search_discover_section_enabled")])
    }

    @Test
    fun upgrade_preservesEachLegacyFocusSectionVisibility() {
        val preferences = mutablePreferencesOf(
            booleanPreferencesKey("search_hot_section_enabled") to true,
            booleanPreferencesKey("search_history_section_enabled") to false,
            booleanPreferencesKey("search_discover_section_enabled") to true,
        )

        migrateFocusSearchLandingSections(preferences)

        assertEquals(listOf(true, false, true), visibleSectionKeys.map { preferences[it] })
        assertEquals(true, preferences[booleanPreferencesKey("search_hot_section_enabled")])
        assertEquals(true, preferences[booleanPreferencesKey("search_discover_section_enabled")])
    }

    @Test
    fun migration_keepsExistingUpstreamChoicesAndOnlyRunsOnce() {
        val preferences = mutablePreferencesOf(
            KEY_SEARCH_HOT_CONTROL_VISIBLE to true,
            KEY_SEARCH_HISTORY_CONTROL_VISIBLE to true,
            KEY_FOCUS_SEARCH_LANDING_SECTION_MIGRATION_VERSION to 1,
            booleanPreferencesKey("search_hot_section_enabled") to false,
        )

        assertFalse(migrateFocusSearchLandingSections(preferences))

        assertEquals(true, preferences[KEY_SEARCH_HOT_CONTROL_VISIBLE])
        assertEquals(true, preferences[KEY_SEARCH_HISTORY_CONTROL_VISIBLE])
        assertEquals(null, preferences[KEY_SEARCH_DISCOVER_CONTROL_VISIBLE])
        assertEquals(false, preferences[booleanPreferencesKey("search_hot_section_enabled")])
    }

    @Test
    fun firstMigration_doesNotOverwriteAlreadyCreatedSectionControls() {
        val preferences = mutablePreferencesOf(
            KEY_SEARCH_HOT_CONTROL_VISIBLE to true,
            booleanPreferencesKey("search_hot_section_enabled") to false,
            booleanPreferencesKey("search_history_section_enabled") to true,
            booleanPreferencesKey("search_discover_section_enabled") to false,
        )

        migrateFocusSearchLandingSections(preferences)

        assertEquals(listOf(true, true, false), visibleSectionKeys.map { preferences[it] })
    }
}
