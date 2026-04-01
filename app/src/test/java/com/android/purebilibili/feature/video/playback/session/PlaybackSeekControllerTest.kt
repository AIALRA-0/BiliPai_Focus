package com.android.purebilibili.feature.video.playback.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlaybackSeekControllerTest {

    @Test
    fun startScrubbing_entersScrubbingStateWithClampedProgress() {
        val state = startPlaybackSeekSession(progress = 1.4f)

        assertTrue(state.isScrubbing)
        assertEquals(1f, state.dragProgress)
        assertNull(state.pendingSettledProgress)
    }

    @Test
    fun finishScrubbing_commitsCurrentDragProgressAndKeepsSettledTarget() {
        val state = updatePlaybackSeekSession(
            state = startPlaybackSeekSession(progress = 0.2f),
            progress = 0.72f
        )

        val result = finishPlaybackSeekSession(state)

        assertFalse(result.state.isScrubbing)
        assertEquals(0.72f, result.committedProgress)
        assertEquals(0.72f, result.state.pendingSettledProgress)
    }

    @Test
    fun cancelScrubbing_clearsPendingAndReturnsToIdle() {
        val state = cancelPlaybackSeekSession(
            state = PlaybackSeekUiState(
                isScrubbing = true,
                dragProgress = 0.55f,
                pendingSettledProgress = 0.8f
            )
        )

        assertFalse(state.isScrubbing)
        assertNull(state.pendingSettledProgress)
        assertEquals(0.55f, state.dragProgress)
    }

    @Test
    fun settleAgainstPlaybackProgress_clearsPendingWhenPlaybackCatchesUp() {
        val state = settlePlaybackSeekSession(
            state = PlaybackSeekUiState(
                isScrubbing = false,
                dragProgress = 0.72f,
                pendingSettledProgress = 0.72f
            ),
            playbackProgress = 0.719f
        )

        assertNull(state.pendingSettledProgress)
    }

    @Test
    fun resolveDisplayProgress_prefersSettledProgressUntilPlaybackCatchesUp() {
        val display = resolvePlaybackSeekDisplayProgress(
            playbackProgress = 0.15f,
            state = PlaybackSeekUiState(
                isScrubbing = false,
                dragProgress = 0.72f,
                pendingSettledProgress = 0.72f
            )
        )

        assertEquals(0.72f, display)
    }
}
