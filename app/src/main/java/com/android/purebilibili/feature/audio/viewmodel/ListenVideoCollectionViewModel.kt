package com.android.purebilibili.feature.audio.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.feature.audio.library.BilibiliListenVideoLibraryDataSource
import com.android.purebilibili.feature.audio.library.ListenVideoLibraryDataSource
import com.android.purebilibili.feature.audio.library.ListenVideoLibraryLoader
import com.android.purebilibili.feature.audio.library.ListenVideoTrack
import com.android.purebilibili.feature.audio.library.resolveListenVideoPlaybackSelection
import com.android.purebilibili.feature.audio.library.toListenVideoTrackOrNull
import com.android.purebilibili.feature.video.player.ExternalPlaylistSource
import com.android.purebilibili.feature.video.player.PlaylistManager
import com.android.purebilibili.feature.video.player.PlaylistSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class ListenVideoCollectionState(
    val firstTrack: ListenVideoTrack? = null,
    val loadedTrackCount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
)

/** The playback destination owns pagination; opening a collection never waits for its whole queue. */
internal class ListenVideoCollectionViewModel(
    source: ListenVideoLibraryDataSource = BilibiliListenVideoLibraryDataSource(),
) : ViewModel() {
    private val loader = ListenVideoLibraryLoader(source)
    private val _state = MutableStateFlow(ListenVideoCollectionState())
    val state = _state.asStateFlow()
    private var request: Pair<Long, Long>? = null
    private var loadJob: Job? = null
    private var session: PlaylistSession? = null

    fun open(mediaId: Long, seasonId: Long) {
        val nextRequest = mediaId to seasonId
        if (request == nextRequest) return
        loadJob?.cancel()
        request = nextRequest
        session = null
        _state.value = ListenVideoCollectionState()
        load()
    }

    fun retry() {
        if (loadJob?.isActive != true) load()
    }

    private fun load() {
        val (mediaId, seasonId) = request ?: return
        _state.update { it.copy(isLoading = true, error = null) }
        loadJob = viewModelScope.launch {
            try {
                val publish: suspend (List<com.android.purebilibili.data.model.response.FavoriteData>) -> Unit = { resources ->
                    val tracks = withContext(Dispatchers.Default) {
                        resources.mapNotNull { it.toListenVideoTrackOrNull() }.distinctBy { it.bvid }
                    }
                    if (tracks.isNotEmpty()) {
                        val selection = withContext(Dispatchers.Default) {
                            resolveListenVideoPlaybackSelection(tracks, tracks.first().bvid)
                        }
                        val activeSession = session
                        if (activeSession == null) {
                            session = PlaylistManager.setExternalPlaylist(
                                selection.items, selection.startIndex, ExternalPlaylistSource.FAVORITE,
                            )
                        } else if (!PlaylistManager.addAllToPlaylistIfCurrent(selection.items, activeSession)) {
                            // Another playback action owns the queue now. Never append this collection to it.
                            throw CancellationException("Playback queue replaced")
                        }
                        _state.update {
                            it.copy(firstTrack = it.firstTrack ?: tracks.first(), loadedTrackCount = maxOf(it.loadedTrackCount, tracks.size))
                        }
                    }
                }
                val result = if (seasonId > 0L) {
                    loader.loadAlbum(seasonId, publish)
                } else {
                    loader.loadFolder(mediaId, publish)
                }
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = if (result.isFailure) {
                            result.exceptionOrNull()?.message?.takeIf(String::isNotBlank) ?: "曲目读取失败，请重试"
                        } else if (it.firstTrack == null) "这个列表中没有可播放的视频" else null,
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _state.update { it.copy(isLoading = false, error = error.message ?: "曲目读取失败，请重试") }
            }
        }
    }
}
