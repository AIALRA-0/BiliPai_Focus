package com.android.purebilibili.feature.video.viewmodel

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

internal data class VideoBandwidthSample(
    val networkHandle: Long,
    val bitrateEstimate: Long,
    val sampledAtMs: Long
)

internal fun isConstrainedVideoNetwork(
    networkHandle: Long?,
    downstreamKbps: Int,
    sample: VideoBandwidthSample?,
    nowMs: Long
): Boolean {
    if (networkHandle == null) return false
    if (sample != null && sample.networkHandle == networkHandle &&
        sample.bitrateEstimate > 0L && nowMs - sample.sampledAtMs in 0L..30_000L
    ) {
        return sample.bitrateEstimate < 2_000_000L
    }
    return downstreamKbps in 1..1_500
}

internal data class RelatedVideoLoadSnapshot(
    val requestToken: Long,
    val constrainedNetwork: Boolean = false,
    val hasCachedRelated: Boolean = false,
    val playbackPrepared: Boolean = false,
    val firstFrameRendered: Boolean = false,
    val playbackReady: Boolean = false,
    val buffering: Boolean = false,
    val playWhenReady: Boolean = true,
    val bufferedDurationMs: Long = 0L,
    val playbackFailed: Boolean = false,
    val playbackEnded: Boolean = false
)

internal enum class RelatedVideoLoadDecision { START, DEFER, CANCEL }

internal fun relatedVideoLoadDecision(
    requestToken: Long,
    snapshot: RelatedVideoLoadSnapshot
): RelatedVideoLoadDecision {
    if (snapshot.requestToken != requestToken) return RelatedVideoLoadDecision.CANCEL
    if (snapshot.hasCachedRelated || snapshot.playbackFailed || snapshot.playbackEnded) {
        return RelatedVideoLoadDecision.START
    }
    if (!snapshot.playbackPrepared ||
        (!snapshot.firstFrameRendered && (snapshot.playWhenReady || !snapshot.playbackReady))
    ) return RelatedVideoLoadDecision.DEFER
    val rebuffering = snapshot.playbackPrepared && snapshot.firstFrameRendered && snapshot.buffering
    if (!snapshot.constrainedNetwork && !rebuffering) return RelatedVideoLoadDecision.START
    val playbackHasHeadroom = snapshot.playbackPrepared && snapshot.playbackReady &&
        (snapshot.firstFrameRendered || !snapshot.playWhenReady) &&
        (!snapshot.playWhenReady || snapshot.bufferedDurationMs >= 1_500L)
    return if (playbackHasHeadroom) RelatedVideoLoadDecision.START else RelatedVideoLoadDecision.DEFER
}

internal class RelatedVideoLoadScheduler(
    private val maxWaitMs: Long = 6_000L,
    private val pollIntervalMs: Long = 200L
) {
    init {
        require(maxWaitMs > 0L)
        require(pollIntervalMs > 0L)
    }

    suspend fun awaitPermit(
        requestToken: Long,
        snapshot: suspend () -> RelatedVideoLoadSnapshot,
        onDeferred: () -> Unit = {}
    ): Boolean {
        when (relatedVideoLoadDecision(requestToken, snapshot())) {
            RelatedVideoLoadDecision.START -> return true
            RelatedVideoLoadDecision.CANCEL -> return false
            RelatedVideoLoadDecision.DEFER -> onDeferred()
        }
        return withTimeoutOrNull(maxWaitMs) {
            var decision = RelatedVideoLoadDecision.DEFER
            while (decision == RelatedVideoLoadDecision.DEFER) {
                delay(pollIntervalMs)
                decision = relatedVideoLoadDecision(requestToken, snapshot())
            }
            decision == RelatedVideoLoadDecision.START
        } ?: (snapshot().requestToken == requestToken)
    }
}
