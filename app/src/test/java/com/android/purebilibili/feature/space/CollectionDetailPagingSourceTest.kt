package com.android.purebilibili.feature.space

import androidx.paging.PagingSource
import com.android.purebilibili.data.model.response.FavoriteInfo
import com.android.purebilibili.data.model.response.VideoItem
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class CollectionDetailPagingSourceTest {
    private fun params(page: Int? = null) = PagingSource.LoadParams.Refresh(CollectionDetailPageKey(page), 1, false)
    private fun video(id: Int) = VideoItem(id = id.toLong(), bvid = "BV$id")

    @Test fun oldestFirstTraversesFromLastPageAndDoesNotRestartAtServerPageOne() = runTest {
        val requested = mutableListOf<Int>()
        val source = collectionDetailPagingSource(ordinaryFavorite = true, oldestFirst = true) { number ->
            requested += number
            CollectionDetailServerPage(
                items = when (number) { 1 -> (41 downTo 22).map(::video); 2 -> (21 downTo 2).map(::video); else -> listOf(video(1)) },
                hasMore = number < 3,
                info = FavoriteInfo(media_count = 41)
            )
        }
        val first = source.load(params()) as PagingSource.LoadResult.Page<CollectionDetailPageKey, CollectionDetailPageChunk>
        assertEquals(listOf(1L), first.data.single().page.items.map { it.id })
        assertEquals(CollectionDetailPageKey(2), first.nextKey)
        val second = source.load(params(2)) as PagingSource.LoadResult.Page<CollectionDetailPageKey, CollectionDetailPageChunk>
        assertEquals(CollectionDetailPageKey(1), second.nextKey)
        val last = source.load(params(1)) as PagingSource.LoadResult.Page<CollectionDetailPageKey, CollectionDetailPageChunk>
        assertEquals((22L..41L).toList(), last.data.single().page.items.map { it.id })
        assertNull(last.nextKey)
        assertEquals(listOf(1, 3, 2, 1), requested)
    }

    @Test fun failedOldestFirstInitialPageCanRetryWithFreshMetadata() = runTest {
        var fail = true
        val source = collectionDetailPagingSource(ordinaryFavorite = true, oldestFirst = true) { number ->
            if (number == 3 && fail) { fail = false; error("末页失败") }
            CollectionDetailServerPage(listOf(video(number)), hasMore = true, info = FavoriteInfo(media_count = 41))
        }
        assertTrue(source.load(params()) is PagingSource.LoadResult.Error)
        val retried = source.load(params()) as PagingSource.LoadResult.Page<CollectionDetailPageKey, CollectionDetailPageChunk>
        assertEquals(listOf(3L), retried.data.single().page.items.map { it.id })
        assertEquals(41, retried.data.single().page.info?.media_count)
        assertEquals(CollectionDetailPageKey(2), retried.nextKey)
    }

    @Test fun subscribedSeasonUsesServerEndFlagAndDuplicateOnlyPageStops() = runTest {
        val source = collectionDetailPagingSource(ordinaryFavorite = false, oldestFirst = false) {
            CollectionDetailServerPage(listOf(video(1)), hasMore = true)
        }
        val first = source.load(params()) as PagingSource.LoadResult.Page<CollectionDetailPageKey, CollectionDetailPageChunk>
        assertEquals(CollectionDetailPageKey(2), first.nextKey)
        val duplicate = source.load(params(2)) as PagingSource.LoadResult.Page<CollectionDetailPageKey, CollectionDetailPageChunk>
        assertTrue(duplicate.data.single().page.items.isEmpty())
        assertNull(duplicate.nextKey)
    }
}
