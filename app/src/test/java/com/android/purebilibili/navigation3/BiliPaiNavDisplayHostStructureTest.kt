package com.android.purebilibili.navigation3

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BiliPaiNavDisplayHostStructureTest {
    @Test
    fun miuixHeroHasOneOwnerForGeometryEffectsAndLifetime() {
        val source = navDisplayHostSource()
        assertTrue(source.contains("resolveVideoHeroMotionSpec("))
        assertTrue(source.contains("heroMotionSpec = heroMotion"))
        assertTrue(source.contains("bindNavigationDriver("))
        assertTrue(source.contains("remember(sourceMetadata.sourceKey) { MiuixVideoCardTransitionProgress() }"))
        assertTrue(source.contains("followNavigationDriver("))
        assertTrue(source.contains("snapshotFlow { videoCardTransitionProgress.settleStateOrNull() }"))
        assertFalse(source.contains("animateFallbackTo("))
        assertTrue(source.contains("LocalMiuixVideoCardTransitionState provides miuixCardTransitionState"))
        assertTrue(source.contains("ProvideMiuixNavViewModelApplicationExtras(application)"))
    }

    @Test
    fun cardDisabledVideoUsesSelectedGlobalNavTransition() {
        val source = navDisplayHostSource()
        val fallbackBlock = source
            .substringAfter("val videoFallbackTransition = if (cardTransitionEnabled)")
            .substringBefore("val observedVideoFallbackTransition")

        assertTrue(fallbackBlock.contains("predictiveBackExcludedTransition"))
        assertTrue(fallbackBlock.contains("globalTransition"))
        assertTrue(source.contains("videoCardTransitionProgress.observe(videoFallbackTransition)"))
    }

    @Test
    fun navDisplayHostOwnsNavigation3RenderingAndSharedTransitionScope() {
        val source = navDisplayHostSource()

        assertTrue(source.contains("NavDisplay("))
        assertTrue(source.contains("entryProvider"))
        assertTrue(source.contains("LocalNavAnimatedContentScope.current"))
        assertTrue(source.contains("ProvideAnimatedVisibilityScope("))
        assertTrue(source.contains("LocalVideoCardSharedElementSourceRoute provides entryRoute"))
        assertTrue(source.contains("sharedTransitionScope = sharedTransitionScope"))
        assertFalse(source.contains("VideoSharedTransitionBackdropHost("))
        assertFalse(source.contains("videoCardTransitionController"))
        assertFalse(source.contains("LocalVideoCardTransitionSession"))
        assertTrue(source.contains("predictivePopTransitionSpec"))
        assertFalse(source.contains("NavDisplayTransitionEffects"))
        assertFalse(source.contains("transitionEffects ="))
    }

    @Test
    fun navigation3RuntimeAndUiUseTheSameOfficialAlpha07Version() {
        val buildFile = buildFileSource()

        assertTrue(buildFile.contains("val navigation3Version = \"1.2.0-alpha07\""))
        assertTrue(buildFile.contains("androidx.navigation3:navigation3-runtime:\$navigation3Version"))
        assertTrue(buildFile.contains("androidx.navigation3:navigation3-ui:\$navigation3Version"))
        assertFalse(buildFile.contains("miuix-navigation3-ui-android"))
    }

    @Test
    fun navDisplayHostScopesEntryStateWithLifecycleNavigation3Decorator() {
        val source = navDisplayHostSource()
        val buildFile = buildFileSource()

        assertTrue(buildFile.contains("androidx.lifecycle:lifecycle-viewmodel-navigation3:"))
        // 上游 navigationevent-compose 被 Gradle exclude 掉，转而使用项目内 vendored 源码
        // (app/src/main/java/androidx/navigationevent/compose/)，以便在 onBackCompleted 回调
        // 内对 transitionState 提交时序做精确控制。
        assertTrue(buildFile.contains("exclude(group = \"androidx.navigationevent\", module = \"navigationevent-compose\")"))
        assertFalse(buildFile.contains("androidx.navigationevent:navigationevent-compose:"))
        assertFalse(buildFile.contains("androidx.navigationevent:navigationevent-compose"))
        assertTrue(source.contains("rememberDecoratedNavEntries("))
        assertTrue(source.contains("rememberSceneState("))
        assertTrue(source.contains("rememberSaveableStateHolderNavEntryDecorator"))
        assertTrue(source.contains("rememberViewModelStoreNavEntryDecorator"))
    }

    @Test
    fun navDisplayHostUsesKeyEqualScenesSoPredictiveReleaseContinuesMorph() {
        val source = navDisplayHostSource()
        // 预测松手时 Scene equals 必须按 destination key，避免 entry 重建后
        // animateTo 把 fraction 置 0，卡片从全屏再缩一次。
        assertTrue(source.contains("BiliPaiKeyEqualSceneStrategy()"))
        assertFalse(source.contains("SinglePaneSceneStrategy()"))
        // scopedContent 不得把 safeBackStack 放进 remember key（pop 会换 content lambda）。
        val scopedRemember = source
            .substringAfter("val scopedContent: @Composable (BiliPaiNavKey) -> Unit = remember(")
            .substringBefore(") {")
        assertFalse(scopedRemember.contains("safeBackStack"))
        assertTrue(source.contains("latestSafeBackStack"))
        assertTrue(source.contains("rememberUpdatedState(safeBackStack)"))
    }

    @Test
    fun navDisplayHostHoistsNavigationEventStateIntoNavDisplay() {
        val source = navDisplayHostSource()

        assertTrue(source.contains("rememberNavigationEventState("))
        assertTrue(source.contains("rememberNavigationEventState(sceneState)"))
        assertTrue(source.contains("NavigationBackHandler("))
        assertTrue(source.contains("onBackCompleted = performBack"))
        assertTrue(source.contains("onBackCancelled"))
        assertTrue(source.contains("navigationEventState = navigationEventState"))
        assertTrue(source.contains("sceneState = sceneState"))
        kotlin.test.assertFalse(source.contains("NavDisplay(\n        backStack = safeBackStack"))
    }

    @Test
    fun navDisplayHostSuppressesPredictiveProgressWhenPreferenceDisabled() {
        val source = navDisplayHostSource()
        val backHandlerBlock = source
            .substringAfter("NavigationBackHandler(")
            .substringBefore("onBackCancelled")

        assertTrue(backHandlerBlock.contains("reportPredictiveProgress = predictiveBackEnabled"))
        assertFalse(backHandlerBlock.contains("usesPredictivePreview"))
        assertTrue(source.contains("predictiveBackEnabled = predictiveBackEnabled"))
    }

    @Test
    fun navDisplayHostAlignsDepthReturnDurationWithSharedMorphRemaining() {
        val source = navDisplayHostSource()
        assertTrue(source.contains("resolveVideoCardSharedMorphRemainingDurationMs("))
        assertTrue(source.contains("gestureFractionAtCommit"))
        assertTrue(source.contains("morphRemainingMs"))
        assertTrue(source.contains("resolveMorphAlignedFallbackDurationMs"))
        assertTrue(source.contains("VideoCardTransitionClock"))
        assertTrue(source.contains("LocalVideoCardMorphProgressReporter"))
    }

    @Test
    fun navDisplayHostPreservesApplicationExtrasForEntryViewModels() {
        val source = navDisplayHostSource()

        assertTrue(source.contains("ProvideNavigation3ViewModelApplicationExtras("))
        assertTrue(source.contains("LocalViewModelStoreOwner provides patchedOwner"))
        assertTrue(source.contains("APPLICATION_KEY"))
    }

    @Test
    fun navDisplayHostDoesNotRegisterClassicBackInterceptor() {
        val source = navDisplayHostSource()

        assertTrue(source.contains("NavDisplay("))
        assertTrue(source.contains("onBack = { performBack { } }"))
        assertTrue(source.contains("onBack()"))
        assertFalse(source.contains("import androidx.activity.compose.BackHandler"))
        assertFalse(source.contains("BackHandler(enabled"))
    }

    @Test
    fun navDisplayHostSynchronizesVideoCardBlurForNestedDetailTransitions() {
        val source = navDisplayHostSource()
        val openingBranch = source
            .substringAfter("openedVideoDetail -> {")
            .substringBefore("returnedFromVideoDetail -> {")
        val returnBranch = source
            .substringAfter("returnedFromVideoDetail -> {")
            .substringBefore("!isCardMorphDestinationNavKey(currentTop)")

        // 单时钟：open/return 走 clock.begin* + fallback；返回不因 shared 跳过消糊。
        assertTrue(openingBranch.contains("beginOpeningIfNeeded("))
        assertTrue(openingBranch.contains("animateFallbackTo("))
        assertTrue(openingBranch.contains("markHeld()"))
        assertTrue(openingBranch.contains("hasActiveSharedMorphProgress()"))
        assertTrue(returnBranch.contains("beginReturning("))
        assertTrue(returnBranch.contains("startDepth = startDepth"))
        assertTrue(returnBranch.contains("resolveMorphAlignedFallbackDurationMs"))
        assertTrue(returnBranch.contains("timelineSpec.returnEasing"))
        assertTrue(returnBranch.contains("parentSourceRoute"))
        assertTrue(returnBranch.contains("snapFallback(startDepth)"))
        assertTrue(returnBranch.contains("animateFallbackTo("))
        // 返回路径不得再「有 shared 就跳过 fallback」（会丢掉模糊→清晰）。
        assertFalse(
            returnBranch.contains(
                "videoCardClock.phase != VideoCardTransitionBackgroundPhase.RETURNING ||\n" +
                    "                                videoCardClock.hasActiveSharedMorphProgress()",
            ),
        )
        assertTrue(source.contains("safeBackStack.size > previousStack.size"))
        assertTrue(source.contains("safeBackStack.size < previousStack.size"))
        assertTrue(source.contains("isCardMorphDestinationNavKey("))
        assertTrue(source.contains("reportSharedMorphProgress"))
    }

    @Test
    fun navDisplayHostReadsLivePredictiveProgressForVideoCardDepth() {
        val source = navDisplayHostSource()
        val progressProviderBlock = source
            .substringAfter("val videoCardBackgroundProgressProvider = remember(")
            .substringBefore("val videoCardTransitionJankState")

        assertTrue(progressProviderBlock.contains("resolveVideoCardPredictiveGestureDepthProgress("))
        assertTrue(progressProviderBlock.contains("NavigationEventTransitionState.InProgress"))
        assertTrue(progressProviderBlock.contains("isVideoCardTransitionBackgroundGesturePhase"))
        assertTrue(progressProviderBlock.contains("latestEvent"))
        assertTrue(progressProviderBlock.contains("videoCardClock.depthProgress()"))
    }

    @Test
    fun navDisplayHostIntegratesPredictiveBackGestureBlurPipeline() {
        val source = navDisplayHostSource()

        assertTrue(source.contains("predictiveBackBackgroundProgress"))
        assertTrue(source.contains("resolvePredictiveBackGestureBlurProgress"))
        assertTrue(source.contains("shouldApplyPredictiveBackGestureBlur"))
        assertTrue(source.contains("LocalPredictiveBackBackgroundState provides"))
        assertTrue(source.contains("predictiveBackBackgroundProgressProvider"))
        assertFalse(source.contains("LaunchedEffect(gesturePredictiveBlurTarget)"))
        assertTrue(source.contains("isLightBackgroundProvider ="))
        assertTrue(source.contains("isLightBackground: Boolean"))
    }

    @Test
    fun navDisplayHostRunsSameCompletedBackPathForClassicAndPredictiveReturn() {
        val source = navDisplayHostSource()
        val performBackBlock = source
            .substringAfter("val performBack: (() -> Unit) -> Unit = {")
            .substringBefore("val scopedContent:")

        assertTrue(source.contains("onBack = { performBack { } }"))
        assertTrue(source.contains("onBackCompleted = performBack"))
        assertTrue(performBackBlock.contains("videoCardClock.depthProgress()"))
        assertTrue(performBackBlock.contains("videoBlurFadeJob"))
        assertTrue(performBackBlock.contains("predictiveBackHandler.onBackPressed("))
        assertTrue(performBackBlock.contains("commitTransitionCallBack()"))
        assertTrue(performBackBlock.contains("onBack()"))
    }

    @Test
    fun navDisplayHostInterruptsOpeningWithoutSwitchingTheReturnTimeline() {
        val source = navDisplayHostSource()
        val performBackBlock = source
            .substringAfter("val performBack: (() -> Unit) -> Unit = {")
            .substringBefore("val scopedContent:")

        assertTrue(source.contains("launchVideoCardDepthAnimation"))
        assertTrue(source.contains("cancelVideoCardDepthAnimation"))
        assertTrue(source.contains("resolveVideoCardTransitionReturnFullDurationMillis"))
        assertTrue(performBackBlock.contains("cancelVideoCardDepthAnimation()"))
        assertTrue(source.contains("VideoCardTransitionClock"))
        assertTrue(source.contains("beginOpeningIfNeeded("))
    }

    @Test
    fun navDisplayHostUsesRootClockWithoutPerFrameSnapshotBridge() {
        val source = navDisplayHostSource()
        assertTrue(source.contains("videoCardClock: VideoCardTransitionClock"))
        assertTrue(source.contains("videoCardClock.depthProgress()"))
        assertFalse(source.contains("onVideoCardDepthFrame"))
        assertFalse(source.contains("snapshotFlow"))
    }

    @Test
    fun programmaticBackSharesPerformBackPathAndRejectsReentry() {
        val source = navDisplayHostSource()
        val performBackBlock = source
            .substringAfter("val performBack: (() -> Unit) -> Unit = {")
            .substringBefore("val quickReturnFromDetailProvider")

        assertTrue(performBackBlock.contains("navigationBackJob?.isActive != true"))
        assertTrue(performBackBlock.contains("programmaticBackDispatcher.register(callback)"))
        assertTrue(performBackBlock.contains("performBack { }"))
        assertTrue(source.contains("onBackCompleted = performBack"))
        assertTrue(source.contains("onBack = { performBack { } }"))
    }

    @Test
    fun navDisplayHostFadesVideoCardBackgroundBlurAlongsidePop() {
        val source = navDisplayHostSource()
        val performBackBlock = source
            .substringAfter("val performBack: (() -> Unit) -> Unit = {")
            .substringBefore("val scopedContent:")

        val preOnBack = performBackBlock.substringBefore("onBack()")
        assertTrue(preOnBack.contains("isVideoCardActiveReturn"))
        assertTrue(preOnBack.contains("VideoCardTransitionBackgroundPhase.HELD"))
        assertTrue(preOnBack.contains("VideoCardTransitionBackgroundPhase.OPENING"))
        assertTrue(preOnBack.contains("beginReturning("))
        assertTrue(preOnBack.contains("resolveMorphAlignedFallbackDurationMs"))
        assertTrue(preOnBack.contains("resolveVideoCardReturnClearStartDepth("))
        assertTrue(preOnBack.contains("shouldSnapClearVideoCardDepthBlurOnQuickReturn("))
        // OPENING 打断仍可 snap；HELD/RETURNING 快速返回走 beginReturning + 连续消糊。
        assertTrue(preOnBack.contains("snapClearAndIdle()"))
    }

    @Test
    fun navDisplayHostSupportsOpeningPhaseVideoCardGestureBlur() {
        val source = navDisplayHostSource()

        assertTrue(source.contains("isVideoCardTransitionBackgroundGesturePhase"))
        assertTrue(source.contains("beginGesture("))
        val gestureBlock = source
            .substringAfter("val gestureReturningVideoCard =")
            .substringBefore("val predictiveBackGestureBlurEnabled")
        assertTrue(gestureBlock.contains("isVideoCardTransitionBackgroundGesturePhase"))
    }

    @Test
    fun navDisplayHostReadsVideoGestureProgressWithoutPerFrameAnimatableEffects() {
        val source = navDisplayHostSource()

        assertTrue(source.contains("videoCardBackgroundProgressProvider"))
        assertTrue(source.contains("progressProvider = videoCardBackgroundProgressProvider"))
        assertFalse(source.contains("LaunchedEffect(gestureBackgroundBlurTarget)"))
    }

    @Test
    fun navDisplayHostSuppressesOpeningBackgroundScaleDuringGestureRestore() {
        val source = navDisplayHostSource()
        assertTrue(source.contains("videoCardClock.beginGestureRestore()"))
        assertTrue(source.contains("videoCardClock.endGestureRestore()"))
        assertTrue(source.contains("isGestureRestoreInProgressProvider"))
    }

    @Test
    fun navDisplayHostPivotsBackgroundScaleAroundTheClickedCard() {
        val source = navDisplayHostSource()
        assertTrue(source.contains("sourceBoundsProvider = { sourceMetadata.sourceBounds }"))
        val hostDepth = source
            .substringAfter("VideoCardTransitionHostDepthLayer(")
            .substringBefore("VideoCardTransitionNavBackdrop(")
        assertTrue(hostDepth.contains("sourceBoundsProvider = { sourceMetadata.sourceBounds }"))
        val backgroundState = source
            .substringAfter("VideoCardTransitionBackgroundState(")
            .substringBefore("val videoCardLayoutWidthProvider")
        assertTrue(backgroundState.contains("sourceBoundsProvider = { sourceMetadata.sourceBounds }"))
    }

    @Test
    fun navDisplayHostTracksOnlyActiveVideoCardTransitionPhases() {
        val source = navDisplayHostSource()
        val trackingBlock = source
            .substringAfter("val videoCardTransitionJankState =")
            .substringBefore("TrackJankStateValue(")

        assertFalse(source.contains("AppRuntimeVisualGuardTracker.decision.collectAsStateWithLifecycle()"))
        assertTrue(source.contains("stateName = VIDEO_CARD_TRANSITION_JANK_STATE"))
        assertTrue(trackingBlock.contains("PredictiveReturn"))
        assertTrue(trackingBlock.contains("GestureRestore"))
        assertTrue(trackingBlock.contains("VideoCardTransitionBackgroundPhase.OPENING"))
        assertTrue(trackingBlock.contains("VideoCardTransitionBackgroundPhase.RETURNING"))
        assertFalse(trackingBlock.contains("VideoCardTransitionBackgroundPhase.HELD"))
        assertTrue(source.contains("resolveVideoCardTransitionMotionTier(reduceMotion)"))
        assertFalse(source.contains("runtimeGuardDecision.effectiveMotionTier"))
        assertTrue(source.contains("stateValue = videoCardTransitionJankState"))
    }

    @Test
    fun navDisplayHostIntegratesPredictiveBackHandlerDecorator() {
        val source = navDisplayHostSource()

        assertTrue(source.contains("resolveBiliPaiPredictiveBackAnimationHandler"))
        assertTrue(source.contains("predictiveBackAnimationDecorator"))
        assertTrue(source.contains("NavEntryDecorator("))
        assertTrue(source.contains("onPredictivePopTransitionSpec"))
        assertFalse(source.contains("LocalVideo" + "PredictiveReturnState"))
        assertFalse(source.contains("onPredictiveBackGestureChange"))
    }

    @Test
    fun navDisplayHostRoutesPredictivePopThroughHandlerPolicy() {
        val source = navDisplayHostSource()

        assertTrue(source.contains("val popRouteTransition = remember("))
        assertTrue(source.contains("resolveBiliPaiPredictiveBackAnimationHandler"))
        assertFalse(source.contains("BiliPaiVideoDetailTargetPredictiveBackAnimation"))
        assertFalse(source.contains("resolveBiliPaiNavPopContentTransform(popRouteTransition)"))
    }

    @Test
    fun navDisplayHostDoesNotCoverGlobalHomeWallpaper() {
        val source = navDisplayHostSource()

        assertTrue(source.contains("LocalGlobalWallpaperBackdropVisible.current"))
        assertTrue(source.contains("if (globalWallpaperVisible)"))
        assertTrue(source.contains("Color.Transparent"))
        assertTrue(source.contains("AppSurfaceTokens.groupedListContainer()"))
    }

    @Test
    fun navDisplayHostLayersVideoCardTransitionNavBackdropBehindNavDisplay() {
        val source = navDisplayHostSource()

        assertTrue(source.contains("VideoCardTransitionNavBackdrop("))
        assertTrue(source.contains("shouldShowVideoCardTransitionNavBackdrop"))
        // Root host Box is multi-line: modifier.fillMaxSize() + background token.
        assertTrue(source.contains("modifier = modifier"))
        assertTrue(source.contains(".fillMaxSize()"))
        val boxBlock = source
            .substringAfter("VideoCardTransitionHostDepthLayer(")
            .substringBefore("@Composable\nprivate fun ProvideNavigation3ViewModelApplicationExtras")
        assertTrue(boxBlock.indexOf("VideoCardTransitionNavBackdrop") < boxBlock.indexOf("NavDisplay("))
    }

    private fun navDisplayHostSource(): String {
        return listOf(
            File("app/src/main/java/com/android/purebilibili/navigation3/BiliPaiNavDisplayHost.kt"),
            File("src/main/java/com/android/purebilibili/navigation3/BiliPaiNavDisplayHost.kt")
        ).first { it.exists() }.readText()
    }

    private fun buildFileSource(): String {
        return listOf(
            File("app/build.gradle.kts"),
            File("build.gradle.kts")
        ).first { it.exists() }.readText()
    }
}
