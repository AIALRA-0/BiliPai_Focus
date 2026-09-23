package com.android.purebilibili.feature.dynamic

import kotlin.test.Test
import kotlin.test.assertEquals

class DynamicScreenShapePolicyTest {

    @Test
    fun `refresh markers preserve their existing corner radii`() {
        assertEquals(24, DynamicScreenShapePolicy.refreshDividerLocatorButtonCornerRadiusDp)
        assertEquals(10, DynamicScreenShapePolicy.oldContentDividerCornerRadiusDp)
    }
}
