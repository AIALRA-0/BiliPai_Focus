package com.android.purebilibili.core.ui.performance

import kotlin.math.abs
import kotlin.math.round

/** 只在当前分辨率内选择档位，优先使用视频播放帧率的整数倍。 */
internal fun resolveIdleVideoDisplayMode(
    modes: List<AppDisplayMode>,
    width: Int,
    height: Int,
    frameRate: Float,
    speed: Float,
): AppDisplayMode? {
    if (!frameRate.isFinite() || !speed.isFinite() || frameRate <= 0f || speed <= 0f) return null
    val playbackRate = frameRate * speed
    if (!playbackRate.isFinite()) return null
    val candidates = modes.filter {
        it.width == width && it.height == height && it.refreshRate.isFinite() &&
            it.refreshRate >= 48f && it.refreshRate + 0.1f >= playbackRate
    }.sortedBy { it.refreshRate }
    // 容忍 23.976/29.97/59.94 与面板标称档位的细小差异。
    return candidates.firstOrNull {
        val multiple = it.refreshRate / playbackRate
        abs(multiple - round(multiple)) <= 0.015f
    } ?: candidates.firstOrNull()
}
