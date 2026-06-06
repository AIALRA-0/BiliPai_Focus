package com.android.purebilibili.feature.video.screen

import com.android.purebilibili.data.model.response.UgcEpisode
import com.android.purebilibili.data.model.response.UgcSeason
import com.android.purebilibili.data.model.response.UgcSection
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoDetailScreenPolicyTest {

    @Test
    fun portraitExitPlayerTarget_prefersCurrentInternalBvidOverRouteBvid() {
        val resolved = resolveVideoPlayerSectionTarget(
            routeBvid = "BV_ROUTE",
            routeCoverUrl = "https://img/route.jpg",
            currentBvid = "BV_PORTRAIT_NEXT"
        )

        assertEquals("BV_PORTRAIT_NEXT", resolved.bvid)
        assertEquals("", resolved.entryCoverUrl)
    }

    @Test
    fun portraitExitPlayerTarget_keepsRouteCoverWhenStillShowingRouteVideo() {
        val resolved = resolveVideoPlayerSectionTarget(
            routeBvid = "BV_ROUTE",
            routeCoverUrl = "https://img/route.jpg",
            currentBvid = "BV_ROUTE"
        )

        assertEquals("BV_ROUTE", resolved.bvid)
        assertEquals("https://img/route.jpg", resolved.entryCoverUrl)
    }

    @Test
    fun portraitExitPlayerTarget_fallsBackToRouteWhenInternalTargetMissing() {
        val resolved = resolveVideoPlayerSectionTarget(
            routeBvid = "BV_ROUTE",
            routeCoverUrl = "https://img/route.jpg",
            currentBvid = ""
        )

        assertEquals("BV_ROUTE", resolved.bvid)
        assertEquals("https://img/route.jpg", resolved.entryCoverUrl)
    }

    @Test
    fun initialVerticalRouteHint_startsPortraitFullscreenBeforeApiDimensionArrives() {
        assertTrue(
            shouldStartInPortraitFullscreenFromRouteHint(
                autoEnterPortraitFromRoute = true,
                startAudioFromRoute = false,
                initialVerticalFromRoute = true
            )
        )
    }

    @Test
    fun secondaryNavigationCallbacks_markExternalNavigationLeaveBeforeRouting() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreen.kt")
            .readText()
        val userSpaceSource = source.substringAfter("val navigateToUserSpaceFromVideo")
            .substringBefore("val navigateToSearchFromVideo")

        assertTrue(
            userSpaceSource.contains("markSecondaryNavigationLeave()") &&
                userSpaceSource.indexOf("markSecondaryNavigationLeave()") <
                userSpaceSource.indexOf("onUpClick(mid)")
        )
    }

    @Test
    fun videoNavigationInsideDetailSwitchesCurrentPageWithoutPushingRoute() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreen.kt")
            .readText()
        val relatedVideoSource = source.substringAfter("val navigateToRelatedVideo")
            .substringBefore("LaunchedEffect(bvid, cid)")

        assertTrue(relatedVideoSource.contains("shouldSwitchCollectionVideoInsideCurrentDetailPage("))
        assertTrue(relatedVideoSource.contains("switchVideoInCurrentDetailPage("))
        assertTrue(relatedVideoSource.contains("onVideoClick(targetBvid, navOptions)"))
        assertTrue(
            source.contains("currentBvid = normalizedBvid") &&
                source.contains("currentBvidCid = safeCid") &&
                source.contains("viewModel.loadVideo(")
        )
    }

    @Test
    fun collectionVideoNavigationSwitchesInsideCurrentDetailOnlyForSameCollection() {
        val season = UgcSeason(
            sections = listOf(
                UgcSection(
                    episodes = listOf(
                        UgcEpisode(bvid = "BV1A", cid = 1001L),
                        UgcEpisode(bvid = "BV2B", cid = 2002L)
                    )
                )
            )
        )

        assertTrue(
            shouldSwitchCollectionVideoInsideCurrentDetailPage(
                targetBvid = "BV2B",
                currentBvid = "BV1A",
                ugcSeason = season
            )
        )
        assertFalse(
            shouldSwitchCollectionVideoInsideCurrentDetailPage(
                targetBvid = "BV3C",
                currentBvid = "BV1A",
                ugcSeason = season
            )
        )
        assertFalse(
            shouldSwitchCollectionVideoInsideCurrentDetailPage(
                targetBvid = "BV1A",
                currentBvid = "BV1A",
                ugcSeason = season
            )
        )
    }

    @Test
    fun frozenCommentBar_usesExistingLiquidGlassGlobalToggle() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreen.kt")
            .readText()

        assertTrue(source.contains("SettingsManager.getHomeSettings(context)"))
        assertTrue(source.contains("val videoDetailLiquidGlassEnabled = homeSettings.isLiquidGlassEnabled"))
        assertTrue(source.contains("isLiquidGlassEnabled = videoDetailLiquidGlassEnabled"))
        assertTrue(source.contains("val showFrozenCommentBar = shouldShowVideoDetailBottomInteractionBar("))
    }

    @Test
    fun videoContentSection_reportsCommentScrollAndAcceptsBottomPadding() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/screen/VideoContentSection.kt")
            .readText()

        assertTrue(source.contains("onCommentScrollStateChange: (Int, Int) -> Unit"))
        assertTrue(source.contains("bottomContentPadding: Dp"))
        assertTrue(
            source.contains(
                "snapshotFlow { commentListState.firstVisibleItemIndex to commentListState.firstVisibleItemScrollOffset }"
            )
        )
    }

    @Test
    fun videoCommentTab_removesInlineComposerAndKeepsBottomComposerEntry() {
        val contentSource = File("src/main/java/com/android/purebilibili/feature/video/screen/VideoContentSection.kt")
            .readText()
        val detailSource = File("src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreen.kt")
            .readText()
        val commentTabSource = contentSource
            .substringAfter("private fun VideoCommentTab(")
            .substringBefore("private fun VideoCommentBackToTopButton(")
        val bottomInputBarSource = detailSource
            .substringAfter("BottomInputBar(")
            .substringBefore("if (shouldShowExternalPlaylistQueueBar)")

        assertFalse(commentTabSource.contains("说点什么，直接评论 UP 主和大家"))
        assertFalse(commentTabSource.contains("onRootCommentClick"))
        assertTrue(bottomInputBarSource.contains("onCommentClick = {"))
        assertTrue(bottomInputBarSource.contains("viewModel.openRootCommentComposer()"))
    }
}
