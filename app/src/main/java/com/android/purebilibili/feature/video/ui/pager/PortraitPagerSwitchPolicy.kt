package com.android.purebilibili.feature.video.ui.pager

import androidx.compose.ui.layout.ContentScale
import com.android.purebilibili.data.model.response.Page
import com.android.purebilibili.data.model.response.RelatedVideo
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.data.model.response.ViewInfo
import kotlin.random.Random
import kotlin.math.abs

private const val PORTRAIT_RECOMMENDATION_PREFETCH_THRESHOLD = 1
private val PORTRAIT_RECOMMENDATION_STOP_WORDS = setOf(
    "视频", "合集", "最新", "一个", "我们", "你们", "今天", "真的", "这个",
    "竖屏", "横屏", "官方", "完整版", "全集", "合集版"
)

private data class PortraitRecommendationSignature(
    val normalizedTitle: String,
    val coverKey: String,
    val titleKeywords: Set<String>,
    val ownerMid: Long,
    val duration: Int
)

internal fun resolveCommittedPage(
    isScrollInProgress: Boolean,
    currentPage: Int,
    lastCommittedPage: Int
): Int? {
    if (isScrollInProgress) return null
    if (currentPage == lastCommittedPage) return null
    return currentPage
}

internal fun shouldApplyLoadResult(
    requestGeneration: Int,
    activeGeneration: Int,
    expectedBvid: String,
    currentPlayingBvid: String?
): Boolean {
    if (requestGeneration != activeGeneration) return false
    if (expectedBvid != currentPlayingBvid) return false
    return true
}

internal fun shouldSkipPortraitReloadForCurrentMedia(
    currentPlayingBvid: String?,
    targetBvid: String,
    currentPlayerMediaId: String?
): Boolean {
    val normalizedMediaId = currentPlayerMediaId?.trim().orEmpty()
    if (normalizedMediaId.isBlank()) return false
    return currentPlayingBvid == targetBvid && normalizedMediaId == targetBvid
}

internal fun shouldShowPortraitCover(
    isLoading: Boolean,
    isCurrentPage: Boolean,
    isPlayerReadyForThisVideo: Boolean,
    hasRenderedFirstFrame: Boolean
): Boolean {
    if (isLoading) return true
    if (!isCurrentPage) return true
    if (!isPlayerReadyForThisVideo) return true
    if (!hasRenderedFirstFrame) return true
    return false
}

internal fun shouldUseViewportBoundPortraitCover(
    isCurrentPage: Boolean,
    isPlayerReadyForThisVideo: Boolean,
    hasRenderedFirstFrame: Boolean
): Boolean {
    if (!isCurrentPage) return true
    if (!isPlayerReadyForThisVideo) return true
    if (!hasRenderedFirstFrame) return true
    return false
}

internal fun resolvePortraitCoverContentScale(): ContentScale = ContentScale.Fit

internal fun shouldShowPortraitPauseIcon(
    isCurrentPage: Boolean,
    isPlaying: Boolean,
    playWhenReady: Boolean,
    isLoading: Boolean,
    isSeekGesture: Boolean
): Boolean {
    if (!isCurrentPage) return false
    if (isLoading) return false
    if (isSeekGesture) return false
    if (isPlaying) return false
    if (playWhenReady) return false
    return true
}

internal fun shouldHandlePortraitSeekGesture(scale: Float): Boolean {
    return scale <= 1.01f
}

internal fun shouldHandlePortraitTapGesture(scale: Float): Boolean {
    return scale <= 1.01f
}

internal fun shouldHandlePortraitLongPressGesture(scale: Float): Boolean {
    return scale <= 1.01f
}

internal fun shouldRestorePortraitLongPressSpeed(
    isLongPressing: Boolean,
    isCurrentPage: Boolean
): Boolean {
    return isLongPressing && !isCurrentPage
}

internal fun resolvePortraitInitialProgressPosition(
    isFirstPage: Boolean,
    initialStartPositionMs: Long
): Long {
    if (!isFirstPage) return 0L
    return initialStartPositionMs.coerceAtLeast(0L)
}

internal fun shouldLoadMorePortraitRecommendations(
    committedPage: Int,
    totalItemsCount: Int,
    isLoadingMoreRecommendations: Boolean,
    prefetchThreshold: Int = PORTRAIT_RECOMMENDATION_PREFETCH_THRESHOLD
): Boolean {
    if (isLoadingMoreRecommendations) return false
    if (committedPage < 0 || totalItemsCount <= 0) return false
    val lastTriggerIndex = (totalItemsCount - 1 - prefetchThreshold).coerceAtLeast(0)
    return committedPage >= lastTriggerIndex
}

internal fun mergePortraitRecommendationAppendItems(
    currentBvid: String,
    existingBvids: Set<String>,
    existingRecommendations: List<RelatedVideo>,
    fetchedRecommendations: List<RelatedVideo>
): List<RelatedVideo> {
    val accepted = existingRecommendations
        .filter { it.bvid.isNotBlank() }
        .toMutableList()

    return fetchedRecommendations.fold(mutableListOf<RelatedVideo>()) { appended, candidate ->
        val canAppend = candidate.bvid.isNotBlank() &&
            candidate.bvid != currentBvid &&
            candidate.bvid !in existingBvids &&
            appended.none { it.bvid == candidate.bvid } &&
            accepted.none { existing -> arePortraitRecommendationsContentSimilar(existing, candidate) }

        if (canAppend) {
            appended += candidate
            accepted += candidate
        }
        appended
    }
}

internal fun resolvePortraitRecommendationShuffleSeed(
    initialBvid: String,
    initialAid: Long
): Int {
    var seed = 17
    seed = 31 * seed + initialBvid.hashCode()
    seed = 31 * seed + initialAid.hashCode()
    return seed
}

