package com.android.purebilibili.feature.video.viewmodel

import com.android.purebilibili.data.model.response.ReplyData

// 分页判定规则已下沉 core-data；原包入口保留，手机各调用方不感知迁移。
internal data class CommentPageResolution(
    val totalCount: Int,
    val isEnd: Boolean
)

internal fun resolveCommentPageResolution(
    data: ReplyData,
    pageToLoad: Int,
    previousRepliesSize: Int,
    combinedRepliesSize: Int,
    newRepliesSize: Int,
    fallbackCount: Int
): CommentPageResolution {
    val shared = com.android.purebilibili.data.repository.resolveCommentPageResolution(
        data = data,
        pageToLoad = pageToLoad,
        previousRepliesSize = previousRepliesSize,
        combinedRepliesSize = combinedRepliesSize,
        newRepliesSize = newRepliesSize,
        fallbackCount = fallbackCount,
    )
    return CommentPageResolution(shared.totalCount, shared.isEnd)
}
