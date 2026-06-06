package com.android.purebilibili.data.repository

internal const val DYNAMIC_VISIBLE_PAGE_TARGET = 16
internal const val DYNAMIC_FILTER_COMPLETION_FETCH_LIMIT = 8

internal fun hasDynamicPaginationProgress(
    previousOffset: String,
    nextOffset: String
): Boolean {
    val previous = previousOffset.trim()
    val next = nextOffset.trim()
    if (next.isBlank()) return false
    return next != previous
}

internal fun shouldContinueDynamicFetchAfterFilter(
    accumulatedVisibleCount: Int,
    hasMore: Boolean,
    previousOffset: String,
    nextOffset: String,
    pagesFetched: Int,
    targetVisibleCount: Int = DYNAMIC_VISIBLE_PAGE_TARGET,
    maxPages: Int = DYNAMIC_FILTER_COMPLETION_FETCH_LIMIT
): Boolean {
    if (accumulatedVisibleCount >= targetVisibleCount.coerceAtLeast(1)) return false
    if (!hasMore) return false
    if (pagesFetched >= maxPages) return false
    if (!hasDynamicPaginationProgress(previousOffset, nextOffset)) return false

    return true
}
