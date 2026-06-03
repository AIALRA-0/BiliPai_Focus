package dev.bilipai.samples.watchcompass

import com.android.purebilibili.plugin.sdk.PluginCapability
import com.android.purebilibili.plugin.sdk.PluginCapabilityManifest
import com.android.purebilibili.plugin.sdk.PluginVideoCandidate
import com.android.purebilibili.plugin.sdk.RecommendationFeedbackSignals
import com.android.purebilibili.plugin.sdk.RecommendationGroup
import com.android.purebilibili.plugin.sdk.RecommendationGroupItem
import com.android.purebilibili.plugin.sdk.RecommendationMode
import com.android.purebilibili.plugin.sdk.RecommendationPluginApi
import com.android.purebilibili.plugin.sdk.RecommendationRequest
import com.android.purebilibili.plugin.sdk.RecommendationResult
import com.android.purebilibili.plugin.sdk.RecommendedVideo
import kotlin.math.ln

class WatchCompassPlugin : RecommendationPluginApi {
    override val capabilityManifest = PluginCapabilityManifest(
        pluginId = "dev.bilipai.samples.watch_compass",
        displayName = "观感罗盘",
        version = "1.0.0",
        apiVersion = 1,
        entryClassName = "dev.bilipai.samples.watchcompass.WatchCompassPlugin",
        capabilities = setOf(
            PluginCapability.RECOMMENDATION_CANDIDATES,
            PluginCapability.LOCAL_HISTORY_READ,
            PluginCapability.LOCAL_FEEDBACK_READ
        )
    )

    override fun buildRecommendations(request: RecommendationRequest): RecommendationResult {
        val historyCreatorMids = request.historyVideos
            .mapNotNull { it.authorMid }
            .filter { it > 0L }
            .toSet()
        val scored = request.candidateVideos
            .asSequence()
            .filter { it.bvid.isNotBlank() && it.title.isNotBlank() }
            .filterNot { it.bvid in request.feedbackSignals.consumedBvids }
            .filterNot { it.bvid in request.feedbackSignals.dislikedBvids }
            .filterNot { it.authorMid != null && it.authorMid in request.feedbackSignals.dislikedCreatorMids }
            .filterNot { candidate ->
                request.feedbackSignals.dislikedKeywords.any { keyword ->
                    keyword.isNotBlank() && candidate.title.contains(keyword, ignoreCase = true)
                }
            }
            .distinctBy { it.bvid }
            .map { candidate ->
                val lane = resolveLane(
                    candidate = candidate,
                    mode = request.mode,
                    feedbackSignals = request.feedbackSignals,
                    historyCreatorMids = historyCreatorMids,
                    nightActive = request.sceneSignals.eyeCareNightActive
                )
                ScoredCompassVideo(
                    candidate = candidate,
                    lane = lane,
                    score = scoreCandidate(
                        candidate = candidate,
                        lane = lane,
                        historyCreatorMids = historyCreatorMids,
                        mode = request.mode,
                        nowEpochSec = request.sceneSignals.nowEpochSec
                    ),
                    explanation = buildExplanation(candidate, lane, request.mode)
                )
            }
            .sortedByDescending { it.score }
            .toList()

        val ranked = interleaveByLane(scored, request.queueLimit.coerceAtLeast(1))
        return RecommendationResult(
            sourcePluginId = capabilityManifest.pluginId,
            mode = request.mode,
            items = ranked.mapIndexed { index, item ->
                RecommendedVideo(
                    video = item.candidate,
                    score = item.score,
                    confidence = (0.86f - index * 0.035f).coerceIn(0.42f, 0.86f),
                    explanation = item.explanation
                )
            },
            groups = buildGroups(scored, request.groupLimit.coerceAtLeast(1)),
            historySampleCount = request.historyVideos.size,
            sceneSignals = request.sceneSignals,
            generatedAt = request.sceneSignals.nowEpochSec
        )
    }
}

private data class ScoredCompassVideo(
    val candidate: PluginVideoCandidate,
    val lane: CompassLane,
    val score: Double,
    val explanation: String
)

private enum class CompassLane(
    val groupId: String,
    val title: String
) {
    EASY_START("easy_start", "轻松起步"),
    DEEP_DIVE("deep_dive", "深挖正片"),
    HIDDEN_GEM("hidden_gem", "冷门宝藏")
}

