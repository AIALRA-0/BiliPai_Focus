package com.android.purebilibili.data.repository

internal sealed interface MessageReadEvent {
    data object Changed : MessageReadEvent

    data class PrivateSession(
        val talkerId: Long,
        val sessionType: Int,
        val ackSeqno: Long,
    ) : MessageReadEvent
}
