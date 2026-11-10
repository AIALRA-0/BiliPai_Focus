package com.android.purebilibili.core.ui.wallpaper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WallpaperTransformPolicyTest {
    @Test
    fun landscapeImageFillsPortraitViewportAndCanReachBothEdges() {
        val centered = resolveWallpaperCropGeometry(400f, 800f, 1600f, 900f, WallpaperTransform())
        assertEquals(800f, centered.height, 0.01f)
        assertTrue(centered.width > 400f)
        assertEquals(-(centered.width - 400f) / 2f, centered.left, 0.01f)
        val left = resolveWallpaperCropGeometry(400f, 800f, 1600f, 900f, WallpaperTransform(offsetX = -1f))
        val right = resolveWallpaperCropGeometry(400f, 800f, 1600f, 900f, WallpaperTransform(offsetX = 1f))
        assertEquals(0f, left.left)
        assertEquals(400f, right.left + right.width, 0.01f)
    }

    @Test
    fun normalizedTransformMatchesPreviewAtAnotherResolution() {
        val transform = WallpaperTransform(2f, 0.6f, -0.4f)
        val screen = resolveWallpaperCropGeometry(1080f, 2160f, 1920f, 1080f, transform)
        val preview = resolveWallpaperCropGeometry(135f, 270f, 1920f, 1080f, transform)
        assertEquals(screen.left / 8f, preview.left, 0.01f)
        assertEquals(screen.top / 8f, preview.top, 0.01f)
        assertEquals(screen.width / 8f, preview.width, 0.01f)
        assertEquals(screen.height / 8f, preview.height, 0.01f)
    }

    @Test
    fun draggingMovesImageWithFingerAndDoesNotExposeEmptyEdges() {
        val current = WallpaperTransform(scale = 2f)
        val next = applyWallpaperGesture(current, 20f, -30f, 1f, 400f, 800f, 400f, 800f)
        val before = resolveWallpaperCropGeometry(400f, 800f, 400f, 800f, current)
        val after = resolveWallpaperCropGeometry(400f, 800f, 400f, 800f, next)
        assertEquals(before.left + 20f, after.left, 0.01f)
        assertEquals(before.top - 30f, after.top, 0.01f)
        val edge = applyWallpaperGesture(next, 10000f, -10000f, 1f, 400f, 800f, 400f, 800f)
        val crop = resolveWallpaperCropGeometry(400f, 800f, 400f, 800f, edge)
        assertTrue(crop.left <= 0f && crop.left + crop.width >= 400f)
        assertTrue(crop.top <= 0f && crop.top + crop.height >= 800f)
    }

    @Test
    fun visibleSamplingRegionTracksFocalPointAndZoom() {
        val left = resolveWallpaperVisibleSourceRegion(400f, 800f, 1600f, 900f, WallpaperTransform(offsetX = -1f))
        val right = resolveWallpaperVisibleSourceRegion(400f, 800f, 1600f, 900f, WallpaperTransform(offsetX = 1f))
        assertEquals(0f, left.left, 0.01f)
        assertEquals(450f, left.right, 0.01f)
        assertEquals(1150f, right.left, 0.01f)
        assertEquals(1600f, right.right, 0.01f)
        val zoomed = resolveWallpaperVisibleSourceRegion(400f, 800f, 1600f, 900f, WallpaperTransform(scale = 2f))
        assertEquals(225f, zoomed.right - zoomed.left, 0.01f)
        assertEquals(450f, zoomed.bottom - zoomed.top, 0.01f)
        val preview = resolveWallpaperVisibleSourceRegion(100f, 200f, 1600f, 900f, WallpaperTransform(scale = 2f))
        assertEquals(zoomed, preview)
    }

    @Test
    fun invalidSavedValuesFallBackAndZoomOutCannotUnderfillViewport() {
        assertEquals(WallpaperTransform(), sanitizeWallpaperTransform(WallpaperTransform(Float.NaN, Float.NaN, Float.POSITIVE_INFINITY)))
        val clamped = sanitizeWallpaperTransform(WallpaperTransform(0.2f, -5f, 8f))
        assertEquals(WallpaperTransform(1f, -1f, 1f), clamped)
        val crop = resolveWallpaperCropGeometry(800f, 400f, 900f, 1600f, clamped)
        assertTrue(crop.width >= 800f && crop.height >= 400f)
    }
}
