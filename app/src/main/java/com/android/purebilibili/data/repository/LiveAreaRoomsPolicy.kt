package com.android.purebilibili.data.repository

import com.android.purebilibili.core.util.getApiErrorMessage
import com.android.purebilibili.data.model.response.LiveRoom

private val LIVE_AREA_FALLBACK_CODES = setOf(-352, -412, -509, 22015)

internal fun shouldFallbackLiveAreaRooms(
    code: Int,
    message: String
): Boolean {
    if (code in LIVE_AREA_FALLBACK_CODES) return true
    val normalized = message.lowercase()
    return normalized.contains("风控") ||
        normalized.contains("频率") ||
        normalized.contains("risk") ||
        normalized.contains("412")
}

internal fun filterFallbackLiveAreaRooms(
    rooms: List<LiveRoom>,
    areaTitle: String
): List<LiveRoom> {
    val normalizedTitle = areaTitle.trim()
    if (normalizedTitle.isBlank()) return rooms

    val exactAreaMatches = rooms.filter { it.areaName.equals(normalizedTitle, ignoreCase = true) }
    if (exactAreaMatches.isNotEmpty()) return exactAreaMatches

    val partialAreaMatches = rooms.filter { it.areaName.contains(normalizedTitle, ignoreCase = true) }
    if (partialAreaMatches.isNotEmpty()) return partialAreaMatches

    val titleMatches = rooms.filter { it.title.contains(normalizedTitle, ignoreCase = true) }
    if (titleMatches.isNotEmpty()) return titleMatches

    return emptyList()
}

internal fun hasMoreLiveAreaRooms(
    loadedCount: Int,
    page: Int,
    pageSize: Int,
    hasMoreFlag: Int?,
    totalCount: Int
): Boolean {
    if (loadedCount <= 0) return false
    if (hasMoreFlag != null) return hasMoreFlag == 1
    // 有服务端总数时用实际返回大小兜底（服务器可能只回 20 条而请求是 30），
    // 不能按请求前的 30 提前判定到底。
    val effectiveSize = minOf(pageSize.coerceAtLeast(1), loadedCount)
    if (totalCount > 0) return page.toLong() * effectiveSize < totalCount
    // 既无 hasMore 也无 total：无法证明还有下一页。此时保留原语义——只有「满页」才
    // 继续探测，避免服务器省略元数据时的无限翻页（短页即视为到底）。
    return loadedCount >= pageSize.coerceAtLeast(1)
}

internal fun resolveLiveAreaRoomsErrorMessage(
    code: Int,
    message: String
): String {
    return getApiErrorMessage(code, message.ifBlank { "加载分区直播失败" })
}
