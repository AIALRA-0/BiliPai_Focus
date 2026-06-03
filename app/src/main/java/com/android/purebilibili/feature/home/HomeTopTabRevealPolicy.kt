package com.android.purebilibili.feature.home

fun resolveHomeTopTabsRevealDelayMs(
    isReturningFromDetail: Boolean,
    cardTransitionEnabled: Boolean,
    isQuickReturnFromDetail: Boolean
): Long {
    // 返回首页不再做额外延迟；折叠态只在详情返场时参与裁决，避免影响首页正常滚动恢复。
    return 0L
}

fun resolveHomeTopTabsVisible(
    isDelayedForCardSettle: Boolean,
    isForwardNavigatingToDetail: Boolean,
    isReturningFromDetail: Boolean,
    topTabsCollapsed: Boolean = false
): Boolean {
    if (isReturningFromDetail) return true
    return !isDelayedForCardSettle && !isForwardNavigatingToDetail
}

internal fun shouldAutoCollapseHomeSearchRow(): Boolean = true

internal fun shouldCollapseHomeTopTabsWithSearchRow(): Boolean = false
