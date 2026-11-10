package com.android.purebilibili.feature.home.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DrawerMotionBudgetPolicyTest {

    @Test
    fun drawerTransition_reducesMotionBudget() {
        assertEquals(
            DrawerMotionBudget.REDUCED,
            resolveDrawerMotionBudget(isDrawerTransitionRunning = true)
        )
        assertEquals(
            DrawerMotionBudget.FULL,
            resolveDrawerMotionBudget(isDrawerTransitionRunning = false)
        )
    }

    @Test
    fun blurStaysEnabledDuringDrawerTransition() {
        // 开合不能延后启用模糊，也不能切换采样网格。
        assertTrue(
            shouldEnableDrawerBlur(
                blurActive = true,
                budget = DrawerMotionBudget.REDUCED
            )
        )
        assertTrue(
            shouldEnableDrawerBlur(
                blurActive = true,
                budget = DrawerMotionBudget.FULL
            )
        )
        assertFalse(
            shouldEnableDrawerBlur(
                blurActive = false,
                budget = DrawerMotionBudget.FULL
            )
        )
    }

    @Test
    fun drawerTransition_keepsBlurAndRefractionBudgetStable() {
        assertFalse(shouldForceLowDrawerBlurBudget(DrawerMotionBudget.REDUCED))
        assertFalse(shouldForceLowDrawerBlurBudget(DrawerMotionBudget.FULL))
    }
}
