package com.android.purebilibili.feature.home

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeCategoryLayoutSpecTest {
    @Test
    fun categoryControlsRetainTheirWidths() {
        assertEquals(400.dp, HomeCategoryLayoutSpec.PopularCategoryControlMaxWidth)
        assertEquals(120.dp, HomeCategoryLayoutSpec.TodayWatchModeItemWidth)
    }
}
