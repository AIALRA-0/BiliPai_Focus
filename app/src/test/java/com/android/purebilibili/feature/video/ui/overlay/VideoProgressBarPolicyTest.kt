package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertEquals

class VideoProgressBarPolicyTest {

    @Test
    fun seekableDuration_usesFallbackWhenPlaybackDurationIsUnset() {
        assertEquals(
            120_000L,
            resolveSeekableDurationMs(
                playbackDurationMs = 0L,
                fallbackDurationMs = 120_000L
            )
        )
    }

    @Test
    fun seekableDuration_prefersPlaybackDurationWhenItIsValid() {
        assertEquals(
            95_000L,
            resolveSeekableDurationMs(
                playbackDurationMs = 95_000L,
                fallbackDurationMs = 120_000L
            )
        )
    }

    @Test
    fun displayedProgress_prefersPlaybackTransitionTarget_untilPlayerCatchesUp() {
        assertEquals(
            PlayerProgress(current = 25_000L, duration = 120_000L, buffered = 30_000L),
            resolveDisplayedPlayerProgress(
                progress = PlayerProgress(current = 1_200L, duration = 120_000L, buffered = 30_000L),
                previewPositionMs = null,
                previewActive = false,
                playbackTransitionPositionMs = 25_000L
            )
        )
    }

    @Test
    fun seekPreviewTarget_usesLiveDragPosition_whileScrubbing() {
        assertEquals(
            61_000L,
            resolveSeekPreviewTargetPositionMs(
                displayPositionMs = 32_000L,
                dragTargetPositionMs = 61_000L,
                isSeekScrubbing = true
            )
        )
    }

    @Test
    fun seekPreviewTarget_fallsBackToDisplayedPosition_whenNotScrubbing() {
        assertEquals(
            32_000L,
            resolveSeekPreviewTargetPositionMs(
                displayPositionMs = 32_000L,
                dragTargetPositionMs = 61_000L,
                isSeekScrubbing = false
            )
        )
    }

}
