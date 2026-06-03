package com.android.purebilibili.plugin.sdk

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecommendationPluginSdkTest {

    @Test
    fun recommendationPluginCanRankSdkVideoCandidatesWithoutAppTypes() {
        val plugin = object : RecommendationPluginApi {
            override val capabilityManifest = PluginCapabilityManifest(
                pluginId = "dev.example.rank",
                displayName = "SDK Ranker",
                version = "1.0.0",
                apiVersion = 1,
                entryClassName = "dev.example.RankPlugin",
                capabilities = setOf(PluginCapability.RECOMMENDATION_CANDIDATES)
            )

            override fun buildRecommendations(request: RecommendationRequest): RecommendationResult {
                val ranked = request.candidateVideos
                    .sortedByDescending { it.durationSeconds }
                    .take(request.queueLimit)
                    .mapIndexed { index, video ->
                        RecommendedVideo(
                            video = video,
                            score = 100.0 - index,
                            confidence = 0.8f,
                            explanation = "duration rank"
                        )
                    }
                return RecommendationResult(
                    sourcePluginId = capabilityManifest.pluginId,
                    mode = request.mode,
                    items = ranked
                )
            }
        }
        val request = RecommendationRequest(
            candidateVideos = listOf(
                PluginVideoCandidate(bvid = "BV1", title = "short", durationSeconds = 30),
                PluginVideoCandidate(
                    bvid = "BV2",
                    title = "long",
                    durationSeconds = 300,
                    partitionId = 36,
                    partitionName = "知识",
                    replyCount = 42,
                    tags = listOf("Kotlin")
                )
            ),
            historyVideos = emptyList(),
            mode = RecommendationMode.RELAX,
            queueLimit = 1,
            groupLimit = 0
        )

        val result = plugin.buildRecommendations(request)

        assertEquals("dev.example.rank", result.sourcePluginId)
        assertEquals(listOf("BV2"), result.items.map { it.video.bvid })
        assertEquals(36, result.items.single().video.partitionId)
        assertEquals("知识", result.items.single().video.partitionName)
        assertEquals(42, result.items.single().video.replyCount)
        assertEquals(listOf("Kotlin"), result.items.single().video.tags)
        assertTrue(result.items.single().explanation.isNotBlank())
    }
}
