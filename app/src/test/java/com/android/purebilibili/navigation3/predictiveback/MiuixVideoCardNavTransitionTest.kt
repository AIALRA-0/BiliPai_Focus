package com.android.purebilibili.navigation3.predictiveback

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import com.android.purebilibili.navigation3.resolveRelatedReturnSourceRestoreDecision
import com.android.purebilibili.core.ui.transition.VideoCardTransitionExposure
import com.android.purebilibili.core.ui.transition.VideoCardSourceLayout
import java.io.File
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.android.purebilibili.core.ui.transition.resolveVideoHeroMotionSpec
import com.android.purebilibili.core.ui.transition.resolveVideoHeroLandingScale
import com.android.purebilibili.core.ui.transition.VideoCardTransitionSettleState
import top.yukonga.miuix.kmp.nav.transition.NavSettleSpec
import top.yukonga.miuix.kmp.nav.transition.NavTransitionScope
import top.yukonga.miuix.kmp.nav.transition.NavSettle
import top.yukonga.miuix.kmp.nav.transition.NavSettlePhase
import top.yukonga.miuix.kmp.nav.transition.NavGesture
import top.yukonga.miuix.kmp.nav.transition.NavSwipeEdge
import top.yukonga.miuix.kmp.nav.transition.NavRole
import top.yukonga.miuix.kmp.nav.runtime.NavChange
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

class MiuixVideoCardNavTransitionTest {
    @Test
    fun heroNavMotionUsesDurationTokensAndVelocityCapableReleaseSpecs() {
        val spec = resolveVideoHeroMotionSpec(360)
        val entering = resolveVideoHeroNavMotion(spec, false)
        val returning = resolveVideoHeroNavMotion(spec, true)
        assertEquals(360, (entering.programmatic as NavSettleSpec.Tween).durationMillis)
        assertEquals(299, (returning.programmatic as NavSettleSpec.Tween).durationMillis)
        assertEquals(spec.commitStiffness, (returning.commit as NavSettleSpec.Spring).stiffness)
        assertEquals(spec.cancelStiffness, (returning.cancel as NavSettleSpec.Spring).stiffness)
        assertEquals(1f, (returning.commit as NavSettleSpec.Spring).dampingRatio)
    }

    @Test
    fun inverseScaleIsUniformAtLandAndTracksClipDuringMorph() {
        val sourceX = 0.46f
        val sourceY = 0.25f
        val landed = resolveMiuixVideoCardInverseScaleForDepth(sourceX, sourceY, 0f)
        assertEquals(1f / sourceX, landed.scaleX, 0.0001f)
        assertEquals(1f / sourceX, landed.scaleY, 0.0001f)

        val mid = resolveMiuixVideoCardInverseScaleForDepth(sourceX, sourceY, 0.18f)
        assertEquals(1f / sourceX, mid.scaleX, 0.0001f)
        assertTrue(abs(mid.scaleY - mid.scaleX) > 0.01f)

        val outerY = resolveMiuixVideoCardOuterScale(sourceY, 0.18f, 1f)
        val compensation = resolveMiuixVideoCardContentCompensation(
            outerScaleX = resolveMiuixVideoCardOuterScale(sourceX, 0.18f, 1f),
            outerScaleY = outerY,
            contentScale = MiuixVideoCardContentScale.FillWidthTop,
        )
        // After FillWidthTop + inverse + outer Y, chrome height tracks the current clip.
        assertEquals(
            outerY / sourceY,
            mid.scaleY * compensation.scaleY * outerY,
            0.0001f,
        )
    }

