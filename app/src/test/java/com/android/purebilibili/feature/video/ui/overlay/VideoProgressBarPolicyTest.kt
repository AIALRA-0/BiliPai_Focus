package com.android.purebilibili.feature.video.ui.overlay

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    @Test
    fun dragCommitPosition_usesLatestDragTarget() {
        assertEquals(
            54_000L,
            resolveSeekDragCommitPositionMs(
                dragStartPositionMs = 18_000L,
                latestDragPositionMs = 54_000L
            )
        )
    }

    @Test
    fun dragCommitPosition_allowsDraggingBackToStart() {
        assertEquals(
            0L,
            resolveSeekDragCommitPositionMs(
                dragStartPositionMs = 18_000L,
                latestDragPositionMs = 0L
            )
        )
    }

    @Test
    fun progressFraction_clampsOutsideBounds() {
        assertEquals(0f, resolveProgressFraction(positionMs = -1L, durationMs = 60_000L))
        assertEquals(1f, resolveProgressFraction(positionMs = 80_000L, durationMs = 60_000L))
    }

    @Test
    fun seekPositionFromTouch_mapsTrackSpaceIntoDuration() {
        assertEquals(
            45_000L,
            resolveSeekPositionFromTouch(
                touchX = 150f,
                containerWidthPx = 200f,
                durationMs = 60_000L
            )
        )
    }

    @Test
    fun progressBarPointerCancellation_cancelsOnlyActiveDragInteraction() {
        assertEquals(true, shouldCancelSeekDragOnPointerInputCompletion(dragInProgress = true))
        assertEquals(false, shouldCancelSeekDragOnPointerInputCompletion(dragInProgress = false))
    }

    @Test
    fun progressBarDragHandler_cleansUpActiveDragWhenPointerInputIsCancelled() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/ui/overlay/BottomControlBar.kt")
            .readText()

        assertTrue(
            source.contains("finally") &&
                source.contains("shouldCancelSeekDragOnPointerInputCompletion(dragInProgress)") &&
                source.contains("currentOnSeekDragCancel()"),
            "Progress bar drag handling must cancel an active seek scrub if the pointerInput coroutine is cancelled before onDragEnd/onDragCancel."
        )
    }

    @Test
    fun seekGestureHost_staysOnSharedOuterContainer_evenWhenChapterLabelExists() {
        assertEquals(
            VideoProgressBarSeekGestureHost.SharedOuterContainer,
            resolveVideoProgressBarSeekGestureHost(hasChapterLabel = false)
        )
        assertEquals(
            VideoProgressBarSeekGestureHost.SharedOuterContainer,
            resolveVideoProgressBarSeekGestureHost(hasChapterLabel = true)
        )
    }

    @Test
    fun progressBar_withChapter_keepsSeekGesturesOnOuterContainer_notTrackSibling() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/ui/overlay/BottomControlBar.kt")
            .readText()
        val progressBarStart = source.indexOf("fun VideoProgressBar(")
        assertTrue(progressBarStart >= 0, "VideoProgressBar composable must exist")
        val progressBarSource = source.substring(progressBarStart)

        assertTrue(
            progressBarSource.contains("resolveVideoProgressBarSeekGestureHost("),
            "Progress bar must resolve a shared outer seek-gesture host."
        )
        assertTrue(
            progressBarSource.contains("wrapContentWidth()") &&
                progressBarSource.contains("LocalMinimumInteractiveComponentSize"),
            "Chapter row must avoid full-width 48dp touch expansion over the seek track."
        )
        assertTrue(
            progressBarSource.contains("detectTapGestures { onChapterClick() }") ||
                progressBarSource.contains("detectTapGestures { onChapterClick("),
            "Chapter label should use tap gestures without Material clickable min-touch expansion."
        )
    }
}
