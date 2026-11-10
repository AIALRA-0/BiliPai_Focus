package com.bytedance.danmaku.render.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class RenderFrameCadenceTest {
    @Test
    fun `native refresh cadence has no skipped vsyncs on 60 90 120 and 144Hz`() {
        listOf(60, 90, 120, 144).forEach { hz ->
            val cadence = RenderFrameCadence()
            var rendered = 0
            repeat(hz * 10) { tick ->
                val time = tick * 1_000_000_000L / hz
                if (cadence.shouldRender(time)) {
                    cadence.onFrame(time)
                    rendered++
                }
            }
            assertEquals(hz * 10, rendered, "display=$hz Hz")
        }
    }

    @Test
    fun `duplicate callbacks are suppressed and resume has no frame debt`() {
        val cadence = RenderFrameCadence()
        cadence.onFrame(1_000_000_000L)
        assertFalse(cadence.shouldRender(1_000_000_000L))
        assertTrue(cadence.shouldRender(1_008_333_333L))
        cadence.onFrame(9_000_000_000L)
        assertFalse(cadence.shouldRender(9_000_000_000L))
        cadence.reset()
        assertTrue(cadence.shouldRender(9_000_000_000L))
    }
}
