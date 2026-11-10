package com.android.purebilibili.core.ui.wallpaper

import kotlin.math.max

/** Normalized focal alignment: -1 selects the left/top edge; +1 the right/bottom edge. */
data class WallpaperTransform(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
)

fun sanitizeWallpaperTransform(value: WallpaperTransform): WallpaperTransform = WallpaperTransform(
    scale = value.scale.takeIf(Float::isFinite)?.coerceIn(1f, 3f) ?: 1f,
    offsetX = value.offsetX.takeIf(Float::isFinite)?.coerceIn(-1f, 1f) ?: 0f,
    offsetY = value.offsetY.takeIf(Float::isFinite)?.coerceIn(-1f, 1f) ?: 0f,
)

data class WallpaperCropGeometry(val width: Float, val height: Float, val left: Float, val top: Float)

data class WallpaperSourceRegion(val left: Float, val top: Float, val right: Float, val bottom: Float)

/** Source-pixel region visible after the same crop/zoom used by WallpaperMedia. */
fun resolveWallpaperVisibleSourceRegion(
    viewportWidth: Float,
    viewportHeight: Float,
    sourceWidth: Float,
    sourceHeight: Float,
    transform: WallpaperTransform,
): WallpaperSourceRegion {
    val sw = sourceWidth.takeIf { it.isFinite() && it > 0f } ?: 1f
    val sh = sourceHeight.takeIf { it.isFinite() && it > 0f } ?: 1f
    val width = viewportWidth.takeIf { it.isFinite() && it > 0f } ?: 1f
    val height = viewportHeight.takeIf { it.isFinite() && it > 0f } ?: 1f
    val crop = resolveWallpaperCropGeometry(width, height, sw, sh, transform)
    val scale = crop.width / sw
    return WallpaperSourceRegion(
        left = (-crop.left / scale).coerceIn(0f, sw),
        top = (-crop.top / scale).coerceIn(0f, sh),
        right = ((width - crop.left) / scale).coerceIn(0f, sw),
        bottom = ((height - crop.top) / scale).coerceIn(0f, sh),
    )
}

/** Shared by image/video layout and preview gestures. No viewport edge can expose empty space. */
fun resolveWallpaperCropGeometry(
    viewportWidth: Float,
    viewportHeight: Float,
    sourceWidth: Float,
    sourceHeight: Float,
    transform: WallpaperTransform,
): WallpaperCropGeometry {
    val width = viewportWidth.takeIf { it.isFinite() && it > 0f } ?: 1f
    val height = viewportHeight.takeIf { it.isFinite() && it > 0f } ?: 1f
    val sw = sourceWidth.takeIf { it.isFinite() && it > 0f } ?: width
    val sh = sourceHeight.takeIf { it.isFinite() && it > 0f } ?: height
    val safe = sanitizeWallpaperTransform(transform)
    val cropScale = max(width / sw, height / sh) * safe.scale
    val renderedWidth = max(width, sw * cropScale)
    val renderedHeight = max(height, sh * cropScale)
    return WallpaperCropGeometry(
        width = renderedWidth,
        height = renderedHeight,
        left = if (renderedWidth <= width || safe.offsetX == -1f) 0f
        else -(renderedWidth - width) * (safe.offsetX + 1f) / 2f,
        top = if (renderedHeight <= height || safe.offsetY == -1f) 0f
        else -(renderedHeight - height) * (safe.offsetY + 1f) / 2f,
    )
}

fun applyWallpaperGesture(
    current: WallpaperTransform,
    panX: Float,
    panY: Float,
    zoomChange: Float,
    viewportWidth: Float,
    viewportHeight: Float,
    sourceWidth: Float,
    sourceHeight: Float,
): WallpaperTransform {
    val safe = sanitizeWallpaperTransform(current)
    val zoom = zoomChange.takeIf { it.isFinite() && it > 0f } ?: 1f
    val scaled = sanitizeWallpaperTransform(safe.copy(scale = safe.scale * zoom))
    val crop = resolveWallpaperCropGeometry(viewportWidth, viewportHeight, sourceWidth, sourceHeight, scaled)
    fun panOffset(offset: Float, pan: Float, overflow: Float): Float =
        if (overflow > 0.5f && pan.isFinite()) offset - 2f * pan / overflow else offset
    return sanitizeWallpaperTransform(scaled.copy(
        offsetX = panOffset(scaled.offsetX, panX, crop.width - viewportWidth),
        offsetY = panOffset(scaled.offsetY, panY, crop.height - viewportHeight),
    ))
}
