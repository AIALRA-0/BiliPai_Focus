package com.android.purebilibili.feature.dynamic

import com.android.purebilibili.data.model.response.ReplyItem

internal fun canOpenDynamicSubReplies(reply: ReplyItem): Boolean {
    return resolveDynamicSubReplyCount(reply) > 0
}

internal fun shouldOpenDynamicCommentThreadOnTap(reply: ReplyItem): Boolean {
    return reply.rpid > 0L
}

internal enum class DynamicCommentSheetHostContent {
    MAIN_LIST,
    THREAD_DETAIL,
}

internal fun resolveDynamicCommentSheetHostContent(
    subReplyVisible: Boolean,
): DynamicCommentSheetHostContent {
    return if (subReplyVisible) {
        DynamicCommentSheetHostContent.THREAD_DETAIL
    } else {
        DynamicCommentSheetHostContent.MAIN_LIST
    }
}

/** 信息流点评论后进入动态详情内联评论区，不弹底部评论层。 */
internal fun shouldOpenDynamicCommentsInDetailPage(): Boolean = true

internal fun resolveDynamicSubReplyCount(reply: ReplyItem): Int {
    return maxOf(
        reply.rcount,
        reply.replies.orEmpty().size
    )
}

internal fun resolveDynamicCommentCountLabel(count: Int): String {
    return "${formatDynamicCommentCount(count.coerceAtLeast(0))}条回复"
}

internal fun resolveDynamicCommentEmptyLabel(): String = "还没有评论"

internal fun resolveDynamicCommentComposerHint(replyUname: String? = null): String {
    val uname = replyUname?.trim().orEmpty()
    return if (uname.isNotEmpty()) "回复 @$uname" else "发一条友善的评论"
}

internal fun resolveDynamicCommentImeSubmission(text: String): String? =
    text.trim().takeIf(String::isNotEmpty)

internal fun resolveDynamicCommentLocationLabel(location: String?): String? {
    return location?.trim()?.takeIf { it.isNotEmpty() }
}

internal data class DynamicCommentComposerTarget(
    val rootRpid: Long,
    val parentRpid: Long,
    val uname: String
)

internal fun resolveDynamicCommentReplyTarget(reply: ReplyItem): DynamicCommentComposerTarget {
    val rootRpid = if (reply.root > 0L) reply.root else reply.rpid
    return DynamicCommentComposerTarget(
        rootRpid = rootRpid,
        parentRpid = reply.rpid,
        uname = reply.member.uname
    )
}

internal fun isDynamicCommentLiked(reply: ReplyItem): Boolean = reply.action == 1

internal fun applyDynamicCommentLike(
    reply: ReplyItem,
    toLiked: Boolean
): ReplyItem {
    val currentlyLiked = isDynamicCommentLiked(reply)
    if (currentlyLiked == toLiked) return reply
    val delta = if (toLiked) 1 else -1
    return reply.copy(
        action = if (toLiked) 1 else 0,
        like = (reply.like + delta).coerceAtLeast(0)
    )
}

internal fun applyDynamicCommentLikeInList(
    comments: List<ReplyItem>,
    rpid: Long,
    toLiked: Boolean
): List<ReplyItem> {
    if (rpid <= 0L) return comments
    return comments.map { reply ->
        val updated = if (reply.rpid == rpid) {
            applyDynamicCommentLike(reply, toLiked)
        } else {
            reply
        }
        val nested = updated.replies
        if (nested.isNullOrEmpty()) {
            updated
        } else {
            updated.copy(replies = applyDynamicCommentLikeInList(nested, rpid, toLiked))
        }
    }
}

private fun formatDynamicCommentCount(count: Int): String {
    return when {
        count >= 100_000_000 -> formatScaledCount(count, 100_000_000, "亿")
        count >= 10_000 -> formatScaledCount(count, 10_000, "万")
        else -> count.toString()
    }
}

private fun formatScaledCount(count: Int, unit: Int, suffix: String): String {
    val whole = count / unit
    val tenths = (count % unit) / (unit / 10)
    return if (tenths == 0) "$whole$suffix" else "$whole.$tenths$suffix"
}
