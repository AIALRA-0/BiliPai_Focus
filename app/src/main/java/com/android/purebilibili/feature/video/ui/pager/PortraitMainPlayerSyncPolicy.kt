package com.android.purebilibili.feature.video.ui.pager

import kotlin.math.abs

internal data class PortraitExitRestoreTarget(
    val bvid: String,
    val cid: Long
)

internal fun shouldReloadMainPlayerAfterPortraitExit(
    snapshotBvid: String?,
    snapshotCid: Long,
    currentBvid: String?,
    currentCid: Long
): Boolean {
    if (snapshotBvid.isNullOrBlank()) return false
    if (currentBvid.isNullOrBlank()) return true
    if (snapshotBvid != currentBvid) return true
    if (snapshotCid <= 0L || currentCid <= 0L) return false
    return snapshotCid != currentCid
}

internal fun shouldPauseMainPlayerOnPortraitEnter(useSharedPlayer: Boolean): Boolean {
    return !useSharedPlayer
}

internal fun resolvePortraitInitialPlayingBvid(
    useSharedPlayer: Boolean,
    initialBvid: String
): String? {
    if (!useSharedPlayer) return null
    return initialBvid
}

internal fun shouldMirrorPortraitProgressToMainPlayer(useSharedPlayer: Boolean): Boolean {
    return !useSharedPlayer
}

/**
 * Portrait progress polls every few hundred ms. Committing each tick into the giant
 * VideoDetailScreenStateHolder invalidates composition continuously and can ANR the
 * main thread while the standalone pager owns playback.
 *
 * Identity changes always commit. Position commits only after a meaningful delta so
 * exit/back still receive a fresh enough snapshot without thrashing composition.
 */
internal fun shouldCommitPortraitProgressToDetailState(
    previousBvid: String?,
    previousCid: Long,
    previousPositionMs: Long,
    nextBvid: String,
    nextCid: Long,
    nextPositionMs: Long,
    positionCommitThresholdMs: Long = 1_000L,
): Boolean {
    if (!nextBvid.isNullOrBlank() && nextBvid != previousBvid) return true
    if (nextCid > 0L && nextCid != previousCid) return true
    if (previousPositionMs < 0L) return true
    return abs(nextPositionMs - previousPositionMs) >= positionCommitThresholdMs
}

internal fun shouldExitPortraitForExternalNavigation(isPortraitFullscreen: Boolean): Boolean {
    return isPortraitFullscreen
}

internal fun shouldExitPortraitForUserSpaceNavigation(isPortraitFullscreen: Boolean): Boolean {
    return isPortraitFullscreen
}

internal fun shouldDeferPortraitRestoreUntilForegroundResume(
    isPortraitFullscreen: Boolean,
    isExternalNavigation: Boolean
): Boolean {
    return isPortraitFullscreen && isExternalNavigation
}

internal fun shouldApplyDeferredPortraitRestoreOnResume(
    hasDeferredRestore: Boolean,
    isPortraitFullscreen: Boolean
): Boolean {
    return hasDeferredRestore && !isPortraitFullscreen
}

internal fun shouldReplaceVideoDetailRouteAfterPortraitExit(
    routeBvid: String,
    portraitBvid: String,
): Boolean {
    return routeBvid.isNotBlank() &&
        portraitBvid.isNotBlank() &&
        routeBvid != portraitBvid
}

internal fun resolvePortraitExitRestoreTarget(
    pendingMainReloadBvidAfterPortrait: String?,
    portraitPendingSelectionBvid: String?,
    portraitSyncSnapshotBvid: String?,
    portraitSyncSnapshotCid: Long,
): PortraitExitRestoreTarget? {
    val targetBvid = pendingMainReloadBvidAfterPortrait
        ?: portraitPendingSelectionBvid
        ?: portraitSyncSnapshotBvid
        ?: return null
    val targetCid = if (targetBvid == portraitSyncSnapshotBvid) {
        portraitSyncSnapshotCid
    } else {
        // currentBvidCid still belongs to the inline detail subject until the portrait
        // selection is committed. Reusing it for a new bvid can load metadata for one
        // video while the shared player is already showing another.
        0L
    }
    return PortraitExitRestoreTarget(
        bvid = targetBvid,
        cid = targetCid
    )
}

internal fun shouldResyncPortraitPagerOnUserSpaceReturn(
    pendingUserSpaceNavigation: Boolean,
    expectedBvid: String,
    currentPlayingBvid: String?,
    currentPlayerMediaId: String?
): Boolean {
    if (!pendingUserSpaceNavigation) return false
    if (expectedBvid.isBlank()) return false
    if (currentPlayingBvid != expectedBvid) return true
    val normalizedMediaId = currentPlayerMediaId?.trim().orEmpty()
    if (normalizedMediaId.isBlank()) return true
    return normalizedMediaId != expectedBvid
}