    @Test
    fun landingCompressionIsBoundedRelativeToFinalSizeOnBothAxes() {
        for (sourceScale in listOf(.05f, .2f, .8f, 1f)) {
            for (i in 0..1000) {
                val depth = i / 1000f
                val scale = resolveMiuixVideoCardOuterScale(sourceScale, depth,
                    resolveVideoHeroLandingScale(depth, true))
                val baseline = resolveMiuixVideoCardOuterScale(sourceScale, depth, 1f)
                assertTrue(abs(scale - baseline) <= .015f * sourceScale)
            }
            assertEquals(sourceScale, resolveMiuixVideoCardOuterScale(sourceScale, 0f, 1f))
            assertEquals(1f, resolveMiuixVideoCardOuterScale(sourceScale, 1f, 1f))
        }
    }

    @Test
    fun ordinaryRelatedSlideKeepsSourceUntilItsActualExitCompletes() {
        val scope = object : NavTransitionScope {
            override var relativeDepth = -.4f
            override var role = NavRole.Outgoing
            override val change = NavChange.Pop
            override val layoutSize = IntSize(1080, 2400)
            override val layoutDirection = LayoutDirection.Ltr
            override val density = Density(3f)
            override val gesture: NavGesture? = null
            override val settle: NavSettle? = null
        }
        val progress = MiuixVideoCardTransitionProgress()
        val fallback = NavTransitions.MiuixDefault
        val observed = progress.observe(fallback)
        with(observed) { Modifier.transformEntry(scope) }
        assertEquals(fallback.motion, observed.motion)
        assertEquals(fallback.opaqueDepth, observed.opaqueDepth)
        assertEquals(VideoCardTransitionSettleState.AutoReturn, progress.settleStateOrNull())
        val moving = resolveRelatedReturnSourceRestoreDecision(
            restorePending = true,
            transitionObserved = false,
            transitionAnimated = true,
            exposure = VideoCardTransitionExposure.Returning,
        )
        assertEquals(false, moving.shouldRestore)
        scope.relativeDepth = -1f
        assertEquals(VideoCardTransitionSettleState.Idle, progress.settleStateOrNull())
        val finished = resolveRelatedReturnSourceRestoreDecision(
            restorePending = true,
            transitionObserved = moving.transitionObserved,
            transitionAnimated = true,
            exposure = VideoCardTransitionExposure.Idle,
        )
        assertTrue(finished.shouldRestore)
    }

    @Test
    fun retainedGestureMetadataDoesNotMisclassifyCommitOrCancelAsSeek() {
        val scope = object : NavTransitionScope {
            override var relativeDepth = -.4f
            override var role = NavRole.Top
            override val change = NavChange.Pop
            override val layoutSize = IntSize(1080, 2400)
            override val layoutDirection = LayoutDirection.Ltr
            override val density = Density(3f)
            override val gesture = NavGesture(.4f, NavSwipeEdge.Left, 500f)
            override var settle: NavSettle? = null
        }
        val progress = MiuixVideoCardTransitionProgress()
        progress.bind(scope)
        assertTrue(progress.isGestureInProgress())
        assertEquals(.6f, progress.depthOrNull())
        scope.settle = object : NavSettle {
            override val phase = NavSettlePhase.Cancel
            override val releaseVelocity = 0f
            override val elapsedMillis = 0f
        }
        assertEquals(false, progress.isGestureInProgress())
        assertEquals(VideoCardTransitionSettleState.CancelRestore, progress.settleStateOrNull())
        scope.settle = object : NavSettle {
            override val phase = NavSettlePhase.Commit
            override val releaseVelocity = 2f
            override val elapsedMillis = 0f
        }
        scope.role = NavRole.Outgoing
        assertEquals(VideoCardTransitionSettleState.AutoReturn, progress.settleStateOrNull())
        scope.relativeDepth = -1f
        assertEquals(VideoCardTransitionSettleState.Idle, progress.settleStateOrNull())

        // The same live presentation subsequently drives a settings navigation. A released
        // video scope must not turn that unrelated transition back into video depth/blur.
        progress.clear()
        scope.relativeDepth = -.2f
        scope.role = NavRole.Incoming
        assertEquals(null, progress.depthOrNull())
        assertEquals(null, progress.settleStateOrNull())
        assertEquals(null, progress.gestureBackProgress())
        assertEquals(false, progress.isGestureInProgress())
        assertEquals(0f, progress.depthOr(0f))

        progress.bind(scope)
        assertEquals(.8f, progress.depthOrNull())
    }
    @Test
    fun transitionKeepsOneOpaqueFlyingCardWithoutStationaryRevealMask() {
        val source = File(
            "src/main/java/com/android/purebilibili/navigation3/predictiveback/MiuixVideoCardNavTransition.kt"
        ).readText()
        val transform = source.substringAfter("override fun Modifier.transformEntry")

        assertEquals(true, transform.contains("alpha = 1f"))
        assertEquals(false, transform.contains("visibleHeightFraction"))
        assertEquals(false, transform.contains("outgoingClipFraction"))
        assertEquals(true, transform.contains("resolveMiuixVideoCardGestureVisualOrigin("))
        assertEquals(true, transform.contains("cameraDistance = transform.cameraDistance"))
        assertEquals(true, transform.contains("shadowElevation = MIUIX_VIDEO_CARD_GESTURE_SHADOW_DP.dp.toPx() * poseWeight"))
        assertEquals(true, transform.contains("floatingCornerPx = MIUIX_VIDEO_CARD_FLOATING_CORNER_DP.dp.toPx()"))
        assertEquals(true, source.contains("56.dp.toPx()"))
    }

