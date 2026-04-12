package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.ReplyCursor
import com.android.purebilibili.data.model.response.ReplyData
import com.android.purebilibili.data.model.response.ReplyControl
import com.android.purebilibili.data.model.response.ReplyItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommentReadAccessPolicyTest {

    @Test
    fun `resolveCommentReadPlan prefers auth for logged user`() {
        val plan = resolveCommentReadPlan(hasSession = true)
        assertEquals(CommentReadApiMode.AUTH, plan.primary)
        assertEquals(CommentReadApiMode.GUEST, plan.fallback)
    }

    @Test
    fun `resolveCommentReadPlan prefers guest for anonymous user`() {
        val plan = resolveCommentReadPlan(hasSession = false)
        assertEquals(CommentReadApiMode.GUEST, plan.primary)
        assertEquals(CommentReadApiMode.AUTH, plan.fallback)
    }

    @Test
    fun `shouldFallbackCommentRead covers unstable api errors`() {
        assertTrue(shouldFallbackCommentRead(-101))
        assertTrue(shouldFallbackCommentRead(-111))
        assertTrue(shouldFallbackCommentRead(-352))
        assertTrue(shouldFallbackCommentRead(-412))
        assertFalse(shouldFallbackCommentRead(12002))
    }

    @Test
    fun `resolveCommentReadErrorMessage avoids forcing login for anonymous read`() {
        assertEquals(
            "评论加载失败，请稍后重试或切换排序",
            resolveCommentReadErrorMessage(-101)
        )
    }

    @Test
    fun `guest hot comment read falls back when success payload is empty but count exists`() {
        assertTrue(
            shouldFallbackGuestHotCommentReadOnEmptySuccess(
                primaryMode = CommentReadApiMode.GUEST,
                page = 1,
                mode = 3,
                responseCode = 0,
                data = ReplyData(
                    cursor = ReplyCursor(allCount = 6118, isEnd = true, next = 0),
                    replies = emptyList(),
                    hots = emptyList()
                )
            )
        )
    }

    @Test
    fun `guest hot comment read keeps payload when renderable comments exist`() {
        assertFalse(
            shouldFallbackGuestHotCommentReadOnEmptySuccess(
                primaryMode = CommentReadApiMode.GUEST,
                page = 1,
                mode = 3,
                responseCode = 0,
                data = ReplyData(
                    cursor = ReplyCursor(allCount = 6118, isEnd = false, next = 2),
                    replies = listOf(ReplyItem(rpid = 1L))
                )
            )
        )
    }

    @Test
    fun `hasRenderableCommentPayload counts top or hot comments as visible content`() {
        assertTrue(
            hasRenderableCommentPayload(
                ReplyData(
                    hots = listOf(ReplyItem(rpid = 1L))
                )
            )
        )
    }

    @Test
    fun `grpc comment read falls back when rendered comments all miss ip location`() {
        assertTrue(
            shouldFallbackGrpcCommentReadOnMissingLocation(
                ReplyData(
                    replies = listOf(
                        ReplyItem(rpid = 1L),
                        ReplyItem(rpid = 2L, replyControl = ReplyControl(location = ""))
                    )
                )
            )
        )
    }

    @Test
    fun `grpc comment read keeps payload when any rendered comment has ip location`() {
        assertFalse(
            shouldFallbackGrpcCommentReadOnMissingLocation(
                ReplyData(
                    hots = listOf(
                        ReplyItem(
                            rpid = 1L,
                            replyControl = ReplyControl(location = "IP属地：上海")
                        )
                    ),
                    replies = listOf(ReplyItem(rpid = 2L))
                )
            )
        )
        assertTrue(
            hasAnyReplyLocation(
                ReplyData(
                    replies = listOf(
                        ReplyItem(
                            rpid = 3L,
                            replyControl = ReplyControl(location = "IP属地：北京")
                        )
                    )
                )
            )
        )
    }
}
