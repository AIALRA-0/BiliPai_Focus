package com.android.purebilibili.feature.list

import com.android.purebilibili.data.model.response.HistoryBusiness
import com.android.purebilibili.data.model.response.HistoryItem
import com.android.purebilibili.data.model.response.VideoItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HistoryFilterPolicyTest {

    @Test
    fun `content filter keeps only the selected history type`() {
        val items = listOf(
            historyVideo("BV1video", HistoryBusiness.ARCHIVE),
            historyVideo("BV1pgc", HistoryBusiness.PGC),
            historyVideo("BV1live", HistoryBusiness.LIVE),
            historyVideo("", HistoryBusiness.ARTICLE, id = 42L)
        )
        val historyById = items.associate { it.videoItem.id to it }

        val result = filterHistoryItemsByContent(
            items = items.map(HistoryItem::videoItem),
            filter = HistoryContentFilter.LIVE,
            resolveHistoryItem = { historyById[it.id] }
        )

        assertEquals(listOf("BV1live"), result.map(VideoItem::bvid))
    }

    @Test
    fun `unknown history type falls back to video filter`() {
        val item = historyVideo("BV1unknown", HistoryBusiness.UNKNOWN)

        val result = filterHistoryItemsByContent(
            items = listOf(item.videoItem),
            filter = HistoryContentFilter.VIDEO,
            resolveHistoryItem = { item }
        )

        assertEquals(listOf(item.videoItem), result)
    }

    @Test
    fun `history list type uses documented cursor filter values`() {
        assertEquals(null, resolveHistoryListType(HistoryContentFilter.ALL))
        assertEquals("archive", resolveHistoryListType(HistoryContentFilter.VIDEO))
        assertEquals(null, resolveHistoryListType(HistoryContentFilter.PGC))
        assertEquals("live", resolveHistoryListType(HistoryContentFilter.LIVE))
        assertEquals("article", resolveHistoryListType(HistoryContentFilter.ARTICLE))
    }

    @Test
    fun `empty filtered page requests another cursor page when possible`() {
        assertTrue(
            shouldLoadMoreHistoryFilterResults(
                filter = HistoryContentFilter.ARTICLE,
                filteredItemCount = 0,
                hasMore = true,
                isLoading = false
            )
        )
        assertFalse(
            shouldLoadMoreHistoryFilterResults(
                filter = HistoryContentFilter.ALL,
                filteredItemCount = 0,
                hasMore = true,
                isLoading = false
            )
        )
        assertFalse(
            shouldLoadMoreHistoryFilterResults(
                filter = HistoryContentFilter.ARTICLE,
                filteredItemCount = 0,
                hasMore = true,
                isLoading = true
            )
        )
    }

    private fun historyVideo(
        bvid: String,
        business: HistoryBusiness,
        id: Long = business.ordinal.toLong() + 1L
    ): HistoryItem {
        return HistoryItem(
            videoItem = VideoItem(id = id, bvid = bvid),
            business = business
        )
    }
}
