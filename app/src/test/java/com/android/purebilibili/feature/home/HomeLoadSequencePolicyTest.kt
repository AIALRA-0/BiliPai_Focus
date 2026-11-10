package com.android.purebilibili.feature.home

import com.android.purebilibili.data.model.response.VideoItem
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HomeLoadSequencePolicyTest {

    @Test
    fun refreshesHomeUserInfoOnlyForNonPaginationLoads() {
        assertTrue(shouldRefreshHomeUserInfoAfterFeedLoad(isLoadMore = false))
        assertFalse(shouldRefreshHomeUserInfoAfterFeedLoad(isLoadMore = true))
    }

    @Test
    fun paginationFailureKeepsContentAndNextPageAvailableForManualRetry() {
        val current = CategoryContent(
            videos = persistentListOf(VideoItem(bvid = "BV_existing")),
            pageIndex = 4,
            hasMore = true,
            isLoading = true,
        )
        val failed = applyHomeFeedLoadFailure(current, isLoadMore = true, message = "网络断开")

        assertEquals(current.videos, failed.videos)
        assertEquals(4, failed.pageIndex)
        assertTrue(failed.hasMore)
        assertFalse(failed.isLoading)
        assertEquals("网络断开", failed.loadMoreError)
        assertNull(failed.error)
    }

    @Test
    fun failedRefreshRetainsContentAndShowsRefreshFailure() {
        val current = CategoryContent(
            videos = persistentListOf(VideoItem(bvid = "BV_existing")),
            pageIndex = 3,
            hasMore = false,
        )
        val failed = applyHomeFeedLoadFailure(current, isLoadMore = false, message = "请求超时")

        assertEquals(current.videos, failed.videos)
        assertEquals(3, failed.pageIndex)
        assertFalse(failed.hasMore)
        assertEquals("请求超时", failed.refreshError)
        assertNull(failed.error)
        assertNull(failed.loadMoreError)
    }

    @Test
    fun firstPageFailureUsesFullPageError() {
        val failed = applyHomeFeedLoadFailure(CategoryContent(), isLoadMore = false, message = "网络断开")

        assertEquals("网络断开", failed.error)
        assertNull(failed.refreshError)
        assertNull(failed.loadMoreError)
    }
}
