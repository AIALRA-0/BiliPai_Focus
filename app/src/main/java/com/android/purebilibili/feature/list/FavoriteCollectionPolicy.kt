package com.android.purebilibili.feature.list

import com.android.purebilibili.data.model.response.FavFolder
import com.android.purebilibili.data.model.response.FavFolderSource
import com.android.purebilibili.data.model.response.FavoriteData
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.data.repository.FavoriteRequestException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

data class FavoriteCollectionRoute(
    val type: String,
    val id: Long,
    val mid: Long,
    val title: String,
    val ownerName: String = "",
    val sharedElementTransition: Boolean = false
)

internal fun resolveFavoriteCollectionSharedElementId(
    type: String,
    id: Long
): String? {
    val normalizedType = type.trim()
    if (normalizedType.isEmpty() || id <= 0L) return null
    return "$normalizedType:$id"
}

/** 文档 x/v3/fav/resource/list：ps 定义域 1-20。 */
internal const val FAVORITE_RESOURCE_PAGE_SIZE_MAX = 20

internal fun resolveFavoriteFolderContentPageSize(): Int = FAVORITE_RESOURCE_PAGE_SIZE_MAX

internal fun normalizeFavoriteResourcePageSize(ps: Int): Int =
    ps.coerceIn(1, FAVORITE_RESOURCE_PAGE_SIZE_MAX)

internal fun resolveSubscribedFavoritePreviewCover(folder: FavFolder): String? {
    if (folder.source != FavFolderSource.SUBSCRIBED) return null
    return folder.cover.trim().takeIf { it.isNotEmpty() }
}

internal fun resolveFavoriteFolderPreviewCover(
    folder: FavFolder,
    loadedItems: List<VideoItem>
): String? {
    folder.cover.trim().takeIf { it.isNotEmpty() }?.let { return it }
    return loadedItems.firstNotNullOfOrNull { item ->
        item.pic.trim().takeIf { it.isNotEmpty() }
    }
}

internal fun mergeFavoriteFoldersForDisplay(
    ownedFolders: List<FavFolder>,
    subscribedFolders: List<FavFolder>
): List<FavFolder> {
    val seenIds = HashSet<Long>()
    return (ownedFolders + subscribedFolders).filter { folder ->
        val valid = folder.id > 0L && folder.title.isNotBlank()
        valid && seenIds.add(folder.id)
    }
}

internal fun resolveFavoriteFolderTabLabel(folder: FavFolder): String {
    return if (folder.source == FavFolderSource.SUBSCRIBED) {
        "${folder.title} · 订阅"
    } else {
        folder.title
    }
}

internal fun resolveFavoriteFolderMediaId(folder: FavFolder): Long {
    if (folder.source != FavFolderSource.SUBSCRIBED) {
        return folder.id.takeIf { it > 0L } ?: folder.fid
    }

    val normalizedFromFid = when {
        folder.fid > 0L && folder.mid > 0L -> {
            val suffix = (folder.mid % 100L).toString().padStart(2, '0')
            "${folder.fid}$suffix".toLongOrNull()
        }
        else -> null
    }

    return when {
        folder.id > 0L && folder.id == normalizedFromFid -> folder.id
        folder.id > 0L && folder.id != folder.fid && folder.id > 100_000_000L -> folder.id
        normalizedFromFid != null -> normalizedFromFid
        folder.id > 0L -> folder.id
        else -> folder.fid
    }
}

internal fun resolveSubscribedFavoriteCollectionRoute(folder: FavFolder): FavoriteCollectionRoute? {
    if (folder.source != FavFolderSource.SUBSCRIBED || folder.type != 21) return null
    if (folder.id <= 0L || folder.title.isBlank()) return null
    return FavoriteCollectionRoute(
        type = "favorite_season",
        id = folder.id,
        mid = folder.mid,
        title = folder.title,
        ownerName = folder.upper?.name.orEmpty(),
        sharedElementTransition = true
    )
}

/** 非 type=21 的订阅夹也走合集详情页时，允许列表→详情做共享元素。 */
internal fun resolveSubscribedFavoriteFolderRoute(folder: FavFolder): FavoriteCollectionRoute? {
    resolveSubscribedFavoriteCollectionRoute(folder)?.let { return it }
    if (folder.source != FavFolderSource.SUBSCRIBED) return null
    val mediaId = resolveFavoriteFolderMediaId(folder)
    if (mediaId <= 0L || folder.title.isBlank()) return null
    return FavoriteCollectionRoute(
        type = "favorite",
        id = mediaId,
        mid = folder.mid,
        title = folder.title,
        ownerName = folder.upper?.name.orEmpty(),
        sharedElementTransition = true
    )
}

