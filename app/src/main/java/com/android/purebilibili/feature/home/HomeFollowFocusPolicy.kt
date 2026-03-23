package com.android.purebilibili.feature.home

import com.android.purebilibili.core.store.FocusFollowGroupConfig
import com.android.purebilibili.core.store.isFocusFollowUserVisible
import com.android.purebilibili.data.model.response.VideoItem

private const val MIN_VISIBLE_HOME_FOLLOW_INITIAL_REFRESH_VIDEOS = 6
private const val MIN_VISIBLE_HOME_FOLLOW_TOTAL_REFRESH_VIDEOS = 12
private const val MIN_VISIBLE_HOME_FOLLOW_APPEND_VIDEOS = 8
private const val MAX_HOME_FOLLOW_INITIAL_REFRESH_PREFETCH_PAGES = 2
private const val MAX_HOME_FOLLOW_TOTAL_REFRESH_PREFETCH_PAGES = 6
private const val MAX_HOME_FOLLOW_APPEND_PREFETCH_PAGES = 4

internal data class HomeFollowPrefetchBudget(
    val minVisibleCount: Int,
    val maxExtraPages: Int
)

internal data class HomeFollowPrefetchPlan(
    val foregroundBudget: HomeFollowPrefetchBudget,
    val backgroundBudget: HomeFollowPrefetchBudget? = null
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
            foregroundBudget = HomeFollowPrefetchBudget(
                minVisibleCount = MIN_VISIBLE_HOME_FOLLOW_APPEND_VIDEOS,
                maxExtraPages = MAX_HOME_FOLLOW_APPEND_PREFETCH_PAGES
            )
        )
    } else {
        HomeFollowPrefetchPlan(
            foregroundBudget = HomeFollowPrefetchBudget(
                minVisibleCount = MIN_VISIBLE_HOME_FOLLOW_INITIAL_REFRESH_VIDEOS,
                maxExtraPages = MAX_HOME_FOLLOW_INITIAL_REFRESH_PREFETCH_PAGES
            ),
            backgroundBudget = HomeFollowPrefetchBudget(
                minVisibleCount = MIN_VISIBLE_HOME_FOLLOW_TOTAL_REFRESH_VIDEOS,
                maxExtraPages = MAX_HOME_FOLLOW_TOTAL_REFRESH_PREFETCH_PAGES
            )
        )
    }
}

internal fun shouldPrefetchMoreHomeFollowVideos(
    visibleVideoCount: Int,
    hasMore: Boolean,
    extraPagesFetched: Int,
    budget: HomeFollowPrefetchBudget
): Boolean {
    if (!hasMore) return false
    if (visibleVideoCount >= budget.minVisibleCount.coerceAtLeast(1)) return false
    return extraPagesFetched < budget.maxExtraPages.coerceAtLeast(0)
}
