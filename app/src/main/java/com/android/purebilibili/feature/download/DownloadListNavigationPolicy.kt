package com.android.purebilibili.feature.download

import java.io.File

internal enum class DownloadTaskClickTarget {
    OfflinePlayer
}

internal fun resolveDownloadTaskClickTarget(
    task: DownloadTask,
    isNetworkAvailable: Boolean
): DownloadTaskClickTarget? {
    return if (isDownloadTaskPlayableOffline(task)) {
        DownloadTaskClickTarget.OfflinePlayer
    } else {
        null
    }
}

internal fun isDownloadTaskPlayableOffline(task: DownloadTask): Boolean {
    return task.isComplete &&
        !task.filePath.isNullOrBlank() &&
        File(task.filePath).exists()
}
