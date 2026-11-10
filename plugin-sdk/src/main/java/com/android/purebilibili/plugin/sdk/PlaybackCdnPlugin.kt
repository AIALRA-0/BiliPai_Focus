package com.android.purebilibili.plugin.sdk

enum class PlaybackCdnCandidateSource {
    CUSTOM,
    REGION,
    ORIGINAL
}

data class PlaybackCdnCandidate(
    val videoUrl: String,
    val audioUrl: String?,
    val source: PlaybackCdnCandidateSource
)

data class PlaybackCdnRewriteResult(
    val candidates: List<PlaybackCdnCandidate>,
    val regionLabel: String?,
    val cacheKeysByUrl: Map<String, String> = emptyMap()
) {
    val videoUrls: List<String> get() = candidates.map { it.videoUrl }
    val audioUrls: List<String> get() = candidates.map { it.audioUrl.orEmpty() }
    val sources: List<PlaybackCdnCandidateSource> get() = candidates.map { it.source }
}

enum class CdnHealthEvent {
    PLAYBACK_READY,
    FIRST_FRAME_TIMEOUT,
    AUDIO_TRACK_MISSING,
    PLAYER_ERROR,
    BUFFERING
}

data class CdnLineDiagnostic(
    val index: Int,
    val host: String,
    val displayName: String,
    val statusLabel: String,
    val latencyMs: Long?,
    val speedKbps: Long?,
    val errorCount: Int,
    val bufferingCount: Int,
    val lastProbeAtMs: Long,
    val score: Int,
    val sourceLabel: String? = null
)

interface PlaybackCdnPlugin : Plugin {
    fun rewritePlaybackCandidates(
        videoUrls: List<String>,
        audioUrls: List<String>
    ): PlaybackCdnRewriteResult

    fun buildPlaybackCdnDiagnostics(
        videoUrls: List<String>,
        sources: List<PlaybackCdnCandidateSource> = emptyList()
    ): List<CdnLineDiagnostic> = emptyList()

    suspend fun probePlaybackCdnCandidates(
        videoUrls: List<String>,
        sources: List<PlaybackCdnCandidateSource> = emptyList()
    ): List<CdnLineDiagnostic> = emptyList()

    fun recordPlaybackCdnEvent(url: String, event: CdnHealthEvent) = Unit

    fun isAdaptivePrefetchEnabled(): Boolean = false
}
