package com.android.purebilibili.core.ui.transition

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
    DANMAKU
}

internal fun videoCardShellSharedElementKey(
    bvid: String,
    sourceRoute: String? = null
): BiliPaiSharedElementKey.Video {
    return BiliPaiSharedElementKey.Video(
        bvid = bvid,
        element = VideoSharedElement.CARD_SHELL,
        sourceRoute = sourceRoute
    )
}

internal fun videoCoverSharedElementKey(
    bvid: String,
    sourceRoute: String? = null
): BiliPaiSharedElementKey.Video {
    return BiliPaiSharedElementKey.Video(
        bvid = bvid,
        element = VideoSharedElement.COVER,
        sourceRoute = sourceRoute
    )
}

internal fun videoPlayerSharedElementKey(
    bvid: String,
    sourceRoute: String? = null
): BiliPaiSharedElementKey.Video {
    return BiliPaiSharedElementKey.Video(
        bvid = bvid,
        element = VideoSharedElement.PLAYER,
        sourceRoute = sourceRoute
    )
}

internal fun videoTitleSharedElementKey(
    bvid: String,
    sourceRoute: String? = null
): BiliPaiSharedElementKey.Video {
    return BiliPaiSharedElementKey.Video(
        bvid = bvid,
        element = VideoSharedElement.TITLE,
        sourceRoute = sourceRoute
    )
}

internal fun videoUpNameSharedElementKey(
    bvid: String,
    sourceRoute: String? = null
): BiliPaiSharedElementKey.Video {
    return BiliPaiSharedElementKey.Video(
        bvid = bvid,
        element = VideoSharedElement.UP_NAME,
        sourceRoute = sourceRoute
    )
}

internal fun videoUpActionSharedElementKey(
    bvid: String,
    sourceRoute: String? = null
): BiliPaiSharedElementKey.Video {
    return BiliPaiSharedElementKey.Video(
        bvid = bvid,
        element = VideoSharedElement.UP_ACTION,
        sourceRoute = sourceRoute
    )
}

internal fun videoAvatarSharedElementKey(
    bvid: String,
    sourceRoute: String? = null
): BiliPaiSharedElementKey.Video {
    return BiliPaiSharedElementKey.Video(
        bvid = bvid,
        element = VideoSharedElement.AVATAR,
        sourceRoute = sourceRoute
    )
}

internal fun videoViewsSharedElementKey(
    bvid: String,
    sourceRoute: String? = null
): BiliPaiSharedElementKey.Video {
    return BiliPaiSharedElementKey.Video(
        bvid = bvid,
        element = VideoSharedElement.VIEWS,
        sourceRoute = sourceRoute
    )
}

internal fun videoDanmakuSharedElementKey(
    bvid: String,
    sourceRoute: String? = null
): BiliPaiSharedElementKey.Video {
    return BiliPaiSharedElementKey.Video(
        bvid = bvid,
        element = VideoSharedElement.DANMAKU,
        sourceRoute = sourceRoute
    )
}

internal fun liveCoverSharedElementKey(
    roomId: Long,
    sourceRoute: String? = null
): BiliPaiSharedElementKey.Live {
    return BiliPaiSharedElementKey.Live(roomId = roomId, sourceRoute = sourceRoute)
}

internal fun avatarSharedElementKey(
    mid: Long,
    sourceRoute: String? = null
): BiliPaiSharedElementKey.Avatar {
    return BiliPaiSharedElementKey.Avatar(mid = mid, sourceRoute = sourceRoute)
}

internal fun articleCoverSharedElementKey(
    articleId: Long,
    sourceRoute: String? = null
): BiliPaiSharedElementKey.ArticleCover {
    return BiliPaiSharedElementKey.ArticleCover(articleId = articleId, sourceRoute = sourceRoute)
}
