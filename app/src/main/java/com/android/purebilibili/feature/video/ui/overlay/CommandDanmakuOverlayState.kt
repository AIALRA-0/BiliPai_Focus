package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import com.android.purebilibili.data.model.response.GradeDanmakuSummary
import com.android.purebilibili.feature.video.danmaku.CommandDanmakuItem
import com.android.purebilibili.feature.video.danmaku.CommandDanmakuType
import com.android.purebilibili.feature.video.danmaku.VoteDanmakuKind
import com.android.purebilibili.feature.video.danmaku.GRADE_STAR_SCORES
import com.android.purebilibili.feature.video.danmaku.VoteOption

/** Owned by the video session, not by the viewport or currently visible command list. */
internal class CommandDanmakuOverlayState {
    private val dismissed = mutableStateMapOf<String, Boolean>()
    private val selections = mutableStateMapOf<String, VoteOption>()
    private val submissions = mutableStateMapOf<String, Boolean>()
    private val gradeSummaries = mutableStateMapOf<String, GradeDanmakuSummary>()

    fun isDismissed(id: String): Boolean = dismissed[id] == true
    fun selection(id: String): VoteOption? = selections[id]
    fun dismiss(id: String) {
        dismissed[id] = true
    }

    fun gradeSummary(item: CommandDanmakuItem): GradeDanmakuSummary? =
        gradeSummaries[item.id] ?: item.gradeSummary?.normalized()

    fun gradeScore(item: CommandDanmakuItem): Int? =
        selections[item.id]?.score ?: gradeSummary(item)?.userScore

    fun isSubmitting(id: String): Boolean = submissions[id] == true

    fun beginGradeSubmission(item: CommandDanmakuItem, option: VoteOption): Boolean {
        if (item.voteKind != VoteDanmakuKind.GRADE ||
            option.score !in GRADE_STAR_SCORES ||
            isDismissed(item.id) || isSubmitting(item.id) || gradeScore(item) != null
        ) return false
        submissions[item.id] = true
        return true
    }

    fun completeGradeSubmission(item: CommandDanmakuItem, option: VoteOption) {
        if (!isSubmitting(item.id)) return
        selections[item.id] = option
        // The POST returns only acceptance. Old aggregates must not masquerade as refreshed stats.
        gradeSummaries[item.id] = GradeDanmakuSummary(userScore = option.score).normalized()
        submissions.remove(item.id)
    }

    fun beginVoteSubmission(item: CommandDanmakuItem): Boolean {
        if (item.type != CommandDanmakuType.VOTE || item.voteKind == VoteDanmakuKind.GRADE ||
            isDismissed(item.id) || isSubmitting(item.id) ||
            selection(item.id) != null || item.voteSelectedIndex != null
        ) return false
        submissions[item.id] = true
        return true
    }

    fun completeVoteSubmission(item: CommandDanmakuItem, option: VoteOption) {
        if (!isSubmitting(item.id)) return
        selections[item.id] = option
        submissions.remove(item.id)
    }

    fun endSubmission(id: String) {
        submissions.remove(id)
    }

    fun updateGradeSummary(id: String, summary: GradeDanmakuSummary) {
        gradeSummaries[id] = summary.normalized()
    }
}

@Composable
internal fun rememberCommandDanmakuOverlayState(contentKey: Any?): CommandDanmakuOverlayState =
    remember(contentKey) { CommandDanmakuOverlayState() }
