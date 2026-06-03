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
    fun blurWaitsUntilDrawerTransitionSettles() {
        assertFalse(
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
    }

    @Test
    fun drawerTransition_forcesLowBlurBudget() {
        assertTrue(shouldForceLowDrawerBlurBudget(DrawerMotionBudget.REDUCED))
        assertFalse(shouldForceLowDrawerBlurBudget(DrawerMotionBudget.FULL))
    }
}
