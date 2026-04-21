package com.android.purebilibili.feature.download

import java.io.File

internal fun resolveOfflineVideoNavigationTask(
    tasks: Collection<DownloadTask>,
    bvid: String,
    cid: Long,
    isNetworkAvailable: Boolean
): DownloadTask? {
    if (isNetworkAvailable) return null

    val playableTasks = tasks
        .filter {
            it.isComplete &&
                !it.filePath.isNullOrBlank() &&
                File(it.filePath).exists() &&
                it.bvid == bvid
        }
        .sortedByDescending { it.createdAt }

    if (playableTasks.isEmpty()) return null
    if (cid > 0L) {
        playableTasks.firstOrNull { it.cid == cid }?.let { return it }
    }
    return playableTasks.firstOrNull()
}
