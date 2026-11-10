package com.android.purebilibili.feature.live

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiveDanmakuInteractionPolicyTest {
    @Test
    fun `room switch rejects an old selected message`() {
        assertEquals(123L, resolveLiveDanmakuReportRoomId(123L, 123L))
        assertNull(resolveLiveDanmakuReportRoomId(456L, 123L))
        assertNull(resolveLiveDanmakuReportRoomId(0L, 123L))
    }

    @Test
    fun `history without room metadata uses the room captured at submission`() {
        assertEquals(123L, resolveLiveDanmakuReportRoomId(123L, 0L))
        assertNull(resolveLiveDanmakuReportRoomId(0L, 0L))
    }
}
