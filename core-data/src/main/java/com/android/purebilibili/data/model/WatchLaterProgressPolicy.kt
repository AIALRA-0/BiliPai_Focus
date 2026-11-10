package com.android.purebilibili.data.model

import com.android.purebilibili.data.model.response.VideoItem

/** Original mobile Watch Later completion rule; unlike history, it uses full duration. */
fun isWatchLaterViewed(item: VideoItem): Boolean =
    item.duration > 0 && item.progress >= item.duration

/** Extracted from the mobile cover overlay. A missing/default -1 is not completion. */
fun resolveWatchLaterDisplayProgressState(item: VideoItem): VideoProgressDisplayState =
    VideoProgressDisplayState(
        progressSec = if (isWatchLaterViewed(item)) -1 else item.progress.coerceAtLeast(0),
        progressFraction = if (item.duration > 0) {
            (item.progress.toFloat() / item.duration).coerceIn(0f, 1f)
        } else {
            0f
        },
        showProgressBar = item.duration > 0 && item.progress > 0,
    )
