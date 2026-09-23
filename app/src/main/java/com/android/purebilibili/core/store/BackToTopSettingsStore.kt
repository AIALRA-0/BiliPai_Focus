package com.android.purebilibili.core.store

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

const val DEFAULT_BACK_TO_TOP_BUTTON_ENABLED = true
const val DEFAULT_BACK_TO_TOP_OFFSET_X_DP = 0f
const val DEFAULT_BACK_TO_TOP_OFFSET_Y_DP = 0f

object BackToTopSettingsStore {
    private val enabledKey = booleanPreferencesKey("back_to_top_button_enabled")
    private val offsetXDpKey = androidx.datastore.preferences.core.floatPreferencesKey("back_to_top_button_offset_x_dp")
    private val offsetYDpKey = androidx.datastore.preferences.core.floatPreferencesKey("back_to_top_button_offset_y_dp")

    @Volatile
    private var cachedOffset: Pair<Float, Float>? = null

    fun getCachedOffsetDp(): Pair<Float, Float> =
        cachedOffset ?: Pair(DEFAULT_BACK_TO_TOP_OFFSET_X_DP, DEFAULT_BACK_TO_TOP_OFFSET_Y_DP)

    fun updateCachedOffset(offsetXDp: Float, offsetYDp: Float) {
        cachedOffset = Pair(offsetXDp, offsetYDp)
    }

    fun isEnabled(context: Context): Flow<Boolean> = context.settingsDataStore.data
        .map { preferences ->
            preferences[enabledKey] ?: DEFAULT_BACK_TO_TOP_BUTTON_ENABLED
        }

    suspend fun setEnabled(context: Context, enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[enabledKey] = enabled
        }
    }

    fun getCustomOffsetDp(context: Context): Flow<Pair<Float, Float>> = context.settingsDataStore.data
        .map { preferences ->
            val x = preferences[offsetXDpKey] ?: DEFAULT_BACK_TO_TOP_OFFSET_X_DP
            val y = preferences[offsetYDpKey] ?: DEFAULT_BACK_TO_TOP_OFFSET_Y_DP
            val offset = Pair(x, y)
            cachedOffset = offset
            offset
        }

    suspend fun setCustomOffsetDp(context: Context, offsetXDp: Float, offsetYDp: Float) {
        cachedOffset = Pair(offsetXDp, offsetYDp)
        context.settingsDataStore.edit { preferences ->
            preferences[offsetXDpKey] = offsetXDp
            preferences[offsetYDpKey] = offsetYDp
        }
    }

    suspend fun resetCustomOffset(context: Context) {
        cachedOffset = Pair(DEFAULT_BACK_TO_TOP_OFFSET_X_DP, DEFAULT_BACK_TO_TOP_OFFSET_Y_DP)
        context.settingsDataStore.edit { preferences ->
            preferences.remove(offsetXDpKey)
            preferences.remove(offsetYDpKey)
        }
    }
}
