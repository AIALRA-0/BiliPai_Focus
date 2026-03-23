package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.theme.UiPreset
import kotlin.test.Test
import kotlin.test.assertEquals

class AppearanceUiPresetSegmentPolicyTest {

    @Test
    fun uiPresetSegmentOptions_exposeStableOrder_andUseProvidedLabels() {
        val options = resolveUiPresetSegmentOptions(
            iosLabel = "iOS",
            androidNativeLabel = "Android Native"
        )

        assertEquals(
            listOf(UiPreset.IOS, UiPreset.MD3),
            options.map { it.value }
        )
        assertEquals(
            listOf("iOS", "Android Native"),
            options.map { it.label }
        )
    }
}
