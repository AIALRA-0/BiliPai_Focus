package com.android.purebilibili.feature.home

import com.android.purebilibili.data.model.response.VideoItem

internal const val TODAY_WATCH_EXPANDED_CACHE_TTL_MS: Long = 10 * 60 * 1000L

internal data class TodayWatchExpandedCandidateCache(
    val baseSignature: String,
    val candidates: List<VideoItem>,
    val loadedAtMillis: Long
)

internal fun buildTodayWatchCandidateSignature(candidates: List<VideoItem>): String {
    return candidates.asSequence()
        .map { it.bvid.trim() }
        .filter { it.isNotBlank() }
        .joinToString(separator = "|")
}

internal fun mergeTodayWatchCandidates(
    localCandidates: List<VideoItem>,
    expandedCandidates: List<VideoItem>
): List<VideoItem> {
    return (localCandidates + expandedCandidates)
        .filter { it.bvid.isNotBlank() && it.title.isNotBlank() }
        .distinctBy { it.bvid }
}

internal fun canReuseTodayWatchExpandedCache(
    cache: TodayWatchExpandedCandidateCache?,
    baseSignature: String,
    nowMillis: Long
): Boolean {
    return cache != null &&
        cache.baseSignature == baseSignature &&
        nowMillis - cache.loadedAtMillis in 0L..TODAY_WATCH_EXPANDED_CACHE_TTL_MS
}

internal fun shouldApplyTodayWatchBuildResult(
    resultGeneration: Long,
    currentGeneration: Long
): Boolean = resultGeneration == currentGeneration
