package com.android.purebilibili.feature.video.player

import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlaylistItemMetadataTest {
    @Test
    fun `saved queues without metadata remain readable`() {
        val item = Json.decodeFromString<PlaylistItem>(
            """{"bvid":"BV1","title":"标题","cover":"","owner":"UP"}""",
        )
        assertEquals("BV1", item.bvid)
        assertNull(item.playCount)
        assertNull(item.commentCount)
        assertNull(item.publishedAt)
    }

    @Test
    fun `queue metadata survives saving and restoring`() {
        val item = PlaylistItem(
            bvid = "BV1", title = "标题", cover = "", owner = "UP",
            duration = 123L, playCount = 1000L, commentCount = 0L, publishedAt = 1792022400L,
        )
        assertEquals(item, Json.decodeFromString<PlaylistItem>(Json.encodeToString(item)))
    }
}
