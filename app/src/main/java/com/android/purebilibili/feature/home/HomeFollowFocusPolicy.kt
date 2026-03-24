package com.android.purebilibili.feature.home

import com.android.purebilibili.core.store.FocusFollowGroupConfig
import com.android.purebilibili.core.store.isFocusFollowUserVisible
import com.android.purebilibili.core.util.appendDistinctByKey
import com.android.purebilibili.core.util.prependDistinctByKey
import com.android.purebilibili.data.model.response.VideoItem

private const val HOME_FOLLOW_MIN_VISIBLE_BATCH_SIZE = 8
private const val HOME_FOLLOW_REFRESH_COMPLETION_FETCH_LIMIT = 12
private const val HOME_FOLLOW_LOAD_MORE_COMPLETION_FETCH_LIMIT = 32
private const val HOME_FOLLOW_EMPTY_MESSAGE = "没有可用关注对象"
private const val HOME_FOLLOW_RANDOM_GAMMA = -7046029254386353131L
private const val HOME_FOLLOW_RANDOM_MIX1 = -4658895280553007687L
private const val HOME_FOLLOW_RANDOM_MIX2 = -7723592293110705685L

internal fun filterHomeFollowVideosByFocusFollowGroups(
    videos: List<VideoItem>,
    config: FocusFollowGroupConfig,
    filterEnabled: Boolean = true
): List<VideoItem> {
    if (!filterEnabled) return videos
    return videos.filter { video ->
        isFocusFollowUserVisible(config, video.owner.mid)
    }
}

internal fun resolveHomeFollowEmptyMessage(
    visibleVideoCount: Int,
    hasResolvedFollowFeedOnce: Boolean = true
): String? {
    if (!hasResolvedFollowFeedOnce || visibleVideoCount > 0) return null
    return HOME_FOLLOW_EMPTY_MESSAGE
}

internal fun resolveHomeFollowErrorAfterRefilter(
    visibleVideoCount: Int,
    hasResolvedFollowFeedOnce: Boolean,
    existingError: String?
): String? {
    if (!existingError.isNullOrBlank() && existingError != HOME_FOLLOW_EMPTY_MESSAGE) {
        return existingError
    }
    return resolveHomeFollowEmptyMessage(
        visibleVideoCount = visibleVideoCount,
        hasResolvedFollowFeedOnce = hasResolvedFollowFeedOnce
    )
}

internal fun resolveHomeFollowVisibleIncrement(
    baselineVisibleCount: Int,
    currentVisibleCount: Int
): Int {
    return (currentVisibleCount - baselineVisibleCount).coerceAtLeast(0)
}

internal fun shouldContinueHomeFollowFetchAfterFocusFilter(
    baselineVisibleCount: Int,
    visibleIncrement: Int,
    hasMore: Boolean,
    continuationFetches: Int,
    isLoadMore: Boolean,
    minimumVisibleIncrement: Int = HOME_FOLLOW_MIN_VISIBLE_BATCH_SIZE
): Boolean {
    if (!hasMore) return false
    val maxContinuationFetches = if (isLoadMore) {
        HOME_FOLLOW_LOAD_MORE_COMPLETION_FETCH_LIMIT
    } else {
        HOME_FOLLOW_REFRESH_COMPLETION_FETCH_LIMIT
    }
    if (continuationFetches >= maxContinuationFetches) return false
    if (visibleIncrement >= minimumVisibleIncrement.coerceAtLeast(1)) return false
    return true
}

internal fun accumulateHomeFollowRoundRawVideos(
    existingRoundRawVideos: List<VideoItem>,
    incomingRawVideos: List<VideoItem>,
    keySelector: (VideoItem) -> String
): List<VideoItem> {
    return appendDistinctByKey(existingRoundRawVideos, incomingRawVideos, keySelector)
}

internal fun resolveHomeFollowPresentedRawVideos(
    baselineRawVideos: List<VideoItem>,
    roundRawVideos: List<VideoItem>,
    isLoadMore: Boolean,
    keySelector: (VideoItem) -> String
): List<VideoItem> {
    return when {
        isLoadMore -> appendDistinctByKey(baselineRawVideos, roundRawVideos, keySelector)
        else -> prependDistinctByKey(baselineRawVideos, roundRawVideos, keySelector)
    }
}

internal fun randomizeHomeFollowIncomingVideos(
    videos: List<VideoItem>,
    seed: Long
): List<VideoItem> {
    if (videos.size <= 1) return videos
    return videos.sortedBy { video ->
        resolveHomeFollowRandomOrderValue(video = video, seed = seed)
    }
}

internal fun resolveHomeFollowRandomOrderValue(
    video: VideoItem,
    seed: Long
): Long {
    val identity = buildString {
        append(video.dynamicId)
        append('|')
        append(video.bvid)
        append('|')
        append(video.owner.mid)
        append('|')
        append(video.aid)
        append('|')
        append(video.title)
    }
    return mixHomeFollowRandomSeed(seed xor identity.hashCode().toLong())
}

private fun mixHomeFollowRandomSeed(value: Long): Long {
    var mixed = value + HOME_FOLLOW_RANDOM_GAMMA
    mixed = (mixed xor (mixed ushr 30)) * HOME_FOLLOW_RANDOM_MIX1
    mixed = (mixed xor (mixed ushr 27)) * HOME_FOLLOW_RANDOM_MIX2
    return mixed xor (mixed ushr 31)
}
