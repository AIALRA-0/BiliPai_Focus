package com.android.purebilibili.feature.web

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LinkOpenGateTest {
    @Test
    fun pendingResolutionBlocksClicksBeyondTimeWindow() {
        val gate = LinkOpenGate()
        assertTrue(gate.tryAcquire(1000))
        assertFalse(gate.tryAcquire(1001))
        assertFalse(gate.tryAcquire(5000))
        gate.release()
        assertTrue(gate.tryAcquire(5000))
    }

    @Test
    fun completedOpenStillDebouncesButAllowsLaterRetry() {
        val gate = LinkOpenGate()
        assertTrue(gate.tryAcquire(0))
        gate.release()
        assertFalse(gate.tryAcquire(799))
        assertTrue(gate.tryAcquire(800))
    }
}
