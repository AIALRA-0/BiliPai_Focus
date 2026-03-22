package com.android.purebilibili.feature.video.screen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TabletVideoLayoutPolicyTest {

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

    @Test
    fun defaultSecondaryTab_prefersRelatedOnlyWhenSectionEnabledAndCommentsEmpty() {
        assertEquals(
            1,
            resolveTabletSecondaryDefaultTab(
                replyCount = 0,
                hasRelatedVideos = true,
                showRelatedVideosSection = true
            )
        )
        assertEquals(
            0,
            resolveTabletSecondaryDefaultTab(
                replyCount = 0,
                hasRelatedVideos = true,
                showRelatedVideosSection = false
            )
        )
    }

    @Test
    fun secondaryTabs_hideRelatedTabWhenSectionDisabled() {
        assertEquals(
            listOf("评论", "相关推荐"),
            resolveTabletSecondaryTabs(
                replyCount = 0,
                showRelatedVideosSection = true
            )
        )
        assertEquals(
            listOf("评论"),
            resolveTabletSecondaryTabs(
                replyCount = 0,
                showRelatedVideosSection = false
            )
        )
    }

    @Test
    fun commentEmptyStateHint_onlyExistsWhenRelatedSectionIsAvailable() {
        assertEquals(
            "先看看相关推荐",
            resolveVideoCommentEmptyStateHint(
                hasRelatedVideos = true,
                showRelatedVideosSection = true
            )
        )
        assertNull(
            resolveVideoCommentEmptyStateHint(
                hasRelatedVideos = true,
                showRelatedVideosSection = false
            )
        )
        assertNull(
            resolveVideoCommentEmptyStateActionLabel(
                hasRelatedVideos = false,
                showRelatedVideosSection = true
            )
        )
    }
}
