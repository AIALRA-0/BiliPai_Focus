package com.android.purebilibili.core.store

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.map

object SearchHintSettingsStore {
    private val enabledKey = booleanPreferencesKey("search_default_hint_enabled")
    fun isEnabled(context: Context) = context.settingsDataStore.data.map { it[enabledKey] ?: true }
    suspend fun setEnabled(context: Context, enabled: Boolean) {
        context.settingsDataStore.edit { it[enabledKey] = enabled }
    }
}
