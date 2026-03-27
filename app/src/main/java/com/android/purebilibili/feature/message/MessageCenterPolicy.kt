package com.android.purebilibili.feature.message

import com.android.purebilibili.data.model.response.MessageFeedUnreadData
import com.android.purebilibili.data.model.response.MessageUnreadData

data class MessageCenterTopItem(
    val title: String,
    val unreadCount: Int,
    val destination: MessageCenterDestination
)

enum class MessageCenterDestination {
    ReplyMe,
    AtMe,
    LikeMe,
    SystemNotice
}

fun totalPrivateUnreadCount(unreadData: MessageUnreadData?): Int {
    if (unreadData == null) return 0
    return unreadData.follow_unread + unreadData.unfollow_unread
}

fun buildMessageCenterTopItems(feedUnread: MessageFeedUnreadData?): List<MessageCenterTopItem> {
    return listOf(
        MessageCenterTopItem(
            title = "回复我的",
            unreadCount = feedUnread?.reply ?: 0,
            destination = MessageCenterDestination.ReplyMe
        ),
        MessageCenterTopItem(
            title = "@我",
            unreadCount = feedUnread?.at ?: 0,
            destination = MessageCenterDestination.AtMe
        ),
        MessageCenterTopItem(
            title = "收到的赞",
            unreadCount = feedUnread?.like ?: 0,
            destination = MessageCenterDestination.LikeMe
        ),
        MessageCenterTopItem(
            title = "系统通知",
            unreadCount = feedUnread?.sysMsg ?: 0,
            destination = MessageCenterDestination.SystemNotice
        )
    )
}
