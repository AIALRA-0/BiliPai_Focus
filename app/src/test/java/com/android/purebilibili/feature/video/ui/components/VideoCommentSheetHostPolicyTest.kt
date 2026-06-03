package com.android.purebilibili.feature.video.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoCommentSheetHostPolicyTest {

    @Test
    fun `host should stay hidden when neither main sheet nor thread detail is visible`() {
        assertEquals(
            VideoCommentSheetHostContent.HIDDEN,
            resolveVideoCommentSheetHostContent(
                mainSheetVisible = false,
                subReplyVisible = false
            )
        )
    }

    @Test
    fun `host should show main list when only the main comment sheet is visible`() {
        assertEquals(
            VideoCommentSheetHostContent.MAIN_LIST,
            resolveVideoCommentSheetHostContent(
                mainSheetVisible = true,
                subReplyVisible = false
            )
        )
    }

    @Test
    fun `comment host should initialize for routed comment even when main sheet is hidden`() {
        assertTrue(
            shouldInitializeVideoCommentSheetHost(
                mainSheetVisible = false,
                forceInitialize = true
            )
        )
        assertFalse(
            shouldInitializeVideoCommentSheetHost(
                mainSheetVisible = false,
                forceInitialize = false
            )
        )
    }

    @Test
    fun `host should prioritize thread detail whenever subreply detail is visible`() {
        assertEquals(
            VideoCommentSheetHostContent.THREAD_DETAIL,
            resolveVideoCommentSheetHostContent(
                mainSheetVisible = true,
                subReplyVisible = true
            )
        )
        assertEquals(
            VideoCommentSheetHostContent.THREAD_DETAIL,
            resolveVideoCommentSheetHostContent(
                mainSheetVisible = false,
                subReplyVisible = true
            )
        )
    }

    @Test
    fun `main comment sheet should keep drawer height and scrim`() {
        assertEquals(
            0.60f,
            resolveVideoCommentSheetHostHeightFraction(
                mainSheetVisible = true,
                screenHeightPx = 1000,
                topReservedPx = 450
            )
        )
        assertEquals(0.5f, resolveVideoCommentSheetHostScrimAlpha(mainSheetVisible = true))
    }

    @Test
    fun `main comment sheet scrim and blur follow presentation progress`() {
        val hidden = resolveVideoCommentSheetHostOverlayVisual(
            mainSheetVisible = true,
            presentationProgress = 0f
        )
        val half = resolveVideoCommentSheetHostOverlayVisual(
            mainSheetVisible = true,
            presentationProgress = 0.5f
        )
        val shown = resolveVideoCommentSheetHostOverlayVisual(
            mainSheetVisible = true,
            presentationProgress = 1f
        )

        assertEquals(0f, hidden.scrimAlpha)
        assertFalse(hidden.blurEnabled)
        assertEquals(0.25f, half.scrimAlpha)
        assertTrue(half.forceLowBlurBudget)
        assertTrue(half.scrimAlpha < shown.scrimAlpha)
        assertEquals(0.5f, shown.scrimAlpha)
        assertFalse(shown.forceLowBlurBudget)
    }

    @Test
    fun `thread only detail should stay below the reserved top area`() {
        assertEquals(
            0.55f,
            resolveVideoCommentSheetHostHeightFraction(
                hostContent = VideoCommentSheetHostContent.THREAD_DETAIL,
                mainSheetVisible = false,
                screenHeightPx = 1000,
                topReservedPx = 450
            )
        )
        assertEquals(0f, resolveVideoCommentSheetHostScrimAlpha(mainSheetVisible = false))
    }

    @Test
    fun `embedded thread detail should cover comment content below reserved player area`() {
        assertEquals(
            0.55f,
            resolveVideoCommentSheetHostHeightFraction(
                hostContent = VideoCommentSheetHostContent.THREAD_DETAIL,
                mainSheetVisible = true,
                screenHeightPx = 1000,
                topReservedPx = 450
            )
        )
    }

    @Test
    fun `embedded portrait pager thread detail covers content when top reserve is not measured`() {
        assertEquals(
            1f,
            resolveVideoCommentSheetHostHeightFraction(
                hostContent = VideoCommentSheetHostContent.THREAD_DETAIL,
                mainSheetVisible = true,
                screenHeightPx = 1000,
                topReservedPx = 0
            )
        )
    }

    @Test
    fun `thread detail height uses actual host height to align with reserved top`() {
        assertEquals(
            750,
            resolveVideoCommentSheetHostHeightPx(
                hostContent = VideoCommentSheetHostContent.THREAD_DETAIL,
                hostHeightPx = 1200,
                topReservedPx = 450
            )
        )
    }

    @Test
    fun `main sheet pixel height keeps drawer fraction`() {
        assertEquals(
            720,
            resolveVideoCommentSheetHostHeightPx(
                hostContent = VideoCommentSheetHostContent.MAIN_LIST,
                hostHeightPx = 1200,
                topReservedPx = 450
            )
        )
    }

    @Test
    fun `thread detail falls back to full host height when reserve is invalid`() {
        assertEquals(
            1200,
            resolveVideoCommentSheetHostHeightPx(
                hostContent = VideoCommentSheetHostContent.THREAD_DETAIL,
                hostHeightPx = 1200,
                topReservedPx = 1300
            )
        )
    }

    @Test
    fun `detached fullscreen thread detail should keep status bar padding`() {
        assertEquals(
            true,
            shouldApplyVideoCommentThreadStatusBarPadding(
                mainSheetVisible = false,
                topReservedPx = 0
            )
        )
        assertEquals(
            false,
            shouldApplyVideoCommentThreadStatusBarPadding(
                mainSheetVisible = false,
                topReservedPx = 450
            )
        )
        assertEquals(
            false,
            shouldApplyVideoCommentThreadStatusBarPadding(
                mainSheetVisible = true,
                topReservedPx = 0
            )
        )
    }

    @Test
    fun `backdrop tap dismissal only applies to main comment sheet`() {
        assertTrue(
            shouldDismissVideoCommentSheetHostOnBackdropTap(
                mainSheetVisible = true
            )
        )
        assertFalse(
            shouldDismissVideoCommentSheetHostOnBackdropTap(
                mainSheetVisible = false
            )
        )
    }

    @Test
    fun `sheet vertical drag follows finger down and back up while offset is positive`() {
        assertTrue(
            shouldHandleVideoCommentSheetVerticalDrag(
                dragAmountPx = 36f,
                currentOffsetPx = 0f
            )
        )
        assertEquals(
            36f,
            resolveVideoCommentSheetDragTargetOffset(
                currentOffsetPx = 0f,
                dragAmountPx = 36f
            )
        )

        assertTrue(
            shouldHandleVideoCommentSheetVerticalDrag(
                dragAmountPx = -14f,
                currentOffsetPx = 36f
            )
        )
        assertEquals(
            22f,
            resolveVideoCommentSheetDragTargetOffset(
                currentOffsetPx = 36f,
                dragAmountPx = -14f
            )
        )
        assertEquals(
            0f,
            resolveVideoCommentSheetDragTargetOffset(
                currentOffsetPx = 8f,
                dragAmountPx = -16f
            )
        )
    }

    @Test
    fun `sheet vertical drag ignores upward drag before the sheet has been pulled`() {
        assertFalse(
            shouldHandleVideoCommentSheetVerticalDrag(
                dragAmountPx = -12f,
                currentOffsetPx = 0f
            )
        )
    }

    @Test
    fun `sheet drag start keeps the currently rendered offset to support interruption`() {
        assertEquals(
            40f,
            resolveVideoCommentSheetDragStartOffset(
                renderedOffsetPx = 40f,
                targetOffsetPx = 0f
            )
        )
        assertEquals(
            56f,
            resolveVideoCommentSheetDragStartOffset(
                renderedOffsetPx = 40f,
                targetOffsetPx = 56f
            )
        )
    }

    @Test
    fun `sheet presentation progress combines host animation and drag progress`() {
        assertEquals(
            0.5f,
            resolveVideoCommentSheetPresentationProgress(
                hostVisibilityProgress = 0.5f,
                dragVisibilityProgress = 1f
            )
        )
        assertEquals(
            0.5f,
            resolveVideoCommentSheetPresentationProgress(
                hostVisibilityProgress = 1f,
                dragVisibilityProgress = 0.5f
            )
        )
        assertEquals(
            0f,
            resolveVideoCommentSheetPresentationProgress(
                hostVisibilityProgress = -1f,
                dragVisibilityProgress = 1f
            )
        )
    }
}
