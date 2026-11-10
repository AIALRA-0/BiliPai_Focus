package com.android.purebilibili.navigation3.predictiveback

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventTransitionState
import com.android.purebilibili.core.ui.transition.resolveVideoHeroMotionSpec
import com.android.purebilibili.core.ui.transition.VideoCardSourceLayout
import com.android.purebilibili.core.ui.transition.VideoCardTransitionBackgroundPhase
import com.android.purebilibili.feature.video.screen.resolveVideoDetailReturnMediaFrame
import com.android.purebilibili.feature.video.screen.resolveVideoDetailFlyingSourceChromeAlpha
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import top.yukonga.miuix.kmp.nav.runtime.NavChange
import top.yukonga.miuix.kmp.nav.transition.NavGesture
import top.yukonga.miuix.kmp.nav.transition.NavRole
import top.yukonga.miuix.kmp.nav.transition.NavSettle
import top.yukonga.miuix.kmp.nav.transition.NavSettlePhase
import top.yukonga.miuix.kmp.nav.transition.NavSettleSpec
import top.yukonga.miuix.kmp.nav.transition.NavSwipeEdge
import top.yukonga.miuix.kmp.nav.transition.NavTransitionScope

class VideoCardGestureFollowStateTest {
    private val bounds = Rect(70f, 400f, 490f, 900f)
    private val motion = resolveVideoHeroMotionSpec(360)

    private class Scope : NavTransitionScope {
        override var relativeDepth by mutableFloatStateOf(0f)
        override var role by mutableStateOf(NavRole.Top)
        override val change = NavChange.Pop
        override val layoutSize = IntSize(1080, 2400)
        override val layoutDirection = LayoutDirection.Ltr
        override val density = Density(3f)
        override var gesture: NavGesture? by mutableStateOf(NavGesture(0f, NavSwipeEdge.Left, 700f))
        override var settle: NavSettle? by mutableStateOf(null)
    }

    private class Settle(override val phase: NavSettlePhase) : NavSettle {
        override val releaseVelocity = 0f
        override var elapsedMillis = 0f
    }

    private fun event(x: Float, y: Float, edge: Int = NavigationEvent.EDGE_LEFT) =
        NavigationEventTransitionState.InProgress(
            NavigationEvent(swipeEdge = edge, touchX = x, touchY = y),
            NavigationEventTransitionState.TRANSITIONING_BACK,
        )

    private fun pose(progress: MiuixVideoCardTransitionProgress, scope: Scope) =
        progress.followPose(scope, bounds, 1080f, 2400f, 30f, motion)!!

    private fun assertOffset(expected: Offset, actual: Offset) {
        assertEquals(expected.x, actual.x, .002f)
        assertEquals(expected.y, actual.y, .002f)
    }

    @Test
    fun grabbingAutomaticFlightKeepsEveryVisiblePoseChannelWithAndWithoutTranslation() {
        for (translationEnabled in listOf(false, true)) {
            for (morph in listOf(.25f, .5f, .75f)) {
                val scope = Scope().apply {
                    relativeDepth = morph - 1f
                    role = NavRole.Incoming
                    gesture = null
                }
                val progress = MiuixVideoCardTransitionProgress(
                    gestureTranslationEnabled = translationEnabled, gesturePoseEnabled = true,
                )
                progress.bind(scope)
                assertEquals(null, progress.followPose(scope, bounds, 1080f, 2400f, 30f, motion))
                val automatic = resolveMiuixVideoCardClickTransform(morph, 1080f, 2400f, bounds)
                val origin = resolveMiuixVideoCardGestureVisualOrigin(
                    bounds, 1080f, 2400f, morph, automatic.transformOrigin,
                )
                progress.recordAutomaticPose(automatic, origin)
                val expected = VideoCardFollowPose(
                    translation = Offset(automatic.translationX, automatic.translationY),
                    rotationZ = automatic.rotationZ, transformOrigin = origin,
                    rotationX = automatic.rotationX, rotationY = automatic.rotationY,
                    liftScale = automatic.liftScale, cameraDistance = automatic.cameraDistance,
                )
                scope.gesture = NavGesture(0f, NavSwipeEdge.Right, 1800f)
                // 导航先知道已开始抓取，坐标观察器尚未收到事件时也保持投影。
                assertEquals(expected, pose(progress, scope))
                progress.gestureFollow.onTransitionState(event(1080f, 1800f, NavigationEvent.EDGE_RIGHT))
                assertEquals(morph, progress.depthOrNull()!!, .0001f)
                assertEquals(expected, pose(progress, scope))
                // 下一帧才开始移动；锚点不被后续输入反复认领。
                progress.gestureFollow.onTransitionState(event(980f, 1850f, NavigationEvent.EDGE_RIGHT))
                val moved = pose(progress, scope)
                assertOffset(if (translationEnabled) Offset(-100f, 50f) else Offset.Zero, moved.translation)
                assertEquals(origin, moved.transformOrigin)
                progress.clear()
                assertEquals(VideoCardFollowPose().cameraDistance, pose(progress, scope).cameraDistance)
            }
        }
    }

