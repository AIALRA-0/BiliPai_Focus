package com.android.purebilibili.feature.home

internal fun shouldLoadMoreHomeCategoryContent(
    totalItems: Int,
    lastVisibleItemIndex: Int,
    contentItemCount: Int,
    isLoading: Boolean,
    hasMore: Boolean,
    autoLoadMoreEnabled: Boolean = true,
    requireUserScrollObservation: Boolean = false,
    userScrollObserved: Boolean = true
): Boolean {
    if (!autoLoadMoreEnabled || isLoading || !hasMore) return false
    if (contentItemCount <= 0 || totalItems <= 0) return false
    if (requireUserScrollObservation && !userScrollObserved) return false
    return lastVisibleItemIndex >= totalItems - 3
}

internal fun hasObservedFollowLoadMoreScroll(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int
): Boolean {
    return firstVisibleItemIndex > 0 || firstVisibleItemScrollOffset > 0
}
