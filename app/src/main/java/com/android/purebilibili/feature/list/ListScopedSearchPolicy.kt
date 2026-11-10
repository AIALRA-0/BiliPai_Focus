package com.android.purebilibili.feature.list

/**
 * 「列表精简搜索」：依赖底栏搜索联动，开启后收藏/历史/稍后再看隐藏顶栏搜索栏，
 * 并由底栏搜索胶囊在页内搜索当前列表内容。
 */
internal fun isListScopedSearchActive(
    bottomBarSearchEnabled: Boolean,
    listScopedSearchEnabled: Boolean,
): Boolean = bottomBarSearchEnabled && listScopedSearchEnabled

internal fun shouldHideListTopSearchBar(
    bottomBarSearchEnabled: Boolean,
    listScopedSearchEnabled: Boolean,
    isSearchDestination: Boolean = false,
): Boolean = isListScopedSearchActive(
    bottomBarSearchEnabled = bottomBarSearchEnabled,
    listScopedSearchEnabled = listScopedSearchEnabled,
) && !isSearchDestination

internal fun shouldShowListScopedSearchActiveBar(
    bottomBarSearchEnabled: Boolean,
    listScopedSearchEnabled: Boolean,
    searchQuery: String,
): Boolean = isListScopedSearchActive(
    bottomBarSearchEnabled = bottomBarSearchEnabled,
    listScopedSearchEnabled = listScopedSearchEnabled,
) && searchQuery.isNotBlank()

internal fun resolveListScopedSearchActiveBarLabel(searchQuery: String): String {
    val normalized = searchQuery.trim()
    return if (normalized.isEmpty()) {
        "搜索中"
    } else {
        "搜索中：$normalized"
    }
}
