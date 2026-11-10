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

    @Test
    fun missingLocalFile_fallsBackToOnlineWhenNetworkAvailable() {
        val target = resolveDownloadTaskClickTarget(
            task = completedTask(filePath = null),
            isNetworkAvailable = true
        )

        assertEquals(DownloadTaskClickTarget.OnlinePlayer, target)
    }

    @Test
    fun missingLocalFile_withoutNetwork_staysPut() {
        val target = resolveDownloadTaskClickTarget(
            task = completedTask(filePath = null),
            isNetworkAvailable = false
        )

        assertNull(target)
    }

    @Test
    fun batchOperationFilters_coverTheRightTasks() {
        val downloading = completedTask().copy(cid = 1L, status = DownloadStatus.DOWNLOADING)
        val queued = completedTask().copy(cid = 2L, status = DownloadStatus.QUEUED)
        val merging = completedTask().copy(cid = 3L, status = DownloadStatus.MERGING)
        val paused = completedTask().copy(cid = 4L, status = DownloadStatus.PAUSED)
        val failed = completedTask().copy(cid = 5L, status = DownloadStatus.FAILED)
        val done = completedTask().copy(cid = 6L, status = DownloadStatus.COMPLETED)

        val tasks = listOf(downloading, queued, merging, paused, failed, done)

        // 暂停全部：排队中/下载中/合成中；继续全部：已暂停/已失败
        assertEquals(
            setOf(downloading.id, queued.id, merging.id),
            tasks.filter(::shouldPauseAllInclude).map { it.id }.toSet()
        )
        assertEquals(
            setOf(paused.id, failed.id),
            tasks.filter(::shouldContinueAllInclude).map { it.id }.toSet()
        )
    }
}
