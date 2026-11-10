package com.android.purebilibili.plugin.sdk

import kotlinx.serialization.Serializable

@Serializable
enum class RecommendationMode {
    RELAX,
    LEARN
}

@Serializable
enum class RecommendationStrategy {
    BALANCED,
    AFFINITY,
    EXPLORE
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
    /** Unix epoch seconds; hosts should supply their current clock explicitly. */
    val nowEpochSec: Long = 0L
)

@Serializable
data class RecommendationRequest<T>(
    val candidateVideos: List<T>,
    val historyVideos: List<T>,
    val creatorSignals: List<RecommendationCreatorSignal> = emptyList(),
    val feedbackSignals: RecommendationFeedbackSignals = RecommendationFeedbackSignals(),
    val sceneSignals: RecommendationSceneSignals = RecommendationSceneSignals(eyeCareNightActive = false),
    val mode: RecommendationMode,
    val queueLimit: Int,
    val groupLimit: Int,
    val strategy: RecommendationStrategy = RecommendationStrategy.BALANCED
)

@Serializable
data class RecommendedVideo<T>(
    val video: T,
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
    val score: Double? = null,
    val watchCount: Int? = null
)

@Serializable
data class RecommendationResult<T>(
    val sourcePluginId: String,
    val mode: RecommendationMode,
    val items: List<RecommendedVideo<T>>,
    val groups: List<RecommendationGroup> = emptyList(),
    val historySampleCount: Int = 0,
    val sceneSignals: RecommendationSceneSignals = RecommendationSceneSignals(eyeCareNightActive = false),
    /** Generation time in Unix epoch milliseconds. */
    val generatedAt: Long = 0L
)

interface RecommendationPluginApi<T> : Plugin {

    fun buildRecommendations(request: RecommendationRequest<T>): RecommendationResult<T>
}
