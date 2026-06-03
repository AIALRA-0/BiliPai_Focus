package com.android.purebilibili.feature.settings

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsPrivacyAuthenticationStructureTest {

    @Test
    fun privacyOptionsUseSeparateSwitchesForHistoryAndContentAuthentication() {
        val settingsSource = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/settings/screen/SettingsScreen.kt"),
            File("src/main/java/com/android/purebilibili/feature/settings/screen/SettingsScreen.kt")
        ).first { it.exists() }.readText()
        val sectionSource = listOf(
            File("app/src/main/java/com/android/purebilibili/feature/settings/ui/SettingsSections.kt"),
            File("src/main/java/com/android/purebilibili/feature/settings/ui/SettingsSections.kt")
        ).first { it.exists() }.readText()

        assertTrue(settingsSource.contains("SettingsManager.setPrivacyModeEnabled(context, enabled)"))
        assertTrue(settingsSource.contains("SettingsManager.setPrivacyContentAuthenticationEnabled(context, enabled)"))
        assertTrue(sectionSource.contains("title = \"不记录历史\""))
        assertTrue(sectionSource.contains("title = \"进入隐私内容时验证\""))
        assertFalse(settingsSource.contains("onPrivacyAuthenticationRequired"))
        assertFalse(settingsSource.contains("PrivacyAuthenticationReason.ENABLE_PRIVACY_MODE"))
        assertFalse(settingsSource.contains("PrivacyAuthenticationReason.DISABLE_PRIVACY_MODE"))
    }
}
