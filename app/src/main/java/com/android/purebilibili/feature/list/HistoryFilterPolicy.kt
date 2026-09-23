package com.android.purebilibili.feature.list

import com.android.purebilibili.data.model.response.HistoryBusiness
import com.android.purebilibili.data.model.response.HistoryItem
import com.android.purebilibili.data.model.response.VideoItem

internal enum class HistoryContentFilter(val label: String) {
    ALL("全部"),
    VIDEO("视频"),
    PGC("番剧"),
    LIVE("直播"),
    ARTICLE("专栏")
}

internal fun resolveHistoryListType(filter: HistoryContentFilter): String? {
    return when (filter) {
        HistoryContentFilter.ALL -> null
        HistoryContentFilter.VIDEO -> "archive"
        HistoryContentFilter.PGC -> null
        HistoryContentFilter.LIVE -> "live"
        HistoryContentFilter.ARTICLE -> "article"
    }
}

internal fun filterHistoryItemsByContent(
    items: List<VideoItem>,
    filter: HistoryContentFilter,
    resolveHistoryItem: (VideoItem) -> HistoryItem?
): List<VideoItem> {
    if (filter == HistoryContentFilter.ALL) return items

    return items.filter { video ->
        when (resolveHistoryItem(video)?.business) {
            HistoryBusiness.PGC -> filter == HistoryContentFilter.PGC
            HistoryBusiness.LIVE -> filter == HistoryContentFilter.LIVE
            HistoryBusiness.ARTICLE -> filter == HistoryContentFilter.ARTICLE
            HistoryBusiness.CHEESE,
            HistoryBusiness.ARCHIVE,
            HistoryBusiness.UNKNOWN,
            null -> filter == HistoryContentFilter.VIDEO
        }
    }
}

internal fun shouldLoadMoreHistoryFilterResults(
    filter: HistoryContentFilter,
    filteredItemCount: Int,
    hasMore: Boolean,
    isLoading: Boolean
): Boolean {
    return filter != HistoryContentFilter.ALL &&
        filteredItemCount == 0 &&
        hasMore &&
        !isLoading
}
