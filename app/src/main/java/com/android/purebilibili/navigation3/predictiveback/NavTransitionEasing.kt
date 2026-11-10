// SPDX-License-Identifier: GPL-3.0-only
package com.android.purebilibili.navigation3.predictiveback

import android.graphics.Path
import android.view.animation.PathInterpolator
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/**
 * 系统「快出特慢入」曲线。路径数据逐字取自框架 interpolator 资源
 * android.R.interpolator.fast_out_extra_slow_in 的 pathData（分段贝塞尔），
 * 预测返回 commit 与跨 Activity 经典动效因此与系统动画同曲线。
 * 资源加载需要 Context，而本文件在顶层 val 中被引用，故以同源路径数据经
 * PathInterpolator 构造；两者对同一 fraction 逐点等值。
 */
internal val FastOutExtraSlowIn: Easing = Easing { fraction ->
    FastOutExtraSlowInInterpolator.getInterpolation(fraction)
}

private val FastOutExtraSlowInInterpolator = PathInterpolator(
    Path().apply {
        moveTo(0f, 0f)
        cubicTo(0.05f, 0f, 0.133333f, 0.06f, 0.166666f, 0.4f)
        cubicTo(0.208333f, 0.82f, 0.25f, 1f, 1f, 1f)
    }
)

internal val BackGestureEasing: Easing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)

internal fun topProgress(depth: Float): Float = (1f + depth).coerceIn(0f, 1f)

internal fun coverProgress(depth: Float): Float = depth.coerceIn(0f, 1f)
