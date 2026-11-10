package com.android.purebilibili.core.ui

import com.android.purebilibili.core.store.SettingsManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BottomBarScrollHidePolicyTest {

    @Test
    fun `only scroll hide mode auto collapses bottom bar`() {
        assertTrue(
            shouldAutoHideBottomBarOnScroll(SettingsManager.BottomBarVisibilityMode.SCROLL_HIDE)
        )
        assertFalse(
            shouldAutoHideBottomBarOnScroll(SettingsManager.BottomBarVisibilityMode.ALWAYS_VISIBLE)
        )
        assertFalse(
            shouldAutoHideBottomBarOnScroll(SettingsManager.BottomBarVisibilityMode.ALWAYS_HIDDEN)
        )
    }

    @Test
    fun `top of list always shows bottom bar`() {
        val update = reduceBottomBarScrollHideDelta(
            previousState = BottomBarScrollHideState(accumulatedY = -200f),
            deltaY = -20f,
            isAtTop = true,
        )

        assertEquals(BottomBarScrollHideIntent.SHOW, update.intent)
        assertEquals(BottomBarScrollHideState(accumulatedY = 0f), update.state)
    }

    @Test
    fun `sustained downward deltas hide bottom bar`() {
        var state = BottomBarScrollHideState()
        var intent: BottomBarScrollHideIntent? = null
        repeat(4) {
            val update = reduceBottomBarScrollHideDelta(
                previousState = state,
                deltaY = -20f,
                isAtTop = false,
                thresholdPx = 48f,
            )
            state = update.state
            intent = update.intent
        }

        assertEquals(BottomBarScrollHideIntent.HIDE, intent)
        assertEquals(BottomBarScrollHideState(accumulatedY = 0f), state)
    }

    @Test
    fun `sustained upward deltas show bottom bar`() {
        var state = BottomBarScrollHideState()
        var intent: BottomBarScrollHideIntent? = null
        repeat(4) {
            val update = reduceBottomBarScrollHideDelta(
                previousState = state,
                deltaY = 20f,
                isAtTop = false,
                thresholdPx = 48f,
            )
            state = update.state
            intent = update.intent
        }

        assertEquals(BottomBarScrollHideIntent.SHOW, intent)
    }

    @Test
    fun `small deltas under threshold do not toggle`() {
        val update = reduceBottomBarScrollHideDelta(
            previousState = BottomBarScrollHideState(accumulatedY = -30f),
            deltaY = -10f,
            isAtTop = false,
            thresholdPx = 48f,
        )

        assertNull(update.intent)
        assertEquals(BottomBarScrollHideState(accumulatedY = -40f), update.state)
    }

    @Test
    fun `direction reverse restarts accumulation`() {
        val reversed = reduceBottomBarScrollHideDelta(
            previousState = BottomBarScrollHideState(accumulatedY = -40f),
            deltaY = 15f,
            isAtTop = false,
            thresholdPx = 48f,
        )

        assertNull(reversed.intent)
        assertEquals(BottomBarScrollHideState(accumulatedY = 15f), reversed.state)
    }

    @Test
    fun `zero delta keeps prior state`() {
        val previous = BottomBarScrollHideState(accumulatedY = -12f)
        val update = reduceBottomBarScrollHideDelta(
            previousState = previous,
            deltaY = 0f,
            isAtTop = false,
        )

        assertNull(update.intent)
        assertEquals(previous, update.state)
    }
}
