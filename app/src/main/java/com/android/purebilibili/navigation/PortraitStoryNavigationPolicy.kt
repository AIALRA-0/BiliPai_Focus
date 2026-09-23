package com.android.purebilibili.navigation

import com.android.purebilibili.data.model.response.resolveKnownVerticalVideo

internal data class PortraitStoryNavigationSeed(
    val bvid: String,
    val cid: Long,
    val coverUrl: String
)

/**
 * 是否短路进 Story 刷视频页。
 *
 * 开启卡片过渡时**不要**进 Story：应走 VideoDetail 的 CARD_SHELL 逐渐放大，
 * 再靠 [resolveDirectPortraitDetailMorphEntry] + standalone pager 进竖屏全屏。
 * Story sharedBounds 全屏几何对不上，会出现半屏叠层并弄坏播放器。
 */
internal fun resolvePortraitStoryNavigationSeed(
    directPortraitStoryEntry: Boolean,
    isVerticalVideo: Boolean,
    startAudio: Boolean,
    bvid: String,
    cid: Long = 0L,
    coverUrl: String = "",
    verticalRatioThreshold: Float = 1.0f,
    cardTransitionEnabled: Boolean = false,
): PortraitStoryNavigationSeed? {
    if (cardTransitionEnabled) return null
    val normalizedBvid = bvid.trim()
    val resolvedVertical = resolveKnownVerticalVideo(
        isVerticalVideo = isVerticalVideo,
        coverUrl = coverUrl,
        verticalRatioThreshold = verticalRatioThreshold
    )
    if (!directPortraitStoryEntry || !resolvedVertical || startAudio || normalizedBvid.isEmpty()) {
        return null
    }
    return PortraitStoryNavigationSeed(
        bvid = normalizedBvid,
        cid = cid.coerceAtLeast(0L),
        coverUrl = coverUrl
    )
}

/**
 * 开启「竖屏直达」且卡片过渡开启时：走详情页 sharedBounds 放大，并标记直达全屏竖屏流。
 * 与首页横视频同一套 CARD_SHELL；不要把 home 常开的 autoPortrait 误当成直达意图。
 */
internal fun resolveDirectPortraitDetailMorphEntry(
    directPortraitStoryEntry: Boolean,
    cardTransitionEnabled: Boolean,
    isVerticalVideo: Boolean,
    coverUrl: String = "",
    startAudio: Boolean = false,
): Boolean {
    if (!directPortraitStoryEntry || !cardTransitionEnabled || startAudio) return false
    return resolveKnownVerticalVideo(
        isVerticalVideo = isVerticalVideo,
        coverUrl = coverUrl,
    )
}
