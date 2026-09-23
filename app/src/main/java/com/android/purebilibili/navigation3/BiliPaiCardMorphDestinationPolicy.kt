package com.android.purebilibili.navigation3

import com.android.purebilibili.navigation.isVideoCardReturnTargetRoute

/**
 * 卡片 sharedBounds / 景深过渡的目标页：普通 VideoDetail。
 *
 * Story「竖屏直达」不是 sharedBounds 目标（全屏几何对不上）；进场走普通 FALLBACK。
 */
internal fun resolveCardMorphDestinationSourceRoute(key: BiliPaiNavKey?): String? {
    return when (key) {
        is BiliPaiNavKey.VideoDetail -> key.sourceRoute
        else -> null
    }
}

internal fun isCardMorphDestinationNavKey(key: BiliPaiNavKey?): Boolean {
    val sourceRoute = resolveCardMorphDestinationSourceRoute(key) ?: return false
    return isVideoCardReturnTargetRoute(sourceRoute)
}

/** 仅 VideoDetail 走持续 sharedBounds NO_OP。 */
internal fun isSharedReadyCardMorphPush(
    key: BiliPaiNavKey,
    sourceMetadata: BiliPaiNavSourceMetadata,
): Boolean {
    if (!sourceMetadata.clickedBoundsRecorded || sourceMetadata.sourceRoute == null) {
        return false
    }
    return when (key) {
        is BiliPaiNavKey.VideoDetail ->
            sourceMetadata.sourceRoute == key.sourceRoute &&
                sourceMetadata.sourceKey == "${sourceMetadata.sourceRoute}:${key.bvid}"
        else -> false
    }
}

/**
 * Story 直达不再静音路由 fade：本地无 sharedBounds 承接时，NO_OP 会造成黑底/叠层。
 * 保留 API 供测试与调用方显式判断；恒为 false。
 */
internal fun shouldSilenceRouteTransitionForSeededStory(
    @Suppress("UNUSED_PARAMETER") key: BiliPaiNavKey,
): Boolean {
    return false
}
