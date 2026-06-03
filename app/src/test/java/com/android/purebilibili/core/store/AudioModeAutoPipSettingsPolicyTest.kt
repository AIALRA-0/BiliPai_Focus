package com.android.purebilibili.core.store

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AudioModeAutoPipSettingsPolicyTest {

    @Test
    fun `audio mode auto pip toggle is enabled for modes that support system pip`() {
        assertFalse(SettingsManager.shouldEnableAudioModeAutoPipToggle(SettingsManager.MiniPlayerMode.OFF))
        assertFalse(SettingsManager.shouldEnableAudioModeAutoPipToggle(SettingsManager.MiniPlayerMode.IN_APP_ONLY))
        assertTrue(SettingsManager.shouldEnableAudioModeAutoPipToggle(SettingsManager.MiniPlayerMode.SYSTEM_PIP))
        assertTrue(SettingsManager.shouldEnableAudioModeAutoPipToggle(SettingsManager.MiniPlayerMode.IN_APP_AND_SYSTEM_PIP))
    }
}
