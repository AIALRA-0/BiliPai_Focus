package com.android.purebilibili.feature.comment

import androidx.paging.PagingSource
import com.android.purebilibili.data.model.response.ReplyData
import com.android.purebilibili.data.model.response.ReplyCursor
import com.android.purebilibili.feature.video.viewmodel.SubReplyPageRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SubReplyPagingSourceTest {
    private fun params(key: SubReplyPageRequest? = null) = PagingSource.LoadParams.Refresh(key, 1, false)

    @Test fun forwardsOpaqueCursorAndStopsWhenServerRepeatsIt() = runTest {
        val requested = mutableListOf<SubReplyPageRequest>()
        val source = subReplyPagingSource { key ->
            requested += key
            ReplyData(grpcNextOffset = "opaque cursor")
        }
        val first = source.load(params()) as PagingSource.LoadResult.Page<SubReplyPageRequest, SubReplyPageChunk>
        assertEquals(SubReplyPageRequest(2, "opaque cursor"), first.nextKey)
        val last = source.load(params(first.nextKey)) as PagingSource.LoadResult.Page<SubReplyPageRequest, SubReplyPageChunk>
        assertNull(last.nextKey)
        assertEquals(listOf(SubReplyPageRequest(1, null), SubReplyPageRequest(2, "opaque cursor")), requested)
    }

    @Test fun resumedBaseListUsesItsSavedCursorInsteadOfReloadingFirstPage() = runTest {
        val requested = mutableListOf<SubReplyPageRequest>()
        val resume = SubReplyPageRequest(4, "saved-base-cursor")
        val source = subReplyPagingSource(initialRequest = resume) { request ->
            requested += request
            ReplyData(grpcNextOffset = "next-base-cursor")
        }
        val page = source.load(params()) as PagingSource.LoadResult.Page<SubReplyPageRequest, SubReplyPageChunk>
        assertEquals(listOf(resume), requested)
        assertEquals(SubReplyPageRequest(5, "next-base-cursor"), page.nextKey)
    }

    @Test fun explicitServerEndOverridesNonemptyCursor() = runTest {
        val source = subReplyPagingSource { ReplyData(cursor = ReplyCursor(isEnd = true), grpcNextOffset = "unused") }
        val last = source.load(params()) as PagingSource.LoadResult.Page<SubReplyPageRequest, SubReplyPageChunk>
        assertNull(last.nextKey)
    }

    @Test fun failedAppendRetriesExactCursorAndDoesNotPoisonOffsetHistory() = runTest {
        var fail = true
        val requests = mutableListOf<SubReplyPageRequest>()
        val source = subReplyPagingSource { request ->
            requests += request
            if (request.page == 2 && fail) { fail = false; error("暂时失败") }
            ReplyData(grpcNextOffset = if (request.page == 1) "second" else "third")
        }
        source.load(params())
        val next = SubReplyPageRequest(2, "second")
        assertTrue(source.load(params(next)) is PagingSource.LoadResult.Error)
        val retried = source.load(params(next)) as PagingSource.LoadResult.Page<SubReplyPageRequest, SubReplyPageChunk>
        assertEquals(SubReplyPageRequest(3, "third"), retried.nextKey)
        assertEquals(listOf(SubReplyPageRequest(1, null), next, next), requests)
    }

    @Test fun cancellationPropagatesWhenThreadChanges() = runTest {
        val source = subReplyPagingSource { throw CancellationException("评论已切换") }
        try {
            source.load(params())
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
    }
}
