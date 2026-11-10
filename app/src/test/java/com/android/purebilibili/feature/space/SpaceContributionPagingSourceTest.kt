package com.android.purebilibili.feature.space

import androidx.paging.PagingSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SpaceContributionPagingSourceTest {
    private fun params(page: Int?) = PagingSource.LoadParams.Refresh(page, 1, false)

    @Test fun duplicateOnlyPageStopsEvenWhenServerClaimsMoreContent() = runTest {
        val source = spaceContributionPagingSource<Long>(itemKey = { it }) { number ->
            SpaceContributionPage(number, listOf(1L, 2L), 100, hasMore = true)
        }
        val first = source.load(params(null)) as PagingSource.LoadResult.Page<Int, SpaceContributionPage<Long>>
        assertEquals(2, first.nextKey)
        val last = source.load(params(2)) as PagingSource.LoadResult.Page<Int, SpaceContributionPage<Long>>
        assertTrue(last.data.single().items.isEmpty())
        assertNull(last.nextKey)
    }

    @Test fun failedAppendCanRetrySamePageWithoutLosingNewItemsOrMetadata() = runTest {
        var fail = true
        val requests = mutableListOf<Int>()
        val source = spaceContributionPagingSource<Long>(itemKey = { it }) { number ->
            requests += number
            if (number == 2 && fail) { fail = false; error("暂时失败") }
            SpaceContributionPage(number, if (number == 1) listOf(1L, 2L) else listOf(2L, 3L), 3, number == 1, "cursor-$number")
        }
        source.load(params(null))
        assertTrue(source.load(params(2)) is PagingSource.LoadResult.Error)
        val retried = source.load(params(2)) as PagingSource.LoadResult.Page<Int, SpaceContributionPage<Long>>
        assertEquals(listOf(3L), retried.data.single().items)
        assertEquals("cursor-2", retried.data.single().offset)
        assertNull(retried.nextKey)
        assertEquals(listOf(1, 2, 2), requests)
    }

    @Test fun cancellationIsNotTurnedIntoAnOrdinaryLoadFailure() = runTest {
        val source = spaceContributionPagingSource<Long>(itemKey = { it }) { throw CancellationException("空间已切换") }
        try {
            source.load(params(null))
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
    }
}
