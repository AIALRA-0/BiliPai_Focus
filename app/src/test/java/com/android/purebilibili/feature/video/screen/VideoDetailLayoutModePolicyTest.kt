package com.android.purebilibili.feature.video.screen

import android.content.pm.ActivityInfo
import com.android.purebilibili.core.store.FullscreenMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoDetailLayoutModePolicyTest {

    @Test
    fun expanded_usesTabletLayout() {
        assertTrue(
            shouldUseTabletVideoLayout(
                isExpandedScreen = true,
                smallestScreenWidthDp = 700
            )
        )
    }

    @Test
    fun compact_doesNotUseTabletLayout() {
        assertFalse(
            shouldUseTabletVideoLayout(
                isExpandedScreen = false,
                smallestScreenWidthDp = 700
            )
        )
    }

    @Test
    fun expandedPhoneLandscape_doesNotUseTabletLayout() {
        assertFalse(
            shouldUseTabletVideoLayout(
                isExpandedScreen = true,
                smallestScreenWidthDp = 411
            )
        )
    }

    @Test
    fun autoRotatePolicy_appliesOnlyOnPhoneLayout() {
        assertTrue(
            shouldApplyPhoneAutoRotatePolicy(
                useTabletLayout = false
            )
        )
        assertFalse(
            shouldApplyPhoneAutoRotatePolicy(
                useTabletLayout = true
            )
        )
        assertFalse(
            shouldApplyPhoneAutoRotatePolicy(
                useTabletLayout = true
            )
        )
    }

    @Test
    fun portraitAndInteractionUi_policiesReflectCurrentBehavior() {
        assertTrue(shouldEnablePortraitExperience())
        assertFalse(shouldShowVideoDetailBottomInteractionBar())
        assertTrue(shouldShowVideoDetailActionButtons())
    }

    @Test
    fun interactionUi_isHiddenOnPhoneToo() {
        assertFalse(shouldShowVideoDetailBottomInteractionBar())
        assertTrue(shouldShowVideoDetailActionButtons())
    }

    @Test
    fun orientationDrivenFullscreen_isPhoneOnly() {
        assertTrue(
            shouldUseOrientationDrivenFullscreen(
                useTabletLayout = false
            )
        )
        assertFalse(
            shouldUseOrientationDrivenFullscreen(
                useTabletLayout = true
            )
        )
        assertFalse(
            shouldUseOrientationDrivenFullscreen(
                useTabletLayout = true
            )
        )
    }

    @Test
    fun detachedCommentThreadHost_isPhoneOnly() {
        assertTrue(
            shouldShowDetachedVideoCommentThreadHost(
                useTabletLayout = false
            )
        )
        assertFalse(
            shouldShowDetachedVideoCommentThreadHost(
                useTabletLayout = true
            )
        )
    }

    @Test
    fun phoneCommentThreadHost_usesMainSheetPresentationWhenEmbeddedPathIsEnabled() {
        assertTrue(
            resolveVideoDetailCommentThreadHostMainSheetVisible(
                useEmbeddedPresentation = true,
                subReplyVisible = true
            )
        )
        assertFalse(
            resolveVideoDetailCommentThreadHostMainSheetVisible(
                useEmbeddedPresentation = true,
                subReplyVisible = false
            )
        )
        assertFalse(
            resolveVideoDetailCommentThreadHostMainSheetVisible(
                useEmbeddedPresentation = false,
                subReplyVisible = true
            )
        )
    }

    @Test
    fun systemMultiWindowFullscreenPolicy_restoresMainWindowBeforeEnteringFullscreen() {
        assertTrue(
            shouldRestoreMainWindowBeforeEnteringFullscreen(
                isInMultiWindowMode = true,
                isInPictureInPictureMode = false,
                isOrientationDrivenFullscreen = true,
                isFullscreenMode = false
            )
        )
        assertFalse(
            shouldRestoreMainWindowBeforeEnteringFullscreen(
                isInMultiWindowMode = false,
                isInPictureInPictureMode = false,
                isOrientationDrivenFullscreen = true,
                isFullscreenMode = false
            )
        )
        assertFalse(
            shouldRestoreMainWindowBeforeEnteringFullscreen(
                isInMultiWindowMode = true,
                isInPictureInPictureMode = true,
                isOrientationDrivenFullscreen = true,
                isFullscreenMode = false
            )
        )
        assertFalse(
            shouldRestoreMainWindowBeforeEnteringFullscreen(
                isInMultiWindowMode = true,
                isInPictureInPictureMode = false,
                isOrientationDrivenFullscreen = true,
                isFullscreenMode = true
            )
        )
    }

    @Test
    fun sharedCoverTransition_requiresSwitchAndBothScopes() {
        assertTrue(
            shouldEnableVideoCoverSharedTransition(
                transitionEnabled = true,
                hasSharedTransitionScope = true,
                hasAnimatedVisibilityScope = true
            )
        )
        assertFalse(
            shouldEnableVideoCoverSharedTransition(
                transitionEnabled = false,
                hasSharedTransitionScope = true,
                hasAnimatedVisibilityScope = true
            )
        )
        assertFalse(
            shouldEnableVideoCoverSharedTransition(
                transitionEnabled = true,
                hasSharedTransitionScope = false,
                hasAnimatedVisibilityScope = true
            )
        )
        assertFalse(
            shouldEnableVideoCoverSharedTransition(
                transitionEnabled = true,
                hasSharedTransitionScope = true,
                hasAnimatedVisibilityScope = false
            )
        )
    }

    @Test
    fun highRefreshMode_prefersHighestRefreshThenResolution() {
        val selected = resolvePreferredHighRefreshModeId(
            currentModeId = 1,
            supportedModes = listOf(
                RefreshModeCandidate(modeId = 1, refreshRate = 60f, width = 2400, height = 1080),
                RefreshModeCandidate(modeId = 2, refreshRate = 120f, width = 1920, height = 1080),
                RefreshModeCandidate(modeId = 3, refreshRate = 120f, width = 2400, height = 1080)
            )
        )

        assertEquals(3, selected)
    }

    @Test
    fun highRefreshMode_returnsNullWhenNoEligibleHighRefresh() {
        val selected = resolvePreferredHighRefreshModeId(
            currentModeId = 1,
            supportedModes = listOf(
                RefreshModeCandidate(modeId = 1, refreshRate = 60f, width = 2400, height = 1080),
                RefreshModeCandidate(modeId = 2, refreshRate = 75f, width = 2400, height = 1080)
            )
        )

        assertEquals(null, selected)
    }

    @Test
    fun phoneOrientationPolicy_returnsNullOnTabletLayout() {
        assertEquals(
            null,
            resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = true,
                fullscreenMode = FullscreenMode.AUTO,
                useTabletLayout = true,
                isOrientationDrivenFullscreen = false,
                isFullscreenMode = false
            )
        )
    }

    @Test
    fun phoneOrientationPolicy_autoRotateEnabled_defaultsToPortraitUntilSensorRequestsLandscape() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = true,
                systemAutoRotateEnabled = true,
                fullscreenMode = FullscreenMode.AUTO,
                useTabletLayout = false,
                isOrientationDrivenFullscreen = true,
                isFullscreenMode = false
            )
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = true,
                systemAutoRotateEnabled = true,
                fullscreenMode = FullscreenMode.AUTO,
                useTabletLayout = false,
                isOrientationDrivenFullscreen = true,
                isFullscreenMode = true
            )
        )
    }

    @Test
    fun phoneOrientationPolicy_autoRotateDisabled_switchesBetweenPortraitAndLandscapeLock() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = false,
                systemAutoRotateEnabled = true,
                fullscreenMode = FullscreenMode.AUTO,
                useTabletLayout = false,
                isOrientationDrivenFullscreen = true,
                isFullscreenMode = false
            )
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = false,
                systemAutoRotateEnabled = true,
                fullscreenMode = FullscreenMode.AUTO,
                useTabletLayout = false,
                isOrientationDrivenFullscreen = true,
                isFullscreenMode = true
            )
        )
    }

    @Test
    fun phoneOrientationPolicy_manualFullscreenRequest_withAutoRotate_forcesLandscape() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = true,
                systemAutoRotateEnabled = true,
                fullscreenMode = FullscreenMode.AUTO,
                useTabletLayout = false,
                isOrientationDrivenFullscreen = true,
                isFullscreenMode = false,
                manualFullscreenRequested = true
            )
        )
    }

    @Test
    fun manualFullscreenRequestReleasePolicy_clearsRequestAfterLeavingObservedFullscreen() {
        assertFalse(
            shouldKeepManualFullscreenRequest(
                manualFullscreenRequested = true,
                hasEnteredFullscreenDuringRequest = true,
                isFullscreenMode = false
            )
        )
    }

    @Test
    fun manualFullscreenRequestReleasePolicy_keepsRequestWhileEnteringFullscreen() {
        assertTrue(
            shouldKeepManualFullscreenRequest(
                manualFullscreenRequested = true,
                hasEnteredFullscreenDuringRequest = false,
                isFullscreenMode = false
            )
        )
    }

    @Test
    fun phoneOrientationPolicy_autoRotateHorizontalMode_withoutManualRequest_usesSensor() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = true,
                systemAutoRotateEnabled = true,
                fullscreenMode = FullscreenMode.HORIZONTAL,
                useTabletLayout = false,
                isOrientationDrivenFullscreen = true,
                isFullscreenMode = false,
                manualFullscreenRequested = false
            )
        )
    }

    @Test
    fun effectivePhoneAutoRotate_requiresAppAndSystemAndNoManualPortraitHold() {
        assertTrue(
            resolveEffectivePhoneAutoRotateEnabled(
                autoRotateEnabled = true,
                systemAutoRotateEnabled = true,
                manualPortraitHoldActive = false
            )
        )
        assertFalse(
            resolveEffectivePhoneAutoRotateEnabled(
                autoRotateEnabled = true,
                systemAutoRotateEnabled = false,
                manualPortraitHoldActive = false
            )
        )
        assertFalse(
            resolveEffectivePhoneAutoRotateEnabled(
                autoRotateEnabled = true,
                systemAutoRotateEnabled = true,
                manualPortraitHoldActive = true
            )
        )
    }

    @Test
    fun phoneOrientationPolicy_systemRotationLock_preventsAutomaticLandscape() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = true,
                systemAutoRotateEnabled = false,
                fullscreenMode = FullscreenMode.AUTO,
                useTabletLayout = false,
                isOrientationDrivenFullscreen = true,
                isFullscreenMode = true
            )
        )
    }

    @Test
    fun phoneOrientationPolicy_manualPortraitHold_forcesPortraitUntilReleased() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = true,
                systemAutoRotateEnabled = true,
                fullscreenMode = FullscreenMode.AUTO,
                useTabletLayout = false,
                isOrientationDrivenFullscreen = true,
                isFullscreenMode = true,
                manualPortraitHoldActive = true
            )
        )
    }

    @Test
    fun autoRotateSensorPolicy_requiresStrongerTiltToEnterLandscapeButKeepsLandscapeStable() {
        assertEquals(
            null,
            resolvePhoneAutoRotateRequestedOrientation(
                orientationDegrees = 52,
                isCurrentlyLandscape = false
            )
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            resolvePhoneAutoRotateRequestedOrientation(
                orientationDegrees = 90,
                isCurrentlyLandscape = false
            )
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            resolvePhoneAutoRotateRequestedOrientation(
                orientationDegrees = 48,
                isCurrentlyLandscape = true
            )
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            resolvePhoneAutoRotateRequestedOrientation(
                orientationDegrees = 8,
                isCurrentlyLandscape = true
            )
        )
    }

    @Test
    fun manualPortraitHoldReleasePolicy_waitsForPortraitStableAngle() {
        assertFalse(
            shouldReleasePhoneManualPortraitHold(orientationDegrees = 90)
        )
        assertTrue(
            shouldReleasePhoneManualPortraitHold(orientationDegrees = 8)
        )
    }

    @Test
    fun phoneOrientationObserverPolicy_keepsListeningWhileManualPortraitHoldIsActive() {
        assertTrue(
            shouldObservePhoneAutoRotate(
                autoRotateEnabled = true,
                systemAutoRotateEnabled = true,
                useTabletLayout = false,
                isOrientationDrivenFullscreen = true,
                fullscreenMode = FullscreenMode.AUTO,
                manualPortraitHoldActive = true
            )
        )
        assertFalse(
            shouldObservePhoneAutoRotate(
                autoRotateEnabled = true,
                systemAutoRotateEnabled = false,
                useTabletLayout = false,
                isOrientationDrivenFullscreen = true,
                fullscreenMode = FullscreenMode.AUTO,
                manualPortraitHoldActive = true
            )
        )
    }

    @Test
    fun phoneOrientationPolicy_fullscreenModeNone_keepsCurrentOrientation() {
        assertEquals(
            null,
            resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = true,
                systemAutoRotateEnabled = true,
                fullscreenMode = FullscreenMode.NONE,
                useTabletLayout = false,
                isOrientationDrivenFullscreen = false,
                isFullscreenMode = true
            )
        )
    }

    @Test
    fun phoneEnterOrientationPolicy_respectsFullscreenMode() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            resolvePhoneFullscreenEnterOrientation(
                fullscreenMode = FullscreenMode.HORIZONTAL,
                isVerticalVideo = false
            )
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            resolvePhoneFullscreenEnterOrientation(
                fullscreenMode = FullscreenMode.VERTICAL,
                isVerticalVideo = true
            )
        )
        assertEquals(
            null,
            resolvePhoneFullscreenEnterOrientation(
                fullscreenMode = FullscreenMode.NONE,
                isVerticalVideo = false
            )
        )
    }

    @Test
    fun videoDetailDispose_restoresOriginalRequestedOrientationWhenPresent() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            resolveVideoDetailExitRequestedOrientation(
                originalRequestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            )
        )
    }

    @Test
    fun videoDetailDispose_defaultsToUnspecifiedWhenNoOriginalOrientationExists() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
            resolveVideoDetailExitRequestedOrientation(
                originalRequestedOrientation = null
            )
        )
    }

    @Test
    fun phoneEnterOrientationPolicy_autoMode_usesVideoDirection() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            resolvePhoneFullscreenEnterOrientation(
                fullscreenMode = FullscreenMode.AUTO,
                isVerticalVideo = true
            )
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            resolvePhoneFullscreenEnterOrientation(
                fullscreenMode = FullscreenMode.AUTO,
                isVerticalVideo = false
            )
        )
    }

    @Test
    fun fullscreenTogglePolicy_entersPortraitFullscreen_whenTargetIsPortraitAndExperienceEnabled() {
        assertTrue(
            shouldEnterPortraitFullscreenOnFullscreenToggle(
                targetOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                portraitExperienceEnabled = true
            )
        )
    }

    @Test
    fun fullscreenTogglePolicy_doesNotEnterPortraitFullscreen_whenExperienceDisabled() {
        assertFalse(
            shouldEnterPortraitFullscreenOnFullscreenToggle(
                targetOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                portraitExperienceEnabled = false
            )
        )
    }

    @Test
    fun fullscreenTogglePolicy_doesNotEnterPortraitFullscreen_whenTargetIsLandscape() {
        assertFalse(
            shouldEnterPortraitFullscreenOnFullscreenToggle(
                targetOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
                portraitExperienceEnabled = true
            )
        )
    }

    @Test
    fun autoPortraitRoutePolicy_enters_whenAllConditionsMatch() {
        assertTrue(
            shouldAutoEnterPortraitFullscreenFromRoute(
                autoEnterPortraitFromRoute = true,
                startAudioFromRoute = false,
                portraitExperienceEnabled = true,
                useOfficialInlinePortraitDetailExperience = false,
                isCurrentRouteVideoLoaded = true,
                isVerticalVideo = true,
                isPortraitFullscreen = false,
                hasAutoEnteredPortraitFromRoute = false
            )
        )
    }

    @Test
    fun autoPortraitRoutePolicy_doesNotEnter_whenAudioRoute() {
        assertFalse(
            shouldAutoEnterPortraitFullscreenFromRoute(
                autoEnterPortraitFromRoute = true,
                startAudioFromRoute = true,
                portraitExperienceEnabled = true,
                useOfficialInlinePortraitDetailExperience = false,
                isCurrentRouteVideoLoaded = true,
                isVerticalVideo = true,
                isPortraitFullscreen = false,
                hasAutoEnteredPortraitFromRoute = false
            )
        )
    }

    @Test
    fun autoPortraitRoutePolicy_doesNotEnter_whenNotVertical() {
        assertFalse(
            shouldAutoEnterPortraitFullscreenFromRoute(
                autoEnterPortraitFromRoute = true,
                startAudioFromRoute = false,
                portraitExperienceEnabled = true,
                useOfficialInlinePortraitDetailExperience = false,
                isCurrentRouteVideoLoaded = true,
                isVerticalVideo = false,
                isPortraitFullscreen = false,
                hasAutoEnteredPortraitFromRoute = false
            )
        )
    }

    @Test
    fun autoPortraitRoutePolicy_doesNotEnter_whenCurrentRouteVideoNotLoaded() {
        assertFalse(
            shouldAutoEnterPortraitFullscreenFromRoute(
                autoEnterPortraitFromRoute = true,
                startAudioFromRoute = false,
                portraitExperienceEnabled = true,
                useOfficialInlinePortraitDetailExperience = false,
                isCurrentRouteVideoLoaded = false,
                isVerticalVideo = true,
                isPortraitFullscreen = false,
                hasAutoEnteredPortraitFromRoute = false
            )
        )
    }

    @Test
    fun autoPortraitRoutePolicy_doesNotEnter_whenOfficialInlinePortraitModeIsActive() {
        assertFalse(
            shouldAutoEnterPortraitFullscreenFromRoute(
                autoEnterPortraitFromRoute = true,
                startAudioFromRoute = false,
                portraitExperienceEnabled = true,
                useOfficialInlinePortraitDetailExperience = true,
                isCurrentRouteVideoLoaded = true,
                isVerticalVideo = true,
                isPortraitFullscreen = false,
                hasAutoEnteredPortraitFromRoute = false
            )
        )
    }
}
