package com.android.purebilibili.navigation3

/** 方向查询和离线初始化只能完成仍属于当前来源页的最后一次视频跳转。 */
internal class VideoNavigationRequestTracker {
    private var sequence = 0L

    fun begin(
        backStack: List<BiliPaiNavKey>,
        sourceGeneration: Long,
    ): VideoNavigationRequest = VideoNavigationRequest(++sequence, backStack.toList(), sourceGeneration)

    fun isCurrent(
        request: VideoNavigationRequest,
        backStack: List<BiliPaiNavKey>,
        sourceGeneration: Long,
    ): Boolean = request.sequence == sequence &&
        request.backStack == backStack && request.sourceGeneration == sourceGeneration
}

internal data class VideoNavigationRequest(
    val sequence: Long,
    val backStack: List<BiliPaiNavKey>,
    val sourceGeneration: Long,
)
