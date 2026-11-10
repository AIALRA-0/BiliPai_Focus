package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.ReplyData
import com.android.purebilibili.data.model.response.ReplyPage
import com.android.purebilibili.data.model.response.ReplyItem

/**
 * 评论分页与楼中楼计数的纯判定规则。原实现位于手机端 VideoCommentViewModel /
 * CommentPaginationPolicy / SubReplySortPolicy，TV 评论阅读消费同一判定，
 * 故下沉共享；手机端原包通过同签名委托保留入口。
 */
enum class SubReplySortMode(val apiMode: Int, val label: String) {
    TIME(2, "按时间"),
    HOT(3, "按热度");

    fun toggled(): SubReplySortMode = if (this == TIME) HOT else TIME
}

data class CommentPageResolution(
    val totalCount: Int,
    val isEnd: Boolean
)

fun resolveCommentPageResolution(
    data: ReplyData,
    pageToLoad: Int,
    previousRepliesSize: Int,
    combinedRepliesSize: Int,
    newRepliesSize: Int,
    fallbackCount: Int
): CommentPageResolution {
    val totalCount = maxOf(
        data.getAllCount(),
        fallbackCount,
        combinedRepliesSize.coerceAtLeast(0)
    )
    val uniqueGrowth = (combinedRepliesSize - previousRepliesSize).coerceAtLeast(0)
    val hasCursorPaginationSignal =
        data.cursor.allCount > 0 || data.cursor.next > 0 || data.cursor.isEnd
    val isEnd = if (hasCursorPaginationSignal) {
        data.cursor.isEnd || (pageToLoad > 1 && uniqueGrowth == 0)
    } else {
        data.getIsEnd(pageToLoad, combinedRepliesSize) ||
            (newRepliesSize == 0 && combinedRepliesSize == 0) ||
            (pageToLoad > 1 && uniqueGrowth == 0)
    }
    return CommentPageResolution(
        totalCount = totalCount,
        isEnd = isEnd
    )
}

fun resolveSubReplyRemoteTotalCount(
    data: ReplyData,
    rootReply: ReplyItem? = null
): Int {
    // 不同接口会把分页窗口大小也写进 page.count；不能把单页数量当总数。
    // 取所有可用声明中的最大值，避免“显示还有 N 条，详情却在首屏结束”。
    return listOf(
        data.page.count,
        data.root?.rcount ?: 0,
        data.root?.count ?: 0,
        data.cursor.allCount,
        rootReply?.rcount ?: 0,
        rootReply?.count ?: 0,
        data.page.acount
    ).filter { it > 0 }.maxOrNull() ?: 0
}

fun resolveSubReplyLoadedTotalCount(
    rootReply: ReplyItem?,
    loadedReplyCount: Int,
    remoteReplyCount: Int,
    previousTotalCount: Int = 0
): Int {
    val rootDeclaredCount = maxOf(
        rootReply?.count ?: 0,
        rootReply?.rcount ?: 0,
        rootReply?.replies.orEmpty().size
    )
    return maxOf(
        previousTotalCount,
        rootDeclaredCount,
        remoteReplyCount,
        loadedReplyCount
    ).coerceAtLeast(0)
}

fun resolveSubReplyPageEnd(
    cursorIsEnd: Boolean,
    fetchedReplyCount: Int,
    loadedReplyCount: Int,
    remoteReplyCount: Int,
    requestedPage: Int = 1,
    pageSize: Int = 20,
    restPage: ReplyPage = ReplyPage()
): Boolean {
    val safeLoadedCount = loadedReplyCount.coerceAtLeast(0)
    val declaredTotal = maxOf(restPage.count, remoteReplyCount).coerceAtLeast(0)
    if (declaredTotal > 0 && safeLoadedCount >= declaredTotal) {
        return true
    }
    // x/v2/reply/reply 的 page.count 可能只是窗口上限；分页进度必须以已解析总数为准。
    if (restPage.count > 0 && restPage.num > 0 && restPage.size > 0) {
        if (restPage.num * restPage.size < declaredTotal) {
            return false
        }
        return fetchedReplyCount <= 0 || safeLoadedCount >= declaredTotal
    }
    if (declaredTotal > safeLoadedCount) {
        // 楼中楼接口可能因审核或折叠导致中间页很稀疏，不能因单页为空提前结束。
        // 最多探测到外层声明总数对应的理论末页，避免异常计数导致无限请求。
        val safePageSize = pageSize.coerceAtLeast(1)
        val expectedLastPage = (declaredTotal + safePageSize - 1) / safePageSize
        return requestedPage.coerceAtLeast(1) >= expectedLastPage
    }
    return cursorIsEnd || fetchedReplyCount <= 0
}

fun isSortedSubReplyPageEnd(cursorIsEnd: Boolean, nextOffset: String?): Boolean =
    cursorIsEnd || nextOffset.isNullOrBlank()