    @Test
    fun returnDepthClearsBlurInsteadOfReversingIt() {
        assertEquals(
            1f,
            resolveMiuixVideoCardDepthProgress(relativeDepth = 0f),
            absoluteTolerance = 0.0001f,
        )
        assertEquals(
            0.5f,
            resolveMiuixVideoCardDepthProgress(relativeDepth = -0.5f),
            absoluteTolerance = 0.0001f,
        )
        assertEquals(
            0f,
            resolveMiuixVideoCardDepthProgress(relativeDepth = -1f),
            absoluteTolerance = 0.0001f,
        )
    }

    @Test
    fun cardClipKeepsPhysicalCornerRadiusAcrossNonUniformScale() {
        val radii = resolveMiuixVideoCardClipRadii(
            sourceCornerPx = 12f,
            outerScaleX = 0.5f,
            outerScaleY = 0.25f,
        )

        assertEquals(12f, radii.radiusX * 0.5f, absoluteTolerance = 0.0001f)
        assertEquals(12f, radii.radiusY * 0.25f, absoluteTolerance = 0.0001f)
    }

    @Test
    fun flyingCardCornersBloomMidPeelThenLandOnTheSourceRadius() {
        assertEquals(
            0f,
            resolveMiuixVideoCardGestureCornerPx(
                sourceCornerPx = 16f,
                morphProgress = 1f,
                floatingCornerPx = 28f,
            ),
            0.0001f,
        )
        assertEquals(
            32f,
            resolveMiuixVideoCardGestureCornerPx(
                sourceCornerPx = 16f,
                morphProgress = 1f,
                floatingCornerPx = 32f,
                fullscreenCornerPx = 32f,
            ),
            0.0001f,
        )
        assertEquals(
            16f,
            resolveMiuixVideoCardGestureCornerPx(
                sourceCornerPx = 16f,
                morphProgress = 0f,
                floatingCornerPx = 28f,
            ),
            0.0001f,
        )
        assertEquals(
            28f,
            resolveMiuixVideoCardGestureCornerPx(
                sourceCornerPx = 16f,
                morphProgress = 0.5f,
                floatingCornerPx = 28f,
            ),
            0.01f,
        )
        val mid = resolveMiuixVideoCardClipRadii(
            sourceCornerPx = 16f,
            outerScaleX = 0.5f,
            outerScaleY = 0.4f,
            morphProgress = 0.5f,
            floatingCornerPx = 28f,
        )
        assertEquals(28f, mid.radiusX * 0.5f, absoluteTolerance = 0.01f)
        assertEquals(28f, mid.radiusY * 0.4f, absoluteTolerance = 0.01f)
        val landed = resolveMiuixVideoCardGestureTransform(
            morphProgress = 0f,
            touchY = 1200f,
            initialTouchY = 1200f,
            widthPx = 1080f,
            heightPx = 2400f,
            isLeftEdge = true,
            maxVerticalTravelPx = 72f,
        )
        assertEquals(1f, landed.liftScale, 0.0001f)
        assertEquals(0f, landed.shadowElevationDp, 0.0001f)
        assertEquals(MIUIX_VIDEO_CARD_GESTURE_CAMERA_DISTANCE_DP, landed.cameraDistance, 0.0001f)
    }

