package com.android.purebilibili.feature.dynamic

enum class DynamicScrollRequest {
    SCROLL_TO_TOP,
    SCROLL_TO_TOP_OR_REFRESH,
    SCROLL_TO_TOP_AND_REFRESH,
}

data class DynamicScrollActionPlan(
    val shouldScrollToTop: Boolean,
    val shouldRefresh: Boolean,
)

fun resolveDynamicScrollActionPlan(
    request: DynamicScrollRequest,
    isAtTop: Boolean,
): DynamicScrollActionPlan {
    val shouldScrollToTop = !isAtTop
    val shouldRefresh = when (request) {
        DynamicScrollRequest.SCROLL_TO_TOP -> false
        DynamicScrollRequest.SCROLL_TO_TOP_OR_REFRESH -> isAtTop
        DynamicScrollRequest.SCROLL_TO_TOP_AND_REFRESH -> true
    }
    return DynamicScrollActionPlan(
        shouldScrollToTop = shouldScrollToTop,
        shouldRefresh = shouldRefresh,
    )
}

internal fun shouldShowDynamicBackToTop(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int
): Boolean {
    if (firstVisibleItemIndex > 1) return true
    if (firstVisibleItemIndex == 1) return true
    return firstVisibleItemScrollOffset >= 600
}

