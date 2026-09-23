package com.android.purebilibili.feature.video.screen

import com.android.purebilibili.core.store.TabletCommentPanelWidthPreset
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TabletCinemaLayoutPolicyTest {

    @Test
    fun cinemaOpenCurtainHostsDanmakuSendAndToggle() {
        val source = File(
            "src/main/java/com/android/purebilibili/feature/video/screen/TabletCinemaLayout.kt"
        ).readText()

        assertTrue(source.contains("TabletSecondaryDanmakuActions("))
        assertTrue(source.contains("playbackActions.showDanmakuSendDialog"))
        assertTrue(source.contains("onDanmakuInputClick = playbackActions.showDanmakuSendDialog"))
        assertTrue(shouldShowTabletCinemaDanmakuActions(TabletSideCurtainState.OPEN))
        assertFalse(shouldShowTabletCinemaDanmakuActions(TabletSideCurtainState.PEEK))
        assertTrue(shouldShowTabletSecondaryDanmakuActions())
    }

    @Test
    fun cinemaCommentsReuseTheFullCommentAndEngagementDock() {
        val source = File(
            "src/main/java/com/android/purebilibili/feature/video/screen/TabletCinemaLayout.kt"
        ).readText()
        val commentsPane = source
            .substringAfter("private fun CinemaCommentsPane(")
            .substringBefore("private fun CinemaRelatedPane(")

        assertTrue(commentsPane.contains("BottomInputBar("))
        assertTrue(commentsPane.contains("engagementActions.toggleLike"))
        assertTrue(commentsPane.contains("engagementActions.openCoinDialog"))
        assertTrue(commentsPane.contains("playbackActions.openRootCommentComposer"))
        assertFalse(commentsPane.contains("写评论，直接和 UP 主交流"))
    }

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
            widthDp = 1280,
            commentWidthPreset = TabletCommentPanelWidthPreset.STANDARD
        )

        assertTrue(policy.curtainPeekWidthDp in 61..64)
        assertTrue(policy.curtainOpenWidthDp in 379..382)
        assertTrue(policy.horizontalPaddingDp in 16..17)
        assertTrue(policy.playerMaxWidthDp in 1090..1100)
    }

    @Test
    fun cinemaPlayerViewportUsesActualPrimaryPaneWidthWhenCurtainIsOpen() {
        assertEquals(
            600,
            resolveCinemaPlayerViewportWidthDp(
                availableWidthDp = 600,
                playerMaxWidthDp = 1095,
            )
        )
    }

    @Test
    fun cinemaPlayerViewportStillHonorsPlayerWidthCapOnUltraWideScreens() {
        assertEquals(
            1280,
            resolveCinemaPlayerViewportWidthDp(
                availableWidthDp = 1400,
                playerMaxWidthDp = 1280,
            )
        )
    }

    @Test
    fun cinemaPlayerViewportIsForwardedToPlayerChrome() {
        val cinemaSource = File(
            "src/main/java/com/android/purebilibili/feature/video/screen/TabletCinemaLayout.kt"
        ).readText()
        val playerSource = File(
            "src/main/java/com/android/purebilibili/feature/video/ui/section/VideoPlayerSection.kt"
        ).readText()
        val overlaySource = File(
            "src/main/java/com/android/purebilibili/feature/video/ui/overlay/VideoPlayerOverlay.kt"
        ).readText()

        assertTrue(cinemaSource.contains("viewportWidthDpOverride = playerViewportWidthDp"))
        assertTrue(playerSource.contains("viewportWidthDpOverride = uiLayoutWidthDp"))
        assertTrue(overlaySource.contains("viewportWidthDpOverride = viewportWidthDpOverride"))
    }

    @Test
    fun commentWidthPresetsScaleCurtainWidthAroundCurrentStandard() {
        val compact = resolveTabletCinemaLayoutPolicy(
            widthDp = 1280,
            commentWidthPreset = TabletCommentPanelWidthPreset.COMPACT
        )
        val standard = resolveTabletCinemaLayoutPolicy(
            widthDp = 1280,
            commentWidthPreset = TabletCommentPanelWidthPreset.STANDARD
        )
        val wide = resolveTabletCinemaLayoutPolicy(
            widthDp = 1280,
            commentWidthPreset = TabletCommentPanelWidthPreset.WIDE
        )
        val ultraWide = resolveTabletCinemaLayoutPolicy(
            widthDp = 1280,
            commentWidthPreset = TabletCommentPanelWidthPreset.ULTRA_WIDE
        )

        assertTrue(compact.curtainOpenWidthDp < standard.curtainOpenWidthDp)
        assertTrue(wide.curtainOpenWidthDp > standard.curtainOpenWidthDp)
        assertTrue(ultraWide.curtainOpenWidthDp >= wide.curtainOpenWidthDp)
    }

    @Test
    fun compactTabletClampsWideCurtainToProtectPlayerSpace() {
        val ultraWide = resolveTabletCinemaLayoutPolicy(
            widthDp = 960,
            commentWidthPreset = TabletCommentPanelWidthPreset.ULTRA_WIDE
        )

        assertEquals(332, ultraWide.curtainOpenWidthDp)
    }

    @Test
    fun ultraWideScreenCapsCommentCurtainWidth() {
        val ultraWide = resolveTabletCinemaLayoutPolicy(
            widthDp = 1920,
            commentWidthPreset = TabletCommentPanelWidthPreset.ULTRA_WIDE
        )

        assertEquals(560, ultraWide.curtainOpenWidthDp)
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
    fun initialCurtainStateOpensOnAllTabletWidths() {
        assertEquals(
            TabletSideCurtainState.OPEN,
            resolveInitialCurtainState(widthDp = 840)
        )
        assertEquals(
            TabletSideCurtainState.OPEN,
            resolveInitialCurtainState(widthDp = 960)
        )
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
                hasRelatedVideos = true
            )
        )
    }

    @Test
    fun commentsTab_staysOnCommentsWhenLoadedEmpty() {
        assertEquals(
            0,
            resolveCinemaSideCurtainSelectedTab(
                currentSelectedTab = 0,
                replyCount = 0,
                isRepliesLoading = false,
                hasRelatedVideos = true
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
                hasRelatedVideos = true
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
                hasRelatedVideos = true
            )
        )
    }

    @Test
    fun cinemaMetaBlocksFoldUpInfoIntoActionsBlock() {
        val blocks = resolveCinemaMetaPanelBlocks(
            hasCollection = false,
            hasMultiplePages = false
        )

        assertEquals(
            listOf(
                CinemaMetaPanelBlock.ACTIONS,
                CinemaMetaPanelBlock.INTRO
            ),
            blocks
        )
    }

    @Test
    fun cinemaMetaBlocksKeepStableBaseOrderWithoutOwnerParameter() {
        val blocks = resolveCinemaMetaPanelBlocks(
            hasCollection = false,
            hasMultiplePages = false
        )

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
            hasCollection = true,
            hasMultiplePages = true
        )

        assertEquals(
            listOf(
                CinemaMetaPanelBlock.ACTIONS,
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
