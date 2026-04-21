package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.SearchType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchLoadPolicyTest {

    @Test
    fun `guest first page video search retries fallback when primary result is empty`() {
        assertTrue(
            shouldFallbackGuestVideoSearch(
                isLoggedIn = false,
                page = 1,
                primaryResultCount = 0
            )
        )
    }

    @Test
    fun `logged in users keep primary result even when empty`() {
        assertFalse(
            shouldFallbackGuestVideoSearch(
                isLoggedIn = true,
                page = 1,
                primaryResultCount = 0
            )
        )
    }

    @Test
    fun `guest later pages do not trigger fallback on empty result`() {
        assertFalse(
            shouldFallbackGuestVideoSearch(
                isLoggedIn = false,
                page = 2,
                primaryResultCount = 0
            )
        )
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
}
