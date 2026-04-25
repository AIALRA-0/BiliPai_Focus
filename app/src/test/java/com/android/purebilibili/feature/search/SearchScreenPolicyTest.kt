package com.android.purebilibili.feature.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchScreenPolicyTest {

    @Test
    fun `hot header stays hidden when hot search toggle is off`() {
        assertFalse(shouldShowSearchHotHeader(hotItemCount = 12, hotSearchEnabled = false))
        assertFalse(shouldShowSearchHotSection(hotItemCount = 12, hotSearchEnabled = false))
    }

    @Test
    fun `hot header and body appear only when enabled and data exists`() {
        assertTrue(shouldShowSearchHotHeader(hotItemCount = 1, hotSearchEnabled = true))
        assertTrue(shouldShowSearchHotSection(hotItemCount = 1, hotSearchEnabled = true))
        assertFalse(shouldShowSearchHotHeader(hotItemCount = 0, hotSearchEnabled = true))
    }

    @Test
    fun resetSearchScroll_onlyWhenShowingNonBlankResults() {
        assertTrue(
            shouldResetSearchResultScroll(
                searchSessionId = 1L,
                showResults = true,
                lastResetSessionId = 0L
            )
        )
        assertFalse(
            shouldResetSearchResultScroll(
                searchSessionId = 0L,
                showResults = true,
                lastResetSessionId = 0L
            )
        )
        assertFalse(
            shouldResetSearchResultScroll(
                searchSessionId = 2L,
                showResults = false,
                lastResetSessionId = 1L
            )
        )
    }

    @Test
    fun submitKeyword_prefersTypedQuery_thenFallsBackToSuggestedKeyword() {
        assertEquals(
            "黑神话悟空",
            resolveSearchSubmitKeyword(
                query = "  黑神话悟空 ",
                suggestedKeyword = "睡羊妹妹m"
            )
        )
        assertEquals(
            "睡羊妹妹m",
            resolveSearchSubmitKeyword(
                query = " ",
                suggestedKeyword = " 睡羊妹妹m "
            )
        )
        assertEquals(
            "",
            resolveSearchSubmitKeyword(
                query = "",
                suggestedKeyword = " "
            )
        )
    }

    @Test
    fun activeSearchResultCount_followsCurrentSearchType() {
        val state = SearchUiState(
            searchType = com.android.purebilibili.data.model.response.SearchType.LIVE,
            searchResults = List(5) { com.android.purebilibili.data.model.response.VideoItem() },
            upResults = List(4) { com.android.purebilibili.data.model.response.SearchUpItem() },
            bangumiResults = List(3) { com.android.purebilibili.data.model.response.BangumiSearchItem() },
            liveResults = List(2) { com.android.purebilibili.data.model.response.LiveRoomSearchItem() }
        )

        assertEquals(2, resolveSearchActiveResultCount(state))
    }

    @Test
    fun searchResultPagination_usesSharedTailTriggerRules() {
        assertTrue(
            shouldLoadMoreSearchResults(
                totalItems = 12,
                lastVisibleItemIndex = 11,
                resultItemCount = 10,
                isLoadingMore = false,
                hasMoreResults = true
            )
        )
        assertFalse(
            shouldLoadMoreSearchResults(
                totalItems = 12,
                lastVisibleItemIndex = 11,
                resultItemCount = 10,
                isLoadingMore = true,
                hasMoreResults = true
            )
        )
    }

    @Test
    fun autoFocus_onlyRequestsOnceForBlankUnfocusedSearchField() {
        assertTrue(
            shouldRequestSearchAutoFocus(
                autoFocusEnabled = true,
                query = "",
                isFocused = false,
                autoFocusConsumed = false
            )
        )
        assertFalse(
            shouldRequestSearchAutoFocus(
                autoFocusEnabled = true,
                query = "测试",
                isFocused = false,
                autoFocusConsumed = false
            )
        )
        assertFalse(
            shouldRequestSearchAutoFocus(
                autoFocusEnabled = true,
                query = "",
                isFocused = true,
                autoFocusConsumed = false
            )
        )
        assertFalse(
            shouldRequestSearchAutoFocus(
                autoFocusEnabled = true,
                query = "",
                isFocused = false,
                autoFocusConsumed = true
            )
        )
    }
}
