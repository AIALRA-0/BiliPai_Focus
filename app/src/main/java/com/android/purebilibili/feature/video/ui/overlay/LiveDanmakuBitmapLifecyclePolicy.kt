package com.android.purebilibili.feature.video.ui.overlay

internal enum class LiveDanmakuBitmapOwnership {
    APP_QUEUE_ONLY,
    CONTROLLER_ATTACHED,
    TIMELINE_DISCARDED,
}

internal fun shouldManuallyRecycleLiveDanmakuBitmap(
    ownership: LiveDanmakuBitmapOwnership
): Boolean {
    return ownership == LiveDanmakuBitmapOwnership.APP_QUEUE_ONLY
}
