package com.android.purebilibili.feature.video.ui.pager

/** Match the fullscreen player's four-second inactivity window. */
internal const val PORTRAIT_CONTROLS_AUTO_HIDE_DELAY_MS = 4_000L

internal fun shouldAutoHidePortraitControls(
    isCurrentPage: Boolean,
    playbackActive: Boolean,
    controlsVisible: Boolean,
    userInteracting: Boolean,
    overlayOpen: Boolean,
): Boolean = isCurrentPage && playbackActive && controlsVisible && !userInteracting && !overlayOpen
