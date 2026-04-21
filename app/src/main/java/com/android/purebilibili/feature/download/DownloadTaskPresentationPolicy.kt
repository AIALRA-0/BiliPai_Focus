package com.android.purebilibili.feature.download

internal fun resolveDownloadTaskSecondaryText(task: DownloadTask): String? {
    val label = task.episodeLabel?.trim().orEmpty()
    if (label.isBlank()) return null
    return label.takeUnless { it == task.title.trim() }
}
