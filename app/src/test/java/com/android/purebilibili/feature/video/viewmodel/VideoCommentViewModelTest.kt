package com.android.purebilibili.feature.video.viewmodel

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoCommentViewModelTest {

    @Test
    fun `comment result applies only to current video subject`() {
        val firstVideo = CommentSubjectKey(oid = 1001L)
        val secondVideo = CommentSubjectKey(oid = 2002L)

        assertTrue(
            shouldApplyCommentSubjectResult(
                expectedSubject = firstVideo,
                currentSubject = firstVideo
            )
        )
        assertFalse(
            shouldApplyCommentSubjectResult(
                expectedSubject = firstVideo,
                currentSubject = secondVideo
            )
        )
    }

    @Test
    fun `sub reply result is dropped after video or root changes`() {
        val firstVideo = CommentSubjectKey(oid = 1001L)
        val secondVideo = CommentSubjectKey(oid = 2002L)

        assertTrue(
            shouldApplySubReplyResult(
                expectedSubject = firstVideo,
                expectedRootId = 11L,
                currentSubject = firstVideo,
                activeRootId = 11L,
                conversationActive = false
            )
        )
        assertFalse(
            shouldApplySubReplyResult(
                expectedSubject = firstVideo,
                expectedRootId = 11L,
                currentSubject = secondVideo,
                activeRootId = 11L,
                conversationActive = false
            )
        )
        assertFalse(
            shouldApplySubReplyResult(
                expectedSubject = firstVideo,
                expectedRootId = 11L,
                currentSubject = firstVideo,
                activeRootId = 22L,
                conversationActive = false
            )
        )
        assertFalse(
            shouldApplySubReplyResult(
                expectedSubject = firstVideo,
                expectedRootId = 11L,
                currentSubject = firstVideo,
                activeRootId = 11L,
                conversationActive = true
            )
        )
    }

    @Test
    fun `conversation result is dropped after dialog changes`() {
        val subject = CommentSubjectKey(oid = 1001L)

        assertTrue(
            shouldApplyConversationReplyResult(
                expectedSubject = subject,
                expectedRootId = 11L,
                expectedDialogId = 33L,
                currentSubject = subject,
                activeRootId = 11L,
                activeDialogId = 33L
            )
        )
        assertFalse(
            shouldApplyConversationReplyResult(
                expectedSubject = subject,
                expectedRootId = 11L,
                expectedDialogId = 33L,
                currentSubject = subject,
                activeRootId = 11L,
                activeDialogId = 44L
            )
        )
    }
}
