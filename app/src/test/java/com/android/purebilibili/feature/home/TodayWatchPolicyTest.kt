package com.android.purebilibili.feature.home

import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.Stat
import com.android.purebilibili.data.model.response.VideoItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TodayWatchPolicyTest {

    @Test
    fun `plan prefers creators user watched more`() {
        val history = listOf(
            VideoItem(bvid = "h1", owner = Owner(mid = 1, name = "UP-A"), duration = 600, progress = 500, view_at = 1_700_000_000),
            VideoItem(bvid = "h2", owner = Owner(mid = 1, name = "UP-A"), duration = 700, progress = 600, view_at = 1_700_001_000),
            VideoItem(bvid = "h3", owner = Owner(mid = 2, name = "UP-B"), duration = 800, progress = 100, view_at = 1_700_002_000)
        )
        val candidates = listOf(
            VideoItem(bvid = "c1", owner = Owner(mid = 1, name = "UP-A"), duration = 480, stat = Stat(view = 1000, danmaku = 10), title = "A 视频"),
            VideoItem(bvid = "c2", owner = Owner(mid = 2, name = "UP-B"), duration = 480, stat = Stat(view = 1000, danmaku = 10), title = "B 视频")
        )

        val plan = buildTodayWatchPlan(
            historyVideos = history,
            candidateVideos = candidates,
            mode = TodayWatchMode.RELAX,
            eyeCareNightActive = false,
            nowEpochSec = 1_700_010_000
        )

        assertEquals(1L, plan.upRanks.first().mid)
        assertEquals("c1", plan.videoQueue.first().bvid)
    }

    @Test
    fun `partial watch history does not outweigh completed watch history`() {
        val history = listOf(
            VideoItem(bvid = "half_watch", owner = Owner(mid = 1, name = "UP-Half"), duration = 1_200, progress = 600, view_at = 1_700_002_000),
            VideoItem(bvid = "complete_watch", owner = Owner(mid = 2, name = "UP-Done"), duration = 1_200, progress = 1_140, view_at = 1_699_400_000)
        )
        val candidates = listOf(
            VideoItem(bvid = "half_candidate", owner = Owner(mid = 1, name = "UP-Half"), duration = 600, stat = Stat(view = 5_000, danmaku = 40), title = "半播来源候选"),
            VideoItem(bvid = "complete_candidate", owner = Owner(mid = 2, name = "UP-Done"), duration = 600, stat = Stat(view = 5_000, danmaku = 40), title = "完播来源候选")
        )

        val plan = buildTodayWatchPlan(
            historyVideos = history,
            candidateVideos = candidates,
            mode = TodayWatchMode.RELAX,
            eyeCareNightActive = false,
            nowEpochSec = 1_700_010_000,
            queueLimit = 1
        )

        assertEquals("complete_candidate", plan.videoQueue.firstOrNull()?.bvid)
        assertEquals(2L, plan.upRanks.firstOrNull()?.mid)
    }

    @Test
    fun `up rank click only enables valid creator mid`() {
        assertTrue(
            shouldEnableTodayWatchUpRankClick(
                TodayUpRank(mid = 42L, name = "UP-A", score = 3.0, watchCount = 2)
            )
        )
        assertFalse(
            shouldEnableTodayWatchUpRankClick(
                TodayUpRank(mid = 0L, name = "UP-B", score = 3.0, watchCount = 2)
            )
        )
    }

    @Test
    fun `night signal pushes short and calm videos`() {
        val history = listOf(
            VideoItem(bvid = "h1", owner = Owner(mid = 1, name = "UP-A"), duration = 600, progress = 500, view_at = 1_700_000_000)
        )
        val candidates = listOf(
            VideoItem(
                bvid = "long_hot",
                owner = Owner(mid = 1, name = "UP-A"),
                duration = 3600,
                stat = Stat(view = 10_000, danmaku = 2_500),
                title = "高刺激长视频"
            ),
            VideoItem(
                bvid = "short_calm",
                owner = Owner(mid = 1, name = "UP-A"),
                duration = 420,
                stat = Stat(view = 10_000, danmaku = 30),
                title = "轻松短视频"
            )
        )

        val plan = buildTodayWatchPlan(
            historyVideos = history,
            candidateVideos = candidates,
            mode = TodayWatchMode.RELAX,
            eyeCareNightActive = true,
            nowEpochSec = 1_700_010_000
        )

        assertEquals("short_calm", plan.videoQueue.first().bvid)
    }

    @Test
    fun `learn mode promotes knowledge titles`() {
        val history = listOf(
            VideoItem(bvid = "h1", owner = Owner(mid = 8, name = "UP-K"), duration = 900, progress = 700, view_at = 1_700_000_000)
        )
        val candidates = listOf(
            VideoItem(
                bvid = "learn",
                owner = Owner(mid = 8, name = "UP-K"),
                duration = 1400,
                stat = Stat(view = 8000, danmaku = 80),
                title = "Kotlin 协程原理与实战教程"
            ),
            VideoItem(
                bvid = "fun",
                owner = Owner(mid = 8, name = "UP-K"),
                duration = 240,
                stat = Stat(view = 8000, danmaku = 80),
                title = "今日搞笑合集"
            )
        )

        val plan = buildTodayWatchPlan(
            historyVideos = history,
            candidateVideos = candidates,
            mode = TodayWatchMode.LEARN,
            eyeCareNightActive = false,
            nowEpochSec = 1_700_010_000
        )

        assertTrue(plan.videoQueue.take(1).any { it.bvid == "learn" })
    }

    @Test
    fun `relax mode resists hot study content when calm entertainment is available`() {
        val candidates = listOf(
            VideoItem(
                bvid = "study_hot",
                owner = Owner(mid = 1, name = "UP-S"),
                duration = 2_400,
                stat = Stat(view = 450_000, danmaku = 1_200),
                title = "Kotlin 协程原理深度教程"
            ),
            VideoItem(
                bvid = "relax_fit",
                owner = Owner(mid = 2, name = "UP-R"),
                duration = 520,
                stat = Stat(view = 16_000, danmaku = 30),
                title = "治愈旅行音乐日常"
            )
        )

        val plan = buildTodayWatchPlan(
            historyVideos = emptyList(),
            candidateVideos = candidates,
            mode = TodayWatchMode.RELAX,
            eyeCareNightActive = false,
            nowEpochSec = 1_700_010_000,
            queueLimit = 1
        )

        assertEquals("relax_fit", plan.videoQueue.firstOrNull()?.bvid)
    }

    @Test
    fun `learn mode resists hot casual content when focused study content is available`() {
        val candidates = listOf(
            VideoItem(
                bvid = "casual_hot",
                owner = Owner(mid = 1, name = "UP-C"),
                duration = 360,
                stat = Stat(view = 600_000, danmaku = 400),
                title = "今日搞笑游戏日常合集"
            ),
            VideoItem(
                bvid = "learn_fit",
                owner = Owner(mid = 2, name = "UP-L"),
                duration = 1_500,
                stat = Stat(view = 18_000, danmaku = 80),
                title = "Android 架构原理实战课程"
            )
        )

        val plan = buildTodayWatchPlan(
            historyVideos = emptyList(),
            candidateVideos = candidates,
            mode = TodayWatchMode.LEARN,
            eyeCareNightActive = false,
            nowEpochSec = 1_700_010_000,
            queueLimit = 1
        )

        assertEquals("learn_fit", plan.videoQueue.firstOrNull()?.bvid)
    }

    @Test
    fun `plan respects queue and up-rank limits`() {
        val history = listOf(
            VideoItem(bvid = "h1", owner = Owner(mid = 1, name = "UP-A"), duration = 600, progress = 500, view_at = 1_700_000_000),
            VideoItem(bvid = "h2", owner = Owner(mid = 2, name = "UP-B"), duration = 600, progress = 500, view_at = 1_700_000_200),
            VideoItem(bvid = "h3", owner = Owner(mid = 3, name = "UP-C"), duration = 600, progress = 500, view_at = 1_700_000_400)
        )
        val candidates = (1..12).map { index ->
            VideoItem(
                bvid = "c$index",
                owner = Owner(mid = (index % 3 + 1).toLong(), name = "UP-$index"),
                duration = 300 + index * 10,
                stat = Stat(view = 5_000 + index, danmaku = 30 + index),
                title = "候选$index"
            )
        }

        val plan = buildTodayWatchPlan(
            historyVideos = history,
            candidateVideos = candidates,
            mode = TodayWatchMode.RELAX,
            eyeCareNightActive = false,
            nowEpochSec = 1_700_010_000,
            upRankLimit = 2,
            queueLimit = 4
        )

        assertEquals(2, plan.upRanks.size)
        assertEquals(4, plan.videoQueue.size)
    }

    @Test
    fun `plan diversifies creators in top queue`() {
        val history = listOf(
            VideoItem(bvid = "h1", owner = Owner(mid = 1, name = "UP-A"), duration = 600, progress = 550, view_at = 1_700_000_000),
            VideoItem(bvid = "h2", owner = Owner(mid = 1, name = "UP-A"), duration = 600, progress = 520, view_at = 1_700_000_500)
        )
        val candidates = listOf(
            VideoItem(bvid = "a1", owner = Owner(mid = 1, name = "UP-A"), duration = 480, stat = Stat(view = 12_000, danmaku = 90), title = "A1"),
            VideoItem(bvid = "a2", owner = Owner(mid = 1, name = "UP-A"), duration = 500, stat = Stat(view = 11_000, danmaku = 85), title = "A2"),
            VideoItem(bvid = "b1", owner = Owner(mid = 2, name = "UP-B"), duration = 520, stat = Stat(view = 10_000, danmaku = 70), title = "B1"),
            VideoItem(bvid = "c1", owner = Owner(mid = 3, name = "UP-C"), duration = 530, stat = Stat(view = 9_500, danmaku = 60), title = "C1")
        )

        val plan = buildTodayWatchPlan(
            historyVideos = history,
            candidateVideos = candidates,
            mode = TodayWatchMode.RELAX,
            eyeCareNightActive = false,
            nowEpochSec = 1_700_010_000,
            queueLimit = 4
        )

        val topThreeMids = plan.videoQueue.take(3).map { it.owner.mid }.toSet()
        assertTrue(topThreeMids.size >= 2)
    }

    @Test
    fun `plan diversifies topics in top queue`() {
        val candidates = listOf(
            VideoItem(
                bvid = "music_hot",
                owner = Owner(mid = 1, name = "UP-M1"),
                duration = 480,
                stat = Stat(view = 35_000, danmaku = 60),
                title = "治愈音乐现场"
            ),
            VideoItem(
                bvid = "music_next",
                owner = Owner(mid = 2, name = "UP-M2"),
                duration = 480,
                stat = Stat(view = 30_000, danmaku = 50),
                title = "音乐合集精选"
            ),
            VideoItem(
                bvid = "travel",
                owner = Owner(mid = 3, name = "UP-T"),
                duration = 480,
                stat = Stat(view = 10_000, danmaku = 20),
                title = "旅行记录"
            )
        )

        val plan = buildTodayWatchPlan(
            historyVideos = emptyList(),
            candidateVideos = candidates,
            mode = TodayWatchMode.RELAX,
            eyeCareNightActive = false,
            nowEpochSec = 1_700_010_000,
            queueLimit = 3
        )

        assertEquals("music_hot", plan.videoQueue.firstOrNull()?.bvid)
        assertTrue(plan.videoQueue.take(2).any { it.bvid == "travel" })
    }

    @Test
    fun `negative feedback penalizes disliked creator and title`() {
        val history = listOf(
            VideoItem(bvid = "h1", owner = Owner(mid = 1, name = "UP-A"), duration = 600, progress = 550, view_at = 1_700_000_000)
        )
        val candidates = listOf(
            VideoItem(
                bvid = "disliked",
                owner = Owner(mid = 8, name = "UP-X"),
                duration = 600,
                stat = Stat(view = 20_000, danmaku = 90),
                title = "震惊吵闹整活合集"
            ),
            VideoItem(
                bvid = "normal",
                owner = Owner(mid = 2, name = "UP-B"),
                duration = 620,
                stat = Stat(view = 16_000, danmaku = 80),
                title = "通勤学习总结"
            )
        )

        val plan = buildTodayWatchPlan(
            historyVideos = history,
            candidateVideos = candidates,
            mode = TodayWatchMode.RELAX,
            eyeCareNightActive = false,
            nowEpochSec = 1_700_010_000,
            penaltySignals = TodayWatchPenaltySignals(
                dislikedBvids = setOf("disliked"),
                dislikedCreatorMids = setOf(8L),
                dislikedKeywords = setOf("吵闹", "整活")
            )
        )

        assertEquals("normal", plan.videoQueue.first().bvid)
        assertFalse(plan.videoQueue.take(1).any { it.bvid == "disliked" })
    }

    @Test
    fun `plan excludes explicitly disliked bvid even when candidate is very hot`() {
        val candidates = listOf(
            VideoItem(
                bvid = "disliked_hot",
                owner = Owner(mid = 1, name = "UP-Hot"),
                duration = 480,
                stat = Stat(view = 1_000_000_000, danmaku = 50),
                title = "超热门但不感兴趣"
            ),
            VideoItem(
                bvid = "normal",
                owner = Owner(mid = 2, name = "UP-Normal"),
                duration = 480,
                stat = Stat(view = 1_000, danmaku = 20),
                title = "普通推荐"
            )
        )

        val plan = buildTodayWatchPlan(
            historyVideos = emptyList(),
            candidateVideos = candidates,
            mode = TodayWatchMode.RELAX,
            eyeCareNightActive = false,
            nowEpochSec = 1_700_010_000,
            queueLimit = 1,
            penaltySignals = TodayWatchPenaltySignals(
                dislikedBvids = setOf("disliked_hot")
            )
        )

        assertEquals("normal", plan.videoQueue.firstOrNull()?.bvid)
        assertFalse(plan.videoQueue.any { it.bvid == "disliked_hot" })
    }

    @Test
    fun `plan includes readable explanation per queued video`() {
        val history = listOf(
            VideoItem(bvid = "h1", owner = Owner(mid = 1, name = "UP-A"), duration = 600, progress = 520, view_at = 1_700_000_000)
        )
        val candidates = listOf(
            VideoItem(
                bvid = "explain1",
                owner = Owner(mid = 1, name = "UP-A"),
                duration = 420,
                stat = Stat(view = 9000, danmaku = 30),
                title = "通勤学习技巧",
                pubdate = 1_700_009_000
            )
        )

        val plan = buildTodayWatchPlan(
            historyVideos = history,
            candidateVideos = candidates,
            mode = TodayWatchMode.LEARN,
            eyeCareNightActive = true,
            nowEpochSec = 1_700_010_000
        )

        val explanation = plan.explanationByBvid["explain1"].orEmpty()
        assertTrue(explanation.isNotBlank())
        assertTrue(explanation.contains("学习"))
        assertTrue(explanation.contains("近期更新"))
    }

    @Test
    fun `creator signals boost preferred up even when history owner mid is missing`() {
        val history = listOf(
            VideoItem(
                bvid = "h_missing_mid",
                owner = Owner(mid = 0, name = "Unknown"),
                duration = 600,
                progress = 500,
                view_at = 1_700_000_000
            )
        )
        val candidates = listOf(
            VideoItem(
                bvid = "prefer_a",
                owner = Owner(mid = 11, name = "UP-A"),
                duration = 640,
                stat = Stat(view = 9000, danmaku = 60),
                title = "普通内容A"
            ),
            VideoItem(
                bvid = "prefer_b",
                owner = Owner(mid = 22, name = "UP-B"),
                duration = 640,
                stat = Stat(view = 9000, danmaku = 60),
                title = "普通内容B"
            )
        )

        val plan = buildTodayWatchPlan(
            historyVideos = history,
            candidateVideos = candidates,
            mode = TodayWatchMode.RELAX,
            eyeCareNightActive = false,
            nowEpochSec = 1_700_010_000,
            creatorSignals = listOf(
                TodayWatchCreatorSignal(
                    mid = 22,
                    name = "UP-B",
                    score = 6.5,
                    watchCount = 4
                )
            )
        )

        assertEquals("prefer_b", plan.videoQueue.firstOrNull()?.bvid)
        assertEquals(22L, plan.upRanks.firstOrNull()?.mid)
    }

    @Test
    fun `plan filters out consumed queue items`() {
        val candidates = listOf(
            VideoItem(
                bvid = "keep",
                owner = Owner(mid = 1, name = "UP-A"),
                duration = 420,
                stat = Stat(view = 8_000, danmaku = 40),
                title = "保留视频"
            ),
            VideoItem(
                bvid = "consumed",
                owner = Owner(mid = 2, name = "UP-B"),
                duration = 420,
                stat = Stat(view = 8_000, danmaku = 40),
                title = "已看视频"
            )
        )

        val plan = buildTodayWatchPlan(
            historyVideos = emptyList(),
            candidateVideos = candidates,
            mode = TodayWatchMode.RELAX,
            eyeCareNightActive = false,
            nowEpochSec = 1_700_010_000,
            penaltySignals = TodayWatchPenaltySignals(
                consumedBvids = setOf("consumed")
            )
        )

        assertTrue(plan.videoQueue.any { it.bvid == "keep" })
        assertFalse(plan.videoQueue.any { it.bvid == "consumed" })
    }
}
