package com.android.purebilibili.navigation

import com.android.purebilibili.core.store.HomeSettings
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppNavigationAppearancePolicyTest {

    @Test
    fun mapsBottomBarAndTransitionFlagsFromHomeSettings() {
        val appearance = resolveAppNavigationAppearance(
            HomeSettings(
                isBottomBarFloating = false,
                bottomBarLabelMode = 2,
                isBottomBarBlurEnabled = false,
                cardTransitionEnabled = false
            )
        )

        assertFalse(appearance.cardTransitionEnabled)
        assertFalse(appearance.bottomBarBlurEnabled)
        kotlin.test.assertEquals(2, appearance.bottomBarLabelMode)
        assertFalse(appearance.bottomBarFloating)
    }

    @Test
    fun keepsDefaultsWithoutRemovedBackPreviewAppearanceState() {
        val appearance = resolveAppNavigationAppearance(HomeSettings())

        assertTrue(appearance.cardTransitionEnabled)
        assertFalse(appearance.bottomBarBlurEnabled)
        kotlin.test.assertEquals(0, appearance.bottomBarLabelMode)
        assertTrue(appearance.bottomBarFloating)
    }

    @Test
    fun defaultSettings_keepFloatingBottomBar() {
        val appearance = resolveAppNavigationAppearance(HomeSettings())

        assertTrue(appearance.bottomBarFloating)
        assertFalse(appearance.bottomBarBlurEnabled)
        kotlin.test.assertEquals(0, appearance.bottomBarLabelMode)
    }

    @Test
    fun explicitSettings_keepDockedBottomBarBlur() {
        val appearance = resolveAppNavigationAppearance(
            homeSettings = HomeSettings(
                isBottomBarFloating = false,
                isBottomBarBlurEnabled = true,
                androidNativeLiquidGlassEnabled = false,
            ),
        )

        assertFalse(appearance.bottomBarFloating)
        assertTrue(appearance.bottomBarBlurEnabled)
    }

    @Test
    fun defaultSettings_disableBottomBarBlur() {
        val appearance = resolveAppNavigationAppearance(HomeSettings())

        assertFalse(appearance.bottomBarBlurEnabled)
    }

    @Test
    fun preservesExplicitBottomBarShellCustomization() {
        val appearance = resolveAppNavigationAppearance(
            homeSettings = HomeSettings(
                isBottomBarFloating = true,
                bottomBarLabelMode = 1,
                isBottomBarBlurEnabled = false
            ),
        )

        assertTrue(appearance.bottomBarFloating)
        assertFalse(appearance.bottomBarBlurEnabled)
        kotlin.test.assertEquals(1, appearance.bottomBarLabelMode)
    }

    @Test
    fun shellDefaults_keepFloatingBottomBar() {
        val appearance = resolveAppNavigationAppearance(HomeSettings())

        assertTrue(appearance.bottomBarFloating)
        assertFalse(appearance.bottomBarBlurEnabled)
        kotlin.test.assertEquals(0, appearance.bottomBarLabelMode)
    }

    @Test
    fun bottomBarBackdropCapturesGlobalWallpaperBeforeNavDisplayContent() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt")
        val capturedLayerSource = source
            .substringAfter(".layerBackdrop(bottomBarBackdrop)")
            .substringBefore("// ===== 全局底栏")

        val wallpaperIndex = capturedLayerSource.indexOf("DepthSyncedGlobalHomeWallpaperBackdrop(")
        val navDisplayIndex = capturedLayerSource.indexOf("BiliPaiNavDisplayHost(")

        assertTrue(wallpaperIndex >= 0)
        assertTrue(navDisplayIndex > wallpaperIndex)
        assertTrue(capturedLayerSource.contains("depthProgressProvider"))
        assertTrue(capturedLayerSource.contains("videoCardTransitionClock.depthProgress()"))
        assertTrue(capturedLayerSource.contains("sourceBoundsProvider"))
        assertTrue(capturedLayerSource.contains("navigation3SourceMetadata.sourceBounds"))
        assertFalse(capturedLayerSource.contains("onVideoCardDepthFrame"))
        assertTrue(capturedLayerSource.contains("if (isBottomBarBlurEnabled && mainHazeState != null)"))
        assertTrue(capturedLayerSource.contains("Modifier.hazeSourceCompat(mainHazeState)"))
        assertTrue(source.contains("mainHostTabRoute = currentBottomNavItem.route"))
        assertTrue(source.contains("LocalWallpaperPalette provides"))
        assertTrue(source.contains("WallpaperPaletteStore.loadWallpaperPalette("))
    }

    @Test
    fun appNavigationPassesSkinDecorationAsReadOnlyBottomBarInput() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt")

        assertTrue(source.contains("val uiSkinState by rememberUiSkinState(context)"))
        assertTrue(source.contains("val bottomBarUiSkinDecoration = rememberBottomBarUiSkinDecoration(uiSkinState)"))
        assertTrue(source.contains("uiSkinDecoration = bottomBarUiSkinDecoration"))
        assertFalse(source.contains("uiSkinState.copy("))
        assertFalse(source.contains("bottomBarLiquidGlassPreset = uiSkin"))
        assertFalse(source.contains("isBottomBarLiquidGlassEnabled = uiSkin"))
    }

    @Test
    fun appNavigationUsesNeutralBottomBarPolicyWithoutReadingStyleLocals() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt")

        assertTrue(source.contains("rememberAppBottomBarContentPadding("))
        assertFalse(source.contains("LocalUiPreset"))
        assertFalse(source.contains("LocalAndroidNativeVariant"))
        assertFalse(source.contains("UiPreset"))
        assertFalse(source.contains("AndroidNativeVariant"))
    }

    @Test
    fun appNavigationProvidesGlobalSharedTransitionSwitch() {
        val navigationSource = loadSource("app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt")
        val activitySource = loadSource("app/src/main/java/com/android/purebilibili/MainActivity.kt")

        assertTrue(navigationSource.contains("val sharedVideoCardTransitionEnabled = cardTransitionEnabled && !systemReduceMotion"))
        assertTrue(navigationSource.contains("val officialVideoSharedBoundsEnabled ="))
        assertTrue(navigationSource.contains("sharedVideoCardTransitionEnabled && !liveSurfaceCardTransitionEnabled"))
        assertTrue(navigationSource.contains("SharedTransitionLayout(modifier = Modifier.fillMaxSize())"))
        assertTrue(navigationSource.contains("LocalSharedTransitionScope provides"))
        assertTrue(navigationSource.contains("this.takeIf { officialVideoSharedBoundsEnabled }"))
        assertTrue(navigationSource.contains("LocalOfficialVideoSharedTransition provides realVideoSharedTransition"))
        assertTrue(navigationSource.contains("cardTransitionEnabled = sharedVideoCardTransitionEnabled"))
        assertTrue(navigationSource.contains("cardTransitionEnabled && !systemReduceMotion"))
        assertTrue(
            navigationSource.contains(
                "VideoCardTransitionVisualTimeline.REDUCED_MOTION_DURATION_MILLIS"
            )
        )
        assertFalse(activitySource.contains("SharedTransitionProvider"))
    }

    @Test
    fun appNavigationReadsVideoTransitionRealtimeBlurSetting() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt")

        assertTrue(source.contains("videoTransitionRealtimeBlurEnabled"))
        assertTrue(source.contains("realtimeBlurEnabledProvider"))
        assertFalse(source.contains("video_source_background_blur"))
        assertFalse(source.contains("RenderEffect.createBlurEffect"))
    }

    @Test
    fun tabletSidebarReservesSpaceWithoutCoveringVideoCards() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt")
        val navigationRow = source
            .substringAfter("Box(modifier = Modifier.fillMaxSize()) {\n            Row(modifier = Modifier.fillMaxSize()) {")
            .substringBefore("// End of navigation content row")

        assertTrue(navigationRow.contains("AnimatedVisibility("))
        assertTrue(navigationRow.contains("FrostedSideBar("))
        assertFalse(navigationRow.contains(".align(Alignment.CenterStart)"))
        assertFalse(navigationRow.contains(".zIndex(2f)"))
    }

    @Test
    fun appNavigationKeepsFlyingEntryAsTheOnlyTransitionPixelOwner() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt")
        val navHostSource = loadSource(
            "app/src/main/java/com/android/purebilibili/navigation3/BiliPaiNavDisplayHost.kt"
        )
        val navHostCall = source
            .substringAfter("BiliPaiNavDisplayHost(")
            .substringBefore(") { key ->")

        assertFalse(source.contains("getVideoTransitionLiveReturnPreviewEnabled"))
        assertFalse(source.contains("videoTransitionLiveReturnPreviewEnabled"))
        assertTrue(navHostCall.contains("preferWholeCardReturn = false"))
        assertTrue(navHostSource.contains("preferWholeCardReturnProvider"))
        assertTrue(navHostSource.contains("preferWholeCardReturn: Boolean = false"))
    }

    @Test
    fun transitionRealtimeBlurDoesNotDependOnRemovedBackgroundScaleSetting() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/navigation/AppNavigation.kt")
        val navHostSource = loadSource(
            "app/src/main/java/com/android/purebilibili/navigation3/BiliPaiNavDisplayHost.kt"
        )
        val navHostCall = source
            .substringAfter("BiliPaiNavDisplayHost(")
            .substringBefore(") { key ->")

        assertTrue(navHostCall.contains("videoTransitionRealtimeBlurEnabled = videoTransitionRealtimeBlurEnabled"))
        assertTrue(navHostCall.contains("miuixTransitionBlurEnabled ="))
        assertFalse(navHostCall.contains("videoCardBackgroundSinkEnabled"))
        assertFalse(navHostSource.contains("isBackgroundSinkEnabledProvider ="))
        assertFalse(navHostSource.contains("videoCardBackgroundSinkEnabled"))
        assertTrue(navHostSource.contains("effectiveRealtimeBlurEnabled = videoTransitionRealtimeBlurEnabled ||"))
        assertTrue(navHostSource.contains("realtimeBlurEnabledProvider = { effectiveRealtimeBlurEnabled }"))
    }

    @Test
    fun appNavigationAppearanceDoesNotExposeRemovedBackPreviewState() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/navigation/AppNavigationAppearancePolicy.kt")

        assertFalse(source.contains("Predictive" + "BackAnimationStyle"))
        assertFalse(source.contains("predictive" + "BackAnimationStyle"))
    }

    private fun loadSource(path: String): String {
        val candidates = listOf(
            File(path),
            File("app", path.removePrefix("app/")),
            File(path.removePrefix("app/")),
            File("..", path)
        )
        return candidates.first { it.exists() }.readText()
    }
}
