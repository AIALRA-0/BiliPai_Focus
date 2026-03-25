package com.android.purebilibili.feature.home

import com.android.purebilibili.core.store.FocusFollowHomeFeedSortMode
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
                visibleIncrement = 15,
                hasMore = true,
                continuationFetches = 2,
                isLoadMore = true,
                requiredVisibleIncrement = 16
            )
        )

        assertFalse(
            shouldContinueHomeFollowFetchAfterFocusFilter(
                visibleIncrement = 16,
                hasMore = true,
                continuationFetches = 2,
                isLoadMore = true,
                requiredVisibleIncrement = 16
            )
        )
    }

    @Test
    fun `refresh completion should stop once cached pool already satisfies first sixteen cards`() {
        assertEquals(
            0,
            resolveHomeFollowRequiredVisibleIncrement(
                isLoadMore = false,
                cachedVisibleCount = 24
            )
        )
        assertFalse(
            shouldContinueHomeFollowFetchAfterFocusFilter(
                visibleIncrement = 0,
                hasMore = true,
                continuationFetches = 1,
                isLoadMore = false,
                requiredVisibleIncrement = 0
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
            video(id = 1, bvid = "BV1", dynamicId = "dyn-1", ownerMid = 101L, pubdate = 400L),
            video(id = 2, bvid = "BV2", dynamicId = "dyn-2", ownerMid = 101L, pubdate = 100L),
            video(id = 3, bvid = "BV3", dynamicId = "dyn-3", ownerMid = 202L, pubdate = 300L),
            video(id = 4, bvid = "BV4", dynamicId = "dyn-4", ownerMid = 202L, pubdate = 200L)
        )

        val seedAResult = randomizeHomeFollowIncomingVideos(source, seed = 11L)
        val seedAOrder = seedAResult.map { it.dynamicId }
        val seedAOrderAgain = randomizeHomeFollowIncomingVideos(source, seed = 11L).map { it.dynamicId }
        val seedBResult = randomizeHomeFollowIncomingVideos(source, seed = 29L)
        val seedBOrder = seedBResult.map { it.dynamicId }

        assertEquals(seedAOrder, seedAOrderAgain)
        assertNotEquals(seedAOrder, seedBOrder)
        assertNotEquals(
            listOf(400L, 300L, 200L, 100L),
            seedAResult.map { it.pubdate }
        )
        assertNotEquals(
            listOf(400L, 300L, 200L, 100L),
            seedBResult.map { it.pubdate }
        )
    }

    @Test
    fun `follow incoming randomization should ignore prioritized videos and randomize the full pool`() {
        val source = listOf(
            video(id = 1, bvid = "BV1", dynamicId = "dyn-1", ownerMid = 11L, pubdate = 100L),
            video(id = 2, bvid = "BV2", dynamicId = "dyn-2", ownerMid = 11L, pubdate = 90L),
            video(id = 3, bvid = "BV3", dynamicId = "dyn-3", ownerMid = 22L, pubdate = 80L),
            video(id = 4, bvid = "BV4", dynamicId = "dyn-4", ownerMid = 22L, pubdate = 70L),
            video(id = 5, bvid = "BV5", dynamicId = "dyn-5", ownerMid = 33L, pubdate = 60L)
        )

        val baseline = randomizeHomeFollowIncomingVideos(source, seed = 7L)
        val prioritized = randomizeHomeFollowIncomingVideos(
            videos = source,
            seed = 7L,
            prioritizedVideoKeys = setOf(
                resolveHomeFollowVideoKey(source[3]),
                resolveHomeFollowVideoKey(source[4])
            )
        )

        assertEquals(
            baseline.map { it.dynamicId },
            prioritized.map { it.dynamicId }
        )
    }

    @Test
    fun `refresh presentation should reshuffle the full visible follow list`() {
        val source = listOf(
            video(id = 1, bvid = "BV1", dynamicId = "dyn-1", ownerMid = 11L, pubdate = 100L),
            video(id = 2, bvid = "BV2", dynamicId = "dyn-2", ownerMid = 11L, pubdate = 100L),
            video(id = 3, bvid = "BV3", dynamicId = "dyn-3", ownerMid = 22L, pubdate = 100L),
            video(id = 4, bvid = "BV4", dynamicId = "dyn-4", ownerMid = 33L, pubdate = 100L)
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
    fun `random refresh should not front load newly fetched videos`() {
        val oldA = video(id = 1, bvid = "BV1", dynamicId = "dyn-old-1", ownerMid = 11L)
        val oldB = video(id = 2, bvid = "BV2", dynamicId = "dyn-old-2", ownerMid = 22L)
        val newA = video(id = 3, bvid = "BV3", dynamicId = "dyn-new-1", ownerMid = 33L)
        val newB = video(id = 4, bvid = "BV4", dynamicId = "dyn-new-2", ownerMid = 44L)

        val refreshed = presentHomeFollowVisibleVideos(
            existingPresentedVisibleVideos = emptyList(),
            incomingVisibleVideos = listOf(oldA, oldB, newA, newB),
            isLoadMore = false,
            seed = 5L,
            reshuffleOnRefresh = true,
            prioritizedVideoKeys = setOf(
                resolveHomeFollowVideoKey(newA),
                resolveHomeFollowVideoKey(newB)
            )
        )
        val refreshedWithoutPrioritization = presentHomeFollowVisibleVideos(
            existingPresentedVisibleVideos = emptyList(),
            incomingVisibleVideos = listOf(oldA, oldB, newA, newB),
            isLoadMore = false,
            seed = 5L,
            reshuffleOnRefresh = true
        )

        assertEquals(
            refreshedWithoutPrioritization.map { it.dynamicId },
            refreshed.map { it.dynamicId }
        )
    }

    @Test
    fun `creator cluster refresh should stay deterministic instead of front-loading prioritized videos`() {
        val oldA = video(id = 1, bvid = "BV1", dynamicId = "dyn-old-1", ownerMid = 11L, pubdate = 100L)
        val oldB = video(id = 2, bvid = "BV2", dynamicId = "dyn-old-2", ownerMid = 11L, pubdate = 95L)
        val newerOtherCreator = video(
            id = 3,
            bvid = "BV3",
            dynamicId = "dyn-old-3",
            ownerMid = 22L,
            pubdate = 140L
        )
        val newlyFetchedOlderCreatorVideo = video(
            id = 4,
            bvid = "BV4",
            dynamicId = "dyn-new-1",
            ownerMid = 33L,
            pubdate = 90L
        )

        val clustered = presentHomeFollowVisibleVideos(
            existingPresentedVisibleVideos = emptyList(),
            incomingVisibleVideos = listOf(oldA, oldB, newerOtherCreator, newlyFetchedOlderCreatorVideo),
            isLoadMore = false,
            seed = 99L,
            reshuffleOnRefresh = true,
            prioritizedVideoKeys = setOf(resolveHomeFollowVideoKey(newlyFetchedOlderCreatorVideo)),
            sortMode = FocusFollowHomeFeedSortMode.CREATOR_CLUSTER_DESC
        )

        assertEquals(
            listOf("dyn-old-3", "dyn-old-1", "dyn-old-2", "dyn-new-1"),
            clustered.map { it.dynamicId }
        )
    }

    @Test
    fun `publish time refresh should stay deterministic instead of front-loading prioritized videos`() {
        val oldestNew = video(id = 1, bvid = "BV1", dynamicId = "dyn-new-1", ownerMid = 11L, pubdate = 90L)
        val newestOld = video(id = 2, bvid = "BV2", dynamicId = "dyn-old-1", ownerMid = 22L, pubdate = 150L)
        val middleOld = video(id = 3, bvid = "BV3", dynamicId = "dyn-old-2", ownerMid = 33L, pubdate = 120L)

        val sorted = presentHomeFollowVisibleVideos(
            existingPresentedVisibleVideos = emptyList(),
            incomingVisibleVideos = listOf(oldestNew, newestOld, middleOld),
            isLoadMore = false,
            seed = 77L,
            reshuffleOnRefresh = true,
            prioritizedVideoKeys = setOf(resolveHomeFollowVideoKey(oldestNew)),
            sortMode = FocusFollowHomeFeedSortMode.PUBLISH_TIME_DESC
        )

        assertEquals(
            listOf("dyn-old-1", "dyn-old-2", "dyn-new-1"),
            sorted.map { it.dynamicId }
        )
    }

    @Test
    fun `creator cluster descending keeps latest active creator first and groups videos by up`() {
        val source = listOf(
            video(id = 1, bvid = "BV1", dynamicId = "dyn-1", ownerMid = 11L, pubdate = 100L),
            video(id = 2, bvid = "BV2", dynamicId = "dyn-2", ownerMid = 11L, pubdate = 95L),
            video(id = 3, bvid = "BV3", dynamicId = "dyn-3", ownerMid = 22L, pubdate = 130L),
            video(id = 4, bvid = "BV4", dynamicId = "dyn-4", ownerMid = 22L, pubdate = 120L),
            video(id = 5, bvid = "BV5", dynamicId = "dyn-5", ownerMid = 33L, pubdate = 90L)
        )

        val clustered = presentHomeFollowVisibleVideos(
            existingPresentedVisibleVideos = emptyList(),
            incomingVisibleVideos = source,
            isLoadMore = false,
            seed = 1L,
            reshuffleOnRefresh = true,
            sortMode = FocusFollowHomeFeedSortMode.CREATOR_CLUSTER_DESC
        )

        assertEquals(
            listOf("dyn-3", "dyn-4", "dyn-1", "dyn-2", "dyn-5"),
            clustered.map { it.dynamicId }
        )
    }

    @Test
    fun `creator cluster ascending keeps earliest active creator first and groups videos by up`() {
        val source = listOf(
            video(id = 1, bvid = "BV1", dynamicId = "dyn-1", ownerMid = 11L, pubdate = 100L),
            video(id = 2, bvid = "BV2", dynamicId = "dyn-2", ownerMid = 11L, pubdate = 95L),
            video(id = 3, bvid = "BV3", dynamicId = "dyn-3", ownerMid = 22L, pubdate = 130L),
            video(id = 4, bvid = "BV4", dynamicId = "dyn-4", ownerMid = 22L, pubdate = 120L),
            video(id = 5, bvid = "BV5", dynamicId = "dyn-5", ownerMid = 33L, pubdate = 90L)
        )

        val clustered = presentHomeFollowVisibleVideos(
            existingPresentedVisibleVideos = emptyList(),
            incomingVisibleVideos = source,
            isLoadMore = false,
            seed = 1L,
            reshuffleOnRefresh = true,
            sortMode = FocusFollowHomeFeedSortMode.CREATOR_CLUSTER_ASC
        )

        assertEquals(
            listOf("dyn-5", "dyn-2", "dyn-1", "dyn-4", "dyn-3"),
            clustered.map { it.dynamicId }
        )
    }

    @Test
    fun `publish time descending keeps newest visible videos first`() {
        val source = listOf(
            video(id = 1, bvid = "BV1", dynamicId = "dyn-1", ownerMid = 11L, pubdate = 100L),
            video(id = 2, bvid = "BV2", dynamicId = "dyn-2", ownerMid = 22L, pubdate = 140L),
            video(id = 3, bvid = "BV3", dynamicId = "dyn-3", ownerMid = 33L, pubdate = 120L)
        )

        val sorted = presentHomeFollowVisibleVideos(
            existingPresentedVisibleVideos = emptyList(),
            incomingVisibleVideos = source,
            isLoadMore = false,
            seed = 1L,
            reshuffleOnRefresh = true,
            sortMode = FocusFollowHomeFeedSortMode.PUBLISH_TIME_DESC
        )

        assertEquals(
            listOf("dyn-2", "dyn-3", "dyn-1"),
            sorted.map { it.dynamicId }
        )
    }

    @Test
    fun `publish time ascending keeps oldest visible videos first`() {
        val source = listOf(
            video(id = 1, bvid = "BV1", dynamicId = "dyn-1", ownerMid = 11L, pubdate = 100L),
            video(id = 2, bvid = "BV2", dynamicId = "dyn-2", ownerMid = 22L, pubdate = 140L),
            video(id = 3, bvid = "BV3", dynamicId = "dyn-3", ownerMid = 33L, pubdate = 120L)
        )

        val sorted = presentHomeFollowVisibleVideos(
            existingPresentedVisibleVideos = emptyList(),
            incomingVisibleVideos = source,
            isLoadMore = false,
            seed = 1L,
            reshuffleOnRefresh = true,
            sortMode = FocusFollowHomeFeedSortMode.PUBLISH_TIME_ASC
        )

        assertEquals(
            listOf("dyn-1", "dyn-3", "dyn-2"),
            sorted.map { it.dynamicId }
        )
    }

    @Test
    fun `load more presentation should keep existing order and append only new entries`() {
        val oldA = video(id = 1, bvid = "BV1", dynamicId = "dyn-1", pubdate = 400L)
        val oldB = video(id = 2, bvid = "BV2", dynamicId = "dyn-2", pubdate = 300L)
        val newA = video(id = 3, bvid = "BV3", dynamicId = "dyn-3", pubdate = 200L)
        val newB = video(id = 4, bvid = "BV4", dynamicId = "dyn-4", pubdate = 100L)

        val presented = presentHomeFollowVisibleVideos(
            existingPresentedVisibleVideos = listOf(oldA, oldB),
            incomingVisibleVideos = listOf(oldA, oldB, newA, newB),
            isLoadMore = true,
            seed = 99L,
            reshuffleOnRefresh = true,
            sortMode = FocusFollowHomeFeedSortMode.PUBLISH_TIME_DESC
        )

        assertEquals(
            listOf(oldA, oldB, newA, newB).map { it.dynamicId },
            presented.map { it.dynamicId }
        )
    }

    @Test
    fun `random load more should append unseen videos following the active random order`() {
        val source = listOf(
            video(id = 1, bvid = "BV1", dynamicId = "dyn-1", ownerMid = 11L, pubdate = 400L),
            video(id = 2, bvid = "BV2", dynamicId = "dyn-2", ownerMid = 11L, pubdate = 100L),
            video(id = 3, bvid = "BV3", dynamicId = "dyn-3", ownerMid = 22L, pubdate = 300L),
            video(id = 4, bvid = "BV4", dynamicId = "dyn-4", ownerMid = 22L, pubdate = 200L)
        )
        val ordered = randomizeHomeFollowIncomingVideos(source, seed = 99L)

        val presented = presentHomeFollowVisibleVideos(
            existingPresentedVisibleVideos = ordered.take(2),
            incomingVisibleVideos = source,
            isLoadMore = true,
            seed = 99L,
            reshuffleOnRefresh = true
        )

        assertEquals(
            ordered.map { it.dynamicId },
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
        ownerMid: Long = id,
        pubdate: Long = id
    ): VideoItem {
        return VideoItem(
            id = id,
            aid = id,
            bvid = bvid,
            dynamicId = dynamicId,
            title = "video-$id",
            pubdate = pubdate,
            owner = Owner(mid = ownerMid, name = "up-$ownerMid")
        )
    }
}
