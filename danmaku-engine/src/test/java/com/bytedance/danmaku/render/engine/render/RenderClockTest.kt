package com.bytedance.danmaku.render.engine.render

import kotlin.test.Test
import kotlin.test.assertEquals

class RenderClockTest {
    @Test
    fun elapsedTimeIsIndependentOfFrameRate() {
        for (fps in listOf(15, 30, 60, 90, 120)) {
            val clock = RenderClock()
            clock.resume(1_000L)
            for (frame in 1..fps) {
                clock.advance(1_000L + frame * 1_000L / fps)
            }
            assertEquals(1_000L, clock.timeMs, "fps=$fps")
        }
    }

    @Test
    fun stalledFrameAccountsForTheWholeInterval() {
        val clock = RenderClock()
        clock.resume(1_000L)
        clock.advance(1_008L)
        clock.advance(1_508L)
        assertEquals(508L, clock.timeMs)
    }

    @Test
    fun pauseWithoutAnIntermediateDrawDoesNotCountPausedTime() {
        val clock = RenderClock()
        clock.resume(1_000L)
        clock.advance(1_016L)
        clock.pause(1_020L)
        clock.advance(10_000L)
        clock.pause(10_000L)
        clock.resume(11_000L)
        clock.advance(11_033L)
        assertEquals(53L, clock.timeMs)
    }

    @Test
    fun repeatedResumeAndDrawDoNotLoseOrDuplicateTime() {
        val clock = RenderClock()
        clock.resume(1_000L)
        clock.resume(1_010L)
        clock.advance(1_033L)
        clock.advance(1_033L)
        assertEquals(33L, clock.timeMs)
    }

    @Test
    fun stopAndRestartUseAFreshBaseline() {
        val clock = RenderClock()
        clock.resume(1_000L)
        clock.advance(2_000L)
        clock.reset()
        clock.advance(5_000L)
        assertEquals(0L, clock.timeMs)
        clock.resume(10_000L)
        clock.advance(10_008L)
        assertEquals(8L, clock.timeMs)
    }
}
