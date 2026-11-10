package com.android.purebilibili.feature.video.viewmodel

import com.android.purebilibili.data.model.response.CommentMainListContinuation
import com.android.purebilibili.data.model.response.ReplyData
import com.android.purebilibili.data.model.response.ReplyItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal data class CommentSearchLoadResult(
    val replies: List<ReplyItem>,
    val totalCount: Int,
    val isComplete: Boolean,
    val error: String? = null,
) {
    val loadedCount: Int get() = replies.sumOf { 1 + it.replies.orEmpty().size }
}

/** 有界、顺序拉取根评论及每楼回复；上限和异常都保留部分结果，不冒充完整结果。 */
internal suspend fun loadCommentSearchReplies(
    maxRequests: Int = 300,
    loadMain: suspend (Int, CommentMainListContinuation?) -> Result<ReplyData>,
    loadThread: suspend (Long, String?) -> Result<ReplyData>,
    onProgress: (CommentSearchLoadResult) -> Unit = {},
): CommentSearchLoadResult {
    val roots = linkedMapOf<Long, ReplyItem>()
    var totalCount = 0
    var requests = 0
    fun snapshot(complete: Boolean = false, error: String? = null) =
        CommentSearchLoadResult(roots.values.toList(), totalCount, complete, error)
    try {
        var page = 1
        var continuation: CommentMainListContinuation? = null
        while (true) {
            currentCoroutineContext().ensureActive()
            if (requests >= maxRequests) return snapshot()
            requests++
            val data = loadMain(page, continuation).getOrThrow()
            currentCoroutineContext().ensureActive()
            totalCount = maxOf(totalCount, data.getAllCount())
            val batch = if (page == 1) data.collectTopReplies() + data.hots.orEmpty() + data.replies.orEmpty()
                else data.replies.orEmpty()
            batch.forEach { roots[it.rpid] = it }
            onProgress(snapshot())
            if (data.cursor.isEnd) break
            val next = requireNotNull(data.mainListContinuation) { "主评论缺少后续游标" }
            require(next != continuation) { "主评论分页游标未前进" }
            continuation = next
            page++
        }
        for (root in roots.values.toList()) {
            if (maxOf(root.count, root.rcount, root.replies.orEmpty().size) <= 0) continue
            var offset: String? = null
            val children = root.replies.orEmpty().associateByTo(linkedMapOf()) { it.rpid }
            while (true) {
                currentCoroutineContext().ensureActive()
                if (requests >= maxRequests) return snapshot()
                requests++
                val data = loadThread(root.rpid, offset).getOrThrow()
                currentCoroutineContext().ensureActive()
                data.replies.orEmpty().forEach { children[it.rpid] = it }
                roots[root.rpid] = root.copy(replies = children.values.toList())
                onProgress(snapshot())
                if (data.cursor.isEnd) break
                val next = data.grpcNextOffset.takeIf { it.isNotBlank() }
                require(next != null && next != offset) { "楼中楼缺少有效的后续游标" }
                offset = next
            }
        }
        return snapshot(complete = true)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        return snapshot(error = e.message ?: "评论加载失败，请稍后重试")
    }
}
