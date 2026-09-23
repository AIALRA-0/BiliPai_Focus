package com.android.purebilibili.feature.home.components.miuix

import kotlin.test.Test
import kotlin.test.assertEquals

class InteractiveHighlightMotionSpecTest {
    @Test
    fun miuixHighlight_preservesItsPressAndPositionPhysics() {
        val press = interactiveHighlightPressSpec()
        val position = interactiveHighlightPositionSpec()

        assertEquals(0.5f, press.dampingRatio)
        assertEquals(300f, press.stiffness)
        assertEquals(0.5f, position.dampingRatio)
        assertEquals(300f, position.stiffness)
    }
}
