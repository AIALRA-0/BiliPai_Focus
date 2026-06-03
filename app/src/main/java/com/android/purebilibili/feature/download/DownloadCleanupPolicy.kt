package com.android.purebilibili.feature.download

import java.io.File

internal data class DownloadCleanupTargets(
    val filePaths: Set<String>,
    val taskDirectoryPath: String
)

internal fun resolveDownloadCleanupTargets(
    taskId: String,
    task: DownloadTask,
    taskDirectoryPath: String
): DownloadCleanupTargets {
    val directory = File(taskDirectoryPath)
    val stableDirectoryPath = taskDirectoryPath.trimEnd('/', '\\')
    val extension = if (task.isAudioOnly) "m4a" else "mp4"
    val filePaths = buildSet {
        add(stableDownloadCleanupPath(stableDirectoryPath, "${taskId}_video.m4s"))
        add(stableDownloadCleanupPath(stableDirectoryPath, "${taskId}_audio.m4s"))
        add(stableDownloadCleanupPath(stableDirectoryPath, "${taskId}.$extension"))
        add(stableDownloadCleanupPath(stableDirectoryPath, "${taskId}_cover.jpg"))
        task.filePath?.takeIf { it.isNotBlank() }?.let(::add)
        task.localCoverPath?.takeIf { it.isNotBlank() }?.let(::add)
        task.localDanmakuMetadataPath?.takeIf { it.isNotBlank() }?.let(::add)
        task.localDanmakuSegmentPaths.filter { it.isNotBlank() }.forEach(::add)
        task.assets.mapNotNull { it.filePath?.takeIf(String::isNotBlank) }.forEach(::add)
        task.assets.mapNotNull { it.tempPath?.takeIf(String::isNotBlank) }.forEach(::add)
        directory.listFiles { file ->
            file.name.startsWith("${taskId}_") &&
                (file.name.endsWith(".part") || file.name.contains(".part.chunk"))
        }?.forEach { add(it.absolutePath) }
    }
    return DownloadCleanupTargets(
        filePaths = filePaths,
        taskDirectoryPath = stableDirectoryPath
    )
}

private fun stableDownloadCleanupPath(directoryPath: String, fileName: String): String {
    if (directoryPath.isBlank()) return fileName
    return "$directoryPath/$fileName"
}
