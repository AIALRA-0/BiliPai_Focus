package com.android.purebilibili.feature.message

import com.android.purebilibili.data.model.response.MessageFeedUnreadData
import com.android.purebilibili.data.model.response.MessageUnreadData
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageCenterUnreadPolicyTest {

    @Test
    fun totalPrivateUnreadCount_sumsFollowAndUnfollowBuckets() {
        val unread = MessageUnreadData(
            unfollow_unread = 2,
            follow_unread = 5
        )

        assertEquals(7, totalPrivateUnreadCount(unread))
    }

    @Test
    fun totalMessageUnreadCount_sumsPrivateAndFeedBuckets() {
        val privateUnread = MessageUnreadData(
            unfollow_unread = 2,
            follow_unread = 5,
            dustbin_unread = 3,
            custom_unread = 4
        )
        val feedUnread = MessageFeedUnreadData(
            reply = 6,
            at = 7,
            like = 8,
            sysMsg = 9
        )

        assertEquals(44, totalMessageUnreadCount(privateUnread, feedUnread))
    }

    @Test
    fun totalMessageUnreadCount_treatsMissingDataAsZero() {
        assertEquals(0, totalMessageUnreadCount(unreadData = null, feedUnread = null))
    }

    @Test
    fun buildMessageCenterTopItems_mapsUnreadCountsInExpectedOrder() {
        val items = buildMessageCenterTopItems(
            feedUnread = MessageFeedUnreadData(
                reply = 12,
                at = 3,
                like = 7,
                sysMsg = 1
            )
        )

        assertEquals(listOf("回复我的", "@我", "收到的赞", "系统通知"), items.map { it.title })
        assertEquals(listOf(12, 3, 7, 1), items.map { it.unreadCount })
    }
}
