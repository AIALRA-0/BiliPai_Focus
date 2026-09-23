package com.android.purebilibili.core.ui.wallpaper

import kotlin.math.abs

data class ProfileWallpaperTransform(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
)

private const val PROFILE_WALLPAPER_MIN_SCALE = 1f
private const val PROFILE_WALLPAPER_MAX_SCALE = 3f

fun sanitizeProfileWallpaperTransform(
    transform: ProfileWallpaperTransform
): ProfileWallpaperTransform {
    val safeScale = transform.scale.takeIf { it.isFinite() } ?: PROFILE_WALLPAPER_MIN_SCALE
    val safeOffsetX = transform.offsetX.takeIf { it.isFinite() } ?: 0f
    val safeOffsetY = transform.offsetY.takeIf { it.isFinite() } ?: 0f
    return ProfileWallpaperTransform(
        scale = safeScale.coerceIn(PROFILE_WALLPAPER_MIN_SCALE, PROFILE_WALLPAPER_MAX_SCALE),
        offsetX = safeOffsetX.coerceIn(-1f, 1f),
        offsetY = safeOffsetY.coerceIn(-1f, 1f)
    )
}

fun applyGestureToProfileWallpaperTransform(
    current: ProfileWallpaperTransform,
    panX: Float,
    panY: Float,
    zoomChange: Float,
    containerWidthPx: Float,
    containerHeightPx: Float
): ProfileWallpaperTransform {
    val width = containerWidthPx.takeIf { it.isFinite() && abs(it) > 0.5f } ?: 1f
    val height = containerHeightPx.takeIf { it.isFinite() && abs(it) > 0.5f } ?: 1f
    val nextScale = (current.scale * (zoomChange.takeIf { it.isFinite() } ?: 1f))
        .coerceIn(PROFILE_WALLPAPER_MIN_SCALE, PROFILE_WALLPAPER_MAX_SCALE)
    val nextOffsetX = current.offsetX + (panX / width) * 2f
    val nextOffsetY = current.offsetY + (panY / height) * 2f
    return sanitizeProfileWallpaperTransform(
        ProfileWallpaperTransform(
            scale = nextScale,
            offsetX = nextOffsetX,
            offsetY = nextOffsetY
        )
    )
}
