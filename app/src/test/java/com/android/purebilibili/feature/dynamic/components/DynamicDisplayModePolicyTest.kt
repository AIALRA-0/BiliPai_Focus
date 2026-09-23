package com.android.purebilibili.feature.dynamic.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicDisplayModePolicyTest {

    @Test
    fun displayModesCoverAllUpPanelPositions() {
        assertEquals(5, DynamicDisplayMode.entries.size)
        assertTrue(DynamicDisplayMode.SIDEBAR.isFixedSidebar())
        assertTrue(DynamicDisplayMode.SIDEBAR_RIGHT.isFixedSidebar())
        assertTrue(DynamicDisplayMode.SIDEBAR_RIGHT.isRightAligned())
        assertTrue(DynamicDisplayMode.DRAWER_RIGHT.isDrawer())
        assertTrue(DynamicDisplayMode.HORIZONTAL.isHorizontalUserList())
        assertFalse(DynamicDisplayMode.HORIZONTAL.isFixedSidebar())
        assertEquals("右侧竖条", resolveDynamicDisplayModeLabel(DynamicDisplayMode.SIDEBAR_RIGHT))
        assertEquals("左侧抽屉", resolveDynamicDisplayModeLabel(DynamicDisplayMode.DRAWER_LEFT))
    }
}
