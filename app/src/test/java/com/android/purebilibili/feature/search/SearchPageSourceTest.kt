package com.android.purebilibili.feature.search

import androidx.paging.PagingSource
import com.android.purebilibili.data.repository.SearchRepository.SearchPageInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*

class SearchPageSourceTest {
    private fun params(page: Int?) = PagingSource.LoadParams.Refresh(page, 1, false)

    @Test fun serverMetadataControlsNextKeyEvenWhenFiltersRemoveAllVisibleItems() = runTest {
        val requested = mutableListOf<Int>()
        val source = SearchPageSource { number ->
            requested += number
            SearchPageChunk(SearchResultPageUiState(), SearchPageInfo(number, 2, 30, number < 2))
        }
        val first = source.load(params(null)) as PagingSource.LoadResult.Page<Int, SearchPageChunk>
        assertEquals(2, first.nextKey)
        assertNull(first.prevKey)
        val last = source.load(params(2)) as PagingSource.LoadResult.Page<Int, SearchPageChunk>
        assertNull(last.nextKey)
        assertEquals(listOf(1, 2), requested)
    }

    @Test fun loadFailureCanRetryTheSameServerPage() = runTest {
        var attempt = 0
        val source = SearchPageSource { number ->
            if (attempt++ == 0) throw IllegalStateException("暂时失败")
            SearchPageChunk(SearchResultPageUiState(), SearchPageInfo(number, 3, 30, true))
        }
        assertTrue(source.load(params(2)) is PagingSource.LoadResult.Error)
        val retried = source.load(params(2)) as PagingSource.LoadResult.Page<Int, SearchPageChunk>
        assertEquals(2, retried.data.single().info.currentPage)
    }

    @Test fun cancellationIsNotConvertedToARetryableError() = runTest {
        val source = SearchPageSource { throw CancellationException("搜索已替换") }
        try {
            source.load(params(null))
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
    }
}
