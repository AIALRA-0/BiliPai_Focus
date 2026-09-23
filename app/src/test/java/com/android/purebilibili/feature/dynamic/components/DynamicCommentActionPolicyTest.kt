package com.android.purebilibili.feature.dynamic.components

import com.android.purebilibili.data.model.response.ReplyControl
import com.android.purebilibili.data.model.response.ReplyItem
import com.android.purebilibili.data.model.response.ReplyMember
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicCommentActionPolicyTest {

    @Test
    fun dynamicAuthorCanDeleteAndTopRootComment() {
        val reply = ReplyItem(
            rpid = 1L,
            root = 0L,
            member = ReplyMember(mid = "22"),
            replyControl = ReplyControl(isUpTop = true),
        )

        val actions = resolveDynamicCommentActionCapabilities(
            reply = reply,
            dynamicAuthorMid = 11L,
            currentUserMid = 11L,
        )

        assertTrue(actions.canDelete)
        assertTrue(actions.canToggleTop)
        assertTrue(actions.isCurrentlyTop)
        assertTrue(actions.canReport)
    }

    @Test
    fun replyAuthorCanDeleteButCannotReportOrTopOwnComment() {
        val reply = ReplyItem(rpid = 1L, member = ReplyMember(mid = "22"))

        val actions = resolveDynamicCommentActionCapabilities(
            reply = reply,
            dynamicAuthorMid = 11L,
            currentUserMid = 22L,
        )

        assertTrue(actions.canDelete)
        assertFalse(actions.canToggleTop)
        assertFalse(actions.canReport)
    }
}
