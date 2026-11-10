package com.android.purebilibili.feature.list

import androidx.paging.PagingSource
import com.android.purebilibili.data.model.response.FavFolder
import com.android.purebilibili.data.repository.FavoriteRepository.CollectedFavFoldersPage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class FavoritePagingSourcesTest {
    private fun params(page: Int?) = PagingSource.LoadParams.Refresh(page, 1, false)

    @Test fun subscriptionPagesUseServerRowsForTotalsAndDeduplicateByFolderIdentity() = runTest {
        val source = subscribedFolderPagingSource(pageSize = 2) { number ->
            CollectedFavFoldersPage(
                if (number == 1) listOf(FavFolder(id = 1, type = 11), FavFolder(id = 1, type = 21))
                else listOf(FavFolder(id = 1, type = 21), FavFolder(id = 2, type = 11)),
                totalCount = 3
            )
        }
        val first = source.load(params(null)) as PagingSource.LoadResult.Page<Int, SubscribedFolderPageChunk>
        assertEquals(2, first.data.single().folders.size)
        assertEquals(2, first.nextKey)
        val last = source.load(params(2)) as PagingSource.LoadResult.Page<Int, SubscribedFolderPageChunk>
        assertEquals(listOf(2L), last.data.single().folders.map { it.id })
        assertNull(last.nextKey)
    }

    @Test fun duplicateOnlyPageTerminatesEvenWithStaleServerTotal() = runTest {
        val source = subscribedFolderPagingSource(1) { CollectedFavFoldersPage(listOf(FavFolder(id = 1)), 99) }
        source.load(params(null))
        val duplicate = source.load(params(2)) as PagingSource.LoadResult.Page<Int, SubscribedFolderPageChunk>
        assertTrue(duplicate.data.single().folders.isEmpty())
        assertNull(duplicate.nextKey)
    }

    @Test fun retryKeepsTheSamePageAndDoesNotCountAFailedResponse() = runTest {
        var attempts = 0
        val source = subscribedFolderPagingSource(1) { number ->
            if (number == 2 && attempts++ == 0) error("暂时失败")
            CollectedFavFoldersPage(listOf(FavFolder(id = number.toLong())), 3)
        }
        source.load(params(null))
        assertTrue(source.load(params(2)) is PagingSource.LoadResult.Error)
        val retried = source.load(params(2)) as PagingSource.LoadResult.Page<Int, SubscribedFolderPageChunk>
        assertEquals(3, retried.nextKey)
        assertEquals(2L, retried.data.single().folders.single().id)
    }
}
