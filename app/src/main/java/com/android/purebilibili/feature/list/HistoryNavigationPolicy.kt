package com.android.purebilibili.feature.list

import com.android.purebilibili.data.model.response.HistoryBusiness
import com.android.purebilibili.data.model.response.HistoryItem
import com.android.purebilibili.data.model.response.VideoItem

internal data class HistoryCardPresentation(
    val videoItem: VideoItem,
    val showUpBadge: Boolean
)

internal enum class HistoryNavigationKind {
    VIDEO,
    PGC,
    LIVE,
    ARTICLE
}

internal fun resolveHistoryNavigationKind(
    historyItem: HistoryItem?
): HistoryNavigationKind {
    return when (historyItem?.business) {
        HistoryBusiness.PGC -> HistoryNavigationKind.PGC
        HistoryBusiness.LIVE -> HistoryNavigationKind.LIVE
        HistoryBusiness.ARTICLE -> HistoryNavigationKind.ARTICLE
        else -> HistoryNavigationKind.VIDEO
    }
}

internal fun resolveHistoryCardPresentation(
    historyItem: HistoryItem?
): HistoryCardPresentation? {
    historyItem ?: return null
    return when (historyItem.business) {
        HistoryBusiness.PGC -> {
            val video = historyItem.videoItem
            HistoryCardPresentation(
                videoItem = if (video.owner.name.isBlank()) {
                    video.copy(owner = video.owner.copy(name = "番剧"))
                } else {
                    video
                },
                showUpBadge = false
            )
        }
        else -> HistoryCardPresentation(
            videoItem = historyItem.videoItem,
            showUpBadge = true
        )
    }
}
