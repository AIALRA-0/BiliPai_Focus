package com.android.purebilibili.core.store

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupAnimationStyleTest {
    @Test
    fun olderAndUnknownValuesKeepFlyout() {
        assertEquals(StartupAnimationStyle.ICON_FLYOUT, StartupAnimationStyle.fromValue(null))
        assertEquals(StartupAnimationStyle.ICON_FLYOUT, StartupAnimationStyle.fromValue("future_style"))
        StartupAnimationStyle.entries.forEach {
            assertEquals(it, StartupAnimationStyle.fromValue(it.value))
        }
    }

    @Test
    fun maidRequiresColdStartAgreementAndAnimationSwitch() {
        val style = StartupAnimationStyle.BLUE_SNOW_MAID
        assertTrue(shouldShowMaidStartup(true, true, true, style))
        assertFalse(shouldShowMaidStartup(false, true, true, style))
        assertFalse(shouldShowMaidStartup(true, false, true, style))
        assertFalse(shouldShowMaidStartup(true, true, false, style))
        assertFalse(shouldShowMaidStartup(true, true, true, StartupAnimationStyle.ICON_FLYOUT))
    }
}
