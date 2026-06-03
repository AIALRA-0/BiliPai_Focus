package com.android.purebilibili.feature.message

import com.android.purebilibili.data.model.response.SessionItem

internal data class InboxSessionPageMergeResult(
    val sessions: List<SessionItem>,
    val newSessions: List<SessionItem>,
    val nextEndTs: Long,
    val hasMore: Boolean,
    val shouldRetryWithOlderCursor: Boolean
)

internal object InboxSessionPaginationPolicy {

    fun resolveSessionKey(session: SessionItem): String {
        if (session.talker_id > 0L) {
            return "${session.talker_id}_${session.session_type}"
        }

        val lastMessage = session.last_msg
        val fallback = when {
            lastMessage?.msg_key != null && lastMessage.msg_key > 0L -> "msg_${lastMessage.msg_key}"
            lastMessage?.msg_seqno != null && lastMessage.msg_seqno > 0L -> "seq_${lastMessage.msg_seqno}"
            session.session_ts > 0L -> "session_ts_${session.session_ts}"
            lastMessage?.timestamp != null && lastMessage.timestamp > 0L -> {
                "time_${lastMessage.timestamp}_${lastMessage.sender_uid}_${lastMessage.msg_type}"
            }
            else -> "content_${lastMessage?.content.orEmpty().hashCode()}_${session.biz}_${session.status}"
        }
        return "invalid_${session.talker_id}_${session.session_type}_$fallback"
    }

    fun normalizeSessions(sessions: List<SessionItem>): List<SessionItem> =
        sortSessions(sessions).distinctBy(::resolveSessionKey)

    fun sortSessions(sessions: List<SessionItem>): List<SessionItem> =
        sessions.sortedWith(
            compareByDescending<SessionItem> { it.top_ts }
                .thenByDescending { it.session_ts }
        )

    fun resolveNextEndTs(sessions: List<SessionItem>, requestedEndTs: Long = 0L): Long {
        val normalized = normalizeSessionEndTs(sessions.lastOrNull()?.session_ts ?: 0L)
        if (normalized <= 0L) return 0L

        // get_sessions 的 end_ts 可能返回边界会话，下一次请求必须严格更早，避免重复页卡住。
        val strictOlderCursor = normalized - 1L
        return if (requestedEndTs > 0L && strictOlderCursor >= requestedEndTs) {
            (requestedEndTs - 1L).coerceAtLeast(0L)
        } else {
            strictOlderCursor
        }
    }

    fun mergePage(
        existing: List<SessionItem>,
        incoming: List<SessionItem>,
        responseHasMore: Boolean,
        requestedEndTs: Long,
        canRetryDuplicatePage: Boolean = true
    ): InboxSessionPageMergeResult {
        val normalizedExisting = normalizeSessions(existing)
        val existingKeys = normalizedExisting.map(::resolveSessionKey).toSet()
        val newSessions = normalizeSessions(incoming).filter { resolveSessionKey(it) !in existingKeys }
        val nextEndTs = resolveNextEndTs(incoming, requestedEndTs)
        val canAdvanceCursor = nextEndTs > 0L && nextEndTs != requestedEndTs
        val isDuplicatePage = newSessions.isEmpty() && incoming.isNotEmpty()
        val shouldRetry = canRetryDuplicatePage &&
            responseHasMore &&
            isDuplicatePage &&
            canAdvanceCursor
        val hasMore = responseHasMore &&
            nextEndTs > 0L &&
            (newSessions.isNotEmpty() || shouldRetry)

        return InboxSessionPageMergeResult(
            sessions = normalizeSessions(normalizedExisting + newSessions),
            newSessions = newSessions,
            nextEndTs = nextEndTs,
            hasMore = hasMore,
            shouldRetryWithOlderCursor = shouldRetry
        )
    }

    private fun normalizeSessionEndTs(sessionTs: Long): Long {
        if (sessionTs <= 0L) return 0L
        return when {
            sessionTs >= 1_000_000_000_000_000L -> sessionTs
            sessionTs >= 1_000_000_000_000L -> sessionTs * 1_000L
            else -> sessionTs * 1_000_000L
        }
    }
}
