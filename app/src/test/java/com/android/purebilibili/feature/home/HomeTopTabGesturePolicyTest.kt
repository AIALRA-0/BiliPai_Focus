package com.android.purebilibili.feature.home

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeTopTabGesturePolicyTest {

    @Test
    fun dragDown_collapsesExpandedTabs() {
        assertEquals(
            HomeTopTabGestureAction.COLLAPSE,
            resolveHomeTopTabGestureAction(
                dragDeltaPx = 60f,
                isCollapsed = false,
                thresholdPx = 40f
            )
        )
    }

    @Test
    fun dragUp_expandsCollapsedTabs() {
        assertEquals(
            HomeTopTabGestureAction.EXPAND,
            resolveHomeTopTabGestureAction(
                dragDeltaPx = -60f,
                isCollapsed = true,
                thresholdPx = 40f
            )
        )
    }

    @Test
    fun smallDrag_keepsCurrentState() {
        assertEquals(
            HomeTopTabGestureAction.NONE,
            resolveHomeTopTabGestureAction(
                dragDeltaPx = 16f,
                isCollapsed = false,
                thresholdPx = 40f
            )
        )
    }

    @Test
    fun collapsedTabs_useSlimHandleHeight() {
        assertEquals(
            12.dp,
            resolveHomeTopTabPresentationHeight(
                expandedHeight = 48.dp,
                isCollapsed = true
            )
        )
    }

    @Test
    fun autoCollapsedTabs_canHideCompletelyWithoutHandle() {
        assertEquals(
            0.dp,
            resolveHomeTopTabPresentationHeight(
                expandedHeight = 48.dp,
                isCollapsed = true,
                collapsedHandleHeight = 0.dp
            )
        )
    }

    @Test
    fun headerOffsetBelowThreshold_autoCollapsesTabs() {
        assertTrue(
            resolveHomeTopTabsAutoCollapsed(
                currentHeaderOffsetPx = -1f,
                isHeaderCollapseEnabled = true
            )
        )
        assertFalse(
            resolveHomeTopTabsAutoCollapsed(
                currentHeaderOffsetPx = 0f,
                isHeaderCollapseEnabled = true
            )
        )
    }
}
