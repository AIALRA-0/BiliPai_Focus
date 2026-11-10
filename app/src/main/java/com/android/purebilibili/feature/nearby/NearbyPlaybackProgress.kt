package com.android.purebilibili.feature.nearby

/** 准备阶段可停留在起点，也可正常前进；使用单调时间限制可接受的进度范围。 */
internal fun nearbyPlaybackPositionMatches(video: NearbyVideo, positionMs: Long, elapsedMs: Long): Boolean {
    if (positionMs < 0) return false
    val lower = (video.positionMs - 3_000L).coerceAtLeast(0L)
    val upper = video.positionMs.toDouble() + elapsedMs.coerceAtLeast(0L).toDouble() * video.speed + 3_000.0
    return positionMs >= lower && positionMs.toDouble() <= upper
}
