package com.android.purebilibili.core.ui.adaptive

import com.android.purebilibili.core.util.WindowWidthSizeClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DeviceUiProfileTest {

    @Test
    fun expandedTablet_usesNormalMotionTier() {
        val profile = resolveDeviceUiProfile(
            widthSizeClass = WindowWidthSizeClass.Expanded
        )

        assertEquals(MotionTier.Normal, profile.motionTier)
        assertEquals(true, profile.isTablet)
    }

    @Test
    fun largeTablet_prefersEnhancedMotionTier() {
        val profile = resolveDeviceUiProfile(
            widthSizeClass = WindowWidthSizeClass.Large
        )

        assertEquals(MotionTier.Enhanced, profile.motionTier)
        assertEquals(true, profile.isTablet)
    }

    @Test
    fun mediumTablet_usesNormalMotionTier() {
        val profile = resolveDeviceUiProfile(
            widthSizeClass = WindowWidthSizeClass.Medium
        )

        assertEquals(MotionTier.Normal, profile.motionTier)
        assertEquals(true, profile.isTablet)
    }

    @Test
    fun compactPhone_usesNormalMotionAndNotTablet() {
        val profile = resolveDeviceUiProfile(
            widthSizeClass = WindowWidthSizeClass.Compact
        )

        assertEquals(MotionTier.Normal, profile.motionTier)
        assertFalse(profile.isTablet)
    }
}
