package com.android.purebilibili.feature.dynamic

import com.android.purebilibili.core.util.appendDistinctByKey
import com.android.purebilibili.core.util.prependDistinctByKey
import com.android.purebilibili.data.model.response.DynamicItem

internal fun resolveDynamicListTopPaddingExtraDp(
    isHorizontalMode: Boolean,
    isHorizontalUserListCollapsed: Boolean = false
): Int {
    return if (isHorizontalMode && !isHorizontalUserListCollapsed) 168 else 100
}

internal fun shouldCollapseDynamicHorizontalUserList(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
    topTolerancePx: Int = 8
): Boolean {
    return firstVisibleItemIndex > 0 || firstVisibleItemScrollOffset > topTolerancePx
}

internal fun resolveDynamicSelectedUserIdAfterClick(
    selectedUserId: Long?,
    clickedUserId: Long?
): Long? {
    if (clickedUserId == null) return null
    return if (selectedUserId == clickedUserId) null else clickedUserId
}

internal fun shouldUseSelectedUserDynamicFeed(
    selectedTab: Int,
    selectedUserId: Long?
): Boolean {
    return selectedTab == 4 && selectedUserId != null
}

internal fun resolveDynamicTabAfterUserSelection(
    selectedUserId: Long?,
    clickedUserId: Long?,
    currentTab: Int
): Int {
    val nextUserId = resolveDynamicSelectedUserIdAfterClick(selectedUserId, clickedUserId)
    return when {
        nextUserId != null -> 4
        currentTab == 4 -> 0
        else -> currentTab
    }
}

internal fun resolveDynamicSelectedTab(
    savedTab: Int?,
    tabCount: Int
): Int {
    if (tabCount <= 0) return 0
    return savedTab?.takeIf { it in 0 until tabCount } ?: 0
}

internal fun resolveDynamicSwipeTargetTab(
    currentTab: Int,
    tabCount: Int,
    dragDistancePx: Float,
    thresholdPx: Float = 96f
): Int? {
    if (tabCount <= 0 || currentTab !in 0 until tabCount) return null
    if (kotlin.math.abs(dragDistancePx) < thresholdPx) return null
    val target = if (dragDistancePx < 0f) currentTab + 1 else currentTab - 1
    return target.takeIf { it in 0 until tabCount && it != currentTab }
}

internal fun resolveDynamicFeedRequestType(selectedTab: Int): String {
    return when (selectedTab) {
        1 -> "video"
        2 -> "pgc"
        3 -> "article"
        else -> "all"
    }
}

internal fun shouldUseServerFilteredDynamicFeed(selectedTab: Int): Boolean {
    return selectedTab in 1..3
}

internal fun resolveHorizontalUserListVerticalPaddingDp(): Int {
    return 4
}

internal fun shouldShowDynamicErrorOverlay(
    error: String?,
    activeItemsCount: Int
): Boolean {
    return !error.isNullOrBlank() && activeItemsCount == 0
}

internal fun shouldShowDynamicLoadingFooter(
    isLoading: Boolean,
    activeItemsCount: Int
): Boolean {
    return isLoading && activeItemsCount > 0
}

internal fun shouldShowDynamicNoMoreFooter(
    hasMore: Boolean,
    activeItemsCount: Int
): Boolean {
    return !hasMore && activeItemsCount > 0
}

internal fun shouldShowDynamicCommentSheet(selectedDynamicId: String?): Boolean {
    return !selectedDynamicId.isNullOrBlank()
}

internal fun resolveDynamicCommentSheetTotalCount(
    liveCount: Int,
    fallbackCount: Int
): Int {
    return if (liveCount > 0) liveCount else fallbackCount.coerceAtLeast(0)
}

internal fun shouldResetFollowedUserListToTopOnRefresh(
    boundaryKey: String?,
    prependedCount: Int,
    selectedUserId: Long?,
    handledBoundaryKey: String?
): Boolean {
    if (boundaryKey.isNullOrBlank()) return false
    if (prependedCount <= 0) return false
    if (selectedUserId != null) return false
    return boundaryKey != handledBoundaryKey
}

enum class DynamicFeedErrorSource {
    NONE,
    INITIAL_LOAD,
    REFRESH,
    APPEND
}

internal fun resolveDynamicActiveLoadingState(
    currentState: DynamicUiState,
    selectedUserId: Long?
): Boolean {
    return if (selectedUserId != null) currentState.userIsLoading else currentState.isLoading
}

internal fun resolveDynamicActiveError(
    currentState: DynamicUiState,
    selectedUserId: Long?
): String? {
    return if (selectedUserId != null) currentState.userError else currentState.error
}

internal fun resolveDynamicFeedStateForLoadStart(
    currentState: DynamicUiState,
    refresh: Boolean,
    showLoading: Boolean
): DynamicUiState {
    val baseState = currentState.copy(
        error = null,
        errorSource = DynamicFeedErrorSource.NONE
    )
    return when {
        refresh && showLoading -> baseState.copy(isLoading = true)
        !refresh -> baseState.copy(isLoading = true)
        else -> baseState
    }
}

internal fun resolveDynamicFeedStateAfterSuccess(
    currentState: DynamicUiState,
    incomingItems: List<DynamicItem>,
    isRefresh: Boolean,
    requestType: String,
    incrementalRefreshEnabled: Boolean,
    hasMore: Boolean
): DynamicUiState {
    val currentItems = currentState.items
    val canUseIncrementalRefresh = isRefresh &&
        incrementalRefreshEnabled &&
        currentState.timelineRequestType == requestType
    val mergedItems = when {
        canUseIncrementalRefresh -> prependDistinctByKey(
            existing = currentItems,
            incoming = incomingItems,
            keySelector = ::dynamicFeedItemKey
        )
        isRefresh -> incomingItems
        else -> appendDistinctByKey(
            existing = currentItems,
            incoming = incomingItems,
            keySelector = ::dynamicFeedItemKey
        )
    }
    val boundary = when {
        canUseIncrementalRefresh -> resolveIncrementalRefreshBoundary(
            existingKeys = currentItems.map(::dynamicFeedItemKey),
            mergedKeys = mergedItems.map(::dynamicFeedItemKey)
        )
        isRefresh -> IncrementalRefreshBoundary(
            boundaryKey = null,
            prependedCount = 0
        )
        else -> IncrementalRefreshBoundary(
            boundaryKey = currentState.incrementalRefreshBoundaryKey,
            prependedCount = currentState.incrementalPrependedCount
        )
    }
    return currentState.copy(
        items = mergedItems,
        isLoading = false,
        error = null,
        errorSource = DynamicFeedErrorSource.NONE,
        hasMore = hasMore,
        timelineRequestType = requestType,
        incrementalRefreshBoundaryKey = boundary.boundaryKey,
        incrementalPrependedCount = boundary.prependedCount
    )
}

internal fun resolveDynamicFeedStateAfterFailure(
    currentState: DynamicUiState,
    errorMessage: String,
    refresh: Boolean
): DynamicUiState {
    val source = when {
        currentState.items.isEmpty() -> DynamicFeedErrorSource.INITIAL_LOAD
        refresh -> DynamicFeedErrorSource.REFRESH
        else -> DynamicFeedErrorSource.APPEND
    }
    return currentState.copy(
        isLoading = false,
        error = errorMessage,
        errorSource = source
    )
}
