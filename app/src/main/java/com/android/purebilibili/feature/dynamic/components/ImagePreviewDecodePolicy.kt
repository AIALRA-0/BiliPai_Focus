package com.android.purebilibili.feature.dynamic.components

internal data class ImageDecodeSize(
    val widthPx: Int,
    val heightPx: Int
)

internal enum class ImageDecodeTarget {
    FULLSCREEN_PREVIEW,
    COMMENT_THUMBNAIL,
    /** 查看原图：按原图全分辨率解码（上限覆盖 B 站图床上传常见尺寸）。 */
    ORIGINAL_QUALITY
}

private const val FULLSCREEN_PREVIEW_MAX_EDGE_PX = 4096
private const val COMMENT_THUMBNAIL_MAX_EDGE_PX = 1024
private const val ORIGINAL_QUALITY_MAX_EDGE_PX = 8192

internal fun resolveImageDecodeSize(target: ImageDecodeTarget): ImageDecodeSize {
    val maxEdgePx = when (target) {
        ImageDecodeTarget.FULLSCREEN_PREVIEW -> FULLSCREEN_PREVIEW_MAX_EDGE_PX
        ImageDecodeTarget.COMMENT_THUMBNAIL -> COMMENT_THUMBNAIL_MAX_EDGE_PX
        ImageDecodeTarget.ORIGINAL_QUALITY -> ORIGINAL_QUALITY_MAX_EDGE_PX
    }
    return ImageDecodeSize(widthPx = maxEdgePx, heightPx = maxEdgePx)
}

internal fun estimateArgb8888ByteCount(size: ImageDecodeSize): Long {
    return size.widthPx.toLong() * size.heightPx.toLong() * 4L
}
