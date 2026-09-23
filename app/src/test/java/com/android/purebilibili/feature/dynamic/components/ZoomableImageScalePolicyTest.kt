package com.android.purebilibili.feature.dynamic.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ZoomableImageScalePolicyTest {

    @Test
    fun regularImage_keepsExistingZoomLevels() {
        val limits = resolveZoomableImageScaleLimits(
            imageWidth = 1920,
            imageHeight = 1080,
            containerWidth = 390,
            containerHeight = 844
        )

        assertEquals(2.5f, limits.doubleTapScale)
        assertEquals(5f, limits.maxScale)
    }

    @Test
    fun tallImage_canFillViewportWidthAndZoomFurther() {
        val limits = resolveZoomableImageScaleLimits(
            imageWidth = 1000,
            imageHeight = 20_000,
            containerWidth = 390,
            containerHeight = 844
        )

        assertTrue(limits.doubleTapScale > 9f)
        assertTrue(limits.maxScale >= limits.doubleTapScale * 2f)
    }

    @Test
    fun wideImage_canFillViewportHeightAndZoomFurther() {
        val limits = resolveZoomableImageScaleLimits(
            imageWidth = 20_000,
            imageHeight = 1000,
            containerWidth = 844,
            containerHeight = 390
        )

        assertTrue(limits.doubleTapScale > 9f)
        assertTrue(limits.maxScale >= limits.doubleTapScale * 2f)
    }

    @Test
    fun extremeAspectRatio_detectsTallAndWideImages() {
        assertTrue(isExtremeAspectRatio(imageWidth = 1000, imageHeight = 4000))
        assertTrue(isExtremeAspectRatio(imageWidth = 4000, imageHeight = 1000))
        assertFalse(isExtremeAspectRatio(imageWidth = 1000, imageHeight = 3000))
    }
}
