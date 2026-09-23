package com.android.purebilibili.feature.bangumi

import com.android.purebilibili.data.model.response.BangumiDetail
import com.android.purebilibili.data.model.response.BangumiEpisode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BangumiResumePolicyTest {

    @Test
    fun `bangumi heartbeat allows initial zero progress while playing`() {
        assertTrue(
            shouldSendBangumiPlaybackHeartbeat(
                isPlaying = true,
                bvid = "BV1pgc",
                cid = 1122L,
                currentPositionMs = 0L
            )
        )
    }

    @Test
    fun `bangumi heartbeat still rejects missing identifiers`() {
        assertFalse(
            shouldSendBangumiPlaybackHeartbeat(
                isPlaying = true,
                bvid = "",
                cid = 1122L,
                currentPositionMs = 1000L
            )
        )
        assertFalse(
            shouldSendBangumiPlaybackHeartbeat(
                isPlaying = true,
                bvid = "BV1pgc",
                cid = 0L,
                currentPositionMs = 1000L
            )
        )
    }

    @Test
    fun `bangumi detail request prefers episode id from pgc history`() {
        val request = resolveBangumiDetailRequest(seasonId = 114514L, epId = 1919810L)

        assertEquals(0L, request.seasonId)
        assertEquals(1919810L, request.epId)
    }

    @Test
    fun `bangumi playback restores local episode progress when the route has none`() {
        assertEquals(
            45_000L,
            resolveBangumiPlaybackStartPositionMs(
                routeResumePositionMs = 0L,
                savedEpisodePositionMs = 45_000L
            )
        )
    }

    @Test
    fun `bangumi playback route progress overrides local episode progress`() {
        assertEquals(
            12_000L,
            resolveBangumiPlaybackStartPositionMs(
                routeResumePositionMs = 12_000L,
                savedEpisodePositionMs = 45_000L
            )
        )
    }

    @Test
    fun `course entry prefers aid then explicit episode then remembered progress`() {
        val detail = BangumiDetail(
            seasonId = 9L,
            episodes = listOf(
                BangumiEpisode(id = 11L, aid = 101L),
                BangumiEpisode(id = 12L, aid = 102L)
            ),
            userStatus = com.android.purebilibili.data.model.response.UserStatus(
                progress = com.android.purebilibili.data.model.response.WatchProgress(lastEpId = 12L)
            )
        )

        assertEquals(12L, resolveBangumiInitialEpisode(detail, 102L, 11L, true)?.epId)
        assertEquals(11L, resolveBangumiInitialEpisode(detail, 0L, 11L, true)?.epId)
        assertEquals(12L, resolveBangumiInitialEpisode(detail, 0L, 0L, true)?.epId)
    }

    @Test
    fun `reattached player restores cached dash playback for the same episode`() {
        assertTrue(
            shouldRestoreBangumiCachedPlayback(
                requestedSeasonId = 1L,
                requestedEpisodeId = 2L,
                loadedSeasonId = 1L,
                loadedEpisodeId = 2L,
                cachedVideoUrl = "https://video",
                cachedAudioUrl = "https://audio",
                attachedPlayerMediaItemCount = 0
            )
        )
    }

    @Test
    fun `cached bangumi durl does not bypass a reload without its segment list`() {
        assertFalse(
            shouldRestoreBangumiCachedPlayback(
                requestedSeasonId = 1L,
                requestedEpisodeId = 2L,
                loadedSeasonId = 1L,
                loadedEpisodeId = 2L,
                cachedVideoUrl = "https://video",
                cachedAudioUrl = null,
                attachedPlayerMediaItemCount = 0
            )
        )
    }
}
