package com.android.purebilibili.feature.video.state

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoPlayerStateReusePolicyTest {

    @Test
    fun `does not reuse mini player when route cid mismatches current cid`() {
        assertFalse(
            shouldReuseMiniPlayerAtEntry(
                isMiniPlayerActive = true,
                miniPlayerBvid = "BV1same",
                miniPlayerCid = 1001L,
                hasMiniPlayerInstance = true,
                requestBvid = "BV1same",
                requestCid = 2002L
            )
        )
    }

    @Test
    fun sessionInactive_suspendsLocalPlayback() {
        assertTrue(shouldSuspendLocalPlaybackWhenSessionInactive(playbackSessionActive = false))
        assertFalse(shouldSuspendLocalPlaybackWhenSessionInactive(playbackSessionActive = true))
        assertFalse(
            shouldSuspendLocalPlaybackWhenSessionInactive(
                playbackSessionActive = false,
                isOwnedByMiniPlayer = true,
            )
        )
        assertTrue(
            shouldTreatPlayerAsOwnedByMiniPlayer(
                isMiniPlayerActive = true,
                isPlayerManaged = true,
                isMiniMode = true,
            )
        )
        assertFalse(
            shouldTreatPlayerAsOwnedByMiniPlayer(
                isMiniPlayerActive = true,
                isPlayerManaged = true,
                isMiniMode = false,
            )
        )
    }

    @Test
    fun foreignPlayback_haltsOnlyWhenIncomingDiffersAndActive() {
        assertTrue(
            shouldHaltForeignPlaybackOnVideoEntry(
                incomingBvid = "BV_new",
                activeBvid = "BV_old",
                isPlaybackLikelyActive = true
            )
        )
        assertFalse(
            shouldHaltForeignPlaybackOnVideoEntry(
                incomingBvid = "BV_same",
                activeBvid = "BV_same",
                isPlaybackLikelyActive = true
            )
        )
        assertFalse(
            shouldHaltForeignPlaybackOnVideoEntry(
                incomingBvid = "BV_new",
                activeBvid = "BV_old",
                isPlaybackLikelyActive = false
            )
        )
        assertFalse(
            shouldHaltForeignPlaybackOnVideoEntry(
                incomingBvid = "BV_new",
                activeBvid = null,
                isPlaybackLikelyActive = true
            )
        )
    }

    @Test
    fun `reuses mini player when route cid is missing but bvid matches active player`() {
        assertTrue(
            shouldReuseMiniPlayerAtEntry(
                isMiniPlayerActive = true,
                miniPlayerBvid = "BV1same",
                miniPlayerCid = 1001L,
                hasMiniPlayerInstance = true,
                requestBvid = "BV1same",
                requestCid = 0L
            )
        )
    }

    @Test
    fun `does not restore cached state when requested cid differs`() {
        assertFalse(
            shouldRestoreCachedUiState(
                cachedBvid = "BV1same",
                cachedCid = 1001L,
                requestBvid = "BV1same",
                requestCid = 2002L
            )
        )
    }

    @Test
    fun `restores cached state when request cid is unknown but bvid matches`() {
        assertTrue(
            shouldRestoreCachedUiState(
                cachedBvid = "BV1same",
                cachedCid = 1001L,
                requestBvid = "BV1same",
                requestCid = 0L
            )
        )
    }
}
