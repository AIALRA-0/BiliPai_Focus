package com.android.purebilibili.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppHingeSplitGeometryTest {

    @Test
    fun `non centered hinge includes a sixteen dp safety band on both sides`() {
        val geometry = resolveAppHingeSplitGeometry(
            totalSizePx = 1_000f,
            hingeStartPx = 420f,
            hingeEndPx = 440f,
            clearancePx = 16f,
            fallbackRatio = 0.65f,
        )

        assertEquals(52f, geometry.dividerSizePx)
        assertEquals(404f / 948f, geometry.primaryRatio)
    }

    @Test
    fun `invalid hinge uses safe fallback geometry`() {
        val geometry = resolveAppHingeSplitGeometry(
            totalSizePx = 0f,
            hingeStartPx = 0f,
            hingeEndPx = 0f,
            clearancePx = 16f,
            fallbackRatio = 0.6f,
        )

        assertEquals(0.6f, geometry.primaryRatio)
        assertTrue(geometry.dividerSizePx >= 1f)
    }
}
