package com.android.purebilibili.feature.dynamic.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImagePreviewVisualPolicyTest {

    @Test
    fun `disabled transition should keep content clear`() {
        val frame = resolveImagePreviewVisualFrame(
            visualProgress = 0.4f,
            transitionEnabled = false,
            maxBlurRadiusPx = 24f
        )

        assertEquals(1f, frame.contentAlpha)
        assertEquals(0f, frame.blurRadiusPx)
        assertEquals(0.4f, frame.backdropAlpha)
    }

    @Test
    fun `visual frame should clamp progress and reduce blur with progress`() {
        val start = resolveImagePreviewVisualFrame(
            visualProgress = -0.2f,
            transitionEnabled = true,
            maxBlurRadiusPx = 24f
        )
        val middle = resolveImagePreviewVisualFrame(
            visualProgress = 0.5f,
            transitionEnabled = true,
            maxBlurRadiusPx = 24f
        )
        val end = resolveImagePreviewVisualFrame(
            visualProgress = 1.2f,
            transitionEnabled = true,
            maxBlurRadiusPx = 24f
        )

        assertEquals(24f, start.blurRadiusPx)
        assertTrue(middle.blurRadiusPx in 11f..13f)
        assertEquals(0f, end.blurRadiusPx)

        assertTrue(start.contentAlpha < middle.contentAlpha)
        assertEquals(1f, end.contentAlpha)
        assertEquals(0f, start.backdropAlpha)
        assertEquals(1f, end.backdropAlpha)
    }

    @Test
    fun `caption visibility should be shown by default and hide after toggle`() {
        assertTrue(shouldShowImagePreviewText(hasText = true, textVisible = true))
        assertEquals(false, resolveImagePreviewTextVisibilityAfterToggle(currentVisible = true))
        assertEquals(true, resolveImagePreviewTextVisibilityAfterToggle(currentVisible = false))
        assertEquals(false, shouldShowImagePreviewText(hasText = true, textVisible = false))
        assertEquals(false, shouldShowImagePreviewText(hasText = false, textVisible = true))
    }

    @Test
    fun `initial caption visibility follows user default only when text exists`() {
        assertEquals(true, resolveImagePreviewInitialTextVisibility(hasText = true, defaultVisible = true))
        assertEquals(false, resolveImagePreviewInitialTextVisibility(hasText = true, defaultVisible = false))
        assertEquals(false, resolveImagePreviewInitialTextVisibility(hasText = false, defaultVisible = true))
    }

    @Test
    fun `comment preview page transform rotates around inner edges`() {
        val centered = resolveCommentImagePreviewPageTransform(pageOffsetFraction = 0f, containerWidthPx = 390f)
        val leftPanel = resolveCommentImagePreviewPageTransform(pageOffsetFraction = 1f, containerWidthPx = 390f)
        val rightPanel = resolveCommentImagePreviewPageTransform(pageOffsetFraction = -1f, containerWidthPx = 390f)

        assertEquals(0f, centered.rotationY, 0.001f)
        assertEquals(0.5f, centered.pivotFractionX, 0.001f)

        assertTrue(leftPanel.rotationY < -60f)
        assertEquals(1f, leftPanel.pivotFractionX, 0.001f)
        assertTrue(leftPanel.translationXPx < 0f)

        assertTrue(rightPanel.rotationY > 60f)
        assertEquals(0f, rightPanel.pivotFractionX, 0.001f)
        assertTrue(rightPanel.translationXPx > 0f)

        assertEquals(leftPanel.rotationY, -rightPanel.rotationY, 0.001f)
        assertEquals(leftPanel.alpha, rightPanel.alpha, 0.001f)
        assertTrue(leftPanel.scale < centered.scale)
    }

    @Test
    fun `comment original image size label uses k and m units`() {
        assertEquals("查看原图", resolveCommentImageOriginalSizeLabel(null))
        assertEquals("查看原图 (369K)", resolveCommentImageOriginalSizeLabel(369f))
        assertEquals("查看原图 (1.1M)", resolveCommentImageOriginalSizeLabel(1126f))
    }
}
