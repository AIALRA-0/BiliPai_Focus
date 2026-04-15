package com.android.purebilibili.feature.video.playback.session

import androidx.media3.common.Player
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlaybackSeekControllerTest {

    @Test
    fun syncFromPlayback_initializesSliderPositionWhenIdle() {
        val state = syncPlaybackSeekSession(
            state = PlaybackSeekSessionState(),
            playbackPositionMs = 12_000L
        )

        assertEquals(12_000L, state.playbackPositionMs)
        assertEquals(12_000L, state.sliderPositionMs)
        assertFalse(state.isSliderMoving)
    }

    @Test
    fun syncFromPlayback_doesNotOverrideSliderWhileUserIsDragging() {
        val draggingState = updatePlaybackSeekInteraction(
            state = startPlaybackSeekInteraction(
                state = syncPlaybackSeekSession(
                    state = PlaybackSeekSessionState(),
                    playbackPositionMs = 10_000L
                )
            ),
            positionMs = 24_000L
        )

        val synced = syncPlaybackSeekSession(
            state = draggingState,
            playbackPositionMs = 11_000L
        )

        assertEquals(11_000L, synced.playbackPositionMs)
        assertEquals(24_000L, synced.sliderPositionMs)
        assertTrue(synced.isSliderMoving)
    }

    @Test
    fun finishSeek_keepsCommittedSliderUntilPlaybackCatchesUp() {
        val draggingState = updatePlaybackSeekInteraction(
            state = startPlaybackSeekInteraction(
                state = syncPlaybackSeekSession(
                    state = PlaybackSeekSessionState(),
                    playbackPositionMs = 10_000L
                )
            ),
            positionMs = 25_000L
        )

        val result = finishPlaybackSeekInteraction(draggingState)
        val staleSync = syncPlaybackSeekSession(
            state = result.state,
            playbackPositionMs = 1_000L
        )
        val settledSync = syncPlaybackSeekSession(
            state = staleSync,
            playbackPositionMs = 24_700L
        )

        assertEquals(25_000L, result.committedPositionMs)
        assertEquals(25_000L, staleSync.sliderPositionMs)
        assertEquals(25_000L, staleSync.pendingSeekPositionMs)
        assertEquals(24_700L, settledSync.sliderPositionMs)
        assertNull(settledSync.pendingSeekPositionMs)
    }

    @Test
    fun cancelSeek_restoresLastPlaybackPosition() {
        val draggingState = updatePlaybackSeekInteraction(
            state = startPlaybackSeekInteraction(
                state = syncPlaybackSeekSession(
                    state = PlaybackSeekSessionState(),
                    playbackPositionMs = 8_000L
                )
            ),
            positionMs = 30_000L
        )

        val cancelled = cancelPlaybackSeekInteraction(draggingState)

        assertFalse(cancelled.isSliderMoving)
        assertEquals(8_000L, cancelled.playbackPositionMs)
        assertEquals(8_000L, cancelled.sliderPositionMs)
        assertNull(cancelled.pendingSeekPositionMs)
    }

    @Test
    fun finishSeek_preservesResumeIntentCapturedAtInteractionStart() {
        val draggingState = updatePlaybackSeekInteraction(
            state = startPlaybackSeekInteraction(
                state = syncPlaybackSeekSession(
                    state = PlaybackSeekSessionState(),
                    playbackPositionMs = 8_000L
                ),
                shouldResumePlayback = true
            ),
            positionMs = 30_000L
        )

        val result = finishPlaybackSeekInteraction(draggingState)

        assertEquals(true, result.shouldResumePlayback)
        assertEquals(true, result.state.shouldResumePlayback)
    }

    @Test
    fun finishSeek_keepsResumeIntentUnsetWhenInteractionStartedWithoutOne() {
        val draggingState = updatePlaybackSeekInteraction(
            state = startPlaybackSeekInteraction(
                state = syncPlaybackSeekSession(
                    state = PlaybackSeekSessionState(),
                    playbackPositionMs = 8_000L
                )
            ),
            positionMs = 30_000L
        )

        val result = finishPlaybackSeekInteraction(draggingState)

        assertNull(result.shouldResumePlayback)
        assertNull(result.state.shouldResumePlayback)
    }

    @Test
    fun startSeekInteractionForPlayerCapturesResumeIntentFromCurrentPlaybackState() {
        val player = mockk<Player>()
        every { player.playWhenReady } returns true
        every { player.playbackState } returns Player.STATE_READY

        val state = startPlaybackSeekInteraction(
            state = syncPlaybackSeekSession(
                state = PlaybackSeekSessionState(),
                playbackPositionMs = 8_000L
            ),
            player = player,
            positionMs = 30_000L
        )

        assertEquals(30_000L, state.sliderPositionMs)
        assertTrue(state.isSliderMoving)
        assertEquals(true, state.shouldResumePlayback)
    }
}
