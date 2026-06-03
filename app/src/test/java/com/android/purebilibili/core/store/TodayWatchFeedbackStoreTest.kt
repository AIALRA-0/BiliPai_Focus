package com.android.purebilibili.core.store

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TodayWatchFeedbackStoreTest {

    @Test
    fun dislikedVideoFeedback_keepsRecentVideoMetadataAndSignals() {
        val snapshot = TodayWatchFeedbackSnapshot().withDislikedVideoFeedback(
            video = TodayWatchDislikedVideoSnapshot(
                bvid = "BV1",
                title = "吵闹整活合集",
                creatorName = "UP-X",
                creatorMid = 42L,
                dislikedAtMillis = 1_700_010_000_000L
            ),
            keywords = setOf("吵闹", "整活")
        )

        assertEquals(setOf("BV1"), snapshot.dislikedBvids)
        assertEquals(setOf(42L), snapshot.dislikedCreatorMids)
        assertTrue(snapshot.dislikedKeywords.contains("吵闹"))
        assertTrue(snapshot.dislikedKeywords.contains("整活"))
        assertEquals("吵闹整活合集", snapshot.recentDislikedVideos.single().title)
        assertEquals("UP-X", snapshot.recentDislikedVideos.single().creatorName)
    }

    @Test
    fun dislikedVideoFeedback_movesRepeatedVideoToLatestPosition() {
        val first = TodayWatchFeedbackSnapshot().withDislikedVideoFeedback(
            video = TodayWatchDislikedVideoSnapshot(
                bvid = "BV1",
                title = "旧标题",
                creatorName = "UP-A",
                creatorMid = 1L,
                dislikedAtMillis = 100L
            ),
            keywords = emptySet()
        )
        val second = first.withDislikedVideoFeedback(
            video = TodayWatchDislikedVideoSnapshot(
                bvid = "BV2",
                title = "另一个视频",
                creatorName = "UP-B",
                creatorMid = 2L,
                dislikedAtMillis = 200L
            ),
            keywords = emptySet()
        )
        val repeated = second.withDislikedVideoFeedback(
            video = TodayWatchDislikedVideoSnapshot(
                bvid = "BV1",
                title = "新标题",
                creatorName = "UP-A",
                creatorMid = 1L,
                dislikedAtMillis = 300L
            ),
            keywords = emptySet()
        )

        assertEquals(listOf("BV2", "BV1"), repeated.recentDislikedVideos.map { it.bvid })
        assertEquals("新标题", repeated.recentDislikedVideos.last().title)
    }
}
