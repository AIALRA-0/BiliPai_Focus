package com.android.purebilibili.feature.video.viewmodel

import kotlinx.collections.immutable.persistentListOf

enum class SubReplySortMode(val apiMode: Int, val label: String) {
    TIME(2, "按时间"),
    HOT(3, "按热度");

    fun toggled(): SubReplySortMode = if (this == TIME) HOT else TIME
}

internal fun SubReplyUiState.resetForSort(mode: SubReplySortMode): SubReplyUiState = copy(
    sortMode = mode,
    items = persistentListOf(),
    baseItems = persistentListOf(),
    page = 1,
    basePage = 1,
    isEnd = false,
    baseIsEnd = false,
    grpcNextOffset = null,
    baseGrpcNextOffset = null,
    conversationAnchor = null,
    targetReplyId = 0L,
    isLoading = true,
    error = null,
)

internal fun isSortedSubReplyPageEnd(cursorIsEnd: Boolean, nextOffset: String?): Boolean =
    cursorIsEnd || nextOffset.isNullOrBlank()
