package com.android.purebilibili.feature.video.ui.components

internal fun shouldShowVideoCommentBackToTop(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int
): Boolean {
    if (firstVisibleItemIndex > 1) return true
    if (firstVisibleItemIndex == 1) return true
    return firstVisibleItemScrollOffset >= 600
}
