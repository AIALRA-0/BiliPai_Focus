package com.android.purebilibili.feature.video.ui.overlay

internal data class MiniPlayerOverlayChrome(
    val showHeaderChrome: Boolean,
    val showCenterControls: Boolean,
    val showSeekHint: Boolean,
    val showDragHint: Boolean,
    val showResizeHandle: Boolean,
    val showProgressBar: Boolean,
    val progressBarAlpha: Float
)

internal fun resolveMiniPlayerOverlayChrome(
    showControls: Boolean,
    isDraggingProgress: Boolean,
    isDraggingPosition: Boolean,
    isResizing: Boolean
): MiniPlayerOverlayChrome {
    val interactionChromeVisible = showControls || isDraggingProgress || isResizing
    val headerChromeVisible = showControls || isDraggingPosition || isResizing
    return MiniPlayerOverlayChrome(
        showHeaderChrome = headerChromeVisible,
        showCenterControls = interactionChromeVisible,
        showSeekHint = isDraggingProgress,
        showDragHint = showControls && !isDraggingProgress && !isDraggingPosition,
        showResizeHandle = showControls || isResizing,
        showProgressBar = true,
        progressBarAlpha = if (interactionChromeVisible) 1f else 0.46f
    )
}
