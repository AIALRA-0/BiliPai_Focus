package com.android.purebilibili.feature.home

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

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
    oldContentStartIndex: Int?,
    refreshTipVisible: Boolean = true
): Boolean {
    if (!refreshTipVisible) return false
    if (currentCategory != HomeCategory.RECOMMEND) return false
    if (refreshNewItemsKey <= 0L || revealedRefreshKey != refreshNewItemsKey) return false
    return !anchorBvid.isNullOrBlank() || (oldContentStartIndex != null && oldContentStartIndex > 0)
}

/**
 * 推荐流增量刷新合并后旧内容的保留上限，对齐 PiliPlus：
 * 旧内容超过 [threshold] 条时只保留最新的 [keepCount] 条，防止长期不刷新列表无限膨胀。
 * 返回应保留的旧内容条数（新内容不受影响）。
 */
internal fun resolveHomeRefreshKeptOldItemCount(
    oldCount: Int,
    threshold: Int = 200,
    keepCount: Int = 50
): Int = if (oldCount > threshold) keepCount else oldCount

/**
 * 「上次刷新到这里」分隔条的容器颜色。
 *
 * 半透明的 primaryContainer：首页/动态信息流铺在模糊壁纸上，卡片都是毛玻璃
 * 半透明容器；分隔条若用实心 primaryContainer 会形成一块不透模糊的不和谐
 * 色块。降低不透明度让模糊背景透出，同时保留主色调的可读性。
 */
internal fun resolveOldContentDividerContainerColor(colorScheme: ColorScheme): Color =
    colorScheme.primaryContainer.copy(alpha = OLD_CONTENT_DIVIDER_CONTAINER_ALPHA)

internal const val OLD_CONTENT_DIVIDER_CONTAINER_ALPHA = 0.72f
