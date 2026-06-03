package com.android.purebilibili.core.network.socket

internal const val LIVE_DANMAKU_SILENCE_TIMEOUT_MS = 75_000L
internal const val LIVE_DANMAKU_HEALTH_CHECK_INTERVAL_MS = 5_000L

internal enum class LiveDanmakuHealthAction {
    KEEP_ALIVE,
    RECONNECT
}

internal data class LiveDanmakuConnectionHealth(
    val connectedAtMs: Long = 0L,
    val lastServerFrameAtMs: Long = 0L,
    val lastHeartbeatReplyAtMs: Long = 0L,
    val lastBusinessMessageAtMs: Long = 0L,
    val disconnectedByUser: Boolean = false
)

internal fun markLiveDanmakuConnected(
    health: LiveDanmakuConnectionHealth,
    nowMs: Long
): LiveDanmakuConnectionHealth {
    return health.copy(
        connectedAtMs = nowMs,
        lastServerFrameAtMs = 0L,
        lastHeartbeatReplyAtMs = 0L,
        lastBusinessMessageAtMs = 0L,
        disconnectedByUser = false
    )
}

internal fun markLiveDanmakuServerFrameReceived(
    health: LiveDanmakuConnectionHealth,
    nowMs: Long
): LiveDanmakuConnectionHealth {
    return health.copy(
        lastServerFrameAtMs = nowMs,
        disconnectedByUser = false
    )
}

internal fun markLiveDanmakuHeartbeatReply(
    health: LiveDanmakuConnectionHealth,
    nowMs: Long
): LiveDanmakuConnectionHealth {
    return health.copy(
        lastServerFrameAtMs = nowMs,
        lastHeartbeatReplyAtMs = nowMs,
        disconnectedByUser = false
    )
}

internal fun markLiveDanmakuBusinessMessage(
    health: LiveDanmakuConnectionHealth,
    nowMs: Long
): LiveDanmakuConnectionHealth {
    return health.copy(
        lastServerFrameAtMs = nowMs,
        lastBusinessMessageAtMs = nowMs,
        disconnectedByUser = false
    )
}

internal fun markLiveDanmakuDisconnectedByUser(
    health: LiveDanmakuConnectionHealth
): LiveDanmakuConnectionHealth {
    return health.copy(disconnectedByUser = true)
}

internal fun resolveLiveDanmakuHealthAction(
    health: LiveDanmakuConnectionHealth,
    nowMs: Long,
    silenceTimeoutMs: Long = LIVE_DANMAKU_SILENCE_TIMEOUT_MS
): LiveDanmakuHealthAction {
    if (health.disconnectedByUser || health.connectedAtMs <= 0L) {
        return LiveDanmakuHealthAction.KEEP_ALIVE
    }
    val lastServerFrameObservedAtMs = health.lastServerFrameAtMs.takeIf { it > 0L } ?: health.connectedAtMs
    if (nowMs - lastServerFrameObservedAtMs > silenceTimeoutMs) {
        return LiveDanmakuHealthAction.RECONNECT
    }

    val lastBusinessMessageAtMs = health.lastBusinessMessageAtMs
    if (lastBusinessMessageAtMs > 0L && nowMs - lastBusinessMessageAtMs > silenceTimeoutMs) {
        return LiveDanmakuHealthAction.RECONNECT
    }

    return LiveDanmakuHealthAction.KEEP_ALIVE
}
