package dev.example.todaywatchremix

import com.android.purebilibili.plugin.sdk.PluginCapability
import com.android.purebilibili.plugin.sdk.PluginCapabilityManifest
import com.android.purebilibili.plugin.sdk.RecommendationMode
import com.android.purebilibili.plugin.sdk.RecommendationPluginApi
import com.android.purebilibili.plugin.sdk.RecommendationRequest
import com.android.purebilibili.plugin.sdk.RecommendationResult
import com.android.purebilibili.plugin.sdk.RecommendedVideo

class TodayWatchRemixPlugin : RecommendationPluginApi {
    override val capabilityManifest = PluginCapabilityManifest(
        pluginId = "dev.example.today_watch_remix",
        displayName = "Today Watch Remix",
        version = "1.0.0",
        apiVersion = 1,
        entryClassName = "dev.example.todaywatchremix.TodayWatchRemixPlugin",
        capabilities = setOf(
            PluginCapability.RECOMMENDATION_CANDIDATES,
            PluginCapability.LOCAL_HISTORY_READ
        )
    )

    override fun buildRecommendations(request: RecommendationRequest): RecommendationResult {
        val ranked = request.candidateVideos
            .filterNot { candidate -> candidate.bvid in request.feedbackSignals.consumedBvids }
            .sortedWith(
                compareByDescending<com.android.purebilibili.plugin.sdk.PluginVideoCandidate> {
                    it.likeCount
                }.thenByDescending { it.playCount }
            )
            .take(request.queueLimit)
            .mapIndexed { index, candidate ->
                RecommendedVideo(
                    video = candidate,
                    score = 100.0 - index,
                    confidence = 0.7f,
                    explanation = "Remix rank: likes first, then plays"
                )
            }

        return RecommendationResult(
            sourcePluginId = capabilityManifest.pluginId,
            mode = request.mode.takeIf { it == RecommendationMode.LEARN } ?: RecommendationMode.RELAX,
            items = ranked,
            historySampleCount = request.historyVideos.size,
            sceneSignals = request.sceneSignals
        )
    }
}
