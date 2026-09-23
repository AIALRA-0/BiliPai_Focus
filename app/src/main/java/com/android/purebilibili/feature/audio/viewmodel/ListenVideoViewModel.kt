package com.android.purebilibili.feature.audio.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.data.model.response.FavFolder
import com.android.purebilibili.data.model.response.FavoriteData
import com.android.purebilibili.feature.audio.library.BilibiliListenVideoLibraryDataSource
import com.android.purebilibili.feature.audio.library.ListenVideoAlbum
import com.android.purebilibili.feature.audio.library.ListenVideoArtist
import com.android.purebilibili.feature.audio.library.ListenVideoLibraryDataSource
import com.android.purebilibili.feature.audio.library.ListenVideoLibraryLoader
import com.android.purebilibili.feature.audio.library.ListenVideoPlaylist
import com.android.purebilibili.feature.audio.library.ListenVideoSection
import com.android.purebilibili.feature.audio.library.ListenVideoTrack
import com.android.purebilibili.feature.audio.library.mapListenVideoAlbums
import com.android.purebilibili.feature.audio.library.mapListenVideoArtists
import com.android.purebilibili.feature.audio.library.mapListenVideoPlaylists
import com.android.purebilibili.feature.audio.library.toListenVideoTrackOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class ListenVideoUiState(
    val generation: Long = 0L,
    val isLoggedIn: Boolean = true,
    val isLoading: Boolean = false,
    val isIndexing: Boolean = false,
    val indexedFolderCount: Int = 0,
    val totalFolderCount: Int = 0,
    val section: ListenVideoSection = ListenVideoSection.PLAYLISTS,
    val playlists: List<ListenVideoPlaylist> = emptyList(),
    val albums: List<ListenVideoAlbum> = emptyList(),
    val artists: List<ListenVideoArtist> = emptyList(),
    val selectedTitle: String = "",
    val selectedTracks: List<ListenVideoTrack> = emptyList(),
    val isDetailLoading: Boolean = false,
    val detailError: String? = null,
    val failedFolderIds: Set<Long> = emptySet(),
    val error: String? = null
)

