package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.theme.AppUiStyle
import kotlin.test.Test
import kotlin.test.assertEquals

class AppearanceUiPresetSegmentPolicyTest {

    @Test
    fun uiStyleSegmentOptions_exposeStableOrder_andUseProvidedLabels() {
        val options = resolveThemeSelectionOptions(
            material3Label = "Material 3",
            miuixLabel = "Miuix",
        )

        assertEquals(
            listOf(AppUiStyle.MATERIAL3, AppUiStyle.MIUIX),
            options.map { it.value }
        )
        assertEquals(
            listOf("Material 3", "Miuix"),
            options.map { it.label }
        )
    }
}
