package com.android.purebilibili.feature.download

import androidx.media3.common.Player
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OfflineVideoPlaybackPolicyTest {

    @Test
    fun horizontalVideo_startsFullscreenByDefault() {
        assertTrue(
            resolveOfflineVideoStartFullscreen(
                isAudioOnly = false,
                isVerticalVideo = false
            )
        )
    }

    @Test
    fun audioOnlyAndVerticalVideo_doNotStartFullscreen() {
        assertFalse(resolveOfflineVideoStartFullscreen(isAudioOnly = true, isVerticalVideo = false))
        assertFalse(resolveOfflineVideoStartFullscreen(isAudioOnly = false, isVerticalVideo = true))
    }

    @Test
    fun seekFromEndedState_restartsPlayback() {
        assertTrue(
            shouldResumePlaybackAfterOfflineSeek(
                playbackState = Player.STATE_ENDED,
                wasPlayingBeforeSeek = false,
                targetPositionMs = 15_000L,
                durationMs = 120_000L
            )
        )
    }

    @Test
    fun pausedSeek_keepsPausedWhenPlayerDidNotEnd() {
        assertFalse(
            shouldResumePlaybackAfterOfflineSeek(
                playbackState = Player.STATE_READY,
                wasPlayingBeforeSeek = false,
                targetPositionMs = 15_000L,
                durationMs = 120_000L
            )
        )
    }
}
