package com.android.purebilibili.feature.dynamic

import com.android.purebilibili.data.model.response.DynamicItem
import com.android.purebilibili.data.model.response.ReplyData
import com.android.purebilibili.data.model.response.ReplyItem
import com.android.purebilibili.feature.dynamic.components.DynamicCardPrimaryAction
import com.android.purebilibili.feature.dynamic.components.resolveDynamicCardPrimaryAction
import com.android.purebilibili.feature.video.viewmodel.SubReplyUiState
import kotlinx.collections.immutable.toImmutableList

internal data class DynamicCommentPayload(
    val replies: List<ReplyItem>,
    val totalCount: Int
)

internal data class DynamicCommentLoadAttempt(
    val target: DynamicCommentTarget,
    val replies: List<ReplyItem>,
    val totalCount: Int,
    val candidateIndex: Int,
    val nextPage: Int = 2,
    val isEnd: Boolean = true,
    val grpcNextOffset: String? = null
)

internal data class DynamicDetailInteractionModel(
    val primaryAction: DynamicCardPrimaryAction,
    val commentTargets: List<DynamicCommentTarget>
)

internal fun resolveDynamicCommentPayload(
    data: ReplyData,
    fallbackCount: Int,
    includeHotReplies: Boolean = true
): DynamicCommentPayload {
    val replies = buildList {
        addAll(data.collectTopReplies())
        if (includeHotReplies) {
            addAll(data.hots.orEmpty())
        }
        addAll(data.replies.orEmpty())
    }.distinctBy { it.rpid }

    return DynamicCommentPayload(
        replies = replies,
        totalCount = maxOf(
            data.getAllCount(),
            fallbackCount,
            replies.size
        )
    )
}

internal fun selectPreferredDynamicCommentAttempt(
    attempts: List<DynamicCommentLoadAttempt>,
    expectedCount: Int = 0
): DynamicCommentLoadAttempt? {
    return attempts.minWithOrNull(
        compareBy<DynamicCommentLoadAttempt> {
            when {
                it.replies.isNotEmpty() -> 0
                it.totalCount > 0 -> 1
                else -> 2
            }
        }.thenBy {
            if (expectedCount > 0 && it.totalCount > 0) {
                kotlin.math.abs(it.totalCount - expectedCount)
            } else {
                0
            }
        }.thenBy { it.candidateIndex }
            .thenByDescending { it.totalCount }
            .thenByDescending { it.replies.size }
    )
}

internal fun resolveDynamicMainCommentPageEnd(
    cursorIsEnd: Boolean,
    fetchedReplyCount: Int,
    loadedReplyCount: Int,
    totalCount: Int
): Boolean {
    if (fetchedReplyCount <= 0) return true
    if (totalCount > loadedReplyCount.coerceAtLeast(0)) {
        return false
    }
    return cursorIsEnd
}

internal fun shouldLoadMoreDynamicDetailComments(
    lastVisibleIndex: Int,
    itemCount: Int,
    loadedCount: Int,
    totalCount: Int,
    isLoading: Boolean,
    isLoadingMore: Boolean,
): Boolean {
    if (isLoading || isLoadingMore) return false
    if (itemCount <= 0 || lastVisibleIndex < 0) return false
    if (loadedCount >= totalCount) return false
    return lastVisibleIndex >= itemCount - 4
}

internal fun resolveDynamicDetailInteractionModel(
    item: DynamicItem
): DynamicDetailInteractionModel {
    return DynamicDetailInteractionModel(
        primaryAction = resolveDynamicCardPrimaryAction(item),
        commentTargets = resolveDynamicCommentTargets(item)
    )
}

internal fun resolveDynamicSubReplyStateAfterSuccess(
    currentState: SubReplyUiState,
    newItems: List<ReplyItem>,
    page: Int,
    isEnd: Boolean,
    totalCount: Int = currentState.totalCount,
    grpcNextOffset: String? = currentState.grpcNextOffset
): SubReplyUiState {
    val mergedItems = if (page == 1) {
        newItems
    } else {
        (currentState.items + newItems).distinctBy { it.rpid }
    }
    return currentState.copy(
        items = mergedItems.toImmutableList(),
        totalCount = totalCount,
        isLoading = false,
        page = page,
        isEnd = isEnd,
        error = null,
        baseItems = mergedItems.toImmutableList(),
        basePage = page,
        baseIsEnd = isEnd,
        grpcNextOffset = grpcNextOffset,
        baseGrpcNextOffset = grpcNextOffset
    )
}

internal fun resolveDynamicSubReplyStateAfterFailure(
    currentState: SubReplyUiState,
    errorMessage: String?
): SubReplyUiState {
    return currentState.copy(
        isLoading = false,
        error = errorMessage
    )
}
