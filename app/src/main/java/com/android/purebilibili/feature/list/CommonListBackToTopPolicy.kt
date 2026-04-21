package com.android.purebilibili.feature.list

internal fun shouldShowCommonListBackToTop(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int
): Boolean {
    if (firstVisibleItemIndex > 1) return true
    if (firstVisibleItemIndex == 1) return true
    return firstVisibleItemScrollOffset >= 600
}
