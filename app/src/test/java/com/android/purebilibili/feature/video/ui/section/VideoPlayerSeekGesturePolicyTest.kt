package com.android.purebilibili.feature.video.ui.section

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VideoPlayerSeekGesturePolicyTest {

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

        assertEquals(3_000L, delta)
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

        assertEquals(2_250L, delta)
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

        assertEquals(250L, delta)
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
}
