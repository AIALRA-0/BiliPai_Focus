package com.android.purebilibili.feature.home

import com.android.purebilibili.core.store.FocusFollowGroupConfig
import com.android.purebilibili.core.store.isFocusFollowUserVisible
import com.android.purebilibili.core.util.appendDistinctByKey
import com.android.purebilibili.core.util.prependDistinctByKey
import com.android.purebilibili.data.model.response.VideoItem
import java.util.ArrayDeque

internal const val HOME_FOLLOW_MIN_VISIBLE_BATCH_SIZE = 16
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

internal fun resolveVisibleHomeFollowUserMids(
    followingMids: Set<Long>,
    blockedMids: Set<Long>,
    config: FocusFollowGroupConfig,
    filterEnabled: Boolean
): List<Long> {
    return followingMids
        .asSequence()
        .filter { mid -> mid > 0L && mid !in blockedMids }
        .filter { mid -> !filterEnabled || isFocusFollowUserVisible(config, mid) }
        .toList()
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

internal fun presentHomeFollowVisibleVideos(
    existingPresentedVisibleVideos: List<VideoItem>,
    incomingVisibleVideos: List<VideoItem>,
    isLoadMore: Boolean,
    seed: Long,
    reshuffleOnRefresh: Boolean
): List<VideoItem> {
    return when {
        !isLoadMore && reshuffleOnRefresh -> randomizeHomeFollowIncomingVideos(
            videos = incomingVisibleVideos,
            seed = seed
        )
        !isLoadMore -> incomingVisibleVideos
        else -> appendDistinctByKey(
            existingPresentedVisibleVideos,
            incomingVisibleVideos,
            ::resolveHomeFollowVideoKey
        )
    }
}

internal fun resolveHomeFollowDisplayCount(
    currentDisplayCount: Int,
    isLoadMore: Boolean,
    batchSize: Int = HOME_FOLLOW_MIN_VISIBLE_BATCH_SIZE
): Int {
    val normalizedBatchSize = batchSize.coerceAtLeast(1)
    return if (isLoadMore) {
        currentDisplayCount.coerceAtLeast(0) + normalizedBatchSize
    } else {
        normalizedBatchSize
    }
}

internal fun resolveDisplayedHomeFollowVisibleVideos(
    presentedVisibleVideos: List<VideoItem>,
    displayCount: Int
): List<VideoItem> {
    return presentedVisibleVideos.take(displayCount.coerceAtLeast(0))
}

internal fun resolveHomeFollowPresentationHasMore(
    presentedVisibleCount: Int,
    displayedVisibleCount: Int,
    sourceHasMore: Boolean
): Boolean {
    return sourceHasMore || presentedVisibleCount > displayedVisibleCount
}

internal fun canRevealMorePresentedHomeFollowVideos(
    presentedVisibleCount: Int,
    displayedVisibleCount: Int
): Boolean {
    return presentedVisibleCount > displayedVisibleCount
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
    val creators = videos
        .groupBy { it.owner.mid }
        .mapValues { (_, creatorVideos) -> ArrayDeque(creatorVideos) }
    val creatorOrder = creators.keys.sortedBy { creatorMid ->
        resolveHomeFollowCreatorRandomOrderValue(creatorMid = creatorMid, seed = seed)
    }

    return buildList(videos.size) {
        while (true) {
            var appendedInRound = false
            creatorOrder.forEach { creatorMid ->
                val creatorQueue = creators[creatorMid] ?: return@forEach
                if (creatorQueue.isEmpty()) return@forEach
                val nextVideo = creatorQueue.removeFirst()
                add(nextVideo)
                appendedInRound = true
            }
            if (!appendedInRound) return@buildList
        }
    }
}

internal fun resolveHomeFollowVideoKey(video: VideoItem): String {
    if (video.dynamicId.isNotBlank()) return "dyn:${video.dynamicId}"
    if (video.bvid.isNotBlank()) return "bvid:${video.bvid}"
    if (video.aid > 0) return "aid:${video.aid}"
    if (video.id > 0) return "id:${video.id}"
    return "${video.owner.mid}:${video.title}:${video.pubdate}"
}

internal fun randomizeHomeFollowVisibleUserMids(
    userMids: Collection<Long>,
    seed: Long
): List<Long> {
    return userMids
        .asSequence()
        .filter { it > 0L }
        .distinct()
        .sortedBy { mid ->
            resolveHomeFollowCreatorRandomOrderValue(creatorMid = mid, seed = seed)
        }
        .toList()
}

internal fun resolveHomeFollowCreatorRandomOrderValue(
    creatorMid: Long,
    seed: Long
): Long {
    return mixHomeFollowRandomSeed(seed xor creatorMid)
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
