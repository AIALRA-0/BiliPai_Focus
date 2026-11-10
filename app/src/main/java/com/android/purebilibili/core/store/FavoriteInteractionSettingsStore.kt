package com.android.purebilibili.core.store

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

const val DEFAULT_FAVORITE_QUICK_SAVE_DEFAULT_FOLDER = false

object FavoriteInteractionSettingsStore {
    private val quickSaveDefaultFolderKey =
        booleanPreferencesKey("favorite_quick_save_default_folder")

    fun getQuickSaveDefaultFolder(context: Context): Flow<Boolean> =
        context.settingsDataStore.data.map { preferences ->
            preferences[quickSaveDefaultFolderKey] ?: DEFAULT_FAVORITE_QUICK_SAVE_DEFAULT_FOLDER
        }

    suspend fun setQuickSaveDefaultFolder(context: Context, enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[quickSaveDefaultFolderKey] = enabled
        }
    }
}
