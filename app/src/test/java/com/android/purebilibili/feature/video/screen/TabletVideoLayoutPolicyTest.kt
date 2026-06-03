package com.android.purebilibili.feature.video.screen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TabletVideoLayoutPolicyTest {

    @Test
    fun secondaryPaneDefaultsToRelatedWhenCommentsEmptyAndRelatedExist() {
        assertEquals(
            1,
            resolveTabletSecondaryDefaultTab(
                replyCount = 0,
                hasRelatedVideos = true
            )
        )
    }

    @Test
    fun secondaryPaneDefaultsToCommentsWhenRepliesExist() {
        assertEquals(
            0,
            resolveTabletSecondaryDefaultTab(
                replyCount = 8,
                hasRelatedVideos = true
            )
        )
    }

    @Test
    fun secondaryPaneDefaultsToCommentsWhenNoRelatedVideos() {
        assertEquals(
            0,
            resolveTabletSecondaryDefaultTab(
                replyCount = 0,
                hasRelatedVideos = false
            )
        )
    }

    @Test
    fun expandedTablet_prioritizesPrimaryPaneWidth() {
        val policy = resolveTabletVideoLayoutPolicy(widthDp = 1280)

        assertEquals(0.72f, policy.primaryRatio)
        assertEquals(1080, policy.playerMaxWidthDp)
        assertEquals(1000, policy.infoMaxWidthDp)
    }

    @Test
    fun ultraWideTablet_balancesPaneRatioAndPlayerCap() {
        val policy = resolveTabletVideoLayoutPolicy(widthDp = 1920)

        assertEquals(0.66f, policy.primaryRatio)
        assertTrue(policy.playerMaxWidthDp >= 1240)
        assertTrue(policy.infoMaxWidthDp >= 1160)
    }

    @Test
    fun ultraWidePolicy_keepsLargePrimaryPane() {
        val policy = resolveTabletVideoLayoutPolicy(widthDp = 1920)

        assertEquals(0.66f, policy.primaryRatio)
        assertEquals(1240, policy.playerMaxWidthDp)
        assertEquals(1160, policy.infoMaxWidthDp)
    }
}
