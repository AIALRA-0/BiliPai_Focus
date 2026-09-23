package com.android.purebilibili.core.ui.transition

private const val HOME_CATEGORY_SOURCE_PREFIX = "home?category="

/**
 * 共享元素 / CardPosition / VideoDetail.sourceRoute 统一归一化：
 * 去掉 title 等 query，避免合集详情 `season_series_detail/...?title=` 与详情页剥 query 后 key 对不上。
 */
internal fun normalizeSharedElementSourceRoute(sourceRoute: String?): String? {
    val normalized = sourceRoute?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return if (normalized.startsWith(HOME_CATEGORY_SOURCE_PREFIX)) {
        normalized
    } else {
        normalized.substringBefore("?")
    }
}

internal sealed interface BiliPaiSharedElementKey {
    val sourceRoute: String?

    data class Video(
        val bvid: String,
        val element: VideoSharedElement,
        override val sourceRoute: String? = null
    ) : BiliPaiSharedElementKey

    data class Live(
        val roomId: Long,
        override val sourceRoute: String? = null
    ) : BiliPaiSharedElementKey

    data class Avatar(
        val mid: Long,
        override val sourceRoute: String? = null
    ) : BiliPaiSharedElementKey

    data class ArticleCover(
        val articleId: Long,
        override val sourceRoute: String? = null
    ) : BiliPaiSharedElementKey

    data class Raw(
        val namespace: String,
        val id: String,
        override val sourceRoute: String? = null
    ) : BiliPaiSharedElementKey
}

internal enum class VideoSharedElement {
    CARD_SHELL,
    COVER,
    PLAYER,
    TITLE,
    UP_NAME,
    UP_ACTION,
    AVATAR,
    VIEWS,
    DANMAKU,
    DURATION
}

internal fun videoCardShellSharedElementKey(
    bvid: String,
    sourceRoute: String? = null
): BiliPaiSharedElementKey.Video {
    return BiliPaiSharedElementKey.Video(
        bvid = bvid,
        element = VideoSharedElement.CARD_SHELL,
        sourceRoute = normalizeSharedElementSourceRoute(sourceRoute)
    )
}

internal fun videoCoverSharedElementKey(
    bvid: String,
    sourceRoute: String? = null
): BiliPaiSharedElementKey.Video {
    return BiliPaiSharedElementKey.Video(
        bvid = bvid,
        element = VideoSharedElement.COVER,
        sourceRoute = normalizeSharedElementSourceRoute(sourceRoute)
    )
}

internal fun videoPlayerSharedElementKey(
    bvid: String,
    sourceRoute: String? = null
): BiliPaiSharedElementKey.Video {
    return BiliPaiSharedElementKey.Video(
        bvid = bvid,
        element = VideoSharedElement.PLAYER,
        sourceRoute = normalizeSharedElementSourceRoute(sourceRoute)
    )
}

internal fun videoTitleSharedElementKey(
    bvid: String,
    sourceRoute: String? = null
): BiliPaiSharedElementKey.Video {
    return BiliPaiSharedElementKey.Video(
        bvid = bvid,
        element = VideoSharedElement.TITLE,
        sourceRoute = normalizeSharedElementSourceRoute(sourceRoute)
    )
}

internal fun videoUpNameSharedElementKey(
    bvid: String,
    sourceRoute: String? = null
): BiliPaiSharedElementKey.Video {
    return BiliPaiSharedElementKey.Video(
        bvid = bvid,
        element = VideoSharedElement.UP_NAME,
        sourceRoute = normalizeSharedElementSourceRoute(sourceRoute)
    )
}

internal fun videoUpActionSharedElementKey(
    bvid: String,
    sourceRoute: String? = null
): BiliPaiSharedElementKey.Video {
    return BiliPaiSharedElementKey.Video(
        bvid = bvid,
        element = VideoSharedElement.UP_ACTION,
        sourceRoute = normalizeSharedElementSourceRoute(sourceRoute)
    )
}

internal fun videoAvatarSharedElementKey(
    bvid: String,
    sourceRoute: String? = null
): BiliPaiSharedElementKey.Video {
    return BiliPaiSharedElementKey.Video(
        bvid = bvid,
        element = VideoSharedElement.AVATAR,
        sourceRoute = normalizeSharedElementSourceRoute(sourceRoute)
    )
}

internal fun videoViewsSharedElementKey(
    bvid: String,
    sourceRoute: String? = null
): BiliPaiSharedElementKey.Video {
    return BiliPaiSharedElementKey.Video(
        bvid = bvid,
        element = VideoSharedElement.VIEWS,
        sourceRoute = normalizeSharedElementSourceRoute(sourceRoute)
    )
}

internal fun videoDanmakuSharedElementKey(
    bvid: String,
    sourceRoute: String? = null
): BiliPaiSharedElementKey.Video {
    return BiliPaiSharedElementKey.Video(
        bvid = bvid,
        element = VideoSharedElement.DANMAKU,
        sourceRoute = normalizeSharedElementSourceRoute(sourceRoute)
    )
}

internal fun videoDurationSharedElementKey(
    bvid: String,
    sourceRoute: String? = null
): BiliPaiSharedElementKey.Video {
    return BiliPaiSharedElementKey.Video(
        bvid = bvid,
        element = VideoSharedElement.DURATION,
        sourceRoute = normalizeSharedElementSourceRoute(sourceRoute)
    )
}

internal fun liveCoverSharedElementKey(
    roomId: Long,
    sourceRoute: String? = null
): BiliPaiSharedElementKey.Live {
    return BiliPaiSharedElementKey.Live(
        roomId = roomId,
        sourceRoute = normalizeSharedElementSourceRoute(sourceRoute)
    )
}

internal fun avatarSharedElementKey(
    mid: Long,
    sourceRoute: String? = null
): BiliPaiSharedElementKey.Avatar {
    return BiliPaiSharedElementKey.Avatar(
        mid = mid,
        sourceRoute = normalizeSharedElementSourceRoute(sourceRoute)
    )
}

internal fun articleCoverSharedElementKey(
    articleId: Long,
    sourceRoute: String? = null
): BiliPaiSharedElementKey.ArticleCover {
    return BiliPaiSharedElementKey.ArticleCover(
        articleId = articleId,
        sourceRoute = normalizeSharedElementSourceRoute(sourceRoute)
    )
}
