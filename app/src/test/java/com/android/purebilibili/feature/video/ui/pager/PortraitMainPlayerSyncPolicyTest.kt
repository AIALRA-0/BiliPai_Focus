package com.android.purebilibili.feature.video.ui.pager

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PortraitMainPlayerSyncPolicyTest {

    @Test
    fun noReloadWhenSnapshotBvidBlank() {
        assertFalse(
            shouldReloadMainPlayerAfterPortraitExit(
                snapshotBvid = " ",
                snapshotCid = 0L,
                currentBvid = "BV1xx411c7mD",
                currentCid = 123L
            )
        )
    }

    @Test
    fun reloadWhenCurrentBvidMissingButSnapshotExists() {
        assertTrue(
            shouldReloadMainPlayerAfterPortraitExit(
                snapshotBvid = "BV1xx411c7mD",
                snapshotCid = 123L,
                currentBvid = null,
                currentCid = 0L
            )
        )
    }

    @Test
    fun noReloadWhenSnapshotMatchesCurrent_bvidAndCid() {
        assertFalse(
            shouldReloadMainPlayerAfterPortraitExit(
                snapshotBvid = "BV1xx411c7mD",
                snapshotCid = 100L,
                currentBvid = "BV1xx411c7mD",
                currentCid = 100L
            )
        )
    }

    @Test
    fun reloadWhenSnapshotDiffersFromCurrent() {
        assertTrue(
            shouldReloadMainPlayerAfterPortraitExit(
                snapshotBvid = "BV17x411w7KC",
                snapshotCid = 200L,
                currentBvid = "BV1xx411c7mD",
                currentCid = 100L
            )
        )
    }

    @Test
    fun reloadWhenBvidSameButCidDiffers() {
        assertTrue(
            shouldReloadMainPlayerAfterPortraitExit(
                snapshotBvid = "BV1xx411c7mD",
                snapshotCid = 222L,
                currentBvid = "BV1xx411c7mD",
                currentCid = 111L
            )
        )
    }

    @Test
    fun sharedPlayerMode_shouldNotPauseMainPlayerOnPortraitEnter() {
        assertFalse(
            shouldPauseMainPlayerOnPortraitEnter(useSharedPlayer = true)
        )
        assertTrue(
            shouldPauseMainPlayerOnPortraitEnter(useSharedPlayer = false)
        )
    }

    @Test
    fun resolvePortraitInitialPlayingBvid_usesInitialBvidOnlyWhenShared() {
        assertEquals(
            "BV1xx411c7mD",
            resolvePortraitInitialPlayingBvid(
                useSharedPlayer = true,
                initialBvid = "BV1xx411c7mD"
            )
        )
        assertEquals(
            null,
            resolvePortraitInitialPlayingBvid(
                useSharedPlayer = false,
                initialBvid = "BV1xx411c7mD"
            )
        )
    }

    @Test
    fun sharedPlayerMode_shouldNotMirrorPortraitProgressBackToMainPlayer() {
        assertFalse(shouldMirrorPortraitProgressToMainPlayer(useSharedPlayer = true))
        assertTrue(shouldMirrorPortraitProgressToMainPlayer(useSharedPlayer = false))
    }

    @Test
    fun externalNavigation_shouldExitPortraitBeforeJumping() {
        assertTrue(shouldExitPortraitForExternalNavigation(isPortraitFullscreen = true))
        assertFalse(shouldExitPortraitForExternalNavigation(isPortraitFullscreen = false))
    }

    @Test
    fun userSpaceNavigation_shouldExitPortraitBeforeJumping() {
        assertTrue(shouldExitPortraitForUserSpaceNavigation(isPortraitFullscreen = true))
        assertFalse(shouldExitPortraitForUserSpaceNavigation(isPortraitFullscreen = false))
    }

    @Test
    fun externalNavigationFromPortrait_shouldDeferInlineRestoreUntilResume() {
        assertTrue(
            shouldDeferPortraitRestoreUntilForegroundResume(
                isPortraitFullscreen = true,
                isExternalNavigation = true
            )
        )
        assertFalse(
            shouldDeferPortraitRestoreUntilForegroundResume(
                isPortraitFullscreen = false,
                isExternalNavigation = true
            )
        )
        assertFalse(
            shouldDeferPortraitRestoreUntilForegroundResume(
                isPortraitFullscreen = true,
                isExternalNavigation = false
            )
        )
    }

    @Test
    fun deferredPortraitRestore_shouldApplyOnlyAfterReturningToInlineHost() {
        assertTrue(
            shouldApplyDeferredPortraitRestoreOnResume(
                hasDeferredRestore = true,
                isPortraitFullscreen = false
            )
        )
        assertFalse(
            shouldApplyDeferredPortraitRestoreOnResume(
                hasDeferredRestore = true,
                isPortraitFullscreen = true
            )
        )
        assertFalse(
            shouldApplyDeferredPortraitRestoreOnResume(
                hasDeferredRestore = false,
                isPortraitFullscreen = false
            )
        )
    }

    @Test
    fun portraitExit_shouldReplaceDetailRoute_whenPagerMovedToAnotherVideo() {
        assertTrue(
            shouldReplaceVideoDetailRouteAfterPortraitExit(
                routeBvid = "BV_FIRST",
                portraitBvid = "BV_SECOND",
            )
        )
        assertFalse(
            shouldReplaceVideoDetailRouteAfterPortraitExit(
                routeBvid = "BV_SECOND",
                portraitBvid = "BV_SECOND",
            )
        )
    }

    @Test
    fun portraitExitRestoreTarget_prefersPendingReloadAndKeepsSnapshotCidForSameVideo() {
        assertEquals(
            PortraitExitRestoreTarget(
                bvid = "BV_TARGET",
                cid = 202L
            ),
            resolvePortraitExitRestoreTarget(
                pendingMainReloadBvidAfterPortrait = "BV_TARGET",
                portraitPendingSelectionBvid = "BV_OTHER",
                portraitSyncSnapshotBvid = "BV_TARGET",
                portraitSyncSnapshotCid = 202L,
            )
        )
    }

    @Test
    fun portraitExitRestoreTarget_systemBackKeepsSettledPortraitVideoIdentity() {
        assertEquals(
            PortraitExitRestoreTarget(
                bvid = "BV_SECOND",
                cid = 302L
            ),
            resolvePortraitExitRestoreTarget(
                pendingMainReloadBvidAfterPortrait = null,
                portraitPendingSelectionBvid = "BV_SECOND",
                portraitSyncSnapshotBvid = "BV_SECOND",
                portraitSyncSnapshotCid = 302L,
            )
        )
    }

    @Test
    fun portraitExitRestoreTarget_fallsBackToSelectionWithoutReusingOldDetailCid() {
        assertEquals(
            PortraitExitRestoreTarget(
                bvid = "BV_OTHER",
                cid = 0L
            ),
            resolvePortraitExitRestoreTarget(
                pendingMainReloadBvidAfterPortrait = null,
                portraitPendingSelectionBvid = "BV_OTHER",
                portraitSyncSnapshotBvid = "BV_TARGET",
                portraitSyncSnapshotCid = 202L,
            )
        )
    }

    @Test
    fun portraitExitRestoreTarget_returnsNullWhenNoPortraitSnapshotExists() {
        assertEquals(
            null,
            resolvePortraitExitRestoreTarget(
                pendingMainReloadBvidAfterPortrait = null,
                portraitPendingSelectionBvid = null,
                portraitSyncSnapshotBvid = null,
                portraitSyncSnapshotCid = 202L,
            )
        )
    }

    @Test
    fun userSpaceReturn_shouldResync_whenCurrentPlayingBvidDiffers() {
        assertTrue(
            shouldResyncPortraitPagerOnUserSpaceReturn(
                pendingUserSpaceNavigation = true,
                expectedBvid = "BV17x411w7KC",
                currentPlayingBvid = "BV1xx411c7mD",
                currentPlayerMediaId = "BV1xx411c7mD"
            )
        )
    }

    @Test
    fun userSpaceReturn_shouldResync_whenPlayerMediaIdMissing() {
        assertTrue(
            shouldResyncPortraitPagerOnUserSpaceReturn(
                pendingUserSpaceNavigation = true,
                expectedBvid = "BV17x411w7KC",
                currentPlayingBvid = "BV17x411w7KC",
                currentPlayerMediaId = " "
            )
        )
    }

    @Test
    fun userSpaceReturn_shouldNotResync_whenAlreadyAligned() {
        assertFalse(
            shouldResyncPortraitPagerOnUserSpaceReturn(
                pendingUserSpaceNavigation = true,
                expectedBvid = "BV17x411w7KC",
                currentPlayingBvid = "BV17x411w7KC",
                currentPlayerMediaId = "BV17x411w7KC"
            )
        )
    }

    @Test
    fun userSpaceReturn_shouldNotResync_whenNoPendingNavigation() {
        assertFalse(
            shouldResyncPortraitPagerOnUserSpaceReturn(
                pendingUserSpaceNavigation = false,
                expectedBvid = "BV17x411w7KC",
                currentPlayingBvid = "BV17x411w7KC",
                currentPlayerMediaId = ""
            )
        )
    }

    @Test
    fun portraitProgressCommit_commitsOnIdentityChange() {
        assertTrue(
            shouldCommitPortraitProgressToDetailState(
                previousBvid = "BV1",
                previousCid = 1L,
                previousPositionMs = 1_000L,
                nextBvid = "BV2",
                nextCid = 1L,
                nextPositionMs = 1_000L,
            )
        )
        assertTrue(
            shouldCommitPortraitProgressToDetailState(
                previousBvid = "BV1",
                previousCid = 1L,
                previousPositionMs = 1_000L,
                nextBvid = "BV1",
                nextCid = 2L,
                nextPositionMs = 1_000L,
            )
        )
    }

    @Test
    fun portraitProgressCommit_throttlesSameIdentitySmallPositionDeltas() {
        assertFalse(
            shouldCommitPortraitProgressToDetailState(
                previousBvid = "BV1",
                previousCid = 11L,
                previousPositionMs = 10_000L,
                nextBvid = "BV1",
                nextCid = 11L,
                nextPositionMs = 10_400L,
            )
        )
        assertTrue(
            shouldCommitPortraitProgressToDetailState(
                previousBvid = "BV1",
                previousCid = 11L,
                previousPositionMs = 10_000L,
                nextBvid = "BV1",
                nextCid = 11L,
                nextPositionMs = 11_200L,
            )
        )
    }

    @Test
    fun portraitProgressCommit_alwaysCommitsFirstObservation() {
        assertTrue(
            shouldCommitPortraitProgressToDetailState(
                previousBvid = null,
                previousCid = 0L,
                previousPositionMs = -1L,
                nextBvid = "BV1",
                nextCid = 11L,
                nextPositionMs = 0L,
            )
        )
    }
}
