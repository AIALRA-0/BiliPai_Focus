package com.android.purebilibili.core.store.home

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.android.purebilibili.core.store.LiquidGlassReadabilityMode
import com.android.purebilibili.core.store.settingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal val liquidGlassReadabilityModePreferencesKey =
    intPreferencesKey("liquid_glass_readability_mode")

object LiquidGlassSettingsStore {
    fun observeReadabilityMode(context: Context): Flow<LiquidGlassReadabilityMode> =
        context.settingsDataStore.data
            .map { preferences ->
                LiquidGlassReadabilityMode.fromValue(
                    preferences[liquidGlassReadabilityModePreferencesKey]
                        ?: LiquidGlassReadabilityMode.STABLE.value
                )
            }
            .distinctUntilChanged()

    suspend fun setReadabilityMode(
        context: Context,
        mode: LiquidGlassReadabilityMode,
    ) {
        context.settingsDataStore.edit { preferences ->
            preferences[liquidGlassReadabilityModePreferencesKey] = mode.value
        }
    }
}
