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

    private fun readyState(
        bvid: String,
        cid: Long,
        aid: Long
    ): VideoPlaybackUiState.Success = VideoPlaybackUiState.Success(
        info = ViewInfo(bvid = bvid, cid = cid, aid = aid),
        playUrl = "https://example.test/video"
    )
}