    @Test
    fun regrabDoesNotClampAnExistingReleaseImpulseOrAutomaticPerspective() {
        val anchor = VideoCardFollowPose(
            translation = Offset(70f, 90f), rotationZ = 5.8f,
            transformOrigin = TransformOrigin(.2f, .7f),
            rotationX = -1.8f, rotationY = 1.7f, liftScale = .95f, cameraDistance = 5.5f,
        )
        val initial = Offset(1080f, 1800f)
        val actual = resolveVideoCardFollowPose(
            VideoCardGestureSample(2, initial, initial), bounds, 1080f, 2400f,
            morph = .5f, anchorMorph = .5f, arcAmplitudePx = 30f,
            anchorPose = anchor, gestureProgress = 0f,
        )
        assertEquals(anchor, actual)
    }

    @Test
    fun screenCoordinatesBecomePaneLocalAndReleaseRetainsLastSample() {
        val state = VideoCardGestureFollowState()
        state.hostOriginOnScreen = Offset(800f, 80f)
        state.onTransitionState(event(810f, 780f))
        state.onTransitionState(event(1120f, 1030f))
        assertOffset(Offset(10f, 700f), state.localSample()!!.initialTouch)
        assertOffset(Offset(320f, 950f), state.localSample()!!.touch)
        val release = state.sample!!
        state.onTransitionState(NavigationEventTransitionState.Idle)
        assertEquals(release, state.sample)
        state.onTransitionState(event(1880f, 900f, NavigationEvent.EDGE_RIGHT))
        assertTrue(state.sample!!.sequence > release.sequence)
        assertEquals(state.sample!!.initialTouch, state.sample!!.touch)
    }

    @Test
    fun rotationAndTranslationChannelsSupportAllFourCombinations() {
        val raw = VideoCardFollowPose(
            translation = Offset(300f, -240f), rotationZ = 2.4f,
            rotationX = -.8f, rotationY = 1.4f, liftScale = .98f, cameraDistance = 7f,
        )
        for (poseEnabled in listOf(false, true)) {
            for (translationEnabled in listOf(false, true)) {
                val result = resolveVideoCardGestureChannels(raw, translationEnabled, poseEnabled)
                assertEquals(if (translationEnabled) raw.translation else Offset.Zero, result.translation)
                assertEquals(if (poseEnabled) raw.rotationZ else 0f, result.rotationZ)
                assertEquals(if (poseEnabled) raw.rotationX else 0f, result.rotationX)
                assertEquals(if (poseEnabled) raw.rotationY else 0f, result.rotationY)
                assertEquals(if (poseEnabled) raw.liftScale else 1f, result.liftScale)
                assertEquals(if (poseEnabled) raw.cameraDistance else 8f, result.cameraDistance)
            }
        }
    }

