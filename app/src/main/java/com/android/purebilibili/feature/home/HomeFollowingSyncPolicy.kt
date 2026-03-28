package com.android.purebilibili.feature.home

import com.android.purebilibili.core.store.FocusFollowGroupConfig

internal enum class HomeFollowingSnapshotChangeKind {
    NONE,
    REMOVED_ONLY,
    RELOAD_REQUIRED
}

internal data class HomeFollowingSnapshotChange(
    val nextFollowingMids: Set<Long>,
    val previousVisibleMids: Set<Long>,
    val nextVisibleMids: Set<Long>,
    val removedVisibleMids: Set<Long>,
    val addedVisibleMids: Set<Long>,
    val kind: HomeFollowingSnapshotChangeKind
)

internal fun resolveHomeFollowingSnapshotChange(
    previousFollowingMids: Set<Long>,
    nextFollowingMids: Set<Long>,
    blockedMids: Set<Long>,
    config: FocusFollowGroupConfig,
    filterEnabled: Boolean
): HomeFollowingSnapshotChange {
    val previousVisibleMids = resolveVisibleHomeFollowUserMids(
        followingMids = previousFollowingMids,
        blockedMids = blockedMids,
        config = config,
        filterEnabled = filterEnabled
    ).toSet()
    val nextVisibleMids = resolveVisibleHomeFollowUserMids(
        followingMids = nextFollowingMids,
        blockedMids = blockedMids,
        config = config,
        filterEnabled = filterEnabled
    ).toSet()
    val removedVisibleMids = previousVisibleMids - nextVisibleMids
    val addedVisibleMids = nextVisibleMids - previousVisibleMids
    val kind = when {
        removedVisibleMids.isEmpty() && addedVisibleMids.isEmpty() ->
            HomeFollowingSnapshotChangeKind.NONE
        removedVisibleMids.isNotEmpty() && addedVisibleMids.isEmpty() ->
            HomeFollowingSnapshotChangeKind.REMOVED_ONLY
        else -> HomeFollowingSnapshotChangeKind.RELOAD_REQUIRED
    }

    return HomeFollowingSnapshotChange(
        nextFollowingMids = nextFollowingMids,
        previousVisibleMids = previousVisibleMids,
        nextVisibleMids = nextVisibleMids,
        removedVisibleMids = removedVisibleMids,
        addedVisibleMids = addedVisibleMids,
        kind = kind
    )
}

internal fun shouldRefreshHomeFollowAfterFollowingChange(
    hasResolvedFollowFeedOnce: Boolean,
    rawFollowFeedCount: Int,
    displayedFollowFeedCount: Int,
    currentCategory: HomeCategory
): Boolean {
    return hasResolvedFollowFeedOnce ||
        rawFollowFeedCount > 0 ||
        displayedFollowFeedCount > 0 ||
        currentCategory == HomeCategory.FOLLOW
}
