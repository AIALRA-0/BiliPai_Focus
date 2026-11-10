package com.android.purebilibili.feature.home.components

internal enum class DrawerMotionBudget {
    FULL,
    REDUCED
}

internal fun resolveDrawerMotionBudget(
    isDrawerTransitionRunning: Boolean
): DrawerMotionBudget {
    return if (isDrawerTransitionRunning) DrawerMotionBudget.REDUCED else DrawerMotionBudget.FULL
}

internal fun shouldEnableDrawerBlur(
    blurActive: Boolean,
    budget: DrawerMotionBudget
): Boolean = blurActive

@Suppress("UNUSED_PARAMETER")
internal fun shouldForceLowDrawerBlurBudget(
    budget: DrawerMotionBudget
): Boolean = false // 开合只影响运动预算，不能在拖动中切换模糊采样与折射材质。
