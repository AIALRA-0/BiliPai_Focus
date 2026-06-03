package com.android.purebilibili.feature.home

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeCategoryLoadMorePolicyTest {

    @Test
    fun emptyPlaceholderOnlyPageDoesNotRequestLoadMore() {
        assertFalse(
            shouldRequestHomeCategoryLoadMore(
                totalItems = 2,
                lastVisibleItemIndex = 1,
                isLoading = false,
                hasMore = true,
                hasVisibleContent = false
            )
        )
    }

    @Test
    fun populatedPageRequestsLoadMoreNearEnd() {
        assertTrue(
            shouldRequestHomeCategoryLoadMore(
                totalItems = 12,
                lastVisibleItemIndex = 9,
                isLoading = false,
                hasMore = true,
                hasVisibleContent = true
            )
        )
    }
}
