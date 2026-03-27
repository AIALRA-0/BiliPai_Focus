package com.android.purebilibili.feature.space

import com.android.purebilibili.data.model.response.FavFolder
import com.android.purebilibili.data.model.response.SeasonArchiveItem
import com.android.purebilibili.data.model.response.SeasonItem
import com.android.purebilibili.data.model.response.SeriesArchiveItem
import com.android.purebilibili.data.model.response.SeriesItem
import com.android.purebilibili.data.model.response.SpaceVideoItem
import com.android.purebilibili.data.model.response.VideoSortOrder

enum class SpaceSearchScope {
    NONE,
    DYNAMIC,
    VIDEO
}

internal fun resolveSpaceSearchScope(
    selectedMainTab: SpaceMainTab,
    selectedSubTab: SpaceSubTab
): SpaceSearchScope {
    return when {
        selectedMainTab == SpaceMainTab.DYNAMIC -> SpaceSearchScope.DYNAMIC
        selectedMainTab == SpaceMainTab.CONTRIBUTION && selectedSubTab == SpaceSubTab.VIDEO -> {
            SpaceSearchScope.VIDEO
        }
        else -> SpaceSearchScope.NONE
    }
}

internal fun resolveSpaceSearchPlaceholder(scope: SpaceSearchScope): String {
    return when (scope) {
        SpaceSearchScope.DYNAMIC -> "搜索 TA 的动态"
        SpaceSearchScope.VIDEO -> "搜索 TA 的视频"
        SpaceSearchScope.NONE -> ""
    }
}

internal fun shouldApplySpaceLoadResult(
    requestMid: Long,
    activeMid: Long,
    requestGeneration: Long,
    activeGeneration: Long
): Boolean {
    return requestMid > 0L &&
        requestMid == activeMid &&
        requestGeneration == activeGeneration
}

internal fun applySpaceSupplementalData(
    state: SpaceUiState.Success,
    seasons: List<SeasonItem>,
    series: List<SeriesItem>,
    createdFavoriteFolders: List<FavFolder>,
    collectedFavoriteFolders: List<FavFolder>,
    seasonArchives: Map<Long, List<SeasonArchiveItem>>,
    seriesArchives: Map<Long, List<SeriesArchiveItem>>
): SpaceUiState.Success {
    val nextState = state.copy(
        seasons = seasons,
        series = series,
        createdFavoriteFolders = createdFavoriteFolders,
        collectedFavoriteFolders = collectedFavoriteFolders,
        seasonArchives = seasonArchives,
        seriesArchives = seriesArchives,
        headerState = state.headerState.copy(
            createdFavorites = createdFavoriteFolders,
            collectedFavorites = collectedFavoriteFolders
        )
    )

    val hasCollectionsLoaded = seasons.isNotEmpty() ||
        series.isNotEmpty() ||
        createdFavoriteFolders.isNotEmpty() ||
        collectedFavoriteFolders.isNotEmpty()

    return nextState.copy(
        tabShellState = nextState.tabShellState.withUpdatedTab(SpaceMainTab.COLLECTIONS) {
            it.copy(hasLoaded = hasCollectionsLoaded)
        }
    )
}

internal fun resolveInitialSpaceVideoPage(
    order: VideoSortOrder,
    totalCount: Int,
    pageSize: Int
): Int {
    val lastPage = resolveSpaceVideoLastPage(totalCount = totalCount, pageSize = pageSize)
    return if (order == VideoSortOrder.OLDEST_PUBDATE) lastPage else 1
}

internal fun resolveNextSpaceVideoPage(
    order: VideoSortOrder,
    currentPage: Int,
    totalCount: Int,
    pageSize: Int
): Int? {
    val lastPage = resolveSpaceVideoLastPage(totalCount = totalCount, pageSize = pageSize)
    if (lastPage <= 0) return null
    return when (order) {
        VideoSortOrder.OLDEST_PUBDATE -> currentPage.takeIf { it > 1 }?.minus(1)
        else -> currentPage.takeIf { it < lastPage }?.plus(1)
    }
}

internal fun normalizeSpaceVideoPage(
    order: VideoSortOrder,
    videos: List<SpaceVideoItem>
): List<SpaceVideoItem> {
    return if (order == VideoSortOrder.OLDEST_PUBDATE) videos.asReversed() else videos
}

private fun resolveSpaceVideoLastPage(totalCount: Int, pageSize: Int): Int {
    if (totalCount <= 0 || pageSize <= 0) return 1
    return ((totalCount - 1) / pageSize) + 1
}
