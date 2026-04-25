package com.android.purebilibili.feature.live

enum class LiveRoomLayoutMode {
    PortraitPanel,
    PortraitVerticalOverlay,
    LandscapeSplit,
    LandscapeOverlay
}

fun resolveLiveRoomLayoutMode(
    isLandscape: Boolean,
    isTablet: Boolean,
    isFullscreen: Boolean,
    isPortraitLive: Boolean
): LiveRoomLayoutMode {
    if (isLandscape) {
        return if (isTablet && !isFullscreen) {
            LiveRoomLayoutMode.LandscapeSplit
        } else {
            LiveRoomLayoutMode.LandscapeOverlay
        }
    }

    return if (isPortraitLive && !isFullscreen) {
        LiveRoomLayoutMode.PortraitVerticalOverlay
    } else {
        LiveRoomLayoutMode.PortraitPanel
    }
}

fun shouldShowLiveChatToggle(
    layoutMode: LiveRoomLayoutMode
): Boolean {
    return layoutMode == LiveRoomLayoutMode.LandscapeSplit ||
        layoutMode == LiveRoomLayoutMode.LandscapeOverlay
}

fun shouldShowLiveSplitChatPanel(
    layoutMode: LiveRoomLayoutMode,
    isChatVisible: Boolean
): Boolean {
    return layoutMode == LiveRoomLayoutMode.LandscapeSplit && isChatVisible
}

fun shouldShowLiveLandscapeChatOverlay(
    layoutMode: LiveRoomLayoutMode,
    isChatVisible: Boolean
): Boolean {
    return layoutMode == LiveRoomLayoutMode.LandscapeOverlay && isChatVisible
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
