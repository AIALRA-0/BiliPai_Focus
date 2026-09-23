package com.android.purebilibili.core.ui.motion

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.IntOffset

private const val NAVIGATION_SPRING_REFERENCE_DURATION_MILLIS = 350f
private const val NAVIGATION_SPRING_REFERENCE_STIFFNESS = 700f
private const val NAVIGATION_SPRING_MAX_STIFFNESS = 2_000f

fun resolveNavigationSlideSpringStiffness(durationMillis: Int): Float {
    val safeDuration = durationMillis.coerceAtLeast(1).toFloat()
    val durationRatio = NAVIGATION_SPRING_REFERENCE_DURATION_MILLIS / safeDuration
    return (NAVIGATION_SPRING_REFERENCE_STIFFNESS * durationRatio * durationRatio)
        .coerceIn(
            NAVIGATION_SPRING_REFERENCE_STIFFNESS,
            NAVIGATION_SPRING_MAX_STIFFNESS,
        )
}

fun navigationSlideSpring(durationMillis: Int): SpringSpec<IntOffset> =
    spring(
        dampingRatio = 1f,
        stiffness = resolveNavigationSlideSpringStiffness(durationMillis),
        visibilityThreshold = IntOffset(1, 1),
    )
