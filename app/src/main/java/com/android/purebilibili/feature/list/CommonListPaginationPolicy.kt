package com.android.purebilibili.feature.list

internal enum class CommonListLoadMoreOwner {
    NONE,
    FAVORITE,
    HISTORY,
    LIKED_VIDEOS,
    SEASON_SERIES_DETAIL
}

internal data class CommonListPaginationSnapshot(
    val hasMore: Boolean,
    val isLoadingMore: Boolean
)

internal fun resolveCommonListLoadMoreOwner(
    isSubscribedBrowse: Boolean,
    hasFavoriteViewModel: Boolean,
    hasHistoryViewModel: Boolean,
    hasLikedVideosViewModel: Boolean = false,
    hasSeasonSeriesDetailViewModel: Boolean
): CommonListLoadMoreOwner {
    if (isSubscribedBrowse) return CommonListLoadMoreOwner.NONE
    if (hasFavoriteViewModel) return CommonListLoadMoreOwner.FAVORITE
    if (hasHistoryViewModel) return CommonListLoadMoreOwner.HISTORY
    if (hasLikedVideosViewModel) return CommonListLoadMoreOwner.LIKED_VIDEOS
    if (hasSeasonSeriesDetailViewModel) return CommonListLoadMoreOwner.SEASON_SERIES_DETAIL
    return CommonListLoadMoreOwner.NONE
}

internal fun resolveCommonListPaginationSnapshot(
    owner: CommonListLoadMoreOwner,
    favoriteHasMore: Boolean,
    favoriteIsLoadingMore: Boolean,
    historyHasMore: Boolean,
    historyIsLoadingMore: Boolean,
    likedVideosHasMore: Boolean = false,
    likedVideosIsLoadingMore: Boolean = false,
    seasonDetailHasMore: Boolean,
    seasonDetailIsLoadingMore: Boolean
): CommonListPaginationSnapshot {
    return when (owner) {
        CommonListLoadMoreOwner.FAVORITE -> CommonListPaginationSnapshot(
            hasMore = favoriteHasMore,
            isLoadingMore = favoriteIsLoadingMore
        )
        CommonListLoadMoreOwner.HISTORY -> CommonListPaginationSnapshot(
            hasMore = historyHasMore,
            isLoadingMore = historyIsLoadingMore
        )
        CommonListLoadMoreOwner.LIKED_VIDEOS -> CommonListPaginationSnapshot(
            hasMore = likedVideosHasMore,
            isLoadingMore = likedVideosIsLoadingMore
        )
        CommonListLoadMoreOwner.SEASON_SERIES_DETAIL -> CommonListPaginationSnapshot(
            hasMore = seasonDetailHasMore,
            isLoadingMore = seasonDetailIsLoadingMore
        )
        CommonListLoadMoreOwner.NONE -> CommonListPaginationSnapshot(
            hasMore = false,
            isLoadingMore = false
        )
    }
}

/** Lazy layouts require unique keys; all-folder search can legitimately return one video more than once. */
internal fun resolveCommonListRenderKeys(itemKeys: List<String>): List<String> {
    val normalizedKeys = itemKeys.mapIndexed { index, key ->
        key.ifBlank { "common-list-item-$index" }
    }
    val totals = normalizedKeys.groupingBy { it }.eachCount()
    val seen = mutableMapOf<String, Int>()
    return normalizedKeys.map { key ->
        if (totals.getValue(key) == 1) {
            key
        } else {
            val occurrence = seen.getOrDefault(key, 0)
            seen[key] = occurrence + 1
            "$key#$occurrence"
        }
    }
}
