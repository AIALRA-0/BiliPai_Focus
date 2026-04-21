package com.android.purebilibili.feature.download

internal data class DownloadCleanupTargets(
    val filePaths: Set<String>,
    val taskDirectoryPath: String
)

internal fun resolveDownloadCleanupTargets(
    taskId: String,
    task: DownloadTask,
    taskDirectoryPath: String
): DownloadCleanupTargets {
    val directoryPath = taskDirectoryPath.trimEnd('/', '\\')
    val extension = if (task.isAudioOnly) "m4a" else "mp4"
    val filePaths = buildSet {
        add("$directoryPath/${taskId}_video.m4s")
        add("$directoryPath/${taskId}_audio.m4s")
        add("$directoryPath/${taskId}.$extension")
        add("$directoryPath/${taskId}_cover.jpg")
        task.filePath?.takeIf { it.isNotBlank() }?.let(::add)
        task.localCoverPath?.takeIf { it.isNotBlank() }?.let(::add)
    }
    return DownloadCleanupTargets(
        filePaths = filePaths,
        taskDirectoryPath = directoryPath
    )
}
