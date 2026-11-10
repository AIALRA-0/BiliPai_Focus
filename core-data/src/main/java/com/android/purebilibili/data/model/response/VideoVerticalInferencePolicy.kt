package com.android.purebilibili.data.model.response

private val COVER_SIZE_PATTERN = Regex("""@(\d+)w_(\d+)h""")
private val FEED_URI_QUERY_PATTERN = Regex("""[?&]([^=&]+)=([^&]*)""")

/**
 * 从推荐流 uri 的 query 里解析横竖屏。
 *
 * App 端推荐接口的 uri 形如
 * `bilibili://video/123456?player_width=1080&player_height=1920&player_rotate=0`，
 * 尺寸随列表一起下发，因此列表卡片与详情页首帧都能直接拿到结论，无需再请求详情接口。
 *
 * 未携带尺寸（网页端推荐、部分卡片类型）时返回 null，由调用方回退其它判定方式。
 */
fun resolveVerticalVideoFromFeedUri(uri: String): Boolean? {
    if (uri.isBlank()) return null
    val params = FEED_URI_QUERY_PATTERN.findAll(uri)
        .associate { it.groupValues[1] to it.groupValues[2] }
    val width = params["player_width"]?.toIntOrNull() ?: return null
    val height = params["player_height"]?.toIntOrNull() ?: return null
    if (width <= 0 || height <= 0) return null
    // rotate=1 表示素材本身是横向录制、播放时旋转 90°，此时宽高需要互换比较。
    val rotated = params["player_rotate"]?.toIntOrNull() == 1
    return if (rotated) width > height else height > width
}

fun inferVerticalVideoFromCoverUrl(
    coverUrl: String,
    verticalRatioThreshold: Float = 1.0f
): Boolean {
    val match = COVER_SIZE_PATTERN.find(coverUrl) ?: return false
    val width = match.groupValues[1].toIntOrNull() ?: return false
    val height = match.groupValues[2].toIntOrNull() ?: return false
    if (width <= 0 || height <= 0) return false
    return height.toFloat() / width.toFloat() > verticalRatioThreshold
}

fun resolveKnownVerticalVideo(
    isVerticalVideo: Boolean,
    coverUrl: String,
    verticalRatioThreshold: Float = 1.0f
): Boolean {
    return isVerticalVideo || inferVerticalVideoFromCoverUrl(coverUrl, verticalRatioThreshold)
}

fun resolveFavoriteVideoVertical(
    dimension: Dimension?,
    coverUrl: String,
    verticalRatioThreshold: Float = 1.0f
): Boolean {
    return resolveKnownVerticalVideo(
        isVerticalVideo = dimension?.isVertical == true,
        coverUrl = coverUrl,
        verticalRatioThreshold = verticalRatioThreshold
    )
}

fun shouldResolveVerticalVideoForPortraitEntry(
    directPortraitStoryEntry: Boolean,
    startAudio: Boolean,
    bvid: String,
    isVerticalVideo: Boolean,
    coverUrl: String,
    verticalRatioThreshold: Float = 1.0f
): Boolean {
    return directPortraitStoryEntry &&
        !startAudio &&
        bvid.trim().isNotEmpty() &&
        !resolveKnownVerticalVideo(
            isVerticalVideo = isVerticalVideo,
            coverUrl = coverUrl,
            verticalRatioThreshold = verticalRatioThreshold
        )
}