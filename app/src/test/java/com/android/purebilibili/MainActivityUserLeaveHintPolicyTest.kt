package com.android.purebilibili

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MainActivityUserLeaveHintPolicyTest {

    @Test
    fun doesNotForceStopWhenUserLeavesAppFromVideoDetailEvenIfStopOnExitEnabled() {
        assertFalse(
            shouldForceStopPlaybackOnUserLeaveHint(
                isInVideoDetail = true,
                stopPlaybackOnExit = true,
                shouldTriggerPip = false
            )
        )
    }

    @Test
    fun doesNotForceStopWhenNotInVideoDetail() {
        assertFalse(
            shouldForceStopPlaybackOnUserLeaveHint(
                isInVideoDetail = false,
                stopPlaybackOnExit = true,
                shouldTriggerPip = false
            )
        )
    }

    @Test
    fun doesNotForceStopWhenStopOnExitDisabled() {
        assertFalse(
            shouldForceStopPlaybackOnUserLeaveHint(
                isInVideoDetail = true,
                stopPlaybackOnExit = false,
                shouldTriggerPip = false
            )
        )
    }

    @Test
    fun doesNotForceStopWhenPipWillTrigger() {
        assertFalse(
            shouldForceStopPlaybackOnUserLeaveHint(
                isInVideoDetail = true,
                stopPlaybackOnExit = true,
                shouldTriggerPip = true
            )
        )
    }

    @Test
    fun restoresPlaybackRouteResumeStateWhenVideoOrAudioRouteIsActive() {
        assertTrue(
            shouldRestorePlaybackRouteStateOnResume(
                isPlaybackRouteActive = true
            )
        )
        assertFalse(
            shouldRestorePlaybackRouteStateOnResume(
                isPlaybackRouteActive = false
            )
        )
    }

    @Test
    fun restoresMutedPlayerVolumeOnlyWhenPlaybackPlayerWasInternallyMuted() {
        assertTrue(
            shouldRestoreMutedPlaybackPlayerVolumeOnResume(
                playerVolume = 0f
            )
        )
        assertTrue(
            shouldRestoreMutedPlaybackPlayerVolumeOnResume(
                playerVolume = -0.1f
            )
        )
        assertFalse(
            shouldRestoreMutedPlaybackPlayerVolumeOnResume(
                playerVolume = 0.3f
            )
        )
    }
}
