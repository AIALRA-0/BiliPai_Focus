package com.android.purebilibili.feature.download

internal fun resolveDownloadTaskProgress(task: DownloadTask): Float {
    return sanitizeDownloadTask(task).progress
}

internal fun resolveDownloadTaskProgressPercent(task: DownloadTask): Int {
    return (resolveDownloadTaskProgress(task) * 100f).toInt()
}

internal fun sanitizeDownloadTask(task: DownloadTask): DownloadTask {
    val sanitizedVideoProgress = sanitizeDownloadProgressValue(task.videoProgress)
    val sanitizedAudioProgress = sanitizeDownloadProgressValue(task.audioProgress)
    val sanitizedOverallProgress = when {
        task.progress.isFinite() -> sanitizeDownloadProgressValue(task.progress)
        task.isAudioOnly -> sanitizedAudioProgress
        else -> sanitizeDownloadProgressValue((sanitizedVideoProgress + sanitizedAudioProgress) / 2f)
    }
    return task.copy(
        progress = sanitizedOverallProgress,
        videoProgress = sanitizedVideoProgress,
        audioProgress = sanitizedAudioProgress
    )
}

private fun sanitizeDownloadProgressValue(progress: Float): Float {
    return if (progress.isFinite()) {
        progress.coerceIn(0f, 1f)
    } else {
        0f
    }
}
