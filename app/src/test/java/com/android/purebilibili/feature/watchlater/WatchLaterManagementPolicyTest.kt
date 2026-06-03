package com.android.purebilibili.feature.watchlater

import com.android.purebilibili.data.model.response.VideoItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WatchLaterManagementPolicyTest {

    private fun item(
        bvid: String,
        progress: Int,
        duration: Int = 100
    ): VideoItem {
        return VideoItem(
            id = bvid.removePrefix("BV").toLongOrNull() ?: 0L,
            bvid = bvid,
            title = bvid,
            progress = progress,
            duration = duration
        )
    }

    @Test
    fun `viewed policy only treats completed videos as viewed`() {
        assertTrue(isWatchLaterViewed(item("BV1", progress = 100, duration = 100)))
        assertTrue(isWatchLaterViewed(item("BV2", progress = 120, duration = 100)))
        assertFalse(isWatchLaterViewed(item("BV3", progress = 99, duration = 100)))
        assertFalse(isWatchLaterViewed(item("BV4", progress = -1, duration = 100)))
        assertFalse(isWatchLaterViewed(item("BV5", progress = 120, duration = 0)))
    }

    @Test
    fun `clear viewed keeps unfinished and unknown progress items`() {
        val items = listOf(
            item("BV1", progress = 100, duration = 100),
            item("BV2", progress = 12, duration = 100),
            item("BV3", progress = -1, duration = 100)
        )

        val result = resolveWatchLaterItemsAfterManagementAction(
            items = items,
            action = WatchLaterManagementAction.CLEAR_VIEWED
        )

        assertEquals(listOf("BV2", "BV3"), result.map { it.bvid })
    }

    @Test
    fun `clear all removes every local item`() {
        val items = listOf(
            item("BV1", progress = 100),
            item("BV2", progress = 12)
        )

        val result = resolveWatchLaterItemsAfterManagementAction(
            items = items,
            action = WatchLaterManagementAction.CLEAR_ALL
        )

        assertEquals(emptyList(), result)
    }
}
