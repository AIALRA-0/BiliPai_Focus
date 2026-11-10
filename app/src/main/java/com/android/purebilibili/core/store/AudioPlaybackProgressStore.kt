package com.android.purebilibili.core.store

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
internal data class AudioPlaybackCheckpoint(
    val accountMid: Long,
    val bvid: String,
    val cid: Long,
    val positionMs: Long,
    val savedAtMs: Long = System.currentTimeMillis(),
)

/** 前台和独立音频进程共用断点；按账号、视频、分 P 隔离，允许显式回退与归零。 */
internal object AudioPlaybackProgressStore {
    private val json = Json { ignoreUnknownKeys = true }
    private val pending = linkedMapOf<String, AudioPlaybackCheckpoint>()

    fun capture(checkpoint: AudioPlaybackCheckpoint) {
        if (checkpoint.bvid.isBlank() || checkpoint.cid <= 0L || checkpoint.positionMs < 0L) return
        synchronized(pending) {
            val id = "${checkpoint.accountMid}:${checkpoint.bvid}:${checkpoint.cid}"
            pending[id] = selectLatestAudioCheckpoint(pending[id], checkpoint)
            while (pending.size > 300) pending.remove(pending.keys.first())
        }
    }
    private fun key(accountMid: Long, bvid: String, cid: Long) =
        stringPreferencesKey("$accountMid:$bvid:$cid")

    suspend fun read(context: Context, accountMid: Long, bvid: String, cid: Long): AudioPlaybackCheckpoint? =
        withContext(Dispatchers.IO) {
            val raw = MultiProcessPreferences.get(context, "audio_playback_progress").data.first()[key(accountMid, bvid, cid)]
            val stored = raw?.let { runCatching { json.decodeFromString<AudioPlaybackCheckpoint>(it) }.getOrNull() }
            val captured = synchronized(pending) { pending["$accountMid:$bvid:$cid"] }
            if (captured == null) stored else selectLatestAudioCheckpoint(stored, captured)
        }

    suspend fun save(context: Context, checkpoint: AudioPlaybackCheckpoint): Unit = withContext(Dispatchers.IO) {
        if (checkpoint.bvid.isBlank() || checkpoint.cid <= 0L || checkpoint.positionMs < 0L) return@withContext
        capture(checkpoint)
        MultiProcessPreferences.get(context, "audio_playback_progress").edit { prefs ->
            val key = key(checkpoint.accountMid, checkpoint.bvid, checkpoint.cid)
            val previous = prefs[key]?.let {
                runCatching { json.decodeFromString<AudioPlaybackCheckpoint>(it) }.getOrNull()
            }
            // 较晚完成的旧写入不能覆盖更新的采样；不能按播放位置取最大值，否则用户无法回退。
            prefs[key] = json.encodeToString(selectLatestAudioCheckpoint(previous, checkpoint))
            if (prefs.asMap().size > 300) {
                prefs.asMap().entries.sortedBy { (_, raw) ->
                    (raw as? String)?.let {
                        runCatching { json.decodeFromString<AudioPlaybackCheckpoint>(it).savedAtMs }.getOrNull()
                    } ?: 0L
                }.take(prefs.asMap().size - 300).forEach { (oldKey, _) -> prefs.remove(oldKey) }
            }
        }
        Unit
    }
}

internal fun selectLatestAudioCheckpoint(
    previous: AudioPlaybackCheckpoint?,
    incoming: AudioPlaybackCheckpoint,
): AudioPlaybackCheckpoint = if (previous == null || incoming.savedAtMs >= previous.savedAtMs) incoming else previous
