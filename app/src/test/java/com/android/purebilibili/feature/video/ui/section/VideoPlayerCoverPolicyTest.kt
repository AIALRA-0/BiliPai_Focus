package com.android.purebilibili.feature.video.ui.section

import com.android.purebilibili.core.ui.transition.VideoSharedTransitionTargetMode
import com.android.purebilibili.core.ui.transition.VideoSharedTransitionPlaybackIntent
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoPlayerCoverPolicyTest {

    @Test
    fun immediatePlaybackCoverBecomesThePlayerUnderlay() {
        assertEquals(
            -1f,
            resolveVideoPlayerCoverLayerZIndex(
                playbackIntent = VideoSharedTransitionPlaybackIntent.ImmediatePlayback,
                forceCoverDuringReturnAnimation = false,
                shouldKeepCoverForManualStart = false,
            ),
        )
        assertEquals(
            100f,
            resolveVideoPlayerCoverLayerZIndex(
                playbackIntent = VideoSharedTransitionPlaybackIntent.CoverFirst,
                forceCoverDuringReturnAnimation = false,
                shouldKeepCoverForManualStart = true,
            ),
        )
    }

    @Test
    fun verticalVideo_fillsPlayerViewportDuringCoverPhase() {
        assertTrue(
            shouldFillPlayerViewportForManualStartCover(
                shouldKeepCoverForManualStart = false,
                forceCoverDuringReturnAnimation = false,
                isVerticalVideo = true
            )
        )
    }

    @Test
    fun returnCoverSharedBounds_doesNotForceViewportFill() {
        assertFalse(
            shouldFillPlayerViewportForManualStartCover(
                shouldKeepCoverForManualStart = true,
                forceCoverDuringReturnAnimation = true,
                isVerticalVideo = true
            )
        )
    }

    @Test
    fun horizontalManualStartCover_keepsInlineCoverContainer() {
        assertFalse(
            shouldFillPlayerViewportForManualStartCover(
                shouldKeepCoverForManualStart = true,
                forceCoverDuringReturnAnimation = false,
                isVerticalVideo = false
            )
        )
    }

    @Test
    fun verticalManualStartCover_canFillViewport() {
        assertTrue(
            shouldFillPlayerViewportForManualStartCover(
                shouldKeepCoverForManualStart = true,
                forceCoverDuringReturnAnimation = false,
                isVerticalVideo = true
            )
        )
    }

    @Test
    fun coverBootstrap_reusesFirstFrameButStillStartsFromCoverForReentry() {
        // 已出画后回首页再进：可跳过等首帧，但必须再走 smooth reveal，否则无封面过渡。
        val reused = resolveVideoPlayerCoverBootstrapState(
            forceCoverDuringReturnAnimation = false,
            shouldKeepCoverForManualStart = false,
            hasPersistedRenderedFirstFrame = true,
        )
        assertTrue(reused.isFirstFrameRendered)
        assertFalse(reused.hasStartedSmoothReveal)
        assertTrue(
            shouldStartSmoothCoverReveal(
                isFirstFrameRendered = reused.isFirstFrameRendered,
                forceCoverDuringReturnAnimation = false,
                shouldKeepCoverForManualStart = false,
            )
        )
        assertTrue(
            shouldCommitSmoothCoverReveal(
                isFirstFrameRendered = true,
                forceCoverDuringReturnAnimation = false,
                shouldKeepCoverForManualStart = false,
            )
        )
        assertTrue(
            shouldHoldEntryCoverUnderlay(
                isFirstFrameRendered = true,
                forceCoverDuringReturnAnimation = false,
                shouldKeepCoverForManualStart = false,
                hasStartedSmoothReveal = false,
            )
        )

        val forcedReturn = resolveVideoPlayerCoverBootstrapState(
            forceCoverDuringReturnAnimation = true,
            shouldKeepCoverForManualStart = false,
            hasPersistedRenderedFirstFrame = true,
        )
        assertFalse(forcedReturn.isFirstFrameRendered)
        assertFalse(forcedReturn.hasStartedSmoothReveal)

        val fresh = resolveVideoPlayerCoverBootstrapState(
            forceCoverDuringReturnAnimation = false,
            shouldKeepCoverForManualStart = false,
            hasPersistedRenderedFirstFrame = false,
        )
        assertFalse(fresh.isFirstFrameRendered)
        assertFalse(fresh.hasStartedSmoothReveal)
    }

    @Test
    fun coverBootstrap_fullscreenSwitchKeepsCurrentFrameVisible() {
        val fullscreenSwitch = resolveVideoPlayerCoverBootstrapState(
            forceCoverDuringReturnAnimation = false,
            shouldKeepCoverForManualStart = false,
            hasPersistedRenderedFirstFrame = false,
            preserveCurrentFrameOnFullscreenChange = true,
        )

        assertTrue(fullscreenSwitch.isFirstFrameRendered)
        assertTrue(fullscreenSwitch.hasStartedSmoothReveal)
        assertFalse(
            shouldShowCoverImage(
                isFirstFrameRendered = fullscreenSwitch.isFirstFrameRendered,
                forceCoverDuringReturnAnimation = false,
                shouldKeepCoverForManualStart = false,
                hasStartedSmoothReveal = fullscreenSwitch.hasStartedSmoothReveal,
            )
        )
    }

    @Test
    fun smoothRevealReset_onlyWhenForcedCoverOrManualStart() {
        assertTrue(
            shouldResetSmoothCoverReveal(
                forceCoverDuringReturnAnimation = true,
                shouldKeepCoverForManualStart = false,
            )
        )
        assertTrue(
            shouldResetSmoothCoverReveal(
                forceCoverDuringReturnAnimation = false,
                shouldKeepCoverForManualStart = true,
            )
        )
        // 首帧尚未到 / 仅缺 isFirstFrame 时不得清掉揭开标记
        assertFalse(
            shouldResetSmoothCoverReveal(
                forceCoverDuringReturnAnimation = false,
                shouldKeepCoverForManualStart = false,
            )
        )
    }

    @Test
    fun immediatePlayback_holdsOpaqueCoverUnderlayUntilFirstFrameReveal() {
        assertTrue(
            shouldHoldEntryCoverUnderlay(
                isFirstFrameRendered = false,
                forceCoverDuringReturnAnimation = false,
                shouldKeepCoverForManualStart = false,
                hasStartedSmoothReveal = false,
            )
        )
        assertTrue(
            shouldHoldEntryCoverUnderlay(
                isFirstFrameRendered = true,
                forceCoverDuringReturnAnimation = false,
                shouldKeepCoverForManualStart = false,
                hasStartedSmoothReveal = false,
            )
        )
        assertFalse(
            shouldHoldEntryCoverUnderlay(
                isFirstFrameRendered = true,
                forceCoverDuringReturnAnimation = false,
                shouldKeepCoverForManualStart = false,
                hasStartedSmoothReveal = true,
            )
        )
        assertFalse(
            shouldEnableCoverImageCrossfade(
                forceCoverDuringReturnAnimation = false,
                holdEntryCoverUnderlay = true,
            )
        )
        assertFalse(
            resolveVideoPlayerCoverMotionSpec(
                forceCoverDuringReturnAnimation = false,
                holdEntryCoverUnderlay = true,
            ).shouldAnimateFade
        )
    }

    @Test
    fun coverFirst_andReturn_keepUnderlayWithoutCrossfade() {
        assertTrue(
            shouldHoldEntryCoverUnderlay(
                isFirstFrameRendered = true,
                forceCoverDuringReturnAnimation = false,
                shouldKeepCoverForManualStart = true,
                hasStartedSmoothReveal = true,
            )
        )
        assertTrue(
            shouldHoldEntryCoverUnderlay(
                isFirstFrameRendered = true,
                forceCoverDuringReturnAnimation = true,
                shouldKeepCoverForManualStart = false,
                hasStartedSmoothReveal = true,
            )
        )
    }

    @Test
    fun autoPlayEnabled_neverUsesManualStartCoverUnderlay() {
        // 自动播放：即便尚未 playWhenReady、进度为 0，也不得进 manual-start 垫封面，
        // 否则会 INVISIBLE surface / 卡住揭开，合集换片或重进详情整页一直封面。
        assertFalse(
            shouldKeepCoverForManualStart(
                playWhenReady = false,
                currentPositionMs = 0L,
                autoPlayEnabled = true,
                hasManualStartPlaybackIntent = false
            )
        )
        assertFalse(
            shouldKeepCoverForManualStart(
                playWhenReady = true,
                currentPositionMs = 0L,
                autoPlayEnabled = true,
                hasManualStartPlaybackIntent = false
            )
        )
        assertFalse(
            shouldKeepCoverForManualStart(
                playWhenReady = false,
                currentPositionMs = 12_000L,
                autoPlayEnabled = true,
                hasManualStartPlaybackIntent = true
            )
        )
    }

    @Test
    fun manualStartCover_staysVisibleForSavedProgressBeforeUserPlay() {
        assertTrue(
            shouldKeepCoverForManualStart(
                playWhenReady = false,
                currentPositionMs = 98_000L,
                autoPlayEnabled = false,
                hasManualStartPlaybackIntent = false
            )
        )
    }

    @Test
    fun manualStartCover_staysVisibleBeforeLoadResetsFreshPlayerFlag() {
        assertTrue(
            shouldKeepCoverForManualStart(
                playWhenReady = true,
                currentPositionMs = 0L,
                autoPlayEnabled = false,
                hasManualStartPlaybackIntent = false
            )
        )
    }

    @Test
    fun manualStartCover_hidesAfterUserPlayIntentEvenWithSavedProgress() {
        assertFalse(
            shouldKeepCoverForManualStart(
                playWhenReady = false,
                currentPositionMs = 98_000L,
                autoPlayEnabled = false,
                hasManualStartPlaybackIntent = true
            )
        )
    }

    @Test
    fun manualStartCover_hidesWhenAutoPlayOverrideStartsPlayback() {
        assertFalse(
            shouldKeepCoverForManualStart(
                playWhenReady = true,
                currentPositionMs = 0L,
                autoPlayEnabled = false,
                hasManualStartPlaybackIntent = true
            )
        )
    }

    @Test
    fun horizontalManualStartCover_usesCoverSharedBoundsWithoutViewportFill() {
        val spec = resolveVideoPlayerEntryPresentationSpec(
            shouldKeepCoverForManualStart = true,
            forceCoverDuringReturnAnimation = false,
            isVerticalVideo = false,
            targetMode = VideoSharedTransitionTargetMode.InlineCover
        )

        assertTrue(spec.coverUsesSharedBounds)
        assertFalse(spec.fillCoverViewport)
        assertTrue(spec.showManualStartPlayButton)
        assertTrue(spec.enableManualStartCoverOverlay)
        assertEquals(VideoPlayerCoverContentScaleMode.Crop, spec.coverContentScaleMode)
    }

    @Test
    fun verticalManualStartCover_usesViewportFillAndFitContentScale() {
        val spec = resolveVideoPlayerEntryPresentationSpec(
            shouldKeepCoverForManualStart = true,
            forceCoverDuringReturnAnimation = false,
            isVerticalVideo = true,
            targetMode = VideoSharedTransitionTargetMode.PortraitFullscreen
        )

        assertFalse(spec.coverUsesSharedBounds)
        assertTrue(spec.fillCoverViewport)
        assertTrue(spec.showManualStartPlayButton)
        assertEquals(VideoPlayerCoverContentScaleMode.Fit, spec.coverContentScaleMode)
    }

    @Test
    fun autoPlaybackCover_doesNotStealSharedBoundsFromPlayerContainer() {
        val spec = resolveVideoPlayerEntryPresentationSpec(
            shouldKeepCoverForManualStart = false,
            forceCoverDuringReturnAnimation = false,
            isVerticalVideo = false,
            targetMode = VideoSharedTransitionTargetMode.InlinePlayer
        )

        assertFalse(spec.coverUsesSharedBounds)
        assertFalse(spec.fillCoverViewport)
        assertFalse(spec.showManualStartPlayButton)
        assertFalse(spec.enableManualStartCoverOverlay)
    }

    @Test
    fun forcedReturnCoverSharedBounds_keepsHomeCoverKeyMatchedDuringReturn() {
        // 返回阶段播放器容器会让出 sharedBounds，强制封面必须承接同一个 cover key。
        assertTrue(
            shouldEnableForcedReturnCoverSharedBounds(
                forceCoverDuringReturnAnimation = true,
                transitionEnabled = true,
                hasSharedTransitionScope = true,
                hasAnimatedVisibilityScope = true,
                sourceRoute = com.android.purebilibili.navigation.ScreenRoutes.Home.route
            )
        )
        assertTrue(
            shouldEnableForcedReturnCoverSharedBounds(
                forceCoverDuringReturnAnimation = true,
                transitionEnabled = true,
                hasSharedTransitionScope = true,
                hasAnimatedVisibilityScope = true,
                sourceRoute = "${com.android.purebilibili.navigation.ScreenRoutes.Home.route}?from=tab"
            )
        )
    }

    @Test
    fun forcedReturnCoverSharedBounds_stillActiveForNonHomeCardReturnTargets() {
        listOf("dynamic", "search", "history", "favorite", "watch_later", "partition").forEach { route ->
            assertTrue(
                shouldEnableForcedReturnCoverSharedBounds(
                    forceCoverDuringReturnAnimation = true,
                    transitionEnabled = true,
                    hasSharedTransitionScope = true,
                    hasAnimatedVisibilityScope = true,
                    sourceRoute = route
                ),
                "expected forced cover sharedBounds to remain enabled for sourceRoute=$route"
            )
        }
        assertTrue(shouldUseReturnLandingMotionForForcedReturnCover(true))
        assertFalse(shouldUseReturnLandingMotionForForcedReturnCover(false))
    }

    @Test
    fun coverFirstOverlaySharedBounds_usesSameCardRouteGuardAsReturn() {
        assertTrue(
            shouldEnableCoverOverlaySharedBounds(
                useCoverOverlaySharedBounds = true,
                transitionEnabled = true,
                hasSharedTransitionScope = true,
                hasAnimatedVisibilityScope = true,
                sourceRoute = "partition"
            )
        )
        assertFalse(
            shouldEnableCoverOverlaySharedBounds(
                useCoverOverlaySharedBounds = true,
                transitionEnabled = true,
                hasSharedTransitionScope = true,
                hasAnimatedVisibilityScope = true,
                sourceRoute = "settings"
            )
        )
    }

    @Test
    fun forcedReturnCoverSourceRoute_keepsEveryVideoCardReturnTargetRoute() {
        listOf(
            "home",
            "dynamic",
            "search",
            "history",
            "favorite",
            "watch_later",
            "partition",
            "dynamic_detail/123",
            "category/1",
            "season_series_detail/series/1/2/title/owner",
            "space/123"
        ).forEach { route ->
            assertTrue(resolveForcedReturnCoverSharedElementSourceRoute(route) == route)
            assertTrue(resolveForcedReturnCoverSharedElementSourceRoute("$route?from=tab") == route)
        }
        assertTrue(resolveForcedReturnCoverSharedElementSourceRoute("settings") == null)
    }

    @Test
    fun detailReturnCoverCrossfade_showsCoverForCoverFirst_butDoesNotFadeAbsentPlayer() {
        assertTrue(
            com.android.purebilibili.core.ui.transition.shouldUseDetailReturnCoverCrossfade(
                isLeaving = true,
                playbackIntent = com.android.purebilibili.core.ui.transition.VideoSharedTransitionPlaybackIntent.CoverFirst
            )
        )
        assertFalse(
            com.android.purebilibili.core.ui.transition.shouldFadePlayerSurfaceOnDetailReturn(
                isLeaving = true,
                playbackIntent = com.android.purebilibili.core.ui.transition.VideoSharedTransitionPlaybackIntent.CoverFirst
            )
        )
    }

    @Test
    fun detailReturnCoverUsesSingleAlphaTimelineWithoutCoilCrossfade() {
        val source = File("src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreenStateHolder.kt")
            .readText()
        val residentCoverBlock = source
            .substringAfter("val residentCoverImageRequest =")
            .substringBefore("PortraitInlineVideoPlayerHost(")

        assertTrue(residentCoverBlock.contains(".crossfade(false)"))
        assertTrue(residentCoverBlock.contains("resolveVideoDetailReturnMediaFrame("))
        assertTrue(residentCoverBlock.contains(").coverAlpha"))
        assertTrue(residentCoverBlock.contains(".zIndex(1f)"))
        // 返回封面透明度由合成视觉进度驱动：animatedVisibility 进度 + morph 深度进度
        assertTrue(residentCoverBlock.contains("resolveVideoDetailReturnVisualProgress("))
        assertTrue(residentCoverBlock.contains("animatedVisibilityProgress ="))
        assertTrue(residentCoverBlock.contains("detailTransitionProgress.value"))
        assertFalse(residentCoverBlock.contains("coverCrossfadeAlpha"))
    }

    @Test
    fun verticalManualStart_usesBlackPlayerWithoutLoadingCover() {
        assertFalse(
            shouldLoadVideoPlayerCoverImage(
                isVerticalVideo = true,
                shouldKeepCoverForManualStart = true,
                forceCoverDuringReturnAnimation = false,
            )
        )
        assertTrue(
            shouldLoadVideoPlayerCoverImage(
                isVerticalVideo = true,
                shouldKeepCoverForManualStart = true,
                forceCoverDuringReturnAnimation = true,
            )
        )
        assertFalse(
            shouldLoadVideoPlayerCoverImage(
                isVerticalVideo = true,
                shouldKeepCoverForManualStart = false,
                forceCoverDuringReturnAnimation = false,
            )
        )
        assertTrue(
            shouldLoadVideoPlayerCoverImage(
                isVerticalVideo = false,
                shouldKeepCoverForManualStart = true,
                forceCoverDuringReturnAnimation = false,
            )
        )
    }

    @Test
    fun manualStartPlaybackButton_usesThemeNativeTvIcon() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/video/ui/section/VideoPlayerSection.kt"),
            File("src/main/java/com/android/purebilibili/feature/video/ui/section/VideoPlayerSection.kt"),
        ).first(File::exists).readText()
        val buttonSource = source
            .substringAfter("if (entryPresentationSpec.showManualStartPlayButton)")
            .substringBefore("// 2. DanmakuView")

        assertTrue(buttonSource.contains("AppIconButton("))
        assertFalse(buttonSource.contains("AppFilledIconButton("))
        assertTrue(buttonSource.contains("imageVector = manualStartPlayIcon"))
        assertTrue(source.contains("val manualStartPlayIcon = resolveAppTvIcon()"))
        assertTrue(buttonSource.contains("tint = MaterialTheme.colorScheme.onPrimaryContainer"))
        assertTrue(buttonSource.contains("Modifier.size(32.dp)"))
        assertFalse(buttonSource.contains("Color(0xFF4D5160)"))
        assertFalse(buttonSource.contains("Color.White.copy(alpha = 0.96f)"))
    }
}
