package com.android.purebilibili.feature.home

import com.android.purebilibili.core.store.FocusFollowGroupConfig
import com.android.purebilibili.core.store.isFocusFollowUserVisible
import com.android.purebilibili.data.model.response.VideoItem

private const val MIN_VISIBLE_HOME_FOLLOW_VIDEOS_AFTER_FOCUS_FILTER = 6
private const val MAX_HOME_FOLLOW_PREFETCH_PAGES = 3

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

internal fun shouldPrefetchMoreFocusFollowVideos(
    visibleVideoCount: Int,
    hasMore: Boolean,
    filterEnabled: Boolean,
    extraPagesFetched: Int,
    minVisibleCount: Int = MIN_VISIBLE_HOME_FOLLOW_VIDEOS_AFTER_FOCUS_FILTER,
    maxExtraPages: Int = MAX_HOME_FOLLOW_PREFETCH_PAGES
): Boolean {
    if (!filterEnabled) return false
    if (!hasMore) return false
    if (visibleVideoCount >= minVisibleCount.coerceAtLeast(1)) return false
    return extraPagesFetched < maxExtraPages.coerceAtLeast(0)
}
