package com.android.purebilibili.core.ui.adaptive

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeviceUiProfilePolicyTest {

    @Test
    fun expandedTabletPrefersEnhancedMotionTier() {
        val profile = resolveDeviceUiProfileSpec(
            widthClass = AdaptiveWidthClass.Expanded,
        )

        assertEquals(MotionTier.Enhanced, profile.motionTier)
        assertTrue(profile.isTablet)
    }

    @Test
    fun mediumTabletUsesNormalMotionTier() {
        val profile = resolveDeviceUiProfileSpec(
            widthClass = AdaptiveWidthClass.Medium,
        )

        assertEquals(MotionTier.Normal, profile.motionTier)
        assertTrue(profile.isTablet)
    }

    @Test
    fun compactPhoneUsesNormalMotionAndIsNotTablet() {
        val profile = resolveDeviceUiProfileSpec(
            widthClass = AdaptiveWidthClass.Compact,
        )

        assertEquals(MotionTier.Normal, profile.motionTier)
        assertFalse(profile.isTablet)
    }
}
