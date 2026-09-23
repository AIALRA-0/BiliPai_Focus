package com.android.purebilibili.feature.list

import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.data.model.response.resolveKnownVerticalVideo

internal data class CommonListVideoNavigationRequest(
    val lookupKey: String,
    val bvid: String,
    val cid: Long,
    val coverUrl: String,
    val isVertical: Boolean = false
)

internal fun resolveCommonListVideoNavigationRequest(
    video: VideoItem,
    fallbackLookupKey: String? = null
): CommonListVideoNavigationRequest? {
    val normalizedBvid = video.bvid.trim()
    val normalizedLookupKey = normalizedBvid.ifEmpty {
        fallbackLookupKey?.trim().orEmpty()
    }
    if (normalizedLookupKey.isEmpty()) return null

    return CommonListVideoNavigationRequest(
        lookupKey = normalizedLookupKey,
        bvid = normalizedBvid,
        cid = video.cid.takeIf { it > 0L } ?: 0L,
        coverUrl = video.pic,
        isVertical = resolveKnownVerticalVideo(
            isVerticalVideo = video.isVertical,
            coverUrl = video.pic
        )
    )
}
