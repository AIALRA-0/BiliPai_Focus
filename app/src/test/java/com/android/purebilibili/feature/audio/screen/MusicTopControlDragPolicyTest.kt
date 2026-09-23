package com.android.purebilibili.feature.audio.screen

import org.junit.Assert.assertEquals
import org.junit.Test

class MusicTopControlDragPolicyTest {
    @Test
    fun idleControlKeepsIdentityTransform() {
        val transform = resolveMusicTopControlTransform(
            dragX = 0f,
            dragY = 0f,
            maxDragPx = 36f,
            widthPx = 48f,
            heightPx = 48f,
            expansionPx = 4f,
        )

        assertEquals(1f, transform.scaleX, 0.001f)
        assertEquals(1f, transform.scaleY, 0.001f)
        assertEquals(0f, transform.translationX, 0.001f)
        assertEquals(0f, transform.translationY, 0.001f)
    }

    @Test
    fun horizontalDragStretchesInPlaceAndClampsDeformation() {
        val transform = resolveMusicTopControlTransform(
            dragX = 72f,
            dragY = 0f,
            maxDragPx = 36f,
            widthPx = 48f,
            heightPx = 48f,
            expansionPx = 4f,
        )

        assertEquals(1.1458f, transform.scaleX, 0.001f)
        assertEquals(1.0833f, transform.scaleY, 0.001f)
        assertEquals(1.799f, transform.translationX, 0.001f)
        assertEquals(0f, transform.translationY, 0.001f)
    }

    @Test
    fun verticalDragStretchesInPlace() {
        val transform = resolveMusicTopControlTransform(
            dragX = 0f,
            dragY = -36f,
            maxDragPx = 36f,
            widthPx = 48f,
            heightPx = 48f,
            expansionPx = 4f,
        )

        assertEquals(1.0833f, transform.scaleX, 0.001f)
        assertEquals(1.1458f, transform.scaleY, 0.001f)
        assertEquals(0f, transform.translationX, 0.001f)
        assertEquals(-1.799f, transform.translationY, 0.001f)
    }

    @Test
    fun deformationDoesNotDependOnTheAppsGlassAppearancePreset() {
        val transform = resolveMusicTopControlTransform(
            dragX = 18f,
            dragY = 0f,
            maxDragPx = 36f,
            widthPx = 48f,
            heightPx = 48f,
            expansionPx = 4f,
        )

        assertEquals(1.0729f, transform.scaleX, 0.001f)
        assertEquals(1.0417f, transform.scaleY, 0.001f)
    }
}
