package com.android.purebilibili.feature.video.ui.components

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
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

    @Test
    fun seekPreviewDestinationRect_preservesLandscapeSourceAspectRatio() {
        assertEquals(
            SeekPreviewDestinationRect(
                offset = IntOffset(14, 0),
                size = IntSize(213, 120)
            ),
            resolveSeekPreviewDestinationRect(
                sourceWidthPx = 160,
                sourceHeightPx = 90,
                containerWidthPx = 240,
                containerHeightPx = 120
            )
        )
    }

    @Test
    fun seekPreviewDestinationRect_centersPortraitSourceWithoutStretching() {
        assertEquals(
            SeekPreviewDestinationRect(
                offset = IntOffset(64, 0),
                size = IntSize(60, 106)
            ),
            resolveSeekPreviewDestinationRect(
                sourceWidthPx = 90,
                sourceHeightPx = 160,
                containerWidthPx = 188,
                containerHeightPx = 106
            )
        )
    }
}