internal fun resolvePortraitRecommendationAppendSeed(
    baseSeed: Int,
    currentBvid: String
): Int {
    return 31 * baseSeed + currentBvid.hashCode()
}

internal fun shufflePortraitRecommendations(
    seed: Int,
    recommendations: List<RelatedVideo>
): List<RelatedVideo> {
    val shuffled = recommendations
        .filter { it.bvid.isNotBlank() }
        .distinctBy { it.bvid }
        .shuffled(Random(seed))

    val deduplicated = mutableListOf<RelatedVideo>()
    shuffled.forEach { candidate ->
        if (deduplicated.none { existing -> arePortraitRecommendationsContentSimilar(existing, candidate) }) {
            deduplicated += candidate
        }
    }
    if (deduplicated.size <= 1) return deduplicated

    val remaining = deduplicated.toMutableList()
    val arranged = mutableListOf<RelatedVideo>()
    while (remaining.isNotEmpty()) {
        val last = arranged.lastOrNull()
        val candidateIndex = remaining.indexOfFirst { candidate ->
            last == null || (
                !arePortraitRecommendationsContentSimilar(last, candidate) &&
                    (last.owner.mid <= 0L || candidate.owner.mid <= 0L || last.owner.mid != candidate.owner.mid)
                )
        }.takeIf { it >= 0 }
            ?: remaining.indexOfFirst { candidate ->
                last == null || !arePortraitRecommendationsContentSimilar(last, candidate)
            }.takeIf { it >= 0 }
            ?: 0

        arranged += remaining.removeAt(candidateIndex)
    }
    return arranged
}

internal fun toRelatedVideoForPortraitRecommendation(item: VideoItem): RelatedVideo? {
    val bvid = item.bvid.trim()
    if (bvid.isEmpty()) return null
    return RelatedVideo(
        aid = item.aid.takeIf { it > 0L } ?: item.id,
        bvid = bvid,
        cid = item.cid,
        title = item.title,
        pic = item.pic,
        owner = item.owner,
        stat = item.stat,
        duration = item.duration
    )
}

internal fun arePortraitRecommendationsContentSimilar(
    first: RelatedVideo,
    second: RelatedVideo
): Boolean {
    if (first.bvid.isBlank() || second.bvid.isBlank()) return false
    if (first.bvid == second.bvid) return true

    val firstSignature = buildPortraitRecommendationSignature(first)
    val secondSignature = buildPortraitRecommendationSignature(second)

    if (
        firstSignature.normalizedTitle.isNotBlank() &&
            firstSignature.normalizedTitle == secondSignature.normalizedTitle
    ) {
        return true
    }

    if (
        firstSignature.coverKey.isNotBlank() &&
            firstSignature.coverKey == secondSignature.coverKey
    ) {
        return true
    }

    val keywordOverlap = firstSignature.titleKeywords
        .intersect(secondSignature.titleKeywords)
        .size
    val durationDelta = abs(firstSignature.duration - secondSignature.duration)
    if (keywordOverlap >= 3) return true
    if (keywordOverlap >= 2 && durationDelta <= 30) return true
    if (
        firstSignature.ownerMid > 0L &&
            firstSignature.ownerMid == secondSignature.ownerMid &&
            keywordOverlap >= 1 &&
            durationDelta <= 45
    ) {
        return true
    }

    return false
}

private fun buildPortraitRecommendationSignature(
    video: RelatedVideo
): PortraitRecommendationSignature {
    return PortraitRecommendationSignature(
        normalizedTitle = normalizePortraitRecommendationTitle(video.title),
        coverKey = normalizePortraitRecommendationCoverKey(video.pic),
        titleKeywords = extractPortraitRecommendationKeywords(video.title),
        ownerMid = video.owner.mid,
        duration = video.duration.coerceAtLeast(0)
    )
}

private fun normalizePortraitRecommendationTitle(title: String): String {
    return title.lowercase()
        .replace(Regex("[\\[{（(【].*?[\\]})）)】]"), " ")
        .replace(Regex("[^\\u4e00-\\u9fa5a-z0-9]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun normalizePortraitRecommendationCoverKey(pic: String): String {
    return pic.trim()
        .substringBefore('?')
        .substringAfterLast('/')
        .lowercase()
}

private fun extractPortraitRecommendationKeywords(title: String): Set<String> {
    val normalized = normalizePortraitRecommendationTitle(title)
    if (normalized.isBlank()) return emptySet()

    val zhTokens = Regex("[\\u4e00-\\u9fa5]{2,6}")
        .findAll(normalized)
        .map { it.value }
        .filter { it !in PORTRAIT_RECOMMENDATION_STOP_WORDS }
        .take(6)
        .toList()

    val enTokens = Regex("[a-z0-9]{3,}")
        .findAll(normalized)
        .map { it.value }
        .take(4)
        .toList()

    return (zhTokens + enTokens).toSet()
}

internal fun snapshotPortraitPageBvids(
    items: List<Any>
): Set<String> {
    return items.mapNotNull { candidate ->
        when (candidate) {
            is ViewInfo -> candidate.bvid
            is RelatedVideo -> candidate.bvid
            else -> null
        }
    }.toSet()
}

internal fun toViewInfoForPortraitDetail(related: RelatedVideo): ViewInfo {
    return ViewInfo(
        bvid = related.bvid,
        aid = related.aid,
        title = related.title,
        desc = "",
        pic = related.pic,
        owner = related.owner,
        stat = related.stat,
        pages = listOf(
            Page(duration = related.duration.toLong())
        )
    )
}
