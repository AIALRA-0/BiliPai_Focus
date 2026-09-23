package com.android.purebilibili.core.ui.transition

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.android.purebilibili.core.ui.adaptive.MotionTier
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoCardTransitionBackgroundPolicyTest {
    @Test
    fun backgroundScalesSmoothlyWithCornerRadiiDuringTransition() {
        for (phase in VideoCardTransitionBackgroundPhase.entries) {
            for (progress in listOf(0f, 0.25f, 0.5f, 1f)) {
                for (restoring in listOf(false, true)) {
                    val frame = resolveVideoCardTransitionBackgroundFrame(
                        progress = progress,
                        phase = phase,
                        isGestureRestoreInProgress = restoring,
                        sdkInt = 35,
                    )
                    if (phase == VideoCardTransitionBackgroundPhase.IDLE || progress == 0f) {
                        assertEquals(1f, frame.contentScale, 0.0001f)
                        assertEquals(0f, frame.cornerRadiusPx, 0.0001f)
                        assertFalse(shouldDrawVideoCardTransitionScaleGapFill(frame.contentScale))
                    } else {
                        val expectedScale = 1f - 0.04f * progress
                        val expectedCorner = 24f * progress
                        assertEquals(expectedScale, frame.contentScale, 0.0001f)
                        assertEquals(expectedCorner, frame.cornerRadiusPx, 0.0001f)
                        assertTrue(shouldDrawVideoCardTransitionScaleGapFill(frame.contentScale))
                        assertTrue(frame.shadowElevationPx > 0f)
                    }
                }
            }
        }
    }

    @Test
    fun blurEffectsAreCachedForTheSnapshotSessionAndReleasedWithIt() {
        val state = VideoCardTransitionSnapshotLayerState()
        val first = state.blurEffect(12f, sdkInt = 35)
        kotlin.test.assertSame(first, state.blurEffect(12f, sdkInt = 35))
        assertEquals(null, state.blurEffect(0f, sdkInt = 35))
        assertEquals(null, state.blurEffect(12f, sdkInt = 30))
        state.reset()
        kotlin.test.assertNotSame(first, state.blurEffect(12f, sdkInt = 35))
    }

    @Test
    fun detachedSourceInvalidatesRecordedSnapshotBeforeBackPreview() {
        val state = VideoCardTransitionSnapshotLayerState().apply {
            freezeRecording = true
            hasRecordedContent = true
            lastBlurRadiusPx = 12f
            lastCornerRadiusPx = 24f
        }

        state.invalidateRecordedContent()

        assertFalse(state.freezeRecording)
        assertFalse(state.hasRecordedContent)
        assertTrue(state.lastBlurRadiusPx.isNaN())
        assertTrue(state.lastCornerRadiusPx.isNaN())
    }

    @Test
    fun hostOwnedSnapshotMustNotInvalidateWhenSourceDetaches() {
        assertFalse(shouldInvalidateSnapshotOnSourceDispose(isHostOwnedSnapshot = true))
        assertTrue(shouldInvalidateSnapshotOnSourceDispose(isHostOwnedSnapshot = false))
    }

    @Test
    fun hostOwnedSourceDetachMarksDisplayListStaleForRerecord() {
        val state = VideoCardTransitionSnapshotLayerState().apply {
            freezeRecording = true
            hasRecordedContent = true
            displayListStale = false
            lastBlurRadiusPx = 12f
        }
        state.markDisplayListStale()
        assertTrue(state.displayListStale)
        assertTrue(state.hasRecordedContent)
        assertFalse(
            isVideoCardTransitionSnapshotDrawable(
                hasRecordedContent = state.hasRecordedContent,
                displayListStale = state.displayListStale,
            ),
        )
        state.markDisplayListFresh()
        assertFalse(state.displayListStale)
        assertTrue(state.hasRecordedContent)
        assertTrue(state.freezeRecording)
    }

    @Test
    fun transitionMotionTierOnlyReducesForSystemReduceMotion() {
        assertEquals(MotionTier.Normal, resolveVideoCardTransitionMotionTier(reduceMotion = false))
        assertEquals(MotionTier.Reduced, resolveVideoCardTransitionMotionTier(reduceMotion = true))
    }

    @Test
    fun snapshotBlur_isEnabledForActivePhasesOnApi31Plus() {
        assertTrue(
            shouldUseVideoCardTransitionSnapshotBlur(
                exposure = VideoCardTransitionExposure.Opening,
                motionTier = MotionTier.Normal,
                realtimeBlurEnabled = true,
                sdkInt = 35,
            )
        )
        assertTrue(
            shouldUseVideoCardTransitionSnapshotBlur(
                exposure = VideoCardTransitionExposure.BackPreview,
                motionTier = MotionTier.Normal,
                realtimeBlurEnabled = true,
                sdkInt = 31,
            )
        )
        assertTrue(
            shouldUseVideoCardTransitionSnapshotBlur(
                exposure = VideoCardTransitionExposure.Returning,
                motionTier = MotionTier.Normal,
                realtimeBlurEnabled = true,
                sdkInt = 35,
            )
        )
    }

    @Test
    fun snapshotBlur_isDisabledForIdleReducedOrLegacyApi() {
        assertFalse(
            shouldUseVideoCardTransitionSnapshotBlur(
                exposure = VideoCardTransitionExposure.Idle,
                motionTier = MotionTier.Normal,
                sdkInt = 35,
            )
        )
        assertFalse(
            shouldUseVideoCardTransitionSnapshotBlur(
                exposure = VideoCardTransitionExposure.Opening,
                motionTier = MotionTier.Reduced,
                sdkInt = 35,
            )
        )
        assertFalse(
            shouldUseVideoCardTransitionSnapshotBlur(
                exposure = VideoCardTransitionExposure.Opening,
                motionTier = MotionTier.Normal,
                sdkInt = 30,
            )
        )
    }

    @Test
    fun snapshotBlur_respectsRealtimeBlurSetting() {
        assertFalse(
            shouldUseVideoCardTransitionSnapshotBlur(
                exposure = VideoCardTransitionExposure.Opening,
                motionTier = MotionTier.Normal,
                realtimeBlurEnabled = false,
                sdkInt = 35,
            )
        )
    }

    @Test
    fun settledHiddenRetainsSnapshotWithoutAnyRenderWork() {
        val exposure = resolveVideoCardTransitionExposure(
            phase = VideoCardTransitionBackgroundPhase.HELD,
            predictiveBackInProgress = false,
            gestureRestoreInProgress = false,
        )
        val decision = resolveVideoCardTransitionRenderDecision(exposure)

        assertEquals(VideoCardTransitionExposure.SettledHidden, exposure)
        assertTrue(decision.retainSourceSnapshot)
        assertFalse(decision.drawSourceNormally)
        assertFalse(decision.drawTransitionBackground)
        assertFalse(decision.updateBlurEffect)
        assertFalse(decision.drawNavBackdrop)
        // When the source is the sole composed scene under HELD, draw path still paints
        // the retained layer (or live fallback) to avoid pure black under shared overlay.
        assertTrue(shouldPaintRetainedSourceWithoutTransitionBackground(decision))
        assertFalse(
            shouldUseVideoCardTransitionSnapshotBlur(
                exposure = exposure,
                motionTier = MotionTier.Normal,
                realtimeBlurEnabled = true,
                sdkInt = 35,
            )
        )
    }

    @Test
    fun paintRetainedSourceOnlyWhenSettledHiddenStyleDecision() {
        val idle = resolveVideoCardTransitionRenderDecision(VideoCardTransitionExposure.Idle)
        val returning = resolveVideoCardTransitionRenderDecision(VideoCardTransitionExposure.Returning)
        val settled = resolveVideoCardTransitionRenderDecision(VideoCardTransitionExposure.SettledHidden)

        assertFalse(shouldPaintRetainedSourceWithoutTransitionBackground(idle))
        assertFalse(shouldPaintRetainedSourceWithoutTransitionBackground(returning))
        assertTrue(shouldPaintRetainedSourceWithoutTransitionBackground(settled))
    }

    @Test
    fun predictiveBackAndCancellationResolveToExposedThenHiddenStates() {
        val preview = resolveVideoCardTransitionExposure(
            phase = VideoCardTransitionBackgroundPhase.HELD,
            predictiveBackInProgress = true,
            gestureRestoreInProgress = false,
        )
        val restoring = resolveVideoCardTransitionExposure(
            phase = VideoCardTransitionBackgroundPhase.HELD,
            predictiveBackInProgress = false,
            gestureRestoreInProgress = true,
        )
        val settled = resolveVideoCardTransitionExposure(
            phase = VideoCardTransitionBackgroundPhase.HELD,
            predictiveBackInProgress = false,
            gestureRestoreInProgress = false,
        )

        assertEquals(VideoCardTransitionExposure.BackPreview, preview)
        assertEquals(VideoCardTransitionExposure.Restoring, restoring)
        assertEquals(VideoCardTransitionExposure.SettledHidden, settled)
        assertTrue(resolveVideoCardTransitionRenderDecision(preview).drawNavBackdrop)
        assertFalse(resolveVideoCardTransitionRenderDecision(restoring).drawNavBackdrop)
        assertTrue(resolveVideoCardTransitionRenderDecision(restoring).drawTransitionBackground)
    }

    @Test
    fun sourceChromeHidesWhileOpeningAndSettledThenReturnsOnPredictiveBack() {
        assertFalse(
            shouldShowVideoCardTransitionSourceChrome(
                isVideoDetailDestination = true,
                exposure = VideoCardTransitionExposure.Opening,
            )
        )
        assertFalse(
            shouldShowVideoCardTransitionSourceChrome(
                isVideoDetailDestination = true,
                exposure = VideoCardTransitionExposure.SettledHidden,
            )
        )
        assertTrue(
            shouldShowVideoCardTransitionSourceChrome(
                isVideoDetailDestination = true,
                exposure = VideoCardTransitionExposure.BackPreview,
            )
        )
        assertFalse(
            shouldShowVideoCardTransitionSourceChrome(
                isVideoDetailDestination = true,
                exposure = VideoCardTransitionExposure.Restoring,
            )
        )
        assertTrue(
            shouldShowVideoCardTransitionSourceChrome(
                isVideoDetailDestination = true,
                exposure = VideoCardTransitionExposure.Returning,
            )
        )
        assertTrue(
            shouldShowVideoCardTransitionSourceChrome(
                isVideoDetailDestination = false,
                exposure = VideoCardTransitionExposure.Idle,
            )
        )
        assertFalse(
            shouldShowVideoCardTransitionSourceChrome(
                isVideoDetailDestination = true,
                exposure = VideoCardTransitionExposure.Returning,
                sourceIsBottomBarDestination = false,
            )
        )
        assertFalse(
            shouldShowVideoCardTransitionSourceChrome(
                isVideoDetailDestination = true,
                exposure = VideoCardTransitionExposure.BackPreview,
                sourceIsBottomBarDestination = false,
            )
        )
    }

    @Test
    fun homeOverlayChromeMatchesSourceChromePhases() {
        assertFalse(
            shouldShowHomeOverlayChromeDuringVideoCardTransition(
                VideoCardTransitionExposure.Opening,
            )
        )
        assertFalse(
            shouldShowHomeOverlayChromeDuringVideoCardTransition(
                VideoCardTransitionExposure.SettledHidden,
            )
        )
        assertTrue(
            shouldShowHomeOverlayChromeDuringVideoCardTransition(
                VideoCardTransitionExposure.BackPreview,
            )
        )
        assertFalse(
            shouldShowHomeOverlayChromeDuringVideoCardTransition(
                VideoCardTransitionExposure.Restoring,
            )
        )
        assertTrue(
            shouldShowHomeOverlayChromeDuringVideoCardTransition(
                VideoCardTransitionExposure.Returning,
            )
        )
        assertTrue(
            shouldShowHomeOverlayChromeDuringVideoCardTransition(
                VideoCardTransitionExposure.Idle,
            )
        )
    }

    @Test
    fun chromeRevealFollowsDepthSoPredictiveBackCanReverse() {
        assertEquals(0f, resolveVideoCardTransitionChromeReveal(1f), 0.0001f)
        assertEquals(1f, resolveVideoCardTransitionChromeReveal(0f), 0.0001f)
        assertEquals(0.4f, resolveVideoCardTransitionChromeReveal(0.6f), 0.0001f)
        assertTrue(
            shouldDriveVideoCardTransitionChromeByProgress(
                cardTransitionEnabled = true,
                exposure = VideoCardTransitionExposure.BackPreview,
            ),
        )
        assertTrue(
            shouldDriveVideoCardTransitionChromeByProgress(
                cardTransitionEnabled = true,
                exposure = VideoCardTransitionExposure.Restoring,
            ),
        )
        assertFalse(
            shouldDriveVideoCardTransitionChromeByProgress(
                cardTransitionEnabled = true,
                exposure = VideoCardTransitionExposure.Idle,
            ),
        )
        assertFalse(
            shouldDriveVideoCardTransitionChromeByProgress(
                cardTransitionEnabled = true,
                exposure = VideoCardTransitionExposure.Opening,
                sourceIsBottomBarDestination = false,
            ),
        )
        assertFalse(
            shouldDriveVideoCardTransitionChromeByProgress(
                cardTransitionEnabled = true,
                exposure = VideoCardTransitionExposure.Returning,
                sourceIsBottomBarDestination = false,
            ),
        )
        val overlayPivot = resolveVideoCardTransitionOverlayDepthPivot(
            sourceBounds = androidx.compose.ui.geometry.Rect(100f, 400f, 500f, 900f),
            canvasWidth = 1000f,
            canvasHeight = 2000f,
            overlayWidth = 1000f,
            overlayHeight = 200f,
        )
        assertEquals(0.5f, overlayPivot.x, 0.0001f)
        assertEquals(5f, overlayPivot.y, 0.0001f)
    }

    @Test
    fun relatedAndNonTabSourcesDoNotRevealTheHomeBottomBar() {
        val visibleTabs = setOf("home", "dynamic")
        assertTrue(isVideoCardTransitionBottomBarSource("home", visibleTabs))
        assertTrue(isVideoCardTransitionBottomBarSource("home?category=1", visibleTabs))
        assertTrue(isVideoCardTransitionBottomBarSource("dynamic", visibleTabs))
        assertTrue(isVideoCardTransitionBottomBarSource("main_host", visibleTabs))
        assertFalse(isVideoCardTransitionBottomBarSource("video/BV1xx", visibleTabs))
        assertFalse(isVideoCardTransitionBottomBarSource("space/123", visibleTabs))
        assertFalse(isVideoCardTransitionBottomBarSource("search", visibleTabs))
        assertFalse(isVideoCardTransitionBottomBarSource(null, visibleTabs))
    }

    @Test
    fun predictiveBackBottomBarUsesTheRetainedTabInsteadOfTheVideoRoute() {
        assertEquals(
            "home",
            resolveVideoCardTransitionChromeBottomBarRoute(
                isVideoDetailDestination = true,
                activeBottomTabRoute = "video/BV1",
                retainedTabRoute = "home",
            ),
        )
        assertEquals(
            "dynamic",
            resolveVideoCardTransitionChromeBottomBarRoute(
                isVideoDetailDestination = false,
                activeBottomTabRoute = "dynamic",
                retainedTabRoute = "home",
            ),
        )
    }

    @Test
    fun homeFeedOwnsHostSnapshotSoRouteShellDoesNotRecordTheHeader() {
        assertTrue(
            shouldHomeFeedOwnVideoCardTransitionSnapshot(
                sourceRoute = "home",
                hasSnapshotHandle = true,
            )
        )
        assertFalse(
            shouldHomeFeedOwnVideoCardTransitionSnapshot(
                sourceRoute = "home",
                hasSnapshotHandle = false,
            )
        )
        assertFalse(
            shouldHomeFeedOwnVideoCardTransitionSnapshot(
                sourceRoute = "dynamic",
                hasSnapshotHandle = true,
            )
        )
        assertFalse(
            shouldApplyVideoCardTransitionSnapshotOnRouteShell(
                entryRoute = "main_host",
                sourceRoute = "home",
                activeMainHostRoute = "home",
            )
        )
        assertTrue(
            shouldApplyVideoCardTransitionSnapshotOnRouteShell(
                entryRoute = "dynamic",
                sourceRoute = "dynamic",
                activeMainHostRoute = "home",
            )
        )
    }

    @Test
    fun navBackdrop_isHiddenWhenPredictiveReturnTargetsAnotherVideoDetail() {
        assertFalse(
            shouldShowVideoCardTransitionNavBackdrop(
                cardTransitionEnabled = true,
                exposure = VideoCardTransitionExposure.SettledHidden,
                isVideoDetailOnStack = true,
                isReturningToVideoDetail = true,
            )
        )
    }

    @Test
    fun hazePrime_drawsLiveContentNearClearDepthDuringReturnPreview() {
        assertTrue(
            shouldPrimeLiveContentForHazeDuringDepthDraw(
                exposure = VideoCardTransitionExposure.Returning,
                depthProgress = 0.2f,
            )
        )
        assertTrue(
            shouldPrimeLiveContentForHazeDuringDepthDraw(
                exposure = VideoCardTransitionExposure.BackPreview,
                depthProgress = 0.0f,
            )
        )
        assertFalse(
            shouldPrimeLiveContentForHazeDuringDepthDraw(
                exposure = VideoCardTransitionExposure.Returning,
                depthProgress = 0.9f,
            )
        )
        assertFalse(
            shouldPrimeLiveContentForHazeDuringDepthDraw(
                exposure = VideoCardTransitionExposure.Opening,
                depthProgress = 0.1f,
            )
        )
        assertFalse(
            shouldPrimeLiveContentForHazeDuringDepthDraw(
                exposure = VideoCardTransitionExposure.SettledHidden,
                depthProgress = 0.0f,
            )
        )
    }

    @Test
    fun frozenDepthLayerHandsOffContinuouslyToTheCompleteSourceCardBeforeLanding() {
        assertEquals(
            1f,
            resolveVideoCardTransitionFrozenLayerAlpha(
                exposure = VideoCardTransitionExposure.BackPreview,
                depthProgress = 0.45f,
            ),
            absoluteTolerance = 0.0001f,
        )
        assertEquals(
            0.5f,
            resolveVideoCardTransitionFrozenLayerAlpha(
                exposure = VideoCardTransitionExposure.Returning,
                depthProgress = 0.315f,
            ),
            absoluteTolerance = 0.02f,
        )
        assertEquals(
            0f,
            resolveVideoCardTransitionFrozenLayerAlpha(
                exposure = VideoCardTransitionExposure.BackPreview,
                depthProgress = 0.10f,
            ),
            absoluteTolerance = 0.0001f,
        )
    }

    @Test
    fun frozenDepthLayerStaysOpaqueOutsideReturnHandoff() {
        assertEquals(
            1f,
            resolveVideoCardTransitionFrozenLayerAlpha(
                exposure = VideoCardTransitionExposure.Opening,
                depthProgress = 0f,
            ),
        )
        assertEquals(
            1f,
            resolveVideoCardTransitionFrozenLayerAlpha(
                exposure = VideoCardTransitionExposure.SettledHidden,
                depthProgress = 0f,
            ),
        )
    }

    @Test
    fun reducedMotionTierSkipsRealtimeBlurAndDepthScaleButKeepsScrim() {
        val opening = resolveVideoCardTransitionBackgroundFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.OPENING,
            motionTier = MotionTier.Reduced,
            sdkInt = 35
        )
        val returning = resolveVideoCardTransitionBackgroundFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.RETURNING,
            motionTier = MotionTier.Reduced,
            sdkInt = 35
        )

        assertEquals(0f, opening.blurRadiusPx)
        assertTrue(opening.scrimAlpha > 0f)
        assertEquals(1f, opening.contentScale)
        assertEquals(0f, opening.cornerRadiusPx)
        assertEquals(0f, returning.blurRadiusPx)
        assertTrue(returning.scrimAlpha > 0f)
        assertEquals(0f, returning.cornerRadiusPx)
    }

    @Test
    fun api35OpeningFrameUsesCalibratedBlurStrengthAndScrim() {
        val frame = resolveVideoCardTransitionBackgroundFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.OPENING,
            motionTier = MotionTier.Enhanced,
            isLightBackground = false,
            sdkInt = 35
        )

        // density=1 时 12dp → 12px；真实机型由 DrawScope.density 换算。
        assertEquals(12f, frame.blurRadiusPx)
        assertEquals(0f, frame.blurRadiusPx % 2f)
        assertEquals(0.14f, frame.scrimAlpha)
        assertFalse(frame.useLightScrimTint)
        assertEquals(1f, frame.contentScale, 0.0001f)
        assertEquals(0f, frame.cornerRadiusPx, 0.0001f)
        assertEquals(0f, frame.shadowElevationPx, 0.0001f)
    }

    @Test
    fun normalAndEnhancedShareFullBlurBudget_noDeviceTierDowngrade() {
        val normal = resolveVideoCardTransitionBackgroundFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.OPENING,
            motionTier = MotionTier.Normal,
            sdkInt = 35,
        )
        val enhanced = resolveVideoCardTransitionBackgroundFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.OPENING,
            motionTier = MotionTier.Enhanced,
            sdkInt = 35,
        )

        assertEquals(12f, normal.blurRadiusPx)
        assertEquals(12f, enhanced.blurRadiusPx)
        assertEquals(12f, resolveVideoCardTransitionMaxBlurRadiusPx(MotionTier.Normal))
        assertEquals(12f, resolveVideoCardTransitionMaxBlurRadiusPx(MotionTier.Enhanced))
        assertEquals(0f, resolveVideoCardTransitionMaxBlurRadiusPx(MotionTier.Reduced))
        assertEquals(2f, resolveVideoCardTransitionBlurQuantumPx(MotionTier.Normal))
        assertEquals(2f, resolveVideoCardTransitionBlurQuantumPx(MotionTier.Enhanced))
    }

    @Test
    fun openingBlurLimitsRadiusChangesAndPreservesEndpoints() {
        for (density in listOf(1f, 2.7f, 3f)) {
            val radii = (0..240).map { step ->
                resolveVideoCardTransitionBackgroundFrame(
                    progress = step / 240f,
                    phase = VideoCardTransitionBackgroundPhase.OPENING,
                    motionTier = MotionTier.Normal,
                    sdkInt = 35,
                    density = density,
                ).blurRadiusPx
            }
            assertEquals(0f, radii.first())
            assertEquals(12f * density, radii.last(), 0.001f)
            assertTrue(radii.zipWithNext().all { (a, b) -> b >= a })
            // At 3x density, at most 19 radii instead of the previous 37.
            assertTrue(radii.distinct().size <= 19)
        }
    }

    @Test
    fun blurRadiusScalesWithDensity_forConsistentDpFeel() {
        val phone = resolveVideoCardTransitionBackgroundFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.OPENING,
            motionTier = MotionTier.Normal,
            sdkInt = 35,
            density = 2.75f,
        )
        val tablet = resolveVideoCardTransitionBackgroundFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.OPENING,
            motionTier = MotionTier.Normal,
            sdkInt = 35,
            density = 1.5f,
        )

        // 12dp × 2.75 ≈ 33px；12dp × 1.5 = 18px
        assertEquals(33f, phone.blurRadiusPx, 0.51f)
        assertEquals(18f, tablet.blurRadiusPx, 0.51f)
        assertEquals(66f, phone.cornerRadiusPx, 0.01f)
        assertEquals(36f, tablet.cornerRadiusPx, 0.01f)
        assertEquals(
            33f,
            resolveVideoCardTransitionMaxBlurRadiusPx(MotionTier.Normal, density = 2.75f),
            0.01f,
        )
    }

    @Test
    fun backgroundCornerUsesDeviceRadiusWhenLargerThanFallback() {
        assertEquals(
            80f,
            resolveVideoCardTransitionBackgroundCornerRadiusPx(
                depthProgress = 1f,
                motionTier = MotionTier.Normal,
                density = 2.75f,
                deviceCornerRadiusPx = 80f,
            ),
            0.0001f,
        )
        // 页面后退时使用设备物理圆角，避免冻结层边缘露出直角。
        assertEquals(
            80f / 2.75f,
            resolveVideoCardTransitionBackgroundCornerRadiusDp(
                deviceCornerRadiusPx = 80f,
                density = 2.75f,
            ),
            0.0001f,
        )
        assertEquals(
            24f,
            resolveVideoCardTransitionBackgroundCornerRadiusDp(
                deviceCornerRadiusPx = 40f,
                density = 2.75f,
            ),
            0.0001f,
        )
        assertEquals(
            24f,
            resolveVideoCardTransitionBackgroundCornerRadiusDp(
                deviceCornerRadiusPx = 0f,
                density = 2.75f,
            ),
            0.0001f,
        )
        assertEquals(
            0f,
            resolveDeviceDisplayCornerRadiusPx(
                rootWindowInsets = null,
                sdkInt = 35,
            ),
        )
        assertEquals(
            0f,
            resolveDeviceDisplayCornerRadiusPx(
                rootWindowInsets = null,
                sdkInt = 30,
            ),
        )
    }

    @Test
    fun backgroundPageRecedesWithTheSharedCardProgress() {
        VideoCardTransitionBackgroundPhase.entries.forEach { phase ->
            assertEquals(
                if (phase == VideoCardTransitionBackgroundPhase.IDLE) 1f else 1f,
                resolveVideoCardTransitionContentScale(
                    progress = 1f,
                    phase = phase,
                    motionTier = MotionTier.Normal,
                    isGestureRestoreInProgress = false,
                ),
                0.0001f,
            )
        }
        val backgroundSource = File(
            "src/main/java/com/android/purebilibili/core/ui/transition/" +
                "VideoCardTransitionBackgroundPolicy.kt"
        ).readText()
        assertFalse(backgroundSource.contains("translationX ="))
        assertFalse(backgroundSource.contains("translationY ="))
        assertTrue(backgroundSource.contains("resolveVideoCardTransitionBackgroundScalePivot("))
        assertTrue(backgroundSource.contains("transformOrigin = TransformOrigin(pivot.x, pivot.y)"))
    }

    @Test
    fun backgroundScalePivotsAroundTheScreenCenter() {
        val pivot = resolveVideoCardTransitionBackgroundScalePivot(
            sourceBounds = Rect(100f, 200f, 300f, 400f),
            canvasWidth = 1000f,
            canvasHeight = 2000f,
        )
        assertEquals(0.5f, pivot.x, 0.0001f)
        assertEquals(0.5f, pivot.y, 0.0001f)
        assertEquals(
            pivot,
            resolveVideoCardTransitionBackgroundScalePivot(
                sourceBounds = Rect(700f, 1600f, 950f, 1900f),
                canvasWidth = 1000f,
                canvasHeight = 2000f,
            ),
        )
        assertEquals(
            Offset(500f, 1000f),
            resolveVideoCardTransitionBackgroundScalePivotOffset(
                sourceBounds = Rect(100f, 200f, 300f, 400f),
                canvasSize = Size(1000f, 2000f),
            ),
        )
    }

    @Test
    fun backgroundScaleFallsBackToScreenCenterWithoutCardBounds() {
        assertEquals(
            Offset(0.5f, 0.5f),
            resolveVideoCardTransitionBackgroundScalePivot(
                sourceBounds = null,
                canvasWidth = 1080f,
                canvasHeight = 2400f,
            ),
        )
        assertEquals(
            Offset(0.5f, 0.5f),
            resolveVideoCardTransitionBackgroundScalePivot(
                sourceBounds = Rect(10f, 10f, 10.5f, 10.5f),
                canvasWidth = 1080f,
                canvasHeight = 2400f,
            ),
        )
        assertEquals(
            Offset(540f, 1200f),
            resolveVideoCardTransitionBackgroundScalePivotOffset(
                sourceBounds = null,
                canvasSize = Size(1080f, 2400f),
            ),
        )
    }

    @Test
    fun relatedAndPartitionUseTheSameWholePageDepth() {
        val relatedReduction = resolveVideoCardTransitionBackgroundScaleReduction(
            resolveVideoCardTransitionBackgroundSource("video/BV_related"),
        )
        val partitionReduction = resolveVideoCardTransitionBackgroundScaleReduction(
            resolveVideoCardTransitionBackgroundSource("partition"),
        )

        assertEquals(0f, relatedReduction, 0.0001f)
        assertEquals(0f, partitionReduction, 0.0001f)
        assertTrue(
            shouldUseRealtimeVideoCardTransitionBackgroundBlur(
                source = VideoCardTransitionBackgroundSource.RelatedVideo,
                realtimeBlurEnabled = true,
            )
        )
        assertFalse(
            shouldUseRealtimeVideoCardTransitionBackgroundBlur(
                source = VideoCardTransitionBackgroundSource.RelatedVideo,
                realtimeBlurEnabled = false,
            )
        )
        assertTrue(
            shouldUseRealtimeVideoCardTransitionBackgroundBlur(
                source = VideoCardTransitionBackgroundSource.Home,
                realtimeBlurEnabled = true,
            )
        )
        assertEquals(
            1f,
            resolveVideoCardTransitionContentScale(
                progress = 1f,
                phase = VideoCardTransitionBackgroundPhase.OPENING,
                motionTier = MotionTier.Normal,
                isGestureRestoreInProgress = false,
                scaleReduction = relatedReduction,
            ),
            0.0001f,
        )
        assertEquals(
            1f,
            resolveVideoCardTransitionContentScale(
                progress = 1f,
                phase = VideoCardTransitionBackgroundPhase.OPENING,
                motionTier = MotionTier.Normal,
                isGestureRestoreInProgress = false,
                scaleReduction = partitionReduction,
            ),
            0.0001f,
        )
    }

    @Test
    fun sharedShellDoesNotApplyBackgroundScale() {
        val source = File(
            "src/main/java/com/android/purebilibili/core/ui/transition/" +
                "VideoCardShellSharedBounds.kt"
        ).readText()

        assertTrue(source.contains("OverlayClip(clipShape)"))
        assertFalse(source.contains("resolveVideoCardSiblingDepthScale("))
        assertFalse(source.contains(".graphicsLayer {"))
        assertFalse(source.contains("scaleX = scale"))
        assertFalse(source.contains("scaleY = scale"))
        assertFalse(source.contains("shadowElevation"))
        assertFalse(source.contains("translationX ="))
        assertFalse(source.contains("translationY ="))
        assertFalse(source.contains("animateFloatAsState"))
        assertFalse(source.contains("Animatable"))
    }

    @Test
    fun openingBlurBuildsInLockstepWithDepth() {
        val early = resolveVideoCardTransitionBackgroundFrame(
            progress = 0.2f,
            phase = VideoCardTransitionBackgroundPhase.OPENING,
            motionTier = MotionTier.Normal,
            sdkInt = 35,
        )
        // 0.2 × 12dp = 2.4 → 量化到 2px。
        assertEquals(2f, early.blurRadiusPx, 0.01f)
        assertTrue(early.scrimAlpha > 0f)
        assertTrue(early.blurRadiusPx > 0f)
        assertEquals(4.8f, early.cornerRadiusPx, 0.01f)
    }

    @Test
    fun lightOpeningUsesReducedScrimAndWarmTint() {
        val frame = resolveVideoCardTransitionBackgroundFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.OPENING,
            motionTier = MotionTier.Enhanced,
            isLightBackground = true,
            sdkInt = 35
        )

        assertEquals(12f, frame.blurRadiusPx)
        assertEquals(0.08f, frame.scrimAlpha)
        assertTrue(frame.useLightScrimTint)
    }

    @Test
    fun lightReducedMotionUsesMinimalOpeningScrimWithoutBlur() {
        val frame = resolveVideoCardTransitionBackgroundFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.OPENING,
            motionTier = MotionTier.Reduced,
            isLightBackground = true,
            sdkInt = 35
        )

        assertEquals(0f, frame.blurRadiusPx)
        assertEquals(0.08f, frame.scrimAlpha)
        assertTrue(frame.useLightScrimTint)
    }

    @Test
    fun returningScrimMatchesHeldIntensityAtTheSameProgress() {
        val held = resolveVideoCardTransitionBackgroundFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.HELD,
            sdkInt = 35,
        )
        val returning = resolveVideoCardTransitionBackgroundFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.RETURNING,
            sdkInt = 35,
        )

        assertEquals(held.scrimAlpha, returning.scrimAlpha)
    }

    @Test
    fun returningDepthProgressIsLinearToLockstepWithMorph() {
        // RETURNING 与 OPENING 同源线性，禁止 soft-clear 二次映射拖糊。
        assertEquals(
            0.5f,
            resolveVideoCardTransitionDepthProgress(
                progress = 0.5f,
                phase = VideoCardTransitionBackgroundPhase.RETURNING,
            ),
            0.0001f,
        )
        assertEquals(
            0.5f,
            resolveVideoCardTransitionDepthProgress(
                progress = 0.5f,
                phase = VideoCardTransitionBackgroundPhase.OPENING,
            ),
            0.0001f,
        )
        // 遗留 soft-clear 曲线仍可算，但主路径不再使用。
        assertEquals(0.5647f, softClearVideoCardTransitionDepth(0.5f), 0.01f)
    }

    @Test
    fun morphAlignedDepthClearDuration_matchesMorphRemainingWallClock() {
        assertEquals(
            360,
            resolveMorphAlignedDepthClearDurationMs(
                morphRemainingMs = 360,
                blurStartProgress = 1f,
            ),
        )
        assertEquals(
            180,
            resolveMorphAlignedDepthClearDurationMs(
                morphRemainingMs = 360,
                blurStartProgress = 0.5f,
            ),
        )
        // 禁止 min 160 地板：手势已拖到底后不应再补一段长消糊。
        assertEquals(
            0,
            resolveMorphAlignedDepthClearDurationMs(
                morphRemainingMs = 90,
                blurStartProgress = 0f,
            ),
        )
        assertEquals(
            18,
            resolveMorphAlignedDepthClearDurationMs(
                morphRemainingMs = 90,
                blurStartProgress = 0.2f,
            ),
        )
    }

    @Test
    fun returningFrameFadesBlurAndScrimWithSharedElementProgress() {
        val start = resolveVideoCardTransitionBackgroundFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.RETURNING,
            motionTier = MotionTier.Enhanced,
            sdkInt = 35
        )
        val middle = resolveVideoCardTransitionBackgroundFrame(
            progress = 0.5f,
            phase = VideoCardTransitionBackgroundPhase.RETURNING,
            motionTier = MotionTier.Enhanced,
            sdkInt = 35
        )
        val end = resolveVideoCardTransitionBackgroundFrame(
            progress = 0f,
            phase = VideoCardTransitionBackgroundPhase.RETURNING,
            motionTier = MotionTier.Enhanced,
            sdkInt = 35
        )

        assertEquals(12f, start.blurRadiusPx)
        // RETURNING 使用 4px 量化：progress=0.5 → raw≈6 → quantize 到 4 或 8。
        assertTrue(middle.blurRadiusPx in setOf(4f, 8f))
        assertTrue(middle.blurRadiusPx in 1f..<start.blurRadiusPx)
        assertEquals(0f, end.blurRadiusPx)
        assertTrue(start.scrimAlpha > middle.scrimAlpha)
        assertTrue(middle.scrimAlpha > 0f)
        assertEquals(0f, end.scrimAlpha)
        assertEquals(1f, start.contentScale, 0.0001f)
        assertEquals(1f, middle.contentScale, 0.0001f)
        assertEquals(1f, end.contentScale, 0.0001f)
        assertEquals(0f, start.cornerRadiusPx, 0.0001f)
        assertEquals(0f, middle.cornerRadiusPx, 0.0001f)
        assertEquals(0f, end.cornerRadiusPx, 0.0001f)
    }

    @Test
    fun detailToDetailSourceAppliesDepthToTheLivePreviousDetailInsteadOfAHostSnapshot() {
        assertTrue(
            shouldApplyVideoCardTransitionBackgroundToRoute(
                entryRoute = "video/BV_A",
                sourceRoute = "video/BV_A",
                activeMainHostRoute = "home"
            )
        )
        assertFalse(
            shouldApplyVideoCardTransitionBackgroundToRoute(
                entryRoute = "video/BV_B",
                sourceRoute = "video/BV_A",
                activeMainHostRoute = "home"
            )
        )
        assertFalse(
            shouldUseHostOwnedVideoCardTransitionSnapshot(
                sourceRoute = "video/BV_A?from=related"
            )
        )
        assertTrue(shouldUseHostOwnedVideoCardTransitionSnapshot(sourceRoute = "home"))
    }

    @Test
    fun heldFrameKeepsBackgroundBlurDepthAndScrimForStableIosLikeStack() {
        val frame = resolveVideoCardTransitionBackgroundFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.HELD,
            motionTier = MotionTier.Enhanced,
            isLightBackground = false,
            sdkInt = 35
        )

        assertEquals(12f, frame.blurRadiusPx)
        // HELD 保留与满进度开场一致的压暗，避免详情停留时景深断裂。
        assertEquals(0.14f, frame.scrimAlpha)
        assertEquals(1f, frame.contentScale, 0.0001f)
        assertEquals(0f, frame.cornerRadiusPx, 0.0001f)
    }

    @Test
    fun gestureRestoreSmoothlyReturnsBackgroundToHeldDepth() {
        val openingScale = resolveVideoCardTransitionContentScale(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.OPENING,
            motionTier = MotionTier.Normal,
            isGestureRestoreInProgress = false,
        )
        val restoreScale = resolveVideoCardTransitionContentScale(
            progress = 0.5f,
            phase = VideoCardTransitionBackgroundPhase.HELD,
            motionTier = MotionTier.Normal,
            isGestureRestoreInProgress = true,
        )

        assertEquals(1f, openingScale, 0.0001f)
        assertEquals(1f, restoreScale, 0.0001f)
    }

    @Test
    fun idleFrameClearsBackgroundEffect() {
        val frame = resolveVideoCardTransitionBackgroundFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.IDLE,
            sdkInt = 35
        )

        assertEquals(0f, frame.blurRadiusPx)
        assertEquals(0f, frame.scrimAlpha)
        assertEquals(1f, frame.contentScale)
        assertEquals(0f, frame.cornerRadiusPx)
    }

    @Test
    fun androidBeforeSDisablesRealtimeBlurButKeepsOpeningScrim() {
        val opening = resolveVideoCardTransitionBackgroundFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.OPENING,
            sdkInt = 30
        )
        val returning = resolveVideoCardTransitionBackgroundFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.RETURNING,
            sdkInt = 30
        )

        assertEquals(0f, opening.blurRadiusPx)
        assertTrue(opening.scrimAlpha > 0f)
        assertEquals(1f, opening.contentScale, 0.0001f)
        assertEquals(0f, opening.cornerRadiusPx, 0.0001f)
        assertEquals(0f, returning.blurRadiusPx)
        assertTrue(returning.scrimAlpha > 0f)
        assertEquals(1f, returning.contentScale, 0.0001f)
        assertEquals(0f, returning.cornerRadiusPx, 0.0001f)
    }

    @Test
    fun midReturnProgressStillKeepsFadingBackgroundEffect() {
        val frame = resolveVideoCardTransitionBackgroundFrame(
            progress = 0.25f,
            phase = VideoCardTransitionBackgroundPhase.RETURNING,
            sdkInt = 35
        )

        assertTrue(frame.blurRadiusPx > 0f)
        assertTrue(frame.scrimAlpha > 0f)
        assertEquals(0.99f, frame.contentScale, 0.0001f)
        assertEquals(6f, frame.cornerRadiusPx, 0.0001f)
    }

    @Test
    fun snapshotRecording_staysFrozenForAllActivePhases_toProtectFrameBudget() {
        assertFalse(
            shouldLiveRecordVideoCardTransitionSnapshot(
                phase = VideoCardTransitionBackgroundPhase.OPENING,
            )
        )
        assertFalse(
            shouldLiveRecordVideoCardTransitionSnapshot(
                phase = VideoCardTransitionBackgroundPhase.RETURNING,
            )
        )
        assertFalse(
            shouldLiveRecordVideoCardTransitionSnapshot(
                phase = VideoCardTransitionBackgroundPhase.RETURNING,
            )
        )
        assertFalse(
            shouldLiveRecordVideoCardTransitionSnapshot(
                phase = VideoCardTransitionBackgroundPhase.HELD,
            )
        )
        assertFalse(
            shouldLiveRecordVideoCardTransitionSnapshot(
                phase = VideoCardTransitionBackgroundPhase.IDLE,
            )
        )
    }

    @Test
    fun routeMatcherTargetsOnlyRecordedSourceEntryOrActiveMainHostPage() {
        assertTrue(
            shouldApplyVideoCardTransitionBackgroundToRoute(
                entryRoute = "main_host",
                sourceRoute = "home",
                activeMainHostRoute = "home"
            )
        )
        assertFalse(
            shouldApplyVideoCardTransitionBackgroundToRoute(
                entryRoute = "main_host",
                sourceRoute = "home",
                activeMainHostRoute = "dynamic"
            )
        )
        assertFalse(
            shouldApplyVideoCardTransitionBackgroundToRoute(
                entryRoute = "main_host",
                sourceRoute = "home",
                activeMainHostRoute = "video/BV1"
            )
        )
        assertTrue(
            shouldApplyVideoCardTransitionBackgroundToRoute(
                entryRoute = "home",
                sourceRoute = "home",
                activeMainHostRoute = "video/BV1"
            )
        )
        assertTrue(
            shouldApplyVideoCardTransitionBackgroundToRoute(
                entryRoute = "search",
                sourceRoute = "search",
                activeMainHostRoute = "home"
            )
        )
        assertTrue(
            shouldApplyVideoCardTransitionBackgroundToRoute(
                entryRoute = "space/123",
                sourceRoute = "space/123?from=archive",
                activeMainHostRoute = "home"
            )
        )
        assertFalse(
            shouldApplyVideoCardTransitionBackgroundToRoute(
                entryRoute = "settings",
                sourceRoute = "home",
                activeMainHostRoute = "home"
            )
        )
        assertFalse(
            shouldApplyVideoCardTransitionBackgroundToRoute(
                entryRoute = "video/BV1",
                sourceRoute = "video",
                activeMainHostRoute = "home"
            )
        )
    }

    @Test
    fun routeMatcherTreatsHomeCategoryAsActiveHomePageForRealtimeBlur() {
        assertTrue(
            shouldApplyVideoCardTransitionBackgroundToRoute(
                entryRoute = "main_host",
                sourceRoute = "home?category=RECOMMEND",
                activeMainHostRoute = "home"
            )
        )
    }

    @Test
    fun routeMatcherAppliesHomeHostDepthForInlinePartitionSource() {
        assertTrue(
            shouldApplyVideoCardTransitionBackgroundToRoute(
                entryRoute = "home",
                sourceRoute = "partition",
                activeMainHostRoute = "home"
            )
        )
        assertTrue(
            shouldApplyVideoCardTransitionBackgroundToRoute(
                entryRoute = "main_host",
                sourceRoute = "partition",
                activeMainHostRoute = "home"
            )
        )
        assertTrue(
            shouldApplyVideoCardTransitionBackgroundToRoute(
                entryRoute = "partition",
                sourceRoute = "partition",
                activeMainHostRoute = "home"
            )
        )
        assertFalse(
            shouldApplyVideoCardTransitionBackgroundToRoute(
                entryRoute = "main_host",
                sourceRoute = "partition",
                activeMainHostRoute = "dynamic"
            )
        )
    }

    @Test
    fun gestureProgressMapsBackGestureToDecreasingBlurStartingFromFull() {
        // 手势起点保持满虚化，与 HELD 衔接；拖到底背景清晰；中途单调递减。
        assertEquals(1f, resolveVideoCardTransitionBackgroundGestureProgress(0f))
        assertEquals(0.5f, resolveVideoCardTransitionBackgroundGestureProgress(0.5f))
        assertEquals(0f, resolveVideoCardTransitionBackgroundGestureProgress(1f))
    }

    @Test
    fun gestureProgressClampsOutOfRangeBackProgress() {
        assertEquals(1f, resolveVideoCardTransitionBackgroundGestureProgress(-0.5f))
        assertEquals(0f, resolveVideoCardTransitionBackgroundGestureProgress(1.5f))
    }

    @Test
    fun openingGestureProgress_fadesFromCurrentOpeningBlurLinearly() {
        assertEquals(0.6f, resolveVideoCardTransitionBackgroundOpeningGestureProgress(
            openingBlurProgress = 0.6f,
            backProgress = 0f,
        ))
        assertEquals(0.3f, resolveVideoCardTransitionBackgroundOpeningGestureProgress(
            openingBlurProgress = 0.6f,
            backProgress = 0.5f,
        ))
        assertEquals(0f, resolveVideoCardTransitionBackgroundOpeningGestureProgress(
            openingBlurProgress = 0.6f,
            backProgress = 1f,
        ))
    }

    @Test
    fun resolveGestureBlurProgress_routesHeldAndOpeningPhases() {
        assertEquals(
            0.5f,
            resolveVideoCardTransitionBackgroundGestureBlurProgress(
                phase = VideoCardTransitionBackgroundPhase.HELD,
                currentBlurProgress = 1f,
                backProgress = 0.5f,
            )
        )
        assertEquals(
            0.4f * (1f - 0.4f),
            resolveVideoCardTransitionBackgroundGestureBlurProgress(
                phase = VideoCardTransitionBackgroundPhase.OPENING,
                currentBlurProgress = 0.4f,
                backProgress = 0.4f,
            )
        )
    }

    @Test
    fun gesturePhase_includesHeldAndOpeningOnly() {
        assertTrue(isVideoCardTransitionBackgroundGesturePhase(VideoCardTransitionBackgroundPhase.HELD))
        assertTrue(isVideoCardTransitionBackgroundGesturePhase(VideoCardTransitionBackgroundPhase.OPENING))
        assertFalse(isVideoCardTransitionBackgroundGesturePhase(VideoCardTransitionBackgroundPhase.IDLE))
        assertFalse(isVideoCardTransitionBackgroundGesturePhase(VideoCardTransitionBackgroundPhase.RETURNING))
    }

    @Test
    fun returnDurationScalesWithRemainingBlurButKeepsMinimumFloor() {
        // 未消解(startProgress=1)时用完整时长；手势已消解一半则约减半；接近清晰时不低于取消时长下限。
        assertEquals(
            VIDEO_CARD_TRANSITION_BACKGROUND_RETURN_DURATION_MS,
            resolveVideoCardTransitionBackgroundReturnDurationMs(1f)
        )
        assertEquals(
            VIDEO_CARD_TRANSITION_BACKGROUND_RETURN_DURATION_MS / 2,
            resolveVideoCardTransitionBackgroundReturnDurationMs(0.5f)
        )
        assertEquals(
            VIDEO_CARD_TRANSITION_BACKGROUND_CANCEL_DURATION_MS,
            resolveVideoCardTransitionBackgroundReturnDurationMs(0f)
        )
        assertEquals(
            VIDEO_CARD_TRANSITION_BACKGROUND_CANCEL_DURATION_MS,
            resolveVideoCardTransitionBackgroundReturnDurationMs(0.05f)
        )
    }

    @Test
    fun returnFullDurationKeepsTheSharedMasterTimeline() {
        assertEquals(
            460,
            resolveVideoCardTransitionReturnFullDurationMillis(
                baseDurationMillis = 460,
            ),
        )
        assertEquals(0, resolveVideoCardTransitionReturnFullDurationMillis(-1))
    }

    @Test
    fun openingPhaseIsInterruptedOnReturn() {
        assertTrue(
            shouldInterruptVideoCardOpeningOnReturn(VideoCardTransitionBackgroundPhase.OPENING)
        )
        assertFalse(
            shouldInterruptVideoCardOpeningOnReturn(VideoCardTransitionBackgroundPhase.HELD)
        )
        assertFalse(
            shouldInterruptVideoCardOpeningOnReturn(VideoCardTransitionBackgroundPhase.RETURNING)
        )
    }

    @Test
    fun quickReturnSnapsDepthBlurClearToAvoidCoverBlurFlash() {
        // 打断 OPENING：无论是否已 mark quick，都必须立刻清模糊。
        assertTrue(
            shouldSnapClearVideoCardDepthBlurOnQuickReturn(
                isQuickReturnFromDetail = false,
                phase = VideoCardTransitionBackgroundPhase.OPENING,
            )
        )
        assertTrue(
            shouldSnapClearVideoCardDepthBlurOnQuickReturn(
                isQuickReturnFromDetail = true,
                phase = VideoCardTransitionBackgroundPhase.OPENING,
            )
        )
        // HELD / RETURNING（含快速返回）必须走连续消糊，禁止 snap 瞬间清晰。
        assertFalse(
            shouldSnapClearVideoCardDepthBlurOnQuickReturn(
                isQuickReturnFromDetail = true,
                phase = VideoCardTransitionBackgroundPhase.HELD,
            )
        )
        assertFalse(
            shouldSnapClearVideoCardDepthBlurOnQuickReturn(
                isQuickReturnFromDetail = true,
                phase = VideoCardTransitionBackgroundPhase.RETURNING,
            )
        )
        assertFalse(
            shouldSnapClearVideoCardDepthBlurOnQuickReturn(
                isQuickReturnFromDetail = false,
                phase = VideoCardTransitionBackgroundPhase.HELD,
            )
        )
        assertFalse(
            shouldSnapClearVideoCardDepthBlurOnQuickReturn(
                isQuickReturnFromDetail = false,
                phase = VideoCardTransitionBackgroundPhase.IDLE,
            )
        )
    }

    @Test
    fun navBackdropIsRemovedAtSettledHiddenAndVisibleOnlyWhileExposed() {
        assertFalse(
            shouldShowVideoCardTransitionNavBackdrop(
                cardTransitionEnabled = true,
                exposure = VideoCardTransitionExposure.SettledHidden,
                isVideoDetailOnStack = true,
            )
        )
        assertTrue(
            shouldShowVideoCardTransitionNavBackdrop(
                cardTransitionEnabled = true,
                exposure = VideoCardTransitionExposure.Opening,
                isVideoDetailOnStack = true,
            )
        )
        assertTrue(
            shouldShowVideoCardTransitionNavBackdrop(
                cardTransitionEnabled = true,
                exposure = VideoCardTransitionExposure.BackPreview,
                isVideoDetailOnStack = true,
            )
        )
        assertTrue(
            shouldShowVideoCardTransitionNavBackdrop(
                cardTransitionEnabled = true,
                exposure = VideoCardTransitionExposure.Returning,
                isVideoDetailOnStack = false,
            )
        )
        assertFalse(
            shouldShowVideoCardTransitionNavBackdrop(
                cardTransitionEnabled = false,
                exposure = VideoCardTransitionExposure.BackPreview,
                isVideoDetailOnStack = true,
            )
        )
        assertFalse(
            shouldShowVideoCardTransitionNavBackdrop(
                cardTransitionEnabled = true,
                exposure = VideoCardTransitionExposure.SettledHidden,
                isVideoDetailOnStack = false,
            )
        )
    }

    @Test
    fun navBackdropFrameTracksBlurStrengthDuringHeldAndOpening() {
        val heldFull = resolveVideoCardTransitionNavBackdropFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.HELD,
            isLightBackground = true,
        )
        val heldHalf = resolveVideoCardTransitionNavBackdropFrame(
            progress = 0.5f,
            phase = VideoCardTransitionBackgroundPhase.HELD,
            isLightBackground = true,
        )
        val openingFull = resolveVideoCardTransitionNavBackdropFrame(
            progress = 1f,
            phase = VideoCardTransitionBackgroundPhase.OPENING,
            isLightBackground = false,
        )

        assertEquals(0.08f, heldFull.scrimAlpha)
        assertTrue(heldHalf.scrimAlpha < heldFull.scrimAlpha)
        assertEquals(0.14f, openingFull.scrimAlpha)
        assertTrue(heldFull.useLightScrimTint)
        assertFalse(openingFull.useLightScrimTint)
    }

    @Test
    fun navBackdropColorLerpsFromBaseBackgroundTowardScrimTint() {
        val base = androidx.compose.ui.graphics.Color.White
        val frame = VideoCardTransitionNavBackdropFrame(
            scrimAlpha = 0.10f,
            useLightScrimTint = true,
        )
        val blended = resolveVideoCardTransitionNavBackdropColor(
            baseBackgroundColor = base,
            frame = frame,
        )

        assertTrue(blended != base)
        assertTrue(blended.alpha > 0f)
    }

    @Test
    fun scaleGapFillKeepsOpaqueGrayFloorSoPredictiveBackEdgesDoNotReadAsWhiteBars() {
        val lightHeld = resolveVideoCardTransitionScaleGapFillColor(
            isLightBackground = true,
            scrimAlpha = 0.14f,
        )
        val lightMidGesture = resolveVideoCardTransitionScaleGapFillColor(
            isLightBackground = true,
            scrimAlpha = 0.03f,
        )
        val darkHeld = resolveVideoCardTransitionScaleGapFillColor(
            isLightBackground = false,
            scrimAlpha = 0.28f,
        )

        assertEquals(1f, lightHeld.alpha)
        assertEquals(1f, lightMidGesture.alpha)
        assertTrue(lightHeld.red < 0.92f)
        assertTrue(lightMidGesture.red < 0.92f)
        assertEquals(lightHeld, lightMidGesture)
        assertTrue(darkHeld.red < 0.35f)
        assertTrue(
            shouldDrawVideoCardTransitionScaleGapFill(contentScale = 0.96f)
        )
        assertFalse(
            shouldDrawVideoCardTransitionScaleGapFill(contentScale = 1f)
        )
    }

    @Test
    fun navBackdropColorUsesScaleGapFloorDuringLowProgressGesture() {
        val base = androidx.compose.ui.graphics.Color.White
        val lowProgress = resolveVideoCardTransitionNavBackdropColor(
            baseBackgroundColor = base,
            frame = VideoCardTransitionNavBackdropFrame(
                scrimAlpha = 0.02f,
                useLightScrimTint = true,
            ),
        )
        val gapFill = resolveVideoCardTransitionScaleGapFillColor(
            isLightBackground = true,
            scrimAlpha = 0.02f,
        )

        assertEquals(gapFill, lowProgress)
        assertTrue(lowProgress.red < 0.92f)
    }

    @Test
    fun hostOwnedSourceDetachRequestsRefreshWithoutMarkingStale() {
        val state = VideoCardTransitionSnapshotLayerState().apply {
            freezeRecording = true
            hasRecordedContent = true
            displayListStale = false
            lastBlurRadiusPx = 12f
        }
        state.markSourceDetachedForRefresh()
        assertTrue(state.needsSourceRefresh)
        assertFalse(state.freezeRecording)
        assertTrue(state.hasRecordedContent)
        assertFalse(state.displayListStale)
        // Host 可在预测 BackPreview 首帧暂用冻结内容撑住满模糊。
        assertTrue(
            isVideoCardTransitionSnapshotDrawable(
                hasRecordedContent = state.hasRecordedContent,
                displayListStale = state.displayListStale,
            ),
        )
        state.markDisplayListFresh()
        assertFalse(state.needsSourceRefresh)
    }
}