    @Test
    fun grabPointFollowsBothAxesOneToOneAtEveryPreviewSize() {
        for (initial in listOf(Offset(0f, 700f), Offset(1080f, 1700f))) {
            for (delta in listOf(Offset(310f, 270f), Offset(-280f, -340f), Offset(0f, 450f))) {
                for (morph in listOf(1f, .85f, .65f)) {
                    val sample = VideoCardGestureSample(1, initial, initial + delta)
                    val pose = resolveVideoCardFollowPose(sample, bounds, 1080f, 2400f, morph, 1f, 30f)
                    val path = resolveVideoCardPathOffset(bounds, 1080f, 2400f, morph, 30f)
                    val grabBeforeOuter = path + Offset(
                        resolveMiuixVideoCardOuterScale(bounds.width / 1080f, morph, 1f) * initial.x,
                        resolveMiuixVideoCardOuterScale(bounds.height / 2400f, morph, 1f) * initial.y,
                    )
                    assertOffset(sample.touch, grabBeforeOuter + pose.translation)
                    assertOffset(grabBeforeOuter, Offset(
                        pose.transformOrigin.pivotFractionX * 1080f,
                        pose.transformOrigin.pivotFractionY * 2400f,
                    ))
                }
            }
        }
    }

    @Test
    fun retainedViewportKeepsGrabPointAttachedDuringHeightContraction() {
        val initial = Offset(1000f, 1800f)
        val sample = VideoCardGestureSample(1, initial, initial + Offset(-200f, 350f))
        for (morph in listOf(.95f, .7f, .65f, .4f, .1f)) {
            val pose = resolveVideoCardFollowPose(
                sample, bounds, 1080f, 2400f, morph, 1f, 30f,
                preserveViewport = true,
            )
            val path = resolveVideoCardPathOffset(bounds, 1080f, 2400f, morph, 30f)
            val grab = path + Offset(
                resolveMiuixVideoCardOuterScale(bounds.width / 1080f, morph, 1f) * initial.x,
                resolveMiuixVideoCardOuterScaleY(bounds.width / 1080f, bounds.height / 2400f, morph, 1f) * initial.y,
            )
            assertOffset(sample.touch, grab + pose.translation)
        }
    }

    @Test
    fun onModeKeepsFloatingPreviewAndOffModeCanLandWhileHeld() {
        for (enabled in listOf(true, false)) {
            val progress = MiuixVideoCardTransitionProgress(gestureTranslationEnabled = enabled, gesturePoseEnabled = enabled)
            val scope = Scope()
            progress.bind(scope)
            progress.gestureFollow.onTransitionState(event(0f, 700f))
            progress.depthOrNull()
            scope.relativeDepth = -.999f
            scope.gesture = NavGesture(.999f, NavSwipeEdge.Left, 1000f, 700f)
            progress.gestureFollow.onTransitionState(event(600f, 1000f))
            val depth = progress.depthOrNull()!!
            if (enabled) {
                assertTrue(depth >= .01f)
                assertTrue(pose(progress, scope).translation != Offset.Zero)
                assertTrue(pose(progress, scope).rotationY != 0f)
                assertTrue(pose(progress, scope).liftScale < 1f)
            } else {
                assertEquals(.001f, depth, .002f)
                assertEquals(null, progress.followPose(scope, bounds, 1080f, 2400f, 30f, motion))
            }
        }
    }

    @Test
    fun commitAndCancelStartAtReleasePoseAndEndWithZeroCorrection() {
        for (phase in listOf(NavSettlePhase.Commit, NavSettlePhase.Cancel)) {
            val scope = Scope()
            val progress = MiuixVideoCardTransitionProgress(gestureTranslationEnabled = true, gesturePoseEnabled = true)
            progress.bind(scope)
            progress.gestureFollow.onTransitionState(event(0f, 700f))
            pose(progress, scope)
            scope.relativeDepth = -.6f
            scope.gesture = NavGesture(.6f, NavSwipeEdge.Left, 1000f, 700f)
            progress.gestureFollow.onTransitionState(event(400f, 1000f))
            val releaseDepth = progress.depthOrNull()!!
            val releasePose = pose(progress, scope)
            progress.gestureFollow.onTransitionState(NavigationEventTransitionState.Idle)
            scope.settle = Settle(phase)
            scope.role = if (phase == NavSettlePhase.Commit) NavRole.Outgoing else NavRole.Incoming
            assertEquals(releaseDepth, progress.depthOrNull())
            assertEquals(releasePose, pose(progress, scope))
            scope.relativeDepth = if (phase == NavSettlePhase.Commit) -.8f else -.3f
            val mid = pose(progress, scope)
            assertOffset(releasePose.translation * .5f, mid.translation)
            scope.relativeDepth = if (phase == NavSettlePhase.Commit) -1f else 0f
            val end = progress.followPose(scope, bounds, 1080f, 2400f, 30f, motion)
            assertOffset(Offset.Zero, end?.translation ?: Offset.Zero)
            assertEquals(0f, end?.rotationX ?: 0f)
            assertEquals(0f, end?.rotationY ?: 0f)
            assertEquals(1f, end?.liftScale ?: 1f)
        }
    }

