// File: core/store/LocalPlaylistStore.kt
package com.android.purebilibili.core.store

import android.content.Context
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

/**
 * 本地歌单：外部歌单导入或用户手动创建的持久化歌曲集合。
 * 歌单项以 bvid 为锚点，播放时再解析 cid。
 */
@Serializable
data class LocalPlaylistItem(
    val bvid: String,
    val title: String = "",
    val cover: String = "",
    val owner: String = "",
    val durationSec: Long = 0L
)

@Serializable
data class LocalPlaylist(
    val id: String,
    val name: String,
    val coverUrl: String = "",
    val source: String = "local",
    val createdAtMs: Long = 0L,
    val items: List<LocalPlaylistItem> = emptyList()
)

object LocalPlaylistStore {
    fun playlists(context: Context): Flow<List<LocalPlaylist>> = kotlinx.coroutines.flow.flow {
        val dao = AudioLibraryDatabase.ready(context).library()
        emitAll(dao.playlists().map { rows -> rows.map { it.toPlaylist() } })
    }

    suspend fun savePlaylist(context: Context, playlist: LocalPlaylist) {
        AudioLibraryDatabase.ready(context).library().savePlaylist(playlist)
    }

    suspend fun deletePlaylist(context: Context, playlistId: String) {
        AudioLibraryDatabase.ready(context).library().deletePlaylist(playlistId)
    }

    suspend fun addToPlaylist(context: Context, playlistId: String, items: List<LocalPlaylistItem>) {
        AudioLibraryDatabase.ready(context).library().addTracks(playlistId, items)
    }
}
