package com.android.purebilibili.feature.dynamic.components

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class DynamicUserLiveBadgeVisualSpecTest {
    @Test
    fun keepsCompactBadgeMetrics() {
        assertEquals(3.dp, DynamicUserLiveBadgeVisualSpec.HorizontalSpacing)
        assertEquals(11.dp, DynamicUserLiveBadgeVisualSpec.IndicatorWidth)
        assertEquals(12.dp, DynamicUserLiveBadgeVisualSpec.IndicatorHeight)
    }
}
