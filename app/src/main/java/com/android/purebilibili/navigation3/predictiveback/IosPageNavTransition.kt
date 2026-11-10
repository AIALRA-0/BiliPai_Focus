// SPDX-License-Identifier: GPL-3.0-only
package com.android.purebilibili.navigation3.predictiveback

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.nav.transition.NavMotion
import top.yukonga.miuix.kmp.nav.transition.NavSettleSpec
import top.yukonga.miuix.kmp.nav.transition.NavTransition
import top.yukonga.miuix.kmp.nav.transition.navGraphicsTransition

internal fun iosPageTranslationFraction(depth: Float, isRtl: Boolean): Float {
    val direction = if (isRtl) -1f else 1f
    return direction * if (depth <= 0f) -depth.coerceIn(-1f, 0f) else -0.33f * depth.coerceIn(0f, 1f)
}

/** 新页侧滑并带动后页视差；返回及取消共用进度。 */
internal val IosPageNavTransition: NavTransition = navGraphicsTransition(
    motion = NavMotion(
        programmatic = NavSettleSpec.Tween(360, CubicBezierEasing(0.32f, 0.72f, 0f, 1f)),
        commit = NavSettleSpec.Tween(240, CubicBezierEasing(0.32f, 0.72f, 0f, 1f), matchReleaseVelocity = true),
        cancel = NavSettleSpec.Spring(stiffness = 1500f),
    ),
    scrim = { scope -> coverProgress(scope.relativeDepth) * 0.16f },
) { scope ->
    val depth = scope.relativeDepth
    val width = scope.layoutSize.width.toFloat()
    translationX = snapTranslationToPixelEdge(
        iosPageTranslationFraction(depth, scope.layoutDirection == LayoutDirection.Rtl) * width,
        1f,
        width,
    )
    if (depth <= 0f) {
        val progress = topProgress(depth)
        // 阴影只在两页交接时出现，首尾均回到零。
        shadowElevation = with(scope.density) { 12.dp.toPx() } * 4f * progress * (1f - progress)
    }
}
