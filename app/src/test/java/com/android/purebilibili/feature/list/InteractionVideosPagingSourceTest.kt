package com.android.purebilibili.feature.list

import androidx.paging.PagingSource
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.data.repository.LikedVideosRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class InteractionVideosPagingSourceTest {
    private fun params(page: Int?) = PagingSource.LoadParams.Refresh(page, 2, false)
    private fun video(id: Long) = VideoItem(id = id, bvid = "BV$id")

    @Test fun duplicateOnlyPageEndsPaginationWithoutDuplicatingCards() = runTest {
        val source = InteractionVideosPagingSource(2) { LikedVideosRepository.Page(listOf(video(1), video(2)), 100) }
        val first = source.load(params(null)) as PagingSource.LoadResult.Page<Int, VideoItem>
        assertEquals(2, first.nextKey)
        val duplicate = source.load(params(2)) as PagingSource.LoadResult.Page<Int, VideoItem>
        assertTrue(duplicate.data.isEmpty())
        assertNull(duplicate.nextKey)
    }

    @Test fun failedAppendRetriesItsPageAndDoesNotLosePreviouslyLoadedCards() = runTest {
        var fail = true
        val requested = mutableListOf<Int>()
        val source = InteractionVideosPagingSource(2) { page ->
            requested += page
            if (page == 2 && fail) { fail = false; error("暂时失败") }
            LikedVideosRepository.Page(if (page == 1) listOf(video(1), video(2)) else listOf(video(2), video(3)), 100)
        }
        source.load(params(null))
        assertTrue(source.load(params(2)) is PagingSource.LoadResult.Error)
        val retried = source.load(params(2)) as PagingSource.LoadResult.Page<Int, VideoItem>
        assertEquals(listOf("BV3"), retried.data.map { it.bvid })
        assertEquals(listOf(1, 2, 2), requested)
    }

    @Test fun cancelledAccountRequestDoesNotBecomeALoadError() = runTest {
        val source = InteractionVideosPagingSource(2) { throw CancellationException("账号已切换") }
        try {
            source.load(params(null))
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
    }
}
