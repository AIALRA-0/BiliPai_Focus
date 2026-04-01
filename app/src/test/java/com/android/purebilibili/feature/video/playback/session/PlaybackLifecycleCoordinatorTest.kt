package com.android.purebilibili.feature.video.playback.session

import androidx.media3.common.Player
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackLifecycleCoordinatorTest {

    @Test
    fun pauseDecision_keepsPlayingForPip() {
        val decision = resolvePlaybackPauseDecision(
            isMiniMode = false,
            isPip = true,
            isBackgroundAudio = false,
            wasPlaybackActive = false,
            hasRecentUserLeaveHint = true
        )

        assertTrue(decision.shouldContinuePlayback)
        assertFalse(decision.shouldPausePlayback)
    }

    @Test
    fun pauseDecision_marksBackgroundAudioOnlyWhenUserActuallyLeftApp() {
        val decision = resolvePlaybackPauseDecision(
            isMiniMode = false,
            isPip = false,
            isBackgroundAudio = true,
            wasPlaybackActive = true,
            hasRecentUserLeaveHint = true
        )

        assertTrue(decision.shouldContinuePlayback)
        assertTrue(decision.shouldMarkBackgroundAudioSession)
    }

    @Test
    fun resumeDecision_doesNotResumeWhenNavigationLeaveWasMarked() {
        val decision = resolvePlaybackResumeDecision(
            wasPlaybackActive = true,
            isPlaying = false,
            playWhenReady = false,
            playbackState = Player.STATE_READY,
            currentVolume = 0f,
            shouldEnsureAudibleOnForeground = true,
            isLeavingByNavigation = true
        )

        assertFalse(decision.shouldResumePlayback)
        assertFalse(decision.shouldRestoreVolume)
    }

    @Test
    fun resumeDecision_restoresVolumeWhenForegroundShouldBeAudible() {
        val decision = resolvePlaybackResumeDecision(
            wasPlaybackActive = false,
            isPlaying = false,
            playWhenReady = false,
            playbackState = Player.STATE_READY,
            currentVolume = 0f,
            shouldEnsureAudibleOnForeground = true,
            isLeavingByNavigation = false
        )

        assertFalse(decision.shouldResumePlayback)
        assertTrue(decision.shouldRestoreVolume)
    }
}
