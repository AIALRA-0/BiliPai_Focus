package com.android.purebilibili.feature.plugin

internal const val CDN_WINDOW_BYTES = 1024L * 1024L
internal const val CDN_MIN_PARALLEL_BYTES = 256L * 1024L
internal const val CDN_MAX_CONNECTIONS = 4

internal data class CdnContentRange(val start: Long, val end: Long, val total: Long)

internal fun parseCdnContentRange(value: String?): CdnContentRange? {
    val match = Regex("bytes (\\d+)-(\\d+)/(\\d+)", RegexOption.IGNORE_CASE)
        .matchEntire(value?.trim().orEmpty()) ?: return null
    val (start, end, total) = match.destructured
    val first = start.toLongOrNull() ?: return null
    val last = end.toLongOrNull() ?: return null
    val size = total.toLongOrNull() ?: return null
    return CdnContentRange(first, last, size).takeIf { first >= 0 && last >= first && size > last }
}

internal fun isExactCdnRange(status: Int, header: String?, range: CdnByteRange): Boolean {
    val actual = parseCdnContentRange(header) ?: return false
    return status == 206 && actual.start == range.start && actual.end == range.endInclusive
}

internal fun splitCdnRange(start: Long, length: Long, connections: Int): List<CdnByteRange> {
    if (start < 0 || length <= 0 || length - 1 > Long.MAX_VALUE - start) return emptyList()
    val count = connections.coerceIn(1, CDN_MAX_CONNECTIONS).coerceAtMost(length.toIntSafe())
    val chunk = length / count
    return (0 until count).map { index ->
        val first = start + index * chunk
        CdnByteRange(first, if (index == count - 1) start + length - 1 else first + chunk - 1)
    }
}

private fun Long.toIntSafe(): Int = coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

internal fun resolveCdnConnections(bufferMs: Long, speedBps: Long, bitrateBps: Long, limited: Boolean): Int = when {
    limited -> 1
    bufferMs >= 30_000 -> 1
    bufferMs < 5_000 || (bitrateBps > 0 && speedBps * 8 < bitrateBps) -> 3
    else -> 2
}

/** Half-life of historical playback penalties; new events do not indefinitely refresh old ones. */
internal fun decayCdnHealth(health: CdnCandidateHealth, nowMs: Long): CdnCandidateHealth {
    val baseline = health.healthWindowStartedAtMs.takeIf { it > 0 }
        ?: health.lastUpdatedAtMs.takeIf { it > 0 } ?: nowMs
    val periods = ((nowMs - baseline).coerceAtLeast(0) / (30 * 60_000L)).coerceAtMost(16).toInt()
    val staleProbe = health.lastProbeAtMs > 0 && nowMs - health.lastProbeAtMs > 2 * 60_000L
    return health.copy(
        firstFrameTimeoutCount = health.firstFrameTimeoutCount ushr periods,
        bufferingCount = health.bufferingCount ushr periods,
        playbackErrorCount = health.playbackErrorCount ushr periods,
        readyCount = health.readyCount ushr periods,
        manualProbeLatencyMs = if (staleProbe) null else health.manualProbeLatencyMs,
        manualProbeSpeedKbps = if (staleProbe) null else health.manualProbeSpeedKbps,
        healthWindowStartedAtMs = if (periods > 0) nowMs else baseline
    )
}
