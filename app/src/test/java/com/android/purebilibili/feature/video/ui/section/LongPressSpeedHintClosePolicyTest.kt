package com.android.purebilibili.feature.video.ui.section

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LongPressSpeedHintClosePolicyTest {

    @Test
    fun closeButtonHiddenByDefaultPreference() {
        assertFalse(shouldShowLongPressSpeedHintCloseButton(closeButtonEnabled = false))
    }

    @Test
    fun closeButtonVisibleWhenHiddenSwitchEnabled() {
        assertTrue(shouldShowLongPressSpeedHintCloseButton(closeButtonEnabled = true))
    }
}
