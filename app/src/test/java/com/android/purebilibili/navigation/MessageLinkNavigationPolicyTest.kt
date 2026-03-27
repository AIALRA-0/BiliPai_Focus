package com.android.purebilibili.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MessageLinkNavigationPolicyTest {

    @Test
    fun resolveMessageLinkNavigationAction_routesAidDeepLinkToVideo() {
        val action = resolveMessageLinkNavigationAction(
            "bilibili://video/115391124741470?page=0&comment_root_id=279569905408"
        )

        val videoAction = assertIs<MessageLinkNavigationAction.Video>(action)
        assertEquals("av115391124741470", videoAction.videoId)
    }
}
