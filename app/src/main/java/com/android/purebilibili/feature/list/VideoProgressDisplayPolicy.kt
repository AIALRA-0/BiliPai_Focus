package com.android.purebilibili.feature.list

private const val COMPLETED_PROGRESS_THRESHOLD = 0.95f

data class VideoProgressDisplayState(
    val progressSec: Int,
    val progressFraction: Float,
    val showProgressBar: Boolean
)

internal fun resolveVideoDisplayProgressState(
    serverProgressSec: Int,
    durationSec: Int,
    localPositionMs: Long = 0L,
    viewAt: Long = 0L
): VideoProgressDisplayState {
    val normalizedServer = normalizeServerProgressSec(
        progressSec = serverProgressSec,
        durationSec = durationSec,
        viewAt = viewAt
    )
    val normalizedLocal = normalizeLocalProgressSec(
        localPositionMs = localPositionMs,
        durationSec = durationSec
    )

    val resolvedProgress = when {
        normalizedServer == -1 || normalizedLocal == -1 -> -1
        else -> maxOf(
            normalizedServer.coerceAtLeast(0),
            normalizedLocal.coerceAtLeast(0)
        )
    }

    val progressFraction = when {
        durationSec <= 0 -> 0f
        resolvedProgress == -1 -> 1f
        resolvedProgress <= 0 -> 0f
        else -> (resolvedProgress.toFloat() / durationSec.toFloat()).coerceIn(0f, 1f)
    }

    val showProgressBar = durationSec > 0 &&
        (resolvedProgress == -1 || resolvedProgress > 0)

    return VideoProgressDisplayState(
        progressSec = resolvedProgress,
        progressFraction = progressFraction,
        showProgressBar = showProgressBar
    )
}

private fun normalizeServerProgressSec(
    progressSec: Int,
    durationSec: Int,
    viewAt: Long
): Int {
    // `VideoItem.progress` defaults to -1 for many feed items that have no history metadata.
    // Only treat -1 as "completed" when the item actually carries a playback record.
    if (progressSec == -1) return if (viewAt > 0L) -1 else 0
    if (durationSec <= 0) return progressSec.coerceAtLeast(0)
    if (progressSec <= 0) return progressSec.coerceAtLeast(0)

    val clamped = progressSec.coerceAtMost(durationSec)
    return if (isCompletedProgress(clamped, durationSec)) -1 else clamped
}

private fun normalizeLocalProgressSec(
    localPositionMs: Long,
    durationSec: Int
): Int {
    if (durationSec <= 0) return 0
    val localSec = (localPositionMs / 1000L).toInt()
    if (localSec <= 0) return 0

    val clamped = localSec.coerceAtMost(durationSec)
    return if (isCompletedProgress(clamped, durationSec)) -1 else clamped
}

private fun isCompletedProgress(
    progressSec: Int,
    durationSec: Int
): Boolean {
    if (durationSec <= 0) return false
    return progressSec >= (durationSec * COMPLETED_PROGRESS_THRESHOLD).toInt().coerceAtLeast(1)
}
