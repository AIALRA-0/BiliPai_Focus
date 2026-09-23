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
    fun collapsedPlayerNavigationBarScopesMiuixFacadeToNonGlassMode() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreenStateHolder.kt")
            .readText()
        val navigationBar = source
            .substringAfter("private fun CollapsedPlayerNavigationBar(")
            .substringBefore("@androidx.annotation.OptIn")

        assertTrue(navigationBar.contains("val useMiuixNonGlassChrome = isMiuixNonGlassEnabled()"))
        assertTrue(navigationBar.contains("AppIconButton("))
        assertTrue(navigationBar.contains("AppIcon("))
        assertTrue(navigationBar.contains("AppText(\"立即播放\""))
        assertTrue(navigationBar.contains("AppChromeSizeTokens.MinimumTouchTarget"))
        assertTrue(navigationBar.contains("IconButton(onClick = onBack"))
        assertTrue(navigationBar.contains("size(width = 42.dp, height = 34.dp)"))
        assertTrue(navigationBar.contains("Text(\"立即播放\""))
    }

    @Test
    fun portraitCollapseKeepsVideoCanvasWithoutImmediatePlayToolbar() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreenStateHolder.kt")
            .readText()
        val playerHost = source
            .substringAfter("if (!isVerticalVideo) {")
            .substringBefore("Box(\n                            modifier = Modifier")

        assertTrue(playerHost.contains("CollapsedPlayerNavigationBar("))
        assertFalse(playerHost.contains("isPortraitVideo"))

        val collapsedHeightPolicy = source
            .substringAfter("val collapsedViewportHeight = when {")
            .substringBefore("val inlineViewportHeight = lerp(")
        assertTrue(
            collapsedHeightPolicy.indexOf("useOfficialInlinePortraitDetailExperience") <
                collapsedHeightPolicy.indexOf("portraitPlayerCollapseMode != PortraitPlayerCollapseMode.OFF")
        )
        assertTrue(collapsedHeightPolicy.contains("collapsedPortraitInlineSpec.heightDp.dp"))
    }

    @Test
    fun localBack_prefersPortraitFullscreenOverLandscapeFullscreen() {
        assertEquals(
            VideoDetailLocalBackTarget.EXIT_PORTRAIT_FULLSCREEN,
            resolveVideoDetailLocalBackTarget(
                isLandscapeFullscreen = true,
                isPortraitFullscreen = true,
            )
        )
        assertEquals(
            VideoDetailLocalBackTarget.EXIT_LANDSCAPE_FULLSCREEN,
            resolveVideoDetailLocalBackTarget(
                isLandscapeFullscreen = true,
                isPortraitFullscreen = false,
            )
        )
        assertEquals(
            VideoDetailLocalBackTarget.NAVIGATE_BACK,
            resolveVideoDetailLocalBackTarget(
                isLandscapeFullscreen = false,
                isPortraitFullscreen = false,
            )
        )
        assertEquals(
            VideoDetailLocalBackTarget.NAVIGATE_BACK,
            resolveVideoDetailLocalBackTarget(
                isLandscapeFullscreen = false,
                isPortraitFullscreen = true,
                directPortraitEntry = true,
            )
        )
    }

    @Test
    fun landscapeNaturalCover_backOnlyConsumesUserRequestedFullscreen() {
        val orientationDrivenFullscreen = false
        val initialFullscreen = resolveVideoDetailFullscreenMode(
            isOrientationDrivenFullscreen = orientationDrivenFullscreen,
            isLandscape = true,
            userRequestedFullscreen = false,
            isInMultiWindowMode = false,
        )
        val requestedFullscreen = resolveVideoDetailFullscreenMode(
            isOrientationDrivenFullscreen = orientationDrivenFullscreen,
            isLandscape = true,
            userRequestedFullscreen = true,
            isInMultiWindowMode = false,
        )

        assertEquals(
            VideoDetailLocalBackTarget.NAVIGATE_BACK,
            resolveVideoDetailLocalBackTarget(
                isLandscapeFullscreen = initialFullscreen,
                isPortraitFullscreen = false,
            )
        )
        assertEquals(
            VideoDetailLocalBackTarget.EXIT_LANDSCAPE_FULLSCREEN,
            resolveVideoDetailLocalBackTarget(
                isLandscapeFullscreen = requestedFullscreen,
                isPortraitFullscreen = false,
            )
        )
    }

    @Test
    fun portraitExitPlayerTarget_prefersCurrentInternalBvidOverRouteBvid() {
        val resolved = resolveVideoPlayerSectionTarget(
            routeBvid = "BV_ROUTE",
            routeCoverUrl = "https://img/route.jpg",
            currentBvid = "BV_PORTRAIT_NEXT"
        )

        assertEquals("BV_PORTRAIT_NEXT", resolved.bvid)
        assertEquals("", resolved.entryCoverUrl)
        // Shared-element key stays on route entry so in-page switch does not rekey the surface.
        assertEquals("BV_ROUTE", resolved.sharedElementBvid)
    }

    @Test
    fun portraitExitPlayerTarget_usesSwitchedCoverWhenInternalBvidDiffers() {
        val resolved = resolveVideoPlayerSectionTarget(
            routeBvid = "BV_ROUTE",
            routeCoverUrl = "https://img/route.jpg",
            currentBvid = "BV_COLLECTION_NEXT",
            switchedCoverUrl = "https://img/episode.jpg"
        )

        assertEquals("BV_COLLECTION_NEXT", resolved.bvid)
        assertEquals("https://img/episode.jpg", resolved.entryCoverUrl)
        assertEquals("BV_ROUTE", resolved.sharedElementBvid)
    }

    @Test
    fun portraitExitPlayerTarget_doesNotFallBackToRouteCoverForPortraitNextWithoutSwitchedCover() {
        // 竖屏已切到下一片但尚未写入 switchedCover 时，entry 应为空，
        // 避免 resolvePreferredVideoCoverUrl 用路由首个视频封面冒充当前片。
        val resolved = resolveVideoPlayerSectionTarget(
            routeBvid = "BV_ROUTE",
            routeCoverUrl = "https://img/first.jpg",
            currentBvid = "BV_PORTRAIT_NEXT",
            switchedCoverUrl = "",
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
        assertEquals("BV_ROUTE", resolved.sharedElementBvid)
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
        assertEquals("BV_ROUTE", resolved.sharedElementBvid)
    }

    @Test
    fun ugcSeasonEpisodeCover_prefersExactCidMatch() {
        val season = UgcSeason(
            sections = listOf(
                UgcSection(
                    episodes = listOf(
                        UgcEpisode(
                            bvid = "BV1",
                            cid = 11L,
                            arc = com.android.purebilibili.data.model.response.UgcEpisodeArc(
                                pic = "https://img/ep1.jpg"
                            )
                        ),
                        UgcEpisode(
                            bvid = "BV1",
                            cid = 22L,
                            arc = com.android.purebilibili.data.model.response.UgcEpisodeArc(
                                pic = "https://img/ep2.jpg"
                            )
                        )
                    )
                )
            )
        )

        assertEquals(
            "https://img/ep2.jpg",
            resolveUgcSeasonEpisodeCoverUrl(
                ugcSeason = season,
                targetBvid = "BV1",
                targetCid = 22L
            )
        )
        assertEquals(
            "https://img/ep1.jpg",
            resolveUgcSeasonEpisodeCoverUrl(
                ugcSeason = season,
                targetBvid = "BV1",
                targetCid = 0L
            )
        )
    }

    @Test
    fun initialVerticalRouteHint_doesNotBypassInlinePresentationPolicy() {
        assertFalse(
            shouldStartInPortraitFullscreenFromRouteHint(
                autoEnterPortraitFromRoute = true,
                startAudioFromRoute = false,
                initialVerticalFromRoute = true
            )
        )
    }

    @Test
    fun secondaryNavigationCallbacks_deferPlaybackExitToNavigationLayer() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreenStateHolder.kt")
            .readText()
        val userSpaceSource = source.substringAfter("val navigateToUserSpaceFromVideo")
            .substringBefore("val navigateToSearchFromVideo")

        assertFalse(userSpaceSource.contains("markSecondaryNavigationLeave"))
        assertTrue(userSpaceSource.contains("onUpClick(mid)"))
    }

    @Test
    fun videoNavigationInsideDetailSwitchesCurrentPageWithoutPushingRoute() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreenStateHolder.kt")
            .readText()
        val relatedVideoSource = source.substringAfter("val navigateToRelatedVideo")
            .substringBefore("LaunchedEffect(bvid, cid)")

        assertTrue(relatedVideoSource.contains("shouldSwitchCollectionVideoInsideCurrentDetailPage("))
        assertTrue(relatedVideoSource.contains("switchVideoInCurrentDetailPage("))
        assertTrue(relatedVideoSource.contains("onVideoClick(targetBvid, navOptions)"))
        assertTrue(relatedVideoSource.contains("captureVideoDetailParentUiSnapshot("))
        assertFalse(relatedVideoSource.contains("captureVideoDetailParentFreezeFrame("))
        assertFalse(relatedVideoSource.contains("withTimeoutOrNull(320L)"))
        assertFalse(relatedVideoSource.contains("PixelCopy.request("))
        assertTrue(
            relatedVideoSource.indexOf("restoreVideoDetailParentUiSnapshot(") <
                relatedVideoSource.indexOf(
                    "canReleaseRelatedVideoNavigation("
                )
        )
        // 相关推荐 push / 同页切集前必须清掉单例弹幕会话，避免新页绑定时弹幕不显示。
        assertTrue(relatedVideoSource.contains("sharedDanmakuManager.clearForVideoChange()"))
        assertTrue(
            source.contains("presentationState.switchVideo(normalizedBvid, safeCid)") &&
                source.contains("viewModel.loadVideo(")
        )
        // Presentation state already changes the player identity. A forced request here races the
        // keyed player effect and can prepare the old player before the new player is attached.
        val switchSource = source.substringAfter("fun switchVideoInCurrentDetailPage")
            .substringBefore("val navigateToRelatedVideo")
        assertFalse(switchSource.contains("force = true"))
        assertTrue(switchSource.contains("resolveUgcSeasonEpisodeCoverUrl("))
        assertTrue(switchSource.contains("pendingInPageSwitchCoverUrl"))
        assertTrue(switchSource.contains("sharedDanmakuManager.clearForVideoChange()"))
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
    fun frozenCommentBar_visibilityDoesNotDependOnLiquidGlassToggle() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailPhoneContent.kt")
            .readText()

        assertFalse(source.contains("val videoDetailLiquidGlassEnabled"))
        assertFalse(source.contains("isLiquidGlassEnabled = videoDetailLiquidGlassEnabled"))
        assertTrue(source.contains("val showFrozenCommentBar = shouldShowVideoDetailBottomInteractionBar("))
        // Visibility stays independent; reuse only switches floating liquid chrome.
        assertTrue(source.contains("shouldUseFloatingLiquidBottomInputBar("))
        assertTrue(source.contains("resolveBottomInputBarContentBottomPadding("))
        assertTrue(source.contains("val bottomInputBarBackdrop = rememberLayerBackdrop()"))
        assertTrue(source.contains(".layerBackdrop(bottomInputBarBackdrop)"))
        assertTrue(source.contains("backdrop = if (floatingLiquidBottomInputBar)"))
        assertTrue(source.contains("hazeState = hazeState"))
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
    fun relatedVideoCardsKeepSharedTransitionAfterParentDetailEntry() {
        val phoneSource = File(
            "src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailPhoneContent.kt"
        ).readText()
        val contentSource = File(
            "src/main/java/com/android/purebilibili/feature/video/screen/VideoContentSection.kt"
        ).readText()
        val relatedCardSource = contentSource
            .substringAfter("RelatedVideoItem(")
            .substringBefore("onClick = openRelatedVideo")

        assertFalse(phoneSource.contains("relatedVideoTransitionEnabled"))
        assertFalse(contentSource.contains("relatedVideoTransitionEnabled"))
        assertFalse(relatedCardSource.contains("sharedTransitionEnabled"))
    }

    @Test
    fun nestedVideoDetailScopesMiuixChromeAndLandingToTheActiveEntry() {
        val stateHolderSource = File(
            "src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreenStateHolder.kt"
        ).readText()
        val transitionHostSource = File(
            "src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailTransitionHost.kt"
        ).readText()

        assertTrue(transitionHostSource.contains("entryOwnsMiuixCardTransition"))
        assertTrue(
            transitionHostSource.contains(
                "activeSourceRoute = transitionBackgroundState.sourceRouteProvider()"
            )
        )
        assertTrue(
            stateHolderSource.contains(
                ".sourceChromeSnapshot\n            .takeIf { miuixVisualAssetsActive }"
            )
        )
        assertTrue(
            stateHolderSource.contains(
                "shouldConsumeMiuixTransitionVisualAssets("
            )
        )
    }

    @Test
    fun tabletRelatedVideoCardsUseTheSameSharedTransition() {
        val tabletSource = File(
            "src/main/java/com/android/purebilibili/feature/video/screen/TabletVideoLayout.kt"
        ).readText()
        val cinemaSource = File(
            "src/main/java/com/android/purebilibili/feature/video/screen/TabletCinemaLayout.kt"
        ).readText()

        assertTrue(tabletSource.contains("transitionEnabled = LocalSharedTransitionEnabled.current"))
        assertTrue(cinemaSource.contains("transitionEnabled = LocalSharedTransitionEnabled.current"))
        assertTrue(tabletSource.contains("LocalVideoCardSharedElementSourceRoute provides"))
        assertTrue(cinemaSource.contains("LocalVideoCardSharedElementSourceRoute provides"))
    }

    @Test
    fun videoCommentTab_removesInlineComposerAndKeepsBottomComposerEntry() {
        val contentSource = File("src/main/java/com/android/purebilibili/feature/video/screen/VideoContentSection.kt")
            .readText()
        val detailSource = File("src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailPhoneContent.kt")
            .readText()
        val commentTabSource = contentSource
            .substringAfter("private fun VideoCommentTab(")
            .substringBefore("private fun VideoHeaderContent(")
        val bottomInputBarSource = detailSource
            .substringAfter("BottomInputBar(")
            .substringBefore("if (shouldShowExternalPlaylistQueueBar)")

        assertFalse(commentTabSource.contains("说点什么，直接评论 UP 主和大家"))
        assertFalse(commentTabSource.contains("onRootCommentClick"))
        assertTrue(bottomInputBarSource.contains("onCommentClick = {"))
        assertTrue(bottomInputBarSource.contains("playbackActions.openRootCommentComposer()"))
    }
}
