package com.android.purebilibili.core.ui.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/** Flutter TabController's ease combined with TabBar's elastic edge interpolation. */
object AppTabIndicatorMotion {
    const val DurationMillis = 300
    val ease = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
    val decelerate = Easing { fraction ->
        kotlin.math.sin(ease.transform(fraction) * Math.PI.toFloat() / 2f)
    }
    val accelerate = Easing { fraction ->
        1f - kotlin.math.cos(ease.transform(fraction) * Math.PI.toFloat() / 2f)
    }
}
