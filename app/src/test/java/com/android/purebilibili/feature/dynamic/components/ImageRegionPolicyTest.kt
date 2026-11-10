package com.android.purebilibili.feature.dynamic.components

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import kotlin.math.ceil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImageRegionPolicyTest {
    @Test
    fun tallImageAtReadingWidthKeepsSourcePixelsInsteadOfShrinkingWholeImage() {
        val request = assertNotNull(resolveImageRegionRequest(
            IntSize(1080, 60000), IntSize(1080, 1920), Rect(0f, 0f, 1080f, 60000f),
        ))
        assertEquals(1, request.sampleSize)
        assertEquals(1080, request.bounds.right)
        assertTrue(request.bounds.bottom in 1920..2048)
    }

    @Test
    fun scrollingToBottomLoadsOriginalBottomWithoutOutOfBoundsDecode() {
        val request = assertNotNull(resolveImageRegionRequest(
            IntSize(1080, 60000), IntSize(1080, 1920), Rect(0f, -58080f, 1080f, 1920f),
        ))
        assertEquals(1, request.sampleSize)
        assertEquals(60000, request.bounds.bottom)
        assertTrue(request.bounds.top in 57900..58080)
        assertTrue(request.bounds.height <= 2048)
    }

    @Test
    fun overviewAndLargeWindowKeepEachBitmapWithinDrawBudget() {
        for (viewport in listOf(IntSize(720, 1600), IntSize(3840, 2160), IntSize(600, 400))) {
            for (scale in listOf(1f, 5f, 100f)) {
                val size = IntSize(12000, 160000)
                val rect = resolveZoomableImageLocalDisplayRect(size, viewport, scale)
                val request = assertNotNull(resolveImageRegionRequest(size, viewport, rect))
                val width = ceil(request.bounds.width.toDouble() / request.sampleSize).toLong()
                val height = ceil(request.bounds.height.toDouble() / request.sampleSize).toLong()
                assertTrue(width <= 2048 && height <= 2048)
                assertTrue(width * height * 4 <= 16L * 1024 * 1024)
            }
        }
    }

    @Test
    fun missingOrOffscreenContentDoesNotScheduleDecoding() {
        assertNull(resolveImageRegionRequest(IntSize.Zero, IntSize(1080, 1920), null))
        assertNull(resolveImageRegionRequest(
            IntSize(1080, 60000), IntSize(1080, 1920), Rect(2000f, 0f, 3080f, 60000f),
        ))
    }
}
