package com.android.purebilibili.core.store

import com.android.purebilibili.core.theme.UiPreset
import kotlin.test.Test
import kotlin.test.assertEquals

class UiPresetSettingsPolicyTest {

    @Test
    fun nullPreferenceValue_defaultsToIosPreset() {
        assertEquals(UiPreset.IOS, resolveUiPresetPreferenceValue(null))
    }

    @Test
    fun invalidPreferenceValue_fallsBackToIosPreset() {
        assertEquals(UiPreset.IOS, resolveUiPresetPreferenceValue(99))
    }

    @Test
    fun persistedMd3Value_restoresMd3Preset() {
        assertEquals(UiPreset.MD3, resolveUiPresetPreferenceValue(UiPreset.MD3.value))
        assertEquals(UiPreset.IOS, resolveUiPresetPreferenceValue(UiPreset.IOS.value))
    }
}
