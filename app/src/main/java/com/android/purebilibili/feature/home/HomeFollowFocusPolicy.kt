package com.android.purebilibili.feature.home

import com.android.purebilibili.core.store.FocusFollowGroupConfig
import com.android.purebilibili.core.store.isFocusFollowUserVisible
import com.android.purebilibili.data.model.response.VideoItem

private const val MIN_VISIBLE_HOME_FOLLOW_REFRESH_VIDEOS = 18
private const val MIN_VISIBLE_HOME_FOLLOW_APPEND_VIDEOS = 8
private const val MAX_HOME_FOLLOW_REFRESH_PREFETCH_PAGES = 12
private const val MAX_HOME_FOLLOW_APPEND_PREFETCH_PAGES = 4

internal data class HomeFollowPrefetchPlan(
    val minVisibleCount: Int,
    val maxExtraPages: Int
)

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

internal fun resolveHomeFollowPrefetchPlan(isLoadMore: Boolean): HomeFollowPrefetchPlan {
    return if (isLoadMore) {
        HomeFollowPrefetchPlan(
            minVisibleCount = MIN_VISIBLE_HOME_FOLLOW_APPEND_VIDEOS,
            maxExtraPages = MAX_HOME_FOLLOW_APPEND_PREFETCH_PAGES
        )
    } else {
        HomeFollowPrefetchPlan(
            minVisibleCount = MIN_VISIBLE_HOME_FOLLOW_REFRESH_VIDEOS,
            maxExtraPages = MAX_HOME_FOLLOW_REFRESH_PREFETCH_PAGES
        )
    }
}

internal fun shouldPrefetchMoreHomeFollowVideos(
    visibleVideoCount: Int,
    hasMore: Boolean,
    extraPagesFetched: Int,
    plan: HomeFollowPrefetchPlan
): Boolean {
    if (!hasMore) return false
    if (visibleVideoCount >= plan.minVisibleCount.coerceAtLeast(1)) return false
    return extraPagesFetched < plan.maxExtraPages.coerceAtLeast(0)
}
