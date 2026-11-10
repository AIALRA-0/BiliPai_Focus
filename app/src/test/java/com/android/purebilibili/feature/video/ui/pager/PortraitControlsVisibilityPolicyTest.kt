package com.android.purebilibili.feature.video.ui.pager

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PortraitControlsVisibilityPolicyTest {
    @Test
    fun playingVisiblePageCanHideItsControlsWhenIdle() {
        assertTrue(shouldAutoHidePortraitControls(true, true, true, false, false))
    }

    @Test
    fun pausedInactiveAndAlreadyHiddenPagesNeverStartAnIdleCountdown() {
        assertFalse(shouldAutoHidePortraitControls(true, false, true, false, false))
        assertFalse(shouldAutoHidePortraitControls(false, true, true, false, false))
        assertFalse(shouldAutoHidePortraitControls(true, true, false, false, false))
    }

    @Test
    fun scrubbingHoldingAndOpenMenusSuspendAutoHideUntilInteractionEnds() {
        assertFalse(shouldAutoHidePortraitControls(true, true, true, true, false))
        assertFalse(shouldAutoHidePortraitControls(true, true, true, false, true))
        assertTrue(shouldAutoHidePortraitControls(true, true, true, false, false))
    }
}
