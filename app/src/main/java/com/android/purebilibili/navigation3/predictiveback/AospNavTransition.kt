// SPDX-License-Identifier: GPL-3.0-only
package com.android.purebilibili.navigation3.predictiveback

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.nav.transition.NavGesture
import top.yukonga.miuix.kmp.nav.transition.NavMotion
import top.yukonga.miuix.kmp.nav.transition.NavRole
import top.yukonga.miuix.kmp.nav.transition.NavSettlePhase
import top.yukonga.miuix.kmp.nav.transition.NavSettleSpec
import top.yukonga.miuix.kmp.nav.transition.NavSwipeEdge
import top.yukonga.miuix.kmp.nav.transition.NavTransition
import top.yukonga.miuix.kmp.nav.transition.navDirectionalTransition
import top.yukonga.miuix.kmp.nav.transition.navGraphicsTransition
import kotlin.math.abs
import kotlin.math.min

internal const val AOSP_PREDICTIVE_COMMIT_DURATION_MILLIS = 220
private const val AOSP_PREDICTIVE_FADE_DURATION_MILLIS = 90f
private const val OPEN_FADE_START = 0.12f
private const val OPEN_FADE_SPAN = 0.71f
private const val CLOSE_FADE_START = 0.21f
private const val CLOSE_FADE_SPAN = 0.74f
private const val CLASSIC_FADE_DURATION = 83f
private const val OPEN_FADE_OFFSET = 50f
private const val CLOSE_FADE_OFFSET = 35f
private const val CROSS_ACTIVITY_MIN_SCALE = 0.9f

private val CrossActivityDrift = 96.dp
private val CrossActivityEdgeMargin = 8.dp

private val ClassicActivityMotion = NavMotion(
    programmatic = NavSettleSpec.Tween(durationMillis = 450, easing = FastOutExtraSlowIn),
)

private val ClassicActivityOpen: NavTransition = navGraphicsTransition(
    motion = ClassicActivityMotion,
    scrim = { 0f },
) { scope ->
    val depth = scope.relativeDepth
    val driftPx = with(scope.density) { CrossActivityDrift.toPx() }
    if (depth <= 0f) {
        val progress = topProgress(depth)
        translationX = (1f - progress) * driftPx
        alpha = if (scope.role == NavRole.Incoming) {
            val settle = scope.settle
            if (settle != null) {
                ((settle.elapsedMillis - OPEN_FADE_OFFSET) / CLASSIC_FADE_DURATION)
                    .coerceIn(0f, 1f)
            } else {
                ((progress - OPEN_FADE_START) / OPEN_FADE_SPAN).coerceIn(0f, 1f)
            }
        } else {
            1f
        }
    } else {
        translationX = -coverProgress(depth) * driftPx
    }
}

private val ClassicActivityClose: NavTransition = navGraphicsTransition(
    motion = ClassicActivityMotion,
    scrim = { 0f },
) { scope ->
    val depth = scope.relativeDepth
    val driftPx = with(scope.density) { CrossActivityDrift.toPx() }
    if (depth <= 0f) {
        val progress = topProgress(depth)
        translationX = (1f - progress) * driftPx
        alpha = if (scope.role == NavRole.Outgoing) {
            val settle = scope.settle
            if (settle != null) {
                (1f - (settle.elapsedMillis - CLOSE_FADE_OFFSET) / CLASSIC_FADE_DURATION)
                    .coerceIn(0f, 1f)
            } else {
                ((progress - CLOSE_FADE_START) / CLOSE_FADE_SPAN).coerceIn(0f, 1f)
            }
        } else {
            1f
        }
    } else {
        translationX = -coverProgress(depth) * driftPx
    }
}

