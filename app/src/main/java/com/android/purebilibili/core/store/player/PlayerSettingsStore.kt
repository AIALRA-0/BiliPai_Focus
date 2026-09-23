package com.android.purebilibili.core.store.player

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.android.purebilibili.core.store.resolvePreferredPlaybackSpeed as resolvePreferredPlaybackSpeedPolicy
import com.android.purebilibili.core.store.normalizePlaybackSpeed as normalizePlaybackSpeedPolicy
import com.android.purebilibili.core.store.settingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

const val DEFAULT_AUDIO_QUALITY_FOLLOW_LAST = -2

internal val defaultAudioQualityPreferenceKey = intPreferencesKey("default_audio_quality")

object PlayerSettingsStore {
    enum class PlayerInsightMode {
        OFF,
        SMART,
        ALWAYS
    }

    private val keyDefaultPlaybackSpeed = floatPreferencesKey("default_playback_speed")
    private val keyRememberLastPlaybackSpeed = booleanPreferencesKey("remember_last_playback_speed")
    private val keyLastPlaybackSpeed = floatPreferencesKey("last_playback_speed")
    private val keyPreferredPlayerVolume = floatPreferencesKey("preferred_player_volume")
    private val keyPlayerInsightMode = stringPreferencesKey("player_insight_mode")
    private val keyNativeMiuixPlayerPopups = booleanPreferencesKey("native_miuix_player_popups")
    private const val playbackSpeedCachePrefs = "playback_speed_cache"
    private const val cacheKeyDefaultPlaybackSpeed = "default_speed"
    private const val cacheKeyRememberLastSpeed = "remember_last_speed"
    private const val cacheKeyLastPlaybackSpeed = "last_speed"
    private const val cacheKeyPreferredPlayerVolume = "preferred_player_volume"
    private const val audioQualityCachePrefs = "quality_settings"
    private const val cacheKeyAudioQuality = "audio_quality"
    private const val cacheKeyDefaultAudioQuality = "default_audio_quality"
    private const val legacyAppPrefs = "app_prefs"
    private const val legacyShowStatsKey = "show_stats"
    private const val cachePlayerInsightModeKey = "player_insight_mode_cache"

    const val PLAYER_VOLUME_STEP = 0.02f

    fun getNativeMiuixPlayerPopups(context: Context): Flow<Boolean> =
        context.settingsDataStore.data.map { preferences ->
            preferences[keyNativeMiuixPlayerPopups] ?: true
        }

