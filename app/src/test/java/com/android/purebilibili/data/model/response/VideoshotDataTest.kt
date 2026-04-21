package com.android.purebilibili.data.model.response

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VideoshotDataTest {

    @Test
    fun isValid_allowsSpritePreviewWithoutTimelineIndex() {
        val data = VideoshotData(
            img_x_len = 2,
            img_y_len = 2,
            img_x_size = 160,
            img_y_size = 90,
            image = listOf("sprite-1"),
            index = emptyList()
        )

        assertTrue(data.isValid)
    }

    @Test
    fun getPreviewInfo_estimatesFrameWhenTimelineMissing() {
        val data = VideoshotData(
            img_x_len = 2,
            img_y_len = 2,
            img_x_size = 160,
            img_y_size = 90,
            image = listOf("sprite-1"),
            index = emptyList()
        )

        val preview = data.getPreviewInfo(positionMs = 3_700L, durationMs = 4_000L)

        assertNotNull(preview)
        assertEquals("sprite-1", preview.first)
        assertEquals(160, preview.second)
        assertEquals(90, preview.third)
    }

    @Test
    fun resolveTimelineMs_convertsSecondTimelineWhenDurationIndicatesSecondUnit() {
        val data = VideoshotData(
            img_x_len = 2,
            img_y_len = 2,
            img_x_size = 160,
            img_y_size = 90,
            image = listOf("sprite-1"),
            index = listOf(0L, 1L, 2L, 3L)
        )

        assertEquals(listOf(0L, 1_000L, 2_000L, 3_000L), data.resolveTimelineMs(durationMs = 4_000L))
    }

    @Test
    fun resolvePreviewAnchorPositionMs_prefersMatchedTimelinePoint() {
        val data = VideoshotData(
            img_x_len = 2,
            img_y_len = 2,
            img_x_size = 160,
            img_y_size = 90,
            image = listOf("sprite-1"),
            index = listOf(0L, 1L, 2L, 3L)
        )

        assertEquals(
            2_000L,
            data.resolvePreviewAnchorPositionMs(
                positionMs = 2_450L,
                durationMs = 4_000L
            )
        )
    }
}
