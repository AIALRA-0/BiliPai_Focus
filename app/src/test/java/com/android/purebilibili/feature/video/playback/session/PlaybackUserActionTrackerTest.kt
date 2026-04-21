package com.android.purebilibili.feature.video.playback.session

import androidx.media3.common.Player
import com.android.purebilibili.feature.video.ui.overlay.PlaybackUserActionType
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackUserActionTrackerTest {

    @Test
    fun playResponse_acceptsExplicitPlayWhenBufferingStarts() {
        assertTrue(
            hasPlaybackUserActionReceivedResponse(
                action = PendingPlaybackUserAction(
                    type = PlaybackUserActionType.PLAY,
                    requestedAtMs = 0L,
                    initialPlaybackState = Player.STATE_IDLE,
                    initialPlayWhenReady = false,
                    initialIsPlaying = false,
                    initialPositionMs = 0L
                ),
                playbackState = Player.STATE_BUFFERING,
                playWhenReady = false,
                isPlaying = false,
                currentPositionMs = 0L
            )
        )
    }

    @Test
    fun playResponse_doesNotNeedTrackingWhenBufferingAlreadyExpressesPlayIntent() {
        assertTrue(
            hasPlaybackAlreadyReachedUserIntent(
                playbackState = Player.STATE_BUFFERING,
                playWhenReady = true,
                isPlaying = false,
                type = PlaybackUserActionType.PLAY
            )
        )
    }

    @Test
    fun pauseResponse_requiresPlayerToActuallyLookPaused() {
        assertFalse(
            hasPlaybackAlreadyReachedUserIntent(
                playbackState = Player.STATE_READY,
                playWhenReady = true,
                isPlaying = false,
                type = PlaybackUserActionType.PAUSE
            )
        )
    }
}
