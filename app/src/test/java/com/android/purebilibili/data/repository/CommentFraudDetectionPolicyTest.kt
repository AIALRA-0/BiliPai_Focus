package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.CommentFraudStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class CommentFraudDetectionPolicyTest {

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
    fun `reply status should be shadow banned when only auth probe found`() {
        val status = resolveReplyFraudStatus(
            guestProbe = CommentPresenceProbe(requestSucceeded = true, found = false),
            authProbe = CommentPresenceProbe(requestSucceeded = true, found = true),
            confirmedNotFoundAfterRetry = false
        )
        assertEquals(CommentFraudStatus.SHADOW_BANNED, status)
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
    fun `root timeline status should be normal when guest timeline found comment`() {
        val status = resolveRootFraudStatusFromTimeline(
            guestTimelineProbe = CommentPresenceProbe(requestSucceeded = true, found = true),
            authReplyPageProbe = CommentReplyPageProbe(requestSucceeded = true, visible = true),
            guestReplyPageProbe = null,
            confirmedDeletedAfterRetry = false
        )
        assertEquals(CommentFraudStatus.NORMAL, status)
    }

    @Test
    fun `root timeline status should be shadow banned when auth page visible but guest page deleted`() {
        val status = resolveRootFraudStatusFromTimeline(
            guestTimelineProbe = CommentPresenceProbe(requestSucceeded = true, found = false),
            authReplyPageProbe = CommentReplyPageProbe(requestSucceeded = true, visible = true),
            guestReplyPageProbe = CommentReplyPageProbe(
                requestSucceeded = true,
                visible = false,
                deletedHint = true
            ),
            confirmedDeletedAfterRetry = false
        )
        assertEquals(CommentFraudStatus.SHADOW_BANNED, status)
    }

    @Test
    fun `root timeline status should be under review when auth and guest reply pages are visible`() {
        val status = resolveRootFraudStatusFromTimeline(
            guestTimelineProbe = CommentPresenceProbe(requestSucceeded = true, found = false),
            authReplyPageProbe = CommentReplyPageProbe(requestSucceeded = true, visible = true),
            guestReplyPageProbe = CommentReplyPageProbe(requestSucceeded = true, visible = true),
            confirmedDeletedAfterRetry = false
        )
        assertEquals(CommentFraudStatus.UNDER_REVIEW, status)
    }

    @Test
    fun `root timeline status should not be deleted when auth page lacks deleted hint`() {
        val status = resolveRootFraudStatusFromTimeline(
            guestTimelineProbe = CommentPresenceProbe(requestSucceeded = true, found = false),
            authReplyPageProbe = CommentReplyPageProbe(requestSucceeded = true, visible = false),
            guestReplyPageProbe = null,
            confirmedDeletedAfterRetry = true
        )
        assertEquals(CommentFraudStatus.UNKNOWN, status)
    }

    @Test
    fun `root timeline status should be deleted only after auth deleted hint and retry confirmation`() {
        val status = resolveRootFraudStatusFromTimeline(
            guestTimelineProbe = CommentPresenceProbe(requestSucceeded = true, found = false),
            authReplyPageProbe = CommentReplyPageProbe(
                requestSucceeded = true,
                visible = false,
                deletedHint = true
            ),
            guestReplyPageProbe = null,
            confirmedDeletedAfterRetry = true
        )
        assertEquals(CommentFraudStatus.DELETED, status)
    }
}
