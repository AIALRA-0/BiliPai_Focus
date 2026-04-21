package com.android.purebilibili.feature.dynamic

import com.android.purebilibili.core.store.FocusFollowGroupConfig
import com.android.purebilibili.core.store.isFocusFollowUserVisible
import com.android.purebilibili.data.model.response.DynamicItem

private const val MIN_VISIBLE_DYNAMIC_ITEMS_AFTER_FOCUS_FILTER = 8
private const val MAX_FOCUS_DYNAMIC_PREFETCH_PAGES = 6

internal fun filterDynamicItemsByFocusFollowGroups(
    items: List<DynamicItem>,
    config: FocusFollowGroupConfig,
    filterEnabled: Boolean = true
): List<DynamicItem> {
    if (!filterEnabled) return items
    return items.filter { item ->
        val mid = item.modules.module_author?.mid ?: 0L
        isFocusFollowUserVisible(config, mid)
    }
}

internal fun filterSidebarUsersByFocusFollowGroups(
    users: List<SidebarUser>,
    config: FocusFollowGroupConfig,
    filterEnabled: Boolean = true
): List<SidebarUser> {
    if (!filterEnabled) return users
    return users.filter { user ->
        isFocusFollowUserVisible(config, user.uid)
    }
}

internal fun resolveSelectedUserIdAfterFocusFollowGroupFilter(
    selectedUserId: Long?,
    config: FocusFollowGroupConfig,
    filterEnabled: Boolean = true
): Long? {
    if (!filterEnabled) return selectedUserId
    val uid = selectedUserId ?: return null
    return uid.takeIf { isFocusFollowUserVisible(config, it) }
}

internal fun resolveDynamicFollowUserEmptyMessage(
    visibleUserCount: Int,
    hasResolvedUsers: Boolean,
    error: String?
): String? {
    if (visibleUserCount > 0 || !hasResolvedUsers || !error.isNullOrBlank()) return null
    return "没有可用关注对象"
}

internal fun shouldPrefetchMoreFocusDynamicItems(
    visibleItemCount: Int,
    hasMore: Boolean,
    filterEnabled: Boolean,
    extraPagesFetched: Int,
    minVisibleCount: Int = MIN_VISIBLE_DYNAMIC_ITEMS_AFTER_FOCUS_FILTER,
    maxExtraPages: Int = MAX_FOCUS_DYNAMIC_PREFETCH_PAGES
): Boolean {
    if (!filterEnabled) return false
    if (!hasMore) return false
    if (visibleItemCount >= minVisibleCount.coerceAtLeast(1)) return false
    return extraPagesFetched < maxExtraPages.coerceAtLeast(0)
}
