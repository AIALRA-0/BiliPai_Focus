package com.android.purebilibili.feature.audio.bgm

import com.android.purebilibili.data.model.response.BgmCommentInfo
import com.android.purebilibili.data.model.response.BgmSongHeat

internal fun resolveBgmHeatPoints(points: List<BgmSongHeat>): List<BgmSongHeat> =
    points.filter { it.date > 0 && it.heat >= 0 }.distinctBy { it.date }.sortedBy { it.date }.takeLast(30)

internal fun resolveBgmCommentType(info: BgmCommentInfo): Int = info.pageType.takeIf { it > 0 } ?: 47

internal data class BgmHeatRange(val minimum: Double, val maximum: Double)

internal fun resolveBgmHeatRange(points: List<BgmSongHeat>): BgmHeatRange {
    if (points.isEmpty()) return BgmHeatRange(0.0, 1.0)
    val minimum = points.minOf { it.heat }.toDouble()
    val maximum = points.maxOf { it.heat }.toDouble()
    if (minimum != maximum) return BgmHeatRange(minimum, maximum)
    val padding = maxOf(1.0, minimum * .1)
    return BgmHeatRange((minimum - padding).coerceAtLeast(0.0), maximum + padding)
}
