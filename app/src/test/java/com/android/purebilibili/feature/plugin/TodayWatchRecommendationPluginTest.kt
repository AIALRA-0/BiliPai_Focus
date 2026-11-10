package com.android.purebilibili.feature.plugin

import com.android.purebilibili.core.plugin.BuiltinPluginIds

import com.android.purebilibili.plugin.sdk.RecommendationCreatorSignal
import com.android.purebilibili.plugin.sdk.RecommendationFeedbackSignals
import com.android.purebilibili.plugin.sdk.RecommendationMode
import com.android.purebilibili.plugin.sdk.RecommendationPluginApi
import com.android.purebilibili.plugin.sdk.RecommendationRequest
import com.android.purebilibili.plugin.sdk.RecommendationSceneSignals
import com.android.purebilibili.plugin.sdk.RecommendationStrategy
import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.Stat
import com.android.purebilibili.data.model.response.VideoItem
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertSame

class TodayWatchRecommendationPluginTest {

    @Test
    fun todayWatchPlugin_buildsRecommendationResultFromSdkRequest() {
        val plugin: RecommendationPluginApi<VideoItem> = TodayWatchPlugin()
        val request = RecommendationRequest(
            candidateVideos = listOf(
                VideoItem(
                    bvid = "candidate_a",
                    owner = Owner(mid = 1, name = "UP-A"),
                    duration = 420,
                    stat = Stat(view = 8_000, danmaku = 30),
                    title = "轻松短视频",
                    pic = "https://example.test/cover.jpg",
                    tname = "生活",
                    pubdate = 1_700_000_000
                ),
                VideoItem(
                    bvid = "candidate_b",
                    owner = Owner(mid = 2, name = "UP-B"),
                    duration = 2_400,
                    stat = Stat(view = 9_000, danmaku = 800),
                    title = "高刺激长视频"
                )
            ),
            historyVideos = listOf(
                VideoItem(
                    bvid = "history_a",
                    owner = Owner(mid = 1, name = "UP-A"),
                    duration = 600,
                    progress = 520,
                    view_at = 1_700_000_000
                )
            ),
            creatorSignals = listOf(
                RecommendationCreatorSignal(mid = 1, name = "UP-A", score = 4.0, watchCount = 3)
            ),
            feedbackSignals = RecommendationFeedbackSignals(dislikedBvids = setOf("candidate_b")),
            sceneSignals = RecommendationSceneSignals(eyeCareNightActive = true, nowEpochSec = 1_700_010_000),
            mode = RecommendationMode.RELAX,
            queueLimit = 6,
            groupLimit = 5
        )

        val beforeBuildMillis = System.currentTimeMillis()
        val result = plugin.buildRecommendations(request)
        val afterBuildMillis = System.currentTimeMillis()

        assertEquals(BuiltinPluginIds.TODAY_WATCH, result.sourcePluginId)
        assertEquals("candidate_a", result.items.first().video.bvid)
        assertSame(request.candidateVideos.first(), result.items.first().video)
        assertEquals(request.candidateVideos.first().owner, result.items.first().video.owner)
        assertEquals(request.candidateVideos.first().stat, result.items.first().video.stat)
        assertEquals("https://example.test/cover.jpg", result.items.first().video.pic)
        assertTrue(result.generatedAt in beforeBuildMillis..afterBuildMillis)
        assertTrue(result.items.first().score in 0.0..1.0)
        assertTrue(result.items.first().confidence in 0f..1f)
        assertTrue(result.items.first().explanation.isNotBlank())
        assertTrue(
            result.groups.any { group ->
                group.id == "preferred_creators" && group.items.any { it.watchCount != null }
            }
        )
    }

    @Test
    fun todayWatchPlugin_ranksPreferredCreatorBeforeOtherwiseEquivalentCandidate() {
        val preferred = VideoItem(
            bvid = "preferred",
            title = "日常记录",
            owner = Owner(mid = 1, name = "常看 UP"),
            duration = 480,
            pubdate = 1_700_000_000,
            stat = Stat(view = 8_000, danmaku = 30)
        )
        val unknown = preferred.copy(bvid = "unknown", owner = Owner(mid = 2, name = "新 UP"))
        val result = TodayWatchPlugin().buildRecommendations(
            RecommendationRequest(
                candidateVideos = listOf(unknown, preferred),
                historyVideos = listOf(
                    preferred.copy(bvid = "history", progress = 470, view_at = 1_700_000_000)
                ),
                sceneSignals = RecommendationSceneSignals(
                    eyeCareNightActive = false,
                    nowEpochSec = 1_700_010_000
                ),
                mode = RecommendationMode.RELAX,
                queueLimit = 2,
                groupLimit = 5
            )
        )

        assertEquals(listOf("preferred", "unknown"), result.items.map { it.video.bvid })
        assertSame(preferred, result.items.first().video)
        assertSame(unknown, result.items.last().video)
    }

    @Test
    fun todayWatchPluginConfig_oldPayloadDefaultsToBalancedLocalMode() {
        val config = Json { ignoreUnknownKeys = true }.decodeFromString<TodayWatchPluginConfig>(
            """{"currentMode":"LEARN","queueBuildLimit":30}"""
        )

        assertEquals(RecommendationStrategy.BALANCED, config.recommendationStrategy)
        assertEquals(TodayWatchCandidatePoolMode.LOCAL, config.candidatePoolMode)
        assertEquals(30, config.queueBuildLimit)
    }
}
