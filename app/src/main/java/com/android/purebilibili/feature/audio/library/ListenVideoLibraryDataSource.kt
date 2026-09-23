package com.android.purebilibili.feature.audio.library

import com.android.purebilibili.data.model.response.FavFolder
import com.android.purebilibili.data.model.response.FavoriteData
import com.android.purebilibili.data.model.response.FavoriteResourceData
import com.android.purebilibili.data.repository.FavoriteRepository
import com.android.purebilibili.feature.list.isFavoriteRiskControlError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val LISTEN_VIDEO_PAGE_SIZE = 20
private const val LISTEN_VIDEO_REQUEST_INTERVAL_MILLIS = 600L
private const val LISTEN_VIDEO_PREVIEW_FOLDER_LIMIT = 8

internal data class ListenVideoCollectedFoldersPage(
    val folders: List<FavFolder>,
    val hasMore: Boolean
)

internal data class ListenVideoIndexResult(
    val resources: List<FavoriteData>,
    val failedFolderIds: Set<Long>,
    val haltedByRiskControl: Boolean = false
)

internal interface ListenVideoLibraryDataSource {
    suspend fun ownedFolders(mid: Long): Result<List<FavFolder>>

    suspend fun collectedFolders(mid: Long, page: Int): Result<ListenVideoCollectedFoldersPage>

    suspend fun folderPage(mediaId: Long, page: Int): Result<FavoriteResourceData>

    suspend fun albumPage(seasonId: Long, page: Int): Result<FavoriteResourceData>
}

internal class BilibiliListenVideoLibraryDataSource : ListenVideoLibraryDataSource {
    override suspend fun ownedFolders(mid: Long): Result<List<FavFolder>> {
        return FavoriteRepository.getFavFolders(mid)
    }

    override suspend fun collectedFolders(
        mid: Long,
        page: Int
    ): Result<ListenVideoCollectedFoldersPage> {
        return FavoriteRepository.getCollectedFavFolders(
            mid = mid,
            pn = page,
            ps = LISTEN_VIDEO_PAGE_SIZE,
            platform = "web"
        ).map { result ->
            ListenVideoCollectedFoldersPage(
                folders = result.folders,
                hasMore = if (result.totalCount > 0) {
                    page * LISTEN_VIDEO_PAGE_SIZE < result.totalCount
                } else {
                    result.folders.size >= LISTEN_VIDEO_PAGE_SIZE
                }
            )
        }
    }

    override suspend fun folderPage(
        mediaId: Long,
        page: Int
    ): Result<FavoriteResourceData> {
        return FavoriteRepository.getFavoriteList(
            mediaId = mediaId,
            pn = page,
            ps = LISTEN_VIDEO_PAGE_SIZE
        )
    }

    override suspend fun albumPage(
        seasonId: Long,
        page: Int
    ): Result<FavoriteResourceData> {
        return FavoriteRepository.getFavoriteSeasonList(seasonId = seasonId, pn = page)
    }
}