    @Test
    fun nearCompleteFollowCommitKeepsReleaseDepthAndPose() {
        val scope = Scope()
        val progress = MiuixVideoCardTransitionProgress(gestureTranslationEnabled = true, gesturePoseEnabled = true)
        progress.bind(scope)
        progress.gestureFollow.onTransitionState(event(0f, 700f))
        pose(progress, scope)
        scope.relativeDepth = -.99f
        scope.gesture = NavGesture(.99f, NavSwipeEdge.Left, 1000f, 700f)
        progress.gestureFollow.onTransitionState(event(600f, 1000f))
        val releaseDepth = progress.depthOrNull()!!
        val releasePose = pose(progress, scope)
        scope.settle = Settle(NavSettlePhase.Commit)
        scope.role = NavRole.Outgoing
        assertEquals(releaseDepth, progress.depthOrNull()!!, .0001f)
        assertEquals(releasePose, pose(progress, scope))
        scope.relativeDepth = -1f
        assertOffset(Offset.Zero, pose(progress, scope).translation)
    }

    @Test
    fun cancellingVerticalOnlyMoveStillAnimatesWithoutDepthTravel() {
        val scope = Scope()
        val progress = MiuixVideoCardTransitionProgress(gestureTranslationEnabled = true, gesturePoseEnabled = true)
        progress.bind(scope)
        progress.gestureFollow.onTransitionState(event(0f, 700f))
        pose(progress, scope)
        progress.gestureFollow.onTransitionState(event(0f, 1200f))
        scope.gesture = NavGesture(0f, NavSwipeEdge.Left, 1200f, 700f)
        val release = pose(progress, scope)
        assertOffset(Offset(0f, 500f), release.translation)
        val settle = Settle(NavSettlePhase.Cancel)
        scope.settle = settle
        assertEquals(release, pose(progress, scope))
        settle.elapsedMillis = motion.cancelDurationMillis / 2f
        assertTrue(pose(progress, scope).translation.y in 0f..499f)
        settle.elapsedMillis = motion.cancelDurationMillis.toFloat()
        assertOffset(Offset.Zero, pose(progress, scope).translation)
        val cancelSpec = resolveVideoHeroNavMotion(motion, true, true).cancel as NavSettleSpec.Tween
        assertEquals(motion.cancelDurationMillis, cancelSpec.durationMillis)
    }

