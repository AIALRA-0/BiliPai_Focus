package com.android.purebilibili.feature.home.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopTabLayoutPolicyTest {

    @Test
    fun `visible slot count should stay in compact range`() {
        assertEquals(4, resolveTopTabVisibleSlots(1))
        assertEquals(4, resolveTopTabVisibleSlots(4))
        assertEquals(5, resolveTopTabVisibleSlots(5, longestLabelLength = 6))
        assertEquals(4, resolveTopTabVisibleSlots(5, longestLabelLength = 9))
        assertEquals(4, resolveTopTabVisibleSlots(8, longestLabelLength = 10))
    }

    @Test
    fun `floating style should enforce wider min width to avoid clipping`() {
        assertEquals(72f, resolveTopTabItemWidthDp(260f, 5, isFloatingStyle = true), 0.001f)
    }

    @Test
    fun `docked style should keep a denser minimum width`() {
        assertEquals(64f, resolveTopTabItemWidthDp(260f, 5, isFloatingStyle = false), 0.001f)
    }

    @Test
    fun `wide containers should use proportional width`() {
        assertEquals(100f, resolveTopTabItemWidthDp(500f, 5, isFloatingStyle = true), 0.001f)
    }

    @Test
    fun `filtered top tabs should reserve symmetric centered padding`() {
        assertEquals(150f, resolveTopTabCenteredStartPaddingDp(400f, 1, isFloatingStyle = false), 0.001f)
        assertEquals(100f, resolveTopTabCenteredStartPaddingDp(400f, 2, isFloatingStyle = false), 0.001f)
        assertEquals(50f, resolveTopTabCenteredStartPaddingDp(400f, 3, isFloatingStyle = false), 0.001f)
        assertEquals(0f, resolveTopTabCenteredStartPaddingDp(400f, 4, isFloatingStyle = false), 0.001f)
    }

    @Test
    fun `live route decision should follow category key not localized label`() {
        assertTrue(shouldRouteTopTabToLivePage("LIVE"))
        assertTrue(shouldRouteTopTabToLivePage("live"))
        assertFalse(shouldRouteTopTabToLivePage("直播"))
        assertFalse(shouldRouteTopTabToLivePage("RECOMMEND"))
    }

    @Test
    fun `md3 top tabs keep four visible slots on every device width`() {
        assertEquals(4, resolveMd3TopTabVisibleSlots())
        assertEquals(80f, resolveMd3TopTabItemWidthDp(containerWidthDp = 320f), 0.001f)
        assertEquals(90f, resolveMd3TopTabItemWidthDp(containerWidthDp = 360f), 0.001f)
        assertEquals(160f, resolveMd3TopTabItemWidthDp(containerWidthDp = 640f), 0.001f)
    }

    @Test
    fun `md3 top tabs keep selected category within four visible slots`() {
        assertEquals(
            listOf(0, 1, 2, 3),
            resolveMd3VisibleTabIndices(totalCount = 5, selectedIndex = 0)
        )
        assertEquals(
            listOf(0, 1, 2, 4),
            resolveMd3VisibleTabIndices(totalCount = 5, selectedIndex = 4)
        )
        assertEquals(
            3,
            resolveMd3SelectedVisibleIndex(
                visibleIndices = listOf(0, 1, 2, 4),
                selectedIndex = 4
            )
        )
    }

    @Test
    fun `md3 filtered top tabs should stay visually centered`() {
        assertEquals(120f, resolveMd3TopTabCenteredStartPaddingDp(containerWidthDp = 320f, categoryCount = 1), 0.001f)
        assertEquals(80f, resolveMd3TopTabCenteredStartPaddingDp(containerWidthDp = 320f, categoryCount = 2), 0.001f)
        assertEquals(40f, resolveMd3TopTabCenteredStartPaddingDp(containerWidthDp = 320f, categoryCount = 3), 0.001f)
        assertEquals(0f, resolveMd3TopTabCenteredStartPaddingDp(containerWidthDp = 320f, categoryCount = 4), 0.001f)
    }
}
