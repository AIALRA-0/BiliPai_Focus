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
    val candidateIndex: Int
)

internal data class DynamicDetailInteractionModel(
    val primaryAction: DynamicCardPrimaryAction,
    val commentTargets: List<DynamicCommentTarget>
)

internal fun resolveDynamicCommentPayload(
    data: ReplyData,
    fallbackCount: Int
): DynamicCommentPayload {
    val replies = buildList {
        addAll(data.collectTopReplies())
        addAll(data.hots.orEmpty())
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
    isEnd: Boolean
): SubReplyUiState {
    val mergedItems = if (page == 1) {
        newItems
    } else {
        (currentState.items + newItems).distinctBy { it.rpid }
    }
    return currentState.copy(
        items = mergedItems.toImmutableList(),
        isLoading = false,
        page = page,
        isEnd = isEnd,
        error = null
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
