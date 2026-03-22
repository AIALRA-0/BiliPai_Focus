package com.android.purebilibili.feature.list

import com.android.purebilibili.data.model.response.HistoryBusiness
import com.android.purebilibili.data.model.response.HistoryItem
import com.android.purebilibili.data.model.response.VideoItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HistoryDeletePolicyTest {

    @Test
    fun `resolveHistoryDeleteKid returns archive kid for normal videos`() {
        val item = HistoryItem(
            videoItem = VideoItem(id = 540580868L, bvid = "BV1NK41157EF"),
            business = HistoryBusiness.ARCHIVE
        )

        assertEquals("archive_540580868", resolveHistoryDeleteKid(item))
    }

    @Test
    fun `resolveHistoryDeleteKid returns pgc kid from season id`() {
        val item = HistoryItem(
            videoItem = VideoItem(id = 0L),
            business = HistoryBusiness.PGC,
            seasonId = 12345L
        )

        assertEquals("pgc_12345", resolveHistoryDeleteKid(item))
    }

    @Test
    fun `resolveHistoryDeleteKid returns live kid from room id`() {
        val item = HistoryItem(
            videoItem = VideoItem(id = 0L),
            business = HistoryBusiness.LIVE,
            roomId = 778899L
        )

        assertEquals("live_778899", resolveHistoryDeleteKid(item))
    }

    @Test
    fun `resolveHistoryDeleteKid returns article kid`() {
        val item = HistoryItem(
            videoItem = VideoItem(id = 334455L),
            business = HistoryBusiness.ARTICLE
        )

        assertEquals("article_334455", resolveHistoryDeleteKid(item))
    }

    @Test
    fun `resolveHistoryDeleteKid falls back to archive for unknown video-like entries`() {
        val item = HistoryItem(
            videoItem = VideoItem(id = 7788L, bvid = "BV1xx"),
            business = HistoryBusiness.UNKNOWN
        )

        assertEquals("archive_7788", resolveHistoryDeleteKid(item))
    }

    @Test
    fun `resolveHistoryDeleteKid returns null when unknown entry lacks valid id`() {
        val item = HistoryItem(
            videoItem = VideoItem(id = 0L),
            business = HistoryBusiness.UNKNOWN
        )

        assertNull(resolveHistoryDeleteKid(item))
    }

    @Test
    fun `resolveHistoryRenderKey prefers bvid`() {
        val item = HistoryItem(
            videoItem = VideoItem(id = 1L, bvid = "BV1abc"),
            business = HistoryBusiness.ARCHIVE
        )

        assertEquals("BV1abc", resolveHistoryRenderKey(item))
    }

    @Test
    fun `resolveHistoryRenderKey falls back to business and oid`() {
        val item = HistoryItem(
            videoItem = VideoItem(id = 2468L, bvid = ""),
            business = HistoryBusiness.PGC
        )

        assertEquals("pgc_2468", resolveHistoryRenderKey(item))
    }
}
