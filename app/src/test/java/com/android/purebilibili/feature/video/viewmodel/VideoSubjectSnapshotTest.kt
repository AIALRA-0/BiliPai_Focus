package com.android.purebilibili.feature.video.viewmodel

import com.android.purebilibili.data.model.response.ViewInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoSubjectSnapshotTest {

    @Test
    fun `same video subject keeps generation`() {
        val ready = readyState(bvid = "BV1", cid = 10L, aid = 20L)
        val previous = ready.toSubjectSnapshot(generation = 3L)

        assertFalse(shouldAdvanceVideoSubjectGeneration(previous, ready))
    }

    @Test
    fun `page or video change advances generation`() {
        val ready = readyState(bvid = "BV1", cid = 10L, aid = 20L)
        val previous = ready.toSubjectSnapshot(generation = 3L)

        assertTrue(
            shouldAdvanceVideoSubjectGeneration(
                previous,
                readyState(bvid = "BV1", cid = 11L, aid = 20L)
            )
        )
        assertTrue(
            shouldAdvanceVideoSubjectGeneration(
                previous,
                readyState(bvid = "BV2", cid = 10L, aid = 21L)
            )
        )
    }

    @Test
    fun `copyright passthrough marks repost and defaults to original`() {
        assertFalse(
            readyState(bvid = "BV1", copyright = 1).toSubjectSnapshot(generation = 1L).isRepost
        )
        assertTrue(
            readyState(bvid = "BV1", copyright = 2).toSubjectSnapshot(generation = 1L).isRepost
        )
        // 历史遗留脏数据 0 与缺失字段都按原创处理
        assertFalse(
            readyState(bvid = "BV1", copyright = 0).toSubjectSnapshot(generation = 1L).isRepost
        )
        assertFalse(
            VideoPlaybackUiState.Success(
                info = ViewInfo(bvid = "BV1"),
                playUrl = "https://example.test/video"
            ).toEngagementSeed().isRepost
        )
        assertTrue(
            readyState(bvid = "BV1", copyright = 2).toEngagementSeed().isRepost
        )
    }

    private fun readyState(
        bvid: String,
        cid: Long = 10L,
        aid: Long = 20L,
        copyright: Int = 1
    ): VideoPlaybackUiState.Success = VideoPlaybackUiState.Success(
        info = ViewInfo(bvid = bvid, cid = cid, aid = aid, copyright = copyright),
        playUrl = "https://example.test/video"
    )
}
