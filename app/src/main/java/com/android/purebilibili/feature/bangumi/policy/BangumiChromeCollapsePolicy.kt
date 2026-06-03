package com.android.purebilibili.feature.bangumi

private const val BANGUMI_INDEX_CHROME_COLLAPSE_OFFSET_PX = 120
private const val BANGUMI_INDEX_BACK_TO_TOP_OFFSET_PX = 600

internal fun shouldCollapseBangumiIndexChrome(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int
): Boolean {
    if (firstVisibleItemIndex > 0) return true
    return firstVisibleItemScrollOffset >= BANGUMI_INDEX_CHROME_COLLAPSE_OFFSET_PX
}

internal fun shouldShowBangumiIndexBackToTop(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int
): Boolean {
    if (firstVisibleItemIndex > 1) return true
    if (firstVisibleItemIndex == 1) return true
    return firstVisibleItemScrollOffset >= BANGUMI_INDEX_BACK_TO_TOP_OFFSET_PX
}
