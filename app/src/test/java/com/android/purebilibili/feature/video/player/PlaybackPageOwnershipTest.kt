package com.android.purebilibili.feature.video.player

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackPageOwnershipTest {
    @Test
    fun returningFromChildKeepsParentPlayerRetained() {
        val ownership = PlaybackPageOwnership()
        val parent = Any()
        val child = Any()
        ownership.retain(parent)
        ownership.retain(child)

        ownership.release(child)

        assertTrue(ownership.isRetained(parent))
        assertFalse(ownership.isRetained(child))
        ownership.release(parent)
        assertFalse(ownership.isRetained(parent))
    }

    @Test
    fun reusedPlayerCannotBeReleasedUntilItsLastPageIsDisposed() {
        val ownership = PlaybackPageOwnership()
        val player = Any()
        ownership.retain(player)
        ownership.retain(player)

        ownership.release(player)
        assertTrue(ownership.isRetained(player))

        ownership.release(player)
        assertFalse(ownership.isRetained(player))
    }

    @Test
    fun separateInstancesWithEqualValuesHaveIndependentOwners() {
        data class PlayerIdentity(val bvid: String)
        val parent = PlayerIdentity("BV_same")
        val child = PlayerIdentity("BV_same")
        val ownership = PlaybackPageOwnership()
        ownership.retain(parent)
        ownership.retain(child)

        ownership.release(child)

        assertTrue(ownership.isRetained(parent))
        assertFalse(ownership.isRetained(child))
    }

    @Test
    fun returningParentReclaimsSessionFromRecommendedVideo() {
        assertTrue(
            shouldReclaimVideoPlaybackSession(
                isVisible = true,
                isSessionActive = true,
                currentPlayer = Any(),
                currentBvid = "BV_child",
                currentCid = 20L,
                pagePlayer = Any(),
                pageBvid = "BV_parent",
                pageCid = 10L,
            )
        )
    }

    @Test
    fun coveredParentCannotStealChildSession() {
        assertFalse(
            shouldReclaimVideoPlaybackSession(
                isVisible = false,
                isSessionActive = true,
                currentPlayer = Any(),
                currentBvid = "BV_child",
                currentCid = 20L,
                pagePlayer = Any(),
                pageBvid = "BV_parent",
                pageCid = 10L,
            )
        )
    }

    @Test
    fun sameVideoWithDifferentPlayerStillNeedsRebinding() {
        assertTrue(
            shouldReclaimVideoPlaybackSession(
                isVisible = true,
                isSessionActive = true,
                currentPlayer = Any(),
                currentBvid = "BV_parent",
                currentCid = 10L,
                pagePlayer = Any(),
                pageBvid = "BV_parent",
                pageCid = 10L,
            )
        )
    }

    @Test
    fun clearedSessionMustBeReclaimedByVisiblePage() {
        assertTrue(
            shouldReclaimVideoPlaybackSession(
                isVisible = true,
                isSessionActive = false,
                currentPlayer = null,
                currentBvid = null,
                currentCid = 0L,
                pagePlayer = Any(),
                pageBvid = "BV_parent",
                pageCid = 10L,
            )
        )
    }

    @Test
    fun anotherPageOfTheSameVideoMustRefreshSessionIdentity() {
        val player = Any()
        assertTrue(
            shouldReclaimVideoPlaybackSession(
                isVisible = true,
                isSessionActive = true,
                currentPlayer = player,
                currentBvid = "BV_parent",
                currentCid = 10L,
                pagePlayer = player,
                pageBvid = "BV_parent",
                pageCid = 11L,
            )
        )
    }

    @Test
    fun currentPageDoesNotRebindItsHealthySession() {
        val player = Any()
        assertFalse(
            shouldReclaimVideoPlaybackSession(
                isVisible = true,
                isSessionActive = true,
                currentPlayer = player,
                currentBvid = "BV_parent",
                currentCid = 10L,
                pagePlayer = player,
                pageBvid = "BV_parent",
                pageCid = 10L,
            )
        )
    }
    @Test
    fun returningFromBottomBarReclaimsTheSamePlayerAndClearsNavigationExit() {
        val player = Any()
        for (visible in listOf(false, true)) {
            kotlin.test.assertEquals(
                visible,
                shouldReclaimVideoPlaybackSession(
                    isVisible = visible,
                    isSessionActive = true,
                    currentPlayer = player,
                    currentBvid = "BV_parent",
                    currentCid = 10L,
                    pagePlayer = player,
                    pageBvid = "BV_parent",
                    pageCid = 10L,
                    isLeavingByNavigation = true,
                ),
            )
        }
    }

}
