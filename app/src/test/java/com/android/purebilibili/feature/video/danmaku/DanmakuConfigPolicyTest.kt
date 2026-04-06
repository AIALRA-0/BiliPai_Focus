package com.android.purebilibili.feature.video.danmaku

import com.bytedance.danmaku.render.engine.utils.LAYER_TYPE_BOTTOM_CENTER
import com.bytedance.danmaku.render.engine.utils.LAYER_TYPE_SCROLL
import com.bytedance.danmaku.render.engine.utils.LAYER_TYPE_TOP_CENTER
import org.junit.Assert.assertEquals
import org.junit.Test

class DanmakuConfigPolicyTest {

    @Test
    fun `minimum visible lines should not degrade to single line`() {
        assertEquals(2, resolveDanmakuMinimumVisibleLines(0.25f))
        assertEquals(3, resolveDanmakuMinimumVisibleLines(0.5f))
        assertEquals(5, resolveDanmakuMinimumVisibleLines(0.75f))
        assertEquals(6, resolveDanmakuMinimumVisibleLines(1.0f))
    }

    @Test
    fun `fallback max lines should remain stable by area ratio`() {
        assertEquals(4, resolveDanmakuFallbackMaxLines(0.25f))
        assertEquals(8, resolveDanmakuFallbackMaxLines(0.5f))
        assertEquals(12, resolveDanmakuFallbackMaxLines(0.75f))
        assertEquals(16, resolveDanmakuFallbackMaxLines(1.0f))
    }

    @Test
    fun `scroll duration should respect explicit duration seconds and speed factor`() {
        assertEquals(
            7000L,
            resolveDanmakuScrollDurationMillis(
                scrollDurationSeconds = 7.0f,
                speedFactor = 1.0f,
                scrollFixedVelocity = false,
                viewportWidthPx = 1080
            )
        )
        assertEquals(
            10500L,
            resolveDanmakuScrollDurationMillis(
                scrollDurationSeconds = 7.0f,
                speedFactor = 1.5f,
                scrollFixedVelocity = false,
                viewportWidthPx = 1080
            )
        )
    }

    @Test
    fun `fixed velocity should scale scroll duration with viewport width`() {
        assertEquals(
            14000L,
            resolveDanmakuScrollDurationMillis(
                scrollDurationSeconds = 7.0f,
                speedFactor = 1.0f,
                scrollFixedVelocity = true,
                viewportWidthPx = 2160
            )
        )
    }

    @Test
    fun `pinned duration should clamp to safe bounds`() {
        assertEquals(2000L, resolveDanmakuPinnedDurationMillis(0.5f))
        assertEquals(4000L, resolveDanmakuPinnedDurationMillis(4.0f))
        assertEquals(15000L, resolveDanmakuPinnedDurationMillis(18.0f))
    }

    @Test
    fun `massive mode should expose more visible lines`() {
        assertEquals(
            10,
            resolveDanmakuVisibleLineCount(
                visibleHeightPx = 280f,
                areaRatioHint = 0.5f,
                fontSize = 42f,
                strokeWidth = 1.5f,
                strokeEnabled = true,
                lineHeight = 1.0f,
                massiveMode = true
            )
        )
    }

    @Test
    fun `line height multiplier should be converted to engine px spacing`() {
        assertEquals(
            67.2f,
            resolveDanmakuLayerLineHeightPx(
                fontSize = 42f,
                lineHeightMultiplier = 1.6f
            ),
            0.001f
        )
    }

    @Test
    fun `static to scroll should remap pinned danmaku to scrolling layer`() {
        assertEquals(
            LAYER_TYPE_SCROLL,
            resolveDanmakuRenderLayerType(
                type = 4,
                staticDanmakuToScroll = true
            )
        )
        assertEquals(
            LAYER_TYPE_SCROLL,
            resolveDanmakuRenderLayerType(
                type = 5,
                staticDanmakuToScroll = true
            )
        )
        assertEquals(
            LAYER_TYPE_BOTTOM_CENTER,
            resolveDanmakuRenderLayerType(
                type = 4,
                staticDanmakuToScroll = false
            )
        )
        assertEquals(
            LAYER_TYPE_TOP_CENTER,
            resolveDanmakuRenderLayerType(
                type = 5,
                staticDanmakuToScroll = false
            )
        )
    }
}
