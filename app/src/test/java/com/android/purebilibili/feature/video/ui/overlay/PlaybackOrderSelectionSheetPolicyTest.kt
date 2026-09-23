package com.android.purebilibili.feature.video.ui.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackOrderSelectionSheetPolicyTest {

    @Test
    fun landscapeUsesCompactOverlayWithLimitedWidth() {
        val spec = resolvePlaybackOrderSheetLayoutSpec(
            isLandscape = true,
            isFullscreen = true,
        )
        assertEquals(PlaybackOrderSheetPresentation.COMPACT_OVERLAY, spec.presentation)
        assertEquals(360, spec.maxWidthDp)
        assertTrue(spec.extendUnderNavigationBars)
        assertEquals(0, spec.outerBottomPaddingDp)
    }

    @Test
    fun portraitUsesBottomSheet() {
        val spec = resolvePlaybackOrderSheetLayoutSpec(
            isLandscape = false,
            isFullscreen = false,
        )
        assertEquals(PlaybackOrderSheetPresentation.BOTTOM_SHEET, spec.presentation)
        assertTrue(spec.extendUnderNavigationBars)
    }

    @Test
    fun fullscreenPortraitStillUsesCompactOverlay() {
        val spec = resolvePlaybackOrderSheetLayoutSpec(
            isLandscape = false,
            isFullscreen = true,
        )
        assertEquals(PlaybackOrderSheetPresentation.COMPACT_OVERLAY, spec.presentation)
    }
}
