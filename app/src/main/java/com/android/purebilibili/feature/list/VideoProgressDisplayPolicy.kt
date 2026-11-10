package com.android.purebilibili.feature.list

import com.android.purebilibili.data.model.resolveVideoDisplayProgressState as resolveSharedVideoDisplayProgressState

// Keep the mobile source API while both clients consume the original display policy.
typealias VideoProgressDisplayState = com.android.purebilibili.data.model.VideoProgressDisplayState

internal fun resolveVideoDisplayProgressState(
    serverProgressSec: Int,
    durationSec: Int,
    localPositionMs: Long = 0L,
    viewAt: Long = 0L
): VideoProgressDisplayState = resolveSharedVideoDisplayProgressState(
    serverProgressSec = serverProgressSec,
    durationSec = durationSec,
    localPositionMs = localPositionMs,
    viewAt = viewAt
)
