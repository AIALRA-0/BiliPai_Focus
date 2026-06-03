package com.android.purebilibili.feature.dynamic

import com.android.purebilibili.data.model.response.ReplyItem
import com.android.purebilibili.feature.video.viewmodel.SubReplyUiState
import kotlinx.collections.immutable.toImmutableList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicCommentReplyPolicyTest {

    @Test
    fun `dynamic comment exposes sub reply entry when server says replies exist`() {
        val reply = ReplyItem(rpid = 1L, rcount = 4)

        assertTrue(canOpenDynamicSubReplies(reply))
        assertEquals(4, resolveDynamicSubReplyCount(reply))
    }

    @Test
    fun `dynamic comment falls back to inline reply preview count`() {
        val reply = ReplyItem(
            rpid = 1L,
            replies = listOf(
                ReplyItem(rpid = 2L),
                ReplyItem(rpid = 3L)
            )
        )

        assertTrue(canOpenDynamicSubReplies(reply))
        assertEquals(2, resolveDynamicSubReplyCount(reply))
    }

    @Test
    fun `dynamic comment hides sub reply entry when no replies exist`() {
        val reply = ReplyItem(rpid = 1L)

        assertFalse(canOpenDynamicSubReplies(reply))
        assertEquals(0, resolveDynamicSubReplyCount(reply))
    }

    @Test
    fun `sub reply append failure preserves existing items`() {
        val currentState = SubReplyUiState(
            items = listOf(ReplyItem(rpid = 1L), ReplyItem(rpid = 2L)).toImmutableList(),
            isLoading = true,
            page = 2
        )

        val result = resolveDynamicSubReplyStateAfterFailure(
            currentState = currentState,
            errorMessage = "加载失败"
        )

        assertEquals(listOf(1L, 2L), result.items.map { it.rpid })
        assertFalse(result.isLoading)
        assertEquals("加载失败", result.error)
    }

    @Test
    fun `sub reply append success deduplicates while preserving previous items`() {
        val currentState = SubReplyUiState(
            items = listOf(ReplyItem(rpid = 1L), ReplyItem(rpid = 2L)).toImmutableList(),
            isLoading = true,
            page = 1
        )

        val result = resolveDynamicSubReplyStateAfterSuccess(
            currentState = currentState,
            newItems = listOf(ReplyItem(rpid = 2L), ReplyItem(rpid = 3L)),
            page = 2,
            isEnd = false
        )

        assertEquals(listOf(1L, 2L, 3L), result.items.map { it.rpid })
        assertEquals(2, result.page)
        assertFalse(result.isLoading)
        assertFalse(result.isEnd)
    }
}