internal class ListenVideoLibraryLoader(
    private val source: ListenVideoLibraryDataSource,
    private val minimumRequestIntervalMillis: Long = LISTEN_VIDEO_REQUEST_INTERVAL_MILLIS,
    private val delayAction: suspend (Long) -> Unit = { delay(it) },
    private val nowMillis: () -> Long = { System.nanoTime() / 1_000_000L },
    private val previewCoverSelector: (List<String>) -> String? = { covers ->
        covers.randomOrNull()
    }
) {
    private val folderRequestMutex = Mutex()
    private val firstFolderPages = mutableMapOf<Long, FavoriteResourceData>()
    private var lastFolderRequestAtMillis: Long? = null

    suspend fun loadCollectedFolders(mid: Long): Result<List<FavFolder>> {
        return loadPages { page -> source.collectedFolders(mid, page) }
    }

    suspend fun loadFolder(mediaId: Long): Result<List<FavoriteData>> {
        return loadFolderPages(mediaId)
    }

    suspend fun loadAlbum(seasonId: Long): Result<List<FavoriteData>> {
        return loadResourcePages { page -> source.albumPage(seasonId, page) }
    }

    suspend fun loadPlaylistPreviewCovers(
        folders: List<FavFolder>
    ): Map<Long, String> {
        val coversByFolder = linkedMapOf<Long, String>()
        folders.asSequence()
            .filter { it.id > 0L && it.cover.isBlank() }
            .take(LISTEN_VIDEO_PREVIEW_FOLDER_LIMIT)
            .forEach { folder ->
                val result = requestFolderPage(folder.id, 1)
                if (result.isFailure && isFavoriteRiskControlError(result.exceptionOrNull()!!)) {
                    return coversByFolder
                }
                val covers = result.getOrNull()?.medias.orEmpty()
                    .map(FavoriteData::cover)
                    .filter(String::isNotBlank)
                previewCoverSelector(covers)?.let { cover -> coversByFolder[folder.id] = cover }
            }
        return coversByFolder
    }

    suspend fun indexFolders(
        folders: List<FavFolder>,
        onFolderIndexed: (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): ListenVideoIndexResult {
        val validFolders = folders.filter { it.id > 0L }
        val resources = mutableListOf<FavoriteData>()
        val failedFolderIds = linkedSetOf<Long>()
        var haltedByRiskControl = false
        for ((index, folder) in validFolders.withIndex()) {
            val result = loadFolderPages(folder.id)
            if (result.isSuccess) {
                resources += result.getOrDefault(emptyList())
            } else {
                failedFolderIds += folder.id
                if (isFavoriteRiskControlError(result.exceptionOrNull()!!)) {
                    haltedByRiskControl = true
                    failedFolderIds += validFolders.drop(index + 1).map(FavFolder::id)
                    onFolderIndexed(index + 1, validFolders.size)
                    break
                }
            }
            onFolderIndexed(index + 1, validFolders.size)
        }

        return ListenVideoIndexResult(
            resources = resources.distinctBy { resource ->
                    if (resource.type == 21) {
                        "album:${resource.season_id.takeIf { it > 0L } ?: resource.id}"
                    } else {
                        "track:${resource.bvid.ifBlank { resource.bv_id }}"
                    }
                },
            failedFolderIds = failedFolderIds,
            haltedByRiskControl = haltedByRiskControl
        )
    }

    private suspend fun loadPages(
        request: suspend (Int) -> Result<ListenVideoCollectedFoldersPage>
    ): Result<List<FavFolder>> {
        return try {
            val folders = mutableListOf<FavFolder>()
            var page = 1
            do {
                val response = request(page).getOrThrow()
                folders += response.folders
                page += 1
            } while (response.hasMore)
            Result.success(folders.distinctBy { it.id })
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }

    private suspend fun loadResourcePages(
        request: suspend (Int) -> Result<FavoriteResourceData>
    ): Result<List<FavoriteData>> {
        return try {
            val resources = mutableListOf<FavoriteData>()
            var page = 1
            do {
                val response = request(page).getOrThrow()
                resources += response.medias.orEmpty()
                page += 1
            } while (response.has_more)
            Result.success(resources)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }

    private suspend fun loadFolderPages(mediaId: Long): Result<List<FavoriteData>> {
        return loadResourcePages { page -> requestFolderPage(mediaId, page) }
    }

    /** Serialize favorite-folder requests and keep a minimum gap between actual network calls. */
    private suspend fun requestFolderPage(
        mediaId: Long,
        page: Int
    ): Result<FavoriteResourceData> = folderRequestMutex.withLock {
        if (page == 1) {
            firstFolderPages[mediaId]?.let { return@withLock Result.success(it) }
        }
        val lastRequest = lastFolderRequestAtMillis
        if (lastRequest != null) {
            val elapsed = (nowMillis() - lastRequest).coerceAtLeast(0L)
            val remaining = minimumRequestIntervalMillis - elapsed
            if (remaining > 0L) delayAction(remaining)
        }
        val result = try {
            source.folderPage(mediaId, page)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } finally {
            lastFolderRequestAtMillis = nowMillis()
        }
        result.getOrNull()?.let { response ->
            if (page == 1) firstFolderPages[mediaId] = response
        }
        result
    }
}
