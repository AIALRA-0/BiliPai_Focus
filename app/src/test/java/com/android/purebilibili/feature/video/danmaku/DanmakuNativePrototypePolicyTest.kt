package com.android.purebilibili.feature.video.danmaku

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DanmakuNativePrototypePolicyTest {

    @Test
    fun `requires three consecutive frame and cpu breaches`() {
        val breach = DanmakuRenderPerformanceSample(frameP95Ms = 18.0, danmakuDrawCpuShare = 0.45)

        assertFalse(shouldPrototypeNativeDanmakuRenderer(listOf(breach, breach)))
        assertTrue(shouldPrototypeNativeDanmakuRenderer(listOf(breach, breach, breach)))
    }

    @Test
    fun `a passing frame or cpu sample keeps kotlin renderer`() {
        val samples = listOf(
            DanmakuRenderPerformanceSample(frameP95Ms = 18.0, danmakuDrawCpuShare = 0.45),
            DanmakuRenderPerformanceSample(frameP95Ms = 16.7, danmakuDrawCpuShare = 0.50),
            DanmakuRenderPerformanceSample(frameP95Ms = 20.0, danmakuDrawCpuShare = 0.40)
        )

        assertFalse(shouldPrototypeNativeDanmakuRenderer(samples))
    }
}
