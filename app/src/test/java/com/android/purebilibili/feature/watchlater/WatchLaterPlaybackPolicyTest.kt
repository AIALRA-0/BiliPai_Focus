package com.android.purebilibili.feature.watchlater

import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.VideoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WatchLaterPlaybackPolicyTest {

    private fun item(
        bvid: String,
        title: String,
        duration: Int = 100,
        owner: String = "up",
        cid: Long = 0L,
        progress: Int = -1
    ): VideoItem {
        return VideoItem(
            bvid = bvid,
            title = title,
            pic = "https://example.com/$bvid.jpg",
            duration = duration,
            owner = Owner(name = owner),
            cid = cid,
            progress = progress
        )
    }

    @Test
    fun `buildExternalPlaylist should start from clicked item index`() {
        val watchLaterItems = listOf(
            item("BV1", "first"),
            item("BV2", "second"),
            item("BV3", "third")
        )

        val result = buildExternalPlaylistFromWatchLater(watchLaterItems, clickedBvid = "BV2")

        assertEquals(1, result?.startIndex)
        assertEquals(listOf("BV1", "BV2", "BV3"), result?.playlistItems?.map { it.bvid })
    }

    @Test
    fun `buildExternalPlaylist should fallback to first when clicked item missing`() {
        val watchLaterItems = listOf(
            item("BV1", "first"),
            item("BV2", "second")
        )

        val result = buildExternalPlaylistFromWatchLater(watchLaterItems, clickedBvid = "BV404")

        assertEquals(0, result?.startIndex)
    }

    @Test
    fun `buildExternalPlaylist should return null for empty list`() {
        val result = buildExternalPlaylistFromWatchLater(emptyList(), clickedBvid = "BV1")

        assertNull(result)
    }

    @Test
    fun `resolveWatchLaterTitle includes loaded item count`() {
        assertEquals("稍后再看 (0)", resolveWatchLaterTitle(itemCount = 0))
        assertEquals("稍后再看 (12)", resolveWatchLaterTitle(itemCount = 12))
    }

    @Test
    fun `resolveWatchLaterPlaybackTarget should keep cid and progress from clicked item`() {
        val watchLaterItems = listOf(
            item("BV1", "first", cid = 101L, progress = 12),
            item("BV2", "second", cid = 202L, progress = 321)
        )

        val result = resolveWatchLaterPlaybackTarget(watchLaterItems, clickedBvid = "BV2")

        assertEquals("BV2", result?.bvid)
        assertEquals(202L, result?.cid)
        assertEquals(321_000L, result?.resumePositionMs)
    }

    @Test
    fun `resolveWatchLaterPlaybackTarget should ignore missing progress`() {
        val watchLaterItems = listOf(
            item("BV1", "first", cid = 101L, progress = -1)
        )

        val result = resolveWatchLaterPlaybackTarget(watchLaterItems, clickedBvid = "BV1")

        assertEquals(101L, result?.cid)
        assertEquals(0L, result?.resumePositionMs)
    }
}
