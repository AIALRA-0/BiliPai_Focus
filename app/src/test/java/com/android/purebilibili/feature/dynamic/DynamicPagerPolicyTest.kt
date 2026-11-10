package com.android.purebilibili.feature.dynamic

import kotlin.test.Test
import kotlin.test.assertEquals

class DynamicPagerPolicyTest {

    @Test
    fun `稳定页索引解析为对应逻辑 Tab`() {
        val visibleTabs = resolveDynamicVisibleTabs(setOf("all", "pgc", "article"))

        assertEquals(2, resolveDynamicSettledLogicalTab(1, visibleTabs))
        assertEquals(3, resolveDynamicSettledLogicalTab(2, visibleTabs))
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

}
