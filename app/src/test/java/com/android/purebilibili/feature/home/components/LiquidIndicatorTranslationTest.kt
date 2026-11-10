package com.android.purebilibili.feature.home.components

import org.junit.Assert.assertEquals
import org.junit.Test

class LiquidIndicatorTranslationTest {

    @Test
    fun `scroll activity without page displacement does not press the indicator`() {
        for (position in listOf(0f, 1f, 2f, 0.0001f, 0.9999f)) {
            assertEquals(false, shouldStartExternalPagerIndicatorPress(true, true, position))
        }
        assertEquals(false, shouldStartExternalPagerIndicatorPress(true, true, null))
        assertEquals(false, shouldStartExternalPagerIndicatorPress(true, true, Float.NaN))
    }

    @Test
    fun `actual pager travel starts press only while motion effects are enabled`() {
        for (position in listOf(0.03f, 0.5f, 1.2f)) {
            assertEquals(true, shouldStartExternalPagerIndicatorPress(true, true, position))
            assertEquals(false, shouldStartExternalPagerIndicatorPress(true, false, position))
            assertEquals(false, shouldStartExternalPagerIndicatorPress(false, true, position))
        }
    }

    @Test
    fun `translation keeps center alignment when clamp disabled`() {
        val translation = resolveIndicatorTranslationXPx(
            position = 0f,
            itemWidthPx = 100f,
            indicatorWidthPx = 136f,
            startPaddingPx = 0f,
            containerWidthPx = 500f,
            clampToBounds = false,
            edgeInsetPx = 0f
        )

        assertEquals(-18f, translation, 0.001f)
    }

    @Test
    fun `translation clamps left edge when enabled`() {
        val translation = resolveIndicatorTranslationXPx(
            position = 0f,
            itemWidthPx = 100f,
            indicatorWidthPx = 136f,
            startPaddingPx = 0f,
            containerWidthPx = 500f,
            clampToBounds = true,
            edgeInsetPx = 0f
        )

        assertEquals(0f, translation, 0.001f)
    }

    @Test
    fun `translation clamps right edge when enabled`() {
        val translation = resolveIndicatorTranslationXPx(
            position = 4f,
            itemWidthPx = 100f,
            indicatorWidthPx = 136f,
            startPaddingPx = 0f,
            containerWidthPx = 500f,
            clampToBounds = true,
            edgeInsetPx = 0f
        )

        assertEquals(364f, translation, 0.001f)
    }

    @Test
    fun `translation clamps with viewport shift to keep visible after parent translation`() {
        val translation = resolveIndicatorTranslationXPx(
            position = 1f,
            itemWidthPx = 100f,
            indicatorWidthPx = 120f,
            startPaddingPx = 11f,
            containerWidthPx = 500f,
            clampToBounds = true,
            edgeInsetPx = 1f,
            viewportShiftPx = 111f
        )

        assertEquals(112f, translation, 0.001f)
    }
}
