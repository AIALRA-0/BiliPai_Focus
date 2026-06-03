package com.android.purebilibili.feature.list

import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.VideoItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FavoritePlaybackPolicyTest {

    private fun item(
        bvid: String,
        title: String,
        duration: Int = 100,
        owner: String = "up",
        isCollectionResource: Boolean = false
    ): VideoItem {
        return VideoItem(
            bvid = bvid,
            title = title,
            pic = "https://example.com/$bvid.jpg",
            duration = duration,
            owner = Owner(name = owner),
            isCollectionResource = isCollectionResource
        )
    }

    @Test
    fun buildExternalPlaylistFromFavorite_startsFromClickedVideo() {
        val items = listOf(
            item("BV1", "first"),
            item("BV2", "second"),
            item("BV3", "third")
        )

        val result = buildExternalPlaylistFromFavorite(items, clickedBvid = "BV2")

        assertEquals(1, result?.startIndex)
        assertEquals(listOf("BV1", "BV2", "BV3"), result?.playlistItems?.map { it.bvid })
    }

    @Test
    fun buildExternalPlaylistFromFavorite_fallbackToFirstWhenClickedMissing() {
        val items = listOf(
            item("BV1", "first"),
            item("BV2", "second")
        )

        val result = buildExternalPlaylistFromFavorite(items, clickedBvid = "BV404")

        assertEquals(0, result?.startIndex)
    }

    @Test
    fun buildExternalPlaylistFromFavorite_returnsNullForEmptyItems() {
        val result = buildExternalPlaylistFromFavorite(emptyList(), clickedBvid = "BV1")
        assertNull(result)
    }

    @Test
    fun buildExternalPlaylistFromFavorite_filtersCollectionRowsAndBlankBvids() {
        val items = listOf(
            item("BV1", "first"),
            item("BV_COLLECTION", "collection", isCollectionResource = true),
            item("", "broken"),
            item("BV2", "second")
        )

        val result = buildExternalPlaylistFromFavorite(items, clickedBvid = "BV2")

        assertEquals(1, result?.startIndex)
        assertEquals(listOf("BV1", "BV2"), result?.playlistItems?.map { it.bvid })
    }

    @Test
    fun favoriteDetailShouldUseFavoriteExternalPlaylist() {
        assertEquals(
            true,
            shouldUseFavoriteExternalPlaylist(
                hasFavoriteViewModel = false,
                isFavoriteDetail = true
            )
        )
        assertEquals(
            false,
            shouldUseFavoriteExternalPlaylist(
                hasFavoriteViewModel = false,
                isFavoriteDetail = false
            )
        )
    }
}
