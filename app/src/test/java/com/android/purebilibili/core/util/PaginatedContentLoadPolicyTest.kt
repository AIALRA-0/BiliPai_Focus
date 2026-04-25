package com.android.purebilibili.core.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PaginatedContentLoadPolicyTest {

    @Test
    fun blocksWhenPagingIsDisabledOrSourceEnded() {
        assertFalse(
            shouldLoadMorePaginatedContent(
                totalItems = 12,
                lastVisibleItemIndex = 11,
                contentItemCount = 10,
                isLoading = false,
                hasMore = true,
                autoLoadMoreEnabled = false
            )
        )
        assertFalse(
            shouldLoadMorePaginatedContent(
                totalItems = 12,
                lastVisibleItemIndex = 11,
                contentItemCount = 10,
                isLoading = false,
                hasMore = false
            )
        )
    }

    @Test
    fun blocksUntilUserActuallyScrolledWhenObservationIsRequired() {
        assertFalse(
            shouldLoadMorePaginatedContent(
                totalItems = 12,
                lastVisibleItemIndex = 11,
                contentItemCount = 10,
                isLoading = false,
                hasMore = true,
                requireUserScrollObservation = true,
                userScrollObserved = false
            )
        )
        assertTrue(
            shouldLoadMorePaginatedContent(
                totalItems = 12,
                lastVisibleItemIndex = 11,
                contentItemCount = 10,
                isLoading = false,
                hasMore = true,
                requireUserScrollObservation = true,
                userScrollObserved = true
            )
        )
    }

    @Test
    fun sparseVisibleContentCanContinueBeforeTailThreshold() {
        assertTrue(
            shouldLoadMorePaginatedContent(
                totalItems = 3,
                lastVisibleItemIndex = 0,
                contentItemCount = 2,
                isLoading = false,
                hasMore = true,
                minimumVisibleItemCountBeforePause = 8
            )
        )
    }
}
