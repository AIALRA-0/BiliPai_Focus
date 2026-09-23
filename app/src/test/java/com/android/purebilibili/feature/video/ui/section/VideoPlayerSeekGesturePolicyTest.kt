package com.android.purebilibili.feature.video.ui.section

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VideoPlayerSeekGesturePolicyTest {

    @Test
    fun `fullscreen overlay reuses horizontal seek direction policy`() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/FullscreenPlayerOverlay.kt"),
            File("src/main/java/com/android/purebilibili/feature/video/ui/overlay/FullscreenPlayerOverlay.kt"),
        ).first { it.exists() }.readText()

        assertTrue(source.contains("shouldEngageHorizontalPlayerSeek"))
    }

    @Test
    fun `player seek requires horizontal dominance over vertical drift`() {
        assertTrue(
            shouldEngageHorizontalPlayerSeek(
                totalDragDistanceX = 80f,
                totalDragDistanceY = 20f,
            )
        )
        assertFalse(
            shouldEngageHorizontalPlayerSeek(
                totalDragDistanceX = 80f,
                totalDragDistanceY = 70f,
            )
        )
    }

    @Test
    fun `fullscreen fixed setting uses precise proportional range`() {
        val delta = resolveHorizontalSeekDeltaMs(
            isFullscreen = true,
            fullscreenSwipeSeekEnabled = true,
            totalDragDistanceX = 240f,
            containerWidthPx = 800f,
            fullscreenSwipeSeekSeconds = 10,
            inlineSwipeSeekSeconds = 30,
            gestureSensitivity = 1f
        )

        assertEquals(6_000L, delta)
    }

    @Test
    fun `fullscreen precise range caps long drags at selected maximum`() {
        val delta = resolveHorizontalSeekDeltaMs(
            isFullscreen = true,
            fullscreenSwipeSeekEnabled = true,
            totalDragDistanceX = 1200f,
            containerWidthPx = 800f,
            fullscreenSwipeSeekSeconds = 10,
            inlineSwipeSeekSeconds = 30,
            gestureSensitivity = 1f
        )

        assertEquals(10_000L, delta)
    }

    @Test
    fun `fullscreen falls back to linear when setting disabled`() {
        val delta = resolveHorizontalSeekDeltaMs(
            isFullscreen = true,
            fullscreenSwipeSeekEnabled = false,
            totalDragDistanceX = 110f,
            containerWidthPx = 800f,
            fullscreenSwipeSeekSeconds = 15,
            inlineSwipeSeekSeconds = 30,
            gestureSensitivity = 1f
        )

        assertEquals(22_000L, delta)
    }

    @Test
    fun `portrait uses configurable precise seek range regardless of fullscreen setting`() {
        val delta = resolveHorizontalSeekDeltaMs(
            isFullscreen = false,
            fullscreenSwipeSeekEnabled = true,
            totalDragDistanceX = 50f,
            containerWidthPx = 800f,
            fullscreenSwipeSeekSeconds = 30,
            inlineSwipeSeekSeconds = 30,
            gestureSensitivity = 1.2f
        )

        assertEquals(4_500L, delta)
    }

    @Test
    fun `portrait precise seek range caps long drags`() {
        val delta = resolveHorizontalSeekDeltaMs(
            isFullscreen = false,
            fullscreenSwipeSeekEnabled = true,
            totalDragDistanceX = 1200f,
            containerWidthPx = 800f,
            fullscreenSwipeSeekSeconds = 30,
            inlineSwipeSeekSeconds = 15,
            gestureSensitivity = 2f
        )

        assertEquals(15_000L, delta)
    }

    @Test
    fun `fullscreen precise range keeps very small drags below commit threshold`() {
        val delta = resolveHorizontalSeekDeltaMs(
            isFullscreen = true,
            fullscreenSwipeSeekEnabled = true,
            totalDragDistanceX = 20f,
            containerWidthPx = 800f,
            fullscreenSwipeSeekSeconds = 10,
            inlineSwipeSeekSeconds = 30,
            gestureSensitivity = 1f
        )

        assertEquals(500L, delta)
    }

    @Test
    fun `configured seek range reaches selected maximum after half screen drag`() {
        val fullscreenDelta = resolveHorizontalSeekDeltaMs(
            isFullscreen = true,
            fullscreenSwipeSeekEnabled = true,
            totalDragDistanceX = 400f,
            containerWidthPx = 800f,
            fullscreenSwipeSeekSeconds = 30,
            inlineSwipeSeekSeconds = 60,
            gestureSensitivity = 1f
        )
        val inlineDelta = resolveHorizontalSeekDeltaMs(
            isFullscreen = false,
            fullscreenSwipeSeekEnabled = true,
            totalDragDistanceX = 400f,
            containerWidthPx = 800f,
            fullscreenSwipeSeekSeconds = 30,
            inlineSwipeSeekSeconds = 60,
            gestureSensitivity = 1f
        )

        assertEquals(30_000L, fullscreenDelta)
        assertEquals(60_000L, inlineDelta)
    }

    @Test
    fun `fullscreen fixed-step seek waits for loaded step setting`() {
        val delta = resolveHorizontalSeekDeltaMs(
            isFullscreen = true,
            fullscreenSwipeSeekEnabled = true,
            totalDragDistanceX = 110f,
            containerWidthPx = 800f,
            fullscreenSwipeSeekSeconds = null,
            inlineSwipeSeekSeconds = 30,
            gestureSensitivity = 1f
        )

        assertNull(delta)
    }

    @Test
    fun `gesture seek commit requires meaningful delta`() {
        assertEquals(
            false,
            shouldCommitGestureSeek(
                currentPositionMs = 100_000L,
                targetPositionMs = 100_500L
            )
        )
        assertEquals(
            true,
            shouldCommitGestureSeek(
                currentPositionMs = 100_000L,
                targetPositionMs = 102_000L
            )
        )
    }

    @Test
    fun `seek step haptic ticks once per second of target progress`() {
        assertFalse(
            shouldTriggerSeekStepHaptic(
                previousTargetMs = 10_200L,
                currentTargetMs = 10_800L
            )
        )
        assertTrue(
            shouldTriggerSeekStepHaptic(
                previousTargetMs = 10_200L,
                currentTargetMs = 11_100L
            )
        )
        assertTrue(
            shouldTriggerSeekStepHaptic(
                previousTargetMs = 11_100L,
                currentTargetMs = 9_900L
            )
        )
        assertFalse(
            shouldTriggerSeekStepHaptic(
                previousTargetMs = 5_000L,
                currentTargetMs = 5_000L
            )
        )
    }
}
