package com.android.purebilibili.navigation3.predictiveback

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventTransitionState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

internal data class VideoCardGestureSample(
    val sequence: Long,
    val initialTouch: Offset,
    val touch: Offset,
)

/** Read-only observer: Miuix remains the sole owner of back completion/cancellation. */
internal class VideoCardGestureFollowState {
    var hostOriginOnScreen: Offset by mutableStateOf(Offset.Zero)
    var hostOriginInRoot: Offset by mutableStateOf(Offset.Zero)
    var sample: VideoCardGestureSample? by mutableStateOf(null)
        private set

    /** 每次新抓取（tracking 从无到有）自增，供 UI 层做触觉反馈等一次性响应 */
    var grabEvent: Long by mutableStateOf(0L)
        private set

    private var tracking = false
    private var sequence = 0L

    fun onTransitionState(state: NavigationEventTransitionState) {
        val event = (state as? NavigationEventTransitionState.InProgress)
            ?.takeIf { it.direction == NavigationEventTransitionState.TRANSITIONING_BACK }
            ?.latestEvent
            ?.takeIf { it.swipeEdge != NavigationEvent.EDGE_NONE }
        if (event == null) {
            // Retain the release coordinates while the navigation driver settles.
            tracking = false
            return
        }
        val touch = Offset(event.touchX, event.touchY)
        val initial = if (!tracking) {
            tracking = true
            sequence++
            grabEvent++
            touch
        } else sample?.initialTouch ?: touch
        sample = VideoCardGestureSample(sequence, initial, touch)
    }

    fun localSample(): VideoCardGestureSample? = sample?.let {
        it.copy(initialTouch = it.initialTouch - hostOriginOnScreen, touch = it.touch - hostOriginOnScreen)
    }
}

internal data class VideoCardFollowPose(
    val translation: Offset = Offset.Zero,
    val rotationZ: Float = 0f,
    val transformOrigin: TransformOrigin = TransformOrigin.Center,
    val rotationX: Float = 0f,
    val rotationY: Float = 0f,
    val liftScale: Float = 1f,
    val cameraDistance: Float = MIUIX_VIDEO_CARD_GESTURE_CAMERA_DISTANCE_DP,
) {
    /** 带有可见变换的中断姿态沿用原枢轴，避免切换触点时改变投影。 */
    val hasSpatialTransform: Boolean
        get() = rotationZ != 0f || rotationX != 0f || rotationY != 0f || liftScale != 1f

    fun settle(weight: Float): VideoCardFollowPose = copy(
        translation = translation * weight.coerceIn(0f, 1f),
        rotationZ = rotationZ * weight.coerceIn(0f, 1f),
        rotationX = rotationX * weight.coerceIn(0f, 1f),
        rotationY = rotationY * weight.coerceIn(0f, 1f),
        liftScale = 1f + (liftScale - 1f) * weight.coerceIn(0f, 1f),
        cameraDistance = MIUIX_VIDEO_CARD_GESTURE_CAMERA_DISTANCE_DP +
            (cameraDistance - MIUIX_VIDEO_CARD_GESTURE_CAMERA_DISTANCE_DP) * weight.coerceIn(0f, 1f),
    )
}

/** Independent output channels: changing translation never enables/disables perspective. */
internal fun resolveVideoCardGestureChannels(
    pose: VideoCardFollowPose,
    translationEnabled: Boolean,
    poseEnabled: Boolean,
    fixedPathOrigin: TransformOrigin = pose.transformOrigin,
): VideoCardFollowPose {
    val result = if (poseEnabled) pose else pose.settle(0f)
    return result.copy(
        translation = if (translationEnabled) pose.translation else Offset.Zero,
        transformOrigin = if (translationEnabled) pose.transformOrigin else fixedPathOrigin,
    )
}

