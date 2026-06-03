package com.android.purebilibili.feature.screenshot

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppScreenshotRegionPolicyTest {

    @Test
    fun normalizeSelectionRect_ordersDragCornersAndClampsToViewport() {
        val rect = normalizeAppScreenshotSelectionRect(
            start = AppScreenshotPoint(x = 420f, y = 320f),
            end = AppScreenshotPoint(x = -20f, y = 80f),
            viewportWidth = 360f,
            viewportHeight = 240f
        )

        assertEquals(AppScreenshotSelectionRect(left = 0f, top = 80f, right = 360f, bottom = 240f), rect)
    }

    @Test
    fun isSelectionLargeEnough_rejectsTinySelections() {
        assertFalse(
            isAppScreenshotSelectionLargeEnough(
                rect = AppScreenshotSelectionRect(left = 10f, top = 10f, right = 22f, bottom = 60f),
                minimumSizePx = 24f
            )
        )
        assertTrue(
            isAppScreenshotSelectionLargeEnough(
                rect = AppScreenshotSelectionRect(left = 10f, top = 10f, right = 50f, bottom = 60f),
                minimumSizePx = 24f
            )
        )
    }

    @Test
    fun mapSelectionRectToBitmap_scalesPreviewCoordinatesToBitmapBounds() {
        val rect = mapAppScreenshotSelectionRectToBitmap(
            rect = AppScreenshotSelectionRect(left = 20f, top = 30f, right = 180f, bottom = 130f),
            previewWidth = 200f,
            previewHeight = 100f,
            bitmapWidth = 1000,
            bitmapHeight = 500
        )

        assertNotNull(rect)
        assertEquals(AppScreenshotCropRect(left = 100, top = 150, width = 800, height = 350), rect)
    }

    @Test
    fun mapSelectionRectToBitmap_returnsNullForEmptyPreviewOrTinySelection() {
        assertNull(
            mapAppScreenshotSelectionRectToBitmap(
                rect = AppScreenshotSelectionRect(left = 0f, top = 0f, right = 20f, bottom = 20f),
                previewWidth = 0f,
                previewHeight = 100f,
                bitmapWidth = 1000,
                bitmapHeight = 500
            )
        )
        assertNull(
            mapAppScreenshotSelectionRectToBitmap(
                rect = AppScreenshotSelectionRect(left = 0f, top = 0f, right = 1f, bottom = 1f),
                previewWidth = 200f,
                previewHeight = 100f,
                bitmapWidth = 1000,
                bitmapHeight = 500
            )
        )
    }

    @Test
    fun captureModeDefault_isFullWindow() {
        assertEquals(
            AppScreenshotCaptureMode.FULL_WINDOW,
            AppScreenshotCaptureMode.fromValue(99)
        )
    }
}