private val CrossActivityPredictive: NavTransition = navGraphicsTransition(
    opaqueDepth = 1f,
    motion = NavMotion(
        commit = NavSettleSpec.Tween(
            durationMillis = AOSP_PREDICTIVE_COMMIT_DURATION_MILLIS,
            easing = FastOutExtraSlowIn,
        ),
        cancel = NavSettleSpec.Spring(stiffness = 1500f),
    ),
    scrim = { scope ->
        val settle = scope.settle
        val gesture = scope.gesture
        when {
            settle?.phase == NavSettlePhase.Commit ->
                (1f - settle.elapsedMillis / AOSP_PREDICTIVE_COMMIT_DURATION_MILLIS.toFloat())
                    .coerceIn(0f, 1f)

            gesture != null ->
                (scope.relativeDepth.coerceIn(0f, 1f) /
                    (1f - gesture.progress).coerceAtLeast(0.01f)).coerceIn(0f, 1f)

            else -> scope.relativeDepth.coerceIn(0f, 1f)
        }
    },
) { scope ->
    val depth = scope.relativeDepth
    val gesture = scope.gesture
    val settle = scope.settle
    val committing = settle?.phase == NavSettlePhase.Commit
    val widthPx = scope.layoutSize.width.toFloat()
    val heightPx = scope.layoutSize.height.toFloat()
    val driftPx = with(scope.density) { CrossActivityDrift.toPx() }
    val hugMax = (
        widthPx * (1f - CROSS_ACTIVITY_MIN_SCALE) / 2f -
            with(scope.density) { CrossActivityEdgeMargin.toPx() }
        ).coerceAtLeast(0f)
    val hugs = gesture?.swipeEdge != NavSwipeEdge.Right
    if (depth <= 0f) {
        val progress = topProgress(depth)
        if (scope.role == NavRole.Outgoing && committing && gesture != null) {
            val releaseProgress = (1f - gesture.progress).coerceAtLeast(0.01f)
            val post = (1f - progress / releaseProgress).coerceIn(0f, 1f)
            val releaseEasedProgress = shapedTopProgress(releaseProgress, gesture)
            val committedScale =
                CROSS_ACTIVITY_MIN_SCALE + (1f - CROSS_ACTIVITY_MIN_SCALE) * releaseEasedProgress
            val grown = committedScale + (1f - committedScale) * post
            scaleX = snapScaleToPixelExtent(grown, widthPx)
            scaleY = scaleX
            var tx = if (hugs) (1f - releaseEasedProgress) * hugMax else 0f
            tx += post * driftPx
            alpha = (1f - settle.elapsedMillis / AOSP_PREDICTIVE_FADE_DURATION_MILLIS)
                .coerceAtLeast(0f)
            translationX = snapTranslationToPixelEdge(tx, scaleX, widthPx)
            translationY = snapTranslationToPixelEdge(
                translation = crossActivityYShift(
                    gesture = gesture,
                    height = heightPx,
                    scale = scaleX,
                    density = scope.density,
                ),
                scale = scaleY,
                extent = heightPx,
            )
        } else {
            val easedProgress = shapedTopProgress(progress, gesture)
            scaleX = snapScaleToPixelExtent(
                scale = (
                    CROSS_ACTIVITY_MIN_SCALE + (1f - CROSS_ACTIVITY_MIN_SCALE) * easedProgress
                    ),
                extent = widthPx,
            )
            scaleY = scaleX
            translationX = snapTranslationToPixelEdge(
                translation = if (hugs) (1f - easedProgress) * hugMax else 0f,
                scale = scaleX,
                extent = widthPx,
            )
            alpha = when {
                scope.role == NavRole.Outgoing && gesture != null -> {
                    val releaseProgress = (1f - gesture.progress).coerceAtLeast(0.01f)
                    (1f - (1f - progress / releaseProgress).coerceIn(0f, 1f) * 3.5f)
                        .coerceAtLeast(0f)
                }

                gesture != null -> 1f
                else -> (progress / 0.2f).coerceIn(0f, 1f)
            }
            translationY = snapTranslationToPixelEdge(
                translation = crossActivityYShift(
                    gesture = gesture,
                    height = heightPx,
                    scale = scaleX,
                    density = scope.density,
                ),
                scale = scaleX,
                extent = heightPx,
            )
        }
    } else {
        val cover = coverProgress(depth)
        val post = if (gesture != null) {
            val releaseProgress = gesture.progress
            if (releaseProgress >= 1f) {
                1f
            } else {
                (((1f - cover) - releaseProgress) / (1f - releaseProgress)).coerceIn(0f, 1f)
            }
        } else {
            1f - cover
        }
        val rawTranslationX = -(1f - post) * driftPx
        if (gesture != null) {
            val travel = if (committing) gesture.progress else (1f - cover)
            val eased = BackGestureEasing.transform(travel.coerceIn(0f, 1f))
            val liveScale =
                CROSS_ACTIVITY_MIN_SCALE + (1f - CROSS_ACTIVITY_MIN_SCALE) * (1f - eased)
            scaleX = snapScaleToPixelExtent(
                liveScale + (1f - liveScale) * post,
                widthPx,
            )
            scaleY = scaleX
        }
        translationX = snapTranslationToPixelEdge(rawTranslationX, scaleX, widthPx)
        translationY = snapTranslationToPixelEdge(
            translation = crossActivityYShift(
                gesture = gesture,
                height = heightPx,
                scale = scaleX,
                density = scope.density,
            ),
            scale = scaleX,
            extent = heightPx,
        )
    }
}

internal val AospNavTransition: NavTransition = navDirectionalTransition(
    push = ClassicActivityOpen,
    pop = ClassicActivityClose,
    predictivePop = CrossActivityPredictive,
)

private fun shapedTopProgress(progress: Float, gesture: NavGesture?): Float =
    if (gesture == null) progress else 1f - BackGestureEasing.transform((1f - progress).coerceIn(0f, 1f))

private fun crossActivityYShift(
    gesture: NavGesture?,
    height: Float,
    scale: Float,
    density: Density,
): Float {
    if (gesture == null || height <= 0f) return 0f
    val rawDelta = gesture.touchY - gesture.initialTouchY
    val half = height / 2f
    val ratio = min(half, abs(rawDelta)) / half
    val damped = 1f - (1f - ratio) * (1f - ratio)
    val marginPx = with(density) { CrossActivityEdgeMargin.toPx() }
    val maxShift = ((height - height * scale) / 2f - marginPx).coerceAtLeast(0f)
    return maxShift * damped * (if (rawDelta < 0f) -1f else 1f)
}
