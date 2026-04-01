package com.android.purebilibili.feature.list

import com.android.purebilibili.data.model.response.HistoryBusiness
import com.android.purebilibili.data.model.response.HistoryItem

internal enum class HistoryDeleteAnimationMode {
    SINGLE_DISSOLVE,
    DIRECT_DELETE
}

internal data class HistoryDeleteSession(
    val targetKeys: Set<String>,
    val completedKeys: Set<String>,
    val animationMode: HistoryDeleteAnimationMode
)

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

internal fun resolveHistoryDeleteAnimationMode(itemCount: Int): HistoryDeleteAnimationMode {
    return if (itemCount <= 1) {
        HistoryDeleteAnimationMode.SINGLE_DISSOLVE
    } else {
        HistoryDeleteAnimationMode.DIRECT_DELETE
    }
}

internal fun createHistoryDeleteSession(
    targetKeys: Set<String>
): HistoryDeleteSession? {
    val normalizedKeys = targetKeys.map(String::trim).filter(String::isNotEmpty).toSet()
    if (normalizedKeys.isEmpty()) return null
    return HistoryDeleteSession(
        targetKeys = normalizedKeys,
        completedKeys = emptySet(),
        animationMode = resolveHistoryDeleteAnimationMode(normalizedKeys.size)
    )
}

internal fun shouldJiggleHistoryDeleteCards(
    animationMode: HistoryDeleteAnimationMode
): Boolean {
    return animationMode == HistoryDeleteAnimationMode.SINGLE_DISSOLVE
}

internal fun shouldCollapseHistoryDeleteCard(
    animationMode: HistoryDeleteAnimationMode
): Boolean {
    return animationMode == HistoryDeleteAnimationMode.SINGLE_DISSOLVE
}

internal fun shouldFinalizeBatchHistoryDelete(
    targetKeys: Set<String>,
    completedKeys: Set<String>
): Boolean {
    return targetKeys.isNotEmpty() && completedKeys.containsAll(targetKeys)
}

internal fun reduceHistoryDeleteSessionOnAnimationComplete(
    session: HistoryDeleteSession,
    completedKey: String
): HistoryDeleteSession {
    val normalizedKey = completedKey.trim()
    if (normalizedKey.isEmpty() || normalizedKey !in session.targetKeys) return session
    return session.copy(completedKeys = session.completedKeys + normalizedKey)
}

internal fun resolveActiveHistoryDeleteKeys(
    session: HistoryDeleteSession?
): Set<String> {
    return session?.targetKeys?.minus(session.completedKeys).orEmpty()
}

internal fun shouldKeepHistoryDeletePlaceholderHidden(
    session: HistoryDeleteSession?,
    key: String
): Boolean {
    val normalizedKey = key.trim()
    if (normalizedKey.isEmpty()) return false
    return session != null && normalizedKey in session.completedKeys
}

internal fun shouldFinalizeHistoryDeleteSession(
    session: HistoryDeleteSession
): Boolean {
    return shouldFinalizeBatchHistoryDelete(
        targetKeys = session.targetKeys,
        completedKeys = session.completedKeys
    )
}
