package com.android.purebilibili.feature.video.screen

import android.os.Bundle
import com.android.purebilibili.data.model.response.RelatedVideo
import com.android.purebilibili.data.model.response.UgcSeason

internal const val VIDEO_NAV_TARGET_CID_KEY = "video_nav_target_cid"
internal const val VIDEO_NAV_COVER_URL_KEY = "video_nav_cover_url"

/**
 * 「下一集 / 相关视频」在全屏播放中点击时，把当时的全屏意图带给新详情页。
 * 新页据此把 bvid 当作 `startInFullscreen` 处理，否则方向请求会回落
 * `SCREEN_ORIENTATION_USER`，表现为切视频时退出全屏。
 */
internal const val VIDEO_NAV_START_FULLSCREEN_KEY = "video_nav_start_fullscreen"

/**
 * 「相关推荐 / 下一集」跳转时带上目标视频的横竖屏结论。
 * 列表阶段已预取过判定，带上它新详情页首帧就能进入正确布局，不必等播放器接口回包。
 */
internal const val VIDEO_NAV_IS_VERTICAL_KEY = "video_nav_is_vertical"

internal fun resolveVideoNavigationStartInFullscreen(options: Bundle?): Boolean =
    options?.getBoolean(VIDEO_NAV_START_FULLSCREEN_KEY, false) == true

/** 读取跳转时携带的横竖屏结论；未携带返回 null，由新详情页自行判断。 */
internal fun resolveVideoNavigationIsVertical(options: Bundle?): Boolean? =
    options?.let {
        if (it.containsKey(VIDEO_NAV_IS_VERTICAL_KEY)) {
            it.getBoolean(VIDEO_NAV_IS_VERTICAL_KEY)
        } else {
            null
        }
    }

internal fun buildVideoNavigationOptions(
    base: Bundle? = null,
    targetCid: Long = 0L,
    coverUrl: String? = null,
    startInFullscreen: Boolean = false,
    isVertical: Boolean? = null,
): Bundle? {
    val normalizedCover = coverUrl?.trim().orEmpty()
    if (targetCid <= 0L && normalizedCover.isEmpty() && !startInFullscreen && isVertical == null) return base
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
        if (startInFullscreen) {
            putBoolean(VIDEO_NAV_START_FULLSCREEN_KEY, true)
        }
        if (isVertical != null) {
            putBoolean(VIDEO_NAV_IS_VERTICAL_KEY, isVertical)
        }
    }
}

/** 目标视频封面：优先合集集封面，其次推荐列表，供新详情页承接首帧前的封面。 */
internal fun resolveNavigationTargetCoverUrl(
    targetBvid: String,
    relatedVideos: List<RelatedVideo> = emptyList(),
    ugcSeason: UgcSeason?,
    targetCid: Long = 0L
): String {
    val normalizedBvid = targetBvid.trim()
    if (normalizedBvid.isBlank()) return ""
    val seasonCover = resolveUgcSeasonEpisodeCoverUrl(
        ugcSeason = ugcSeason,
        targetBvid = normalizedBvid,
        targetCid = targetCid
    )
    if (seasonCover.isNotBlank()) return seasonCover
    return relatedVideos
        .firstOrNull { video -> video.bvid == normalizedBvid }
        ?.pic
        ?.trim()
        .orEmpty()
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
