package com.android.purebilibili.core.ui.motion

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically

internal enum class VerticalContentRevealMode {
    DefaultExpand,
    FloatUp
}

internal data class VerticalContentRevealMotionSpec(
    val mode: VerticalContentRevealMode,
    val delayMillis: Int,
    val durationMillis: Int,
    val slideOffsetDp: Float,
    val initialScale: Float
)

internal fun resolveCommentVerticalContentRevealMotionSpec(): VerticalContentRevealMotionSpec {
    return VerticalContentRevealMotionSpec(
        mode = VerticalContentRevealMode.DefaultExpand,
        delayMillis = 0,
        durationMillis = 0,
        slideOffsetDp = 0f,
        initialScale = 1f
    )
}

internal fun resolveDetailVerticalContentRevealMotionSpec(
    delayMillis: Int,
    durationMillis: Int,
    slideOffsetDp: Float,
    initialScale: Float
): VerticalContentRevealMotionSpec {
    return VerticalContentRevealMotionSpec(
        mode = VerticalContentRevealMode.FloatUp,
        delayMillis = delayMillis,
        durationMillis = durationMillis,
        slideOffsetDp = slideOffsetDp,
        initialScale = initialScale
    )
}

internal fun verticalContentRevealEnterTransition(
    spec: VerticalContentRevealMotionSpec
): EnterTransition {
    return when (spec.mode) {
        VerticalContentRevealMode.DefaultExpand -> expandVertically() + fadeIn()
        VerticalContentRevealMode.FloatUp -> fadeIn()
    }
}

internal fun verticalContentRevealExitTransition(
    spec: VerticalContentRevealMotionSpec
): ExitTransition {
    return when (spec.mode) {
        VerticalContentRevealMode.DefaultExpand -> shrinkVertically() + fadeOut()
        VerticalContentRevealMode.FloatUp -> fadeOut()
    }
}
