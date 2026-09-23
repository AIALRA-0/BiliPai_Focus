package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.theme.AppUiStyle
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsLanguageStateTest {

    @Test
    fun settingsUiState_defaultsToMiuixStyle() {
        assertEquals(AppUiStyle.MIUIX, SettingsUiState().themeSelection)
    }

    @Test
    fun settingsUiState_preservesExplicitUiStyle() {
        assertEquals(
            AppUiStyle.MIUIX,
            SettingsUiState(themeSelection = AppUiStyle.MIUIX).themeSelection,
        )
    }

    @Test
    fun settingsUiState_defaultsToFollowSystemLanguage() {
        assertEquals(
            AppLanguage.FOLLOW_SYSTEM,
            SettingsUiState().appLanguage
        )
    }

    @Test
    fun settingsUiState_preservesExplicitLanguageSelection() {
        val state = SettingsUiState(appLanguage = AppLanguage.ENGLISH)

        assertEquals(AppLanguage.ENGLISH, state.appLanguage)
    }
}
