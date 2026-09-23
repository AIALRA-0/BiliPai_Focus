package com.android.purebilibili.feature.video.screen

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerCollapseAutoPausePolicyTest {

    @Test
    fun pausesOnlyWhenEnabledCollapsedAndPlaying() {
        assertTrue(
            shouldAutoPauseOnPlayerCollapse(
                autoPauseEnabled = true,
                isPlayerCollapsed = true,
                isPlaying = true,
            )
        )
        assertFalse(
            shouldAutoPauseOnPlayerCollapse(
                autoPauseEnabled = false,
                isPlayerCollapsed = true,
                isPlaying = true,
            )
        )
        assertFalse(
            shouldAutoPauseOnPlayerCollapse(
                autoPauseEnabled = true,
                isPlayerCollapsed = false,
                isPlaying = true,
            )
        )
        assertFalse(
            shouldAutoPauseOnPlayerCollapse(
                autoPauseEnabled = true,
                isPlayerCollapsed = true,
                isPlaying = false,
            )
        )
    }

    @Test
    fun resumesOnlyWhenExpandingAfterAutoPause() {
        assertTrue(
            shouldAutoResumeOnPlayerExpand(
                autoPauseEnabled = true,
                isPlayerCollapsed = false,
                wasAutoPausedByCollapse = true,
            )
        )
        assertFalse(
            shouldAutoResumeOnPlayerExpand(
                autoPauseEnabled = true,
                isPlayerCollapsed = false,
                wasAutoPausedByCollapse = false,
            )
        )
        assertFalse(
            shouldAutoResumeOnPlayerExpand(
                autoPauseEnabled = true,
                isPlayerCollapsed = true,
                wasAutoPausedByCollapse = true,
            )
        )
        assertFalse(
            shouldAutoResumeOnPlayerExpand(
                autoPauseEnabled = false,
                isPlayerCollapsed = false,
                wasAutoPausedByCollapse = true,
            )
        )
    }

    @Test
    fun portraitFullscreenDoesNotTreatHiddenInlinePlayerAsCollapsed() {
        assertFalse(
            shouldAutoPauseOnPlayerCollapse(
                autoPauseEnabled = true,
                isPlayerCollapsed = true,
                isPlaying = true,
                isPortraitFullscreen = true,
            )
        )
        assertTrue(
            shouldAutoResumeOnPlayerExpand(
                autoPauseEnabled = true,
                isPlayerCollapsed = true,
                wasAutoPausedByCollapse = true,
                isPortraitFullscreen = true,
            )
        )
    }
}
