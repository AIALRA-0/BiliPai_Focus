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
    fun `embedded portrait pager thread detail keeps main sheet height without top reserve`() {
        assertEquals(
            0.60f,
            resolveVideoCommentSheetHostHeightFraction(
                hostContent = VideoCommentSheetHostContent.THREAD_DETAIL,
                mainSheetVisible = true,
                screenHeightPx = 1000,
                topReservedPx = 0
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
}
