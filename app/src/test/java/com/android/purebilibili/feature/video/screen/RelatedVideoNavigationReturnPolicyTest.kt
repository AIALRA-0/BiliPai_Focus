package com.android.purebilibili.feature.video.screen

import com.android.purebilibili.core.ui.transition.VideoCardTransitionBackgroundPhase
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RelatedVideoNavigationReturnPolicyTest {
    @Test
    fun restoredParentHeldSessionAllowsAnotherRecommendation() {
        assertTrue(canReleaseRelatedVideoNavigation(VideoCardTransitionBackgroundPhase.HELD, false, false))
        assertTrue(canReleaseRelatedVideoNavigation(VideoCardTransitionBackgroundPhase.IDLE, false, false))
    }

    @Test
    fun openingReturningAndCancelledBackGestureKeepTheNavigationGuard() {
        assertFalse(canReleaseRelatedVideoNavigation(VideoCardTransitionBackgroundPhase.OPENING, false, false))
        assertFalse(canReleaseRelatedVideoNavigation(VideoCardTransitionBackgroundPhase.RETURNING, false, false))
        for (phase in VideoCardTransitionBackgroundPhase.entries) {
            assertFalse(canReleaseRelatedVideoNavigation(phase, true, false))
            assertFalse(canReleaseRelatedVideoNavigation(phase, false, true))
        }
    }
}