    suspend fun setNativeMiuixPlayerPopups(context: Context, enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[keyNativeMiuixPlayerPopups] = enabled
        }
    }

    fun normalizePlayerVolume(volume: Float): Float {
        val stepCount = (volume.coerceIn(0f, 1f) / PLAYER_VOLUME_STEP).toInt()
        val lower = stepCount * PLAYER_VOLUME_STEP
        val upper = ((stepCount + 1) * PLAYER_VOLUME_STEP).coerceAtMost(1f)
        return if (volume - lower < upper - volume) lower else upper
    }

    fun normalizePlaybackSpeed(speed: Float): Float {
        return normalizePlaybackSpeedPolicy(speed)
    }

    fun resolvePreferredPlaybackSpeed(
        defaultSpeed: Float,
        rememberLastSpeed: Boolean,
        lastSpeed: Float
    ): Float {
        return resolvePreferredPlaybackSpeedPolicy(
            defaultSpeed = defaultSpeed,
            rememberLastSpeed = rememberLastSpeed,
            lastSpeed = lastSpeed
        )
    }

    fun getDefaultPlaybackSpeed(context: Context): Flow<Float> = context.settingsDataStore.data
        .map { preferences -> normalizePlaybackSpeed(preferences[keyDefaultPlaybackSpeed] ?: 1.0f) }

    suspend fun setDefaultPlaybackSpeed(context: Context, speed: Float) {
        val normalized = normalizePlaybackSpeed(speed)
        context.settingsDataStore.edit { preferences ->
            preferences[keyDefaultPlaybackSpeed] = normalized
        }
        context.getSharedPreferences(playbackSpeedCachePrefs, Context.MODE_PRIVATE)
            .edit()
            .putFloat(cacheKeyDefaultPlaybackSpeed, normalized)
            .apply()
    }

    fun getRememberLastPlaybackSpeed(context: Context): Flow<Boolean> = context.settingsDataStore.data
        .map { preferences -> preferences[keyRememberLastPlaybackSpeed] ?: false }

    suspend fun setRememberLastPlaybackSpeed(context: Context, enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[keyRememberLastPlaybackSpeed] = enabled
        }
        context.getSharedPreferences(playbackSpeedCachePrefs, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(cacheKeyRememberLastSpeed, enabled)
            .apply()
    }

    fun getLastPlaybackSpeed(context: Context): Flow<Float> = context.settingsDataStore.data
        .map { preferences -> normalizePlaybackSpeed(preferences[keyLastPlaybackSpeed] ?: 1.0f) }

    suspend fun setLastPlaybackSpeed(context: Context, speed: Float) {
        val normalized = normalizePlaybackSpeed(speed)
        context.settingsDataStore.edit { preferences ->
            preferences[keyLastPlaybackSpeed] = normalized
        }
        context.getSharedPreferences(playbackSpeedCachePrefs, Context.MODE_PRIVATE)
            .edit()
            .putFloat(cacheKeyLastPlaybackSpeed, normalized)
            .apply()
    }

    fun getPreferredPlaybackSpeed(context: Context): Flow<Float> = combine(
        getDefaultPlaybackSpeed(context),
        getRememberLastPlaybackSpeed(context),
        getLastPlaybackSpeed(context)
    ) { defaultSpeed, rememberLast, lastSpeed ->
        resolvePreferredPlaybackSpeed(
            defaultSpeed = defaultSpeed,
            rememberLastSpeed = rememberLast,
            lastSpeed = lastSpeed
        )
    }

    fun getPreferredPlaybackSpeedSync(context: Context): Float {
        val prefs = context.getSharedPreferences(playbackSpeedCachePrefs, Context.MODE_PRIVATE)
        val defaultSpeed = normalizePlaybackSpeed(prefs.getFloat(cacheKeyDefaultPlaybackSpeed, 1.0f))
        val rememberLast = prefs.getBoolean(cacheKeyRememberLastSpeed, false)
        val lastSpeed = normalizePlaybackSpeed(prefs.getFloat(cacheKeyLastPlaybackSpeed, 1.0f))
        return resolvePreferredPlaybackSpeed(
            defaultSpeed = defaultSpeed,
            rememberLastSpeed = rememberLast,
            lastSpeed = lastSpeed
        )
    }

    fun getPreferredPlayerVolume(context: Context): Flow<Float> = context.settingsDataStore.data
        .map { preferences ->
            normalizePlayerVolume(preferences[keyPreferredPlayerVolume] ?: 1.0f)
        }

    suspend fun setPreferredPlayerVolume(context: Context, volume: Float) {
        val normalized = normalizePlayerVolume(volume)
        context.settingsDataStore.edit { preferences ->
            preferences[keyPreferredPlayerVolume] = normalized
        }
        context.getSharedPreferences(playbackSpeedCachePrefs, Context.MODE_PRIVATE)
            .edit()
            .putFloat(cacheKeyPreferredPlayerVolume, normalized)
            .apply()
    }

    fun getPreferredPlayerVolumeSync(context: Context): Float {
        return normalizePlayerVolume(
            context.getSharedPreferences(playbackSpeedCachePrefs, Context.MODE_PRIVATE)
                .getFloat(cacheKeyPreferredPlayerVolume, 1.0f)
        )
    }

    fun getDefaultAudioQuality(context: Context): Flow<Int> = context.settingsDataStore.data
        .map { preferences ->
            preferences[defaultAudioQualityPreferenceKey] ?: DEFAULT_AUDIO_QUALITY_FOLLOW_LAST
        }

    suspend fun setDefaultAudioQuality(context: Context, value: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[defaultAudioQualityPreferenceKey] = value
        }
        context.getSharedPreferences(audioQualityCachePrefs, Context.MODE_PRIVATE)
            .edit()
            .putInt(cacheKeyDefaultAudioQuality, value)
            .commit()
    }

    fun getCachedDefaultAudioQuality(context: Context): Int {
        return context.getSharedPreferences(audioQualityCachePrefs, Context.MODE_PRIVATE)
            .getInt(cacheKeyDefaultAudioQuality, DEFAULT_AUDIO_QUALITY_FOLLOW_LAST)
    }

    fun getCachedLastSelectedAudioQuality(context: Context): Int {
        return context.getSharedPreferences(audioQualityCachePrefs, Context.MODE_PRIVATE)
            .getInt(cacheKeyAudioQuality, -1)
    }

    fun getPlayerInsightMode(context: Context): Flow<PlayerInsightMode> = context.settingsDataStore.data
        .map { preferences ->
            val legacyPreferences = context.getSharedPreferences(legacyAppPrefs, Context.MODE_PRIVATE)
            resolvePlayerInsightMode(
                storedMode = preferences[keyPlayerInsightMode],
                legacyPreferencePresent = legacyPreferences.contains(legacyShowStatsKey),
                legacyStatsEnabled = legacyPreferences.getBoolean(legacyShowStatsKey, false)
            )
        }

    suspend fun setPlayerInsightMode(context: Context, mode: PlayerInsightMode) {
        context.settingsDataStore.edit { preferences ->
            preferences[keyPlayerInsightMode] = mode.name
        }
        context.getSharedPreferences(legacyAppPrefs, Context.MODE_PRIVATE)
            .edit()
            .putString(cachePlayerInsightModeKey, mode.name)
            .putBoolean(legacyShowStatsKey, mode != PlayerInsightMode.OFF)
            .apply()
    }

    fun getPlayerInsightModeSync(context: Context): PlayerInsightMode {
        val preferences = context.getSharedPreferences(legacyAppPrefs, Context.MODE_PRIVATE)
        return resolvePlayerInsightMode(
            storedMode = preferences.getString(cachePlayerInsightModeKey, null),
            legacyPreferencePresent = preferences.contains(legacyShowStatsKey),
            legacyStatsEnabled = preferences.getBoolean(legacyShowStatsKey, false)
        )
    }

    internal fun resolvePlayerInsightMode(
        storedMode: String?,
        legacyPreferencePresent: Boolean,
        legacyStatsEnabled: Boolean
    ): PlayerInsightMode {
        PlayerInsightMode.entries.firstOrNull { it.name == storedMode }?.let { return it }
        // 全新安装 / 无历史偏好：默认关闭，避免首播叠加洞察浮层
        if (!legacyPreferencePresent) return PlayerInsightMode.OFF
        return if (legacyStatsEnabled) {
            PlayerInsightMode.ALWAYS
        } else {
            PlayerInsightMode.OFF
        }
    }
}
