package com.android.purebilibili.core.store

import com.android.purebilibili.core.theme.AndroidNativeVariant
import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidNativeVariantSettingsPolicyTest {

    @Test
    fun nullPreferenceValue_defaultsToMaterial3Variant() {
        assertEquals(
            AndroidNativeVariant.MATERIAL3,
            resolveAndroidNativeVariantPreferenceValue(null)
        )
    }

    @Test
    fun invalidPreferenceValue_fallsBackToMaterial3Variant() {
        assertEquals(
            AndroidNativeVariant.MATERIAL3,
            resolveAndroidNativeVariantPreferenceValue(99)
        )
    }

    @Test
    fun persistedValue_restoresMatchingVariant() {
        assertEquals(
            AndroidNativeVariant.MATERIAL3,
            resolveAndroidNativeVariantPreferenceValue(AndroidNativeVariant.MATERIAL3.value)
        )
        assertEquals(
            AndroidNativeVariant.MIUIX,
            resolveAndroidNativeVariantPreferenceValue(AndroidNativeVariant.MIUIX.value)
        )
    }
}
