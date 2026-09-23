package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PortraitSubtitleOverlayPolicyTest {

    @Test
    fun subtitleBottomPadding_raisesWhenControlsVisible() {
        assertEquals(
            88,
            resolvePortraitSubtitleBottomPaddingDp(controlsVisible = true)
        )
        assertEquals(
            40,
            resolvePortraitSubtitleBottomPaddingDp(controlsVisible = false)
        )
        assertEquals(
            36,
            resolvePortraitSubtitleBottomPaddingDp(
                controlsVisible = true,
                commentExpansionProgress = 0.5f
            )
        )
    }

    @Test
    fun subtitleChip_requiresFeatureAndTracks() {
        assertTrue(
            shouldShowPortraitSubtitleChip(
                featureEnabled = true,
                trackAvailable = true
            )
        )
        assertFalse(
            shouldShowPortraitSubtitleChip(
                featureEnabled = true,
                trackAvailable = false
            )
        )
        assertFalse(
            shouldShowPortraitSubtitleChip(
                featureEnabled = false,
                trackAvailable = true
            )
        )
    }
}
