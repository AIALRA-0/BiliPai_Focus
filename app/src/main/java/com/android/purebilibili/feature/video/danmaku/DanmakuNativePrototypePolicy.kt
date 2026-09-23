package com.android.purebilibili.feature.video.danmaku

internal data class DanmakuRenderPerformanceSample(
    val frameP95Ms: Double,
    val danmakuDrawCpuShare: Double
)

internal fun shouldPrototypeNativeDanmakuRenderer(
    recentSamples: List<DanmakuRenderPerformanceSample>
): Boolean = recentSamples
    .takeLast(REQUIRED_NATIVE_GATE_BREACHES)
    .takeIf { it.size == REQUIRED_NATIVE_GATE_BREACHES }
    ?.all { sample ->
        sample.frameP95Ms > NATIVE_GATE_FRAME_P95_MS &&
            sample.danmakuDrawCpuShare > NATIVE_GATE_DRAW_CPU_SHARE
    } == true

private const val REQUIRED_NATIVE_GATE_BREACHES = 3
private const val NATIVE_GATE_FRAME_P95_MS = 16.7
private const val NATIVE_GATE_DRAW_CPU_SHARE = 0.40
