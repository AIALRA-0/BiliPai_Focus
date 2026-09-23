package com.android.purebilibili.core.store

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.first

interface AicuConsentStore {
    suspend fun acceptedVersion(): Int
    suspend fun accept(version: Int)
}

class DataStoreAicuConsentStore(context: Context) : AicuConsentStore {
    private val appContext = context.applicationContext
    private val key = intPreferencesKey("aicu_disclaimer_accepted_version")
    override suspend fun acceptedVersion(): Int = appContext.settingsDataStore.data.first()[key] ?: 0
    override suspend fun accept(version: Int) {
        appContext.settingsDataStore.edit { it[key] = version }
    }
}
