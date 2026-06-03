package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PortraitFullscreenOverlayPolicyTest {

    @Test
    fun useCompactTopBarOnNarrowScreen() {
        assertTrue(
            resolvePortraitFullscreenOverlayLayoutPolicy(
                widthDp = 360
            ).compactMode
        )
        assertTrue(
            resolvePortraitFullscreenOverlayLayoutPolicy(
                widthDp = 320
            ).compactMode
        )
    }

    @Test
    fun keepNormalTopBarOnWideScreen() {
        assertFalse(
            resolvePortraitFullscreenOverlayLayoutPolicy(
                widthDp = 411
            ).compactMode
        )
    }

    @Test
    fun hideViewCountInCompactMode() {
        assertFalse(shouldShowPortraitViewCount(viewCount = 12345, compactMode = true))
    }

    @Test
    fun showViewCountInNormalModeWhenHasData() {
        assertTrue(shouldShowPortraitViewCount(viewCount = 12345, compactMode = false))
    }

    @Test
    fun hideViewCountWhenNoData() {
        assertFalse(shouldShowPortraitViewCount(viewCount = 0, compactMode = false))
    }

    @Test
    fun hideTopMoreActionToAvoidDuplicateWithBottomBar() {
        assertFalse(shouldShowPortraitTopMoreAction())
    }

    @Test
    fun progressTimeLabel_formatsCurrentAndDuration() {
        assertEquals(
            "01:05 / 02:05",
            resolvePortraitProgressTimeLabel(
                positionMs = 65_000L,
                durationMs = 125_000L
            )
        )
    }

    @Test
    fun progressTimeLabel_clampsPositionToDuration() {
        assertEquals(
            "02:05 / 02:05",
            resolvePortraitProgressTimeLabel(
                positionMs = 130_000L,
                durationMs = 125_000L
            )
        )
    }
}
