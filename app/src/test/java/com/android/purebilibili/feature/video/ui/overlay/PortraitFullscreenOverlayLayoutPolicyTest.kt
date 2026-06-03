package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PortraitFullscreenOverlayLayoutPolicyTest {

    @Test
    fun compactPhone_usesDenseTopAndInfoLayout() {
        val policy = resolvePortraitFullscreenOverlayLayoutPolicy(
            widthDp = 320
        )

        assertTrue(policy.compactMode)
        assertEquals(10, policy.topHorizontalPaddingDp)
        assertEquals(20, policy.topActionIconSizeDp)
        assertEquals(52, policy.bottomInputSpacerHeightDp)
        assertEquals(4, policy.bottomInputLiftDp)
        assertEquals(15, policy.titleFontSp)
        assertEquals(116, policy.topScrimHeightDp)
        assertEquals(300, policy.bottomTextScrimHeightDp)
    }

    @Test
    fun mediumTablet_improvesInfoPanelSpacing() {
        val policy = resolvePortraitFullscreenOverlayLayoutPolicy(
            widthDp = 720
        )

        assertFalse(policy.compactMode)
        assertEquals(16, policy.topHorizontalPaddingDp)
        assertEquals(23, policy.topActionIconSizeDp)
        assertEquals(54, policy.bottomInputSpacerHeightDp)
        assertEquals(4, policy.bottomInputLiftDp)
        assertEquals(15, policy.titleFontSp)
        assertEquals(136, policy.topScrimHeightDp)
        assertEquals(340, policy.bottomTextScrimHeightDp)
    }

    @Test
    fun tablet_expandsHeaderAndVideoInfoReadability() {
        val policy = resolvePortraitFullscreenOverlayLayoutPolicy(
            widthDp = 1024
        )

        assertFalse(policy.compactMode)
        assertEquals(18, policy.topHorizontalPaddingDp)
        assertEquals(24, policy.topActionIconSizeDp)
        assertEquals(56, policy.bottomInputSpacerHeightDp)
        assertEquals(5, policy.bottomInputLiftDp)
        assertEquals(16, policy.titleFontSp)
        assertEquals(148, policy.topScrimHeightDp)
        assertEquals(372, policy.bottomTextScrimHeightDp)
    }

    @Test
    fun ultraWide_forcesLargestPortraitFullscreenScale() {
        val policy = resolvePortraitFullscreenOverlayLayoutPolicy(
            widthDp = 1920
        )

        assertFalse(policy.compactMode)
        assertEquals(24, policy.topHorizontalPaddingDp)
        assertEquals(28, policy.topActionIconSizeDp)
        assertEquals(64, policy.bottomInputSpacerHeightDp)
        assertEquals(6, policy.bottomInputLiftDp)
        assertEquals(18, policy.titleFontSp)
        assertEquals(168, policy.topScrimHeightDp)
        assertEquals(440, policy.bottomTextScrimHeightDp)
    }

    @Test
    fun scrimAlpha_keepsBottomTextMoreReadableThanTopControls() {
        val policy = resolvePortraitFullscreenOverlayLayoutPolicy(widthDp = 411)

        assertEquals(0.42f, policy.topScrimStartAlpha)
        assertEquals(0.68f, policy.bottomTextScrimEndAlpha)
        assertTrue(policy.bottomTextScrimEndAlpha > policy.topScrimStartAlpha)
    }
}
