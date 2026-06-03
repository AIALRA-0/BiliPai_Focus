package com.android.purebilibili.navigation3

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BiliPaiReturnSessionStateTest {

    @Test
    fun videoSourceRouteIsStoredOutsideCardPositionManager() {
        val state = BiliPaiReturnSessionState()
            .recordVideoSource(
                BiliPaiVideoSource(
                    route = "search",
                    key = "search:BV1"
                )
            )

        assertEquals("search", state.lastVideoSourceRoute)
        assertEquals("search:BV1", state.lastVideoSourceKey)
        assertFalse(state.isReturningFromDetail)
        assertFalse(state.isQuickReturnFromDetail)
    }

    @Test
    fun returnSessionMarksQuickReturnFromElapsedTime() {
        val state = BiliPaiReturnSessionState()
            .markDetailEntered(nowMillis = 1_000L)
            .markReturning(nowMillis = 1_450L)

        assertTrue(state.isReturningFromDetail)
        assertTrue(state.isQuickReturnFromDetail)
    }

    @Test
    fun clearReturningKeepsSourceRouteForNextSharedElementMatch() {
        val state = BiliPaiReturnSessionState()
            .recordVideoSource(
                BiliPaiVideoSource(
                    route = "home",
                    key = "home:BV1"
                )
            )
            .markDetailEntered(nowMillis = 1_000L)
            .markReturning(nowMillis = 2_000L)
            .clearReturning()

        assertEquals("home", state.lastVideoSourceRoute)
        assertEquals("home:BV1", state.lastVideoSourceKey)
        assertFalse(state.isReturningFromDetail)
        assertFalse(state.isQuickReturnFromDetail)
    }

    @Test
    fun legacyRouteRecordingClearsPreviousSourceKey() {
        val state = BiliPaiReturnSessionState()
            .recordVideoSource(
                BiliPaiVideoSource(
                    route = "search",
                    key = "search:BV1"
                )
            )
            .recordVideoSourceRoute("history?from=tab")

        assertEquals("history", state.lastVideoSourceRoute)
        assertEquals(null, state.lastVideoSourceKey)
    }
}
