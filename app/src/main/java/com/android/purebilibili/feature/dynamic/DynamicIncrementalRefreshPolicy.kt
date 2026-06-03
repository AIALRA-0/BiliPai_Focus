package com.android.purebilibili.feature.dynamic

import com.android.purebilibili.data.model.response.DynamicItem

internal const val FOLLOWINGS_REFRESH_TTL_MS: Long = 5 * 60 * 1000L

internal data class IncrementalRefreshBoundary(
    val boundaryKey: String?,
    val prependedCount: Int
)

internal fun dynamicFeedItemKey(item: DynamicItem): String {
    if (item.id_str.isNotBlank()) return item.id_str
    val authorMid = item.modules.module_author?.mid ?: 0L
    val pubTs = item.modules.module_author?.pub_ts ?: 0L
    return "${item.type}-$authorMid-$pubTs"
}

internal fun resolveIncrementalRefreshBoundary(
    existingKeys: List<String>,
    mergedKeys: List<String>
): IncrementalRefreshBoundary {
    val boundaryKey = existingKeys.firstOrNull()?.takeIf { mergedKeys.contains(it) }
    if (boundaryKey == null) {
        return IncrementalRefreshBoundary(
            boundaryKey = null,
            prependedCount = 0
        )
    }
    val dividerIndex = mergedKeys.indexOf(boundaryKey)
    return IncrementalRefreshBoundary(
        boundaryKey = boundaryKey,
        prependedCount = dividerIndex.coerceAtLeast(0)
    )
}

internal fun resolveOldContentDividerIndex(
    displayKeys: List<String>,
    boundaryKey: String?,
    showDivider: Boolean
): Int {
    if (!showDivider || boundaryKey.isNullOrBlank()) return -1
    val dividerIndex = displayKeys.indexOf(boundaryKey)
    return if (dividerIndex > 0) dividerIndex else -1
}

internal fun shouldReloadFollowings(
    nowMs: Long,
    lastLoadMs: Long,
    ttlMs: Long = FOLLOWINGS_REFRESH_TTL_MS
): Boolean {
    if (lastLoadMs <= 0L) return true
    return nowMs - lastLoadMs >= ttlMs
}

internal fun shouldStartDynamicRefresh(
    isRefreshing: Boolean,
    isLoadingLocked: Boolean
): Boolean {
    return !isRefreshing && !isLoadingLocked
}
