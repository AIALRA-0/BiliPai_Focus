package com.android.purebilibili.feature.video.ui.feedback

data class TripleActionVisualState(
    val isLiked: Boolean,
    val coinCount: Int,
    val isFavorited: Boolean
)

internal fun shouldTreatTripleActionCoinFailureAsAlreadyCoined(
    coinFailureMessage: String?
): Boolean {
    // 覆盖原创「已投满2个硬币」与转载上限类提示（含服务端转发的「最多投」文案）
    return coinFailureMessage?.contains("已投满") == true ||
        coinFailureMessage?.contains("最多投") == true
}

/**
 * 三连后的本地视觉状态。attemptedCoinCount 为本次三连实际投币数
 * （原创 2、转载 1），不能用固定 2 乐观更新转载视频的投币数。
 */
fun resolveTripleActionVisualState(
    currentLiked: Boolean,
    currentCoinCount: Int,
    currentFavorited: Boolean,
    likeSuccess: Boolean,
    coinSuccess: Boolean,
    coinFailureMessage: String?,
    favoriteSuccess: Boolean,
    attemptedCoinCount: Int = 2
): TripleActionVisualState {
    return TripleActionVisualState(
        isLiked = currentLiked || likeSuccess,
        coinCount = when {
            coinSuccess -> maxOf(currentCoinCount, attemptedCoinCount)
            shouldTreatTripleActionCoinFailureAsAlreadyCoined(coinFailureMessage) -> maxOf(currentCoinCount, attemptedCoinCount)
            else -> currentCoinCount
        },
        isFavorited = currentFavorited || favoriteSuccess
    )
}
