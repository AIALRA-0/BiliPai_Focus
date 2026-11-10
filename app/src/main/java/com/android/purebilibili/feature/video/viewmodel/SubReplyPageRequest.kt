package com.android.purebilibili.feature.video.viewmodel

/** 保存失败请求，而不是从当前可见回复数猜测重试哪一页。 */
data class SubReplyPageRequest(val page: Int, val offset: String?)

internal fun resolveSubReplyNextRequest(state: SubReplyUiState): SubReplyPageRequest {
    if (state.error != null) {
        state.retryRequest?.let { return it }
        // 首页可能已展示预览或本地对话，items 非空并不表示首页请求成功。
        if (state.grpcNextOffset.isNullOrBlank()) return SubReplyPageRequest(1, null)
    }
    return SubReplyPageRequest(state.page + 1, state.grpcNextOffset)
}
