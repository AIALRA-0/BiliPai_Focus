package com.android.purebilibili.feature.home.components

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScrollCompactNavigationPolicyTest {
    @Test
    fun expandedDockDoesNotClipTheOriginalPressAndDragOverflow() {
        assertFalse(shouldClipScrollCompactNavigation(0f))
        assertFalse(shouldClipScrollCompactNavigation(-0.02f))
        assertTrue(shouldClipScrollCompactNavigation(0.5f))
        assertTrue(shouldClipScrollCompactNavigation(1f))
    }

    @Test
    fun playbackAndItsExitAlwaysKeepOriginalDockOwnership() {
        for (enabled in listOf(false, true)) {
            for (hasAudio in listOf(false, true)) {
                for (retainingAudio in listOf(false, true)) {
                    assertEquals(enabled && !hasAudio && !retainingAudio,
                        shouldUseScrollCompactNavigation(enabled, hasAudio, retainingAudio))
                }
            }
        }
        // Absent -> playing -> exiting -> absent: only the empty endpoints allow compacting.
        assertTrue(shouldUseScrollCompactNavigation(true, false, false))
        assertEquals(false, shouldUseScrollCompactNavigation(true, true, false))
        assertEquals(false, shouldUseScrollCompactNavigation(true, false, true))
        assertTrue(shouldUseScrollCompactNavigation(true, false, false))
    }

    @Test
    fun directionThresholdIgnoresJitterAndReversesOnFullUpwardTravel() {
        var distance = 0f
        repeat(5) { distance = accumulateDockScroll(distance, 4f) }
        assertNull(resolveScrollCompactIntent(distance, 24f))
        distance = accumulateDockScroll(distance, 4f)
        assertEquals(true, resolveScrollCompactIntent(distance, 24f))
        distance = accumulateDockScroll(distance, -3f)
        assertNull(resolveScrollCompactIntent(distance, 24f))
        distance = accumulateDockScroll(distance, -21f)
        assertEquals(false, resolveScrollCompactIntent(distance, 24f))
    }

    @Test
    fun compactEndpointIsCircularAtAllWindowWidths() {
        for (width in listOf(120, 240, 360, 520)) {
            val button = 56
            assertEquals(width, resolveScrollCompactExtent(width, button, 0f))
            assertEquals(button, resolveScrollCompactExtent(width, button, 1f))
            assertEquals(button, resolveScrollCompactExtent(64, button, 1f))
            var previous = width
            repeat(101) { step ->
                val extent = resolveScrollCompactExtent(width, button, step / 100f)
                assertTrue(extent in button..previous)
                previous = extent
            }
        }
    }
}