    @Test
    fun fillWidthTopPreservesAspectRatioAndTopAlignment() {
        val compensation = resolveMiuixVideoCardContentCompensation(
            outerScaleX = 0.5f,
            outerScaleY = 0.25f,
            contentScale = MiuixVideoCardContentScale.FillWidthTop,
        )

        assertEquals(0.5f, 0.5f * compensation.scaleX, absoluteTolerance = 0.0001f)
        assertEquals(0.5f, 0.25f * compensation.scaleY, absoluteTolerance = 0.0001f)
        assertEquals(TransformOrigin(0.5f, 0f), compensation.transformOrigin)
    }

    @Test
    fun cropCenterPreservesAspectRatioUsingCoverScale() {
        val compensation = resolveMiuixVideoCardContentCompensation(
            outerScaleX = 0.35f,
            outerScaleY = 0.6f,
            contentScale = MiuixVideoCardContentScale.CropCenter,
        )

        assertEquals(0.6f, 0.35f * compensation.scaleX, absoluteTolerance = 0.0001f)
        assertEquals(0.6f, 0.6f * compensation.scaleY, absoluteTolerance = 0.0001f)
        assertEquals(TransformOrigin.Center, compensation.transformOrigin)
    }

    @Test
    fun sideBySideAndStackedUseFillWidthTopForLandingAnchors() {
        assertEquals(
            MiuixVideoCardContentScale.FillWidthTop,
            resolveMiuixVideoCardContentScaleForSourceLayout(
                sourceLayout = VideoCardSourceLayout.SIDE_BY_SIDE,
            ),
        )
        assertEquals(
            MiuixVideoCardContentScale.FillWidthTop,
            resolveMiuixVideoCardContentScaleForSourceLayout(
                sourceLayout = VideoCardSourceLayout.STACKED,
            ),
        )
        assertEquals(
            MiuixVideoCardContentScale.CropCenter,
            resolveMiuixVideoCardContentScaleForSourceLayout(
                sourceLayout = VideoCardSourceLayout.STACKED,
                fullscreen = true,
            ),
        )
    }

    @Test
    fun gestureFollowPeaksMidFlightAndKeepsBothLandingEndpointsExact() {
        val fullscreen = resolveMiuixVideoCardGestureTransform(
            morphProgress = 1f,
            touchY = 900f,
            initialTouchY = 500f,
            widthPx = 1080f,
            heightPx = 2400f,
            isLeftEdge = true,
            maxVerticalTravelPx = 72f,
        )
        val midFlight = resolveMiuixVideoCardGestureTransform(
            morphProgress = 0.5f,
            touchY = 900f,
            initialTouchY = 500f,
            widthPx = 1080f,
            heightPx = 2400f,
            isLeftEdge = true,
            maxVerticalTravelPx = 72f,
        )
        val landed = resolveMiuixVideoCardGestureTransform(
            morphProgress = 0f,
            touchY = 900f,
            initialTouchY = 500f,
            widthPx = 1080f,
            heightPx = 2400f,
            isLeftEdge = true,
            maxVerticalTravelPx = 72f,
        )

        assertEquals(0f, fullscreen.translationX)
        assertEquals(0f, fullscreen.translationY)
        assertEquals(0f, fullscreen.rotationZ)
        assertEquals(0f, landed.translationX)
        assertEquals(0f, landed.translationY)
        assertEquals(0f, landed.rotationZ)
        assertEquals(true, abs(midFlight.translationX) > 0f)
        assertEquals(true, abs(midFlight.translationY) > 0f)
        assertEquals(true, abs(midFlight.rotationZ) > 0f)
    }

