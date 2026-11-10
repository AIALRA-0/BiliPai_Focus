package com.android.purebilibili.core.ui.transition

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoCardSourceWorkBudgetTest {
    @Test
    fun feedHeavyWorkWaitsThroughOpenHiddenPreviewReturnAndRestore() {
        for (exposure in listOf(
            VideoCardTransitionExposure.Opening,
            VideoCardTransitionExposure.SettledHidden,
            VideoCardTransitionExposure.BackPreview,
            VideoCardTransitionExposure.Returning,
            VideoCardTransitionExposure.Restoring,
        )) {
            assertTrue(shouldDeferVideoCardSourceHeavyWork(exposure))
        }
        assertFalse(shouldDeferVideoCardSourceHeavyWork(VideoCardTransitionExposure.Idle))
    }

    @Test
    fun predictiveCancellationKeepsCoveredFeedDeferredUntilItIsActuallyVisible() {
        val restoredDetail = resolveVideoCardTransitionExposure(
            phase = VideoCardTransitionBackgroundPhase.HELD,
            predictiveBackInProgress = false,
            gestureRestoreInProgress = false,
        )
        assertTrue(shouldDeferVideoCardSourceHeavyWork(restoredDetail))
        val returnedFeed = resolveVideoCardTransitionExposure(
            phase = VideoCardTransitionBackgroundPhase.IDLE,
            predictiveBackInProgress = false,
            gestureRestoreInProgress = false,
        )
        assertFalse(shouldDeferVideoCardSourceHeavyWork(returnedFeed))
    }
}
