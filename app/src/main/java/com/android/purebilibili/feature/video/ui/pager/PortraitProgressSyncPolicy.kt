package com.android.purebilibili.feature.video.ui.pager

import kotlin.math.abs

private const val PORTRAIT_PENDING_SEEK_TOLERANCE_MS = 500L

internal fun shouldApplyPortraitProgressSync(
    snapshotBvid: String?,
    snapshotCid: Long,
    currentBvid: String?,
    currentCid: Long
): Boolean {
    if (snapshotBvid.isNullOrBlank()) return false
    if (currentBvid.isNullOrBlank()) return false
    if (snapshotBvid != currentBvid) return false
    if (snapshotCid <= 0L || currentCid <= 0L) return true
    return snapshotCid == currentCid
}

internal fun shouldHoldPortraitSeekUiPosition(
    playerPositionMs: Long,
    pendingSeekPositionMs: Long?,
    toleranceMs: Long = PORTRAIT_PENDING_SEEK_TOLERANCE_MS
): Boolean {
    val pendingPosition = pendingSeekPositionMs ?: return false
    return abs(playerPositionMs - pendingPosition) > toleranceMs
}

internal fun resolvePortraitDisplayedProgressPosition(
    playerPositionMs: Long,
    localSeekPositionMs: Long,
    pendingSeekPositionMs: Long?
): Long {
    return if (shouldHoldPortraitSeekUiPosition(playerPositionMs, pendingSeekPositionMs)) {
        localSeekPositionMs
    } else {
        playerPositionMs
    }
}
