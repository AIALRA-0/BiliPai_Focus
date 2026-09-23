package com.android.purebilibili.feature.video.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class ModalChildScrollTest {

    @Test
    fun `upward remaining movement is consumed before reaching parent`() {
        assertEquals(-24f, resolveModalRemainingScrollConsumption(availableY = -24f))
    }

    @Test
    fun `downward remaining movement is consumed before reaching parent`() {
        assertEquals(18f, resolveModalRemainingScrollConsumption(availableY = 18f))
    }
}
