package com.android.purebilibili.feature.video.viewmodel

import com.android.purebilibili.data.model.response.ReplyItem
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals

class SubReplyPageRequestTest {
    @Test
    fun `local preview does not turn failed first request into page two`() {
        val state = SubReplyUiState(
            items = persistentListOf(ReplyItem(rpid = 2)),
            page = 1, error = "offline", grpcNextOffset = null,
        )
        assertEquals(SubReplyPageRequest(1, null), resolveSubReplyNextRequest(state))
    }

    @Test
    fun `failed refresh retries first page rather than continuing old list`() {
        val state = SubReplyUiState(
            page = 4, grpcNextOffset = "old-next", error = "offline",
            retryRequest = SubReplyPageRequest(1, null),
        )
        assertEquals(SubReplyPageRequest(1, null), resolveSubReplyNextRequest(state))
    }

    @Test
    fun `failed later request retains its exact cursor and successful page advances normally`() {
        val state = SubReplyUiState(
            page = 4, grpcNextOffset = "cursor-four", error = "offline",
            retryRequest = SubReplyPageRequest(5, "cursor-four"),
        )
        assertEquals(SubReplyPageRequest(5, "cursor-four"), resolveSubReplyNextRequest(state))
        assertEquals(SubReplyPageRequest(5, "cursor-four"), resolveSubReplyNextRequest(state.copy(error = null)))
        assertEquals(null, state.resetForSort(SubReplySortMode.HOT).retryRequest)
    }
}
