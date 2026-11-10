package com.android.purebilibili.feature.dynamic.components

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ZoomableImageScalePolicyTest {

    @Test
    fun fitImage_isCenteredInUntransformedViewport() {
        assertEquals(
            Rect(0f, 300f, 400f, 500f),
            resolveZoomableImageLocalDisplayRect(IntSize(1000, 500), IntSize(400, 800)),
        )
    }

    @Test
    fun zoomedImage_appliesOnlyItsOwnScaleAndPan() {
        assertEquals(
            Rect(-190f, 170f, 610f, 570f),
            resolveZoomableImageLocalDisplayRect(
                imageSize = IntSize(1000, 500),
                containerSize = IntSize(400, 800),
                scale = 2f,
                offsetX = 10f,
                offsetY = -30f,
            ),
        )
    }

    @Test
    fun localDisplayRect_waitsForValidImageAndViewportSizes() {
        assertNull(resolveZoomableImageLocalDisplayRect(IntSize.Zero, IntSize(400, 800)))
        assertNull(resolveZoomableImageLocalDisplayRect(IntSize(1000, 500), IntSize.Zero))
        assertNull(resolveZoomableImageLocalDisplayRect(IntSize(0, 500), IntSize(400, 800)))
    }

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