internal class ListenVideoViewModel(
    private val dataSource: ListenVideoLibraryDataSource = BilibiliListenVideoLibraryDataSource(),
    private val currentMid: () -> Long = { TokenManager.midCache ?: 0L }
) : ViewModel() {
    private val loader = ListenVideoLibraryLoader(dataSource)
    private val _uiState = MutableStateFlow(ListenVideoUiState())
    val uiState = _uiState.asStateFlow()

    private var refreshJob: Job? = null
    private var indexJob: Job? = null
    private var detailJob: Job? = null
    private var ownedFolders: List<FavFolder> = emptyList()
    private var collectedFolders: List<FavFolder> = emptyList()
    private var indexedResources: List<FavoriteData> = emptyList()
    private var indexCompleted = false
    private var lastDetailTitle = ""
    private var lastDetailRequest: (suspend () -> Result<List<FavoriteData>>)? = null

    fun refresh() {
        refreshJob?.cancel()
        indexJob?.cancel()
        detailJob?.cancel()
        val generation = _uiState.value.generation + 1L
        val mid = currentMid()
        if (mid <= 0L) {
            ownedFolders = emptyList()
            collectedFolders = emptyList()
            indexedResources = emptyList()
            indexCompleted = false
            lastDetailTitle = ""
            lastDetailRequest = null
            _uiState.value = ListenVideoUiState(
                generation = generation,
                isLoggedIn = false
            )
            return
        }

        _uiState.value = ListenVideoUiState(
            generation = generation,
            isLoggedIn = true,
            isLoading = true
        )
        refreshJob = viewModelScope.launch {
            try {
                val foldersResult = dataSource.ownedFolders(mid)
                if (_uiState.value.generation != generation) return@launch
                val folders = foldersResult.getOrElse { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.userMessage("收藏夹加载失败"))
                    }
                    return@launch
                }
                val collectedResult = loader.loadCollectedFolders(mid)
                if (_uiState.value.generation != generation) return@launch
                ownedFolders = folders
                collectedFolders = collectedResult.getOrDefault(emptyList())
                indexedResources = emptyList()
                indexCompleted = false
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        playlists = mapListenVideoPlaylists(ownedFolders),
                        albums = mapListenVideoAlbums(collectedFolders, emptyList()),
                        artists = emptyList(),
                        totalFolderCount = ownedFolders.size,
                        error = collectedResult.exceptionOrNull()?.userMessage("部分合集加载失败")
                    )
                }
                val previewCovers = loader.loadPlaylistPreviewCovers(ownedFolders)
                if (_uiState.value.generation != generation) return@launch
                if (previewCovers.isNotEmpty()) {
                    _uiState.update { state ->
                        state.copy(
                            playlists = state.playlists.map { playlist ->
                                previewCovers[playlist.mediaId]?.let { cover ->
                                    playlist.copy(coverUrl = cover)
                                } ?: playlist
                            }
                        )
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            }
        }
    }

    fun selectSection(section: ListenVideoSection) {
        _uiState.update { it.copy(section = section) }
        if (section != ListenVideoSection.PLAYLISTS && !indexCompleted) {
            indexLibrary()
        }
    }

    fun openPlaylist(playlist: ListenVideoPlaylist) {
        // Playlist covers are best-effort. Stop their background requests before loading
        // the user-selected folder so the favorite endpoint does not trip HTTP 412.
        refreshJob?.cancel()
        loadDetail(playlist.title) { loader.loadFolder(playlist.mediaId) }
    }

    fun openAlbum(album: ListenVideoAlbum) {
        loadDetail(album.title) { loader.loadAlbum(album.seasonId) }
    }

    fun openArtist(artist: ListenVideoArtist) {
        detailJob?.cancel()
        _uiState.update {
            it.copy(
                selectedTitle = artist.name,
                selectedTracks = artist.tracks,
                isDetailLoading = false,
                detailError = null
            )
        }
    }

    fun closeDetail() {
        detailJob?.cancel()
        _uiState.update {
            it.copy(
                selectedTitle = "",
                selectedTracks = emptyList(),
                isDetailLoading = false,
                detailError = null
            )
        }
    }

    fun retryDetail() {
        val request = lastDetailRequest ?: return
        loadDetail(lastDetailTitle, request)
    }

    fun retryFailedIndex() {
        if (_uiState.value.failedFolderIds.isEmpty()) return
        indexLibrary(onlyFailed = true)
    }

    private fun indexLibrary(onlyFailed: Boolean = false) {
        indexJob?.cancel()
        val generation = _uiState.value.generation
        val targets = if (onlyFailed) {
            ownedFolders.filter { it.id in _uiState.value.failedFolderIds }
        } else {
            ownedFolders
        }
        if (targets.isEmpty()) {
            indexCompleted = true
            return
        }
        _uiState.update {
            it.copy(
                isIndexing = true,
                indexedFolderCount = 0,
                totalFolderCount = targets.size
            )
        }
        indexJob = viewModelScope.launch {
            val result = loader.indexFolders(targets) { completed, total ->
                if (_uiState.value.generation == generation) {
                    _uiState.update {
                        it.copy(indexedFolderCount = completed, totalFolderCount = total)
                    }
                }
            }
            if (_uiState.value.generation != generation) return@launch
            indexedResources = (indexedResources + result.resources)
                .distinctBy { resource ->
                    if (resource.type == 21) {
                        "album:${resource.season_id.takeIf { it > 0L } ?: resource.id}"
                    } else {
                        "track:${resource.bvid.ifBlank { resource.bv_id }}"
                    }
                }
            indexCompleted = result.failedFolderIds.isEmpty()
            _uiState.update {
                it.copy(
                    isIndexing = false,
                    albums = mapListenVideoAlbums(collectedFolders, indexedResources),
                    artists = mapListenVideoArtists(indexedResources),
                    failedFolderIds = result.failedFolderIds,
                    error = result.failedFolderIds.takeIf(Set<Long>::isNotEmpty)?.let {
                        if (result.haltedByRiskControl) {
                            "已触发收藏夹风控，整理已暂停；${it.size} 个收藏夹可稍后重试"
                        } else {
                            "${it.size} 个收藏夹暂时无法索引"
                        }
                    }
                )
            }
        }
    }

    private fun loadDetail(
        title: String,
        request: suspend () -> Result<List<FavoriteData>>
    ) {
        lastDetailTitle = title
        lastDetailRequest = request
        detailJob?.cancel()
        val generation = _uiState.value.generation
        _uiState.update {
            it.copy(
                selectedTitle = title,
                selectedTracks = emptyList(),
                isDetailLoading = true,
                detailError = null
            )
        }
        detailJob = viewModelScope.launch {
            val result = request()
            if (_uiState.value.generation != generation) return@launch
            val resources = result.getOrNull().orEmpty()
            indexedResources = (indexedResources + resources).distinctBy { resource ->
                "${resource.type}:${resource.bvid.ifBlank { resource.bv_id }}:${resource.season_id}:${resource.id}"
            }
            _uiState.update {
                it.copy(
                    selectedTracks = resources.mapNotNull(FavoriteData::toListenVideoTrackOrNull)
                        .distinctBy(ListenVideoTrack::bvid),
                    isDetailLoading = false,
                    detailError = result.exceptionOrNull()?.userMessage("内容加载失败"),
                    albums = mapListenVideoAlbums(collectedFolders, indexedResources),
                    artists = mapListenVideoArtists(indexedResources)
                )
            }
        }
    }
}

private fun Throwable.userMessage(fallback: String): String {
    return message?.trim()?.takeIf(String::isNotEmpty) ?: fallback
}
