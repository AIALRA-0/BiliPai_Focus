package com.android.purebilibili.core.ui.blur

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProgressiveFadeTest {

    @Test
    fun `alpha factors match telegram non-linear easing curve`() {
        val factors = ProgressiveFadeDefaults.ALPHA_FACTORS
        assertEquals(5, factors.size)
        // 1.0f (0xFF), ~0.91f (0xE8), ~0.69f (0xB0), ~0.38f (0x60), 0.0f
        assertEquals(1.0f, factors[0], 0.001f)
        assertEquals(232f / 255f, factors[1], 0.001f)
        assertEquals(176f / 255f, factors[2], 0.001f)
        assertEquals(96f / 255f, factors[3], 0.001f)
        assertEquals(0.0f, factors[4], 0.001f)

        // 必须严格单调递减
        for (i in 0 until factors.size - 1) {
            assertTrue(factors[i] > factors[i + 1], "Alpha factor at $i must be strictly greater than at ${i + 1}")
        }
    }

    @Test
    fun `createStops maps positions and scales alpha proportionally`() {
        val baseColor = Color(red = 1f, green = 1f, blue = 1f, alpha = 0.8f)
        val stops = ProgressiveFadeDefaults.createStops(baseColor)

        assertEquals(5, stops.size)

        // 检查位置
        assertEquals(0.00f, stops[0].first)
        assertEquals(0.25f, stops[1].first)
        assertEquals(0.50f, stops[2].first)
        assertEquals(0.75f, stops[3].first)
        assertEquals(1.00f, stops[4].first)

        // 检查颜色与 Alpha 比例
        assertEquals(0.8f * 1.0f, stops[0].second.alpha, 0.001f)
        assertEquals(0.8f * (232f / 255f), stops[1].second.alpha, 0.001f)
        assertEquals(0.8f * (176f / 255f), stops[2].second.alpha, 0.001f)
        assertEquals(0.8f * (96f / 255f), stops[3].second.alpha, 0.001f)
        assertEquals(0.0f, stops[4].second.alpha, 0.001f)

        // RGB 通道不变
        for (stop in stops) {
            assertEquals(1f, stop.second.red)
            assertEquals(1f, stop.second.green)
            assertEquals(1f, stop.second.blue)
        }
    }

    @Test
    fun `transparent base color yields all zero alpha stops`() {
        val transparent = Color.Transparent
        val stops = ProgressiveFadeDefaults.createStops(transparent)
        for (stop in stops) {
            assertEquals(0f, stop.second.alpha, 0.001f)
        }
    }
}
