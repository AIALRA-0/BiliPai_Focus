package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.SearchType
import com.android.purebilibili.data.model.response.VideoItem
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchLoadPolicyTest {

    @Test
    fun `video pagination uses result total when page total is absent`() {
        val first = resolveVideoSearchPageInfo(1, 1, 0, 41, 20, 20)
        assertEquals(3, first.totalPages)
        assertTrue(first.hasMore)
        assertTrue(resolveVideoSearchPageInfo(3, 3, 0, 41, 20, 1).hasMore)
    }

    @Test
    fun `both full and short video pages without totals can continue`() {
        assertTrue(resolveVideoSearchPageInfo(2, 1, 0, 0, 20, 20).hasMore)
        assertEquals(2, resolveVideoSearchPageInfo(2, 1, 0, 0, 20, 20).currentPage)
        assertTrue(resolveVideoSearchPageInfo(2, 2, 0, 0, 20, 2).hasMore)
    }

    @Test
    fun `short nonempty video page respects explicit remaining pages`() {
        assertTrue(resolveVideoSearchPageInfo(1, 1, 4, 80, 20, 2).hasMore)
    }

    @Test
    fun `thirteen sorted results probe next page like PiliPlus before ending`() {
        val first = resolveVideoSearchPageInfo(1, 1, 1, 13, 20, 13)
        assertTrue(first.hasMore)
        assertEquals(13, first.totalResults)
        val next = resolveVideoSearchPageInfo(2, 2, 1, 13, 20, 0)
        assertFalse(next.hasMore)
        assertEquals(2, next.currentPage)
    }

    @Test
    fun `empty server page stops even when pagination metadata claims more`() {
        assertFalse(resolveVideoSearchPageInfo(2, 2, 9, 180, 20, 0).hasMore)
    }

    @Test
    fun `resolveSearchLoadedPage never regresses below requested page`() {
        assertEquals(3, resolveSearchLoadedPage(requestedPage = 3, responsePage = 1))
        assertEquals(2, resolveSearchLoadedPage(requestedPage = 2, responsePage = 2))
    }

    @Test
    fun `shouldApplySearchResult requires matching session query and type`() {
        assertTrue(
            shouldApplySearchResult(
                requestSessionId = 4L,
                activeSessionId = 4L,
                requestQuery = "测试",
                activeQuery = "测试",
                requestType = SearchType.VIDEO,
                activeType = SearchType.VIDEO
            )
        )
        assertFalse(
            shouldApplySearchResult(
                requestSessionId = 4L,
                activeSessionId = 5L,
                requestQuery = "测试",
                activeQuery = "测试",
                requestType = SearchType.VIDEO,
                activeType = SearchType.VIDEO
            )
        )
        assertFalse(
            shouldApplySearchResult(
                requestSessionId = 4L,
                activeSessionId = 4L,
                requestQuery = "测试",
                activeQuery = "别的",
                requestType = SearchType.VIDEO,
                activeType = SearchType.VIDEO
            )
        )
    }

    @Test
    fun `mergeSearchPageResults preserves existing order and removes duplicates`() {
        val merged = mergeSearchPageResults(
            existing = listOf("BV1", "BV2"),
            incoming = listOf("BV2", "BV3", "BV1", "BV4"),
            keySelector = { it }
        )

        assertEquals(listOf("BV1", "BV2", "BV3", "BV4"), merged)
    }

    @Test
    fun `search duration selection treats empty and all as all only`() {
        assertContentEquals(
            listOf(SearchDuration.ALL),
            resolveSearchDurationRequests(emptySet())
        )
        assertContentEquals(
            listOf(SearchDuration.ALL),
            resolveSearchDurationRequests(setOf(SearchDuration.ALL, SearchDuration.UNDER_10MIN))
        )
    }

    @Test
    fun `search duration toggle supports multi selection and all reset`() {
        val first = toggleSearchDurationSelection(emptySet(), SearchDuration.UNDER_10MIN)
        val second = toggleSearchDurationSelection(first, SearchDuration.TEN_TO_30MIN)
        val reset = toggleSearchDurationSelection(second, SearchDuration.ALL)

        assertEquals(setOf(SearchDuration.UNDER_10MIN), first)
        assertEquals(setOf(SearchDuration.UNDER_10MIN, SearchDuration.TEN_TO_30MIN), second)
        assertEquals(emptySet(), reset)
    }

    @Test
    fun `mergeSearchDurationResultPages dedupes by bvid and combines page state`() {
        val firstPageInfo = SearchRepository.SearchPageInfo(
            currentPage = 2,
            totalPages = 5,
            totalResults = 20,
            hasMore = true
        )
        val secondPageInfo = SearchRepository.SearchPageInfo(
            currentPage = 2,
            totalPages = 3,
            totalResults = 8,
            hasMore = false
        )

        val (videos, pageInfo) = mergeSearchDurationResultPages(
            listOf(
                listOf(VideoItem(bvid = "BV1"), VideoItem(bvid = "BV2")) to firstPageInfo,
                listOf(VideoItem(bvid = "BV2"), VideoItem(bvid = "BV3")) to secondPageInfo
            )
        )

        assertEquals(listOf("BV1", "BV2", "BV3"), videos.map { it.bvid })
        assertEquals(2, pageInfo.currentPage)
        assertEquals(5, pageInfo.totalPages)
        assertEquals(28, pageInfo.totalResults)
        assertTrue(pageInfo.hasMore)
    }
}
