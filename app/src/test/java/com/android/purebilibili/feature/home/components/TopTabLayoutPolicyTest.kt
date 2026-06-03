package com.android.purebilibili.feature.home.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopTabLayoutPolicyTest {

    @Test
    fun `visible slot count should stay in compact range`() {
        assertEquals(1, resolveTopTabVisibleSlots(1))
        assertEquals(3, resolveTopTabVisibleSlots(3))
        assertEquals(4, resolveTopTabVisibleSlots(4))
        assertEquals(5, resolveTopTabVisibleSlots(5, longestLabelLength = 6))
        assertEquals(6, resolveTopTabVisibleSlots(6, longestLabelLength = 2))
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
    fun `ios top tab action shares centered slot with visible categories`() {
        assertEquals(1, resolveTopTabVisibleCategorySlots(1, longestLabelLength = 2))
        assertEquals(3, resolveTopTabVisibleCategorySlots(3, longestLabelLength = 2))
        assertEquals(5, resolveTopTabVisibleCategorySlots(5, longestLabelLength = 6))
        assertEquals(150f, resolveTopTabActionSlotWidthDp(600f, 3, longestLabelLength = 2), 0.001f)
        assertEquals(100f, resolveTopTabActionSlotWidthDp(600f, 5, longestLabelLength = 6), 0.001f)
        assertEquals(100f, resolveTopTabItemWidthDp(500f, 5, isFloatingStyle = false), 0.001f)
    }

    @Test
    fun `md3 top tabs use compact scrollable item widths instead of fixed four slots`() {
        assertEquals(3, resolveMd3TopTabVisibleSlots())
        assertEquals(96f, resolveMd3TopTabItemWidthDp(containerWidthDp = 320f), 0.001f)
        assertEquals(108f, resolveMd3TopTabItemWidthDp(containerWidthDp = 360f), 0.001f)
        assertEquals(120f, resolveMd3TopTabItemWidthDp(containerWidthDp = 640f), 0.001f)
        assertEquals(60f, resolveMd3TopTabItemWidthDp(containerWidthDp = 360f, visibleSlots = 6), 0.001f)
    }

    @Test
    fun `md3 top tabs show all tabs for every label mode when partition is an inline page`() {
        assertEquals(
            6,
            resolveMd3TopTabLayoutVisibleSlots(
                categoryCount = 6,
                labelMode = 2,
                showPartitionAction = false
            )
        )
        assertEquals(
            5,
            resolveMd3TopTabLayoutVisibleSlots(
                categoryCount = 5,
                labelMode = 2,
                showPartitionAction = false
            )
        )
        assertEquals(
            4,
            resolveMd3TopTabLayoutVisibleSlots(
                categoryCount = 4,
                labelMode = 2,
                showPartitionAction = false
            )
        )
        assertEquals(
            6,
            resolveMd3TopTabLayoutVisibleSlots(
                categoryCount = 6,
                labelMode = 0,
                showPartitionAction = false
            )
        )
        assertEquals(
            6,
            resolveMd3TopTabLayoutVisibleSlots(
                categoryCount = 6,
                labelMode = 1,
                showPartitionAction = false
            )
        )
    }

    @Test
    fun `md3 top tabs cap expanded custom tabs at six visible slots`() {
        assertEquals(
            6,
            resolveMd3TopTabLayoutVisibleSlots(
                categoryCount = 8,
                labelMode = 0,
                showPartitionAction = false
            )
        )
        assertEquals(
            6,
            resolveMd3TopTabLayoutVisibleSlots(
                categoryCount = 8,
                labelMode = 2,
                showPartitionAction = false
            )
        )
    }

    @Test
    fun `md3 top tabs keep compact scrollable slots for external partition action`() {
        assertEquals(
            3,
            resolveMd3TopTabLayoutVisibleSlots(
                categoryCount = 6,
                labelMode = 2,
                showPartitionAction = true
            )
        )
    }

    @Test
    fun `ios top tabs reserve enough height for icon label modes`() {
        assertEquals(52f, resolveIosTopTabRowHeight(isFloatingStyle = true, labelMode = 2).value, 0.001f)
        assertEquals(52f, resolveIosTopTabRowHeight(isFloatingStyle = true, labelMode = 1).value, 0.001f)
        assertEquals(58f, resolveIosTopTabRowHeight(isFloatingStyle = true, labelMode = 0).value, 0.001f)
        assertEquals(56f, resolveIosTopTabRowHeight(isFloatingStyle = false, labelMode = 0).value, 0.001f)
    }

    @Test
    fun `top tab item content policy avoids clipping icon plus text`() {
        assertEquals(42f, resolveTopTabContentMinHeightDp(labelMode = 0), 0.001f)
        assertEquals(36f, resolveTopTabContentMinHeightDp(labelMode = 1), 0.001f)
        assertEquals(36f, resolveTopTabContentMinHeightDp(labelMode = 2), 0.001f)
        assertEquals(2f, resolveTopTabContentVerticalPaddingDp(labelMode = 0), 0.001f)
        assertEquals(4f, resolveTopTabContentVerticalPaddingDp(labelMode = 1), 0.001f)
        assertEquals(4f, resolveTopTabContentVerticalPaddingDp(labelMode = 2), 0.001f)
    }

    @Test
    fun `md3 top tabs keep every category in scroll order`() {
        assertEquals(
            listOf(0, 1, 2, 3, 4),
            resolveMd3VisibleTabIndices(totalCount = 5, selectedIndex = 0)
        )
        assertEquals(
            listOf(0, 1, 2, 3, 4),
            resolveMd3VisibleTabIndices(totalCount = 5, selectedIndex = 4)
        )
        assertEquals(
            4,
            resolveMd3SelectedVisibleIndex(
                visibleIndices = listOf(0, 1, 2, 3, 4),
                selectedIndex = 4
            )
        )
    }

    @Test
    fun `miuix top tabs render at most four complete labels without shifting tail target to front`() {
        assertEquals(
            listOf(0, 1, 2, 3),
            resolveMiuixVisibleTabIndices(totalCount = 5, selectedIndex = 0)
        )
        assertEquals(
            listOf(0, 1, 2, 3),
            resolveMiuixVisibleTabIndices(totalCount = 5, selectedIndex = 3)
        )
        assertEquals(
            listOf(0, 1, 2, 4),
            resolveMiuixVisibleTabIndices(totalCount = 5, selectedIndex = 4)
        )
        assertEquals(
            3,
            resolveMiuixSelectedVisibleIndex(
                visibleIndices = listOf(0, 1, 2, 4),
                selectedIndex = 4
            )
        )
        assertEquals(
            listOf(0, 1, 2),
            resolveMiuixVisibleTabIndices(totalCount = 3, selectedIndex = 2)
        )
    }
}
