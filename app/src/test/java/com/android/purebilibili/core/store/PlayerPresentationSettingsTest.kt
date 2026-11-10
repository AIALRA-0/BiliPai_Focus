package com.android.purebilibili.core.store

import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerPresentationSettingsTest {


    @Test
    fun `invalid bottom progress behavior falls back to hidden`() {
        assertEquals(
            BottomProgressBehavior.ALWAYS_HIDE,
            BottomProgressBehavior.fromValue(Int.MAX_VALUE)
        )
    }

}