/** Same source-directed path used by the morph layer, in host-local physical pixels. */
internal fun resolveVideoCardPathOffset(
    bounds: Rect,
    width: Float,
    height: Float,
    morph: Float,
    arcAmplitudePx: Float,
): Offset {
    val remaining = 1f - morph
    val arcSign = if (bounds.center.x < width / 2f) 1f else -1f
    return Offset(
        bounds.left.coerceIn(-width, width) * remaining +
            arcSign * arcAmplitudePx * sin(PI.toFloat() * morph) * remaining,
        bounds.top.coerceIn(-height, height) * remaining,
    )
}

/**
 * Cancel the source-directed path during direct manipulation. Scaling stays pinned to the
 * initial grab point, so moving either axis by N pixels moves that point by exactly N pixels.
 * The correction is outside the morph/content layers and is not compressed by their scales.
 */
internal fun resolveVideoCardFollowPose(
    sample: VideoCardGestureSample,
    bounds: Rect,
    width: Float,
    height: Float,
    morph: Float,
    anchorMorph: Float,
    arcAmplitudePx: Float,
    anchorPose: VideoCardFollowPose = VideoCardFollowPose(),
    gestureProgress: Float = (anchorMorph - morph).coerceIn(0f, 1f),
    preserveViewport: Boolean = false,
): VideoCardFollowPose {
    val scaleX = resolveMiuixVideoCardOuterScale(bounds.width / width, morph, 1f)
    val scaleY = if (preserveViewport) {
        resolveMiuixVideoCardOuterScaleY(bounds.width / width, bounds.height / height, morph, 1f)
    } else resolveMiuixVideoCardOuterScale(bounds.height / height, morph, 1f)
    val anchorScaleX = resolveMiuixVideoCardOuterScale(bounds.width / width, anchorMorph, 1f)
    val anchorScaleY = if (preserveViewport) {
        resolveMiuixVideoCardOuterScaleY(bounds.width / width, bounds.height / height, anchorMorph, 1f)
    } else resolveMiuixVideoCardOuterScale(bounds.height / height, anchorMorph, 1f)
    val path = resolveVideoCardPathOffset(bounds, width, height, morph, arcAmplitudePx)
    val anchorPath = resolveVideoCardPathOffset(bounds, width, height, anchorMorph, arcAmplitudePx)
    val delta = sample.touch - sample.initialTouch
    val scaleCompensation = Offset(
        (anchorScaleX - scaleX) * sample.initialTouch.x,
        (anchorScaleY - scaleY) * sample.initialTouch.y,
    )
    val anchorGrab = anchorPath + Offset(
        anchorScaleX * sample.initialTouch.x, anchorScaleY * sample.initialTouch.y,
    )
    // Re-grabbing a cancelling pose can move the rotation pivot. Preserve the same rigid
    // transform at the interruption frame before following the new finger.
    val keepInterruptedPivot = anchorPose.hasSpatialTransform
    val pivotDelta = if (keepInterruptedPivot) Offset.Zero else Offset(
        anchorPose.transformOrigin.pivotFractionX * width,
        anchorPose.transformOrigin.pivotFractionY * height,
    ) - anchorGrab
    val angle = anchorPose.rotationZ * PI.toFloat() / 180f
    val rotatedPivotDelta = Offset(
        pivotDelta.x * cos(angle) - pivotDelta.y * sin(angle),
        pivotDelta.x * sin(angle) + pivotDelta.y * cos(angle),
    )
    val pivotCompensation = pivotDelta - rotatedPivotDelta
    val pull = gestureProgress.coerceIn(0f, 1f)
    // 姿态包络改为快速上升后保持：拖满时姿态不再中途回正（否则位移仍在跟手、
    // 姿态却在自动摆正，两种感知打架）。松手后的收敛统一交给 settle 回放。
    // 值为 2.5 表示约 40% 返回进度即到达满姿态；之前的 sin(π·pull) 会在两端归零。
    val poseWeight = if (pull <= 0f) 0f else {
        val ramp = (pull * 2.5f).coerceIn(0f, 1f)
        ramp * ramp * (3f - 2f * ramp)
    }
    val edgeSign = if (sample.initialTouch.x < width / 2f) 1f else -1f
    val grabTilt = (sample.initialTouch.y / height - .5f) * 2f
    val moveTilt = (delta.y / (height * .5f)).coerceIn(-1f, 1f)
    val tilt = (grabTilt * .65f + moveTilt * .35f).coerceIn(-1f, 1f)
    return VideoCardFollowPose(
        translation = anchorPose.translation + pivotCompensation + anchorPath - path + scaleCompensation + delta,
        // A small, continuous tilt; the grab point itself stays under the finger.
        rotationZ = (anchorPose.rotationZ + edgeSign * (2.4f + tilt * 2f) * poseWeight)
            .coerceIn(minOf(-4.4f, anchorPose.rotationZ), maxOf(4.4f, anchorPose.rotationZ)),
        rotationX = (anchorPose.rotationX - tilt * 1.6f * poseWeight)
            .coerceIn(minOf(-1.6f, anchorPose.rotationX), maxOf(1.6f, anchorPose.rotationX)),
        rotationY = (anchorPose.rotationY + edgeSign * 1.4f * poseWeight)
            .coerceIn(minOf(-1.4f, anchorPose.rotationY), maxOf(1.4f, anchorPose.rotationY)),
        liftScale = (anchorPose.liftScale - MIUIX_VIDEO_CARD_GESTURE_LIFT_SCALE * poseWeight)
            .coerceIn(minOf(.96f, anchorPose.liftScale), maxOf(1f, anchorPose.liftScale)),
        // 自动飞行的透视距离或释放冲量可能超出跟随区间；钳制不能改变接手首帧。
        cameraDistance = (anchorPose.cameraDistance - MIUIX_VIDEO_CARD_GESTURE_CAMERA_PULL_DP * poseWeight)
            .coerceAtLeast(minOf(6f, anchorPose.cameraDistance)),
        transformOrigin = if (keepInterruptedPivot) anchorPose.transformOrigin else TransformOrigin(
            (path.x + scaleX * sample.initialTouch.x) / width,
            (path.y + scaleY * sample.initialTouch.y) / height,
        ),
    )
}

