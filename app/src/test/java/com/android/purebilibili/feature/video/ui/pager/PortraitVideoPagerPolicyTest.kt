package com.android.purebilibili.feature.video.ui.pager

import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.RelatedVideo
import com.android.purebilibili.data.model.response.Stat
import com.android.purebilibili.data.model.response.ViewInfo
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import com.android.purebilibili.feature.video.viewmodel.PlayerUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PortraitVideoPagerPolicyTest {

    @Test
    fun resolvePortraitInitialPageIndex_returnsFirstPageWhenInitialMatchesInfo() {
        val index = resolvePortraitInitialPageIndex(
            initialBvid = "BV1",
            initialInfoBvid = "BV1",
            recommendations = listOf(RelatedVideo(bvid = "BV2"))
        )

        assertEquals(0, index)
    }

    @Test
    fun resolvePortraitInitialPageIndex_pointsToRecommendationWhenMatched() {
        val index = resolvePortraitInitialPageIndex(
            initialBvid = "BV3",
            initialInfoBvid = "BV1",
            recommendations = listOf(
                RelatedVideo(bvid = "BV2"),
                RelatedVideo(bvid = "BV3"),
                RelatedVideo(bvid = "BV4")
            )
        )

        assertEquals(2, index)
    }

    @Test
    fun resolvePortraitInitialPageIndex_fallsBackToFirstPageWhenNotFound() {
        val index = resolvePortraitInitialPageIndex(
            initialBvid = "BV9",
            initialInfoBvid = "BV1",
            recommendations = listOf(RelatedVideo(bvid = "BV2"))
        )

        assertEquals(0, index)
    }

    @Test
    fun resolvePortraitPagerRepeatMode_defaultsToOffForOrderedPlayback() {
        assertEquals(Player.REPEAT_MODE_OFF, resolvePortraitPagerRepeatMode())
    }

    @Test
    fun sharedPlayerSurfaceRebindPolicy_requiresCurrentReadyPageWithVideoFrame() {
        assertTrue(
            shouldRebindSharedPlayerSurfaceOnAttach(
                isCurrentPage = true,
                isPlayerReadyForThisVideo = true,
                hasPlayerView = true,
                videoWidth = 720,
                videoHeight = 1280
            )
        )
    }

    @Test
    fun sharedPlayerSurfaceRebindPolicy_allowsCurrentPageRebindBeforeVideoSizeAvailable() {
        assertTrue(
            shouldRebindSharedPlayerSurfaceOnAttach(
                isCurrentPage = true,
                isPlayerReadyForThisVideo = true,
                hasPlayerView = true,
                videoWidth = 0,
                videoHeight = 1280
            )
        )
    }

    @Test
    fun sharedPlayerSurfaceRebindPolicy_skipsWhenPageIsNotReady() {
        assertFalse(
            shouldRebindSharedPlayerSurfaceOnAttach(
                isCurrentPage = false,
                isPlayerReadyForThisVideo = true,
                hasPlayerView = true,
                videoWidth = 720,
                videoHeight = 1280
            )
        )
    }

    @Test
    fun initialAspectRatio_resetsToPortraitFallbackWhenTargetVideoNotReady() {
        assertEquals(
            9f / 16f,
            resolvePortraitInitialVideoAspectRatio(
                itemBvid = "BV_NEXT",
                currentPlayingBvid = "BV_PREV",
                playerVideoWidth = 1920,
                playerVideoHeight = 1080
            )
        )
    }

    @Test
    fun initialAspectRatio_usesPlayerVideoSizeWhenTargetVideoAlreadyReady() {
        assertEquals(
            9f / 16f,
            resolvePortraitInitialVideoAspectRatio(
                itemBvid = "BV_CURRENT",
                currentPlayingBvid = "BV_CURRENT",
                playerVideoWidth = 720,
                playerVideoHeight = 1280
            )
        )
    }

    @Test
    fun runtimeAspectRatio_keepsKnownPortraitAspectWhenPlayerReportsRotatedLandscapeSize() {
        assertEquals(
            9f / 16f,
            resolvePortraitRuntimeVideoAspectRatio(
                knownVideoAspectRatio = 9f / 16f,
                playerVideoWidth = 1920,
                playerVideoHeight = 1080
            )
        )
    }

    @Test
    fun runtimeAspectRatio_usesPlayerAspectWhenOrientationMatchesKnownVideo() {
        assertEquals(
            720f / 1280f,
            resolvePortraitRuntimeVideoAspectRatio(
                knownVideoAspectRatio = 9f / 16f,
                playerVideoWidth = 720,
                playerVideoHeight = 1280
            )
        )
    }

    @Test
    fun portraitViewportSize_canFillContainerForImmersivePlayback() {
        val layout = resolvePortraitVideoViewportSize(
            containerWidth = 1200,
            containerHeight = 800,
            currentVideoAspect = 16f / 9f,
            fillContainer = true
        )

        assertEquals(1200, layout.width)
        assertEquals(800, layout.height)
    }

    @Test
    fun portraitViewportSize_fitModeKeepsAspectInsideContainer() {
        val layout = resolvePortraitVideoViewportSize(
            containerWidth = 1200,
            containerHeight = 800,
            currentVideoAspect = 16f / 9f,
            fillContainer = false
        )

        assertEquals(1200, layout.width)
        assertEquals(675, layout.height)
    }

    @Test
    fun portraitPager_defaultViewportPolicy_doesNotForceFillContainer() {
        assertFalse(resolvePortraitPagerFillContainer())
    }

    @Test
    fun portraitPager_defaultResizeMode_prefersFit() {
        assertEquals(
            AspectRatioFrameLayout.RESIZE_MODE_FIT,
            resolvePortraitPagerResizeMode()
        )
    }

    @Test
    fun portraitDanmakuSurface_usesFullPageForLandscapeVideo() {
        assertEquals(
            PortraitDanmakuSurfaceMode.Page,
            resolvePortraitDanmakuSurfaceMode(currentVideoAspect = 16f / 9f)
        )
    }

    @Test
    fun portraitDanmakuSurface_keepsViewportForPortraitVideo() {
        assertEquals(
            PortraitDanmakuSurfaceMode.VideoViewport,
            resolvePortraitDanmakuSurfaceMode(currentVideoAspect = 9f / 16f)
        )
    }

    @Test
    fun portraitDanmakuReadableFontScale_boostsDefaultWithoutExceedingSettingsLimit() {
        assertEquals(1.18f, resolvePortraitDanmakuReadableFontScale(1f))
        assertEquals(2.0f, resolvePortraitDanmakuReadableFontScale(1.9f))
    }

    @Test
    fun sharedPlayerEntry_reusesExistingFrameWhenSharedPlayerAlreadyHasVideoSize() {
        assertEquals(
            0,
            resolvePortraitInitialRenderedFirstFrameGeneration(
                useSharedPlayer = true,
                sharedPlayerHasFrameAtEntry = true
            )
        )
    }

    @Test
    fun portraitInteraction_staysEnabledForCurrentPageEvenWhenSharedModelStillPointsToPreviousVideo() {
        assertTrue(
            shouldHandlePortraitVideoInteraction(
                isCurrentPage = true,
                aid = 2002L,
                bvid = "BV_NEXT"
            )
        )
    }

    @Test
    fun portraitFavoriteTap_opensFavoriteFoldersInsteadOfImmediateDefaultFavorite() {
        assertEquals(
            PortraitFavoriteAction.OpenFavoriteFolders,
            resolvePortraitFavoriteAction()
        )
    }

    @Test
    fun portraitInteractionUi_prefersLocalOverrideWhenSharedPlayerStateBelongsToAnotherVideo() {
        val sharedState = PlayerUiState.Success(
            info = ViewInfo(
                bvid = "BV_PREV",
                aid = 1001L,
                owner = Owner(mid = 1L, name = "up"),
                stat = Stat(like = 20, favorite = 10)
            ),
            playUrl = "https://example.com/video.mp4",
            isLiked = true,
            isFavorited = true
        )

        val resolved = resolvePortraitVideoInteractionUiState(
            targetBvid = "BV_NEXT",
            fallbackStat = Stat(like = 8, favorite = 3),
            sharedState = sharedState,
            localOverride = PortraitVideoInteractionOverride(
                isLiked = true,
                isFavorited = true,
                likeCount = 9,
                favoriteCount = 4
            )
        )

        assertTrue(resolved.isLiked)
        assertTrue(resolved.isFavorited)
        assertEquals(9, resolved.likeCount)
        assertEquals(4, resolved.favoriteCount)
    }

    @Test
    fun portraitRecommendationSnapshot_extractsStableBvidSetFromMixedPageItems() {
        assertEquals(
            setOf("BV_INFO", "BV_RELATED"),
            snapshotPortraitPageBvids(
                listOf(
                    ViewInfo(bvid = "BV_INFO", aid = 1L),
                    RelatedVideo(bvid = "BV_RELATED", aid = 2L),
                    "ignored"
                )
            )
        )
    }
}
