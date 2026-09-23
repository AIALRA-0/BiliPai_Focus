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

    @Test
    fun `comment header and empty copy use compact reply chrome`() {
        assertEquals("0条回复", resolveDynamicCommentCountLabel(-3))
        assertEquals("12条回复", resolveDynamicCommentCountLabel(12))
        assertEquals("1.2万条回复", resolveDynamicCommentCountLabel(12_000))
        assertEquals("还没有评论", resolveDynamicCommentEmptyLabel())
        assertEquals("发一条友善的评论", resolveDynamicCommentComposerHint())
        assertEquals("回复 @小明", resolveDynamicCommentComposerHint("小明"))
        assertEquals("评论内容", resolveDynamicCommentImeSubmission("  评论内容  "))
        assertEquals(null, resolveDynamicCommentImeSubmission("   "))
    }

    @Test
    fun `feed comment opens the detail page instead of a bottom sheet`() {
        assertTrue(shouldOpenDynamicCommentsInDetailPage())
    }

    @Test
    fun `tapping a comment always opens the thread`() {
        val reply = ReplyItem(rpid = 8L)
        assertTrue(shouldOpenDynamicCommentThreadOnTap(reply))
        assertFalse(canOpenDynamicSubReplies(reply))
        assertFalse(shouldOpenDynamicCommentThreadOnTap(ReplyItem(rpid = 0L)))
        assertEquals(
            DynamicCommentSheetHostContent.THREAD_DETAIL,
            resolveDynamicCommentSheetHostContent(subReplyVisible = true),
        )
        assertEquals(
            DynamicCommentSheetHostContent.MAIN_LIST,
            resolveDynamicCommentSheetHostContent(subReplyVisible = false),
        )
    }

    @Test
    fun `reply target uses root when present and otherwise the comment itself`() {
        val nested = ReplyItem(rpid = 9L, root = 3L, member = com.android.purebilibili.data.model.response.ReplyMember(uname = "小红"))
        val root = resolveDynamicCommentReplyTarget(ReplyItem(rpid = 3L, member = com.android.purebilibili.data.model.response.ReplyMember(uname = "小明")))

        assertEquals(DynamicCommentComposerTarget(rootRpid = 3L, parentRpid = 3L, uname = "小明"), root)
        assertEquals(DynamicCommentComposerTarget(rootRpid = 3L, parentRpid = 9L, uname = "小红"), resolveDynamicCommentReplyTarget(nested))
    }

    @Test
    fun `liking a comment updates action and count without touching siblings`() {
        val comments = listOf(
            ReplyItem(
                rpid = 1L,
                like = 3,
                action = 0,
                replies = listOf(ReplyItem(rpid = 2L, like = 1, action = 0))
            ),
            ReplyItem(rpid = 3L, like = 8, action = 1)
        )

        val liked = applyDynamicCommentLikeInList(comments, rpid = 2L, toLiked = true)
        val unliked = applyDynamicCommentLikeInList(liked, rpid = 3L, toLiked = false)

        assertEquals(1, liked[0].replies?.first()?.action)
        assertEquals(2, liked[0].replies?.first()?.like)
        assertEquals(0, unliked[1].action)
        assertEquals(7, unliked[1].like)
        assertEquals(3, unliked[0].like)
    }
}
