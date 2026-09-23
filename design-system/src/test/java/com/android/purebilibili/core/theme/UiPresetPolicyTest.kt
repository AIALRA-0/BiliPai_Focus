package com.android.purebilibili.core.theme

import kotlin.test.Test
import kotlin.test.assertEquals

class UiPresetPolicyTest {

    @Test
    fun unknownPresetValue_fallsBackToMd3() {
        assertEquals(UiPreset.MD3, UiPreset.fromValue(99))
    }
}
