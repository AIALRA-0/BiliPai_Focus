package com.android.purebilibili.navigation3.predictiveback

import top.yukonga.miuix.kmp.nav.core.NavBackCompletionPolicy

// Enter floating-card mode once sufficiently shrunk. Leaving it requires an explicit
// expansion toward the detail page, not a reverse velocity or distance from the source.
internal object VideoCardBackCompletionPolicy : NavBackCompletionPolicy {
    private const val FLOATING_CARD_PROGRESS = 0.65f
    private const val RESTORE_DETAIL_PROGRESS = 0.35f

    override fun shouldCommit(progress: Float, peakProgress: Float, velocity: Float): Boolean {
        if (!progress.isFinite() || !peakProgress.isFinite() || !velocity.isFinite()) return false
        if (peakProgress >= FLOATING_CARD_PROGRESS) return progress > RESTORE_DETAIL_PROGRESS
        return velocity > -1f
    }

    override fun shouldCommitOnCancel(progress: Float, peakProgress: Float): Boolean =
        progress.isFinite() && peakProgress.isFinite() &&
            peakProgress >= FLOATING_CARD_PROGRESS && progress > RESTORE_DETAIL_PROGRESS
}
