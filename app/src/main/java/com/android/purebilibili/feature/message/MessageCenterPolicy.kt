package com.android.purebilibili.feature.message

import com.android.purebilibili.data.model.response.MessageFeedUnreadData
import com.android.purebilibili.data.model.response.MessageUnreadData

data class MessageCenterTopItem(
    val title: String,
    val unreadCount: Int,
    val destination: MessageCenterDestination
)

data class MessageSessionCategoryItem(
    val category: MessageSessionCategory,
    val unreadCount: Int
)

enum class MessageCenterDestination {
    ReplyMe,
    AtMe,
    LikeMe,
    SystemNotice
}

enum class MessageSessionCategory(
    val title: String,
    val apiSessionType: Int
) {
    All("全部", 4),
    Follow("关注", 9),
    Unfollow("未关注", 2),
    Stranger("陌生人", 8),
    Group("粉丝团", 3),
    Dustbin("拦截", 5),
    System("系统", 7)
}

fun totalPrivateUnreadCount(unreadData: MessageUnreadData?): Int {
    if (unreadData == null) return 0
    return unreadData.follow_unread +
        unreadData.unfollow_unread +
        unreadData.dustbin_unread +
        unreadData.custom_unread
}

fun totalMessageUnreadCount(
    unreadData: MessageUnreadData?,
    feedUnread: MessageFeedUnreadData?
): Int {
    val feedUnreadCount = if (feedUnread == null) {
        0
    } else {
        feedUnread.reply + feedUnread.at + feedUnread.like + feedUnread.sysMsg
    }
    return totalPrivateUnreadCount(unreadData) + feedUnreadCount
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

fun buildMessageSessionCategoryItems(unreadData: MessageUnreadData?): List<MessageSessionCategoryItem> {
    return MessageSessionCategory.values().map { category ->
        MessageSessionCategoryItem(
            category = category,
            unreadCount = unreadCountForCategory(category, unreadData)
        )
    }
}

fun unreadCountForCategory(
    category: MessageSessionCategory,
    unreadData: MessageUnreadData?
): Int {
    if (unreadData == null) return 0
    return when (category) {
        MessageSessionCategory.All -> totalPrivateUnreadCount(unreadData)
        MessageSessionCategory.Follow -> unreadData.follow_unread
        MessageSessionCategory.Unfollow,
        MessageSessionCategory.Stranger -> unreadData.unfollow_unread
        MessageSessionCategory.Group -> 0
        MessageSessionCategory.Dustbin -> unreadData.dustbin_unread
        MessageSessionCategory.System -> unreadData.custom_unread
    }
}
