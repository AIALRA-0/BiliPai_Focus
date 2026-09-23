package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PortraitTopBarLayoutPolicyTest {

    @Test
    fun portraitTopBar_padsOnlyWhenStatusBarVisible() {
        assertTrue(shouldApplyStatusBarPaddingToPortraitTopBar(statusBarVisible = true))
        assertFalse(shouldApplyStatusBarPaddingToPortraitTopBar(statusBarVisible = false))
    }

    @Test
    fun compactPhone_usesDenseControlSizing() {
        val policy = resolvePortraitTopBarLayoutPolicy(
            widthDp = 393
        )

        assertEquals(8, policy.horizontalPaddingDp)
        assertEquals(32, policy.buttonSizeDp)
        assertEquals(18, policy.iconSizeDp)
        assertEquals(11, policy.chipFontSp)
    }

    @Test
    fun mediumTablet_improvesSpacingWithoutTabletOverstretch() {
        val policy = resolvePortraitTopBarLayoutPolicy(
            widthDp = 720
        )

        assertEquals(12, policy.horizontalPaddingDp)
        assertEquals(36, policy.buttonSizeDp)
        assertEquals(20, policy.iconSizeDp)
        assertEquals(12, policy.chipFontSp)
    }

    @Test
    fun tabletWidth_expandsPaddingAndButtonSize() {
        val policy = resolvePortraitTopBarLayoutPolicy(
            widthDp = 900
        )

        assertEquals(16, policy.horizontalPaddingDp)
        assertEquals(40, policy.buttonSizeDp)
        assertEquals(22, policy.iconSizeDp)
        assertEquals(13, policy.chipFontSp)
    }

    @Test
    fun ultraWide_forcesLargestTargetAndSpacing() {
        val policy = resolvePortraitTopBarLayoutPolicy(
            widthDp = 1920
        )

        assertEquals(24, policy.horizontalPaddingDp)
        assertEquals(52, policy.buttonSizeDp)
        assertEquals(28, policy.iconSizeDp)
        assertEquals(15, policy.chipFontSp)
    }
}
