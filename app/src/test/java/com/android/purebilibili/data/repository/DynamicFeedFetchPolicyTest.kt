package com.android.purebilibili.data.repository

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicFeedFetchPolicyTest {

    @Test
    fun `continue loading when no visible items yet and next page exists`() {
        assertTrue(
            shouldContinueDynamicFetchAfterFilter(
                accumulatedVisibleCount = 0,
                hasMore = true,
                previousOffset = "100",
                nextOffset = "200",
                pagesFetched = 1,
                maxPages = 3
            )
        )
    }

    @Test
    fun `stop loading when visible items already found`() {
        assertFalse(
            shouldContinueDynamicFetchAfterFilter(
                accumulatedVisibleCount = 2,
                hasMore = true,
                previousOffset = "100",
                nextOffset = "200",
                pagesFetched = 1,
                maxPages = 3
            )
        )
    }

    @Test
    fun `stop loading when has more is false`() {
        assertFalse(
            shouldContinueDynamicFetchAfterFilter(
                accumulatedVisibleCount = 0,
                hasMore = false,
                previousOffset = "100",
                nextOffset = "200",
                pagesFetched = 1,
                maxPages = 3
            )
        )
    }

    @Test
    fun `stop loading when offset does not move forward`() {
        assertFalse(
            shouldContinueDynamicFetchAfterFilter(
                accumulatedVisibleCount = 0,
                hasMore = true,
                previousOffset = "100",
                nextOffset = "100",
                pagesFetched = 1,
                maxPages = 3
            )
        )
    }

    @Test
    fun `stop loading when reaching max pages`() {
        assertFalse(
            shouldContinueDynamicFetchAfterFilter(
                accumulatedVisibleCount = 0,
                hasMore = true,
                previousOffset = "100",
                nextOffset = "200",
                pagesFetched = 3,
                maxPages = 3
            )
        )
    }
}
