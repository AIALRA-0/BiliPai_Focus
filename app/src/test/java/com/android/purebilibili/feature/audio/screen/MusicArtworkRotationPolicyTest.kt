package com.android.purebilibili.feature.audio.screen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MusicArtworkRotationPolicyTest {

    @Test
    fun rotatesOnlyWhilePlayingWithoutReduceMotion() {
        assertTrue(shouldRotateMusicArtwork(isPlaying = true, reduceMotion = false))
        assertFalse(shouldRotateMusicArtwork(isPlaying = false, reduceMotion = false))
        assertFalse(shouldRotateMusicArtwork(isPlaying = true, reduceMotion = true))
        assertFalse(shouldRotateMusicArtwork(isPlaying = false, reduceMotion = true))
    }

    @Test
    fun rotationDurationFollowsPlaybackSpeed() {
        assertEquals(24_000, resolveMusicArtworkRotationDurationMs(1f))
        assertEquals(12_000, resolveMusicArtworkRotationDurationMs(2f))
        assertEquals(48_000, resolveMusicArtworkRotationDurationMs(0.5f))
        assertEquals(6_000, resolveMusicArtworkRotationDurationMs(4f))
        assertEquals(96_000, resolveMusicArtworkRotationDurationMs(0.25f))
    }

    @Test
    fun appleMusicArtworkScalesDownOnlyWhenPaused() {
        assertEquals(1.0f, resolveAppleMusicCoverScale(isPlaying = true))
        assertEquals(0.88f, resolveAppleMusicCoverScale(isPlaying = false))
        assertEquals(16f, resolveAppleMusicCoverShadowElevation(1f))
        assertEquals(11.52f, resolveAppleMusicCoverShadowElevation(0f), 0.001f)
    }
}
