package com.android.purebilibili.feature.video.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class CommentAvatarPushPolicyTest {
    private fun contactingAvatarTop(nextTop: Int): Int {
        val itemTop = nextTop - 250
        return itemTop + 10 + resolveCommentAvatarPushOffset(
            itemOffsetPx = itemTop,
            itemHeightPx = 240,
            topInsetPx = 10,
            bottomInsetPx = 1,
            viewportTopPx = 0,
            nextAvatarTopPx = nextTop,
        )
    }

    @Test
    fun avatarsDoNotPushWhileThereIsStillAVisibleGap() {
        assertEquals(0, contactingAvatarTop(nextTop = 60))
        assertEquals(0, contactingAvatarTop(nextTop = 43))
        assertEquals(0, contactingAvatarTop(nextTop = 42))
    }

    @Test
    fun outgoingAvatarTouchesItsNeighbourThroughoutThePush() {
        for (nextTop in 0..42) {
            assertEquals(nextTop, contactingAvatarTop(nextTop) + 42)
        }
    }

    @Test
    fun footersAndSectionTitlesCannotBecomeCollisionPartners() {
        assertTrue(isCommentAvatarItemKey(123L))
        assertTrue(isCommentAvatarItemKey("reply_123"))
        assertTrue(isCommentAvatarItemKey("curtain_reply_123"))
        assertTrue(isCommentAvatarItemKey("123_false"))
        assertTrue(isCommentAvatarItemKey("123_true"))
        assertFalse(isCommentAvatarItemKey("root_reply"))
        assertFalse(isCommentAvatarItemKey("footer"))
        assertFalse(isCommentAvatarItemKey("video_comment_vote"))
    }

    private fun avatarTop(
        itemTop: Int,
        height: Int = 240,
        viewportTop: Int = 0,
        contentHeight: Int? = null,
    ): Int = itemTop + 10 + resolveCommentAvatarPushOffset(
        itemOffsetPx = itemTop,
        itemHeightPx = height,
        topInsetPx = 10,
        bottomInsetPx = 1,
        viewportTopPx = viewportTop,
        contentHeightPx = contentHeight,
    )

    @Test
    fun visibleAuthorsKeepTheirAvatarsBesideTheirNames() {
        assertEquals(10, avatarTop(itemTop = 0))
        assertEquals(110, avatarTop(itemTop = 100, height = 600))
        assertEquals(380, avatarTop(itemTop = 370, height = 300))
    }

    @Test
    fun offscreenAuthorPinsAtTheTopUntilItsCommentLeaves() {
        assertEquals(0, avatarTop(itemTop = -100))
        assertEquals(-29, avatarTop(itemTop = -220))
        assertEquals(-109, avatarTop(itemTop = -300))
    }

    @Test
    fun reversingScrollRestoresTheExactOriginalPosition() {
        val positions = listOf(0, 100, -10, -100, -220, -100, -10, 100, 0)
        assertEquals(
            listOf(10, 110, 0, 0, -29, 0, 0, 110, 10),
            positions.map { avatarTop(it) },
        )
    }

    @Test
    fun rootAvatarDoesNotFollowTheReplySectionInTheSameLazyItem() {
        val top = avatarTop(itemTop = -220, height = 320, contentHeight = 240)
        assertEquals(-29, top)
        assertTrue(top + 42 < -220 + 240)
    }

    @Test
    fun staleContentHeightCannotEscapeTheCurrentItem() {
        assertEquals(
            avatarTop(itemTop = -220, height = 240),
            avatarTop(itemTop = -220, height = 240, contentHeight = 400),
        )
    }

    @Test
    fun unmeasuredContentDoesNotDisplaceTheAvatar() {
        assertEquals(0, resolveCommentAvatarPushOffset(
            itemOffsetPx = 0,
            itemHeightPx = 320,
            topInsetPx = 10,
            bottomInsetPx = 1,
            viewportTopPx = 0,
            contentHeightPx = 0,
        ))
    }

    @Test
    fun originalClampOrderHandlesShortCells() {
        // Final cell-bottom clamp intentionally wins over the 48px minimum slot.
        assertEquals(-4, resolveNagramChatAvatarTop(0, 40, 400, 48, 44))
    }

    @Test
    fun originalGeometryScalesAtHigherDensity() {
        assertEquals(1068, resolveNagramChatAvatarTop(300, 1800, 1200, 144, 132))
    }

    @Test
    fun searchCardPaddingDefinesTheContentBoundary() {
        val offset = resolveCommentAvatarPushOffset(
            itemOffsetPx = 100,
            itemHeightPx = 300,
            topInsetPx = 12,
            bottomInsetPx = 12,
            viewportTopPx = 0,
        )
        assertEquals(112, 100 + 12 + offset)
    }

    @Test
    fun expandingVisibleContentDoesNotMoveTheAuthorAvatar() {
        assertEquals(10, avatarTop(itemTop = 0, height = 140))
        assertEquals(10, avatarTop(itemTop = 0, height = 600))
        assertEquals(10, avatarTop(itemTop = 0, height = 140))
    }
}
