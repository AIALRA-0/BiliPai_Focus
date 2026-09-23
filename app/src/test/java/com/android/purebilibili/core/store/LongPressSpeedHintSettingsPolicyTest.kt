package com.android.purebilibili.core.store

import kotlin.test.Test
import kotlin.test.assertEquals

class LongPressSpeedHintSettingsPolicyTest {

    @Test
    fun `hint scale clamps to supported range`() {
        assertEquals(LONG_PRESS_SPEED_HINT_SCALE_MIN, normalizeLongPressSpeedHintScale(0.5f))
        assertEquals(1.0f, normalizeLongPressSpeedHintScale(1.0f))
        assertEquals(1.2f, normalizeLongPressSpeedHintScale(1.2f))
        assertEquals(LONG_PRESS_SPEED_HINT_SCALE_MAX, normalizeLongPressSpeedHintScale(2.0f))
        assertEquals(LONG_PRESS_SPEED_HINT_DEFAULT_SCALE, normalizeLongPressSpeedHintScale(Float.NaN))
    }

    @Test
    fun `hint alpha clamps to supported range`() {
        assertEquals(LONG_PRESS_SPEED_HINT_ALPHA_MIN, normalizeLongPressSpeedHintAlpha(0.1f))
        assertEquals(0.5f, normalizeLongPressSpeedHintAlpha(0.5f))
        assertEquals(0.8f, normalizeLongPressSpeedHintAlpha(0.8f))
        assertEquals(LONG_PRESS_SPEED_HINT_ALPHA_MAX, normalizeLongPressSpeedHintAlpha(1.5f))
        assertEquals(LONG_PRESS_SPEED_HINT_DEFAULT_ALPHA, normalizeLongPressSpeedHintAlpha(Float.NaN))
    }
}
