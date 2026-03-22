package com.android.purebilibili.feature.video.screen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TabletCinemaLayoutPolicyTest {

    @Test
    fun largeTabletGetsWiderCurtainAndPlayerCap() {
        val policy = resolveTabletCinemaLayoutPolicy(
            widthDp = 1800
        )

        assertTrue(policy.curtainOpenWidthDp >= 470)
        assertTrue(policy.playerMaxWidthDp >= 1270)
    }

    @Test
    fun cinemaPolicyScalesSmoothlyAcrossTabletWidths() {
        val compact = resolveTabletCinemaLayoutPolicy(
            widthDp = 960
        )
        val medium = resolveTabletCinemaLayoutPolicy(
            widthDp = 1280
        )
        val large = resolveTabletCinemaLayoutPolicy(
            widthDp = 1600
        )

        assertTrue(medium.curtainOpenWidthDp > compact.curtainOpenWidthDp)
        assertTrue(large.curtainOpenWidthDp > medium.curtainOpenWidthDp)
        assertTrue(medium.curtainPeekWidthDp > compact.curtainPeekWidthDp)
        assertTrue(large.curtainPeekWidthDp > medium.curtainPeekWidthDp)
        assertTrue(medium.horizontalPaddingDp > compact.horizontalPaddingDp)
        assertTrue(large.horizontalPaddingDp > medium.horizontalPaddingDp)
        assertTrue(medium.playerMaxWidthDp > compact.playerMaxWidthDp)
        assertTrue(large.playerMaxWidthDp > medium.playerMaxWidthDp)
    }

    @Test
    fun mediumTabletUsesBalancedCinemaPolicy() {
        val policy = resolveTabletCinemaLayoutPolicy(
            widthDp = 1280
        )

        assertTrue(policy.curtainPeekWidthDp in 61..64)
        assertTrue(policy.curtainOpenWidthDp in 379..382)
        assertTrue(policy.horizontalPaddingDp in 16..17)
        assertTrue(policy.playerMaxWidthDp in 1090..1100)
    }

    @Test
    fun ultraWideUsesLargestCinemaPolicy() {
        val policy = resolveTabletCinemaLayoutPolicy(
            widthDp = 1920
        )

        assertEquals(74, policy.curtainPeekWidthDp)
        assertEquals(480, policy.curtainOpenWidthDp)
        assertEquals(24, policy.horizontalPaddingDp)
    }

    @Test
    fun curtainWidthFollowsStateMachine() {
        val policy = TabletCinemaLayoutPolicy(
            curtainPeekWidthDp = 60,
            curtainOpenWidthDp = 400,
            horizontalPaddingDp = 16,
            playerMaxWidthDp = 1080
        )

        assertEquals(0, resolveCurtainWidthDp(TabletSideCurtainState.HIDDEN, policy))
        assertEquals(60, resolveCurtainWidthDp(TabletSideCurtainState.PEEK, policy))
        assertEquals(400, resolveCurtainWidthDp(TabletSideCurtainState.OPEN, policy))
    }

    @Test
    fun initialCurtainStateUsesScreenWidthBuckets() {
        assertEquals(
            TabletSideCurtainState.OPEN,
            resolveInitialCurtainState(widthDp = 1080)
        )
        assertEquals(
            TabletSideCurtainState.OPEN,
            resolveInitialCurtainState(widthDp = 1280)
        )
        assertEquals(
            TabletSideCurtainState.OPEN,
            resolveInitialCurtainState(widthDp = 1366)
        )
    }

    @Test
    fun autoBehaviorCollapsesOpenCurtainWhenPlaying() {
        assertEquals(
            TabletSideCurtainState.PEEK,
            resolveCurtainStateAfterAutoBehavior(
                currentState = TabletSideCurtainState.OPEN,
                isActivelyPlaying = true
            )
        )
    }

    @Test
    fun autoBehaviorAvoidsFullyHiddenCurtainWhenPaused() {
        assertEquals(
            TabletSideCurtainState.PEEK,
            resolveCurtainStateAfterAutoBehavior(
                currentState = TabletSideCurtainState.HIDDEN,
                isActivelyPlaying = false
            )
        )
    }

    @Test
    fun commentsTab_doesNotAutoSwitchWhileCommentsAreReloading() {
        assertEquals(
            0,
            resolveCinemaSideCurtainSelectedTab(
                currentSelectedTab = 0,
                replyCount = 0,
                isRepliesLoading = true,
                hasRelatedVideos = true,
                showRelatedVideosSection = true
            )
        )
    }

    @Test
    fun commentsTab_autoSwitchesToRelatedAfterLoadedEmptyComments() {
        assertEquals(
            1,
            resolveCinemaSideCurtainSelectedTab(
                currentSelectedTab = 0,
                replyCount = 0,
                isRepliesLoading = false,
                hasRelatedVideos = true,
                showRelatedVideosSection = true
            )
        )
    }

    @Test
    fun commentsTab_staysOnCommentsWhenRepliesExist() {
        assertEquals(
            0,
            resolveCinemaSideCurtainSelectedTab(
                currentSelectedTab = 0,
                replyCount = 12,
                isRepliesLoading = false,
                hasRelatedVideos = true,
                showRelatedVideosSection = true
            )
        )
    }

    @Test
    fun relatedTab_keepsCurrentSelection() {
        assertEquals(
            1,
            resolveCinemaSideCurtainSelectedTab(
                currentSelectedTab = 1,
                replyCount = 0,
                isRepliesLoading = false,
                hasRelatedVideos = true,
                showRelatedVideosSection = true
            )
        )
    }

    @Test
    fun relatedTab_isForcedBackToCommentsWhenSectionHidden() {
        assertEquals(
            0,
            resolveCinemaSideCurtainSelectedTab(
                currentSelectedTab = 1,
                replyCount = 0,
                isRepliesLoading = false,
                hasRelatedVideos = true,
                showRelatedVideosSection = false
            )
        )
    }

    @Test
    fun cinemaMetaBlocksIncludeUpInfoWhenOwnerIsAvailable() {
        val blocks = resolveCinemaMetaPanelBlocks(
            hasOwner = true,
            hasCollection = false,
            hasMultiplePages = false
        )

        assertEquals(
            listOf(
                CinemaMetaPanelBlock.ACTIONS,
                CinemaMetaPanelBlock.UP_INFO,
                CinemaMetaPanelBlock.INTRO
            ),
            blocks
        )
    }

    @Test
    fun cinemaMetaBlocksSkipUpInfoWhenOwnerIsMissing() {
        val blocks = resolveCinemaMetaPanelBlocks(
            hasOwner = false,
            hasCollection = false,
            hasMultiplePages = false
        )

        assertFalse(blocks.contains(CinemaMetaPanelBlock.UP_INFO))
        assertEquals(
            listOf(
                CinemaMetaPanelBlock.ACTIONS,
                CinemaMetaPanelBlock.INTRO
            ),
            blocks
        )
    }

    @Test
    fun cinemaMetaBlocksAppendCollectionAndPagesWhenAvailable() {
        val blocks = resolveCinemaMetaPanelBlocks(
            hasOwner = true,
            hasCollection = true,
            hasMultiplePages = true
        )

        assertEquals(
            listOf(
                CinemaMetaPanelBlock.ACTIONS,
                CinemaMetaPanelBlock.UP_INFO,
                CinemaMetaPanelBlock.INTRO,
                CinemaMetaPanelBlock.COLLECTION,
                CinemaMetaPanelBlock.PAGES
            ),
            blocks
        )
    }

    @Test
    fun cinemaMetaBlocksOnlyAppendAvailableSupplementalSections() {
        val blocks = resolveCinemaMetaPanelBlocks(
            hasOwner = false,
            hasCollection = true,
            hasMultiplePages = false
        )

        assertEquals(
            listOf(
                CinemaMetaPanelBlock.ACTIONS,
                CinemaMetaPanelBlock.INTRO,
                CinemaMetaPanelBlock.COLLECTION
            ),
            blocks
        )
    }
}
