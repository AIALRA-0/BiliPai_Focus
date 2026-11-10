package com.android.purebilibili.feature.dynamic

import com.android.purebilibili.core.store.SettingsManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DynamicBottomBarScrollPolicyTest {

    @Test
    fun `only scroll hide mode auto collapses dynamic bottom bar`() {
        assertTrue(
            shouldAutoCollapseDynamicBottomBar(SettingsManager.BottomBarVisibilityMode.SCROLL_HIDE)
        )
        assertFalse(
            shouldAutoCollapseDynamicBottomBar(SettingsManager.BottomBarVisibilityMode.ALWAYS_VISIBLE)
        )
        assertFalse(
            shouldAutoCollapseDynamicBottomBar(SettingsManager.BottomBarVisibilityMode.ALWAYS_HIDDEN)
        )
    }

    @Test
    fun `top of feed always shows bottom bar`() {
        val update = reduceDynamicBottomBarScrollDelta(
            previousState = DynamicBottomBarScrollState(accumulatedY = -200f),
            deltaY = -20f,
            isAtTop = true,
        )

        assertEquals(DynamicBottomBarScrollIntent.SHOW, update.intent)
        assertEquals(DynamicBottomBarScrollState(accumulatedY = 0f), update.state)
    }

    @Test
    fun `sustained downward deltas hide bottom bar`() {
        var state = DynamicBottomBarScrollState()
        var intent: DynamicBottomBarScrollIntent? = null
        repeat(4) {
            val update = reduceDynamicBottomBarScrollDelta(
                previousState = state,
                deltaY = -20f,
                isAtTop = false,
                thresholdPx = 48f,
            )
            state = update.state
            intent = update.intent
        }

        assertEquals(DynamicBottomBarScrollIntent.HIDE, intent)
        assertEquals(DynamicBottomBarScrollState(accumulatedY = 0f), state)
    }

    @Test
    fun `sustained upward deltas show bottom bar`() {
        var state = DynamicBottomBarScrollState()
        var intent: DynamicBottomBarScrollIntent? = null
        repeat(4) {
            val update = reduceDynamicBottomBarScrollDelta(
                previousState = state,
                deltaY = 20f,
                isAtTop = false,
                thresholdPx = 48f,
            )
            state = update.state
            intent = update.intent
        }

        assertEquals(DynamicBottomBarScrollIntent.SHOW, intent)
    }

    @Test
    fun `small deltas under threshold do not toggle`() {
        val update = reduceDynamicBottomBarScrollDelta(
            previousState = DynamicBottomBarScrollState(accumulatedY = -30f),
            deltaY = -10f,
            isAtTop = false,
            thresholdPx = 48f,
        )

        assertNull(update.intent)
        assertEquals(DynamicBottomBarScrollState(accumulatedY = -40f), update.state)
    }

    @Test
    fun `direction reverse restarts accumulation`() {
        val reversed = reduceDynamicBottomBarScrollDelta(
            previousState = DynamicBottomBarScrollState(accumulatedY = -40f),
            deltaY = 15f,
            isAtTop = false,
            thresholdPx = 48f,
        )

        assertNull(reversed.intent)
        assertEquals(DynamicBottomBarScrollState(accumulatedY = 15f), reversed.state)
    }

    @Test
    fun `zero delta keeps prior state`() {
        val previous = DynamicBottomBarScrollState(accumulatedY = -12f)
        val update = reduceDynamicBottomBarScrollDelta(
            previousState = previous,
            deltaY = 0f,
            isAtTop = false,
        )

        assertNull(update.intent)
        assertEquals(previous, update.state)
    }
}
