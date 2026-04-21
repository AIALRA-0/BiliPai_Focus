package com.android.purebilibili.feature.download

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DownloadQueuePolicyTest {

    @Test
    fun activeTask_blocksQueuedTaskDispatch() {
        val tasks = listOf(
            baseTask.copy(status = DownloadStatus.DOWNLOADING, createdAt = 1L),
            baseTask.copy(cid = 2L, status = DownloadStatus.QUEUED, createdAt = 2L)
        )

        assertNull(resolveNextQueuedDownloadTaskId(tasks))
    }

    @Test
    fun idleQueue_dispatchesOldestQueuedTask() {
        val older = baseTask.copy(cid = 2L, status = DownloadStatus.QUEUED, createdAt = 10L)
        val newer = baseTask.copy(cid = 3L, status = DownloadStatus.QUEUED, createdAt = 20L)

        assertEquals(older.id, resolveNextQueuedDownloadTaskId(listOf(newer, older)))
    }

    private val baseTask = DownloadTask(
        bvid = "BV1queue",
        cid = 1L,
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
