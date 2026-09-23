package com.android.purebilibili.core.ui.motion

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically

enum class VerticalContentRevealMode {
    DefaultExpand,
    FloatUp
}

data class VerticalContentRevealMotionSpec(
    val mode: VerticalContentRevealMode,
    val delayMillis: Int,
    val durationMillis: Int,
    val slideOffsetDp: Float,
    val initialScale: Float
)

fun resolveCommentVerticalContentRevealMotionSpec(): VerticalContentRevealMotionSpec {
    return VerticalContentRevealMotionSpec(
        mode = VerticalContentRevealMode.DefaultExpand,
        delayMillis = 0,
        durationMillis = 0,
        slideOffsetDp = 0f,
        initialScale = 1f
    )
}

fun resolveDetailVerticalContentRevealMotionSpec(
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

fun verticalContentRevealEnterTransition(
    spec: VerticalContentRevealMotionSpec
): EnterTransition {
    return when (spec.mode) {
        VerticalContentRevealMode.DefaultExpand -> expandVertically() + fadeIn()
        VerticalContentRevealMode.FloatUp -> fadeIn()
    }
}

fun verticalContentRevealExitTransition(
    spec: VerticalContentRevealMotionSpec
): ExitTransition {
    return when (spec.mode) {
        VerticalContentRevealMode.DefaultExpand -> shrinkVertically() + fadeOut()
        VerticalContentRevealMode.FloatUp -> fadeOut()
    }
}
