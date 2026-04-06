package com.android.purebilibili.feature.dynamic

internal fun shouldShowDynamicBackToTop(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int
): Boolean {
    if (firstVisibleItemIndex > 1) return true
    if (firstVisibleItemIndex == 1) return true
    return firstVisibleItemScrollOffset >= 600
}
