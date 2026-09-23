package com.android.purebilibili.feature.home

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeVideoTransitionBackgroundStructureTest {

    @Test
    fun headerAndFeedShareOneDepthSnapshot() {
        val source = homeScreenSource()
        val layout = source.substringAfter("val scaffoldLayout: @Composable () -> Unit = {")
            .substringBefore("val scaffoldContent:")
        assertTrue(layout.indexOf(".then(homeFeedSnapshotModifier)") < layout.indexOf("AppScaffold("))
        assertTrue(layout.contains("homeFeedOwnsVideoCardSnapshot ||"))
        assertTrue(layout.contains("!homeFeedOwnsVideoCardSnapshot && it.phase"))
        assertTrue(layout.split(".then(homeFeedSnapshotModifier)").size == 2)
    }

    @Test
    fun homeRootNoLongerOwnsVideoTransitionBackgroundBlurAndScrim() {
        val source = homeScreenSource()

        assertFalse(source.contains("homeVideoTransitionBackgroundProgress"))
        assertFalse(source.contains("HomeVideoTransitionBackgroundPhase"))
        assertFalse(source.contains("homeVideoTransitionBackgroundEffect"))
    }

    @Test
    fun homePageRecordsHostSnapshotAndRetainsHeaderFallback() {
        val source = homeScreenSource()

        assertTrue(source.contains("shouldHomeFeedOwnVideoCardTransitionSnapshot("))
        assertTrue(source.contains("videoCardTransitionBackgroundEffect("))
        assertTrue(source.contains(".then(homeFeedSnapshotModifier)"))
        assertTrue(source.contains("BottomBarMatchedDockVisibility("))
        assertTrue(source.contains("edge = BottomBarMatchedDockEdge.TOP"))
        assertTrue(source.contains("videoCardTransitionOverlayDepthEffect("))
        assertTrue(source.contains("shouldShowHomeOverlayChromeDuringVideoCardTransition("))
        val fullScreenHeaderDepthLayer = source
            .substringAfter("A header-sized layer clamps the blur kernel")
            .substringBefore("BottomBarMatchedDockVisibility(")
        assertTrue(fullScreenHeaderDepthLayer.contains(".fillMaxSize()"))
        assertTrue(fullScreenHeaderDepthLayer.contains(".videoCardTransitionOverlayDepthEffect("))
    }

    @Test
    fun navigationHostOnlyProvidesVideoTransitionBackgroundState() {
        val source = navDisplayHostSource()

        assertTrue(source.contains("VideoCardTransitionBackgroundPhase.OPENING"))
        assertTrue(source.contains("VideoCardTransitionBackgroundPhase.RETURNING"))
        assertTrue(source.contains("LocalVideoCardTransitionBackgroundState provides"))
        assertTrue(source.contains("resolveVideoCardTransitionMotionTier(reduceMotion)"))
        assertFalse(source.contains("runtimeGuardDecision.effectiveMotionTier"))
        assertFalse(source.contains("videoCardTransitionBackgroundEffect("))
    }

    @Test
    fun appNavigationAppliesVideoTransitionBackgroundToRealRouteContent() {
        val source = appNavigationSource()

        assertTrue(source.contains("VideoCardTransitionBackgroundRouteContent("))
        assertTrue(source.contains("videoCardTransitionBackgroundEffect("))
        assertTrue(source.contains("videoCardTransitionLiveBackgroundEffect("))
        assertTrue(source.contains("realtimeBlurEnabledProvider"))
        assertTrue(source.contains("shouldApplyVideoCardTransitionBackgroundToRoute("))
        assertTrue(source.contains("shouldApplyVideoCardTransitionSnapshotOnRouteShell("))
        assertTrue(source.contains("shouldUseHostOwnedVideoCardTransitionSnapshot("))
        assertTrue(source.contains("RenderNavigationContent(key)"))
        assertTrue(source.contains("sourceRoute = backgroundState.sourceRouteProvider()"))
        assertTrue(source.contains("sourceBoundsProvider = backgroundState.sourceBoundsProvider"))
        assertFalse(source.contains("val predictiveBlurProgress = predictiveBackState.progressProvider()"))
    }

    @Test
    fun mainHostOwnsExactlyOneVideoTransitionBackgroundLayer() {
        val source = appNavigationSource()
        val mainHostBranch = source
            .substringAfter("BiliPaiNavEntryContentRole.MAIN_HOST -> {")
            .substringBefore("BiliPaiNavEntryContentRole.HOME ->")

        assertFalse(
            mainHostBranch.contains("VideoCardTransitionBackgroundRouteContent("),
            "MainHost Pager must not recursively record the root snapshot GraphicsLayer",
        )
        assertTrue(
            source.substringAfter("BiliPaiNavDisplayHost(")
                .contains("VideoCardTransitionBackgroundRouteContent(key)"),
            "NavDisplay entry shell must remain the single transition background owner",
        )
        assertTrue(
            source.contains("val activeMainHostRoute = currentBottomNavItem.route"),
            "MainHost transition matching must retain the selected pager route while VideoDetail is top-most",
        )
        assertFalse(
            source.contains("activeMainHostRoute = activeBottomTabRoute"),
            "The top-most video route cannot be reused as the retained MainHost page identity",
        )
    }

    @Test
    fun videoCardTransitionBackgroundUsesFrozenSnapshotLayerForDynamicBlur() {
        val source = listOf(
            File("app/src/main/java/com/android/purebilibili/core/ui/transition/VideoCardTransitionBackgroundPolicy.kt"),
            File("src/main/java/com/android/purebilibili/core/ui/transition/VideoCardTransitionBackgroundPolicy.kt"),
        ).first { it.exists() }.readText()

        assertTrue(source.contains("rememberGraphicsLayer()"))
        assertTrue(source.contains("shouldUseVideoCardTransitionSnapshotBlur"))
        assertTrue(source.contains("freezeRecording"))
        assertTrue(source.contains("contentLayer.record"))
        assertTrue(source.contains("BlurEffect("))
        val overlayDepthEffect = source
            .substringAfter("internal fun Modifier.videoCardTransitionOverlayDepthEffect(")
            .substringBefore("internal fun resolveVideoCardTransitionBackgroundScalePivotOffset(")
        assertTrue(overlayDepthEffect.contains("resolveVideoCardTransitionBackgroundFrame("))
        assertTrue(overlayDepthEffect.contains("frame.blurRadiusPx"))
        assertTrue(source.contains("DisposableEffect(snapshotState, contentLayer, isHostOwnedSnapshot)"))
        assertTrue(source.contains("shouldInvalidateSnapshotOnSourceDispose"))
        assertTrue(source.contains("snapshotState.invalidateRecordedContent()"))
        // 冻结后不得每帧对 live content 再挂 android RenderEffect
        assertFalse(source.contains("android.graphics.RenderEffect"))
        assertFalse(source.contains("createBlurEffect("))
    }

    @Test
    fun navDisplayHostOwnsSessionDepthLayerUnderNavDisplay() {
        val source = navDisplayHostSource()
        assertTrue(source.contains("VideoCardTransitionHostDepthLayer("))
        assertTrue(source.contains("shouldReleaseHostOwnedDepthLayer("))
        assertTrue(source.contains("videoCardSnapshotHandle.releaseSession()"))
        // SettledHidden 不得再 clear 糊层，否则预测手势无满糊起点
        assertFalse(
            source.contains(
                "effectiveVideoCardExposure == VideoCardTransitionExposure.SettledHidden ||",
            ),
        )
        val boxBlock = source
            .substringAfter("Box(modifier = modifier.fillMaxSize())")
            .substringBefore("private fun ProvideNavigation3ViewModelApplicationExtras")
        assertTrue(boxBlock.indexOf("VideoCardTransitionHostDepthLayer") < boxBlock.indexOf("NavDisplay("))
    }

    private fun homeScreenSource(): String {
        return listOf(
            File("app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt"),
            File("src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt")
        ).first { it.exists() }.readText()
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
}
