package com.android.purebilibili.feature.message

import com.android.purebilibili.data.model.response.PrivateMessageItem

object ChatMessageMutationPolicy {

    fun markWithdrawn(
        messages: List<PrivateMessageItem>,
        msgKey: Long
    ): List<PrivateMessageItem> {
        return messages.map { message ->
            if (message.msg_key == msgKey) {
                message.copy(msg_status = 1)
            } else {
                message
            }
        }
    }
}
