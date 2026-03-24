package com.android.purebilibili.feature.home

import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.VideoItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class HomeFollowFocusPolicyTest {

    @Test
    fun `follow completion keeps fetching until at least sixteen visible items are added`() {
        assertTrue(
            shouldContinueHomeFollowFetchAfterFocusFilter(
                baselineVisibleCount = 12,
                visibleIncrement = 15,
                hasMore = true,
                continuationFetches = 2,
                isLoadMore = true
            )
        )

        assertFalse(
            shouldContinueHomeFollowFetchAfterFocusFilter(
                baselineVisibleCount = 12,
                visibleIncrement = 16,
                hasMore = true,
                continuationFetches = 2,
                isLoadMore = true
            )
        )
    }

    @Test
    fun `refresh should keep cached follow videos and prepend new ones`() {
        val oldA = video(id = 1, bvid = "BVOLD1", dynamicId = "dyn-old-1")
        val oldB = video(id = 2, bvid = "BVOLD2", dynamicId = "dyn-old-2")
        val newA = video(id = 3, bvid = "BVNEW1", dynamicId = "dyn-new-1")
        val newB = video(id = 4, bvid = "BVNEW2", dynamicId = "dyn-new-2")

        val merged = resolveHomeFollowPresentedRawVideos(
            baselineRawVideos = listOf(oldA, oldB),
            roundRawVideos = listOf(newA, newB, oldB),
            isLoadMore = false,
            keySelector = { it.dynamicId }
        )

        assertEquals(
            listOf(newA, newB, oldA, oldB).map { it.dynamicId },
            merged.map { it.dynamicId }
        )
    }

    @Test
    fun `follow incoming randomization should be deterministic per seed and vary across seeds`() {
        val source = listOf(
            video(id = 1, bvid = "BV1", dynamicId = "dyn-1", ownerMid = 101L),
            video(id = 2, bvid = "BV2", dynamicId = "dyn-2", ownerMid = 202L),
            video(id = 3, bvid = "BV3", dynamicId = "dyn-3", ownerMid = 303L),
            video(id = 4, bvid = "BV4", dynamicId = "dyn-4", ownerMid = 404L)
        )

        val seedAOrder = randomizeHomeFollowIncomingVideos(source, seed = 11L).map { it.dynamicId }
        val seedAOrderAgain = randomizeHomeFollowIncomingVideos(source, seed = 11L).map { it.dynamicId }
        val seedBOrder = randomizeHomeFollowIncomingVideos(source, seed = 29L).map { it.dynamicId }

        assertEquals(seedAOrder, seedAOrderAgain)
        assertNotEquals(seedAOrder, seedBOrder)
    }

    @Test
    fun `follow incoming randomization should interleave creators before repeating same up`() {
        val source = listOf(
            video(id = 1, bvid = "BV1", dynamicId = "dyn-1", ownerMid = 11L),
            video(id = 2, bvid = "BV2", dynamicId = "dyn-2", ownerMid = 11L),
            video(id = 3, bvid = "BV3", dynamicId = "dyn-3", ownerMid = 22L),
            video(id = 4, bvid = "BV4", dynamicId = "dyn-4", ownerMid = 22L),
            video(id = 5, bvid = "BV5", dynamicId = "dyn-5", ownerMid = 33L)
        )

        val randomized = randomizeHomeFollowIncomingVideos(source, seed = 7L)

        assertEquals(3, randomized.take(3).map { it.owner.mid }.distinct().size)
    }

    @Test
    fun `refresh presentation should reshuffle the full visible follow list`() {
        val source = listOf(
            video(id = 1, bvid = "BV1", dynamicId = "dyn-1", ownerMid = 11L),
            video(id = 2, bvid = "BV2", dynamicId = "dyn-2", ownerMid = 11L),
            video(id = 3, bvid = "BV3", dynamicId = "dyn-3", ownerMid = 22L),
            video(id = 4, bvid = "BV4", dynamicId = "dyn-4", ownerMid = 33L)
        )

        val refreshed = presentHomeFollowVisibleVideos(
            existingPresentedVisibleVideos = listOf(video(id = 99, bvid = "BV99", dynamicId = "dyn-99")),
            incomingVisibleVideos = source,
            isLoadMore = false,
            seed = 7L,
            reshuffleOnRefresh = true
        )

        assertEquals(
            randomizeHomeFollowIncomingVideos(source, seed = 7L).map { it.dynamicId },
            refreshed.map { it.dynamicId }
        )
        assertNotEquals(source.map { it.dynamicId }, refreshed.map { it.dynamicId })
    }

    @Test
    fun `load more presentation should keep existing order and append only new entries`() {
        val oldA = video(id = 1, bvid = "BV1", dynamicId = "dyn-1")
        val oldB = video(id = 2, bvid = "BV2", dynamicId = "dyn-2")
        val newA = video(id = 3, bvid = "BV3", dynamicId = "dyn-3")
        val newB = video(id = 4, bvid = "BV4", dynamicId = "dyn-4")

        val presented = presentHomeFollowVisibleVideos(
            existingPresentedVisibleVideos = listOf(oldB, oldA),
            incomingVisibleVideos = listOf(oldA, oldB, newA, newB),
            isLoadMore = true,
            seed = 99L,
            reshuffleOnRefresh = true
        )

        assertEquals(
            listOf(oldB, oldA, newA, newB).map { it.dynamicId },
            presented.map { it.dynamicId }
        )
    }

    @Test
    fun `follow display window should reveal sixteen items per page`() {
        val allVideos = (1L..40L).map { index ->
            video(
                id = index,
                bvid = "BV$index",
                dynamicId = "dyn-$index"
            )
        }

        val refreshDisplayCount = resolveHomeFollowDisplayCount(
            currentDisplayCount = 48,
            isLoadMore = false
        )
        val firstPage = resolveDisplayedHomeFollowVisibleVideos(
            presentedVisibleVideos = allVideos,
            displayCount = refreshDisplayCount
        )
        val secondPageCount = resolveHomeFollowDisplayCount(
            currentDisplayCount = firstPage.size,
            isLoadMore = true
        )
        val secondPage = resolveDisplayedHomeFollowVisibleVideos(
            presentedVisibleVideos = allVideos,
            displayCount = secondPageCount
        )

        assertEquals(16, firstPage.size)
        assertEquals(32, secondPage.size)
        assertTrue(
            canRevealMorePresentedHomeFollowVideos(
                presentedVisibleCount = allVideos.size,
                displayedVisibleCount = firstPage.size
            )
        )
        assertTrue(
            resolveHomeFollowPresentationHasMore(
                presentedVisibleCount = allVideos.size,
                displayedVisibleCount = secondPage.size,
                sourceHasMore = false
            )
        )
    }

    private fun video(
        id: Long,
        bvid: String,
        dynamicId: String,
        ownerMid: Long = id
    ): VideoItem {
        return VideoItem(
            id = id,
            aid = id,
            bvid = bvid,
            dynamicId = dynamicId,
            title = "video-$id",
            owner = Owner(mid = ownerMid, name = "up-$ownerMid")
        )
    }
}
