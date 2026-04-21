package com.android.purebilibili.core.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import com.android.purebilibili.core.ui.adaptive.MotionTier
import com.android.purebilibili.core.ui.motion.continuityTween
import com.android.purebilibili.core.ui.motion.gentleEnterTween
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * 瀑布流/交错式入场动画
 * Staggered entrance animation modifier.
 *
 * @param index The index of the item in the list.
 * @param visible Whether the item should be visible.
 * @param offsetDistance The initial Y offset distance.
 */
data class StaggeredEntranceMotionPolicy(
    val delayStepMs: Int,
    val maxDelayMs: Int,
    val alphaDurationMs: Int,
    val translationDurationMs: Int,
    val scaleDurationMs: Int,
    val initialScale: Float,
    val offsetFactor: Float
)

internal data class StaggeredEntranceInitialState(
    val alpha: Float,
    val translationY: Float,
    val scale: Float
)

fun resolveStaggeredEntranceMotionPolicy(
    motionTier: MotionTier
): StaggeredEntranceMotionPolicy {
    return when (motionTier) {
        MotionTier.Reduced -> StaggeredEntranceMotionPolicy(
            delayStepMs = 10,
            maxDelayMs = 60,
            alphaDurationMs = 160,
            translationDurationMs = 190,
            scaleDurationMs = 170,
            initialScale = 0.985f,
            offsetFactor = 0.30f
        )

        MotionTier.Enhanced -> StaggeredEntranceMotionPolicy(
            delayStepMs = 26,
            maxDelayMs = 220,
            alphaDurationMs = 300,
            translationDurationMs = 430,
            scaleDurationMs = 380,
            initialScale = 0.92f,
            offsetFactor = 1.05f
        )

        MotionTier.Normal -> StaggeredEntranceMotionPolicy(
            delayStepMs = 24,
            maxDelayMs = 180,
            alphaDurationMs = 220,
            translationDurationMs = 320,
            scaleDurationMs = 300,
            initialScale = 0.96f,
            offsetFactor = 0.72f
        )
    }
}

internal fun resolveStaggeredEntranceInitialState(
    visible: Boolean,
    offsetDistance: Float,
    policy: StaggeredEntranceMotionPolicy
): StaggeredEntranceInitialState {
    return if (visible) {
        StaggeredEntranceInitialState(
            alpha = 1f,
            translationY = 0f,
            scale = 1f
        )
    } else {
        StaggeredEntranceInitialState(
            alpha = 0f,
            translationY = offsetDistance * policy.offsetFactor,
            scale = policy.initialScale
        )
    }
}

internal fun shouldRunStaggeredEntranceAnimation(
    visible: Boolean,
    currentAlpha: Float,
    currentTranslationY: Float,
    currentScale: Float
): Boolean {
    if (!visible) return false
    return currentAlpha < 0.999f || abs(currentTranslationY) > 0.5f || currentScale < 0.999f
}

fun Modifier.staggeredEntrance(
    index: Int,
    visible: Boolean,
    offsetDistance: Float = 50f,
    motionTier: MotionTier = MotionTier.Normal
): Modifier = composed {
    val policy = remember(motionTier) { resolveStaggeredEntranceMotionPolicy(motionTier) }
    val initialState = remember(offsetDistance, policy) {
        resolveStaggeredEntranceInitialState(
            visible = visible,
            offsetDistance = offsetDistance,
            policy = policy
        )
    }
    val alpha = remember { Animatable(initialState.alpha) }
    val translationY = remember { Animatable(initialState.translationY) }
    val scale = remember { Animatable(initialState.scale) }

    LaunchedEffect(visible) {
        if (!visible) {
            alpha.snapTo(0f)
            translationY.snapTo(offsetDistance * policy.offsetFactor)
            scale.snapTo(policy.initialScale)
            return@LaunchedEffect
        }
        if (!shouldRunStaggeredEntranceAnimation(
                visible = visible,
                currentAlpha = alpha.value,
                currentTranslationY = translationY.value,
                currentScale = scale.value
            )
        ) {
            return@LaunchedEffect
        }

        // Delay based on index for the staggered effect
        val delayMs = (index * policy.delayStepMs).coerceAtMost(policy.maxDelayMs).toLong()
        delay(delayMs)

        // Parallel animations
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = gentleEnterTween(policy.alphaDurationMs)
            )
        }
        launch {
            translationY.animateTo(
                targetValue = 0f,
                animationSpec = continuityTween(policy.translationDurationMs)
            )
        }
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = gentleEnterTween(policy.scaleDurationMs)
            )
        }
    }

    this.graphicsLayer {
        this.alpha = alpha.value
        this.translationY = translationY.value * density // Convert dp-like float to pixels if needed, but here we treat input as px or user dp
        this.scaleX = scale.value
        this.scaleY = scale.value
    }
}
