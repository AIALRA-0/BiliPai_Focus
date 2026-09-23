package com.android.purebilibili.navigation3

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BiliPaiProgrammaticBackDispatcherTest {
    @Test
    fun dispatcherConsumesRapidRepeatWithoutDispatchingAnotherPop() {
        val dispatcher = BiliPaiProgrammaticBackDispatcher()
        var dispatchedCount = 0
        val callback = { dispatchedCount += 1 }
        dispatcher.register(callback)

        assertTrue(dispatcher.dispatch(nowUptimeMs = 1_000L))
        assertTrue(dispatcher.dispatch(nowUptimeMs = 1_200L))
        assertEquals(1, dispatchedCount)

        assertTrue(dispatcher.dispatch(nowUptimeMs = 1_300L))
        assertEquals(2, dispatchedCount)
    }

    @Test
    fun dispatcherReportsUnhandledOnlyWhenNoHostCallbackIsRegistered() {
        val dispatcher = BiliPaiProgrammaticBackDispatcher()

        assertFalse(dispatcher.dispatch(nowUptimeMs = 1_000L))
    }

    @Test
    fun videoReturnWaitsForItsConfiguredTransitionWindow() {
        val dispatcher = BiliPaiProgrammaticBackDispatcher()
        var dispatchedCount = 0
        dispatcher.register { dispatchedCount += 1 }

        assertTrue(dispatcher.dispatch(nowUptimeMs = 1_000L, debounceWindowMs = 420L))
        assertTrue(dispatcher.dispatch(nowUptimeMs = 1_350L, debounceWindowMs = 420L))
        assertEquals(1, dispatchedCount)
        assertTrue(dispatcher.dispatch(nowUptimeMs = 1_420L, debounceWindowMs = 420L))
        assertEquals(2, dispatchedCount)
    }
}
