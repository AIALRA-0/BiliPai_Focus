package com.android.purebilibili.feature.video.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoCommentCountPolicyTest {
    private val video = CommentSubjectKey(10L)

    @Test
    fun detailSeedsFirstFrameBeforeDeferredCommentsInitialize() {
        assertEquals(70, CommentUiState().resolveReplyCount(video, 70))
        assertNull(CommentUiState().resolveReplyCount(video))
    }

    @Test
    fun authoritativeZeroDoesNotResurrectOldDetailCount() {
        val loaded = CommentUiState(
            replyCount = 0,
            replyCountSubject = video,
            replyCountIsAuthoritative = true,
        )
        assertEquals(0, loaded.resolveReplyCount(video, 70))
    }

    @Test
    fun staleVideoOrCommentTypeCannotSupplyTheDisplayedCount() {
        val loaded = CommentUiState(
            replyCount = 900,
            replyCountSubject = CommentSubjectKey(9L),
            replyCountIsAuthoritative = true,
        )
        assertEquals(70, loaded.resolveReplyCount(video, 70))
        assertNull(loaded.resolveReplyCount(video))
        assertNull(loaded.copy(replyCountSubject = CommentSubjectKey(10L, 33)).resolveReplyCount(video))
    }

    @Test
    fun freshDetailReplacesNonAuthoritativeSeedAndLoadedCountCanDecrease() {
        val seeded = CommentUiState(replyCount = 0, replyCountSubject = video)
        assertEquals(70, seeded.resolveReplyCount(video, 70))
        assertEquals(12, seeded.copy(replyCount = 12, replyCountIsAuthoritative = true).resolveReplyCount(video, 70))
        assertEquals(0, CommentUiState().resolveReplyCount(video, 0))
    }
}
