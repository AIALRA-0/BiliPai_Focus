package com.android.purebilibili.core.store

import com.android.purebilibili.core.theme.UiPreset
import kotlin.test.Test
import kotlin.test.assertEquals

class UiPresetSettingsPolicyTest {

    @Test
    fun nullPreferenceValue_defaultsToMd3Preset() {
        assertEquals(
            UiPreset.MD3,
            resolveUiPresetPreferenceValue(null)
        )
    }

    @Test
    fun persistedPreferenceValue_restoresMatchingPreset() {
        assertEquals(
            UiPreset.IOS,
            resolveUiPresetPreferenceValue(UiPreset.IOS.value)
        )
        assertEquals(
            UiPreset.MD3,
            resolveUiPresetPreferenceValue(UiPreset.MD3.value)
        )
    }
}
