package com.android.purebilibili.feature.video.viewmodel

import com.android.purebilibili.data.model.response.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class CommentSearchLoaderTest {
    @Test
    fun `search loads every thread page and deduplicates preview replies`() = runTest {
        val seenOffsets = mutableListOf<String?>()
        val result = loadCommentSearchReplies(
            loadMain = { _, _ -> Result.success(ReplyData(
                cursor = ReplyCursor(isEnd = true),
                replies = listOf(ReplyItem(rpid = 1, count = 3, replies = listOf(ReplyItem(rpid = 2)))),
            )) },
            loadThread = { root, offset ->
                assertEquals(1L, root)
                seenOffsets += offset
                Result.success(if (offset == null) ReplyData(
                    replies = listOf(ReplyItem(rpid = 2), ReplyItem(rpid = 3)), grpcNextOffset = "next",
                ) else ReplyData(cursor = ReplyCursor(isEnd = true), replies = listOf(ReplyItem(rpid = 4))))
            },
        )
        assertEquals(listOf(null, "next"), seenOffsets)
        assertEquals(listOf(2L, 3L, 4L), result.replies.single().replies?.map { it.rpid })
        assertEquals(4, result.loadedCount)
        assertTrue(result.isComplete)
    }

    @Test
    fun `empty intermediate main page does not hide later results`() = runTest {
        var requests = 0
        val result = loadCommentSearchReplies(
            loadMain = { page, _ ->
                requests++
                Result.success(if (page == 1) ReplyData(mainListContinuation = CommentMainListContinuation(
                    CommentMainListRoute.GRPC, next = 99,
                )) else ReplyData(cursor = ReplyCursor(isEnd = true), replies = listOf(ReplyItem(rpid = 1))))
            },
            loadThread = { _, _ -> error("No threads") },
        )
        assertEquals(2, requests)
        assertEquals(1, result.loadedCount)
        assertTrue(result.isComplete)
    }

    @Test
    fun `request cap keeps partial results without declaring completion`() = runTest {
        val result = loadCommentSearchReplies(
            maxRequests = 1,
            loadMain = { _, _ -> Result.success(ReplyData(
                cursor = ReplyCursor(isEnd = true), replies = listOf(ReplyItem(rpid = 1, count = 10)),
            )) },
            loadThread = { _, _ -> error("Budget exhausted") },
        )
        assertEquals(1, result.loadedCount)
        assertFalse(result.isComplete)
    }

    @Test
    fun `thread failure preserves earlier results and cancellation propagates`() = runTest {
        val result = loadCommentSearchReplies(
            loadMain = { _, _ -> Result.success(ReplyData(
                cursor = ReplyCursor(isEnd = true), replies = listOf(ReplyItem(rpid = 1, count = 1)),
            )) },
            loadThread = { _, _ -> Result.failure(IllegalStateException("offline")) },
        )
        assertFalse(result.isComplete)
        assertEquals("offline", result.error)
        assertEquals(1, result.loadedCount)
        assertFailsWith<CancellationException> {
            loadCommentSearchReplies(
                loadMain = { _, _ -> throw CancellationException("changed video") },
                loadThread = { _, _ -> error("Cancelled") },
            )
        }
    }
}
