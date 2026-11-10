package com.android.purebilibili.feature.dynamic

import androidx.paging.PagingSource
import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.repository.DynamicFeedFetchResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class DynamicPagingSourceTest {
    private fun params(offset: String? = null) = PagingSource.LoadParams.Refresh(offset, 1, false)

    @Test fun incrementalRefreshContinuesFromExistingTailAndOnlyFirstPageUsesBaseline() = runTest {
        val requests = mutableListOf<Pair<String, String>>()
        val source = dynamicPagingSource(
            updateBaseline = "baseline",
            previousTail = DynamicFeedFetchResult(emptyList(), nextOffset = "old-tail", hasMore = true),
            preserveTail = { true },
            loadPage = { offset, baseline ->
                requests += offset to baseline
                DynamicFeedFetchResult(listOf(DynamicItem(id_str = "new")), nextOffset = "fresh-tail", hasMore = true)
            }
        )
        val first = source.load(params()) as PagingSource.LoadResult.Page<String, DynamicPageChunk>
        assertEquals("old-tail", first.nextKey)
        source.load(params(first.nextKey))
        assertEquals(listOf("" to "baseline", "old-tail" to ""), requests)
    }

    @Test fun replacementRefreshUsesItsOwnCursorAndRepeatedCursorEndsPagination() = runTest {
        val source = dynamicPagingSource(
            previousTail = DynamicFeedFetchResult(emptyList(), nextOffset = "old-tail"),
            loadPage = { _, _ -> DynamicFeedFetchResult(emptyList(), nextOffset = "new-tail", hasMore = true) }
        )
        val first = source.load(params()) as PagingSource.LoadResult.Page<String, DynamicPageChunk>
        assertEquals("new-tail", first.nextKey)
        val last = source.load(params("new-tail")) as PagingSource.LoadResult.Page<String, DynamicPageChunk>
        assertNull(last.nextKey)
        assertFalse(last.data.single().page.hasMore)
    }

    @Test fun failedAppendRetriesTheCursorAndCancellationPropagates() = runTest {
        var attempts = 0
        val source = dynamicPagingSource { offset, _ ->
            if (offset == "next" && attempts++ == 0) error("暂时失败")
            DynamicFeedFetchResult(emptyList(), nextOffset = "next", hasMore = true)
        }
        source.load(params())
        assertTrue(source.load(params("next")) is PagingSource.LoadResult.Error)
        assertTrue(source.load(params("next")) is PagingSource.LoadResult.Page)
        val cancelled = dynamicPagingSource { _, _ -> throw CancellationException("作者已切换") }
        try {
            cancelled.load(params())
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
    }

    @Test fun responseKeepsInteractionsMadeDuringRequestAndDoesNotResurrectDeletedItems() {
        val before = DynamicItem(id_str = "1")
        val edited = before.copy(visible = false)
        val deleted = DynamicItem(id_str = "2")
        val newItem = DynamicItem(id_str = "3")
        assertEquals(
            listOf(edited, newItem),
            reconcileDynamicPageItems(listOf(before, deleted, newItem), listOf(before, deleted), listOf(edited), setOf("2"))
        )
    }
}
