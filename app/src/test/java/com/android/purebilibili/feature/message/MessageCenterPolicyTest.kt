package com.android.purebilibili.feature.message

import com.android.purebilibili.data.model.response.MessageUnreadData
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageCenterPolicyTest {

    @Test
    fun buildMessageSessionCategoryItems_mapsUnreadCountsToDailyCategories() {
        val unread = MessageUnreadData(
            unfollow_unread = 2,
            follow_unread = 3,
            dustbin_unread = 5,
            custom_unread = 7
        )

        val items = buildMessageSessionCategoryItems(unread)
            .associate { it.category to it.unreadCount }

        assertEquals(17, items[MessageSessionCategory.All])
        assertEquals(3, items[MessageSessionCategory.Follow])
        assertEquals(2, items[MessageSessionCategory.Unfollow])
        assertEquals(2, items[MessageSessionCategory.Stranger])
        assertEquals(5, items[MessageSessionCategory.Dustbin])
        assertEquals(7, items[MessageSessionCategory.System])
    }

    @Test
    fun messageSessionCategory_usesDocumentedApiSessionTypes() {
        assertEquals(4, MessageSessionCategory.All.apiSessionType)
        assertEquals(9, MessageSessionCategory.Follow.apiSessionType)
        assertEquals(2, MessageSessionCategory.Unfollow.apiSessionType)
        assertEquals(8, MessageSessionCategory.Stranger.apiSessionType)
        assertEquals(3, MessageSessionCategory.Group.apiSessionType)
        assertEquals(5, MessageSessionCategory.Dustbin.apiSessionType)
        assertEquals(7, MessageSessionCategory.System.apiSessionType)
    }
}
