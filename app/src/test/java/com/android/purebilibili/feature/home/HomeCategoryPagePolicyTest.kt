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

    @Test
    fun shouldLoadMoreHomeCategoryContent_blocksFollowAutoPagingUntilUserHasScrolled() {
        assertFalse(
            shouldLoadMoreHomeCategoryContent(
                totalItems = 16,
                lastVisibleItemIndex = 15,
                contentItemCount = 16,
                isLoading = false,
                hasMore = true,
                requireUserScrollObservation = true,
                userScrollObserved = false
            )
        )

        assertTrue(
            shouldLoadMoreHomeCategoryContent(
                totalItems = 16,
                lastVisibleItemIndex = 15,
                contentItemCount = 16,
                isLoading = false,
                hasMore = true,
                requireUserScrollObservation = true,
                userScrollObserved = true
            )
        )
    }

    @Test
    fun hasObservedFollowLoadMoreScroll_requiresContentToMoveAwayFromTop() {
        assertFalse(
            hasObservedFollowLoadMoreScroll(
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 0
            )
        )
        assertTrue(
            hasObservedFollowLoadMoreScroll(
                firstVisibleItemIndex = 0,
                firstVisibleItemScrollOffset = 8
            )
        )
        assertTrue(
            hasObservedFollowLoadMoreScroll(
                firstVisibleItemIndex = 1,
                firstVisibleItemScrollOffset = 0
            )
        )
    }
}
