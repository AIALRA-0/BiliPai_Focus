package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.ui.adaptive.MotionTier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsEntranceMotionPolicyTest {

    @Test
    fun animationSettingsCardMotion_stillReflectsDisabledHomeCardEntrance() {
        val tier = resolveAnimationSettingsCardMotionTier(
            baseTier = MotionTier.Enhanced,
            cardAnimationEnabled = false
        )

        assertEquals(MotionTier.Reduced, tier)
    }

    @Test
    fun bottomPagerHost_disablesRootEntrance() {
        assertFalse(
            shouldStartSettingsEntrance(
                entranceEnabled = false,
                navigationTransitionRunning = false,
            )
        )
    }

    @Test
    fun navigationTransitionRunning_delaysRootEntrance() {
        assertFalse(
            shouldStartSettingsEntrance(
                entranceEnabled = true,
                navigationTransitionRunning = true,
            )
        )
    }

    @Test
    fun navigationTransitionFinished_startsRootEntrance() {
        assertTrue(
            shouldStartSettingsEntrance(
                entranceEnabled = true,
                navigationTransitionRunning = false,
            )
        )
    }

    @Test
    fun missingNavigationScope_startsImmediately() {
        val navigationTransitionRunning = false

        assertTrue(
            shouldStartSettingsEntrance(
                entranceEnabled = true,
                navigationTransitionRunning = navigationTransitionRunning,
            )
        )
    }
}
