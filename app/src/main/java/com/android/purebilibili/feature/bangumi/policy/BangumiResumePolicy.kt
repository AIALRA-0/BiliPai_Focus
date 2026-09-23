package com.android.purebilibili.feature.bangumi

import com.android.purebilibili.data.model.response.BangumiDetail

internal const val BANGUMI_HEARTBEAT_INTERVAL_MS = 15_000L

internal data class BangumiResumeTarget(
    val epId: Long,
    val resumePositionMs: Long
)

internal data class BangumiDetailRequest(
    val seasonId: Long,
    val epId: Long
)

internal fun resolveBangumiDetailRequest(
    seasonId: Long,
    epId: Long
): BangumiDetailRequest {
    return if (epId > 0L) {
        BangumiDetailRequest(seasonId = 0L, epId = epId)
    } else {
        BangumiDetailRequest(seasonId = seasonId.coerceAtLeast(0L), epId = 0L)
    }
}

internal fun resolveBangumiAutoResumeTarget(
    detail: BangumiDetail,
    routeEpId: Long,
    autoResumeEnabled: Boolean
): BangumiResumeTarget? {
    if (!autoResumeEnabled || routeEpId > 0L) return null

    val progress = detail.userStatus?.progress ?: return null
    val lastEpId = progress.lastEpId.takeIf { it > 0L } ?: return null
    val episodes = detail.episodes.orEmpty()
    if (episodes.isNotEmpty() && episodes.none { it.id == lastEpId }) return null

    return BangumiResumeTarget(
        epId = lastEpId,
        resumePositionMs = resolveBangumiResumePositionMs(progress.lastTime)
    )
}

/**
 * PiliPlus chooses an explicitly supplied video aid before the ep id and finally the
 * remembered progress. Keeping this decision in one policy makes course entry points
 * behave identically whether they came from search, history or a share link.
 */
internal fun resolveBangumiInitialEpisode(
    detail: BangumiDetail,
    preferredAid: Long,
    routeEpId: Long,
    autoResumeEnabled: Boolean
): BangumiResumeTarget? {
    val episodes = detail.episodes.orEmpty()
    if (episodes.isEmpty()) return null
    preferredAid.takeIf { it > 0L }
        ?.let { aid -> episodes.firstOrNull { it.aid == aid }?.let { return BangumiResumeTarget(it.id, 0L) } }
    routeEpId.takeIf { it > 0L }
        ?.let { epId -> episodes.firstOrNull { it.id == epId }?.let { return BangumiResumeTarget(it.id, 0L) } }
    return resolveBangumiAutoResumeTarget(
        detail = detail,
        routeEpId = routeEpId,
        autoResumeEnabled = autoResumeEnabled
    ) ?: episodes.firstOrNull()?.let { BangumiResumeTarget(it.id, 0L) }
}

internal fun resolveBangumiResumePositionMs(lastTimeSec: Long): Long {
    return lastTimeSec.coerceAtLeast(0L) * 1000L
}

internal fun resolveBangumiPlaybackStartPositionMs(
    routeResumePositionMs: Long,
    savedEpisodePositionMs: Long
): Long {
    return routeResumePositionMs.takeIf { it > 0L }
        ?: savedEpisodePositionMs.coerceAtLeast(0L)
}

internal fun shouldSendBangumiPlaybackHeartbeat(
    isPlaying: Boolean,
    bvid: String,
    cid: Long,
    currentPositionMs: Long
): Boolean {
    return isPlaying &&
        bvid.isNotBlank() &&
        cid > 0L &&
        currentPositionMs >= 0L
}
