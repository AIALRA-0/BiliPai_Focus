package com.android.purebilibili.feature.video.danmaku

/** Frozen item and player viewport in window pixels; independent of menu measurement. */
data class DanmakuTapAnchor(
    val left: Float, val top: Float, val right: Float, val bottom: Float,
    val tapX: Float,
    val likeCount: Int = 0,
    val owner: Any? = null,
    val viewportLeft: Float, val viewportTop: Float,
    val viewportRight: Float, val viewportBottom: Float,
)
