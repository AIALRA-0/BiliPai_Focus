package com.android.purebilibili.core.ui.blur

import com.android.purebilibili.core.ui.adaptive.MotionTier

enum class BlurSurfaceType {
    HEADER,
    BOTTOM_BAR,
    DRAWER_OR_SHEET,
    OVERLAY,
    GENERIC
}

data class BlurBudget(
    val maxBlurLevel: Int,
    val backgroundAlphaMultiplier: Float,
    val allowRealtime: Boolean
)

fun resolveBlurBudget(
    surfaceType: BlurSurfaceType,
    motionTier: MotionTier,
    isScrolling: Boolean,
    isTransitionRunning: Boolean,
    forceLowBudget: Boolean = false
): BlurBudget {
    var maxBlurLevel = when (surfaceType) {
        BlurSurfaceType.HEADER -> 2
        BlurSurfaceType.DRAWER_OR_SHEET -> 2
        BlurSurfaceType.BOTTOM_BAR -> 1
        BlurSurfaceType.OVERLAY -> 1
        BlurSurfaceType.GENERIC -> 1
    }
    var backgroundAlphaMultiplier = when (surfaceType) {
        BlurSurfaceType.HEADER -> 1.0f
        BlurSurfaceType.DRAWER_OR_SHEET -> 1.0f
        BlurSurfaceType.BOTTOM_BAR -> 0.95f
        BlurSurfaceType.OVERLAY -> 0.92f
        BlurSurfaceType.GENERIC -> 0.95f
    }
    var allowRealtime = true

    when (motionTier) {
        MotionTier.Reduced -> {
            maxBlurLevel = 0
            backgroundAlphaMultiplier *= 0.9f
            allowRealtime = false
        }

        MotionTier.Normal -> Unit
        MotionTier.Enhanced -> Unit
    }

    if (isScrolling || isTransitionRunning) {
        // 保持视觉稳定：各表面在滑动与转场期间保持模糊材质等级与透明度恒定，
        // 杜绝以往强切 0 级模糊导致的明暗跳跃与视觉割裂（Pulsing / Popping）。
        allowRealtime = false
    }

    if (forceLowBudget) {
        maxBlurLevel = 0
        backgroundAlphaMultiplier *= 0.9f
        allowRealtime = false
    }

    return BlurBudget(
        maxBlurLevel = maxBlurLevel.coerceIn(0, 2),
        backgroundAlphaMultiplier = backgroundAlphaMultiplier.coerceIn(0.70f, 1.10f),
        allowRealtime = allowRealtime
    )
}


fun resolveBlurInputScale(
    budget: BlurBudget,
    surfaceType: BlurSurfaceType
): Float {
    if (budget.allowRealtime) return 1f
    return when (surfaceType) {
        BlurSurfaceType.HEADER -> 0.88f
        BlurSurfaceType.DRAWER_OR_SHEET -> 0.84f
        BlurSurfaceType.BOTTOM_BAR -> 0.82f
        BlurSurfaceType.OVERLAY -> 0.84f
        BlurSurfaceType.GENERIC -> 0.84f
    }
}
