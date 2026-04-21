package com.android.purebilibili.feature.download

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DownloadListNavigationPolicyTest {

    private fun completedTask(filePath: String? = null) = DownloadTask(
        bvid = "BV1offline",
        cid = 1001L,
        title = "Cached video",
        cover = "cover",
        ownerName = "UP",
        ownerFace = "face",
        duration = 120,
        quality = 80,
        qualityDesc = "1080P",
        videoUrl = "https://example.com/video.m4s",
        audioUrl = "https://example.com/audio.m4s",
        status = DownloadStatus.COMPLETED,
        progress = 1f,
        filePath = filePath
    )

    @Test
    fun completedDownload_prefersOfflinePlaybackEvenWhenNetworkIsAvailable() {
        val tempFile = File.createTempFile("download_nav", ".mp4").apply {
            writeText("cached")
            deleteOnExit()
        }
        val target = resolveDownloadTaskClickTarget(
            task = completedTask(filePath = tempFile.absolutePath),
            isNetworkAvailable = true
        )

        assertEquals(DownloadTaskClickTarget.OfflinePlayer, target)
    }

    @Test
    fun incompleteDownload_doesNotNavigate() {
        val target = resolveDownloadTaskClickTarget(
            task = completedTask().copy(status = DownloadStatus.DOWNLOADING, progress = 0.4f),
            isNetworkAvailable = true
        )

        assertNull(target)
    }
}
