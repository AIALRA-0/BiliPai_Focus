package com.android.purebilibili.feature.video.usecase

import com.android.purebilibili.data.model.response.Page
import com.android.purebilibili.data.model.response.ViewInfo
import kotlin.test.Test
import kotlin.test.assertEquals

class VideoPlaybackUseCaseDurationPolicyTest {

    @Test
    fun `video load duration prefers playurl timelength`() {
        val info = ViewInfo(
            cid = 2L,
            pages = listOf(Page(cid = 2L, duration = 120L))
        )

        assertEquals(
            118_000L,
            resolveVideoLoadDurationMs(
                playUrlDurationMs = 118_000L,
                info = info
            )
        )
    }

    @Test
    fun `video load duration falls back to current page duration`() {
        val info = ViewInfo(
            cid = 2L,
            pages = listOf(
                Page(cid = 1L, duration = 60L),
                Page(cid = 2L, duration = 180L)
            )
        )

        assertEquals(
            180_000L,
            resolveVideoLoadDurationMs(
                playUrlDurationMs = 0L,
                info = info
            )
        )
    }

}
