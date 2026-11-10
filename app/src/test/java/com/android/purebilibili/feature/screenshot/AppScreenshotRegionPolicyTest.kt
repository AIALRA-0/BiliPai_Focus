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
    fun cropSelectionIdentifiesEdgesAndMovesWithinImage() {
        val rect = AppScreenshotSelectionRect(20f, 30f, 120f, 130f)
        assertEquals(RegionDragTarget.TOP_LEFT, resolveRegionDragTarget(rect, AppScreenshotPoint(21f, 31f), 12f))
        assertEquals(RegionDragTarget.MOVE, resolveRegionDragTarget(rect, AppScreenshotPoint(70f, 80f), 12f))
        assertEquals(RegionDragTarget.NEW, resolveRegionDragTarget(rect, AppScreenshotPoint(180f, 180f), 12f))
        assertEquals(AppScreenshotSelectionRect(100f, 100f, 200f, 200f), adjustRegionSelection(rect, RegionDragTarget.MOVE, AppScreenshotPoint(300f, 300f), 200f, 200f))
    }

    @Test
    fun cropResizeCannotInvertOrLeaveImage() {
        val rect = AppScreenshotSelectionRect(20f, 30f, 120f, 130f)
        assertEquals(AppScreenshotSelectionRect(96f, 106f, 120f, 130f), adjustRegionSelection(rect, RegionDragTarget.TOP_LEFT, AppScreenshotPoint(200f, 200f), 200f, 200f))
        assertEquals(AppScreenshotSelectionRect(20f, 30f, 200f, 200f), adjustRegionSelection(rect, RegionDragTarget.BOTTOM_RIGHT, AppScreenshotPoint(300f, 300f), 200f, 200f))
    }

    @Test
    fun captureModeDefault_isFullWindow() {
        assertEquals(
            AppScreenshotCaptureMode.FULL_WINDOW,
            AppScreenshotCaptureMode.fromValue(99)
        )
    }
}
