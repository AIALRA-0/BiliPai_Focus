package com.android.purebilibili.feature.home.components.cards

import androidx.compose.ui.graphics.Color
import com.android.purebilibili.feature.list.VideoProgressDisplayState
import com.android.purebilibili.feature.list.resolveVideoDisplayProgressState

internal fun resolveVideoCardHistoryProgressState(
    viewAt: Long,
    durationSec: Int,
    progressSec: Int,
    localPositionMs: Long = 0L
): VideoProgressDisplayState {
    return resolveVideoDisplayProgressState(
        serverProgressSec = progressSec,
        durationSec = durationSec,
        localPositionMs = localPositionMs,
        viewAt = viewAt
    )
}

internal fun shouldShowVideoCardHistoryProgressBar(
    viewAt: Long,
    durationSec: Int,
    progressSec: Int,
    localPositionMs: Long = 0L
): Boolean {
    return resolveVideoCardHistoryProgressState(
        viewAt = viewAt,
        durationSec = durationSec,
        progressSec = progressSec,
        localPositionMs = localPositionMs
    ).showProgressBar
}

internal fun resolveVideoCardHistoryProgressFraction(
    progressSec: Int,
    durationSec: Int
): Float {
    return resolveVideoCardHistoryProgressState(
        viewAt = 1L,
        durationSec = durationSec,
        progressSec = progressSec
    ).progressFraction
}

internal fun resolveVideoCardHistoryProgressBarColor(
    themePrimary: Color
): Color = themePrimary
