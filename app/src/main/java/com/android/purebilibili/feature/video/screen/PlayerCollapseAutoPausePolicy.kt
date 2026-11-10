package com.android.purebilibili.feature.video.screen

/** Automatic collapse pauses must keep the geometry and gesture rules of playback. */
internal fun resolveIsPlaybackPausedForInlineLayout(
    isPlaybackPaused: Boolean,
    wasAutoPausedByCollapse: Boolean,
): Boolean = isPlaybackPaused && !wasAutoPausedByCollapse

/**
 * Comment layout compaction survives pause-driven gesture changes. Gesture consumption is
 * not a layout state: pausing enables the hide gesture without expanding the player.
 */
internal fun resolvePlayerCollapsedForAutoPause(
    swipeHidePlayerEnabled: Boolean,
    playerHeightOffsetPx: Float,
    videoHeightPx: Float,
    compactForCommentTab: Boolean,
    restoreRequested: Boolean = false,
): Boolean {
    if (restoreRequested) return false
    return compactForCommentTab || resolveIsPlayerCollapsed(
        swipeHidePlayerEnabled = swipeHidePlayerEnabled,
        playerHeightOffsetPx = playerHeightOffsetPx,
        videoHeightPx = videoHeightPx,
    )
}

/**
 * When the detail player is swipe-collapsed (e.g. browsing related), optionally auto-pause
 * so audio does not keep playing under the list.
 */
internal fun shouldAutoPauseOnPlayerCollapse(
    autoPauseEnabled: Boolean,
    isPlayerCollapsed: Boolean,
    isPlaying: Boolean,
    isPortraitFullscreen: Boolean = false,
): Boolean = autoPauseEnabled && isPlayerCollapsed && isPlaying && !isPortraitFullscreen

/**
 * Resume only if this feature previously auto-paused — never override a manual pause.
 */
internal fun shouldAutoResumeOnPlayerExpand(
    autoPauseEnabled: Boolean,
    isPlayerCollapsed: Boolean,
    wasAutoPausedByCollapse: Boolean,
    isPortraitFullscreen: Boolean = false,
): Boolean = autoPauseEnabled &&
    (!isPlayerCollapsed || isPortraitFullscreen) &&
    wasAutoPausedByCollapse
