package com.android.purebilibili.feature.video.screen

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoDetailEntryVisualPolicyTest {

    @Test
    fun `disabled transition should not apply blur or scrim`() {
        val frame = resolveVideoDetailEntryVisualFrame(
            rawProgress = 0.1f,
            transitionEnabled = false,
            fallbackBlurEnabled = false,
            maxBlurRadiusPx = 28f
        )

        assertEquals(1f, frame.contentAlpha)
        assertEquals(0f, frame.scrimAlpha)
        assertEquals(0f, frame.blurRadiusPx)
    }

    @Test
    fun `entry frame should stay fully opaque and unblurred in shared transition mode`() {
        val start = resolveVideoDetailEntryVisualFrame(
            rawProgress = -0.2f,
            transitionEnabled = true,
            fallbackBlurEnabled = true,
            maxBlurRadiusPx = 20f
        )
        val mid = resolveVideoDetailEntryVisualFrame(
            rawProgress = 0.5f,
            transitionEnabled = true,
            fallbackBlurEnabled = true,
            maxBlurRadiusPx = 20f
        )
        val end = resolveVideoDetailEntryVisualFrame(
            rawProgress = 1.2f,
            transitionEnabled = true,
            fallbackBlurEnabled = true,
            maxBlurRadiusPx = 20f
        )

        assertEquals(0f, start.blurRadiusPx)
        assertEquals(0f, mid.blurRadiusPx)
        assertEquals(0f, end.blurRadiusPx)
        assertEquals(0f, start.scrimAlpha)
        assertEquals(0f, mid.scrimAlpha)
        assertEquals(0f, end.scrimAlpha)
        assertEquals(1f, start.contentAlpha)
        assertEquals(1f, mid.contentAlpha)
        assertEquals(1f, end.contentAlpha)
    }

    @Test
    fun `disabled shared transition fallback applies light blur that settles to zero`() {
        val start = resolveVideoDetailEntryVisualFrame(
            rawProgress = 0f,
            transitionEnabled = false,
            fallbackBlurEnabled = true,
            maxBlurRadiusPx = 24f
        )
        val mid = resolveVideoDetailEntryVisualFrame(
            rawProgress = 0.5f,
            transitionEnabled = false,
            fallbackBlurEnabled = true,
            maxBlurRadiusPx = 24f
        )
        val end = resolveVideoDetailEntryVisualFrame(
            rawProgress = 1f,
            transitionEnabled = false,
            fallbackBlurEnabled = true,
            maxBlurRadiusPx = 24f
        )

        assertEquals(6f, start.blurRadiusPx)
        assertEquals(3f, mid.blurRadiusPx)
        assertEquals(0f, end.blurRadiusPx)
        assertEquals(0f, start.scrimAlpha)
        assertEquals(1f, start.contentAlpha)
    }

    @Test
    fun `fallback blur is capped as a lightweight effect`() {
        val frame = resolveVideoDetailEntryVisualFrame(
            rawProgress = 0f,
            transitionEnabled = false,
            fallbackBlurEnabled = true,
            maxBlurRadiusPx = 100f
        )

        assertEquals(6f, frame.blurRadiusPx)
    }
}
