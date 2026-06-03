package com.android.purebilibili.feature.message

import com.android.purebilibili.data.model.response.SessionItem
import com.android.purebilibili.data.model.response.SessionMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InboxSessionPaginationPolicyTest {

    @Test
    fun resolveNextEndTs_returnsStrictlyOlderMicrosecondCursor() {
        val sessions = listOf(SessionItem(talker_id = 1L, session_ts = 1_712_305_278_098_351L))

        val nextEndTs = InboxSessionPaginationPolicy.resolveNextEndTs(sessions)

        assertEquals(1_712_305_278_098_350L, nextEndTs)
    }

    @Test
    fun mergePage_requestsRetryWhenCursorPageOnlyContainsExistingSessions() {
        val existing = listOf(SessionItem(talker_id = 1L, session_type = 1, session_ts = 100L))
        val incoming = listOf(SessionItem(talker_id = 1L, session_type = 1, session_ts = 100L))

        val result = InboxSessionPaginationPolicy.mergePage(
            existing = existing,
            incoming = incoming,
            responseHasMore = true,
            requestedEndTs = 100_000_000L
        )

        assertTrue(result.shouldRetryWithOlderCursor)
        assertEquals(99_999_999L, result.nextEndTs)
    }

    @Test
    fun mergePage_stopsHasMoreWhenDuplicatePageCannotRetry() {
        val existing = listOf(SessionItem(talker_id = 1L, session_type = 1, session_ts = 100L))
        val incoming = listOf(SessionItem(talker_id = 1L, session_type = 1, session_ts = 100L))

        val result = InboxSessionPaginationPolicy.mergePage(
            existing = existing,
            incoming = incoming,
            responseHasMore = true,
            requestedEndTs = 100_000_000L,
            canRetryDuplicatePage = false
        )

        assertFalse(result.shouldRetryWithOlderCursor)
        assertFalse(result.hasMore)
    }

    @Test
    fun resolveSessionKey_usesMessageIdentityForInvalidTalkerSessions() {
        val first = SessionItem(
            talker_id = 0L,
            session_type = 1,
            last_msg = SessionMessage(msg_key = 101L)
        )
        val second = SessionItem(
            talker_id = 0L,
            session_type = 1,
            last_msg = SessionMessage(msg_key = 102L)
        )

        assertEquals("invalid_0_1_msg_101", InboxSessionPaginationPolicy.resolveSessionKey(first))
        assertEquals("invalid_0_1_msg_102", InboxSessionPaginationPolicy.resolveSessionKey(second))
    }

    @Test
    fun normalizeSessions_deduplicatesExactInvalidTalkerSessionsBeforeUiList() {
        val duplicate = SessionItem(
            talker_id = 0L,
            session_type = 1,
            session_ts = 1_712_305_278L,
            last_msg = SessionMessage(msg_key = 0L, msg_seqno = 1L)
        )

        val result = InboxSessionPaginationPolicy.normalizeSessions(listOf(duplicate, duplicate))

        assertEquals(1, result.size)
        assertEquals("invalid_0_1_seq_1", InboxSessionPaginationPolicy.resolveSessionKey(result.first()))
    }
}
