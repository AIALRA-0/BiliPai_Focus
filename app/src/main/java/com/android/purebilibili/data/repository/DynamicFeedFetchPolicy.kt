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

internal fun shouldContinueDynamicIncrementalFetch(
    accumulatedItemCount: Int,
    updateNum: Int,
    hasMore: Boolean,
    previousOffset: String,
    nextOffset: String
): Boolean {
    if (accumulatedItemCount >= updateNum.coerceAtLeast(0)) return false
    if (!hasMore) return false

    val previous = previousOffset.trim()
    val next = nextOffset.trim()
    if (next.isBlank()) return false
    if (next == previous) return false

    return true
}

internal fun resolveDynamicFeedUpdateBaseline(
    currentBaseline: String,
    responseBaseline: String,
    pagesFetched: Int
): String {
    if (pagesFetched > 0) return currentBaseline
    return responseBaseline.ifBlank { currentBaseline }
}
