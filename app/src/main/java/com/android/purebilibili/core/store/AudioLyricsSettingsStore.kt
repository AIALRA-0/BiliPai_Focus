package com.android.purebilibili.core.store

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

object AudioLyricsSettingsStore {
    private fun translationKey(videoAudioMode: Boolean) = booleanPreferencesKey(
        if (videoAudioMode) "audio_mode_subtitle_show_translations" else "music_lyrics_show_translations"
    )

    fun observeShowTranslations(context: Context, videoAudioMode: Boolean) = context.settingsDataStore.data
        .map { it[translationKey(videoAudioMode)] ?: !videoAudioMode }
        .distinctUntilChanged()

    suspend fun setShowTranslations(context: Context, enabled: Boolean, videoAudioMode: Boolean) {
        context.settingsDataStore.edit { it[translationKey(videoAudioMode)] = enabled }
    }
}
