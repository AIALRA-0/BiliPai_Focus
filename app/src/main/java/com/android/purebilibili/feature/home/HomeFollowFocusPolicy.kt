package com.android.purebilibili.feature.home

import com.android.purebilibili.core.store.FocusFollowGroupConfig
import com.android.purebilibili.core.store.isFocusFollowUserVisible
import com.android.purebilibili.core.util.appendDistinctByKey
import com.android.purebilibili.core.util.prependDistinctByKey
import com.android.purebilibili.data.model.response.VideoItem

private const val HOME_FOLLOW_REFRESH_COMPLETION_FETCH_LIMIT = 4
private const val HOME_FOLLOW_LOAD_MORE_COMPLETION_FETCH_LIMIT = 32
private const val HOME_FOLLOW_EMPTY_MESSAGE = "没有可用关注对象"

internal fun filterHomeFollowVideosByFocusFollowGroups(
    videos: List<VideoItem>,
    config: FocusFollowGroupConfig,
    filterEnabled: Boolean = true
): List<VideoItem> {
    if (!filterEnabled) return videos
    return videos.filter { video ->
        isFocusFollowUserVisible(config, video.owner.mid)
    }
}

internal fun resolveHomeFollowEmptyMessage(
    visibleVideoCount: Int,
    hasResolvedFollowFeedOnce: Boolean = true
): String? {
    if (!hasResolvedFollowFeedOnce || visibleVideoCount > 0) return null
    return HOME_FOLLOW_EMPTY_MESSAGE
}

internal fun resolveHomeFollowErrorAfterRefilter(
    visibleVideoCount: Int,
    hasResolvedFollowFeedOnce: Boolean,
    existingError: String?
): String? {
    if (!existingError.isNullOrBlank() && existingError != HOME_FOLLOW_EMPTY_MESSAGE) {
        return existingError
    }
    return resolveHomeFollowEmptyMessage(
        visibleVideoCount = visibleVideoCount,
        hasResolvedFollowFeedOnce = hasResolvedFollowFeedOnce
    )
}

internal fun resolveHomeFollowVisibleIncrement(
    baselineVisibleCount: Int,
    currentVisibleCount: Int
): Int {
    return (currentVisibleCount - baselineVisibleCount).coerceAtLeast(0)
}

internal fun shouldContinueHomeFollowFetchAfterFocusFilter(
    baselineVisibleCount: Int,
    visibleIncrement: Int,
    hasMore: Boolean,
    continuationFetches: Int,
    isLoadMore: Boolean
): Boolean {
    if (!hasMore) return false
    val maxContinuationFetches = if (isLoadMore) {
        HOME_FOLLOW_LOAD_MORE_COMPLETION_FETCH_LIMIT
    } else {
        HOME_FOLLOW_REFRESH_COMPLETION_FETCH_LIMIT
    }
    if (continuationFetches >= maxContinuationFetches) return false
    if (visibleIncrement > 0) return false
    return isLoadMore || baselineVisibleCount == 0
}

internal fun accumulateHomeFollowRoundRawVideos(
    existingRoundRawVideos: List<VideoItem>,
    incomingRawVideos: List<VideoItem>,
    keySelector: (VideoItem) -> String
): List<VideoItem> {
    return appendDistinctByKey(existingRoundRawVideos, incomingRawVideos, keySelector)
}

internal fun resolveHomeFollowPresentedRawVideos(
    baselineRawVideos: List<VideoItem>,
    roundRawVideos: List<VideoItem>,
    isLoadMore: Boolean,
    incrementalTimelineRefreshEnabled: Boolean,
    keySelector: (VideoItem) -> String
): List<VideoItem> {
    return when {
        isLoadMore -> appendDistinctByKey(baselineRawVideos, roundRawVideos, keySelector)
        incrementalTimelineRefreshEnabled -> prependDistinctByKey(baselineRawVideos, roundRawVideos, keySelector)
        else -> roundRawVideos
    }
}