internal fun filterFavoriteFoldersByQuery(
    folders: List<FavFolder>,
    query: String
): List<FavFolder> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return folders
    return folders.filter { folder ->
        folder.title.contains(normalizedQuery, ignoreCase = true)
    }
}

internal fun resolveFavoriteCollectionRoute(item: VideoItem): FavoriteCollectionRoute? {
    if (!item.isCollectionResource || item.collectionId <= 0L) return null
    return FavoriteCollectionRoute(
        type = "favorite_season",
        id = item.collectionId,
        mid = item.collectionMid,
        title = item.title,
        ownerName = item.owner.name
    )
}

internal fun resolveFavoriteFolderLoadState(
    previousState: ListUiState,
    title: String,
    canRemoveItems: Boolean,
    result: Result<List<VideoItem>>
): ListUiState {
    return result.fold(
        onSuccess = { items ->
            previousState.copy(
                title = title,
                items = items,
                isLoading = false,
                error = null,
                canRemoveItems = canRemoveItems
            )
        },
        onFailure = { throwable ->
            previousState.copy(
                title = title,
                isLoading = false,
                error = resolveFavoriteErrorMessage(throwable),
                canRemoveItems = canRemoveItems
            )
        }
    )
}

internal fun resolveFavoriteFolderItems(
    expectedItemCount: Int,
    resources: List<FavoriteData>?
): Result<List<VideoItem>> {
    if (resources == null && expectedItemCount > 0) {
        return Result.failure(IllegalStateException("收藏夹内容响应不完整，请稍后重试"))
    }
    return Result.success(resources.orEmpty().map { it.toVideoItem() })
}

internal suspend fun <T> requestFavoriteFolderWithRetry(
    maxAttempts: Int = 2,
    retryDelayMillis: Long = 400L,
    delayAction: suspend (Long) -> Unit = { delay(it) },
    request: suspend () -> Result<T>
): Result<T> {
    require(maxAttempts > 0)
    var lastFailure: Throwable = IllegalStateException("收藏夹请求未执行")
    repeat(maxAttempts) { attempt ->
        val result = runCatching { request() }
            .fold(
                onSuccess = { it },
                onFailure = { throwable ->
                    if (throwable is CancellationException) throw throwable
                    Result.failure(throwable)
                }
            )
        result.exceptionOrNull()?.let { throwable ->
            if (throwable is CancellationException) throw throwable
        }
        if (result.isSuccess) return result
        lastFailure = result.exceptionOrNull() ?: lastFailure
        if (attempt < maxAttempts - 1 && shouldRetryFavoriteFolderRequest(lastFailure)) {
            delayAction(retryDelayMillis)
        } else {
            return Result.failure(lastFailure)
        }
    }
    return Result.failure(lastFailure)
}

internal fun shouldRetryFavoriteFolderRequest(throwable: Throwable): Boolean {
    if (throwable is CancellationException) throw throwable
    if (isFavoriteRiskControlError(throwable)) return false
    return when (throwable) {
        is FavoriteRequestException -> throwable.httpCode in 500..599
        is IOException -> true
        else -> throwable.cause?.let(::shouldRetryFavoriteFolderRequest) ?: false
    }
}

internal fun resolveFavoriteErrorMessage(throwable: Throwable): String {
    if (isFavoriteRiskControlError(throwable)) {
        return "请求被风控，请稍后重试"
    }
    return throwable.message?.takeIf(String::isNotBlank)
        ?: "加载收藏夹失败，请稍后重试"
}

internal fun isFavoriteRiskControlError(throwable: Throwable): Boolean {
    val requestError = throwable as? FavoriteRequestException
    if (requestError?.apiCode in setOf(-412, 412, -429, 429)) return true
    if (requestError?.httpCode in setOf(412, 429)) return true
    val message = throwable.message.orEmpty()
    return message.contains("HTTP 412", ignoreCase = true) ||
        message.contains("HTTP 429", ignoreCase = true) ||
        throwable.cause?.let(::isFavoriteRiskControlError) == true
}

internal fun shouldApplyFavoriteFolderResult(
    requestGeneration: Long,
    currentGeneration: Long,
    requestedMediaId: Long,
    currentMediaId: Long?,
    requestedOrder: String,
    currentOrder: String
): Boolean {
    return requestGeneration == currentGeneration &&
        requestedMediaId == currentMediaId &&
        requestedOrder == currentOrder
}
