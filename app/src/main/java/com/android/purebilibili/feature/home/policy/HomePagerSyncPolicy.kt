package com.android.purebilibili.feature.home.policy

import com.android.purebilibili.feature.home.HomeCategory

internal enum class HomePagerSettledAction {
    NONE,
    SWITCH_CATEGORY,
    OPEN_LIVE_PAGE
}

internal fun shouldSwitchHomeCategoryFromPager(
    hasSyncedPagerWithState: Boolean,
    pagerCurrentPage: Int,
    pagerScrolling: Boolean,
    currentCategoryIndex: Int
): Boolean {
    if (!hasSyncedPagerWithState) return false
    if (pagerScrolling) return false
    return pagerCurrentPage != currentCategoryIndex
}

internal fun resolveHomePagerSettledAction(
    hasSyncedPagerWithState: Boolean,
    pagerCurrentPage: Int,
    pagerScrolling: Boolean,
    currentCategoryIndex: Int,
    settledCategory: HomeCategory?
): HomePagerSettledAction {
    if (!shouldSwitchHomeCategoryFromPager(
            hasSyncedPagerWithState = hasSyncedPagerWithState,
            pagerCurrentPage = pagerCurrentPage,
            pagerScrolling = pagerScrolling,
            currentCategoryIndex = currentCategoryIndex
        )
    ) {
        return HomePagerSettledAction.NONE
    }

    return if (settledCategory == HomeCategory.LIVE) {
        HomePagerSettledAction.OPEN_LIVE_PAGE
    } else {
        HomePagerSettledAction.SWITCH_CATEGORY
    }
}

internal fun shouldUseInitialHomePagerSnap(
    hasSyncedPagerWithState: Boolean,
    targetPage: Int
): Boolean {
    return !hasSyncedPagerWithState && targetPage >= 0
}

internal fun shouldAnimateHomePagerToCategory(
    hasSyncedPagerWithState: Boolean,
    targetPage: Int,
    pagerCurrentPage: Int,
    pagerScrolling: Boolean,
    programmaticPageSwitchInProgress: Boolean
): Boolean {
    if (!hasSyncedPagerWithState) return false
    if (targetPage < 0) return false
    if (targetPage == pagerCurrentPage) return false
    if (pagerScrolling) return false
    if (programmaticPageSwitchInProgress) return false
    return true
}
