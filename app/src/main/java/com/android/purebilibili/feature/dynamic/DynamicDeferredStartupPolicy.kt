package com.android.purebilibili.feature.dynamic

internal fun createDynamicStartupUiState(selectedTab: Int = 0): DynamicUiState {
    return DynamicUiState(
        timelineRequestType = resolveDynamicFeedRequestType(selectedTab),
        isLoading = true,
    )
}

/** A delayed cache placeholder cannot replace another account or any started timeline request. */
internal fun shouldApplyDeferredDynamicCache(
    sameAccount: Boolean,
    requestTokenBeforeRead: Long?,
    currentRequestToken: Long?,
    hasTimelineItems: Boolean,
): Boolean = sameAccount && requestTokenBeforeRead == null && currentRequestToken == null && !hasTimelineItems