    @Test
    fun heldRotatedCardRestoresCoverAndBottomChromeBeforeRelease() {
        val scope = Scope()
        val progress = MiuixVideoCardTransitionProgress(gestureTranslationEnabled = true, gesturePoseEnabled = true)
        progress.bind(scope)
        progress.gestureFollow.onTransitionState(event(0f, 700f))
        pose(progress, scope)
        var previousCover = 0f
        for (fraction in listOf(.5f, .85f, .93f, .999f)) {
            scope.relativeDepth = -fraction
            scope.gesture = NavGesture(fraction, NavSwipeEdge.Left, 1100f, 700f)
            progress.gestureFollow.onTransitionState(event(500f * fraction, 1100f))
            val depth = progress.depthOrNull()!!
            val media = resolveVideoDetailReturnMediaFrame(
                depth, isCommittedCardReturn = false, hasResidentCover = true,
                liveReturnMorph = true, isReturnGestureInProgress = true,
            )
            val chrome = resolveVideoDetailFlyingSourceChromeAlpha(
                depth, VideoCardTransitionBackgroundPhase.HELD, true, VideoCardSourceLayout.STACKED,
            )
            assertEquals(media.coverAlpha, chrome, .001f)
            assertTrue(media.coverAlpha >= previousCover)
            previousCover = media.coverAlpha
            assertTrue(pose(progress, scope).rotationY != 0f)
        }
        assertEquals(1f, previousCover, .001f)
        // Reversing the held gesture restores the live player and removes the source metadata.
        scope.relativeDepth = -.5f
        scope.gesture = NavGesture(.5f, NavSwipeEdge.Left, 900f, 700f)
        assertEquals(0f, resolveVideoDetailReturnMediaFrame(
            progress.depthOrNull()!!, false, true, true, true).coverAlpha)
    }

    @Test
    fun regrabbingCancelKeepsVisibleDepthAndCompleteThreeDimensionalPose() {
        val scope = Scope()
        val progress = MiuixVideoCardTransitionProgress(gestureTranslationEnabled = true, gesturePoseEnabled = true)
        progress.bind(scope)
        progress.gestureFollow.onTransitionState(event(0f, 700f))
        pose(progress, scope)
        scope.relativeDepth = -.6f
        scope.gesture = NavGesture(.6f, NavSwipeEdge.Left, 1000f, 700f)
        progress.gestureFollow.onTransitionState(event(400f, 1000f))
        pose(progress, scope)
        progress.gestureFollow.onTransitionState(NavigationEventTransitionState.Idle)
        scope.settle = Settle(NavSettlePhase.Cancel)
        pose(progress, scope)
        scope.relativeDepth = -.3f
        val before = pose(progress, scope)
        val depth = progress.depthOrNull()!!
        scope.settle = null
        scope.gesture = NavGesture(0f, NavSwipeEdge.Right, 1800f)
        progress.gestureFollow.onTransitionState(event(1080f, 1800f, NavigationEvent.EDGE_RIGHT))
        assertEquals(depth, progress.depthOrNull())
        assertEquals(before, pose(progress, scope))
    }

    @Test
    fun changingGrabPointPreservesInterruptedRotationAndPosition() {
        val anchor = VideoCardFollowPose(Offset(90f, 140f), 3f, TransformOrigin(.2f, .4f))
        val initial = Offset(1050f, 1800f)
        val pose = resolveVideoCardFollowPose(
            VideoCardGestureSample(2, initial, initial), bounds, 1080f, 2400f, .8f, .8f, 30f, anchor,
        )
        fun apply(pose: VideoCardFollowPose, point: Offset): Offset {
            val pivot = Offset(pose.transformOrigin.pivotFractionX * 1080f,
                pose.transformOrigin.pivotFractionY * 2400f)
            val angle = pose.rotationZ * PI.toFloat() / 180f
            val delta = point - pivot
            return pivot + Offset(delta.x * cos(angle) - delta.y * sin(angle),
                delta.x * sin(angle) + delta.y * cos(angle)) + pose.translation
        }
        for (point in listOf(Offset.Zero, Offset(540f, 1200f), Offset(1080f, 2400f))) {
            assertOffset(apply(anchor, point), apply(pose, point))
        }
    }

    @Test
    fun revealedParentDoesNotFollowChildGesture() {
        val scope = Scope()
        val progress = MiuixVideoCardTransitionProgress(gestureTranslationEnabled = true, gesturePoseEnabled = true)
        progress.bind(scope)
        progress.gestureFollow.onTransitionState(event(0f, 700f))
        progress.gestureFollow.onTransitionState(event(500f, 1200f))
        scope.relativeDepth = .4f
        scope.role = NavRole.Covered
        scope.gesture = NavGesture(.6f, NavSwipeEdge.Left, 1200f, 700f)
        assertEquals(1f, progress.depthOrNull())
        assertEquals(null, progress.followPose(scope, bounds, 1080f, 2400f, 30f, motion))
    }
}
