package com.android.purebilibili.feature.dynamic

import com.android.purebilibili.core.store.FocusFollowGroupConfig
import com.android.purebilibili.core.store.isFocusFollowUserVisible
import com.android.purebilibili.data.model.response.DynamicItem

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
