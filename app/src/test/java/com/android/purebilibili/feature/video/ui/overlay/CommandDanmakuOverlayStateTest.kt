package com.android.purebilibili.feature.video.ui.overlay

import com.android.purebilibili.data.model.response.GradeDanmakuSummary
import com.android.purebilibili.feature.video.danmaku.CommandDanmakuItem
import com.android.purebilibili.feature.video.danmaku.CommandDanmakuType
import com.android.purebilibili.feature.video.danmaku.VoteDanmakuKind
import com.android.purebilibili.feature.video.danmaku.VoteOption
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommandDanmakuOverlayStateTest {
    @Test
    fun `accepted votes remain selected and cannot submit again after cleanup`() {
        val state = CommandDanmakuOverlayState()
        val item = voteItem()
        val option = VoteOption("3", "蓝疯子太疯了", optionIndex = 3)
        assertTrue(state.beginVoteSubmission(item))
        state.completeVoteSubmission(item, option)
        state.endSubmission(item.id)
        assertFalse(state.beginVoteSubmission(item))
        assertEquals(option, state.selection(item.id))
    }

    @Test
    fun `dismissed commands stay dismissed without affecting another command`() {
        val state = CommandDanmakuOverlayState()
        val first = voteItem().copy(id = "first")
        val second = voteItem().copy(id = "second")
        state.dismiss(first.id)
        assertTrue(state.isDismissed(first.id))
        assertFalse(state.beginVoteSubmission(first))
        assertNull(state.selection(first.id))
        assertTrue(state.beginVoteSubmission(second))
        assertFalse(state.isDismissed(second.id))
    }

    @Test
    fun `grade submission locks duplicate taps without confirming a score`() {
        val state = CommandDanmakuOverlayState()
        val item = gradeItem(GradeDanmakuSummary(76L, 9.8))
        val option = VoteOption("four", "four", 8)

        assertTrue(state.beginGradeSubmission(item, option))
        assertTrue(state.isSubmitting(item.id))
        assertFalse(state.beginGradeSubmission(item, VoteOption("five", "five", 10)))
        assertNull(state.selection(item.id))
        assertNull(state.gradeScore(item))
        assertEquals(GradeDanmakuSummary(76L, 9.8), state.gradeSummary(item))
    }

    @Test
    fun `failed or cancelled grade submission unlocks for another attempt`() {
        val state = CommandDanmakuOverlayState()
        val item = gradeItem()
        val option = VoteOption("four", "four", 8)

        assertTrue(state.beginGradeSubmission(item, option))
        state.endSubmission(item.id)
        assertFalse(state.isSubmitting(item.id))
        assertNull(state.selection(item.id))
        assertNull(state.gradeScore(item))
        assertTrue(state.beginGradeSubmission(item, option))
    }

    @Test
    fun `accepted grade stays confirmed when statistics refresh fails`() {
        val state = CommandDanmakuOverlayState()
        val item = gradeItem(GradeDanmakuSummary(76L, 9.8))
        val option = VoteOption("four", "four", 8)

        assertTrue(state.beginGradeSubmission(item, option))
        state.completeGradeSubmission(item, option)
        // Submission cleanup also runs after a refresh failure; it must not clear acceptance.
        state.endSubmission(item.id)
        assertFalse(state.isSubmitting(item.id))
        assertEquals(option, state.selection(item.id))
        assertEquals(8, state.gradeScore(item))
        assertEquals(GradeDanmakuSummary(userScore = 8), state.gradeSummary(item))
        assertFalse(state.beginGradeSubmission(item, VoteOption("five", "five", 10)))
    }

    @Test
    fun `fresh grade aggregates replace unavailable statistics without overriding confirmed choice`() {
        val state = CommandDanmakuOverlayState()
        val item = gradeItem()
        val option = VoteOption("four", "four", 8)

        assertTrue(state.beginGradeSubmission(item, option))
        state.completeGradeSubmission(item, option)
        state.updateGradeSummary(item.id, GradeDanmakuSummary(139L, 9.8, 10))
        assertEquals(GradeDanmakuSummary(139L, 9.8, 10), state.gradeSummary(item))
        assertEquals(8, state.gradeScore(item))
        assertFalse(state.beginGradeSubmission(item, option))
    }

    @Test
    fun `server personal grade blocks resubmission before any local choice`() {
        val state = CommandDanmakuOverlayState()
        val item = gradeItem(GradeDanmakuSummary(139L, 9.8, 10))

        assertEquals(10, state.gradeScore(item))
        assertNull(state.selection(item.id))
        assertFalse(state.beginGradeSubmission(item, VoteOption("four", "four", 8)))
        assertFalse(state.isSubmitting(item.id))
    }

    @Test
    fun `refreshed server personal grade also blocks resubmission`() {
        val state = CommandDanmakuOverlayState()
        val item = gradeItem()
        state.updateGradeSummary(item.id, GradeDanmakuSummary(139L, 9.8, 6))

        assertEquals(6, state.gradeScore(item))
        assertFalse(state.beginGradeSubmission(item, VoteOption("four", "four", 8)))
    }

    @Test
    fun `dismissed grade and illegal scores cannot start submission`() {
        val state = CommandDanmakuOverlayState()
        val item = gradeItem()
        assertFalse(state.beginGradeSubmission(item, VoteOption("illegal", "illegal", 3)))
        state.dismiss(item.id)
        assertFalse(state.beginGradeSubmission(item, VoteOption("four", "four", 8)))
        assertFalse(state.isSubmitting(item.id))
    }

    @Test
    fun `state normalizes invalid initial and refreshed grade fields`() {
        val state = CommandDanmakuOverlayState()
        val item = gradeItem(GradeDanmakuSummary(-1L, Double.NaN, 0))
        assertEquals(GradeDanmakuSummary(), state.gradeSummary(item))
        assertNull(state.gradeScore(item))
        state.updateGradeSummary(item.id, GradeDanmakuSummary(-2L, Double.POSITIVE_INFINITY, 3))
        assertEquals(GradeDanmakuSummary(), state.gradeSummary(item))
        assertNull(state.gradeScore(item))
        assertTrue(state.beginGradeSubmission(item, VoteOption("four", "four", 8)))
    }

    @Test
    fun `pending votes do not show success and failed votes remain retryable`() {
        val state = CommandDanmakuOverlayState()
        val item = voteItem()
        assertTrue(state.beginVoteSubmission(item))
        assertFalse(state.beginVoteSubmission(item))
        assertTrue(state.isSubmitting(item.id))
        assertNull(state.selection(item.id))
        state.endSubmission(item.id)
        assertFalse(state.isSubmitting(item.id))
        assertNull(state.selection(item.id))
        assertTrue(state.beginVoteSubmission(item))
    }

    @Test
    fun `server voted option blocks another submission without a local selection`() {
        val state = CommandDanmakuOverlayState()
        val item = voteItem().copy(voteSelectedIndex = 2)
        assertNull(state.selection(item.id))
        assertFalse(state.beginVoteSubmission(item))
        assertFalse(state.isSubmitting(item.id))
    }

    private fun voteItem() = CommandDanmakuItem(
        id = "vote",
        type = CommandDanmakuType.VOTE,
        content = "来投票",
        startTimeMs = 0L,
        durationMs = 8000L,
        voteKind = VoteDanmakuKind.VOTE,
        voteId = "21517957",
    )

    private fun gradeItem(summary: GradeDanmakuSummary? = null) = CommandDanmakuItem(
        id = "grade",
        type = CommandDanmakuType.VOTE,
        content = "熟练程度如何",
        startTimeMs = 1000L,
        durationMs = 8000L,
        voteKind = VoteDanmakuKind.GRADE,
        voteId = "3651137",
        gradeSummary = summary
    )
}
