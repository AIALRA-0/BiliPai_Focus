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
        assertTrue(source.contains("biliPaiNavEntries("))
        assertTrue(source.contains("LocalOfficialVideoSharedTransition.current"))
        assertTrue(source.contains("realSharedTransition.AnimatedVisibility("))
        assertTrue(source.contains("LocalAnimatedVisibilityScope provides this"))
        assertTrue(source.contains("LocalVideoCardSharedElementSourceRoute provides key.toLegacyRoute()"))
        assertTrue(source.contains("LocalOfficialVideoSharedTransition"))
        assertFalse(source.contains("VideoSharedTransitionBackdropHost("))
        assertFalse(source.contains("videoCardTransitionController"))
        assertFalse(source.contains("LocalVideoCardTransitionSession"))
        assertFalse(source.contains("NavDisplayTransitionEffects"))
        assertFalse(source.contains("transitionEffects ="))
    }

    @Test
    fun navigationHostUsesMiuixNavDependencyFromSharedVersionCatalog() {
        val buildFile = buildFileSource()
        val versionCatalog = versionCatalogSource()
        val settings = settingsSource()

        assertTrue(buildFile.contains("implementation(libs.miuix.navigation)"))
        assertTrue(versionCatalog.contains("miuix-navigation = { group = \"top.yukonga.miuix.kmp\", name = \"miuix-nav-android\", version.ref = \"miuix\" }"))
        assertTrue(settings.contains("substitute(module(\"top.yukonga.miuix.kmp:miuix-nav-android\"))"))
        assertTrue(settings.contains("using(project(\":miuix-nav\"))"))
        assertFalse(buildFile.contains("androidx.navigation3:navigation3-ui:"))
    }

    @Test
    fun navDisplayHostScopesMiuixEntryBackStateAndApplicationExtras() {
        val source = navDisplayHostSource()
        val buildFile = buildFileSource()

        assertTrue(buildFile.contains("implementation(libs.miuix.navigation)"))
        assertTrue(source.contains("biliPaiNavEntries("))
        assertTrue(source.contains("rememberNavigationEventState(NavigationEventInfo.None)"))
        assertTrue(source.contains("NavigationBackHandler("))
        assertTrue(source.contains("isBackEnabled = interceptPredictiveBack"))
        assertTrue(source.contains("ProvideMiuixNavViewModelApplicationExtras(application)"))
        assertTrue(source.contains("LocalViewModelStoreOwner provides patchedOwner"))
    }

    @Test
    fun navDisplayHostUsesOneMiuixDriverAcrossMorphAndPredictiveRelease() {
        val source = navDisplayHostSource()
        assertTrue(source.contains("remember(sourceMetadata.sourceKey) { MiuixVideoCardTransitionProgress() }"))
        assertTrue(source.contains("videoCardTransitionProgress.observe(videoFallbackTransition)"))
        assertTrue(source.contains("progress = videoCardTransitionProgress"))
        assertTrue(source.contains("videoCardClock.bindNavigationDriver("))
        assertTrue(source.contains("videoCardTransitionProgress.clear()"))
        assertTrue(source.contains("state == VideoCardTransitionSettleState.Idle"))
    }

    @Test
    fun navDisplayHostScopesNavigationEventStateToEachMiuixEntry() {
        val source = navDisplayHostSource()

        val entryBlock = source
            .substringAfter("private fun BiliPaiMiuixNavEntry(")
            .substringBefore("@Composable\nprivate fun ProvideMiuixNavViewModelApplicationExtras")
        val displayCall = source
            .substringAfter("NavDisplay(")
            .substringBefore("biliPaiNavEntries(")

        assertTrue(source.contains("BiliPaiMiuixNavEntry("))
        assertTrue(entryBlock.contains("rememberNavigationEventState(NavigationEventInfo.None)"))
        assertTrue(source.contains("NavigationBackHandler("))
        assertTrue(entryBlock.contains("onBackCompleted = onBack"))
        assertTrue(displayCall.contains("backStack = backStack as NavBackStack"))
        assertTrue(displayCall.contains("onBack = performBack"))
        assertFalse(displayCall.contains("navigationEventState"))
    }

    @Test
    fun navDisplayHostSuppressesPredictiveProgressWhenPreferenceDisabled() {
        val source = navDisplayHostSource()
        val fallbackBlock = source
            .substringAfter("val predictiveBackExcludedTransition = remember(")
            .substringBefore("// A restored parent session")
        val transitionPolicy = predictiveBackTransitionSource()

        assertTrue(fallbackBlock.contains("miuixPredictiveBackProgressEnabled = false"))
        assertTrue(transitionPolicy.contains("enabled && animation == BiliPaiPredictiveBackAnimationStyle.MIUIX"))
        assertTrue(source.contains("interceptPredictiveBack = interceptPredictiveBack"))
        assertTrue(source.contains("style == BiliPaiPredictiveBackAnimationStyle.NONE && backStack.size > 1"))
    }

    @Test
    fun navDisplayHostAlignsDepthReturnDurationWithSharedMorphRemaining() {
        val source = navDisplayHostSource()
        val transitionBlock = source
            .substringAfter("val videoCardTransition = remember(")
            .substringBefore("val fullscreenVideoCardTransition")

        assertTrue(source.contains("videoCardClock.bindNavigationDriver("))
        assertTrue(source.contains("videoCardTransitionProgress.depthOrNull()"))
        assertTrue(transitionBlock.contains("progress = videoCardTransitionProgress"))
        assertTrue(source.contains("progressProvider = videoCardProgressProvider"))
        assertFalse(source.contains("resolveMorphAlignedFallbackDurationMs"))
    }

    @Test
    fun navDisplayHostPreservesApplicationExtrasForEntryViewModels() {
        val source = navDisplayHostSource()

        assertTrue(source.contains("ProvideMiuixNavViewModelApplicationExtras("))
        assertTrue(source.contains("LocalViewModelStoreOwner provides patchedOwner"))
        assertTrue(source.contains("APPLICATION_KEY"))
        assertTrue(source.contains("viewModelStore = navEntryOwner.viewModelStore"))
        assertTrue(source.contains("defaultFactoryOwner?.defaultViewModelProviderFactory"))
    }

    @Test
    fun navDisplayHostDoesNotRegisterClassicBackInterceptor() {
        val source = navDisplayHostSource()

        assertTrue(source.contains("NavDisplay("))
        assertTrue(source.contains("onBack = performBack"))
        assertTrue(source.contains("onBackCompleted = onBack"))
        assertTrue(source.contains("latestOnBack()"))
        assertFalse(source.contains("import androidx.activity.compose.BackHandler"))
        assertFalse(source.contains("BackHandler(enabled"))
    }

    @Test
    fun navDisplayHostSynchronizesVideoCardBlurForNestedDetailTransitions() {
        val source = navDisplayHostSource()
        val stackTransitionBlock = source
            .substringAfter("LaunchedEffect(stackSnapshot, cardMorphAvailable)")
            .substringBefore("LaunchedEffect(cardMorphAvailable, videoCardTransitionProgress")

        // The same Miuix depth drives the detail morph and background exposure through both push
        // and pop. The clock tracks coarse Miuix settle states for background lifetime only.
        assertTrue(stackTransitionBlock.contains("openedCardDestination"))
        assertTrue(stackTransitionBlock.contains("beginOpeningIfNeeded(sourceMetadata.sourceRoute)"))
        assertTrue(stackTransitionBlock.contains("returnedFromCardDestination"))
        assertTrue(stackTransitionBlock.contains("beginReturning(sourceMetadata.sourceRoute"))
        assertTrue(source.contains("progress = videoCardTransitionProgress"))
        assertTrue(source.contains("progressProvider = videoCardProgressProvider"))
        assertTrue(source.contains("exposureProvider = videoCardExposureProvider"))
        assertTrue(source.contains("sourceRouteProvider = { sourceMetadata.sourceRoute }"))
        assertTrue(source.contains("resolveVideoCardTransitionExposure("))
        assertTrue(source.contains("isCardMorphDestinationNavKey("))
        assertTrue(source.contains("LocalVideoCardSharedElementSourceRoute provides key.toLegacyRoute()"))
    }

    @Test
    fun navDisplayHostReadsLivePredictiveProgressForVideoCardDepth() {
        val source = navDisplayHostSource()
        val progressProviderBlock = source
            .substringAfter("val videoCardProgressProvider = remember(")
            .substringBefore("val videoCardGestureProvider")
        val progressSource = miuixVideoTransitionSource()

        assertTrue(progressProviderBlock.contains("videoCardTransitionProgress.depthOr(videoCardClock.depthProgress())"))
        assertTrue(progressSource.contains("fun depthOrNull(): Float?"))
        assertTrue(progressSource.contains("resolveMiuixVideoCardDepthProgress(it.relativeDepth)"))
        assertTrue(progressSource.contains("fun isGestureInProgress(): Boolean"))
        assertTrue(progressSource.contains("it.gesture != null && it.settle == null"))
        assertTrue(progressSource.contains("scope.settle?.phase == NavSettlePhase.Commit"))
    }

    @Test
    fun navDisplayHostIntegratesPredictiveBackGestureBlurPipeline() {
        val source = navDisplayHostSource()
        val stateBlock = source
            .substringAfter("val predictiveBackBackgroundState = remember(")
            .substringBefore("val roundAllCorners")
        val appNavigation = appNavigationSource()

        assertTrue(stateBlock.contains("PredictiveBackBackgroundState("))
        assertTrue(stateBlock.contains("videoCardTransitionProgress.gestureBackProgress()"))
        assertTrue(stateBlock.contains("resolvePredictiveBackGestureBlurProgress(it)"))
        assertTrue(stateBlock.contains("effectiveRealtimeBlurEnabled"))
        assertTrue(stateBlock.contains("isCardMorphDestinationNavKey(currentKey)"))
        assertTrue(stateBlock.contains("targetKeyProvider = { currentBackTarget }"))
        assertTrue(source.contains("LocalPredictiveBackBackgroundState provides"))
        assertTrue(appNavigation.contains("LocalPredictiveBackBackgroundState.current"))
        assertTrue(appNavigation.contains("progressProvider = predictiveBackState.progressProvider"))
        assertFalse(source.contains("LaunchedEffect(gesturePredictiveBlurTarget)"))
    }

    @Test
    fun navDisplayHostRunsSameCompletedBackPathForClassicAndPredictiveReturn() {
        val source = navDisplayHostSource()
        val performBackBlock = source
            .substringAfter("val performBack = remember(")
            .substringBefore("DisposableEffect(programmaticBackDispatcher, performBack)")
        val entryHandler = source
            .substringAfter("private fun BiliPaiMiuixNavEntry(")
            .substringBefore("@Composable\nprivate fun ProvideMiuixNavViewModelApplicationExtras")

        assertTrue(source.contains("onBack = performBack"))
        assertTrue(entryHandler.contains("onBackCompleted = onBack"))
        assertTrue(performBackBlock.contains("latestPrepareReturn()"))
        assertTrue(performBackBlock.contains("videoCardClock.beginReturning(sourceMetadata.sourceRoute, videoCardClock.depthProgress())"))
        assertTrue(performBackBlock.contains("latestOnBack()"))
        assertTrue(performBackBlock.indexOf("latestPrepareReturn()") < performBackBlock.indexOf("latestOnBack()"))
    }

    @Test
    fun navDisplayHostInterruptsOpeningWithoutSwitchingTheReturnTimeline() {
        val source = navDisplayHostSource()
        val transitionSource = miuixVideoTransitionSource()
        val videoTransitionBlock = source
            .substringAfter("val videoCardTransition = remember(")
            .substringBefore("val fullscreenVideoCardTransition")

        assertTrue(source.contains("videoCardClock.bindNavigationDriver("))
        assertTrue(videoTransitionBlock.contains("miuixVideoCardNavTransition("))
        assertTrue(videoTransitionBlock.contains("progress = videoCardTransitionProgress"))
        assertTrue(transitionSource.contains("programmatic = NavSettleSpec.Tween("))
        assertTrue(transitionSource.contains("commit = NavSettleSpec.Spring("))
        assertTrue(transitionSource.contains("cancel = NavSettleSpec.Spring("))
        assertTrue(source.contains("videoCardClock.followNavigationDriver(state, videoCardTransitionProgress.releaseVelocity())"))
        assertTrue(source.contains("beginOpeningIfNeeded(sourceMetadata.sourceRoute)"))
        assertFalse(source.contains("launchVideoCardDepthAnimation"))
    }

    @Test
    fun navDisplayHostUsesRootClockWithoutPerFrameSnapshotBridge() {
        val source = navDisplayHostSource()
        assertTrue(source.contains("videoCardClock: VideoCardTransitionClock"))
        assertTrue(source.contains("videoCardClock.depthProgress()"))
        assertFalse(source.contains("onVideoCardDepthFrame"))
        assertTrue(source.contains("videoCardClock.bindNavigationDriver("))
        val settleFlow = source
            .substringAfter("LaunchedEffect(cardMorphAvailable, videoCardTransitionProgress, heroMotion, sourceMetadata.sourceKey)")
            .substringBefore("val videoCardSnapshotHandle")
        assertTrue(settleFlow.contains("snapshotFlow { videoCardTransitionProgress.settleStateOrNull() }"))
        assertFalse(settleFlow.contains("snapshotFlow { videoCardTransitionProgress.depthOrNull()"))
    }

    @Test
    fun programmaticBackSharesPerformBackPathAndRejectsReentry() {
        val source = navDisplayHostSource()
        val appNavigation = appNavigationSource()

        assertTrue(source.contains("shouldDispatchProgrammaticBack(lastDispatchUptimeMs, nowUptimeMs, debounceWindowMs)"))
        assertTrue(source.contains("lastDispatchUptimeMs = nowUptimeMs"))
        assertTrue(source.contains("programmaticBackDispatcher.register(performBack)"))
        assertTrue(appNavigation.contains("navigation3ProgrammaticBackDispatcher.dispatch("))
        assertTrue(appNavigation.contains("nowUptimeMs = android.os.SystemClock.uptimeMillis()"))
        assertTrue(appNavigation.contains("programmaticBackDispatcher = navigation3ProgrammaticBackDispatcher"))
        assertTrue(source.contains("onBackCompleted = onBack"))
        assertTrue(source.contains("onBack = performBack"))
    }

    @Test
    fun navDisplayHostFadesVideoCardBackgroundBlurAlongsidePop() {
        val source = navDisplayHostSource()
        val performBackBlock = source
            .substringAfter("val performBack = remember(")
            .substringBefore("DisposableEffect(programmaticBackDispatcher, performBack)")

        assertTrue(performBackBlock.contains("latestPrepareReturn()"))
        assertTrue(performBackBlock.contains("videoCardClock.beginReturning(sourceMetadata.sourceRoute, videoCardClock.depthProgress())"))
        assertTrue(source.contains("VideoCardTransitionBackgroundState("))
        assertTrue(source.contains("progressProvider = videoCardProgressProvider"))
        assertTrue(source.contains("realtimeBlurEnabledProvider = { effectiveRealtimeBlurEnabled }"))
        assertTrue(source.contains("effectiveRealtimeBlurEnabled = videoTransitionRealtimeBlurEnabled ||"))
    }

    @Test
    fun navDisplayHostSupportsOpeningPhaseVideoCardGestureBlur() {
        val source = navDisplayHostSource()
        val transitionSource = miuixVideoTransitionSource()

        assertTrue(source.contains("videoCardTransitionProgress.isGestureInProgress()"))
        assertTrue(source.contains("videoCardTransitionProgress.gestureBackProgress()"))
        assertTrue(transitionSource.contains("val gesture = scope.gesture"))
        assertTrue(transitionSource.contains("gesture != null"))
        assertTrue(transitionSource.contains("resolveMiuixVideoCardGestureTransform("))
        assertTrue(source.contains("VideoCardTransitionBackgroundState("))
        assertTrue(source.contains("progressProvider = videoCardProgressProvider"))
    }

    @Test
    fun navDisplayHostReadsVideoGestureProgressWithoutPerFrameAnimatableEffects() {
        val source = navDisplayHostSource()

        assertTrue(source.contains("val videoCardProgressProvider = remember("))
        assertTrue(source.contains("videoCardTransitionProgress.depthOr(videoCardClock.depthProgress())"))
        assertTrue(source.contains("progressProvider = videoCardProgressProvider"))
        assertTrue(source.contains("videoCardTransitionProgress.gestureBackProgress()"))
        assertFalse(source.contains("LaunchedEffect(gestureBackgroundBlurTarget)"))
        assertFalse(source.contains("animateFallbackTo("))
    }

    @Test
    fun navDisplayHostSuppressesOpeningBackgroundScaleDuringGestureRestore() {
        val source = navDisplayHostSource()
        val clockSource = videoCardClockSource()
        assertTrue(source.contains("VideoCardTransitionSettleState.CancelRestore"))
        assertTrue(source.contains("videoCardClock.followNavigationDriver(state, videoCardTransitionProgress.releaseVelocity())"))
        assertTrue(source.contains("settleState == VideoCardTransitionSettleState.CancelRestore"))
        assertTrue(source.contains("isGestureRestoreInProgressProvider"))
        assertTrue(clockSource.contains("gestureRestoreInProgress = state == VideoCardTransitionSettleState.CancelRestore"))
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
            .substringAfter("val videoCardTransitionJankState = when (")
            .substringBefore("TrackJankStateValue(")

        assertTrue(source.contains("stateName = VIDEO_CARD_TRANSITION_JANK_STATE"))
        assertTrue(trackingBlock.contains("VideoCardTransitionSettleState.AutoEnter -> \"Opening\""))
        assertTrue(trackingBlock.contains("VideoCardTransitionSettleState.AutoReturn -> \"Returning\""))
        assertTrue(trackingBlock.contains("VideoCardTransitionSettleState.InteractiveSeek -> \"PredictiveReturn\""))
        assertTrue(trackingBlock.contains("VideoCardTransitionSettleState.CancelRestore -> \"GestureRestore\""))
        assertTrue(trackingBlock.contains("else -> null"))
        assertTrue(source.contains("stateValue = videoCardTransitionJankState"))
    }

    @Test
    fun navDisplayHostIntegratesPredictiveBackHandlerDecorator() {
        val source = navDisplayHostSource()
        val entryBlock = source
            .substringAfter("private fun BiliPaiMiuixNavEntry(")
            .substringBefore("@Composable\nprivate fun ProvideMiuixNavViewModelApplicationExtras")

        assertTrue(source.contains("BiliPaiMiuixNavEntry("))
        assertTrue(entryBlock.contains("rememberNavigationEventState(NavigationEventInfo.None)"))
        assertTrue(entryBlock.contains("NavigationBackHandler("))
        assertTrue(entryBlock.contains("isBackEnabled = interceptPredictiveBack"))
        assertTrue(entryBlock.contains("onBackCompleted = onBack"))
        assertTrue(source.contains("VideoCardTransitionSettleState.CancelRestore"))
        assertFalse(source.contains("LocalVideo" + "PredictiveReturnState"))
        assertFalse(source.contains("predictiveBackAnimationDecorator"))
    }

    @Test
    fun navDisplayHostRoutesPredictivePopThroughHandlerPolicy() {
        val source = navDisplayHostSource()
        val entries = navEntryProviderSource()

        assertTrue(source.contains("transition = globalTransition"))
        assertTrue(entries.contains("entry<BiliPaiNavKey.VideoDetail>("))
        assertTrue(entries.contains("transition = videoCardTransition"))
        assertTrue(entries.contains("entry<BiliPaiNavKey.FocusSettings>(swipeDismiss = swipeBackDirection"))
        assertTrue(source.contains("NavigationBackHandler("))
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
            .substringBefore("@Composable\nprivate fun BiliPaiMiuixNavEntry")
        assertTrue(boxBlock.indexOf("VideoCardTransitionNavBackdrop") < boxBlock.indexOf("NavDisplay("))
    }

    private fun navDisplayHostSource(): String {
        return listOf(
            File("app/src/main/java/com/android/purebilibili/navigation3/BiliPaiNavDisplayHost.kt"),
            File("src/main/java/com/android/purebilibili/navigation3/BiliPaiNavDisplayHost.kt")
        ).first { it.exists() }.readText()
    }

    private fun appNavigationSource(): String {
        return listOf(
            File("app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt"),
            File("src/main/java/com/android/purebilibili/navigation/AppNavigation.kt")
        ).first { it.exists() }.readText()
    }

    private fun navEntryProviderSource(): String {
        return listOf(
            File("app/src/main/java/com/android/purebilibili/navigation3/BiliPaiNavEntryProvider.kt"),
            File("src/main/java/com/android/purebilibili/navigation3/BiliPaiNavEntryProvider.kt")
        ).first { it.exists() }.readText()
    }

    private fun versionCatalogSource(): String {
        return listOf(File("gradle/libs.versions.toml"), File("../gradle/libs.versions.toml"))
            .first { it.exists() }.readText()
    }

    private fun settingsSource(): String {
        return listOf(File("settings.gradle.kts"), File("../settings.gradle.kts"))
            .first { it.exists() }.readText()
    }

    private fun predictiveBackTransitionSource(): String {
        return listOf(
            File("app/src/main/java/com/android/purebilibili/navigation3/predictiveback/BiliPaiMiuixNavTransition.kt"),
            File("src/main/java/com/android/purebilibili/navigation3/predictiveback/BiliPaiMiuixNavTransition.kt")
        ).first { it.exists() }.readText()
    }

    private fun miuixVideoTransitionSource(): String {
        return listOf(
            File("app/src/main/java/com/android/purebilibili/navigation3/predictiveback/MiuixVideoCardNavTransition.kt"),
            File("src/main/java/com/android/purebilibili/navigation3/predictiveback/MiuixVideoCardNavTransition.kt")
        ).first { it.exists() }.readText()
    }

    private fun videoCardClockSource(): String {
        return listOf(
            File("app/src/main/java/com/android/purebilibili/core/ui/transition/VideoCardTransitionClock.kt"),
            File("src/main/java/com/android/purebilibili/core/ui/transition/VideoCardTransitionClock.kt")
        ).first { it.exists() }.readText()
    }

    private fun buildFileSource(): String {
        return listOf(
            File("app/build.gradle.kts"),
            File("build.gradle.kts")
        ).first { it.exists() }.readText()
    }
}
