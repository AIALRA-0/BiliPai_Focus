package com.android.purebilibili.core.network.socket

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DanmakuProtocolLimitsTest {

    @Test
    fun `empty and oversized websocket frames are rejected before copying`() {
        assertFalse(shouldAcceptLiveDanmakuFrame(0))
        assertFalse(shouldAcceptLiveDanmakuFrame(MAX_LIVE_DANMAKU_FRAME_BYTES + 1))
    }

    @Test
    fun `bounded websocket frame is accepted`() {
        assertTrue(shouldAcceptLiveDanmakuFrame(MAX_LIVE_DANMAKU_FRAME_BYTES))
    }
}
