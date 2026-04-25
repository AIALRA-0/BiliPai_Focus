package com.android.purebilibili.core.util

internal fun shouldLoadMorePaginatedContent(
    totalItems: Int,
    lastVisibleItemIndex: Int,
    contentItemCount: Int,
    isLoading: Boolean,
    hasMore: Boolean,
    autoLoadMoreEnabled: Boolean = true,
    preloadThreshold: Int = 4,
    minimumVisibleItemCountBeforePause: Int = 0,
    requireUserScrollObservation: Boolean = false,
    userScrollObserved: Boolean = true
): Boolean {
    if (!autoLoadMoreEnabled) return false
    if (requireUserScrollObservation && !userScrollObserved) return false
    if (contentItemCount <= 0) return false
    if (isLoading || !hasMore) return false
    val minimumVisibleCount = minimumVisibleItemCountBeforePause.coerceAtLeast(0)
    if (minimumVisibleCount > 0 && contentItemCount < minimumVisibleCount) return true
    if (totalItems <= 0) return false
    return lastVisibleItemIndex >= totalItems - preloadThreshold.coerceAtLeast(1)
}
