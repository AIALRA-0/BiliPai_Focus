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

internal fun resolveLiveAreaRoomsErrorMessage(
    code: Int,
    message: String
): String {
    return getApiErrorMessage(code, message.ifBlank { "加载分区直播失败" })
}
