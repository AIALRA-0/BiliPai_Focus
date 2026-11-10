package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.ReplyData
import com.android.purebilibili.data.model.response.ReplyItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommentReplyPreviewTest {
    private val selected = ReplyItem(rpid = 11L)
    private val root = ReplyItem(
        oid = 170001L,
        rpid = 10L,
        count = 8,
        replies = listOf(selected),
    )

    @Test
    fun `sparse main list sample is filled asynchronously without replacing selected reply`() = runTest {
        val response = CompletableDeferred<Result<ReplyData>>()
        val requested = CompletableDeferred<Int>()
        val pending = async {
            loadCommentReplyPreview(root, 3) { pageSize ->
                requested.complete(pageSize)
                response.await()
            }
        }
        assertEquals(3, requested.await())
        assertFalse(pending.isCompleted)
        response.complete(Result.success(ReplyData(replies = listOf(
            ReplyItem(rpid = 12L), selected.copy(like = 99), ReplyItem(rpid = 13L),
        ))))

        val preview = pending.await()
        assertEquals(listOf(11L, 12L, 13L), preview.map { it.rpid })
        assertEquals(selected, preview.first())
    }

    @Test
    fun `thread with only two replies requests two and never invents a third`() = runTest {
        val preview = loadCommentReplyPreview(root.copy(count = 2), 3) { pageSize ->
            assertEquals(2, pageSize)
            Result.success(ReplyData(replies = listOf(selected, ReplyItem(rpid = 12L))))
        }
        assertEquals(listOf(11L, 12L), preview.map { it.rpid })
    }

    @Test
    fun `already complete single reply needs no network`() = runTest {
        val preview = loadCommentReplyPreview(root.copy(count = 1), 3) {
            error("A complete preview must not request its thread")
        }
        assertEquals(listOf(selected), preview)
    }

    @Test
    fun `raising limit fills missing replies and lowering retains loaded data`() = runTest {
        val single = loadCommentReplyPreview(root, 1) { error("One reply is already loaded") }
        var calls = 0
        val three = loadCommentReplyPreview(root, 3, single) {
            calls++
            Result.success(ReplyData(replies = listOf(selected, ReplyItem(rpid = 12L), ReplyItem(rpid = 13L))))
        }
        val lowered = loadCommentReplyPreview(root, 1, three) { error("Do not refetch on lowering limit") }
        val restored = loadCommentReplyPreview(root, 3, lowered) { error("Do not refetch loaded previews") }
        assertEquals(1, calls)
        assertEquals(listOf(11L, 12L, 13L), restored.map { it.rpid })
    }

    @Test
    fun `rcount can supply total when count is absent`() = runTest {
        val preview = loadCommentReplyPreview(root.copy(count = 0, rcount = 3), 3) {
            Result.success(ReplyData(replies = listOf(selected, ReplyItem(rpid = 12L), ReplyItem(rpid = 13L))))
        }
        assertEquals(3, preview.size)
    }

    @Test
    fun `short filtered page and duplicate ids do not create fake previews`() = runTest {
        val preview = loadCommentReplyPreview(root, 3) {
            Result.success(ReplyData(replies = listOf(root, selected, selected, ReplyItem(rpid = 12L))))
        }
        assertEquals(listOf(11L, 12L), preview.map { it.rpid })
    }

    @Test
    fun `failed enrichment preserves available preview without retry`() = runTest {
        var calls = 0
        val preview = loadCommentReplyPreview(root, 3) {
            calls++
            Result.failure(IllegalStateException("offline"))
        }
        assertEquals(1, calls)
        assertEquals(listOf(selected), preview)
    }

    @Test
    fun `cancellation is propagated rather than treated as an empty thread`() = runTest {
        var cancelled = false
        try {
            loadCommentReplyPreview(root, 3) { Result.failure(CancellationException("left comment row")) }
        } catch (_: CancellationException) {
            cancelled = true
        }
        assertTrue(cancelled)
    }
}
