package com.android.purebilibili.feature.home

import com.android.purebilibili.core.store.FocusFollowGroupConfig
import com.android.purebilibili.core.store.isFocusFollowUserVisible
import com.android.purebilibili.data.model.response.VideoItem

private const val HOME_FOLLOW_FILTER_COMPLETION_FETCH_LIMIT = 8

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
    visibleVideoCount: Int
): String? {
    if (visibleVideoCount > 0) return null
    return "没有可用关注对象"
}

internal fun resolveHomeFollowVisibleIncrement(
    baselineVisibleCount: Int,
    currentVisibleCount: Int
): Int {
    return (currentVisibleCount - baselineVisibleCount).coerceAtLeast(0)
}

internal fun shouldContinueHomeFollowFetchAfterFocusFilter(
    targetRawIncrement: Int?,
    visibleIncrement: Int,
    hasMore: Boolean,
    continuationFetches: Int,
    maxContinuationFetches: Int = HOME_FOLLOW_FILTER_COMPLETION_FETCH_LIMIT
): Boolean {
    if (!hasMore) return false
    if (continuationFetches >= maxContinuationFetches) return false

    val requiredVisibleIncrement = targetRawIncrement ?: 1
    return visibleIncrement < requiredVisibleIncrement
}
