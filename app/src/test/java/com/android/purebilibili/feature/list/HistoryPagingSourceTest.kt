package com.android.purebilibili.feature.list

import androidx.paging.PagingSource
import com.android.purebilibili.data.model.response.HistoryCursor
import com.android.purebilibili.data.model.response.HistoryData
import com.android.purebilibili.data.model.response.HistoryPage
import com.android.purebilibili.data.repository.HistoryResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class HistoryPagingSourceTest {
    private fun entry(id: Long, viewedAt: Long = 100) = HistoryData(
        history = HistoryPage(bvid = "BV$id", oid = id, business = "archive"), view_at = viewedAt
    )
    private fun params(key: HistoryPageKey? = null) = PagingSource.LoadParams.Refresh(key, 1, false)

    @Test fun forwardsCompleteCursorAndStopsWhenItDoesNotAdvance() = runTest {
        val requested = mutableListOf<HistoryPageKey>()
        val source = HistoryPagingSource(searching = false) { key ->
            requested += key
            HistoryResult(listOf(entry(1)), HistoryCursor(max = 9, view_at = 100, business = "archive"))
        }
        val first = source.load(params()) as PagingSource.LoadResult.Page<HistoryPageKey, HistoryPageChunk>
        assertEquals(HistoryPageKey(max = 9, viewAt = 100, business = "archive"), first.nextKey)
        val last = source.load(params(first.nextKey)) as PagingSource.LoadResult.Page<HistoryPageKey, HistoryPageChunk>
        assertNull(last.nextKey)
        assertEquals(listOf(HistoryPageKey(), first.nextKey), requested)
    }

    @Test fun historySearchUsesPageNumbersInsteadOfCursor() = runTest {
        val source = HistoryPagingSource(searching = true) { key ->
            HistoryResult(if (key.page == 1) List(20) { entry(it.toLong()) } else emptyList(), null)
        }
        val first = source.load(params()) as PagingSource.LoadResult.Page<HistoryPageKey, HistoryPageChunk>
        assertEquals(HistoryPageKey(page = 2), first.nextKey)
        assertNull((source.load(params(first.nextKey)) as PagingSource.LoadResult.Page<HistoryPageKey, HistoryPageChunk>).nextKey)
    }

    @Test fun partialDeleteFailureRestoresOnlyFailedRowsAndKeepsNewPages() {
        val one = entry(1).toHistoryItem()
        val two = entry(2).toHistoryItem()
        val three = entry(3).toHistoryItem()
        val edits = HistoryPresentationEdits()
        val batch = edits.hide(listOf(one, two))
        assertEquals(listOf(three), edits.visible(listOf(one, two, three)))
        edits.restore(batch, succeeded = setOf("BV1"))
        assertEquals(listOf(two, three), edits.visible(listOf(one, two, three)))
    }

    @Test fun rewatchIsVisibleAndAnOlderRollbackDoesNotUndoANewerDelete() {
        val old = entry(1).toHistoryItem()
        val newer = entry(1, viewedAt = 200).toHistoryItem()
        val edits = HistoryPresentationEdits()
        val first = edits.hide(listOf(old))
        assertEquals(listOf(newer), edits.visible(listOf(old, newer)))
        edits.hide(listOf(newer))
        edits.restore(first)
        assertTrue(edits.visible(listOf(old, newer)).isEmpty())
    }

    @Test fun cancellationDoesNotBecomeRetryableHistoryFailure() = runTest {
        val source = HistoryPagingSource(searching = false) { throw CancellationException("账号已切换") }
        try {
            source.load(params())
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
    }
}
