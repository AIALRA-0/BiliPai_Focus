package com.android.purebilibili.feature.download

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OfflineVideoRoutingPolicyTest {

    @Test
    fun noNetwork_prefersExactCachedTask() {
        val older = completedTask(taskIdSuffix = "old", cid = 22L, createdAt = 10L)
        val exact = completedTask(taskIdSuffix = "exact", cid = 33L, createdAt = 20L)

        val resolved = resolveOfflineVideoNavigationTask(
            tasks = listOf(older, exact),
            bvid = "BV1cached",
            cid = 33L,
            isNetworkAvailable = false
        )

        assertEquals(exact.id, resolved?.id)
    }

    @Test
    fun noNetwork_withoutCidFallsBackToNewestCachedTaskForSameVideo() {
        val older = completedTask(taskIdSuffix = "old", cid = 22L, createdAt = 10L)
        val newest = completedTask(taskIdSuffix = "new", cid = 44L, createdAt = 99L)

        val resolved = resolveOfflineVideoNavigationTask(
            tasks = listOf(older, newest),
            bvid = "BV1cached",
            cid = 0L,
            isNetworkAvailable = false
        )

        assertEquals(newest.id, resolved?.id)
    }

    @Test
    fun onlineNavigation_doesNotForceOfflineFallback() {
        val resolved = resolveOfflineVideoNavigationTask(
            tasks = listOf(completedTask(taskIdSuffix = "cached", cid = 33L, createdAt = 20L)),
            bvid = "BV1cached",
            cid = 33L,
            isNetworkAvailable = true
        )

        assertNull(resolved)
    }

    private fun completedTask(
        taskIdSuffix: String,
        cid: Long,
        createdAt: Long
    ): DownloadTask {
        val file = File.createTempFile(taskIdSuffix, ".mp4").apply { deleteOnExit() }
        return DownloadTask(
            bvid = "BV1cached",
            cid = cid,
            title = "离线视频",
            cover = "cover",
            ownerName = "UP",
            ownerFace = "",
            duration = 120,
            quality = 80,
            qualityDesc = "1080P",
            videoUrl = "",
            audioUrl = "",
            status = DownloadStatus.COMPLETED,
            progress = 1f,
            filePath = file.absolutePath,
            createdAt = createdAt
        )
    }
}
