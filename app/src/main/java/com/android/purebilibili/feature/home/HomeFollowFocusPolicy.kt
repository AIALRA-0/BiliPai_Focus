package com.android.purebilibili.feature.home

import com.android.purebilibili.core.store.FocusFollowGroupConfig
import com.android.purebilibili.core.store.isFocusFollowUserVisible
import com.android.purebilibili.data.model.response.VideoItem

private const val HOME_FOLLOW_VISIBLE_BATCH_SIZE = 8
private const val MAX_HOME_FOLLOW_INITIAL_REFRESH_PREFETCH_PAGES = 4
private const val MAX_HOME_FOLLOW_BACKGROUND_PREFETCH_PAGES = 12
private const val MAX_HOME_FOLLOW_APPEND_PREFETCH_PAGES = 8

internal data class HomeFollowPrefetchBudget(
    val targetVisibleCount: Int,
    val maxExtraPages: Int
)

internal data class HomeFollowBackgroundPrefetchPlan(
    val publishBatchSize: Int,
    val maxExtraPages: Int
)

internal data class HomeFollowPrefetchPlan(
    val foregroundBudget: HomeFollowPrefetchBudget,
    val backgroundPlan: HomeFollowBackgroundPrefetchPlan? = null
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

internal fun resolveHomeFollowPrefetchPlan(
    isLoadMore: Boolean,
    currentVisibleCount: Int
): HomeFollowPrefetchPlan {
    return if (isLoadMore) {
        HomeFollowPrefetchPlan(
            foregroundBudget = HomeFollowPrefetchBudget(
                targetVisibleCount = currentVisibleCount.coerceAtLeast(0) + HOME_FOLLOW_VISIBLE_BATCH_SIZE,
                maxExtraPages = MAX_HOME_FOLLOW_APPEND_PREFETCH_PAGES
            )
        )
    } else {
        HomeFollowPrefetchPlan(
            foregroundBudget = HomeFollowPrefetchBudget(
                targetVisibleCount = HOME_FOLLOW_VISIBLE_BATCH_SIZE,
                maxExtraPages = MAX_HOME_FOLLOW_INITIAL_REFRESH_PREFETCH_PAGES
            ),
            backgroundPlan = HomeFollowBackgroundPrefetchPlan(
                publishBatchSize = HOME_FOLLOW_VISIBLE_BATCH_SIZE,
                maxExtraPages = MAX_HOME_FOLLOW_BACKGROUND_PREFETCH_PAGES
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
    if (visibleVideoCount >= budget.targetVisibleCount.coerceAtLeast(1)) return false
    return extraPagesFetched < budget.maxExtraPages.coerceAtLeast(0)
}

internal fun shouldContinueHomeFollowBackgroundPrefetch(
    hasMore: Boolean,
    extraPagesFetched: Int,
    plan: HomeFollowBackgroundPrefetchPlan?
): Boolean {
    val effectivePlan = plan ?: return false
    if (!hasMore) return false
    return extraPagesFetched < effectivePlan.maxExtraPages.coerceAtLeast(0)
}

internal fun shouldPublishHomeFollowPrefetchBatch(
    visibleVideoCount: Int,
    publishedVisibleCount: Int,
    hasMore: Boolean,
    batchSize: Int = HOME_FOLLOW_VISIBLE_BATCH_SIZE
): Boolean {
    val normalizedBatchSize = batchSize.coerceAtLeast(1)
    if (visibleVideoCount >= publishedVisibleCount.coerceAtLeast(0) + normalizedBatchSize) {
        return true
    }
    return !hasMore && visibleVideoCount > publishedVisibleCount.coerceAtLeast(0)
}
