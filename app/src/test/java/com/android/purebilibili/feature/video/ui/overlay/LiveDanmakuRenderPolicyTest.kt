package com.android.purebilibili.feature.video.ui.overlay

import kotlin.test.Test
import kotlin.test.assertEquals

class LiveDanmakuRenderPolicyTest {

    @Test
    fun `data refresh restarts live danmaku engine before invalidating view`() {
        val calls = mutableListOf<String>()

        executeLiveDanmakuDataRefresh(
            pause = { calls += "pause" },
            setData = { calls += "setData" },
            start = { calls += "start" },
            invalidateView = { calls += "invalidateView" }
        )

        assertEquals(
            listOf("pause", "setData", "start", "invalidateView"),
            calls
        )
    }
}
