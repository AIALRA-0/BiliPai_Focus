package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PortraitInteractionBarLayoutPolicyTest {

    @Test
    fun portraitTriplePress_startsOnlyAfterLongPressConfirmation() {
        assertFalse(shouldStartPortraitTriplePress(longPressConfirmed = false))
        assertTrue(shouldStartPortraitTriplePress(longPressConfirmed = true))
    }

    @Test
    fun portraitTriplePressRelease_cancelsOnlyIncompleteProgress() {
        assertTrue(
            shouldCancelPortraitTriplePressOnRelease(
                isTriplePressing = true,
                tripleCompleted = false
            )
        )
        assertFalse(
            shouldCancelPortraitTriplePressOnRelease(
                isTriplePressing = true,
                tripleCompleted = true
            )
        )
        assertFalse(
            shouldCancelPortraitTriplePressOnRelease(
                isTriplePressing = false,
                tripleCompleted = false
            )
        )
    }

    @Test
    fun compactPhone_usesDenseInteractionRail() {
        val policy = resolvePortraitInteractionBarLayoutPolicy(
            widthDp = 393
        )

        assertEquals(8, policy.endPaddingDp)
        assertEquals(180, policy.bottomPaddingDp)
        assertEquals(20, policy.itemSpacingDp)
        assertEquals(29, policy.iconSizeDp)
        assertEquals(38, policy.iconBackingSizeDp)
        assertEquals(4, policy.iconBackingInnerPaddingDp)
        assertEquals(0.14f, policy.iconBackingAlpha)
        assertEquals(12, policy.labelFontSp)
    }

    @Test
    fun mediumTablet_improvesRailSpacingWithoutOverstretch() {
        val policy = resolvePortraitInteractionBarLayoutPolicy(
            widthDp = 720
        )

        assertEquals(10, policy.endPaddingDp)
        assertEquals(188, policy.bottomPaddingDp)
        assertEquals(22, policy.itemSpacingDp)
        assertEquals(32, policy.iconSizeDp)
        assertEquals(42, policy.iconBackingSizeDp)
        assertEquals(5, policy.iconBackingInnerPaddingDp)
        assertEquals(0.14f, policy.iconBackingAlpha)
        assertEquals(12, policy.labelFontSp)
    }

    @Test
    fun tablet_expandsIconAndSpacing() {
        val policy = resolvePortraitInteractionBarLayoutPolicy(
            widthDp = 1024
        )

        assertEquals(12, policy.endPaddingDp)
        assertEquals(196, policy.bottomPaddingDp)
        assertEquals(24, policy.itemSpacingDp)
        assertEquals(35, policy.iconSizeDp)
        assertEquals(46, policy.iconBackingSizeDp)
        assertEquals(5, policy.iconBackingInnerPaddingDp)
        assertEquals(0.15f, policy.iconBackingAlpha)
        assertEquals(13, policy.labelFontSp)
    }

    @Test
    fun ultraWide_forcesLargestInteractionRailScale() {
        val policy = resolvePortraitInteractionBarLayoutPolicy(
            widthDp = 1920
        )

        assertEquals(18, policy.endPaddingDp)
        assertEquals(220, policy.bottomPaddingDp)
        assertEquals(28, policy.itemSpacingDp)
        assertEquals(40, policy.iconSizeDp)
        assertEquals(52, policy.iconBackingSizeDp)
        assertEquals(6, policy.iconBackingInnerPaddingDp)
        assertEquals(0.16f, policy.iconBackingAlpha)
        assertEquals(15, policy.labelFontSp)
    }
}
