package com.android.purebilibili.feature.dynamic.components

import com.android.purebilibili.data.model.response.ReplyItem

internal data class DynamicCommentActionCapabilities(
    val canDelete: Boolean,
    val canReport: Boolean,
    val canToggleTop: Boolean,
    val isCurrentlyTop: Boolean,
)

internal fun resolveDynamicCommentActionCapabilities(
    reply: ReplyItem,
    dynamicAuthorMid: Long,
    currentUserMid: Long?,
): DynamicCommentActionCapabilities {
    val currentMid = currentUserMid?.takeIf { it > 0L } ?: 0L
    val replyAuthorMid = reply.member.mid.toLongOrNull()?.takeIf { it > 0L } ?: reply.mid
    val isDynamicAuthor = currentMid > 0L && currentMid == dynamicAuthorMid
    val isReplyAuthor = currentMid > 0L && currentMid == replyAuthorMid
    return DynamicCommentActionCapabilities(
        canDelete = isReplyAuthor,
        canReport = currentMid > 0L && !isReplyAuthor,
        canToggleTop = isDynamicAuthor && reply.root == 0L,
        isCurrentlyTop = reply.replyControl?.isUpTop == true,
    )
}
