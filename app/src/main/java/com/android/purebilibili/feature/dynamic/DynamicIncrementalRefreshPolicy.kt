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

internal fun dynamicTimelineItemsOverlap(
    existing: List<DynamicItem>,
    incoming: List<DynamicItem>
): Boolean {
    if (existing.isEmpty() || incoming.isEmpty()) return false
    val existingKeys = HashSet<String>(existing.size)
    for (item in existing) {
        existingKeys.add(dynamicFeedItemKey(item))
    }
    return incoming.any { existingKeys.contains(dynamicFeedItemKey(it)) }
}

internal fun canPerformIncrementalTimelineRefresh(
    isRefresh: Boolean,
    incrementalRefreshEnabled: Boolean,
    isCachePlaceholder: Boolean = false,
    existingItems: List<DynamicItem>,
    incomingItems: List<DynamicItem>
): Boolean {
    if (!isRefresh || !incrementalRefreshEnabled) return false
    // 冷启动离线缓存占位符绝不参与增量拼接，必须完整刷新，对齐 PiliPlus
    if (isCachePlaceholder) return false
    if (existingItems.isEmpty() || incomingItems.isEmpty()) return false
    // 必须存在重叠节点证明时间线连续，否则回退全量替换，避免出现时间断层/吞动态
    return dynamicTimelineItemsOverlap(existing = existingItems, incoming = incomingItems)
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

/** FeedVerticalStaggeredGrid adds a full-line chrome inset before data items. */
internal fun resolveDynamicRefreshDividerGridIndex(
    dividerDataIndex: Int,
    leadingGridItemCount: Int = 1,
): Int = if (dividerDataIndex < 0) {
    -1
} else {
    dividerDataIndex + leadingGridItemCount.coerceAtLeast(0)
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

internal fun resolveDynamicRefreshUserId(
    selectedTab: Int,
    selectedUserId: Long?
): Long? {
    return selectedUserId.takeIf {
        shouldUseSelectedUserDynamicFeed(
            selectedTab = selectedTab,
            selectedUserId = selectedUserId
        )
    }
}
