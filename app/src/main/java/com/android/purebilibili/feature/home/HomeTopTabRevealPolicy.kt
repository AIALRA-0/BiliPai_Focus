package com.android.purebilibili.feature.home

import androidx.compose.ui.unit.Dp
import com.android.purebilibili.core.ui.AppSpacingTokens

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
    topTabsCollapsed: Boolean = false,
    hideTopTabs: Boolean = false
): Boolean {
    if (hideTopTabs) return false
    if (isReturningFromDetail) return true
    return !isDelayedForCardSettle && !isForwardNavigatingToDetail
}

fun resolveEffectiveHomeTabRowHeight(
    hideTopTabs: Boolean,
    defaultTabRowHeight: Dp
): Dp {
    return if (hideTopTabs) AppSpacingTokens.None else defaultTabRowHeight
}

fun resolveEffectiveHomeTopChromeHeight(
    hideTopTabs: Boolean,
    useUnifiedPanel: Boolean,
    searchBarHeight: Dp,
    tabRowHeight: Dp,
    unifiedPanelInnerPadding: Dp,
    searchToTabsSpacing: Dp
): Dp {
    return if (hideTopTabs) {
        if (useUnifiedPanel) {
            searchBarHeight + (unifiedPanelInnerPadding * 2)
        } else {
            searchBarHeight
        }
    } else if (useUnifiedPanel) {
        searchBarHeight + tabRowHeight + (unifiedPanelInnerPadding * 2) + searchToTabsSpacing
    } else {
        searchBarHeight + searchToTabsSpacing + tabRowHeight
    }
}

internal fun shouldAutoCollapseHomeSearchRow(): Boolean = true

internal fun shouldCollapseHomeTopTabsWithSearchRow(): Boolean = false
