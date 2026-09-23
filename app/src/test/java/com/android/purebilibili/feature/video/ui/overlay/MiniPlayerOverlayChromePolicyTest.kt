package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MiniPlayerOverlayChromePolicyTest {

    @Test
    fun hiddenControls_useCleanVideoMode() {
        val chrome = resolveMiniPlayerOverlayChrome(
            showControls = false,
            isDraggingProgress = false,
            isDraggingPosition = false,
            isResizing = false
        )

        assertFalse(chrome.showHeaderChrome)
        assertFalse(chrome.showCenterControls)
        assertFalse(chrome.showDragHint)
        assertFalse(chrome.showResizeHandle)
        assertTrue(chrome.showProgressBar)
        assertEquals(0.46f, chrome.progressBarAlpha)
    }

    @Test
    fun visibleControls_showInteractiveChrome() {
        val chrome = resolveMiniPlayerOverlayChrome(
            showControls = true,
            isDraggingProgress = false,
            isDraggingPosition = false,
            isResizing = false
        )

        assertTrue(chrome.showHeaderChrome)
        assertTrue(chrome.showCenterControls)
        assertTrue(chrome.showDragHint)
        assertTrue(chrome.showResizeHandle)
        assertTrue(chrome.showProgressBar)
        assertEquals(1f, chrome.progressBarAlpha)
    }

    @Test
    fun seekingKeepsSeekFeedbackVisibleWithoutHeaderTitle() {
        val chrome = resolveMiniPlayerOverlayChrome(
            showControls = false,
            isDraggingProgress = true,
            isDraggingPosition = false,
            isResizing = false
        )

        assertFalse(chrome.showHeaderChrome)
        assertTrue(chrome.showCenterControls)
        assertTrue(chrome.showSeekHint)
        assertFalse(chrome.showDragHint)
        assertFalse(chrome.showResizeHandle)
        assertEquals(1f, chrome.progressBarAlpha)
    }
}
