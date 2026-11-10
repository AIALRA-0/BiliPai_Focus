package com.android.purebilibili.core.plugin

import com.android.purebilibili.data.model.response.SponsorProgressMarker
import com.android.purebilibili.data.model.response.SponsorSegment
import com.android.purebilibili.plugin.sdk.PlayerPluginSession

/** Per-playback SponsorBlock state and explicitly requested community operations. */
interface SponsorBlockSessionApi : PlayerPluginSession {
    fun markAsSkipped(segmentId: String): SponsorSegment?
    fun getProgressMarkers(): List<SponsorProgressMarker>
    fun getSegments(): List<SponsorSegment>
    fun getActiveSegment(): SponsorSegment?
    fun isCommunityContributionEnabled(): Boolean
    fun getCommunityServerBaseUrl(): String
    suspend fun uploadViewedSegmentIfEnabled(segmentId: String)
    suspend fun voteOnCommunitySegment(segmentId: String, voteType: Int): Result<Unit>
    suspend fun submitCommunitySegment(
        bvid: String,
        cid: Long,
        videoDurationSeconds: Float,
        startMs: Long,
        endMs: Long,
        category: String,
        actionType: String,
    ): Result<List<SponsorSegment>>
}
