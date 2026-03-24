package com.android.purebilibili.feature.home

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeCategoryPagePolicyTest {

    @Test
    fun shouldLoadMoreHomeCategoryContent_blocksAutoPagingWhenNoPrimaryContentExists() {
        assertFalse(
            shouldLoadMoreHomeCategoryContent(
                totalItems = 1,
                lastVisibleItemIndex = 0,
                contentItemCount = 0,
                isLoading = false,
                hasMore = true
            )
        )
    }

    @Test
    fun shouldLoadMoreHomeCategoryContent_allowsAutoPagingNearTailWhenVideosExist() {
        assertTrue(
            shouldLoadMoreHomeCategoryContent(
                totalItems = 8,
                lastVisibleItemIndex = 7,
                contentItemCount = 4,
                isLoading = false,
                hasMore = true
            )
        )
    }

    @Test
    fun shouldLoadMoreHomeCategoryContent_blocksAutoPagingWhenAutoLoadMoreIsSuppressed() {
        assertFalse(
            shouldLoadMoreHomeCategoryContent(
                totalItems = 8,
                lastVisibleItemIndex = 7,
                contentItemCount = 4,
                isLoading = false,
                hasMore = true,
                autoLoadMoreEnabled = false
            )
        )
    }
}
