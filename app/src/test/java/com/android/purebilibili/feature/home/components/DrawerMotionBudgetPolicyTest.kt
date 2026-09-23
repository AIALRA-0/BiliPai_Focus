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
        // Motion budget only lowers blur quality; it must not delay enabling blur
        // until the drawer settles (that looked like blur "kicking in late").
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
    fun drawerTransition_forcesLowBlurBudget() {
        assertTrue(shouldForceLowDrawerBlurBudget(DrawerMotionBudget.REDUCED))
        assertFalse(shouldForceLowDrawerBlurBudget(DrawerMotionBudget.FULL))
    }
}
