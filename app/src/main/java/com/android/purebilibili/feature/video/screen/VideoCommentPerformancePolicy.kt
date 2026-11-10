package com.android.purebilibili.feature.video.screen

internal fun resolveVideoDetailBeyondViewportPageCount(
    selectedTabIndex: Int = 0,
    commentPagePreloadReady: Boolean = true,
): Int = if (selectedTabIndex == 1 || !commentPagePreloadReady) {
    // 先只测量当前页；首次简介挂载完成并进入评论工作阶段后，才预组合相邻评论页。
    // 这里只控制屏外页，直接打开评论的当前页始终照常挂载。
    0
} else {
    1
}

internal fun shouldLoadMoreVideoComments(
    lastVisibleItemIndex: Int,
    totalItemsCount: Int,
    isLoading: Boolean,
    isEnd: Boolean,
    prefetchThreshold: Int = 3
): Boolean {
    if (isLoading || isEnd) return false
    if (totalItemsCount <= 0 || lastVisibleItemIndex < 0) return false
    return lastVisibleItemIndex >= totalItemsCount - 1 - prefetchThreshold
}

internal fun shouldUseLightweightCommentRendering(
    selectedTabIndex: Int,
    isVideoPlaying: Boolean,
    isCommentListScrolling: Boolean = false
): Boolean {
    if (selectedTabIndex != 1) return false
    // 仅在滚动中或播放中临时降级附属装饰；静止后恢复完整视觉效果。
    return isVideoPlaying || isCommentListScrolling
}
