package com.android.purebilibili.feature.space

import com.android.purebilibili.data.model.response.SpaceVideoItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpacePlaybackPolicyTest {

    private fun item(
        bvid: String,
        title: String,
        length: String,
        author: String = "up"
    ): SpaceVideoItem {
        return SpaceVideoItem(
            bvid = bvid,
            title = title,
            pic = "https://example.com/$bvid.jpg",
            length = length,
            author = author
        )
    }

    @Test
    fun buildExternalPlaylistFromSpaceVideos_startsFromClickedVideo() {
        val videos = listOf(
            item(bvid = "BV1", title = "first", length = "01:23"),
            item(bvid = "BV2", title = "second", length = "10:24"),
            item(bvid = "BV3", title = "third", length = "1:02:03")
        )

        val playlist = buildExternalPlaylistFromSpaceVideos(videos, clickedBvid = "BV2")

        assertEquals(1, playlist?.startIndex)
        assertEquals(listOf("BV1", "BV2", "BV3"), playlist?.playlistItems?.map { it.bvid })
        assertEquals(83L, playlist?.playlistItems?.get(0)?.duration)
        assertEquals(624L, playlist?.playlistItems?.get(1)?.duration)
        assertEquals(3723L, playlist?.playlistItems?.get(2)?.duration)
    }

    @Test
    fun buildExternalPlaylistFromSpaceVideos_fallbackToFirstWhenClickedMissing() {
        val videos = listOf(
            item(bvid = "BV1", title = "first", length = "00:30"),
            item(bvid = "BV2", title = "second", length = "01:00")
        )

        val playlist = buildExternalPlaylistFromSpaceVideos(videos, clickedBvid = "BV404")

        assertEquals(0, playlist?.startIndex)
    }

    @Test
    fun buildExternalPlaylistFromSpaceVideos_returnsNullForEmptyVideos() {
        assertNull(buildExternalPlaylistFromSpaceVideos(emptyList(), clickedBvid = "BV1"))
    }

    @Test
    fun resolveSpacePlayAllStartTarget_returnsFirstVideoBvid() {
        val videos = listOf(
            item(bvid = "BV1", title = "first", length = "00:10"),
            item(bvid = "BV2", title = "second", length = "00:20")
        )

        assertEquals("BV1", resolveSpacePlayAllStartTarget(videos))
        assertNull(resolveSpacePlayAllStartTarget(emptyList()))
    }

    @Test
    fun resolveSpaceVideoProgressState_showsLocalPlaybackProgress() {
        val state = resolveSpaceVideoProgressState(
            video = item(bvid = "BV1", title = "first", length = "02:00"),
            localPositionMs = 30_000L
        )

        assertTrue(state.showProgressBar)
        assertEquals(30, state.progressSec)
        assertEquals(0.25f, state.progressFraction)
    }

    @Test
    fun resolveSpaceVideoProgressState_marksNearEndLocalProgressCompleted() {
        val state = resolveSpaceVideoProgressState(
            video = item(bvid = "BV1", title = "first", length = "02:00"),
            localPositionMs = 118_000L
        )

        assertTrue(state.showProgressBar)
        assertEquals(-1, state.progressSec)
        assertEquals(1f, state.progressFraction)
    }

    @Test
    fun resolveSpaceResumePositionMs_usesPositiveLocalProgressOnly() {
        assertEquals(42_000L, resolveSpaceResumePositionMs(localPositionMs = 42_000L))
        assertEquals(0L, resolveSpaceResumePositionMs(localPositionMs = -1L))
    }

    @Test
    fun resolveSpacePriorityTabLoadState_keeps_tabs_independent() {
        val shell = buildInitialTabShellState(selectedTab = SpaceMainTab.CONTRIBUTION)
            .withUpdatedTab(SpaceMainTab.CONTRIBUTION) { it.copy(isLoading = true, hasLoaded = true) }
            .withUpdatedTab(SpaceMainTab.DYNAMIC) { it.copy(error = "动态失败", hasLoaded = true) }

        val state = resolveSpacePriorityTabLoadState(shell)

        assertTrue(state.contribution.isLoading)
        assertEquals("动态失败", state.dynamic.error)
        assertEquals(false, state.collections.hasLoaded)
    }

    @Test
    fun resolveSpaceCollectionDetailRequest_validates_supported_targets() {
        val season = resolveSpaceCollectionDetailRequest(
            type = "season",
            id = 12L,
            mid = 34L,
            title = "合集"
        )
        val favorite = resolveSpaceCollectionDetailRequest(
            type = "favorite",
            id = 56L,
            mid = 0L,
            title = "收藏夹"
        )
        val favoriteSeason = resolveSpaceCollectionDetailRequest(
            type = "favorite_season",
            id = 78L,
            mid = 0L,
            title = "追更合集"
        )

        assertEquals(SpaceCollectionDetailType.SEASON, season?.type)
        assertEquals("合集", season?.title)
        assertEquals(SpaceCollectionDetailType.FAVORITE, favorite?.type)
        assertEquals(SpaceCollectionDetailType.FAVORITE_SEASON, favoriteSeason?.type)
        assertNull(resolveSpaceCollectionDetailRequest("series", id = 0L, mid = 1L, title = ""))
        assertNull(resolveSpaceCollectionDetailRequest("unknown", id = 1L, mid = 1L, title = ""))
    }
}
