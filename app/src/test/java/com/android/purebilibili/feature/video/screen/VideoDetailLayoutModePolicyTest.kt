package com.android.purebilibili.feature.video.screen

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import com.android.purebilibili.core.store.FullscreenMode
import com.android.purebilibili.core.ui.transition.shouldEnableVideoCoverSharedTransition
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
                isTabletDevice = true
            )
        )
    }

    @Test
    fun compact_doesNotUseTabletLayout() {
        assertFalse(
            shouldUseTabletVideoLayout(
                isExpandedScreen = false,
                isTabletDevice = true
            )
        )
    }

    @Test
    fun expandedPhoneLandscape_doesNotUseTabletLayout() {
        assertFalse(
            shouldUseTabletVideoLayout(
                isExpandedScreen = true,
                isTabletDevice = false
            )
        )
    }

    @Test
    fun autoRotatePolicy_appliesOnlyOnPhoneLayout() {
        assertTrue(
            shouldApplyPhoneAutoRotatePolicy(
                isCompactDevice = true
            )
        )
        assertFalse(
            shouldApplyPhoneAutoRotatePolicy(
                isCompactDevice = false
            )
        )
        assertFalse(
            shouldApplyPhoneAutoRotatePolicy(
                isCompactDevice = false
            )
        )
    }

    @Test
    fun portraitAndInteractionUi_policiesReflectCurrentBehavior() {
        assertTrue(shouldEnablePortraitExperience())
        assertTrue(shouldShowFrozenCommentBar())
        assertTrue(shouldShowVideoDetailActionButtons())
    }

    @Test
    fun frozenCommentBar_followsLiquidGlassGlobalToggle() {
        assertFalse(shouldShowFrozenCommentBar(isLiquidGlassEnabled = false))
        assertTrue(shouldShowVideoDetailActionButtons())
    }

    @Test
    fun frozenCommentBar_hidesOutsideSafePhoneCommentTab() {
        assertFalse(shouldShowFrozenCommentBar(selectedTabIndex = 0))
        assertFalse(shouldShowFrozenCommentBar(useTabletLayout = true))
        assertFalse(shouldShowFrozenCommentBar(isFullscreenMode = true))
        assertFalse(shouldShowFrozenCommentBar(isPortraitFullscreen = true))
    }

    @Test
    fun frozenCommentBar_hidesBehindBlockingOverlays() {
        assertFalse(shouldShowFrozenCommentBar(isCommentInputVisible = true))
        assertFalse(shouldShowFrozenCommentBar(isCommentThreadVisible = true))
        assertFalse(shouldShowFrozenCommentBar(isFavoriteFolderDialogVisible = true))
        assertFalse(shouldShowFrozenCommentBar(isExternalPlaylistQueueBarVisible = true))
    }

    @Test
    fun orientationDrivenFullscreen_isPhoneOnly() {
        assertTrue(
            shouldUseOrientationDrivenFullscreen(
                isCompactDevice = true
            )
        )
        assertFalse(
            shouldUseOrientationDrivenFullscreen(
                isCompactDevice = false
            )
        )
        assertFalse(
            shouldUseOrientationDrivenFullscreen(
                isCompactDevice = false
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
    fun splitBackRotationPolicy_treatsExpandedPhoneLandscapeAsPhone() {
        assertTrue(
            shouldRotateToPortraitOnSplitBack(
                useTabletLayout = true,
                isCompactDevice = true,
                orientation = Configuration.ORIENTATION_LANDSCAPE
            )
        )
        assertFalse(
            shouldRotateToPortraitOnSplitBack(
                useTabletLayout = true,
                isCompactDevice = false,
                orientation = Configuration.ORIENTATION_LANDSCAPE
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
    fun routedComment_forcesDetachedCommentThreadHostInitializationAfterAidIsReady() {
        assertTrue(
            shouldForceInitializeDetachedCommentThreadHostForRoute(
                routeCommentRootRpid = 11L,
                aid = 100L,
                hasHandledRouteComment = false
            )
        )
        assertFalse(
            shouldForceInitializeDetachedCommentThreadHostForRoute(
                routeCommentRootRpid = 11L,
                aid = 0L,
                hasHandledRouteComment = false
            )
        )
        assertFalse(
            shouldForceInitializeDetachedCommentThreadHostForRoute(
                routeCommentRootRpid = 11L,
                aid = 100L,
                hasHandledRouteComment = true
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
    fun systemMultiWindowFullscreenPolicy_doesNotApplyRouteOrientationRequestInsideSmallWindow() {
        assertFalse(
            shouldApplyStartFullscreenOrientationRequest(
                startInFullscreen = true,
                isOrientationDrivenFullscreen = true,
                isLandscape = false,
                isInMultiWindowMode = true
            )
        )
    }

    @Test
    fun phoneOrientationPolicy_doesNotWriteRequestedOrientationInsideSmallWindow() {
        assertEquals(
            null,
            resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = true,
                systemAutoRotateEnabled = true,
                fullscreenMode = FullscreenMode.AUTO,
                isCompactDevice = true,
                isOrientationDrivenFullscreen = true,
                isFullscreenMode = true,
                manualFullscreenRequested = true,
                isInMultiWindowMode = true
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
    fun phoneOrientationPolicy_releasesOrientationLockOnNonCompactLayout() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
            resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = true,
                fullscreenMode = FullscreenMode.AUTO,
                isCompactDevice = false,
                isOrientationDrivenFullscreen = false,
                isFullscreenMode = false
            )
        )
    }

    @Test
    fun phoneOrientationPolicy_tabletFullscreen_respectsConfiguredHorizontalMode() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = true,
                systemAutoRotateEnabled = false,
                fullscreenMode = FullscreenMode.HORIZONTAL,
                isCompactDevice = false,
                isOrientationDrivenFullscreen = false,
                isFullscreenMode = true
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
                isCompactDevice = true,
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
                isCompactDevice = true,
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
                isCompactDevice = true,
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
                isCompactDevice = true,
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
                isCompactDevice = true,
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
                isCompactDevice = true,
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
                isCompactDevice = true,
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
                isCompactDevice = true,
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
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
            resolvePhoneAutoRotateRequestedOrientation(
                orientationDegrees = 90,
                isCurrentlyLandscape = false
            )
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
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
    fun autoRotateSensorPolicy_usesExactLandscapeSideForRightTilt() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            resolvePhoneAutoRotateRequestedOrientation(
                orientationDegrees = 270,
                isCurrentlyLandscape = false
            )
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            resolvePhoneAutoRotateRequestedOrientation(
                orientationDegrees = 312,
                isCurrentlyLandscape = true
            )
        )
    }

    @Test
    fun phoneAutoRotateApplyPolicy_appliesLandscapeCandidateImmediately() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
            resolvePhoneAutoRotateTargetToApply(
                candidateOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
                lastLandscapeAppliedAtMs = null,
                nowMs = 1_000L
            )
        )
    }

    @Test
    fun phoneAutoRotateApplyPolicy_suppressesPortraitCandidateAfterRecentLandscape() {
        assertEquals(
            null,
            resolvePhoneAutoRotateTargetToApply(
                candidateOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                lastLandscapeAppliedAtMs = 1_000L,
                nowMs = 1_300L
            )
        )
    }

    @Test
    fun phoneAutoRotateApplyPolicy_allowsPortraitCandidateAfterLandscapeSettleWindow() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            resolvePhoneAutoRotateTargetToApply(
                candidateOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                lastLandscapeAppliedAtMs = 1_000L,
                nowMs = 1_000L + PHONE_AUTO_ROTATE_LANDSCAPE_SETTLE_MS
            )
        )
    }

    @Test
    fun phoneAutoRotateApplyPolicy_clearsUnknownCandidateImmediately() {
        assertEquals(
            null,
            resolvePhoneAutoRotateTargetToApply(
                candidateOrientation = null,
                lastLandscapeAppliedAtMs = 1_000L,
                nowMs = 1_300L
            )
        )
    }

    @Test
    fun phoneOrientationPolicy_autoRotateFullscreen_preservesExactLandscapeSide() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = true,
                systemAutoRotateEnabled = true,
                fullscreenMode = FullscreenMode.AUTO,
                isCompactDevice = true,
                isOrientationDrivenFullscreen = true,
                isFullscreenMode = true,
                currentRequestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
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
                isCompactDevice = true,
                isOrientationDrivenFullscreen = true,
                fullscreenMode = FullscreenMode.AUTO,
                manualPortraitHoldActive = true
            )
        )
        assertFalse(
            shouldObservePhoneAutoRotate(
                autoRotateEnabled = true,
                systemAutoRotateEnabled = false,
                isCompactDevice = true,
                isOrientationDrivenFullscreen = true,
                fullscreenMode = FullscreenMode.AUTO,
                manualPortraitHoldActive = true
            )
        )
    }

    @Test
    fun phoneOrientationObserverPolicy_doesNotListenInsideSmallWindow() {
        assertFalse(
            shouldObservePhoneAutoRotate(
                autoRotateEnabled = true,
                systemAutoRotateEnabled = true,
                isCompactDevice = true,
                isOrientationDrivenFullscreen = true,
                fullscreenMode = FullscreenMode.AUTO,
                manualPortraitHoldActive = false,
                isInMultiWindowMode = true
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
                isCompactDevice = true,
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
    fun autoPortraitRoutePolicy_doesNotEnter_whenWideWindowKeepsDetailContentVisible() {
        assertFalse(
            shouldAutoEnterPortraitFullscreenFromRoute(
                autoEnterPortraitFromRoute = true,
                startAudioFromRoute = false,
                portraitExperienceEnabled = true,
                useOfficialInlinePortraitDetailExperience = false,
                allowStandalonePortraitAutoEnter = false,
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

    private fun shouldShowFrozenCommentBar(
        isLiquidGlassEnabled: Boolean = true,
        useTabletLayout: Boolean = false,
        selectedTabIndex: Int = 1,
        isFullscreenMode: Boolean = false,
        isPortraitFullscreen: Boolean = false,
        isCommentInputVisible: Boolean = false,
        isCommentThreadVisible: Boolean = false,
        isFavoriteFolderDialogVisible: Boolean = false,
        isExternalPlaylistQueueBarVisible: Boolean = false
    ): Boolean {
        return shouldShowVideoDetailBottomInteractionBar(
            isLiquidGlassEnabled = isLiquidGlassEnabled,
            useTabletLayout = useTabletLayout,
            selectedTabIndex = selectedTabIndex,
            isFullscreenMode = isFullscreenMode,
            isPortraitFullscreen = isPortraitFullscreen,
            isCommentInputVisible = isCommentInputVisible,
            isCommentThreadVisible = isCommentThreadVisible,
            isFavoriteFolderDialogVisible = isFavoriteFolderDialogVisible,
            isExternalPlaylistQueueBarVisible = isExternalPlaylistQueueBarVisible
        )
    }
}
