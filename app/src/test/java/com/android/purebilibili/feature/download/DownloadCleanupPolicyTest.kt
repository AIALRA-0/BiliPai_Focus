package com.android.purebilibili.feature.download

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DownloadCleanupPolicyTest {

    @Test
    fun cleanupTargets_includeOutputCoverAndTempArtifacts() {
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
            taskDirectoryPath = "/tmp/downloads"
        )

        assertEquals("/tmp/downloads", targets.taskDirectoryPath)
        assertTrue("/tmp/downloads/${task.id}_video.m4s" in targets.filePaths)
        assertTrue("/tmp/downloads/${task.id}_audio.m4s" in targets.filePaths)
        assertTrue("/tmp/downloads/${task.id}.mp4" in targets.filePaths)
        assertTrue("/tmp/downloads/${task.id}_cover.jpg" in targets.filePaths)
        assertTrue("/tmp/downloads/BV1cleanup_7_80.mp4" in targets.filePaths)
        assertTrue("/tmp/downloads/BV1cleanup_7_80_cover.jpg" in targets.filePaths)
    }
}
