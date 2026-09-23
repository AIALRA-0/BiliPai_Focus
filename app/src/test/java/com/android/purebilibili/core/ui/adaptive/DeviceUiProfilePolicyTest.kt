package com.android.purebilibili.core.ui.adaptive

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeviceUiProfilePolicyTest {

    @Test
    fun expandedTabletUsesNormalMotionTier() {
        val profile = resolveDeviceUiProfileSpec(
            widthClass = AdaptiveWidthClass.Expanded,
        )

        assertEquals(MotionTier.Normal, profile.motionTier)
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
    fun largeTabletPrefersEnhancedMotionTier() {
        val profile = resolveDeviceUiProfileSpec(
            widthClass = AdaptiveWidthClass.Large,
        )

        assertEquals(MotionTier.Enhanced, profile.motionTier)
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

    @Test
    fun bookPostureForcesReducedMotionTier() {
        val profile = resolveDeviceUiProfileSpec(
            widthClass = AdaptiveWidthClass.Expanded,
            foldPosture = AdaptiveFoldPosture.Book,
        )

        assertEquals(MotionTier.Reduced, profile.motionTier)
        assertEquals(AdaptiveFoldPosture.Book, profile.foldPosture)
    }

    @Test
    fun tabletopPostureForcesReducedMotionTier() {
        val profile = resolveDeviceUiProfileSpec(
            widthClass = AdaptiveWidthClass.Large,
            foldPosture = AdaptiveFoldPosture.Tabletop,
        )

        assertEquals(MotionTier.Reduced, profile.motionTier)
        assertEquals(AdaptiveFoldPosture.Tabletop, profile.foldPosture)
    }

    @Test
    fun flatPostureKeepsBaseMotionTier() {
        val profile = resolveDeviceUiProfileSpec(
            widthClass = AdaptiveWidthClass.Expanded,
            foldPosture = AdaptiveFoldPosture.Flat,
        )

        assertEquals(MotionTier.Normal, profile.motionTier)
        assertEquals(AdaptiveFoldPosture.Flat, profile.foldPosture)
    }
}
