package com.android.purebilibili.feature.comment

import androidx.paging.PagingSource
import com.android.purebilibili.core.paging.ForwardPage
import com.android.purebilibili.core.paging.ForwardPagingSource
import com.android.purebilibili.data.model.response.ReplyData
import com.android.purebilibili.feature.video.viewmodel.SubReplyPageRequest
import com.android.purebilibili.feature.video.viewmodel.isSortedSubReplyPageEnd
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal data class SubReplyPageChunk(val request: SubReplyPageRequest, val data: ReplyData)

internal fun subReplyPagingSource(
    initialRequest: SubReplyPageRequest = SubReplyPageRequest(1, null),
    loadPage: suspend (SubReplyPageRequest) -> ReplyData,
): PagingSource<SubReplyPageRequest, SubReplyPageChunk> {
    val offsets = mutableSetOf<String>()
    return ForwardPagingSource(initialRequest) { request ->
        val data = loadPage(request)
        currentCoroutineContext().ensureActive()
        request.offset?.let(offsets::add)
        val next = data.grpcNextOffset.takeIf {
            !isSortedSubReplyPageEnd(data.cursor.isEnd, it) && it !in offsets
        }
        ForwardPage(listOf(SubReplyPageChunk(request, data)), next?.let { SubReplyPageRequest(request.page + 1, it) })
    }
}