private fun resolveLane(
    candidate: PluginVideoCandidate,
    mode: RecommendationMode,
    feedbackSignals: RecommendationFeedbackSignals,
    historyCreatorMids: Set<Long>,
    nightActive: Boolean
): CompassLane {
    val durationMin = candidate.durationSeconds / 60.0
    val knownCreator = candidate.authorMid != null && candidate.authorMid in historyCreatorMids
    val title = candidate.title.lowercase()
    val wantsDeep = mode == RecommendationMode.LEARN ||
        title.contains("教程") ||
        title.contains("解析") ||
        title.contains("复盘") ||
        title.contains("原理")
    val feedbackKeywords = feedbackSignals.dislikedKeywords.map { it.lowercase() }

    return when {
        wantsDeep && durationMin >= 8.0 -> CompassLane.DEEP_DIVE
        !knownCreator && candidate.playCount in 1..49_999L && feedbackKeywords.none { it in title } -> {
            CompassLane.HIDDEN_GEM
        }
        nightActive || durationMin <= 8.0 -> CompassLane.EASY_START
        else -> CompassLane.DEEP_DIVE
    }
}

private fun scoreCandidate(
    candidate: PluginVideoCandidate,
    lane: CompassLane,
    historyCreatorMids: Set<Long>,
    mode: RecommendationMode,
    nowEpochSec: Long
): Double {
    val heat = ln(candidate.playCount.coerceAtLeast(1).toDouble()) * 0.8 +
        ln(candidate.likeCount.coerceAtLeast(1).toDouble()) * 0.6
    val creatorBonus = if (candidate.authorMid != null && candidate.authorMid in historyCreatorMids) 2.4 else 0.0
    val freshnessBonus = candidate.publishTimeEpochSec
        ?.let { published ->
            val ageDays = ((nowEpochSec - published).coerceAtLeast(0L) / 86_400.0).coerceAtMost(90.0)
            (2.0 - ageDays / 20.0).coerceAtLeast(0.0)
        }
        ?: 0.0
    val laneBonus = when (lane) {
        CompassLane.EASY_START -> if (mode == RecommendationMode.RELAX) 2.0 else 0.4
        CompassLane.DEEP_DIVE -> if (mode == RecommendationMode.LEARN) 2.2 else 0.8
        CompassLane.HIDDEN_GEM -> 1.4
    }
    return heat + creatorBonus + freshnessBonus + laneBonus
}

private fun buildExplanation(
    candidate: PluginVideoCandidate,
    lane: CompassLane,
    mode: RecommendationMode
): String {
    return when (lane) {
        CompassLane.EASY_START -> "轻松起步：${candidate.durationSeconds / 60} 分钟左右，适合先热身"
        CompassLane.DEEP_DIVE -> if (mode == RecommendationMode.LEARN) {
            "深挖正片：更贴近学习模式，适合完整看完"
        } else {
            "深挖正片：信息密度更高，适合留出整段时间"
        }
        CompassLane.HIDDEN_GEM -> "冷门宝藏：非高热视频，给新来源一个机会"
    }
}

private fun interleaveByLane(
    scored: List<ScoredCompassVideo>,
    limit: Int
): List<ScoredCompassVideo> {
    val queues = CompassLane.entries.associateWith { lane ->
        scored.filter { it.lane == lane }.toMutableList()
    }
    val result = mutableListOf<ScoredCompassVideo>()
    while (result.size < limit && queues.values.any { it.isNotEmpty() }) {
        CompassLane.entries.forEach { lane ->
            val next = queues[lane]?.removeFirstOrNull()
            if (next != null && result.size < limit) {
                result += next
            }
        }
    }
    return result
}

private fun buildGroups(
    scored: List<ScoredCompassVideo>,
    groupLimit: Int
): List<RecommendationGroup> {
    return CompassLane.entries.mapNotNull { lane ->
        val laneItems = scored
            .filter { it.lane == lane }
            .take(groupLimit)
        if (laneItems.isEmpty()) {
            null
        } else {
            RecommendationGroup(
                id = lane.groupId,
                title = lane.title,
                items = laneItems.map { item ->
                    RecommendationGroupItem(
                        id = item.candidate.bvid,
                        title = item.candidate.title,
                        subtitle = item.explanation,
                        score = item.score
                    )
                }
            )
        }
    }
}
