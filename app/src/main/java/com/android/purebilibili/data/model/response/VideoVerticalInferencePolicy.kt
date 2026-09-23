package com.android.purebilibili.data.model.response

private val COVER_SIZE_PATTERN = Regex("""@(\d+)w_(\d+)h""")

internal fun inferVerticalVideoFromCoverUrl(
    coverUrl: String,
    verticalRatioThreshold: Float = 1.0f
): Boolean {
    val match = COVER_SIZE_PATTERN.find(coverUrl) ?: return false
    val width = match.groupValues[1].toIntOrNull() ?: return false
    val height = match.groupValues[2].toIntOrNull() ?: return false
    if (width <= 0 || height <= 0) return false
    return height.toFloat() / width.toFloat() > verticalRatioThreshold
}

internal fun resolveKnownVerticalVideo(
    isVerticalVideo: Boolean,
    coverUrl: String,
    verticalRatioThreshold: Float = 1.0f
): Boolean {
    return isVerticalVideo || inferVerticalVideoFromCoverUrl(coverUrl, verticalRatioThreshold)
}

internal fun resolveFavoriteVideoVertical(
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

internal fun shouldResolveVerticalVideoForPortraitEntry(
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