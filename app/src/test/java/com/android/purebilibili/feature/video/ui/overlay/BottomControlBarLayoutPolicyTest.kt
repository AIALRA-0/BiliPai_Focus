package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertEquals

class BottomControlBarLayoutPolicyTest {

    @Test
    fun compactPhone_usesDenseBottomControls() {
        val policy = resolveBottomControlBarLayoutPolicy(
            widthDp = 393
        )

        assertEquals(32, policy.playButtonSizeDp)
        assertEquals(28, policy.playIconSizeDp)
        assertEquals(12, policy.timeFontSp)
        assertEquals(13, policy.actionTextFontSp)
        assertEquals(4, policy.danmakuSettingEndPaddingDp)
        assertEquals(12, policy.horizontalPaddingDp)
        assertEquals(8, policy.afterTimeSpacingDp)
        assertEquals(8, policy.danmakuSwitchToInputSpacingDp)
        assertEquals(10, policy.afterInputSpacingDp)
        assertEquals(8, policy.rightActionSpacingDp)
        assertEquals(6, policy.danmakuSwitchHorizontalPaddingDp)
        assertEquals(6, policy.danmakuSwitchVerticalPaddingDp)
        assertEquals(5, policy.actionChipHorizontalPaddingDp)
        assertEquals(4, policy.actionChipVerticalPaddingDp)
    }

    @Test
    fun mediumTablet_increasesTapTargetsWithoutOverexpansion() {
        val policy = resolveBottomControlBarLayoutPolicy(
            widthDp = 720
        )

        assertEquals(36, policy.playButtonSizeDp)
        assertEquals(30, policy.playIconSizeDp)
        assertEquals(13, policy.timeFontSp)
        assertEquals(14, policy.actionTextFontSp)
        assertEquals(4, policy.danmakuSettingEndPaddingDp)
        assertEquals(14, policy.afterTimeSpacingDp)
        assertEquals(10, policy.danmakuSwitchToInputSpacingDp)
        assertEquals(12, policy.afterInputSpacingDp)
        assertEquals(14, policy.rightActionSpacingDp)
        assertEquals(8, policy.danmakuSwitchHorizontalPaddingDp)
        assertEquals(6, policy.danmakuSwitchVerticalPaddingDp)
        assertEquals(7, policy.actionChipHorizontalPaddingDp)
        assertEquals(4, policy.actionChipVerticalPaddingDp)
    }

    @Test
    fun tabletWidth_expandsTouchTargetsAndSpacing() {
        val policy = resolveBottomControlBarLayoutPolicy(
            widthDp = 1024
        )

        assertEquals(40, policy.playButtonSizeDp)
        assertEquals(32, policy.playIconSizeDp)
        assertEquals(13, policy.timeFontSp)
        assertEquals(15, policy.actionTextFontSp)
        assertEquals(5, policy.danmakuSettingEndPaddingDp)
    }

    @Test
    fun ultraWide_forcesLargestBottomControlScale() {
        val policy = resolveBottomControlBarLayoutPolicy(
            widthDp = 1920
        )

        assertEquals(48, policy.playButtonSizeDp)
        assertEquals(36, policy.playIconSizeDp)
        assertEquals(15, policy.timeFontSp)
        assertEquals(17, policy.actionTextFontSp)
        assertEquals(6, policy.danmakuSettingEndPaddingDp)
    }
}
