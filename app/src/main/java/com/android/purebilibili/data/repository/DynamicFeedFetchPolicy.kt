package com.android.purebilibili.data.repository

internal const val DYNAMIC_EMPTY_PAGE_FETCH_LIMIT = 3

internal fun shouldContinueDynamicFetchAfterFilter(
    accumulatedVisibleCount: Int,
    hasMore: Boolean,
    previousOffset: String,
    nextOffset: String,
    pagesFetched: Int,
    minimumVisibleCount: Int = 1,
    maxPages: Int = DYNAMIC_EMPTY_PAGE_FETCH_LIMIT
): Boolean {
    if (accumulatedVisibleCount >= minimumVisibleCount.coerceAtLeast(1)) return false
    if (!hasMore) return false
    if (pagesFetched >= maxPages) return false

    val previous = previousOffset.trim()
    val next = nextOffset.trim()
    if (next.isBlank()) return false
    if (next == previous) return false

    return true
}
