package com.android.purebilibili.feature.download

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DownloadCleanupPolicyTest {

    @Test
    fun cleanupTargets_includeOutputCoverAndTempArtifacts() {
        val directory = File("/tmp/downloads")
        val task = DownloadTask(
            bvid = "BV1cleanup",
            cid = 7L,
            title = "缓存视频",
            cover = "cover",
            ownerName = "UP",
            ownerFace = "",
            duration = 120,
            quality = 80,
            qualityDesc = "1080P",
            videoUrl = "",
            audioUrl = "",
            status = DownloadStatus.COMPLETED,
            filePath = "/tmp/downloads/BV1cleanup_7_80.mp4",
            localCoverPath = "/tmp/downloads/BV1cleanup_7_80_cover.jpg"
        )

        val targets = resolveDownloadCleanupTargets(
            taskId = task.id,
            task = task,
            taskDirectoryPath = directory.path
        )

        assertEquals(directory.absolutePath, targets.taskDirectoryPath)
        assertTrue(File(directory, "${task.id}_video.m4s").absolutePath in targets.filePaths)
        assertTrue(File(directory, "${task.id}_audio.m4s").absolutePath in targets.filePaths)
        assertTrue(File(directory, "${task.id}.mp4").absolutePath in targets.filePaths)
        assertTrue(File(directory, "${task.id}_cover.jpg").absolutePath in targets.filePaths)
        assertTrue("/tmp/downloads/BV1cleanup_7_80.mp4" in targets.filePaths)
        assertTrue("/tmp/downloads/BV1cleanup_7_80_cover.jpg" in targets.filePaths)
    }
}
