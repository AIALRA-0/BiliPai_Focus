package com.android.purebilibili.core.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AppWindowGlassActionGroupsTest {
    @Test
    fun nestedOptionsBecomeOneGlassPanelAndKeepSelectionAndActions() {
        val select: () -> Unit = {}
        val option = AppWindowAction("最新发布", selected = true, onClick = select)
        val groups = listOf(listOf(AppWindowAction("布局", onClick = {}), AppWindowAction("排序", children = listOf(option))))

        val result = flattenGlassWindowActionGroups(groups).single()
        assertEquals(listOf("布局", "排序", "最新发布"), result.map { it.label })
        assertFalse(result[1].enabled)
        assertTrue(result[2].selected)
        assertSame(select, result[2].onClick)
        assertEquals(1, groups.single().last().children.size)
    }

    @Test
    fun disabledParentDisablesAllNestedOptions() {
        val result = flattenGlassWindowActionGroups(listOf(listOf(
            AppWindowAction("画质", enabled = false, children = listOf(AppWindowAction("高清", enabled = true))),
        ))).single()
        assertTrue(result.all { !it.enabled })
    }
}
