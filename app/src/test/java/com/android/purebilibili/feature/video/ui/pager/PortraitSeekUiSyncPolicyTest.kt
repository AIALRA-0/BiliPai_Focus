package com.android.purebilibili.feature.video.ui.pager

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PortraitSeekUiSyncPolicyTest {

    @Test
    fun localSeekPosition_winsWhileSeekCommitIsStillAwaitingPlayerCatchUp() {
        assertEquals(
            25_000L,
            resolvePortraitDisplayedProgressPosition(
                playerPositionMs = 1_000L,
                localSeekPositionMs = 25_000L,
                pendingSeekPositionMs = 25_000L
            )
        )
    }

    @Test
    fun playerPosition_drivesUiWhenThereIsNoPendingSeek() {
        assertEquals(
            3_000L,
            resolvePortraitDisplayedProgressPosition(
                playerPositionMs = 3_000L,
                localSeekPositionMs = 25_000L,
                pendingSeekPositionMs = null
            )
        )
    }

    @Test
    fun pendingSeek_clearsWhenPlayerHasCaughtUpToCommittedTarget() {
        assertFalse(
            shouldHoldPortraitSeekUiPosition(
                playerPositionMs = 24_800L,
                pendingSeekPositionMs = 25_000L
            )
        )
    }

    @Test
    fun pendingSeek_staysActiveWhilePlayerStillReportsStaleOldPosition() {
        assertTrue(
            shouldHoldPortraitSeekUiPosition(
                playerPositionMs = 1_200L,
                pendingSeekPositionMs = 25_000L
            )
        )
    }
}
