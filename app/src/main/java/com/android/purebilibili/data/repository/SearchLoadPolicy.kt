package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.SearchType
import com.android.purebilibili.data.model.response.VideoItem

internal fun resolveVideoSearchPageInfo(
    requestedPage: Int,
    responsePage: Int,
    totalPages: Int,
    totalResults: Int,
    pageSize: Int,
    resultCount: Int
): SearchRepository.SearchPageInfo {
    val currentPage = resolveSearchLoadedPage(requestedPage, responsePage)
    val size = pageSize.takeIf { it > 0 } ?: 20
    val resolvedTotalPages = when {
        totalPages > 0 -> totalPages
        totalResults > 0 -> ((totalResults.toLong() + size - 1) / size).toInt()
        resultCount >= size -> currentPage + 1
        else -> currentPage
    }
    return SearchRepository.SearchPageInfo(
        currentPage = currentPage,
        totalPages = resolvedTotalPages,
        totalResults = totalResults.takeIf { it > 0 } ?: resultCount,
        // Match PiliPlus CommonListController: totals are informational; only an
        // empty server page ends pagination, even after a short nonempty page.
        hasMore = resultCount > 0
    )
}

internal fun resolveSearchLoadedPage(
    requestedPage: Int,
    responsePage: Int
): Int {
    return maxOf(requestedPage, responsePage.coerceAtLeast(1))
}

internal fun shouldApplySearchResult(
    requestSessionId: Long,
    activeSessionId: Long,
    requestQuery: String,
    activeQuery: String,
    requestType: SearchType,
    activeType: SearchType
): Boolean {
    return requestSessionId == activeSessionId &&
        requestQuery == activeQuery &&
        requestType == activeType
}

internal fun <T, K> mergeSearchPageResults(
    existing: List<T>,
    incoming: List<T>,
    keySelector: (T) -> K
): List<T> {
    val seen = LinkedHashSet<K>()
    val merged = ArrayList<T>(existing.size + incoming.size)
    for (item in existing) {
        if (seen.add(keySelector(item))) {
            merged += item
        }
    }
    for (item in incoming) {
        if (seen.add(keySelector(item))) {
            merged += item
        }
    }
    return merged
}

internal fun resolveSearchDurationRequests(
    durations: Set<SearchDuration>
): List<SearchDuration> {
    if (SearchDuration.ALL in durations) return listOf(SearchDuration.ALL)
    val normalized = durations.filter { it != SearchDuration.ALL }.distinct()
    return normalized.ifEmpty { listOf(SearchDuration.ALL) }
}

internal fun toggleSearchDurationSelection(
    current: Set<SearchDuration>,
    duration: SearchDuration
): Set<SearchDuration> {
    if (duration == SearchDuration.ALL) return emptySet()
    return if (duration in current) {
        current - duration
    } else {
        current + duration
    }
}

internal fun resolveSearchDurationFilterLabel(
    durations: Set<SearchDuration>
): String {
    val requests = resolveSearchDurationRequests(durations)
    return when (requests.size) {
        1 -> requests.single().displayName
        else -> "时长 ${requests.size} 项"
    }
}

internal fun mergeSearchDurationResultPages(
    pages: List<Pair<List<VideoItem>, SearchRepository.SearchPageInfo>>
): Pair<List<VideoItem>, SearchRepository.SearchPageInfo> {
    val merged = mergeSearchPageResults(
        existing = emptyList(),
        incoming = pages.flatMap { it.first },
        keySelector = ::resolveSearchVideoMergeKey
    )
    val pageInfos = pages.map { it.second }
    val pageInfo = SearchRepository.SearchPageInfo(
        currentPage = pageInfos.maxOfOrNull { it.currentPage } ?: 1,
        totalPages = pageInfos.maxOfOrNull { it.totalPages } ?: 1,
        totalResults = pageInfos.sumOf { it.totalResults },
        hasMore = pageInfos.any { it.hasMore }
    )
    return merged to pageInfo
}

private fun resolveSearchVideoMergeKey(video: VideoItem): String {
    return when {
        video.bvid.isNotBlank() -> "bvid:${video.bvid}"
        video.aid > 0L -> "aid:${video.aid}"
        video.id > 0L -> "id:${video.id}"
        else -> "title:${video.title}:${video.owner.mid}"
    }
}
