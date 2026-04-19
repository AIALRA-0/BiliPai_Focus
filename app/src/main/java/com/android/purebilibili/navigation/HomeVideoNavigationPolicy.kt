package com.android.purebilibili.navigation

import com.android.purebilibili.feature.home.HomeVideoClickRequest
import com.android.purebilibili.feature.home.HomeVideoClickSource
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal data class HomeVideoNavigationIntent(
    val bvid: String,
    val cid: Long,
    val coverUrl: String,
    val source: HomeVideoClickSource
)

internal sealed interface HomeNavigationTarget {
    data class Video(val route: String) : HomeNavigationTarget
    data class DynamicDetail(val dynamicId: String) : HomeNavigationTarget
}

internal fun resolveHomeVideoNavigationIntent(
    request: HomeVideoClickRequest
): HomeVideoNavigationIntent? {
    val normalizedBvid = request.bvid.trim()
    if (normalizedBvid.isEmpty()) return null

    return HomeVideoNavigationIntent(
        bvid = normalizedBvid,
        cid = request.cid.takeIf { it > 0L } ?: 0L,
        coverUrl = request.coverUrl,
        source = request.source
    )
}

internal fun resolveHomeVideoRoute(request: HomeVideoClickRequest): String? {
    val intent = resolveHomeVideoNavigationIntent(request) ?: return null
    val encodedCover = URLEncoder.encode(intent.coverUrl, StandardCharsets.UTF_8.toString())
    return VideoRoute.resolveVideoRoutePath(
        bvid = intent.bvid,
        cid = intent.cid,
        encodedCover = encodedCover,
        startAudio = false,
        autoPortrait = true,
        fullscreen = false
    )
}

internal fun resolveHomeNavigationTarget(
    request: HomeVideoClickRequest
): HomeNavigationTarget? {
    val normalizedDynamicId = request.dynamicId.trim()
    val normalizedBvid = request.bvid.trim()

    // 非 BV 的占位 bvid（例如动态卡片）优先走动态详情
    if (normalizedDynamicId.isNotEmpty() && !normalizedBvid.startsWith("BV", ignoreCase = true)) {
        return HomeNavigationTarget.DynamicDetail(normalizedDynamicId)
    }

    val videoRoute = resolveHomeVideoRoute(request)
    if (videoRoute != null) {
        return HomeNavigationTarget.Video(videoRoute)
    }

    if (normalizedDynamicId.isNotEmpty()) {
        return HomeNavigationTarget.DynamicDetail(normalizedDynamicId)
    }

    return null
}
