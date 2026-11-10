package com.android.purebilibili.feature.dynamic.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.min

/** Pan bounds use the fitted bitmap, rather than the viewport's aspect ratio. */
internal fun resolveZoomableImagePanBounds(
    imageSize: IntSize,
    containerSize: IntSize,
    scale: Float,
): Offset {
    if (imageSize.width <= 0 || imageSize.height <= 0 ||
        containerSize.width <= 0 || containerSize.height <= 0
    ) return Offset.Zero
    val fitScale = min(
        containerSize.width.toFloat() / imageSize.width,
        containerSize.height.toFloat() / imageSize.height,
    )
    return Offset(
        max(0f, (imageSize.width * fitScale * scale - containerSize.width) / 2f),
        max(0f, (imageSize.height * fitScale * scale - containerSize.height) / 2f),
    )
}

internal fun clampZoomableImageOffset(offset: Offset, bounds: Offset): Offset = Offset(
    offset.x.coerceIn(-bounds.x, bounds.x),
    offset.y.coerceIn(-bounds.y, bounds.y),
)

/** Reject outward motion at an edge without discarding motion along the other axis. */
internal fun resolveZoomableImageFlingVelocity(
    velocity: Offset,
    offset: Offset,
    bounds: Offset,
    density: Float,
): Offset {
    fun axis(speed: Float, position: Float, bound: Float): Float {
        if (!speed.isFinite() || bound <= 0f ||
            (position >= bound && speed > 0f) || (position <= -bound && speed < 0f) ||
            kotlin.math.abs(speed) < 60f * density
        ) return 0f
        return speed.coerceIn(-4000f * density, 4000f * density)
    }
    return Offset(axis(velocity.x, offset.x, bounds.x), axis(velocity.y, offset.y, bounds.y))
}
