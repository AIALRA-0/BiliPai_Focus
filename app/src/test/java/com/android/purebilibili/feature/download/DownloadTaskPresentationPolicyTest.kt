package com.android.purebilibili.feature.download

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DownloadTaskPresentationPolicyTest {

    @Test
    fun multiEpisodeTask_exposesEpisodeLabelAsSecondaryText() {
        val task = baseTask.copy(
            title = "系列课程",
            episodeLabel = "P2 第二集"
        )

        assertEquals("P2 第二集", resolveDownloadTaskSecondaryText(task))
    }

    @Test
    fun identicalEpisodeLabel_doesNotDuplicateTitle() {
        val task = baseTask.copy(
            title = "系列课程",
            episodeLabel = "系列课程"
        )

        assertNull(resolveDownloadTaskSecondaryText(task))
    }

    private val baseTask = DownloadTask(
        bvid = "BV1episode",
        cid = 100L,
        title = "缓存视频",
        cover = "cover",
        ownerName = "UP",
        ownerFace = "",
        duration = 120,
        quality = 80,
        qualityDesc = "1080P",
        videoUrl = "",
        audioUrl = ""
    )
}
