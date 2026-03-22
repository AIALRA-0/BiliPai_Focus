package com.android.purebilibili.feature.list

import com.android.purebilibili.data.model.response.HistoryBusiness
import com.android.purebilibili.data.model.response.HistoryItem

internal fun resolveHistoryRenderKey(item: HistoryItem): String {
    val bvid = item.videoItem.bvid.trim()
    if (bvid.isNotEmpty()) return bvid

    val fallbackId = when (item.business) {
        HistoryBusiness.ARCHIVE -> item.videoItem.id
        HistoryBusiness.PGC -> item.seasonId.takeIf { it > 0L } ?: item.videoItem.id
        HistoryBusiness.LIVE -> item.roomId.takeIf { it > 0L } ?: item.videoItem.id
        HistoryBusiness.ARTICLE -> item.videoItem.id
        HistoryBusiness.UNKNOWN -> item.videoItem.id
    }
    val businessTag = item.business.value.ifBlank { "unknown" }
    return "${businessTag}_${fallbackId.coerceAtLeast(0L)}"
}

internal fun resolveHistoryLookupKey(item: HistoryItem): String {
    val bvid = item.videoItem.bvid.trim()
    if (bvid.isNotEmpty()) return bvid
    return resolveHistoryRenderKey(item)
}

internal fun resolveHistoryDeleteKid(item: HistoryItem): String? {
    val prefixAndId = when (item.business) {
        HistoryBusiness.ARCHIVE -> "archive" to item.videoItem.id
        HistoryBusiness.PGC -> "pgc" to item.seasonId
        HistoryBusiness.LIVE -> "live" to item.roomId
        HistoryBusiness.ARTICLE -> "article" to item.videoItem.id
        HistoryBusiness.UNKNOWN -> {
            if (item.videoItem.bvid.isNotBlank()) {
                "archive" to item.videoItem.id
            } else {
                null
            }
        }
    } ?: return null

    val (prefix, targetId) = prefixAndId
    if (targetId <= 0L) return null
    return "${prefix}_${targetId}"
}
