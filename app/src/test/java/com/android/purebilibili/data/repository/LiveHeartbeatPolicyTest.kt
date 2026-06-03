package com.android.purebilibili.data.repository

import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals

class LiveHeartbeatPolicyTest {

    @Test
    fun `heartbeat query uses web base64 payload`() {
        val query = buildLiveHeartbeatQuery(
            roomId = 26863308,
            lastIntervalSec = 60
        )
        val decoded = String(Base64.getDecoder().decode(query["hb"]), Charsets.UTF_8)

        assertEquals("web", query["pf"])
        assertEquals("60|26863308|1|0", decoded)
    }

    @Test
    fun `heartbeat response interval falls back to default when missing or invalid`() {
        assertEquals(45, parseLiveHeartbeatNextInterval("""{"code":0,"data":{"next_interval":45}}"""))
        assertEquals(60, parseLiveHeartbeatNextInterval("""{"code":0,"data":{}}"""))
        assertEquals(60, parseLiveHeartbeatNextInterval("""{"code":-400,"message":"bad"}}"""))
    }
}
