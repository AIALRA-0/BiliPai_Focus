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

internal fun shouldForceLowDrawerBlurBudget(
    budget: DrawerMotionBudget
): Boolean = budget == DrawerMotionBudget.REDUCED
