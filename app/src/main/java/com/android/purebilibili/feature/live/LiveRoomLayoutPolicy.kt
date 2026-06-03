package com.android.purebilibili.feature.live

import kotlin.math.roundToInt

enum class LiveRoomLayoutMode {
    PortraitPanel,
    PortraitVerticalOverlay,
    LandscapeSplit,
    LandscapeOverlay
}

data class LivePortraitOverlayMetrics(
    val panelHeightFraction: Float,
    val minPanelHeightDp: Int,
    val minPlayerClearanceDp: Int,
    val playerControlsGapDp: Int,
    val topChromeReserveDp: Int,
    val playerControlsReserveDp: Int
)

data class LiveOverlayContentInsets(
    val topDp: Int,
    val bottomDp: Int
)

data class LiveLandscapeChatOverlayMetrics(
    val widthFraction: Float,
    val heightFraction: Float,
    val minWidthDp: Int,
    val maxWidthDp: Int,
    val minHeightDp: Int,
    val edgePaddingDp: Int,
    val topControlReserveDp: Int,
    val bottomControlReserveDp: Int
)

fun resolveLiveRoomLayoutMode(
    isLandscape: Boolean,
    isTablet: Boolean,
    isFullscreen: Boolean,
    isPortraitLive: Boolean
): LiveRoomLayoutMode {
    return if (isTablet) {
        if (isFullscreen) {
            LiveRoomLayoutMode.LandscapeOverlay
        } else if (isLandscape) {
            LiveRoomLayoutMode.LandscapeSplit
        } else if (isPortraitLive) {
            LiveRoomLayoutMode.PortraitVerticalOverlay
        } else {
            LiveRoomLayoutMode.PortraitPanel
        }
    } else {
        if (isLandscape || isFullscreen) {
            LiveRoomLayoutMode.LandscapeOverlay
        } else if (isPortraitLive) {
            LiveRoomLayoutMode.PortraitVerticalOverlay
        } else {
            LiveRoomLayoutMode.PortraitPanel
        }
    }
}

fun shouldShowLiveChatToggle(
    layoutMode: LiveRoomLayoutMode
): Boolean {
    return layoutMode == LiveRoomLayoutMode.PortraitVerticalOverlay ||
        layoutMode == LiveRoomLayoutMode.LandscapeSplit ||
        layoutMode == LiveRoomLayoutMode.LandscapeOverlay
}

fun defaultLiveInteractionPanelVisible(): Boolean = false

fun shouldShowLiveSplitChatPanel(
    layoutMode: LiveRoomLayoutMode,
    isInteractionPanelVisible: Boolean
): Boolean {
    return layoutMode == LiveRoomLayoutMode.LandscapeSplit && isInteractionPanelVisible
}

fun shouldShowLiveLandscapeChatOverlay(
    layoutMode: LiveRoomLayoutMode,
    isInteractionPanelVisible: Boolean
): Boolean {
    return layoutMode == LiveRoomLayoutMode.LandscapeOverlay && isInteractionPanelVisible
}

fun shouldReserveLivePortraitInteractionPanel(
    layoutMode: LiveRoomLayoutMode,
    isInteractionPanelVisible: Boolean
): Boolean {
    return layoutMode == LiveRoomLayoutMode.PortraitVerticalOverlay && isInteractionPanelVisible
}

fun shouldApplyLiveTopControlSystemInsets(
    layoutMode: LiveRoomLayoutMode,
    isFullscreen: Boolean
): Boolean {
    return isFullscreen ||
        layoutMode == LiveRoomLayoutMode.PortraitVerticalOverlay ||
        layoutMode == LiveRoomLayoutMode.LandscapeOverlay
}

fun shouldApplyLiveBottomControlSystemInsets(
    layoutMode: LiveRoomLayoutMode,
    isFullscreen: Boolean,
    hasReservedBottomOverlay: Boolean
): Boolean {
    if (hasReservedBottomOverlay) return false
    return isFullscreen ||
        layoutMode == LiveRoomLayoutMode.PortraitVerticalOverlay ||
        layoutMode == LiveRoomLayoutMode.LandscapeOverlay
}

fun shouldShowLivePlayerControlsTopBar(
    layoutMode: LiveRoomLayoutMode,
    isFullscreen: Boolean
): Boolean {
    if (isFullscreen) return true
    return layoutMode == LiveRoomLayoutMode.LandscapeOverlay
}

fun resolveLivePortraitOverlayMetrics(
    screenHeightDp: Int
): LivePortraitOverlayMetrics {
    val compactHeight = screenHeightDp < 720
    return LivePortraitOverlayMetrics(
        panelHeightFraction = if (compactHeight) 0.35f else 0.38f,
        minPanelHeightDp = if (compactHeight) 220 else 260,
        minPlayerClearanceDp = if (compactHeight) 380 else 460,
        playerControlsGapDp = 10,
        topChromeReserveDp = if (compactHeight) 86 else 96,
        playerControlsReserveDp = if (compactHeight) 68 else 74
    )
}

