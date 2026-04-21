package com.android.purebilibili.core.ui.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

internal object AppMotionEasing {
    val EmphasizedEnter: Easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    val EmphasizedExit: Easing = CubicBezierEasing(0.32f, 0f, 0.67f, 0f)
    val Continuity: Easing = CubicBezierEasing(0.20f, 0.90f, 0.22f, 1.00f)
    val GentleEnter: Easing = CubicBezierEasing(0.18f, 0.80f, 0.20f, 1.00f)
}

internal fun <T> emphasizedEnterTween(durationMillis: Int): TweenSpec<T> =
    tween(durationMillis = durationMillis, easing = AppMotionEasing.EmphasizedEnter)

internal fun <T> emphasizedExitTween(durationMillis: Int): TweenSpec<T> =
    tween(durationMillis = durationMillis, easing = AppMotionEasing.EmphasizedExit)

internal fun <T> continuityTween(durationMillis: Int): TweenSpec<T> =
    tween(durationMillis = durationMillis, easing = AppMotionEasing.Continuity)

internal fun <T> gentleEnterTween(durationMillis: Int): TweenSpec<T> =
    tween(durationMillis = durationMillis, easing = AppMotionEasing.GentleEnter)

internal fun <T> softLandingSpring(): SpringSpec<T> =
    spring(
        dampingRatio = 0.86f,
        stiffness = Spring.StiffnessMediumLow
    )

internal fun interactiveSnapSpring(): SpringSpec<Float> =
    spring(
        dampingRatio = 0.78f,
        stiffness = 420f
    )

internal fun expressiveSnapSpring(): SpringSpec<Float> =
    spring(
        dampingRatio = 0.72f,
        stiffness = 520f
    )

internal fun pressFeedbackSpring(): SpringSpec<Float> =
    spring(
        dampingRatio = 1f,
        stiffness = 1000f,
        visibilityThreshold = 0.001f
    )

internal fun selectionSpring(): SpringSpec<Float> =
    spring(
        dampingRatio = 0.82f,
        stiffness = 500f
    )

internal fun indicatorSpring(): SpringSpec<Float> =
    spring(
        dampingRatio = 0.7f,
        stiffness = Spring.StiffnessMedium
    )
