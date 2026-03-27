package com.android.purebilibili.feature.message

import com.android.purebilibili.data.model.response.PrivateMessageItem
import kotlin.test.Test
import kotlin.test.assertEquals

class ChatMessageMutationPolicyTest {

    @Test
    fun markWithdrawn_updatesOnlyTargetMessageStatus() {
        val original = listOf(
            PrivateMessageItem(msg_key = 11L, msg_status = 0, content = """{"content":"first"}"""),
            PrivateMessageItem(msg_key = 22L, msg_status = 0, content = """{"content":"second"}""")
        )

        val updated = ChatMessageMutationPolicy.markWithdrawn(
            messages = original,
            msgKey = 22L
        )

        assertEquals(0, updated[0].msg_status)
        assertEquals(1, updated[1].msg_status)
        assertEquals("""{"content":"second"}""", updated[1].content)
    }
}
