package com.android.purebilibili.feature.download

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DownloadPersistencePolicyTest {

    @Test
    fun normalizeRestoredDownloadTask_requeuesInterruptedActiveStates() {
        assertEquals(
            DownloadStatus.QUEUED,
            normalizeRestoredDownloadTask(baseTask.copy(status = DownloadStatus.QUEUED)).status
        )
        assertEquals(
            DownloadStatus.QUEUED,
            normalizeRestoredDownloadTask(baseTask.copy(status = DownloadStatus.PENDING)).status
        )
        assertEquals(
            DownloadStatus.QUEUED,
            normalizeRestoredDownloadTask(baseTask.copy(status = DownloadStatus.DOWNLOADING)).status
        )
        assertEquals(
            DownloadStatus.QUEUED,
            normalizeRestoredDownloadTask(baseTask.copy(status = DownloadStatus.MERGING)).status
        )
    }

    @Test
    fun normalizeRestoredDownloadTask_keepsStableAndUserPausedStatesUntouched() {
        val completedTask = baseTask.copy(status = DownloadStatus.COMPLETED)
        val pausedTask = baseTask.copy(status = DownloadStatus.PAUSED)

        assertEquals(completedTask, normalizeRestoredDownloadTask(completedTask))
        assertEquals(pausedTask, normalizeRestoredDownloadTask(pausedTask))
    }

    @Test
    fun shouldPersistDownloadTaskUpdate_ignoresProgressOnlyChanges() {
        val updated = baseTask.copy(progress = 0.5f, videoProgress = 0.4f, audioProgress = 0.6f)

        assertFalse(shouldPersistDownloadTaskUpdate(baseTask, updated))
    }

    @Test
    fun shouldPersistDownloadTaskUpdate_persistsStatusAndFileChanges() {
        assertTrue(
            shouldPersistDownloadTaskUpdate(
                baseTask,
                baseTask.copy(status = DownloadStatus.COMPLETED, filePath = "/tmp/video.mp4")
            )
        )
    }

    private val baseTask = DownloadTask(
        bvid = "BV1persist",
        cid = 9L,
        title = "缓存视频",
        cover = "cover",
        ownerName = "UP",
        ownerFace = "",
        duration = 120,
        quality = 80,
        qualityDesc = "1080P",
        videoUrl = "video",
        audioUrl = "audio"
    )
}
