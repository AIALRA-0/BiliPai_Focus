package com.android.purebilibili.core.network.socket

import kotlin.test.Test
import kotlin.test.assertEquals

class LiveDanmakuConnectionHealthPolicyTest {

    @Test
    fun `heartbeat reply refreshes server frame health`() {
        val health = markLiveDanmakuHeartbeatReply(
            health = markLiveDanmakuConnected(LiveDanmakuConnectionHealth(), nowMs = 1_000L),
            nowMs = 31_000L
        )

        assertEquals(31_000L, health.lastServerFrameAtMs)
        assertEquals(31_000L, health.lastHeartbeatReplyAtMs)
    }

    @Test
    fun `business message refreshes server frame health`() {
        val health = markLiveDanmakuBusinessMessage(
            health = markLiveDanmakuConnected(LiveDanmakuConnectionHealth(), nowMs = 1_000L),
            nowMs = 45_000L
        )

        assertEquals(45_000L, health.lastServerFrameAtMs)
        assertEquals(45_000L, health.lastBusinessMessageAtMs)
    }

    @Test
    fun `silent connection requests reconnect after timeout`() {
        val health = markLiveDanmakuConnected(LiveDanmakuConnectionHealth(), nowMs = 1_000L)

        val action = resolveLiveDanmakuHealthAction(
            health = health,
            nowMs = 77_001L,
            silenceTimeoutMs = 75_000L
        )

        assertEquals(LiveDanmakuHealthAction.RECONNECT, action)
    }

    @Test
    fun `active connection stays connected before timeout`() {
        val health = markLiveDanmakuHeartbeatReply(
            health = markLiveDanmakuConnected(LiveDanmakuConnectionHealth(), nowMs = 1_000L),
            nowMs = 60_000L
        )

        val action = resolveLiveDanmakuHealthAction(
            health = health,
            nowMs = 120_000L,
            silenceTimeoutMs = 75_000L
        )

        assertEquals(LiveDanmakuHealthAction.KEEP_ALIVE, action)
    }

    @Test
    fun `heartbeat only connection reconnects after business message stalls`() {
        val health = markLiveDanmakuHeartbeatReply(
            health = markLiveDanmakuBusinessMessage(
                health = markLiveDanmakuConnected(LiveDanmakuConnectionHealth(), nowMs = 1_000L),
                nowMs = 10_000L
            ),
            nowMs = 80_000L
        )

        val action = resolveLiveDanmakuHealthAction(
            health = health,
            nowMs = 90_001L,
            silenceTimeoutMs = 75_000L
        )

        assertEquals(LiveDanmakuHealthAction.RECONNECT, action)
    }

    @Test
    fun `heartbeat only connection stays connected before first business message`() {
        val health = markLiveDanmakuHeartbeatReply(
            health = markLiveDanmakuConnected(LiveDanmakuConnectionHealth(), nowMs = 1_000L),
            nowMs = 80_000L
        )

        val action = resolveLiveDanmakuHealthAction(
            health = health,
            nowMs = 140_000L,
            silenceTimeoutMs = 75_000L
        )

        assertEquals(LiveDanmakuHealthAction.KEEP_ALIVE, action)
    }

    @Test
    fun `manual disconnect suppresses silent reconnect`() {
        val health = markLiveDanmakuDisconnectedByUser(
            markLiveDanmakuConnected(LiveDanmakuConnectionHealth(), nowMs = 1_000L)
        )

        val action = resolveLiveDanmakuHealthAction(
            health = health,
            nowMs = 120_000L,
            silenceTimeoutMs = 75_000L
        )

        assertEquals(LiveDanmakuHealthAction.KEEP_ALIVE, action)
    }
}
