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
    fun sensorLandscapeConfiguration_doesNotBecomeAnOppositeFixedSide() {
        assertEquals(
            null,
            resolveCurrentExactLandscapeOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE),
        )
        for (fullscreen in listOf(false, true)) {
            assertEquals(
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
                resolvePhoneVideoRequestedOrientation(
                    autoRotateEnabled = true,
                    fullscreenMode = FullscreenMode.HORIZONTAL,
                    isCompactDevice = true,
                    isOrientationDrivenFullscreen = true,
                    isFullscreenMode = fullscreen,
                    currentRequestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
                    isCurrentlyLandscape = true,
                ),
            )
        }
    }

    @Test
    fun fullscreenConfiguration_preservesEitherSensorDetectedExactSide() {
        for (side in listOf(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
        )) {
            assertEquals(
                side,
                resolvePhoneVideoRequestedOrientation(
                    autoRotateEnabled = true,
                    fullscreenMode = FullscreenMode.HORIZONTAL,
                    isCompactDevice = true,
                    isOrientationDrivenFullscreen = true,
                    isFullscreenMode = true,
                    currentRequestedOrientation = side,
                    isCurrentlyLandscape = true,
                ),
            )
        }
    }

    @Test
    fun landscapeThenPortraitThenManualFullscreen_keepsSensorSideSelection() {
        fun request(landscape: Boolean, fullscreen: Boolean, manual: Boolean, current: Int) =
            resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = true,
                fullscreenMode = FullscreenMode.HORIZONTAL,
                isCompactDevice = true,
                isOrientationDrivenFullscreen = true,
                isFullscreenMode = fullscreen,
                manualFullscreenRequested = manual,
                currentRequestedOrientation = current,
                isCurrentlyLandscape = landscape,
            )

        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            request(true, false, false, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            request(false, false, false, ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            request(false, false, true, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            request(true, true, true, ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE))
    }

    @Test
    fun foldableCoverWindow_isNotInferredAsFloatingFromInnerDisplayBounds() {
        assertFalse(
            shouldInferFloatingWindowFromBounds(
                currentBoundsSmallerThanMaximum = true,
                isFoldableCoverWindow = true,
            )
        )
        assertTrue(
            shouldInferFloatingWindowFromBounds(
                currentBoundsSmallerThanMaximum = true,
                isFoldableCoverWindow = false,
            )
        )
    }

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
    fun frozenCommentBar_remainsVisibleWhenLiquidGlassIsDisabled() {
        assertTrue(shouldShowFrozenCommentBar())
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
        assertTrue(shouldShowFrozenCommentBar(isCommentThreadVisible = true))
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
    fun phoneCommentThreadHost_doesNotOverlayExtraMainSheetUnderneathSubReply() {
        assertFalse(
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
    fun systemMultiWindowFullscreenPolicy_usesInWindowFullscreenInsteadOfRelaunchingTask() {
        assertTrue(
            shouldUseInWindowFullscreenForSystemMultiWindow(
                isInMultiWindowMode = true,
                isInPictureInPictureMode = false,
                isOrientationDrivenFullscreen = true,
                isFullscreenMode = false
            )
        )
        assertFalse(
            shouldUseInWindowFullscreenForSystemMultiWindow(
                isInMultiWindowMode = false,
                isInPictureInPictureMode = false,
                isOrientationDrivenFullscreen = true,
                isFullscreenMode = false
            )
        )
        assertFalse(
            shouldUseInWindowFullscreenForSystemMultiWindow(
                isInMultiWindowMode = true,
                isInPictureInPictureMode = true,
                isOrientationDrivenFullscreen = true,
                isFullscreenMode = false
            )
        )
        assertFalse(
            shouldUseInWindowFullscreenForSystemMultiWindow(
                isInMultiWindowMode = true,
                isInPictureInPictureMode = false,
                isOrientationDrivenFullscreen = true,
                isFullscreenMode = true
            )
        )
    }

    @Test
    fun floatingWindowFallback_detectsCurrentBoundsSmallerThanMaximum() {
        assertTrue(
            isWindowBoundsSmallerThanMaximum(
                currentWidth = 720,
                currentHeight = 1280,
                maximumWidth = 1080,
                maximumHeight = 2400
            )
        )
        assertFalse(
            isWindowBoundsSmallerThanMaximum(
                currentWidth = 1080,
                currentHeight = 2400,
                maximumWidth = 1080,
                maximumHeight = 2400
            )
        )
    }

    @Test
    fun fullscreenModePolicy_allowsManualInWindowFullscreenInsideSystemSmallWindow() {
        assertTrue(
            resolveVideoDetailFullscreenMode(
                isOrientationDrivenFullscreen = true,
                isLandscape = false,
                userRequestedFullscreen = true,
                isInMultiWindowMode = true
            )
        )
        assertFalse(
            resolveVideoDetailFullscreenMode(
                isOrientationDrivenFullscreen = true,
                isLandscape = false,
                userRequestedFullscreen = true,
                isInMultiWindowMode = false
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
    fun largeScreenAutoRotate_usesFullSensorWithoutEnteringFullscreen() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR,
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
                fullscreenMode = FullscreenMode.HORIZONTAL,
                isCompactDevice = false,
                isOrientationDrivenFullscreen = false,
                isFullscreenMode = true
            )
        )
    }

    @Test
    fun foldableInnerScreenDefaultsAutoFullscreenToPortrait() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = true,
                fullscreenMode = FullscreenMode.AUTO,
                isCompactDevice = false,
                isOrientationDrivenFullscreen = false,
                isFullscreenMode = true,
                preferPortraitForFlatFoldable = true
            )
        )
    }

    @Test
    fun tabletAutoFullscreenStillUsesLandscapeWhenVideoReportsVertical() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = true,
                fullscreenMode = FullscreenMode.AUTO,
                isCompactDevice = false,
                isOrientationDrivenFullscreen = false,
                isFullscreenMode = true,
                isVerticalVideo = true,
                preferPortraitForFlatFoldable = false
            )
        )
    }

    @Test
    fun compactPhoneIgnoresFoldablePortraitPreference() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = false,
                fullscreenMode = FullscreenMode.AUTO,
                isCompactDevice = true,
                isOrientationDrivenFullscreen = true,
                isFullscreenMode = true,
                preferPortraitForFlatFoldable = true
            )
        )
    }

    @Test
    fun phoneOrientationPolicy_autoRotateEnabled_defaultsToPortraitUntilSensorRequestsLandscape() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = true,
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
                fullscreenMode = FullscreenMode.AUTO,
                isCompactDevice = true,
                isOrientationDrivenFullscreen = true,
                isFullscreenMode = false
            )
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = false,
                fullscreenMode = FullscreenMode.AUTO,
                isCompactDevice = true,
                isOrientationDrivenFullscreen = true,
                isFullscreenMode = true
            )
        )
    }

    @Test
    fun autoRotateDisabled_preservesCurrentExactLandscapeSideOnBothScreens() {
        for (isCompactDevice in listOf(true, false)) {
            assertEquals(
                ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
                resolvePhoneVideoRequestedOrientation(
                    autoRotateEnabled = false,
                    fullscreenMode = FullscreenMode.AUTO,
                    isCompactDevice = isCompactDevice,
                    isOrientationDrivenFullscreen = isCompactDevice,
                    isFullscreenMode = true,
                    currentRequestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                )
            )
        }
    }

    @Test
    fun phoneOrientationPolicy_manualFullscreenRequest_withAutoRotate_forcesLandscape() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = true,
                fullscreenMode = FullscreenMode.AUTO,
                isCompactDevice = true,
                isOrientationDrivenFullscreen = true,
                isFullscreenMode = false,
                manualFullscreenRequested = true
            )
        )
    }

    @Test
    fun phoneOrientationPolicy_manualFullscreenRequest_withAutoRotateDisabled_holdsLandscapeUntilConfigurationCatchesUp() {
        // Regression: with auto-rotate OFF, pressing fullscreen applies SENSOR_LANDSCAPE
        // directly. Before configuration flips to landscape, isFullscreenMode is still
        // false. The orientation effect must honor manualFullscreenRequested and keep
        // requesting landscape, otherwise it reverts to PORTRAIT and yanks the user
        // back out of fullscreen (#782 class loop on auto-rotate off).
        // autoRotate off → resolveStableOrientationWhenAutoRotateDisabled converts
        // SENSOR_LANDSCAPE to a fixed LANDSCAPE side (matches the existing
        // phoneOrientationPolicy_autoRotateDisabled_switchesBetweenPortraitAndLandscapeLock
        // convention). The pre-fix branch returned PORTRAIT here and yanked the user
        // back out of fullscreen.
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = false,
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
    fun manualFullscreenSensorPolicy_doesNotTreatExistingPortraitPostureAsFullscreenExit() {
        val allowPortraitTransition = shouldAllowPhoneSensorPortraitTransition(
            autoRotateEnabled = true,
            manualFullscreenRequested = true,
        )
        assertFalse(allowPortraitTransition)
        assertEquals(
            null,
            resolvePhoneAutoRotateRequestedOrientation(
                orientationDegrees = 0,
                isCurrentlyLandscape = true,
                allowPortraitTransitions = allowPortraitTransition,
            )
        )

        val allowPortraitAfterLandscapeObserved = shouldAllowPhoneSensorPortraitTransition(
            autoRotateEnabled = true,
            manualFullscreenRequested = false,
        )
        assertTrue(
            allowPortraitAfterLandscapeObserved
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            resolvePhoneAutoRotateRequestedOrientation(
                orientationDegrees = 0,
                isCurrentlyLandscape = true,
                allowPortraitTransitions = allowPortraitAfterLandscapeObserved,
            )
        )
    }

    @Test
    fun manualFullscreenSensorPolicy_releasesIntentAfterPhysicalLandscapeIsObserved() {
        assertTrue(
            shouldReleaseManualFullscreenRequestAfterSensorTarget(
                manualFullscreenRequested = true,
                sensorTargetOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
            )
        )
        assertFalse(
            shouldReleaseManualFullscreenRequestAfterSensorTarget(
                manualFullscreenRequested = true,
                sensorTargetOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            )
        )
        assertFalse(
            shouldReleaseManualFullscreenRequestAfterSensorTarget(
                manualFullscreenRequested = false,
                sensorTargetOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            )
        )
    }

    @Test
    fun phoneOrientationPolicy_autoRotateHorizontalMode_withoutManualRequest_usesSensor() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = true,
                fullscreenMode = FullscreenMode.HORIZONTAL,
                isCompactDevice = true,
                isOrientationDrivenFullscreen = true,
                isFullscreenMode = false,
                manualFullscreenRequested = false
            )
        )
    }

    @Test
    fun effectivePhoneAutoRotate_requiresAppSettingAndNoManualPortraitHold() {
        assertTrue(
            resolveEffectivePhoneAutoRotateEnabled(
                autoRotateEnabled = true,
                manualPortraitHoldActive = false
            )
        )
        assertFalse(
            resolveEffectivePhoneAutoRotateEnabled(
                autoRotateEnabled = false,
                manualPortraitHoldActive = false
            )
        )
        assertFalse(
            resolveEffectivePhoneAutoRotateEnabled(
                autoRotateEnabled = true,
                manualPortraitHoldActive = true
            )
        )
    }

    @Test
    fun phoneOrientationPolicy_appAutoRotateCanRequestLandscapeIndependently() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
            resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = true,
                fullscreenMode = FullscreenMode.AUTO,
                isCompactDevice = true,
                isOrientationDrivenFullscreen = true,
                isFullscreenMode = true
            )
        )
    }

    @Test
    fun phoneOrientationPolicy_regularPhonePreservesDetectedSideAcrossFullscreenUpdates() {
        for (exactLandscape in listOf(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        )) {
            assertEquals(
                exactLandscape,
                resolvePhoneVideoRequestedOrientation(
                    autoRotateEnabled = true,
                    fullscreenMode = FullscreenMode.AUTO,
                    isCompactDevice = true,
                    isOrientationDrivenFullscreen = true,
                    isFullscreenMode = true,
                    currentRequestedOrientation = exactLandscape
                )
            )
            assertEquals(
                exactLandscape,
                resolvePhoneVideoRequestedOrientation(
                    autoRotateEnabled = true,
                    fullscreenMode = FullscreenMode.AUTO,
                    isCompactDevice = true,
                    isOrientationDrivenFullscreen = true,
                    isFullscreenMode = true,
                    manualFullscreenRequested = true,
                    currentRequestedOrientation = exactLandscape
                )
            )
        }
    }

    @Test
    fun phoneOrientationPolicy_coverScreenPreservesDetectedLandscapeSideWhileFullscreen() {
        for (exactLandscape in listOf(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        )) {
            assertEquals(
                exactLandscape,
                resolvePhoneVideoRequestedOrientation(
                    autoRotateEnabled = true,
                    fullscreenMode = FullscreenMode.AUTO,
                    isCompactDevice = true,
                    isOrientationDrivenFullscreen = true,
                    isFullscreenMode = true,
                    currentRequestedOrientation = exactLandscape,
                    preserveExactLandscapeSide = true,
                )
            )
        }
    }

    @Test
    fun phoneOrientationPolicy_manualPortraitHold_forcesPortraitUntilReleased() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = true,
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
    fun autoRotateSensorPolicy_regularPhoneTracksBothLandscapeSidesExplicitly() {
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
    fun autoRotateSensorPolicy_coverScreenUsesExactDetectedLandscapeSide() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            resolvePhoneAutoRotateRequestedOrientation(
                orientationDegrees = 270,
                isCurrentlyLandscape = false,
                useExactLandscapeSide = true,
            )
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
            resolvePhoneAutoRotateRequestedOrientation(
                orientationDegrees = 90,
                isCurrentlyLandscape = true,
                useExactLandscapeSide = true,
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
    fun autoRotateFullscreen_preservesCompactSideButReleasesItOnLargeScreen() {
        for (initialOrientation in listOf(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        )) {
            val compactOrientation = requireNotNull(resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = true,
                fullscreenMode = FullscreenMode.AUTO,
                isCompactDevice = true,
                isOrientationDrivenFullscreen = true,
                isFullscreenMode = true,
                currentRequestedOrientation = initialOrientation
            ))
            assertEquals(initialOrientation, compactOrientation)

            val expandedOrientation = requireNotNull(resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = true,
                fullscreenMode = FullscreenMode.AUTO,
                isCompactDevice = false,
                isOrientationDrivenFullscreen = false,
                isFullscreenMode = true,
                currentRequestedOrientation = compactOrientation
            ))
            assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, expandedOrientation)

            // Folding back starts from the released sensor request. The compact-screen listener
            // will resolve the physical side on its next orientation event.
            assertEquals(
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
                resolvePhoneVideoRequestedOrientation(
                    autoRotateEnabled = true,
                    fullscreenMode = FullscreenMode.AUTO,
                    isCompactDevice = true,
                    isOrientationDrivenFullscreen = true,
                    isFullscreenMode = true,
                    currentRequestedOrientation = expandedOrientation
                )
            )
        }
    }

    @Test
    fun regularPhone_halfTurnAndFullscreenUpdatesKeepDetectedSide() {
        for (manualFullscreen in listOf(false, true)) {
            var currentRequest = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            for ((degrees, expected) in listOf(
                90 to ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
                180 to ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
                270 to ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
                180 to ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
                90 to ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
            )) {
                currentRequest = resolvePhoneAutoRotateRequestedOrientation(
                    orientationDegrees = degrees,
                    isCurrentlyLandscape = true,
                ) ?: currentRequest
                // Simulate the fullscreen effect running after each sensor/configuration update.
                currentRequest = requireNotNull(resolvePhoneVideoRequestedOrientation(
                    autoRotateEnabled = true,
                    fullscreenMode = FullscreenMode.AUTO,
                    isCompactDevice = true,
                    isOrientationDrivenFullscreen = true,
                    isFullscreenMode = true,
                    manualFullscreenRequested = manualFullscreen,
                    currentRequestedOrientation = currentRequest,
                ))
                assertEquals(expected, currentRequest)
            }
        }
    }

    @Test
    fun autoRotateSensorPolicy_tracksBothExactLandscapeSidesDuringHalfTurn() {
        val expectedOrientations = listOf(
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
        )
        for ((orientationDegrees, expectedOrientation) in
            listOf(90, 270, 90).zip(expectedOrientations)
        ) {
            val requestedOrientation = resolvePhoneAutoRotateRequestedOrientation(
                orientationDegrees = orientationDegrees,
                isCurrentlyLandscape = true,
                useExactLandscapeSide = true,
            )
            assertEquals(expectedOrientation, requestedOrientation)
            assertTrue(isLandscapeRequestedOrientation(requireNotNull(requestedOrientation)))
        }
    }

    @Test
    fun autoRotateSensorPolicy_doesNotTreatUpsideDownAsPortraitWhileLandscape() {
        for (orientationDegrees in listOf(155, 180, 205)) {
            assertEquals(
                null,
                resolvePhoneAutoRotateRequestedOrientation(
                    orientationDegrees = orientationDegrees,
                    isCurrentlyLandscape = true,
                    useExactLandscapeSide = true,
                )
            )
            assertEquals(
                null,
                resolvePhoneAutoRotateRequestedOrientation(
                    orientationDegrees = orientationDegrees,
                    isCurrentlyLandscape = true,
                )
            )
        }
    }

    @Test
    fun autoRotateSensorPolicy_coverScreenSwitchesLandscapeSidesThroughUpsideDown() {
        val flipPath = listOf(
            90 to ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
            140 to ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
            180 to null,
            220 to ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            270 to ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
        )
        for ((orientationDegrees, expectedOrientation) in flipPath) {
            assertEquals(
                expectedOrientation,
                resolvePhoneAutoRotateRequestedOrientation(
                    orientationDegrees = orientationDegrees,
                    isCurrentlyLandscape = true,
                    useExactLandscapeSide = true,
                )
            )
        }
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
                isCompactDevice = true,
                isOrientationDrivenFullscreen = true,
                fullscreenMode = FullscreenMode.AUTO,
                manualPortraitHoldActive = true
            )
        )
        assertFalse(
            shouldObservePhoneAutoRotate(
                autoRotateEnabled = false,
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
                isCompactDevice = true,
                isOrientationDrivenFullscreen = true,
                fullscreenMode = FullscreenMode.AUTO,
                manualPortraitHoldActive = false,
                isInMultiWindowMode = true
            )
        )
    }

    @Test
    fun phoneOrientationObserverPolicy_keepsCoverScreenSensorActiveWhenAppAutoRotateIsOff() {
        assertTrue(
            shouldObservePhoneAutoRotate(
                autoRotateEnabled = false,
                isCompactDevice = true,
                isOrientationDrivenFullscreen = true,
                fullscreenMode = FullscreenMode.AUTO,
                manualPortraitHoldActive = false,
                observeWhenAutoRotateDisabled = true,
            )
        )
    }

    @Test
    fun fullscreenObserver_survivesCoverBeingReclassifiedAsPhoneWithAutoRotateOff() {
        for (isCover in listOf(true, false, true)) {
            assertTrue(shouldObservePhoneAutoRotate(
                autoRotateEnabled = false,
                isCompactDevice = true,
                isOrientationDrivenFullscreen = true,
                fullscreenMode = FullscreenMode.AUTO,
                manualPortraitHoldActive = false,
                observeWhenAutoRotateDisabled = isCover,
                isFullscreenMode = true,
            ))
        }
        for ((multiWindow, pip, portraitFullscreen) in listOf(
            Triple(true, false, false),
            Triple(false, true, false),
            Triple(false, false, true),
        )) {
            assertFalse(shouldObservePhoneAutoRotate(
                autoRotateEnabled = false,
                isCompactDevice = true,
                isOrientationDrivenFullscreen = true,
                fullscreenMode = FullscreenMode.AUTO,
                manualPortraitHoldActive = false,
                isInMultiWindowMode = multiWindow,
                isInPictureInPictureMode = pip,
                isPortraitFullscreen = portraitFullscreen,
                isFullscreenMode = true,
            ))
        }
    }

    @Test
    fun fullscreenHalfTurn_autoRotateOffKeepsDetectedSideAcrossConfigurationUpdates() {
        var currentRequest = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        for ((degrees, expected) in listOf(
            90 to ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
            180 to ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
            270 to ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            0 to ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            90 to ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
        )) {
            currentRequest = resolvePhoneAutoRotateRequestedOrientation(
                orientationDegrees = degrees,
                isCurrentlyLandscape = true,
                allowPortraitTransitions = false,
            ) ?: currentRequest
            currentRequest = requireNotNull(resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = false,
                fullscreenMode = FullscreenMode.AUTO,
                isCompactDevice = true,
                isOrientationDrivenFullscreen = true,
                isFullscreenMode = true,
                currentRequestedOrientation = currentRequest,
            ))
            assertEquals(expected, currentRequest)
        }
    }

    @Test
    fun phoneOrientationObserverPolicy_doesNotListenInPictureInPicture() {
        assertFalse(
            shouldObservePhoneAutoRotate(
                autoRotateEnabled = true,
                isCompactDevice = true,
                isOrientationDrivenFullscreen = true,
                fullscreenMode = FullscreenMode.AUTO,
                manualPortraitHoldActive = false,
                isInPictureInPictureMode = true
            )
        )
    }

    @Test
    fun phoneOrientationObserverPolicy_stopsListeningDuringPortraitImmersiveFullscreen() {
        assertFalse(
            shouldObservePhoneAutoRotate(
                autoRotateEnabled = true,
                isCompactDevice = true,
                isOrientationDrivenFullscreen = true,
                fullscreenMode = FullscreenMode.AUTO,
                manualPortraitHoldActive = false,
                isPortraitFullscreen = true
            )
        )
    }

    @Test
    fun phoneOrientationPolicy_doesNotForceRotationWhilePortraitImmersiveFullscreen() {
        assertEquals(
            null,
            resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = true,
                fullscreenMode = FullscreenMode.AUTO,
                isCompactDevice = true,
                isOrientationDrivenFullscreen = true,
                isFullscreenMode = true,
                isVerticalVideo = true,
                isPortraitFullscreen = true
            )
        )
    }

    @Test
    fun largeScreenAutoRotate_doesNotWriteOrientationInsideSmallWindowOrPip() {
        assertEquals(
            null,
            resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = true,
                fullscreenMode = FullscreenMode.AUTO,
                isCompactDevice = false,
                isOrientationDrivenFullscreen = false,
                isFullscreenMode = false,
                isInMultiWindowMode = true
            )
        )
        assertEquals(
            null,
            resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = true,
                fullscreenMode = FullscreenMode.AUTO,
                isCompactDevice = false,
                isOrientationDrivenFullscreen = false,
                isFullscreenMode = false,
                isInPictureInPictureMode = true
            )
        )
    }

    @Test
    fun phoneOrientationPolicy_fullscreenModeNone_keepsCurrentOrientation() {
        assertEquals(
            null,
            resolvePhoneVideoRequestedOrientation(
                autoRotateEnabled = true,
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
    fun videoDetailDispose_neverRestoresVideoDrivenLandscapeLock() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
            resolveVideoDetailExitRequestedOrientation(
                originalRequestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            )
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
            resolveVideoDetailExitRequestedOrientation(
                originalRequestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
            )
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
            resolveVideoDetailExitRequestedOrientation(
                originalRequestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            )
        )
    }

    @Test
    fun videoDetailEntrySnapshot_ignoresAlreadyLockedLandscape() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
            resolveVideoDetailEntryOrientationSnapshot(
                currentRequestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            )
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            resolveVideoDetailEntryOrientationSnapshot(
                currentRequestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            )
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
            resolveVideoDetailEntryOrientationSnapshot(
                currentRequestedOrientation = null
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

    @Test
    fun autoPortraitRoutePolicy_keepsOfficialInline_whenOnlySoftInitialVerticalHint() {
        // Home always passes autoPortrait + initialVertical for vertical cards; that must
        // not force standalone when official inline detail is active.
        assertFalse(
            shouldAutoEnterPortraitFullscreenFromRoute(
                autoEnterPortraitFromRoute = true,
                startAudioFromRoute = false,
                portraitExperienceEnabled = true,
                useOfficialInlinePortraitDetailExperience = true,
                isCurrentRouteVideoLoaded = true,
                isVerticalVideo = true,
                isPortraitFullscreen = false,
                hasAutoEnteredPortraitFromRoute = false,
                initialVerticalFromRoute = true,
            )
        )
    }

    @Test
    fun autoPortraitRoutePolicy_entersStandalone_whenDirectPortraitEntryDespiteInline() {
        assertTrue(
            shouldAutoEnterPortraitFullscreenFromRoute(
                autoEnterPortraitFromRoute = false,
                startAudioFromRoute = false,
                portraitExperienceEnabled = true,
                useOfficialInlinePortraitDetailExperience = true,
                isCurrentRouteVideoLoaded = true,
                isVerticalVideo = true,
                isPortraitFullscreen = false,
                hasAutoEnteredPortraitFromRoute = false,
                directPortraitEntryFromRoute = true,
            )
        )
    }

    @Test
    fun directPortraitSetting_recoversMissingRouteHintAfterDirectionLoads() {
        assertTrue(shouldEnterFromSetting())
        assertFalse(shouldEnterFromSetting(enabled = false))
        assertFalse(shouldEnterFromSetting(vertical = false))
        assertFalse(shouldEnterFromSetting(loaded = false))
        assertFalse(shouldEnterFromSetting(audio = true))
        assertFalse(shouldEnterFromSetting(alreadyEntered = true))
        assertFalse(shouldEnterFromSetting(fullscreen = true))
        assertFalse(shouldEnterFromSetting(allowAutoEnter = false))
    }

    private fun shouldEnterFromSetting(
        enabled: Boolean = true,
        vertical: Boolean = true,
        loaded: Boolean = true,
        audio: Boolean = false,
        alreadyEntered: Boolean = false,
        fullscreen: Boolean = false,
        allowAutoEnter: Boolean = true,
    ): Boolean = shouldAutoEnterPortraitFullscreenFromRoute(
        autoEnterPortraitFromRoute = false,
        startAudioFromRoute = audio,
        portraitExperienceEnabled = true,
        useOfficialInlinePortraitDetailExperience = true,
        allowStandalonePortraitAutoEnter = allowAutoEnter,
        isCurrentRouteVideoLoaded = loaded,
        isVerticalVideo = vertical,
        isPortraitFullscreen = fullscreen,
        hasAutoEnteredPortraitFromRoute = alreadyEntered,
        directPortraitEntryFromRoute = false,
        directPortraitEntryEnabled = enabled,
    )

    @Test
    fun startPortraitHint_onlyDirectPortraitEntryStartsInFullscreen() {
        assertTrue(
            shouldStartInPortraitFullscreenFromRouteHint(
                autoEnterPortraitFromRoute = false,
                startAudioFromRoute = false,
                initialVerticalFromRoute = false,
                directPortraitEntryFromRoute = true,
            )
        )
        assertFalse(
            shouldStartInPortraitFullscreenFromRouteHint(
                autoEnterPortraitFromRoute = true,
                startAudioFromRoute = false,
                initialVerticalFromRoute = true,
                directPortraitEntryFromRoute = false,
            )
        )
        assertFalse(
            shouldStartInPortraitFullscreenFromRouteHint(
                autoEnterPortraitFromRoute = true,
                startAudioFromRoute = false,
                initialVerticalFromRoute = false,
                directPortraitEntryFromRoute = false,
            )
        )
    }

    private fun shouldShowFrozenCommentBar(
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
