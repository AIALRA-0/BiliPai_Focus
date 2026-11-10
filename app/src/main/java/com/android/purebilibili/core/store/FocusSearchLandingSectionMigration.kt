package com.android.purebilibili.core.store

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey

internal val KEY_SEARCH_HOT_CONTROL_VISIBLE = booleanPreferencesKey("search_hot_control_visible")
internal val KEY_SEARCH_HISTORY_CONTROL_VISIBLE = booleanPreferencesKey("search_history_control_visible")
internal val KEY_SEARCH_DISCOVER_CONTROL_VISIBLE = booleanPreferencesKey("search_discover_control_visible")
internal val KEY_FOCUS_SEARCH_LANDING_SECTION_MIGRATION_VERSION =
    intPreferencesKey("focus_search_landing_section_migration_version")
private const val FOCUS_SEARCH_LANDING_SECTION_MIGRATION_VERSION = 1
private val LEGACY_SEARCH_LANDING_SECTION_KEYS = listOf(
    booleanPreferencesKey("search_hot_section_enabled"),
    booleanPreferencesKey("search_history_section_enabled"),
    booleanPreferencesKey("search_discover_section_enabled"),
)
private val SEARCH_SECTION_BODY_AVAILABILITY_KEYS = listOf(
    LEGACY_SEARCH_LANDING_SECTION_KEYS[0],
    LEGACY_SEARCH_LANDING_SECTION_KEYS[2],
)

/** Copies the older Focus visibility settings to the upstream section controls once. */
internal fun migrateFocusSearchLandingSections(preferences: MutablePreferences): Boolean {
    if ((preferences[KEY_FOCUS_SEARCH_LANDING_SECTION_MIGRATION_VERSION] ?: 0) >=
        FOCUS_SEARCH_LANDING_SECTION_MIGRATION_VERSION
    ) {
        return false
    }

    val sectionControlKeys = listOf(
        KEY_SEARCH_HOT_CONTROL_VISIBLE,
        KEY_SEARCH_HISTORY_CONTROL_VISIBLE,
        KEY_SEARCH_DISCOVER_CONTROL_VISIBLE,
    )
    sectionControlKeys.forEachIndexed { index, targetKey ->
        if (preferences[targetKey] == null) {
            preferences[targetKey] = preferences[LEGACY_SEARCH_LANDING_SECTION_KEYS[index]] ?: false
        }
    }

    // The old Focus keys also gated the section contents. The new section controls now own
    // visibility, so keep the upstream content controls at their enabled default.
    SEARCH_SECTION_BODY_AVAILABILITY_KEYS.forEach { key -> preferences[key] = true }
    preferences[KEY_FOCUS_SEARCH_LANDING_SECTION_MIGRATION_VERSION] =
        FOCUS_SEARCH_LANDING_SECTION_MIGRATION_VERSION
    return true
}
