package com.android.purebilibili.plugin.sdk

import kotlinx.serialization.Serializable

@Serializable
enum class RecommendationMode {
    RELAX,
    LEARN
}

@Serializable
data class PluginVideoCandidate(
    val bvid: String,
    val title: String,
    val authorName: String = "",
    val authorMid: Long? = null,
    val partitionId: Int = 0,
    val partitionName: String = "",
    val durationSeconds: Long = 0,
    val playCount: Long = 0,
    val likeCount: Long = 0,
    val replyCount: Long = 0,
    val danmakuCount: Long = 0,
    val publishTimeEpochSec: Long? = null,
    val tags: List<String> = emptyList()
)

@Serializable
data class RecommendationCreatorSignal(
    val mid: Long,
    val name: String,
    val score: Double,
    val watchCount: Int
)

@Serializable
data class RecommendationFeedbackSignals(
    val consumedBvids: Set<String> = emptySet(),
    val dislikedBvids: Set<String> = emptySet(),
    val dislikedCreatorMids: Set<Long> = emptySet(),
    val dislikedKeywords: Set<String> = emptySet()
)

@Serializable
data class RecommendationSceneSignals(
    val eyeCareNightActive: Boolean,
    val nowEpochSec: Long = 0L
)

@Serializable
data class RecommendationRequest(
    val candidateVideos: List<PluginVideoCandidate>,
    val historyVideos: List<PluginVideoCandidate>,
    val creatorSignals: List<RecommendationCreatorSignal> = emptyList(),
    val feedbackSignals: RecommendationFeedbackSignals = RecommendationFeedbackSignals(),
    val sceneSignals: RecommendationSceneSignals = RecommendationSceneSignals(eyeCareNightActive = false),
    val mode: RecommendationMode,
    val queueLimit: Int,
    val groupLimit: Int
)

@Serializable
data class RecommendedVideo(
    val video: PluginVideoCandidate,
    val score: Double,
    val confidence: Float,
    val explanation: String,
    val actions: List<RecommendationAction> = emptyList()
)

@Serializable
data class RecommendationAction(
    val id: String,
    val label: String,
    val targetBvid: String? = null
)

@Serializable
data class RecommendationGroup(
    val id: String,
    val title: String,
    val items: List<RecommendationGroupItem>
)

@Serializable
data class RecommendationGroupItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val score: Double? = null
)

@Serializable
data class RecommendationResult(
    val sourcePluginId: String,
    val mode: RecommendationMode,
    val items: List<RecommendedVideo>,
    val groups: List<RecommendationGroup> = emptyList(),
    val historySampleCount: Int = 0,
    val sceneSignals: RecommendationSceneSignals = RecommendationSceneSignals(eyeCareNightActive = false),
    val generatedAt: Long = 0L
)

interface RecommendationPluginApi {
    val capabilityManifest: PluginCapabilityManifest

    fun buildRecommendations(request: RecommendationRequest): RecommendationResult
}
