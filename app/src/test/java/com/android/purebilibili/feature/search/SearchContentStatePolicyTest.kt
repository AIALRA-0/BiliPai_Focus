package com.android.purebilibili.feature.search

import com.android.purebilibili.data.model.response.SearchType
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.data.repository.shouldApplySearchResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchContentStatePolicyTest {

    @Test
    fun `draft editing cannot change cached result ownership or invalidate active response`() {
        val page = SearchResultPageUiState(query = "QQ", currentPage = 3)
        val submitted = SearchUiState(
            query = "QQ", showResults = true, searchSessionId = 7L,
            resultPages = mapOf(SearchType.UP to page),
        )
        for (draft in listOf("", " ", "新关键词")) {
            val edited = submitted.withEditedSearchQuery(draft)
            assertEquals("QQ", edited.toCurrentSearchResultPage().query)
            assertEquals(page, resolveSearchResultPageState(edited, SearchType.UP))
            assertTrue(shouldApplySearchResult(
                7L, edited.searchSessionId, "QQ", edited.submittedQuery,
                SearchType.ALL, edited.searchType,
            ))
            assertFalse(shouldApplySearchResult(
                6L, edited.searchSessionId, "QQ", edited.submittedQuery,
                SearchType.ALL, edited.searchType,
            ))
            val restored = edited.withCurrentSearchResultPageCached()
                .withSearchResultPageMirrored(SearchType.UP, page)
            assertEquals(draft, restored.query)
            assertEquals("QQ", restored.submittedQuery)
        }
    }

    @Test
    fun `search recording never overrides global privacy`() {
        assertTrue(shouldRecordSearchHistory(true, false))
        assertFalse(shouldRecordSearchHistory(false, false))
        assertFalse(shouldRecordSearchHistory(true, true))
        assertFalse(shouldRecordSearchHistory(false, true))
    }

    @Test
    fun `clearing results query preserves the page session rows cache and errors`() {
        val rows = listOf(VideoItem(bvid = "BV1QQ", title = "QQ"))
        val cachedPage = SearchResultPageUiState(query = "QQ", currentPage = 1, searchResults = rows)
        val original = SearchUiState(
            query = "QQ",
            showResults = true,
            searchSessionId = 12L,
            currentPage = 3,
            searchResults = rows,
            error = "保留当前结果状态",
            loadMoreError = "保留翻页重试入口",
            resultPages = mapOf(SearchType.ALL to cachedPage),
        )
        for (query in listOf("", " ", "新搜索词")) {
            val edited = original.withEditedSearchQuery(query)
            assertEquals(original.copy(query = query), edited)
        }
    }

    @Test
    fun `clearing draft preserves the submitted request and stays on results`() {
        val original = SearchUiState(
            query = "QQ", showResults = true, isSearching = true, isLoadingMore = true,
            suggestions = listOf(SearchSuggestionUiModel("QQ")),
        )
        val cleared = original.withEditedSearchQuery("")
        assertTrue(cleared.showResults)
        assertTrue(cleared.isSearching)
        assertTrue(cleared.isLoadingMore)
        assertEquals("QQ", cleared.submittedQuery)
        assertEquals("", cleared.query)
        assertTrue(cleared.suggestions.isEmpty())
        assertFalse(SearchUiState(query = "QQ").withEditedSearchQuery("").showResults)
    }

    @Test
    fun `video pagination can fill an empty filtered page or a one or two item grid`() {
        assertTrue(shouldLoadMoreSearchVideos(0, -1, true, false, false, false))
        assertTrue(shouldLoadMoreSearchVideos(1, 0, true, false, false, false))
        assertTrue(shouldLoadMoreSearchVideos(2, 1, true, false, false, false))
    }

    @Test
    fun `video pagination uses visible tail instead of exact card index`() {
        assertFalse(shouldLoadMoreSearchVideos(20, 8, true, false, false, false))
        assertTrue(shouldLoadMoreSearchVideos(20, 19, true, false, false, false))
        assertFalse(shouldLoadMoreSearchVideos(2, -1, true, false, false, false))
    }

    @Test
    fun `video pagination waits during sorting loading errors and end of results`() {
        assertFalse(shouldLoadMoreSearchVideos(0, -1, true, true, false, false))
        assertFalse(shouldLoadMoreSearchVideos(0, -1, true, false, true, false))
        assertFalse(shouldLoadMoreSearchVideos(0, -1, true, false, false, true))
        assertFalse(shouldLoadMoreSearchVideos(0, -1, false, false, false, false))
    }

    @Test
    fun `initial search shows loading before any result exists`() {
        assertEquals(
            SearchResultPresentation(
                body = SearchResultBodyMode.LOADING,
                footer = SearchResultFooterMode.NONE,
            ),
            resolveSearchResultPresentation(
                itemCount = 0,
                isSearching = true,
                error = null,
                emptyStateReason = SearchEmptyStateReason.NONE,
                isLoadingMore = false,
                loadMoreError = null,
                hasMoreResults = false,
            )
        )
    }

    @Test
    fun `blocking failure wins over empty state`() {
        assertEquals(
            SearchResultBodyMode.ERROR,
            resolveSearchResultPresentation(
                itemCount = 0,
                isSearching = false,
                error = "network",
                emptyStateReason = SearchEmptyStateReason.NO_RESULTS,
                isLoadingMore = false,
                loadMoreError = null,
                hasMoreResults = false,
            ).body
        )
    }

    @Test
    fun `empty result renders empty state`() {
        assertEquals(
            SearchResultBodyMode.EMPTY,
            resolveSearchResultPresentation(
                itemCount = 0,
                isSearching = false,
                error = null,
                emptyStateReason = SearchEmptyStateReason.NO_RESULTS,
                isLoadingMore = false,
                loadMoreError = null,
                hasMoreResults = false,
            ).body
        )
    }

    @Test
    fun `pagination failure stays at the content footer`() {
        assertEquals(
            SearchResultFooterMode.ERROR,
            resolveSearchResultPresentation(
                itemCount = 12,
                isSearching = false,
                error = null,
                emptyStateReason = SearchEmptyStateReason.NONE,
                isLoadingMore = false,
                loadMoreError = "page two failed",
                hasMoreResults = true,
            ).footer
        )
    }

    @Test
    fun `pagination end is only shown after content`() {
        assertEquals(
            SearchResultFooterMode.END,
            resolveSearchResultPresentation(
                itemCount = 12,
                isSearching = false,
                error = null,
                emptyStateReason = SearchEmptyStateReason.NONE,
                isLoadingMore = false,
                loadMoreError = null,
                hasMoreResults = false,
            ).footer
        )
    }

    @Test
    fun `landing failure is local when old data exists`() {
        assertEquals(
            SearchLandingSectionMode.CONTENT,
            resolveSearchLandingSectionMode(
                enabled = true,
                itemCount = 5,
                isRefreshing = false,
                error = "network",
            )
        )
    }

    @Test
    fun `disabled landing section is hidden`() {
        assertEquals(
            SearchLandingSectionMode.HIDDEN,
            resolveSearchLandingSectionMode(
                enabled = false,
                itemCount = 0,
                isRefreshing = false,
                error = null,
            )
        )
    }

    @Test
    fun `load more failure preserves existing results and retry metadata`() {
        val original = SearchUiState(
            query = "kotlin",
            searchResults = listOf(),
            currentPage = 3,
            totalPages = 8,
            hasMoreResults = true,
            isLoadingMore = true,
        )

        val failed = original.withLoadMoreFailure("network")

        assertEquals(original.searchResults, failed.searchResults)
        assertEquals(original.currentPage, failed.currentPage)
        assertEquals(original.totalPages, failed.totalPages)
        assertTrue(failed.hasMoreResults)
        assertFalse(failed.isLoadingMore)
        assertEquals("network", failed.loadMoreError)
    }

    @Test
    fun `starting load more clears only the previous pagination error`() {
        val started = SearchUiState(
            query = "kotlin",
            error = "blocking error",
            loadMoreError = "old page error",
            isLoadingMore = false,
        ).withLoadMoreStarted()

        assertTrue(started.isLoadingMore)
        assertEquals(null, started.loadMoreError)
        assertEquals("blocking error", started.error)
    }
}
