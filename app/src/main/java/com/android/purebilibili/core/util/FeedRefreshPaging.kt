package com.android.purebilibili.core.util

/**
 * 手动下拉刷新时，用「下一页替换当前列表」来换一批内容。
 * [nextLoadPage] 是 loadMore 将要请求的页码；没有更多时回到第 1 页。
 */
fun resolveReplaceRefreshPage(
    nextLoadPage: Int,
    hasMore: Boolean
): Int {
    if (!hasMore) return 1
    return nextLoadPage.coerceAtLeast(1)
}
