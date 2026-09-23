package com.android.purebilibili.feature.video.danmaku

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DanmakuViewportPolicyTest {
    @Test
    fun `invalid geometry does not produce a placeholder viewport`() {
        assertNull(resolveDanmakuViewport(0, 608, 3f, 1080f))
        assertNull(resolveDanmakuViewport(1080, 608, 0f, 1080f))
        assertNull(resolveDanmakuViewport(1080, 608, 3f, Float.NaN))
    }

    @Test
    fun `rotation preserves scale and a smaller window really shrinks below three quarters`() {
        val landscape = requireNotNull(resolveDanmakuViewport(2392, 1080, 3f, 1080f))
        val portrait = requireNotNull(resolveDanmakuViewport(1080, 2392, 3f, 1080f))
        val inline = requireNotNull(resolveDanmakuViewport(1080, 608, 3f, 1080f))
        assertEquals(landscape.scale, portrait.scale, 0f)
        assertEquals(608f / 1080f, inline.scale, 0.0001f)
        assertTrue(inline.scale < 0.75f)
    }

    @Test
    fun `proportional geometry retains line budget including scaled interline spacing`() {
        fun lines(scale: Float) = resolveDanmakuVisibleLineCount(
            visibleHeightPx = 500f * scale,
            areaRatioHint = 0.5f,
            fontSize = 20f * scale,
            strokeWidth = 1.5f * scale,
            strokeEnabled = true,
            lineHeight = 1.6f,
            massiveMode = true,
            viewportScale = scale
        )
        assertEquals(lines(1f), lines(0.5f))
        val scale = 0.5f
        val occupiedHeight = 32f * scale + (lines(scale) - 1) * (32f + 18f) * scale
        assertTrue(occupiedHeight <= 500f * scale)
    }
}
