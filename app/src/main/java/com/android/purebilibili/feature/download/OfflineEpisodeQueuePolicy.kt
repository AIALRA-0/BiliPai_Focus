package com.android.purebilibili.feature.download

import java.io.File

private val PAGE_LABEL_REGEX = Regex("""^\s*[Pp](\d+)\b""")

internal fun resolveOfflineEpisodeQueue(
    tasks: Collection<DownloadTask>,
    currentTask: DownloadTask
): List<DownloadTask> {
    val currentGroupKey = currentTask.groupKey?.trim().orEmpty()
    return tasks
        .filter { candidate ->
            candidate.isComplete &&
                candidate.isAudioOnly == currentTask.isAudioOnly &&
                !candidate.filePath.isNullOrBlank() &&
                File(candidate.filePath!!).exists() &&
                when {
                    currentGroupKey.isNotBlank() -> candidate.groupKey == currentGroupKey
                    else -> candidate.bvid == currentTask.bvid
                }
        }
        .sortedWith(
            compareBy<DownloadTask>(
                { resolveOfflineEpisodeSortIndex(it) },
                { it.createdAt },
                { it.cid }
            )
        )
}

internal fun resolveOfflineEpisodeSortIndex(task: DownloadTask): Int {
    if (task.episodeSortIndex > 0) return task.episodeSortIndex
    val fromLabel = PAGE_LABEL_REGEX.find(task.episodeLabel.orEmpty())
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
    if (fromLabel != null && fromLabel > 0) return fromLabel
    return Int.MAX_VALUE
}
