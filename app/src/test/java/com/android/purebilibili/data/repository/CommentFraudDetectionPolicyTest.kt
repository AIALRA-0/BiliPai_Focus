package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.CommentFraudStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class CommentFraudDetectionPolicyTest {

    @Test
    fun `sub reply scan follows known total and stops on final page`() {
        assertEquals(true, shouldContinueSubReplyFraudScan(1, 20, 20, 41, 50))
        assertEquals(false, shouldContinueSubReplyFraudScan(3, 20, 1, 41, 50))
    }

    @Test
    fun `sub reply scan fallback remains bounded when total is absent`() {
        assertEquals(true, shouldContinueSubReplyFraudScan(1, 20, 20, 0, 50))
        assertEquals(false, shouldContinueSubReplyFraudScan(50, 20, 20, 0, 50))
        assertEquals(false, shouldContinueSubReplyFraudScan(1, 20, 0, 0, 50))
    }

    @Test
    fun `fraud detection should start only when enabled and reply id is valid`() {
        assertEquals(true, shouldStartCommentFraudDetection(enabled = true, rpid = 123L))
        assertEquals(false, shouldStartCommentFraudDetection(enabled = false, rpid = 123L))
        assertEquals(false, shouldStartCommentFraudDetection(enabled = true, rpid = 0L))
    }

    @Test
    fun `normal fraud result uses light message instead of dialog`() {
        assertEquals(false, shouldShowCommentFraudResultDialog(CommentFraudStatus.NORMAL))
        assertEquals("评论已正常显示", resolveCommentFraudLightMessage(CommentFraudStatus.NORMAL))
    }

    @Test
    fun `abnormal fraud results use dialog instead of light message`() {
        assertEquals(true, shouldShowCommentFraudResultDialog(CommentFraudStatus.SHADOW_BANNED))
        assertEquals(true, shouldShowCommentFraudResultDialog(CommentFraudStatus.DELETED))
        assertEquals(true, shouldShowCommentFraudResultDialog(CommentFraudStatus.UNDER_REVIEW))
        assertEquals(true, shouldShowCommentFraudResultDialog(CommentFraudStatus.INVISIBLE))
        assertEquals(true, shouldShowCommentFraudResultDialog(CommentFraudStatus.UNKNOWN))
        assertEquals(null, resolveCommentFraudLightMessage(CommentFraudStatus.SHADOW_BANNED))
    }

    @Test
    fun `reply status should be normal when guest probe found`() {
        val status = resolveReplyFraudStatus(
            guestProbe = CommentPresenceProbe(requestSucceeded = true, found = true),
            authProbe = CommentPresenceProbe(requestSucceeded = true, found = true),
            confirmedNotFoundAfterRetry = false
        )
        assertEquals(CommentFraudStatus.NORMAL, status)
    }

    @Test
    fun `reply status should be invisible when guest probe found invisible comment`() {
        val status = resolveReplyFraudStatus(
            guestProbe = CommentPresenceProbe(requestSucceeded = true, found = true, invisible = true),
            authProbe = CommentPresenceProbe(requestSucceeded = true, found = true),
            confirmedNotFoundAfterRetry = false
        )
        assertEquals(CommentFraudStatus.INVISIBLE, status)
    }

    @Test
    fun `reply status should be shadow banned when only auth probe found`() {
        val status = resolveReplyFraudStatus(
            guestProbe = CommentPresenceProbe(requestSucceeded = true, found = false),
            authProbe = CommentPresenceProbe(requestSucceeded = true, found = true),
            confirmedNotFoundAfterRetry = false
        )
        assertEquals(CommentFraudStatus.SHADOW_BANNED, status)
    }

    @Test
    fun `reply status should be invisible when auth probe found invisible comment`() {
        val status = resolveReplyFraudStatus(
            guestProbe = CommentPresenceProbe(requestSucceeded = true, found = false),
            authProbe = CommentPresenceProbe(requestSucceeded = true, found = true, invisible = true),
            confirmedNotFoundAfterRetry = false
        )
        assertEquals(CommentFraudStatus.INVISIBLE, status)
    }

    @Test
    fun `reply status should be deleted when auth probe reports deleted hint`() {
        val status = resolveReplyFraudStatus(
            guestProbe = CommentPresenceProbe(requestSucceeded = true, found = false),
            authProbe = CommentPresenceProbe(
                requestSucceeded = true,
                found = false,
                deletedHint = true
            ),
            confirmedNotFoundAfterRetry = false
        )
        assertEquals(CommentFraudStatus.DELETED, status)
    }

    @Test
    fun `reply status should be unknown when guest probe failed`() {
        val status = resolveReplyFraudStatus(
            guestProbe = CommentPresenceProbe(requestSucceeded = false, found = false),
            authProbe = CommentPresenceProbe(requestSucceeded = true, found = true),
            confirmedNotFoundAfterRetry = false
        )
        assertEquals(CommentFraudStatus.UNKNOWN, status)
    }

    @Test
    fun `root status should be under review when auth found and guest reply page visible`() {
        val status = resolveRootFraudStatus(
            guestSeekProbe = CommentPresenceProbe(requestSucceeded = true, found = false),
            authSeekProbe = CommentPresenceProbe(requestSucceeded = true, found = true),
            guestReplyPageVisible = true,
            confirmedNotFoundAfterRetry = false
        )
        assertEquals(CommentFraudStatus.UNDER_REVIEW, status)
    }

    @Test
    fun `root status should be shadow banned when auth found and guest reply page deleted`() {
        val status = resolveRootFraudStatus(
            guestSeekProbe = CommentPresenceProbe(requestSucceeded = true, found = false),
            authSeekProbe = CommentPresenceProbe(requestSucceeded = true, found = true),
            guestReplyPageVisible = false,
            confirmedNotFoundAfterRetry = false
        )
        assertEquals(CommentFraudStatus.SHADOW_BANNED, status)
    }

    @Test
    fun `root status should be invisible when auth seek found invisible comment`() {
        val status = resolveRootFraudStatus(
            guestSeekProbe = CommentPresenceProbe(requestSucceeded = true, found = false),
            authSeekProbe = CommentPresenceProbe(requestSucceeded = true, found = true, invisible = true),
            guestReplyPageVisible = null,
            confirmedNotFoundAfterRetry = false
        )
        assertEquals(CommentFraudStatus.INVISIBLE, status)
    }

    @Test
    fun `root status should be deleted when auth seek probe has deleted hint`() {
        val status = resolveRootFraudStatus(
            guestSeekProbe = CommentPresenceProbe(requestSucceeded = true, found = false),
            authSeekProbe = CommentPresenceProbe(
                requestSucceeded = true,
                found = false,
                deletedHint = true
            ),
            guestReplyPageVisible = null,
            confirmedNotFoundAfterRetry = false
        )
        assertEquals(CommentFraudStatus.DELETED, status)
    }

    @Test
    fun `root status should be normal when guest seek found comment`() {
        val status = resolveRootFraudStatus(
            guestSeekProbe = CommentPresenceProbe(requestSucceeded = true, found = true),
            authSeekProbe = CommentPresenceProbe(requestSucceeded = true, found = true),
            guestReplyPageVisible = null,
            confirmedNotFoundAfterRetry = false
        )
        assertEquals(CommentFraudStatus.NORMAL, status)
    }

    @Test
    fun `root status should be invisible when guest seek found invisible comment`() {
        val status = resolveRootFraudStatus(
            guestSeekProbe = CommentPresenceProbe(requestSucceeded = true, found = true, invisible = true),
            authSeekProbe = CommentPresenceProbe(requestSucceeded = true, found = true),
            guestReplyPageVisible = null,
            confirmedNotFoundAfterRetry = false
        )
        assertEquals(CommentFraudStatus.INVISIBLE, status)
    }

    @Test
    fun `root status should stay unknown when probes are not conclusive`() {
        val status = resolveRootFraudStatus(
            guestSeekProbe = CommentPresenceProbe(requestSucceeded = true, found = false),
            authSeekProbe = CommentPresenceProbe(requestSucceeded = true, found = false),
            guestReplyPageVisible = null,
            confirmedNotFoundAfterRetry = false
        )
        assertEquals(CommentFraudStatus.UNKNOWN, status)
    }

    @Test
    fun `root status should be deleted when confirmed not found after retry`() {
        val status = resolveRootFraudStatus(
            guestSeekProbe = CommentPresenceProbe(requestSucceeded = true, found = false),
            authSeekProbe = CommentPresenceProbe(requestSucceeded = true, found = false),
            guestReplyPageVisible = null,
            confirmedNotFoundAfterRetry = true
        )
        assertEquals(CommentFraudStatus.DELETED, status)
    }
}
