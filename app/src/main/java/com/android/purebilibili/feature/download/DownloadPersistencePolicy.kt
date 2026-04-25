package com.android.purebilibili.feature.download

internal fun normalizeRestoredDownloadTask(task: DownloadTask): DownloadTask {
    val normalizedStatusTask = when (task.status) {
        DownloadStatus.QUEUED,
        DownloadStatus.PENDING,
        DownloadStatus.DOWNLOADING,
        DownloadStatus.MERGING -> task.copy(status = DownloadStatus.QUEUED)
        else -> task
    }
    return sanitizeDownloadTask(normalizedStatusTask)
}

internal fun shouldPersistDownloadTaskUpdate(
    previous: DownloadTask,
    updated: DownloadTask
): Boolean {
    return previous.status != updated.status ||
        previous.filePath != updated.filePath ||
        previous.fileSize != updated.fileSize ||
        previous.downloadedSize != updated.downloadedSize ||
        previous.errorMessage != updated.errorMessage ||
        previous.localCoverPath != updated.localCoverPath ||
        previous.customSaveDir != updated.customSaveDir ||
        previous.exportedFileUri != updated.exportedFileUri ||
        previous.lastPlaybackPositionMs != updated.lastPlaybackPositionMs
}
