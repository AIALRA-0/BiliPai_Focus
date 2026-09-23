package com.android.purebilibili.feature.dynamic

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicPagerPolicyTest {

    @Test
    fun `稳定页索引解析为对应逻辑 Tab`() {
        val visibleTabs = resolveDynamicVisibleTabs(setOf("all", "pgc", "up"))

        assertEquals(2, resolveDynamicSettledLogicalTab(1, visibleTabs))
        assertEquals(4, resolveDynamicSettledLogicalTab(2, visibleTabs))
        assertEquals(null, resolveDynamicSettledLogicalTab(3, visibleTabs))
    }

    @Test
    fun `顶部指示器跟随 Pager 实时偏移并限制在可见页范围`() {
        assertEquals(
            1.25f,
            resolveDynamicPagerIndicatorPosition(
                currentPage = 1,
                currentPageOffsetFraction = 0.25f,
                pageCount = 4
            )
        )
        assertEquals(
            0f,
            resolveDynamicPagerIndicatorPosition(
                currentPage = 0,
                currentPageOffsetFraction = -0.3f,
                pageCount = 4
            )
        )
        assertEquals(
            3f,
            resolveDynamicPagerIndicatorPosition(
                currentPage = 3,
                currentPageOffsetFraction = 0.4f,
                pageCount = 4
            )
        )
    }

    @Test
    fun `Pager 不在组合阶段创建列表状态或切页回顶`() {
        val source = File("src/main/java/com/android/purebilibili/feature/dynamic/DynamicScreen.kt")
            .readText()

        assertFalse(source.contains("derivedStateOf {\n            val tab = visibleTabs.getOrNull(pagerState.currentPage)"))
        assertFalse(source.contains("listStates.getOrPut"))
        assertFalse(source.contains("beyondViewportPageCount = 1"))
        assertFalse(source.contains("activeListState?.scrollToItem(0)"))
        assertTrue(source.contains("snapshotFlow { pagerState.settledPage }"))
        assertTrue(source.contains("val dynamicTabIndicatorPositionProvider = remember(pagerState, visibleTabs)"))
        assertTrue(source.contains("val dynamicTabScrollInProgressProvider = remember(pagerState)"))
        assertTrue(source.contains("currentPageOffsetFraction = pagerState.currentPageOffsetFraction"))
        assertTrue(source.contains("indicatorPositionProvider = dynamicTabIndicatorPositionProvider"))
        assertTrue(source.contains("isScrollInProgressProvider = dynamicTabScrollInProgressProvider"))
    }
}
