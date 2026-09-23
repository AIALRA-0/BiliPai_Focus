package com.android.purebilibili.feature.plugin

import com.android.purebilibili.core.plugin.PluginCapability
import com.android.purebilibili.core.plugin.RecommendationCreatorSignal
import com.android.purebilibili.core.plugin.RecommendationFeedbackSignals
import com.android.purebilibili.core.plugin.RecommendationMode
import com.android.purebilibili.core.plugin.RecommendationPluginApi
import com.android.purebilibili.core.plugin.RecommendationRequest
import com.android.purebilibili.core.plugin.RecommendationSceneSignals
import com.android.purebilibili.data.model.response.Owner
import com.android.purebilibili.data.model.response.Stat
import com.android.purebilibili.data.model.response.VideoItem
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TodayWatchRecommendationPluginTest {

    @Test
    fun todayWatchPlugin_declaresRecommendationCapabilities() {
        val plugin: Any = TodayWatchPlugin()

        assertTrue(plugin is RecommendationPluginApi)
        assertTrue(plugin.capabilityManifest.capabilities.contains(PluginCapability.RECOMMENDATION_CANDIDATES))
        assertTrue(plugin.capabilityManifest.capabilities.contains(PluginCapability.LOCAL_HISTORY_READ))
        assertTrue(plugin.capabilityManifest.capabilities.contains(PluginCapability.LOCAL_FEEDBACK_READ))
        assertEquals("1.1.0", plugin.version)
    }

    @Test
    fun todayWatchPlugin_buildsRecommendationResultFromSdkRequest() {
        val plugin = TodayWatchPlugin()
        val request = RecommendationRequest(
            candidateVideos = listOf(
                VideoItem(
                    bvid = "candidate_a",
                    owner = Owner(mid = 1, name = "UP-A"),
                    duration = 420,
                    stat = Stat(view = 8_000, danmaku = 30),
                    title = "轻松短视频"
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

        val result = plugin.buildRecommendations(request)

        assertEquals("today_watch", result.sourcePluginId)
        assertEquals("candidate_a", result.items.first().video.bvid)
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
    fun todayWatchPluginConfig_oldPayloadDefaultsToBalancedLocalMode() {
        val config = Json { ignoreUnknownKeys = true }.decodeFromString<TodayWatchPluginConfig>(
            """{"currentMode":"LEARN","queueBuildLimit":30}"""
        )

        assertEquals(com.android.purebilibili.core.plugin.RecommendationStrategy.BALANCED, config.recommendationStrategy)
        assertEquals(TodayWatchCandidatePoolMode.LOCAL, config.candidatePoolMode)
        assertEquals(30, config.queueBuildLimit)
    }
}
