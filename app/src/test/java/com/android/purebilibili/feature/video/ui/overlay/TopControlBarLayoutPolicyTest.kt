package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TopControlBarLayoutPolicyTest {
    @Test
    fun everyMode_hasTheSameNativeHeaderGeometry() {
        for (portrait in listOf(false, true)) {
            for (fullscreen in listOf(false, true)) {
                val policy = resolveTopControlBarLayoutPolicy(portrait, fullscreen)
                assertEquals(42, policy.buttonWidthDp)
                assertEquals(34, policy.buttonHeightDp)
                assertEquals(12, policy.verticalPaddingDp)
                assertEquals(16, policy.titleFontSp)
                assertEquals(13, policy.statusFontSp)
                assertEquals(11, policy.onlineCountFontSp)
            }
        }
    }

    @Test
    fun topBar_padsOnlyWhenHostReportsVisibleStatusBar() {
        for (fullscreen in listOf(false, true)) {
            assertTrue(shouldApplyStatusBarPaddingToTopControlBar(fullscreen, statusBarVisible = true))
            assertFalse(shouldApplyStatusBarPaddingToTopControlBar(fullscreen, statusBarVisible = false))
        }
    }

    @Test
    fun portraitFullscreen_reservesTopSafetyEvenWithHiddenSystemBars() {
        assertTrue(shouldReserveHiddenPlayerStatusBar(isFullscreen = true, isPortrait = true))
        assertFalse(shouldReserveHiddenPlayerStatusBar(isFullscreen = true, isPortrait = false))
        assertFalse(shouldReserveHiddenPlayerStatusBar(isFullscreen = false, isPortrait = true))
        assertFalse(shouldReserveHiddenPlayerStatusBar(isFullscreen = false, isPortrait = false))
    }
}
