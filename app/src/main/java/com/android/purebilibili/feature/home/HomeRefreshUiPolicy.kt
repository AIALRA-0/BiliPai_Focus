package com.android.purebilibili.feature.home

internal fun shouldHandleRefreshNewItemsEvent(
    refreshKey: Long,
    handledKey: Long
): Boolean {
    if (refreshKey <= 0L) return false
    return refreshKey > handledKey
}

/**
 * 分区/热门综合等分页流：手动刷新时翻到下一页，避免永远拉 pn=1 看起来没换视频。
 */
internal fun resolvePagedFeedPageToFetch(
    isLoadMore: Boolean,
    isManualRefresh: Boolean,
    currentPageIndex: Int,
    advanceOnManualRefresh: Boolean
): Int = when {
    isLoadMore -> currentPageIndex + 1
    isManualRefresh && advanceOnManualRefresh -> (currentPageIndex + 1).coerceAtLeast(1)
    else -> 1
}

internal fun shouldAdvancePagedFeedOnManualRefresh(
    category: HomeCategory,
    popularSubCategory: PopularSubCategory
): Boolean = when (category) {
    HomeCategory.RECOMMEND,
    HomeCategory.FOLLOW,
    HomeCategory.LIVE -> false
    HomeCategory.POPULAR -> popularSubCategory == PopularSubCategory.COMPREHENSIVE
    else -> category.tid > 0
}

internal fun resolvePagedFeedPageIndexAfterFetch(
    isLoadMore: Boolean,
    isManualRefresh: Boolean,
    advanceOnManualRefresh: Boolean,
    pageToFetch: Int,
    incomingCount: Int,
    previousPageIndex: Int
): Int = when {
    isLoadMore -> if (incomingCount > 0) previousPageIndex + 1 else previousPageIndex
    isManualRefresh && advanceOnManualRefresh -> {
        if (incomingCount > 0) pageToFetch else 0
    }
    else -> 1
}

/**
 * 关注流下拉刷新的「新增」提示数。
 *
 * 对齐 bilibili-API-collect：只有带着 `update_baseline` 请求时，
 * 响应里的 `update_num` 才表示基线以上的新动态条数。
 * 未走基线的整表重载不应提示「暂无新内容」。
 */
internal fun resolveHomeFollowRefreshNewItemsCount(
    usedUpdateBaseline: Boolean,
    apiUpdateNum: Int,
    insertedVideoCount: Int
): Int? {
    if (!usedUpdateBaseline) return null
    if (apiUpdateNum <= 0) return 0
    return insertedVideoCount.coerceAtLeast(0)
}

internal fun shouldFullReplaceFollowFeedAfterBaselineProbe(
    incrementalRefreshEnabled: Boolean,
    apiUpdateNum: Int
): Boolean = !incrementalRefreshEnabled && apiUpdateNum > 0

internal fun shouldShowRecommendOldContentDivider(
    currentCategory: HomeCategory,
    refreshNewItemsKey: Long,
    revealedRefreshKey: Long,
    anchorBvid: String?,
    oldContentStartIndex: Int?
): Boolean {
    if (currentCategory != HomeCategory.RECOMMEND) return false
    if (refreshNewItemsKey <= 0L || revealedRefreshKey != refreshNewItemsKey) return false
    return !anchorBvid.isNullOrBlank() || (oldContentStartIndex != null && oldContentStartIndex > 0)
}
