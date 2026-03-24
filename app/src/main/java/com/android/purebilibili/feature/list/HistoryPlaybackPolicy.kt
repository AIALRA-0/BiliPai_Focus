package com.android.purebilibili.feature.list

import com.android.purebilibili.data.model.response.HistoryItem

internal fun resolveHistoryPlaybackCid(
    clickedCid: Long,
    historyItem: HistoryItem?
): Long {
    val historyCid = historyItem?.cid ?: 0L
    return if (historyCid > 0L) historyCid else clickedCid.coerceAtLeast(0L)
}

internal fun resolveHistoryResumePositionMs(historyItem: HistoryItem?): Long {
    val progressSec = historyItem?.progress ?: 0
    return progressSec
        .takeIf { it > 0 }
        ?.times(1000L)
        ?: 0L
}

internal fun resolveHistoryDisplayProgress(
    serverProgressSec: Int,
    durationSec: Int,
    localPositionMs: Long
): Int {
    return resolveVideoDisplayProgressState(
        serverProgressSec = serverProgressSec,
        durationSec = durationSec,
        localPositionMs = localPositionMs,
        viewAt = 1L
    ).progressSec
}