fun resolveLivePortraitOverlayPanelHeightDp(
    screenHeightDp: Int,
    metrics: LivePortraitOverlayMetrics
): Int {
    val maxPanelHeight = (screenHeightDp - metrics.minPlayerClearanceDp)
        .coerceAtLeast(0)
    val lowerBound = metrics.minPanelHeightDp.coerceAtMost(maxPanelHeight)
    val preferredHeight = (screenHeightDp * metrics.panelHeightFraction).roundToInt()
    return preferredHeight.coerceIn(lowerBound, maxPanelHeight)
}

fun resolveLiveOverlayContentInsets(
    layoutMode: LiveRoomLayoutMode,
    portraitPanelHeightDp: Int,
    portraitMetrics: LivePortraitOverlayMetrics,
    isInteractionPanelVisible: Boolean
): LiveOverlayContentInsets {
    if (layoutMode != LiveRoomLayoutMode.PortraitVerticalOverlay) {
        return LiveOverlayContentInsets(topDp = 0, bottomDp = 0)
    }
    val interactionPanelReserveDp = if (isInteractionPanelVisible) {
        portraitPanelHeightDp
    } else {
        0
    }
    return LiveOverlayContentInsets(
        topDp = portraitMetrics.topChromeReserveDp,
        bottomDp = interactionPanelReserveDp +
            portraitMetrics.playerControlsGapDp +
            portraitMetrics.playerControlsReserveDp
    )
}

fun resolveLiveLandscapeChatOverlayMetrics(
    screenWidthDp: Int,
    screenHeightDp: Int
): LiveLandscapeChatOverlayMetrics {
    val compactHeight = screenHeightDp < 420
    val compactWidth = screenWidthDp < 700
    return LiveLandscapeChatOverlayMetrics(
        widthFraction = if (compactWidth) 0.42f else 0.34f,
        heightFraction = if (compactHeight) 0.46f else 0.54f,
        minWidthDp = if (compactWidth) 220 else 260,
        maxWidthDp = 360,
        minHeightDp = if (compactHeight) 132 else 180,
        edgePaddingDp = 16,
        topControlReserveDp = if (compactHeight) 76 else 86,
        bottomControlReserveDp = if (compactHeight) 92 else 98
    )
}

fun resolveLiveLandscapeChatOverlayWidthDp(
    screenWidthDp: Int,
    metrics: LiveLandscapeChatOverlayMetrics
): Int {
    val maxAvailableWidth = (screenWidthDp - metrics.edgePaddingDp * 2)
        .coerceAtLeast(0)
    val upperBound = minOf(metrics.maxWidthDp, maxAvailableWidth)
    val lowerBound = metrics.minWidthDp.coerceAtMost(upperBound)
    val preferredWidth = (maxAvailableWidth * metrics.widthFraction).roundToInt()
    return preferredWidth.coerceIn(lowerBound, upperBound)
}

fun resolveLiveLandscapeChatOverlayHeightDp(
    screenHeightDp: Int,
    metrics: LiveLandscapeChatOverlayMetrics
): Int {
    val maxAvailableHeight = (
        screenHeightDp -
            metrics.topControlReserveDp -
            metrics.bottomControlReserveDp
        ).coerceAtLeast(0)
    val lowerBound = metrics.minHeightDp.coerceAtMost(maxAvailableHeight)
    val preferredHeight = (screenHeightDp * metrics.heightFraction).roundToInt()
    return preferredHeight.coerceIn(lowerBound, maxAvailableHeight)
}

fun formatLiveDuration(
    liveStartTimeSeconds: Long,
    nowMillis: Long = System.currentTimeMillis()
): String {
    if (liveStartTimeSeconds <= 0L || nowMillis <= liveStartTimeSeconds * 1000L) {
        return ""
    }
    val totalMinutes = ((nowMillis - liveStartTimeSeconds * 1000L) / 60_000L).coerceAtLeast(0L)
    if (totalMinutes <= 0L) return "刚刚开播"
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return buildString {
        append("开播")
        if (hours > 0L) append(hours).append("小时")
        if (minutes > 0L || hours == 0L) append(minutes).append("分钟")
    }
}

fun formatLiveViewerCount(count: Int): String {
    return when {
        count >= 100_000_000 -> "%.1f亿".format(count / 100_000_000f)
        count >= 10_000 -> "%.1f万".format(count / 10_000f)
        count > 0 -> count.toString()
        else -> "-"
    }
}
