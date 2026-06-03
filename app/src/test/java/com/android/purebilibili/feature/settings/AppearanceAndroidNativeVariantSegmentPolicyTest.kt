package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.theme.AndroidNativeVariant
import kotlin.test.Test
import kotlin.test.assertEquals

class AppearanceAndroidNativeVariantSegmentPolicyTest {

    @Test
    fun androidNativeVariantSegmentOptions_exposeStableOrder_andUseProvidedLabels() {
        val options = resolveAndroidNativeVariantSegmentOptions(
            material3Label = "Material 3",
            miuixLabel = "Miuix"
        )

        assertEquals(
            listOf(
                AndroidNativeVariant.MATERIAL3,
                AndroidNativeVariant.MIUIX
            ),
            options.map { it.value }
        )
        assertEquals(
            listOf("Material 3", "Miuix"),
            options.map { it.label }
        )
    }
}
