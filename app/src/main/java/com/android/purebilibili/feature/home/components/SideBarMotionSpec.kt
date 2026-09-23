package com.android.purebilibili.feature.home.components

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

internal const val NavigationSelectionScale = 1.1f
// Keep the cross-scale accent inside the compact dock's capture band: a large
// enlargement combined with an 8dp lift cropped the top of icon+text items.
internal const val FloatingBottomBarSelectionScale = 1.1f
internal const val FloatingBottomBarSelectionLiftDp = 2f
internal const val NavigationSelectionWobbleDegrees = 4f
internal const val NavigationSelectionCounterWobbleDegrees = -3f

internal fun resolveNavigationIconCrossScale(
    enabled: Boolean,
    coverage: Float,
): Float {
    if (!enabled) return 1f
    val progress = coverage.coerceIn(0f, 1f)
    // Cross-scale is a transition accent, not a persistent selected state. A sine arc keeps
    // both endpoints at the icon's authored size and reaches the enlargement peak only while
    // the indicator is travelling between destinations.
    val transitionArc = kotlin.math.sin(Math.PI.toFloat() * progress)
    return androidx.compose.ui.util.lerp(1f, FloatingBottomBarSelectionScale, transitionArc)
}

internal fun resolveNavigationIconSelectionLiftDp(scale: Float): Float {
    val progress = ((scale - 1f) / (FloatingBottomBarSelectionScale - 1f)).coerceIn(0f, 1f)
    return FloatingBottomBarSelectionLiftDp * progress
}

internal fun <T> navigationSelectionScaleMotionSpec(): SpringSpec<T> = spring(
    dampingRatio = 0.72f,
    stiffness = 420f,
)

internal fun <T> navigationSelectionWobbleMotionSpec(): SpringSpec<T> = spring(
    dampingRatio = 0.62f,
    stiffness = 720f,
)

@Immutable
internal data class NavigationSelectionTransform(
    val scale: () -> Float,
    val rotationDegrees: () -> Float,
)

@Composable
internal fun rememberNavigationSelectionTransform(
    selected: Boolean,
    label: String,
): NavigationSelectionTransform {
    var wobbleTarget by remember { mutableFloatStateOf(0f) }
    var hasObservedSelection by remember { mutableStateOf(false) }
    val scale = animateFloatAsState(
        targetValue = if (selected) NavigationSelectionScale else 1f,
        animationSpec = navigationSelectionScaleMotionSpec(),
        label = "${label}_selection_scale",
    )
    val rotation = animateFloatAsState(
        targetValue = wobbleTarget,
        animationSpec = navigationSelectionWobbleMotionSpec(),
        label = "${label}_selection_wobble",
    )

    LaunchedEffect(selected) {
        if (hasObservedSelection && selected) {
            wobbleTarget = NavigationSelectionWobbleDegrees
            delay(45)
            wobbleTarget = NavigationSelectionCounterWobbleDegrees
            delay(45)
        }
        wobbleTarget = 0f
        hasObservedSelection = true
    }
    return remember(scale, rotation) {
        NavigationSelectionTransform(
            scale = { scale.value },
            rotationDegrees = { rotation.value },
        )
    }
}
