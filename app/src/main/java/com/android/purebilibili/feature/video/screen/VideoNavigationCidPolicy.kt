package com.android.purebilibili.feature.video.screen

import android.os.Bundle
import com.android.purebilibili.data.model.response.RelatedVideo
import com.android.purebilibili.data.model.response.UgcSeason

internal const val VIDEO_NAV_TARGET_CID_KEY = "video_nav_target_cid"
internal const val VIDEO_NAV_COVER_URL_KEY = "video_nav_cover_url"

internal fun buildVideoNavigationOptions(
    base: Bundle? = null,
    targetCid: Long = 0L,
    coverUrl: String? = null
): Bundle? {
    val normalizedCover = coverUrl?.trim().orEmpty()
    if (targetCid <= 0L && normalizedCover.isEmpty()) return base
    return Bundle().apply {
        if (base != null) {
            putAll(base)
        }
        if (targetCid > 0L) {
            putLong(VIDEO_NAV_TARGET_CID_KEY, targetCid)
        }
        if (normalizedCover.isNotEmpty()) {
            putString(VIDEO_NAV_COVER_URL_KEY, normalizedCover)
        }
    }
}

internal fun resolveNavigationTargetCid(
    targetBvid: String,
    explicitCid: Long,
    relatedVideos: List<RelatedVideo> = emptyList(),
    ugcSeason: UgcSeason?
): Long {
    if (explicitCid > 0L) return explicitCid
    if (targetBvid.isBlank()) return 0L
    relatedVideos.firstOrNull { video -> video.bvid == targetBvid }
        ?.cid
        ?.takeIf { it > 0L }
        ?.let { return it }
    return ugcSeason
        ?.sections
        ?.asSequence()
        ?.flatMap { section -> section.episodes.asSequence() }
        ?.firstOrNull { episode -> episode.bvid == targetBvid }
        ?.cid
        ?.takeIf { it > 0L }
        ?: 0L
}
