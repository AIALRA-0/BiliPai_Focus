package com.android.purebilibili.feature.live

internal data class LivePortraitPresentation(
    val usePortraitControls: Boolean,
    val clearScreen: Boolean,
    val showChrome: Boolean,
    val showChatPreview: Boolean,
    val showMediaOverlays: Boolean,
)

internal fun resolveLivePortraitPresentation(
    layoutMode: LiveRoomLayoutMode,
    clearScreen: Boolean,
    chatVisible: Boolean,
): LivePortraitPresentation {
    val portrait = layoutMode == LiveRoomLayoutMode.PortraitVerticalOverlay
    val cleared = portrait && clearScreen
    return LivePortraitPresentation(
        usePortraitControls = portrait,
        clearScreen = cleared,
        showChrome = portrait && !cleared,
        showChatPreview = portrait && !cleared && chatVisible,
        showMediaOverlays = !cleared,
    )
}

internal fun resolveLivePortraitChatPreviewCount(heightDp: Int, fontScale: Float): Int =
    if (heightDp < 720 || fontScale > 1.2f) 4 else 6

data class LivePlayerGesturePolicy(
    val doubleTapPlayback: Boolean,
    val centerDragFullscreen: Boolean,
)

fun resolveLivePlayerGesturePolicy(layoutMode: LiveRoomLayoutMode): LivePlayerGesturePolicy {
    val portrait = layoutMode == LiveRoomLayoutMode.PortraitVerticalOverlay
    return LivePlayerGesturePolicy(
        doubleTapPlayback = !portrait,
        centerDragFullscreen = !portrait,
    )
}