// 释放冲量：把系统返回驱动给的速度标量换算成额外旋转角。
// 钳制值刻意高于跟随期的 ±4.4°，冲量允许短暂越界，衰减后自然回到姿态区间。
internal const val VIDEO_CARD_RELEASE_ROTATION_IMPULSE_PER_VELOCITY = 1.4f
internal const val VIDEO_CARD_RELEASE_ROTATION_IMPULSE_MAX_DEGREE = 2f

/**
 * 释放后的旋转冲量随收敛进度建立再消退，两端均为零且斜率为零。
 * 幅度随释放速度变化，方向沿既有倾斜方向；无可见倾斜时不施加。
 */
internal fun VideoCardFollowPose.withReleaseImpulse(
    releaseVelocity: Float,
    settleWeight: Float,
): VideoCardFollowPose {
    val direction = when {
        rotationZ > 0.05f -> 1f
        rotationZ < -0.05f -> -1f
        else -> 0f
    }
    if (direction == 0f || !releaseVelocity.isFinite() || releaseVelocity == 0f) return this
    val weight = settleWeight.coerceIn(0f, 1f)
    val pulse = 4f * weight * (1f - weight)
    val impulse = direction * (abs(releaseVelocity) * VIDEO_CARD_RELEASE_ROTATION_IMPULSE_PER_VELOCITY)
        .coerceIn(0f, VIDEO_CARD_RELEASE_ROTATION_IMPULSE_MAX_DEGREE) *
        pulse * pulse
    return copy(rotationZ = (rotationZ + impulse).coerceIn(-6.4f, 6.4f))
}
