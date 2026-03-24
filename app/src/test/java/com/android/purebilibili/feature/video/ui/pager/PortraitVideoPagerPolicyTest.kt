package com.android.purebilibili.feature.video.ui.pager

import com.android.purebilibili.data.model.response.RelatedVideo
import androidx.media3.common.Player
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
    fun sharedPlayerEntry_reusesExistingFrameWhenSharedPlayerAlreadyHasVideoSize() {
        assertEquals(
            0,
            resolvePortraitInitialRenderedFirstFrameGeneration(
                useSharedPlayer = true,
                sharedPlayerHasFrameAtEntry = true
            )
        )
    }
}
