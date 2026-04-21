package com.android.purebilibili.feature.download

internal fun isDownloadTaskActive(task: DownloadTask): Boolean {
    return when (task.status) {
        DownloadStatus.PENDING,
        DownloadStatus.DOWNLOADING,
        DownloadStatus.MERGING -> true
        else -> false
    }
}

internal fun resolveNextQueuedDownloadTaskId(tasks: Collection<DownloadTask>): String? {
    if (tasks.any(::isDownloadTaskActive)) return null

    return tasks
        .asSequence()
        .filter { it.status == DownloadStatus.QUEUED }
        .sortedWith(compareBy<DownloadTask> { it.createdAt }.thenBy { it.id })
        .map { it.id }
        .firstOrNull()
}
