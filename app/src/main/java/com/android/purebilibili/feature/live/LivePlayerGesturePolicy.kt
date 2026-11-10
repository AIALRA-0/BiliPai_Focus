package com.android.purebilibili.feature.live

data class LivePlayerGesturePolicy(
    val doubleTapPlayback: Boolean,
    val centerDragFullscreen: Boolean,
) {
    companion object {
        /** 横竖屏采用同一套播放与全屏手势。 */
        val Shared = LivePlayerGesturePolicy(
            doubleTapPlayback = true,
            centerDragFullscreen = true,
        )
    }
}

internal fun shouldToggleLiveFullscreenForDrag(
    dragY: Float,
    threshold: Float,
    isFullscreen: Boolean,
    reverse: Boolean,
): Boolean {
    if (kotlin.math.abs(dragY) <= threshold) return false
    val entering = (dragY < 0f) != reverse
    return entering != isFullscreen
}
