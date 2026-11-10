package com.android.purebilibili.navigation3

import com.android.purebilibili.navigation3.predictiveback.circularRevealRadius
import com.android.purebilibili.navigation3.predictiveback.iosPageTranslationFraction
import com.android.purebilibili.navigation3.predictiveback.BiliPaiPredictiveBackAnimationStyle
import com.android.purebilibili.navigation3.predictiveback.resolvePageTransitionPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PageRevealAndIosGeometryTest {
    @Test
    fun `sliding fades move covered page and pure fade has no vertical movement`() {
        assertTrue(resolvePageTransitionPreset(BiliPaiPredictiveBackAnimationStyle.RIGHT_TO_LEFT_FADE).coveredPageMovement)
        assertTrue(resolvePageTransitionPreset(BiliPaiPredictiveBackAnimationStyle.LEFT_TO_RIGHT_FADE).coveredPageMovement)
        assertEquals(0f, resolvePageTransitionPreset(BiliPaiPredictiveBackAnimationStyle.FADE_IN).offsetDp, 0f)
        assertTrue(resolvePageTransitionPreset(BiliPaiPredictiveBackAnimationStyle.FADE).offsetDp > 0f)
    }

    @Test
    fun `center reveal covers all corners for portrait landscape and narrow panes`() {
        listOf(1080f to 2400f, 2400f to 1080f, 320f to 1600f).forEach { (width, height) ->
            val radius = circularRevealRadius(width, height, 1f).toDouble()
            val cornerDistanceSquared = width.toDouble() * width / 4.0 + height.toDouble() * height / 4.0
            assertTrue(radius * radius >= cornerDistanceSquared - 1.0)
            assertEquals(0f, circularRevealRadius(width, height, 0f), 0f)
            assertEquals(radius.toFloat(), circularRevealRadius(width, height, 2f), 0f)
            assertEquals(0f, circularRevealRadius(width, height, -1f), 0f)
        }
    }

    @Test
    fun `reveal shrinks monotonically on return and handles an unmeasured container`() {
        val samples = listOf(1f, 0.8f, 0.4f, 0.2f)
        val radii = samples.map { circularRevealRadius(320f, 1600f, it) }
        assertTrue(radii.zipWithNext().all { (a, b) -> a > b })
        assertEquals(0f, circularRevealRadius(0f, 0f, 0.5f), 0f)
    }

    @Test
    fun `ios moves the covered page with the entering page`() {
        assertEquals(1f, iosPageTranslationFraction(-1f, false), 0f)
        assertEquals(0f, iosPageTranslationFraction(0f, false), 0f)
        assertEquals(-0.33f, iosPageTranslationFraction(1f, false), 0f)
        assertEquals(1f, iosPageTranslationFraction(-2f, false), 0f)
        assertEquals(-0.33f, iosPageTranslationFraction(2f, false), 0f)
    }

    @Test
    fun `ios mirrors both pages for rtl without discontinuity at rest`() {
        listOf(-1f, -0.7f, -0.2f, 0f, 0.2f, 0.7f, 1f).forEach { depth ->
            assertEquals(-iosPageTranslationFraction(depth, false), iosPageTranslationFraction(depth, true), 0f)
        }
        assertEquals(0f, iosPageTranslationFraction(-0.0001f, false), 0.001f)
        assertEquals(0f, iosPageTranslationFraction(0.0001f, false), 0.001f)
    }
}
