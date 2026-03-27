package com.android.purebilibili.feature.video.ui.components

import com.android.purebilibili.data.model.response.VideoshotData
import kotlin.test.Test
import kotlin.test.assertEquals

class SeekPreviewBubblePolicyTest {

    @Test
    fun seekPreviewAnchor_quantizesToCurrentVideoshotFrameBoundary() {
        val videoshotData = VideoshotData(
            img_x_len = 2,
            img_y_len = 2,
            image = listOf("sprite-1"),
            index = listOf(0L, 1_000L, 2_000L, 3_000L)
        )

        assertEquals(
            2_000L,
            resolveSeekPreviewAnchorPositionMs(
                videoshotData = videoshotData,
                targetPositionMs = 2_850L,
                durationMs = 4_000L
            )
        )
    }

    @Test
    fun seekPreviewAnchor_keepsTargetPositionWhenVideoshotUnavailable() {
        assertEquals(
            2_850L,
            resolveSeekPreviewAnchorPositionMs(
                videoshotData = null,
                targetPositionMs = 2_850L,
                durationMs = 4_000L
            )
        )
    }

    @Test
    fun seekPreviewAnchor_estimatesFrameBoundaryWhenTimelineMissing() {
        val videoshotData = VideoshotData(
            img_x_len = 2,
            img_y_len = 2,
            image = listOf("sprite-1"),
            index = emptyList()
        )

        assertEquals(
            3_000L,
            resolveSeekPreviewAnchorPositionMs(
                videoshotData = videoshotData,
                targetPositionMs = 3_700L,
                durationMs = 4_000L
            )
        )
    }
}