    @Test
    fun gestureFollowTiltsFromTheGrabPointEvenWithoutVerticalMove() {
        val centered = resolveMiuixVideoCardGestureTransform(
            morphProgress = 0.5f,
            touchY = 1200f,
            initialTouchY = 1200f,
            widthPx = 1080f,
            heightPx = 2400f,
            isLeftEdge = true,
            maxVerticalTravelPx = 72f,
        )
        assertEquals(0f, centered.translationY, 0.001f)
        assertEquals(
            MIUIX_VIDEO_CARD_GESTURE_PEEL_ROTATION_DEGREES,
            centered.rotationZ,
            0.01f,
        )
        assertEquals(1f - MIUIX_VIDEO_CARD_GESTURE_LIFT_SCALE, centered.liftScale, 0.0001f)
        assertEquals(12f, centered.shadowElevationDp, 0.01f)
        assertTrue(centered.cameraDistance < MIUIX_VIDEO_CARD_GESTURE_CAMERA_DISTANCE_DP)
        assertTrue(abs(centered.translationX) > 40f)
        assertEquals(0f, resolveMiuixVideoCardGesturePoseWeight(0f), 0.0001f)
        assertEquals(0f, resolveMiuixVideoCardGesturePoseWeight(1f), 0.0001f)
        assertEquals(1f, resolveMiuixVideoCardGesturePoseWeight(0.5f), 0.001f)
    }

    @Test
    fun gestureVisualOriginTracksTheFlyingCardInsteadOfTheFullscreenCenter() {
        val source = androidx.compose.ui.geometry.Rect(80f, 400f, 500f, 900f)
        val origin = resolveMiuixVideoCardGestureVisualOrigin(
            sourceBounds = source,
            layoutWidth = 1080f,
            layoutHeight = 2400f,
            morph = 0f,
            localOrigin = TransformOrigin(0.14f, 0.6f),
        )
        assertEquals(
            (source.left + source.width * 0.14f) / 1080f,
            origin.pivotFractionX,
            0.0001f,
        )
        assertEquals(
            (source.top + source.height * 0.6f) / 2400f,
            origin.pivotFractionY,
            0.0001f,
        )
        val fullscreen = resolveMiuixVideoCardGestureVisualOrigin(
            sourceBounds = source,
            layoutWidth = 1080f,
            layoutHeight = 2400f,
            morph = 1f,
            localOrigin = TransformOrigin(0.14f, 0.6f),
        )
        assertEquals(0.14f, fullscreen.pivotFractionX, 0.0001f)
        assertEquals(0.6f, fullscreen.pivotFractionY, 0.0001f)
    }

    @Test
    fun gestureFollowMirrorsAcrossScreenEdgesWithoutChangingVerticalLanding() {
        val left = resolveMiuixVideoCardGestureTransform(
            morphProgress = 0.5f,
            touchY = 800f,
            initialTouchY = 500f,
            widthPx = 1080f,
            heightPx = 2400f,
            isLeftEdge = true,
            maxVerticalTravelPx = 72f,
        )
        val right = resolveMiuixVideoCardGestureTransform(
            morphProgress = 0.5f,
            touchY = 800f,
            initialTouchY = 500f,
            widthPx = 1080f,
            heightPx = 2400f,
            isLeftEdge = false,
            maxVerticalTravelPx = 72f,
        )

        assertEquals(-left.translationX, right.translationX, absoluteTolerance = 0.001f)
        assertEquals(left.translationY, right.translationY, absoluteTolerance = 0.001f)
        assertEquals(-left.rotationZ, right.rotationZ, absoluteTolerance = 0.001f)
    }
}
