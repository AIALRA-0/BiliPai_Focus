package com.android.purebilibili.feature.dynamic

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicScreenStatePolicyTest {

    @Test
    fun shouldLoadMoreDynamicFeed_blocksAutoPagingWhenNoVisibleItemsRemain() {
        assertFalse(
            shouldLoadMoreDynamicFeed(
                totalItems = 1,
                lastVisibleItemIndex = 0,
                visibleItemCount = 0,
                isLoading = false,
                hasMore = true
            )
        )
    }

    @Test
    fun shouldLoadMoreDynamicFeed_allowsAutoPagingNearTailWhenVisibleItemsExist() {
        assertTrue(
            shouldLoadMoreDynamicFeed(
                totalItems = 6,
                lastVisibleItemIndex = 5,
                visibleItemCount = 3,
                isLoading = false,
                hasMore = true
            )
        )
    }
}
