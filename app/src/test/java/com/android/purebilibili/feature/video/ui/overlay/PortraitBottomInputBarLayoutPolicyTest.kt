package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertEquals

class PortraitBottomInputBarLayoutPolicyTest {

    @Test
    fun compactPhone_usesCurrentInputBarDensity() {
        val policy = resolvePortraitBottomInputBarLayoutPolicy(
            widthDp = 393
        )

        assertEquals(12, policy.horizontalPaddingDp)
        assertEquals(8, policy.verticalPaddingDp)
        assertEquals(36, policy.inputHeightDp)
        assertEquals(14, policy.inputFontSp)
        assertEquals(48, policy.actionButtonSizeDp)
        assertEquals(24, policy.actionIconSizeDp)
    }

    @Test
    fun mediumTablet_balancesInputReadabilityAndActionReachability() {
        val policy = resolvePortraitBottomInputBarLayoutPolicy(
            widthDp = 720
        )

        assertEquals(14, policy.horizontalPaddingDp)
        assertEquals(9, policy.verticalPaddingDp)
        assertEquals(40, policy.inputHeightDp)
        assertEquals(15, policy.inputFontSp)
        assertEquals(48, policy.actionButtonSizeDp)
        assertEquals(26, policy.actionIconSizeDp)
    }

    @Test
    fun tablet_expandsInputAndActionTargets() {
        val policy = resolvePortraitBottomInputBarLayoutPolicy(
            widthDp = 1024
        )

        assertEquals(16, policy.horizontalPaddingDp)
        assertEquals(10, policy.verticalPaddingDp)
        assertEquals(44, policy.inputHeightDp)
        assertEquals(16, policy.inputFontSp)
        assertEquals(48, policy.actionButtonSizeDp)
        assertEquals(28, policy.actionIconSizeDp)
    }

    @Test
    fun ultraWide_forcesLargestBottomInputScale() {
        val policy = resolvePortraitBottomInputBarLayoutPolicy(
            widthDp = 1920
        )

        assertEquals(20, policy.horizontalPaddingDp)
        assertEquals(12, policy.verticalPaddingDp)
        assertEquals(52, policy.inputHeightDp)
        assertEquals(18, policy.inputFontSp)
        assertEquals(54, policy.actionButtonSizeDp)
        assertEquals(32, policy.actionIconSizeDp)
    }
}
