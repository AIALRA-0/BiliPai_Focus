package com.android.purebilibili.feature.list

import com.android.purebilibili.data.model.response.HistoryItem
import com.android.purebilibili.data.model.response.HistoryBusiness
import com.android.purebilibili.data.model.response.VideoItem
import kotlin.test.Test
import kotlin.test.assertEquals

class HistoryPlaybackPolicyTest {

    @Test
    fun `resolveHistoryPlaybackCid prefers cid from history item`() {
        val historyItem = HistoryItem(
            videoItem = VideoItem(bvid = "BV1"),
            business = HistoryBusiness.ARCHIVE,
            cid = 9527L
        )

        val resolved = resolveHistoryPlaybackCid(
            clickedCid = 0L,
            historyItem = historyItem
        )

        assertEquals(9527L, resolved)
    }

    @Test
    fun `resolveHistoryPlaybackCid falls back to clicked cid when history cid missing`() {
        val historyItem = HistoryItem(
            videoItem = VideoItem(bvid = "BV1"),
            business = HistoryBusiness.ARCHIVE,
            cid = 0L
        )

        val resolved = resolveHistoryPlaybackCid(
            clickedCid = 114L,
            historyItem = historyItem
        )

        assertEquals(114L, resolved)
    }

    @Test
    fun `resolveHistoryResumePosition converts positive server progress to milliseconds`() {
        val historyItem = HistoryItem(
            videoItem = VideoItem(bvid = "BV1"),
            business = HistoryBusiness.ARCHIVE,
            progress = 123
        )

        val resolved = resolveHistoryResumePositionMs(historyItem)

        assertEquals(123_000L, resolved)
    }

    @Test
    fun `resolveHistoryResumePosition ignores missing or non-positive progress`() {
        assertEquals(0L, resolveHistoryResumePositionMs(null))
        assertEquals(
            0L,
            resolveHistoryResumePositionMs(
                HistoryItem(
                    videoItem = VideoItem(bvid = "BV1"),
                    business = HistoryBusiness.ARCHIVE,
                    progress = 0
                )
            )
        )
    }

    @Test
    fun `resolveHistoryDisplayProgress prefers furthest valid progress when both are available`() {
        val resolved = resolveHistoryDisplayProgress(
            serverProgressSec = 120,
            durationSec = 600,
            localPositionMs = 300_000L
        )

        assertEquals(300, resolved)
    }

    @Test
    fun `resolveHistoryDisplayProgress falls back to local cached progress when server is zero`() {
        val resolved = resolveHistoryDisplayProgress(
            serverProgressSec = 0,
            durationSec = 600,
            localPositionMs = 180_000L
        )

        assertEquals(180, resolved)
    }

    @Test
    fun `resolveHistoryDisplayProgress prefers local cached progress when it is ahead of server`() {
        val resolved = resolveHistoryDisplayProgress(
            serverProgressSec = 120,
            durationSec = 600,
            localPositionMs = 180_000L
        )

        assertEquals(180, resolved)
    }

    @Test
    fun `resolveHistoryDisplayProgress treats near-end local progress as completed`() {
        val resolved = resolveHistoryDisplayProgress(
            serverProgressSec = 0,
            durationSec = 1000,
            localPositionMs = 980_000L
        )

        assertEquals(-1, resolved)
    }
}
