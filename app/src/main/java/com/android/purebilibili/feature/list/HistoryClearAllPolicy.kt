package com.android.purebilibili.feature.list

internal fun shouldShowHistoryClearAllAction(
    hasHistoryViewModel: Boolean,
    hasItems: Boolean,
    settingEnabled: Boolean,
    isBatchMode: Boolean
): Boolean {
    return hasHistoryViewModel &&
        hasItems &&
        settingEnabled &&
        !isBatchMode
}
