package com.android.purebilibili.feature.settings

import com.android.purebilibili.navigation3.BiliPaiNavKey

internal fun isSettingsSubtreeNavKey(key: BiliPaiNavKey): Boolean {
    return isSettingsSubtreeRoute(key.routeBase)
}

internal fun isSettingsSearchNavKey(key: BiliPaiNavKey): Boolean {
    return key is BiliPaiNavKey.SettingsSearch || key.routeBase == SETTINGS_SEARCH_ROUTE_BASE
}

internal fun resolveSettingsTabletShellCategory(key: BiliPaiNavKey): SettingsRootCategory? {
    return when (key) {
        BiliPaiNavKey.Settings -> null
        is BiliPaiNavKey.SettingsCategory -> canonicalSettingsRootCategory(key.category)
        else -> resolveSettingsRootCategoryForNavKey(key)
    }
}

internal fun shouldRenderSettingsTabletDetailPane(
    selectedCategory: SettingsRootCategory?,
    isSearchActive: Boolean,
): Boolean {
    return selectedCategory != null || isSearchActive
}
