package com.android.purebilibili.core.store

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.android.purebilibili.feature.settings.highlights.UpdateHighlightHistory
import com.android.purebilibili.feature.settings.highlights.prepareUpdateHighlights
import kotlinx.coroutines.flow.map

/** Installation-local history: exclude these keys from settings export/import. */
internal object UpdateHighlightsStore {
    private val version = intPreferencesKey("update_highlights_last_version")
    private val pending = stringSetPreferencesKey("update_highlights_pending_ids")
    private val handled = stringSetPreferencesKey("update_highlights_handled_ids")

    fun history(context: Context) = context.settingsDataStore.data.map { preferences ->
        UpdateHighlightHistory(preferences[version] ?: 0, preferences[pending].orEmpty(), preferences[handled].orEmpty())
    }

    suspend fun prepare(context: Context, currentVersion: Int, existingInstall: Boolean) {
        context.settingsDataStore.edit { preferences ->
            val old = UpdateHighlightHistory(preferences[version] ?: 0, preferences[pending].orEmpty(), preferences[handled].orEmpty())
            val next = prepareUpdateHighlights(old, currentVersion, existingInstall)
            preferences[version] = next.lastVersionCode
            preferences[pending] = next.pendingIds
            preferences[handled] = next.handledIds
        }
    }

    suspend fun markPresented(context: Context, ids: Set<String>) {
        context.settingsDataStore.edit { preferences ->
            preferences[handled] = preferences[handled].orEmpty() + ids
            preferences[pending] = preferences[pending].orEmpty() - ids
        }
    }
}
