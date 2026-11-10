package com.android.purebilibili.feature.video.screen

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.transition.VideoCardSourceLayout
import com.android.purebilibili.core.ui.transition.resolveVideoCardSourceLayout
import kotlin.math.max
import kotlin.math.roundToInt

/** System-bar spacing belongs to the expanded page, never to the source-card endpoint. */
internal fun resolveVideoDetailCardTopInsetFraction(morphDepth: Float): Float =
    ((morphDepth - 0.9f) / 0.1f).coerceIn(0f, 1f)

/** Crop the host's fixed system-bar backdrop while the navigation card is floating. */
internal fun Modifier.videoDetailRemoveFloatingTopInset(
    removedInsetProvider: () -> Dp,
): Modifier = graphicsLayer {
    clip = removedInsetProvider() > 0.dp
}.layout { measurable, constraints ->
    if (!constraints.hasBoundedHeight) {
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    } else {
        val removed = removedInsetProvider().roundToPx().coerceAtLeast(0)
        val placeable = measurable.measure(constraints.copy(
            minHeight = constraints.minHeight + removed,
            maxHeight = constraints.maxHeight + removed,
        ))
        layout(placeable.width, placeable.height - removed) { placeable.place(0, -removed) }
    }
}

/** Click-time source geometry used only to align the real detail media with the source cover. */
internal data class VideoDetailReturnSourceCardLayout(
    val sourceScale: Float,
    val cardWidthPx: Float,
    val cardHeightPx: Float,
    val coverHeightPx: Float,
    val coverWidthPx: Float,
    val coverOffsetXPx: Float = 0f,
    val coverOffsetYPx: Float = 0f,
    val infoWidthPx: Float,
    val infoHeightPx: Float,
    val cardAnchorXInViewportPx: Float,
    val cardAnchorYInViewportPx: Float,
    val infoAnchorXInViewportPx: Float,
    val infoAnchorYInViewportPx: Float,
    val layout: VideoCardSourceLayout = VideoCardSourceLayout.COVER_ONLY,
) {
    val canRender: Boolean
        get() = sourceScale > 0f &&
            cardWidthPx > 1f &&
            cardHeightPx > 1f &&
            infoWidthPx > 1f &&
            infoHeightPx > 1f &&
            layout != VideoCardSourceLayout.COVER_ONLY

    @Deprecated("Use infoWidthPx", ReplaceWith("infoWidthPx"))
    val sourceWidthPx: Float get() = infoWidthPx

    @Deprecated("Use infoHeightPx", ReplaceWith("infoHeightPx"))
    val sourceInfoHeightPx: Float get() = infoHeightPx

    @Deprecated("Use infoAnchorYInViewportPx", ReplaceWith("infoAnchorYInViewportPx"))
    val anchorYInViewportPx: Float get() = infoAnchorYInViewportPx

    @Deprecated("Use infoAnchorXInViewportPx", ReplaceWith("infoAnchorXInViewportPx"))
    val anchorXInViewportPx: Float get() = infoAnchorXInViewportPx
}

private fun emptyReturnSourceLayout(
    layout: VideoCardSourceLayout = VideoCardSourceLayout.COVER_ONLY,
) = VideoDetailReturnSourceCardLayout(
    sourceScale = 0f,
    cardWidthPx = 0f,
    cardHeightPx = 0f,
    coverHeightPx = 0f,
    coverWidthPx = 0f,
    infoWidthPx = 0f,
    infoHeightPx = 0f,
    cardAnchorXInViewportPx = 0f,
    cardAnchorYInViewportPx = 0f,
    infoAnchorXInViewportPx = 0f,
    infoAnchorYInViewportPx = 0f,
    layout = layout,
)

/**
 * 重建壳信息板应覆盖的矩形（**相对卡片左上角**），null 表示这份实测数据不可用。
 *
 * 优先采用列表卡自己量出来的信息面（`onPlaced` / `boundsInRoot`）。它比「卡底 − 封面底」
 * 的减法推导可靠：点击瞬间卡片可能还没量到最终高度（或快照来自更紧凑的一帧），减法推导
 * 会把信息板做短，卡槽下半截就露出列表自己的信息面 —— 观感上就是一块盖不住的白带。
 */
private fun resolveVideoDetailReturnInfoBandFromMeasuredBounds(
    bounds: Rect,
    coverBounds: Rect,
    infoBounds: Rect?,
): Rect? {
    val measured = infoBounds?.takeIf { it.width > 1f && it.height > 1f } ?: return null
    // 信息面必须贴在封面下沿、整卡宽度、且不越出卡片；否则视为脏数据，回退减法推导。
    if (measured.top < coverBounds.bottom - 2f) return null
    if (measured.width < bounds.width * 0.6f) return null
    if (measured.bottom > bounds.bottom + 2f) return null
    val height = measured.height
    if (height <= 1f) return null
    return Rect(
        left = (measured.left - bounds.left).coerceAtLeast(0f),
        top = (measured.top - bounds.top).coerceAtLeast(0f),
        right = (measured.right - bounds.left).coerceAtLeast(0f),
        bottom = (measured.bottom - bounds.top).coerceAtLeast(0f),
    )
}

/** Resolves the measured source cover inside the full-width detail entry. */
internal fun resolveVideoDetailReturnSourceCardLayout(
    viewportWidthPx: Float,
    sourceBounds: Rect?,
    sourceCoverBounds: Rect?,
    sourceLayout: VideoCardSourceLayout? = null,
    sourceInfoBounds: Rect? = null,
): VideoDetailReturnSourceCardLayout {
    val viewportWidth = viewportWidthPx.coerceAtLeast(1f)
    val bounds = sourceBounds?.takeIf { it.width > 1f && it.height > 1f }
        ?: return emptyReturnSourceLayout()
    val coverBounds = sourceCoverBounds?.takeIf { it.width > 1f && it.height > 1f }
        ?: return emptyReturnSourceLayout()
    val layout = sourceLayout ?: resolveVideoCardSourceLayout(bounds, coverBounds)
    val sourceScale = (bounds.width / viewportWidth).coerceIn(0.01f, 1f)

    return when (layout) {
        VideoCardSourceLayout.STACKED -> {
            val horizontalTolerance = bounds.width * 0.1f
            val isFullWidthCover = coverBounds.left <= bounds.left + horizontalTolerance &&
                coverBounds.right >= bounds.right - horizontalTolerance
            val isVerticallyInsideCard = coverBounds.top >= bounds.top - 1f &&
                coverBounds.bottom in (bounds.top + 1f)..(bounds.bottom + 1f)
            if (!isFullWidthCover || !isVerticallyInsideCard) {
                return emptyReturnSourceLayout(layout)
            }
            val coverHeight = coverBounds.height.coerceAtLeast(0f)
            val coverOffsetY = (coverBounds.top - bounds.top).coerceAtLeast(0f)
            // 信息板落点：优先用列表卡实测的信息面，回退到「卡底 − 封面底」。
            val measuredInfoBand = resolveVideoDetailReturnInfoBandFromMeasuredBounds(
                bounds = bounds,
                coverBounds = coverBounds,
                infoBounds = sourceInfoBounds,
            )
            val infoTop = measuredInfoBand?.top ?: (coverOffsetY + coverHeight)
            val infoHeight = measuredInfoBand?.height
                ?: (bounds.bottom - coverBounds.bottom).coerceAtLeast(0f)
            if (infoHeight <= 1f || coverHeight <= 1f) {
                return emptyReturnSourceLayout(layout)
            }
            VideoDetailReturnSourceCardLayout(
                sourceScale = sourceScale,
                cardWidthPx = bounds.width,
                cardHeightPx = bounds.height,
                coverHeightPx = coverHeight,
                coverWidthPx = coverBounds.width.coerceAtMost(bounds.width),
                coverOffsetXPx = (coverBounds.left - bounds.left).coerceAtLeast(0f),
                coverOffsetYPx = coverOffsetY,
                infoWidthPx = bounds.width,
                infoHeightPx = infoHeight,
                cardAnchorXInViewportPx = 0f,
                cardAnchorYInViewportPx = 0f,
                infoAnchorXInViewportPx = 0f,
                infoAnchorYInViewportPx = infoTop / sourceScale,
                layout = layout,
            )
        }

        VideoCardSourceLayout.SIDE_BY_SIDE -> {
            val coverOnLeft = coverBounds.center.x <= bounds.center.x
            val coverNarrower = coverBounds.width < bounds.width * 0.85f
            val coverWidth: Float
            val coverHeight: Float
            val coverOffsetX: Float
            val coverOffsetY: Float
            val infoWidth: Float
            if (coverOnLeft && coverNarrower) {
                coverWidth = coverBounds.width.coerceAtLeast(1f)
                coverHeight = coverBounds.height
                    .coerceAtLeast(1f)
                    .coerceAtMost(bounds.height)
                coverOffsetX = (coverBounds.left - bounds.left).coerceAtLeast(0f)
                coverOffsetY = (coverBounds.top - bounds.top).coerceAtLeast(0f)
                infoWidth = (bounds.right - coverBounds.right).coerceAtLeast(0f)
            } else {
                coverWidth = bounds.width * 0.38f
                coverHeight = bounds.height * 0.85f
                coverOffsetX = 0f
                coverOffsetY = (bounds.height - coverHeight) / 2f
                infoWidth = bounds.width - coverWidth
            }
            val infoHeight = bounds.height.coerceAtLeast(0f)
            if (infoWidth <= 1f || infoHeight <= 1f || coverWidth <= 1f) {
                return emptyReturnSourceLayout(layout)
            }
            VideoDetailReturnSourceCardLayout(
                sourceScale = sourceScale,
                cardWidthPx = bounds.width,
                cardHeightPx = bounds.height,
                coverHeightPx = coverHeight,
                coverWidthPx = coverWidth,
                coverOffsetXPx = coverOffsetX,
                coverOffsetYPx = coverOffsetY,
                infoWidthPx = infoWidth,
                infoHeightPx = infoHeight,
                cardAnchorXInViewportPx = 0f,
                cardAnchorYInViewportPx = 0f,
                infoAnchorXInViewportPx = (coverOffsetX + coverWidth) / sourceScale,
                infoAnchorYInViewportPx = 0f,
                layout = layout,
            )
        }

        VideoCardSourceLayout.COVER_ONLY -> emptyReturnSourceLayout(layout)
    }
}

internal fun resolveVideoDetailReturnCoverHeightInEntryPx(
    layout: VideoDetailReturnSourceCardLayout,
): Float {
    if (!layout.canRender) return 0f
    val landingEdgeOverscanPx = if (layout.layout == VideoCardSourceLayout.SIDE_BY_SIDE) {
        1f
    } else {
        0f
    }
    return (layout.coverHeightPx + landingEdgeOverscanPx) / layout.sourceScale
}

internal fun resolveVideoDetailReturnCoverWidthInEntryPx(
    layout: VideoDetailReturnSourceCardLayout,
): Float = if (layout.canRender) layout.coverWidthPx / layout.sourceScale else 0f

internal fun resolveVideoDetailReturnCoverOffsetXInEntryPx(
    layout: VideoDetailReturnSourceCardLayout,
): Float = if (layout.canRender) layout.coverOffsetXPx / layout.sourceScale else 0f

internal fun resolveVideoDetailReturnCoverOffsetYInEntryPx(
    layout: VideoDetailReturnSourceCardLayout,
): Float = if (layout.canRender) layout.coverOffsetYPx / layout.sourceScale else 0f

internal data class VideoDetailReturnMediaLayoutFrame(
    val offsetXPx: Int,
    val offsetYPx: Int,
    val widthPx: Int,
    val heightPx: Int,
)

/** Shares measured viewport geometry, without publishing animation frames as snapshot state. */
internal class VideoDetailReturnMediaGeometry {
    val viewportSize = androidx.compose.runtime.mutableStateOf(androidx.compose.ui.unit.IntSize.Zero)
    var frameProvider: () -> VideoDetailReturnMediaLayoutFrame? = { null }
}

/** Keep the source gap while attaching the info band to the current, rounded media edge. */
internal fun resolveVideoDetailStackedInfoTopPx(
    layout: VideoDetailReturnSourceCardLayout,
    inverseScaleY: Float,
    mediaFrame: VideoDetailReturnMediaLayoutFrame?,
): Int {
    val infoTop = layout.infoAnchorYInViewportPx * layout.sourceScale
    if (mediaFrame == null) return (infoTop * inverseScaleY).roundToInt()
    val gap = infoTop - (layout.coverOffsetYPx + layout.coverHeightPx)
    return mediaFrame.offsetYPx + mediaFrame.heightPx + (gap * inverseScaleY).roundToInt()
}

/** Source geometry is entry-relative; a split-pane player starts in a smaller local slot. */
internal fun resolveVideoDetailReturnMediaFrameInSlot(
    frame: VideoDetailReturnMediaLayoutFrame,
    slotOrigin: Offset,
    handoffProgress: Float,
): VideoDetailReturnMediaLayoutFrame {
    val progress = handoffProgress.coerceIn(0f, 1f)
    if (slotOrigin == Offset.Zero || progress == 0f) return frame
    return frame.copy(
        offsetXPx = (frame.offsetXPx - slotOrigin.x * progress).roundToInt(),
        offsetYPx = (frame.offsetYPx - slotOrigin.y * progress).roundToInt(),
    )
}

internal fun resolveVideoDetailReturnMediaLayoutFrame(
    containerWidthPx: Int,
    containerHeightPx: Int,
    landingLayout: VideoDetailReturnSourceCardLayout?,
    handoffProgress: Float,
    inverseScaleX: Float = landingLayout?.let { 1f / it.sourceScale } ?: 1f,
    inverseScaleY: Float = inverseScaleX,
    contentTopInsetPx: Int = 0,
    nativeSnapshotBounds: Rect? = null,
): VideoDetailReturnMediaLayoutFrame {
    val safeContainerWidth = containerWidthPx.coerceAtLeast(1)
    val safeContainerHeight = containerHeightPx.coerceAtLeast(1)
    val contentTop = contentTopInsetPx.coerceIn(0, safeContainerHeight - 1)
    val landing = landingLayout?.takeIf { it.canRender }
    val nativeTarget = nativeSnapshotBounds?.takeIf { it.width > 1f && it.height > 1f }
    val progress = if (landing == null && nativeTarget == null) {
        0f
    } else {
        handoffProgress.coerceIn(0f, 1f)
    }
    fun interpolate(start: Float, end: Float): Int =
        (start + (end - start) * progress).roundToInt()

    val safeInverseX = inverseScaleX.coerceAtLeast(0.01f)
    val safeInverseY = inverseScaleY.coerceAtLeast(0.01f)
    val landingEdgeOverscanPx = if (landing?.layout == VideoCardSourceLayout.SIDE_BY_SIDE) {
        1f
    } else {
        0f
    }
    // Native bounds are host-space metadata. Inside the flying entry the complete capsule
    // starts at (0, 0), with inverse compensation applied once by this layout.
    val targetWidth = nativeTarget?.let { it.width * safeInverseX }
        ?: landing
        ?.let { it.coverWidthPx * safeInverseX }
        ?.takeIf { it > 1f }
        ?: safeContainerWidth.toFloat()
    val targetHeight = nativeTarget?.let { it.height * safeInverseY }
        ?: landing
        ?.let { (it.coverHeightPx + landingEdgeOverscanPx) * safeInverseY }
        ?.takeIf { it > 1f }
        ?: safeContainerHeight.toFloat()
    val targetOffsetX = nativeTarget?.let { 0f }
        ?: landing?.let { it.coverOffsetXPx * safeInverseX }
        ?: 0f
    val targetOffsetY = nativeTarget?.let { 0f }
        ?: landing?.let { it.coverOffsetYPx * safeInverseY }
        ?: 0f

    val offsetY = interpolate(contentTop.toFloat(), targetOffsetY)
    // Round the shared edge once. Rounding offset and height independently can leave
    // a one-pixel seam when the cover includes a changing system-bar inset.
    val bottomY = interpolate(safeContainerHeight.toFloat(), targetOffsetY + targetHeight)
    return VideoDetailReturnMediaLayoutFrame(
        offsetXPx = interpolate(0f, targetOffsetX),
        offsetYPx = offsetY,
        widthPx = interpolate(safeContainerWidth.toFloat(), targetWidth).coerceAtLeast(1),
        heightPx = (bottomY - offsetY).coerceAtLeast(1),
    )
}

/** Remeasures only the detail media to the real source cover; it never redraws card chrome. */
internal fun Modifier.videoDetailReturnMediaLayout(
    landingLayout: VideoDetailReturnSourceCardLayout?,
    handoffProgressProvider: () -> Float,
    inverseScaleXProvider: () -> Float = {
        landingLayout?.let { 1f / it.sourceScale } ?: 1f
    },
    inverseScaleYProvider: () -> Float = inverseScaleXProvider,
    contentTopInset: Dp = 0.dp,
    clipCornerDp: Dp = 0.dp,
    nativeSnapshotBoundsProvider: (() -> Rect?)? = null,
    slotOriginProvider: () -> Offset = { Offset.Zero },
    contentTopInsetProvider: () -> Dp = { contentTopInset },
): Modifier = layout { measurable, constraints ->
    if (!constraints.hasBoundedWidth || !constraints.hasBoundedHeight) {
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    } else {
        val progress = handoffProgressProvider()
        val entryFrame = resolveVideoDetailReturnMediaLayoutFrame(
            containerWidthPx = constraints.maxWidth,
            containerHeightPx = constraints.maxHeight,
            landingLayout = landingLayout,
            handoffProgress = progress,
            inverseScaleX = inverseScaleXProvider(),
            inverseScaleY = inverseScaleYProvider(),
            contentTopInsetPx = contentTopInsetProvider().roundToPx(),
            nativeSnapshotBounds = nativeSnapshotBoundsProvider?.invoke(),
        )
        val frame = resolveVideoDetailReturnMediaFrameInSlot(entryFrame, slotOriginProvider(), progress)
        val placeable = measurable.measure(
            Constraints.fixed(width = frame.widthPx, height = frame.heightPx),
        )
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.place(frame.offsetXPx, frame.offsetYPx)
        }
    }
}.then(
    if (clipCornerDp > 0.dp) Modifier.graphicsLayer {
        // Read alongside the media layout so the detail endpoint has no residual card clip.
        val progress = if (landingLayout?.canRender == true) {
            handoffProgressProvider().coerceIn(0f, 1f)
        } else {
            0f
        }
        clip = progress > 0f
        shape = RoundedCornerShape(clipCornerDp * progress)
    } else Modifier,
)

internal data class VideoDetailReturnPlayerTransform(
    val translationXPx: Float,
    val translationYPx: Float,
    val scale: Float,
)

/** Cover the changing media slot without stretching or resizing the playback surface. */
internal fun resolveVideoDetailReturnPlayerTransform(
    contentWidthPx: Int,
    contentHeightPx: Int,
    frame: VideoDetailReturnMediaLayoutFrame,
): VideoDetailReturnPlayerTransform {
    val width = contentWidthPx.coerceAtLeast(1)
    val height = contentHeightPx.coerceAtLeast(1)
    val scale = max(frame.widthPx.toFloat() / width, frame.heightPx.toFloat() / height)
    return VideoDetailReturnPlayerTransform(
        translationXPx = frame.offsetXPx + (frame.widthPx - width * scale) / 2f,
        translationYPx = frame.offsetYPx + (frame.heightPx - height * scale) / 2f,
        scale = scale,
    )
}

/** Animation state is read in placement/draw; PlayerView keeps its detail measurement. */
internal fun Modifier.videoDetailReturnPlayerLayout(
    landingLayout: VideoDetailReturnSourceCardLayout?,
    handoffProgressProvider: () -> Float,
    inverseScaleXProvider: () -> Float,
    inverseScaleYProvider: () -> Float,
    clipCornerDp: Dp = 0.dp,
    nativeSnapshotBoundsProvider: (() -> Rect?)? = null,
    slotOriginProvider: () -> Offset = { Offset.Zero },
): Modifier {
    // Layout and draw both need the frame for the same node size within one frame; memoize it so
    // the interpolation, inverse-scale and snapshot-bounds reads run once instead of twice.
    var memoWidth = Int.MIN_VALUE
    var memoHeight = Int.MIN_VALUE
    var memoProgress = Float.NaN
    var memoInverseScaleX = Float.NaN
    var memoInverseScaleY = Float.NaN
    var memoNativeBounds: Rect? = null
    var memoSlotOrigin = Offset.Unspecified
    var memoFrame: VideoDetailReturnMediaLayoutFrame? = null

    fun frame(width: Int, height: Int): VideoDetailReturnMediaLayoutFrame {
        val progress = handoffProgressProvider()
        val inverseScaleX = inverseScaleXProvider()
        val inverseScaleY = inverseScaleYProvider()
        val nativeBounds = nativeSnapshotBoundsProvider?.invoke()
        val slotOrigin = slotOriginProvider()
        memoFrame?.let { cached ->
            if (width == memoWidth &&
                height == memoHeight &&
                progress == memoProgress &&
                inverseScaleX == memoInverseScaleX &&
                inverseScaleY == memoInverseScaleY &&
                nativeBounds == memoNativeBounds && slotOrigin == memoSlotOrigin
            ) {
                return cached
            }
        }
        val entryFrame = resolveVideoDetailReturnMediaLayoutFrame(
            containerWidthPx = width,
            containerHeightPx = height,
            landingLayout = landingLayout,
            handoffProgress = progress,
            inverseScaleX = inverseScaleX,
            inverseScaleY = inverseScaleY,
            nativeSnapshotBounds = nativeBounds,
        )
        val resolved = resolveVideoDetailReturnMediaFrameInSlot(entryFrame, slotOrigin, progress)
        memoWidth = width
        memoHeight = height
        memoProgress = progress
        memoInverseScaleX = inverseScaleX
        memoInverseScaleY = inverseScaleY
        memoNativeBounds = nativeBounds
        memoSlotOrigin = slotOrigin
        memoFrame = resolved
        return resolved
    }
    // Clip outside the transformed child so the clip stays in media-slot coordinates.
    return drawWithCache {
        val path = Path()
        onDrawWithContent {
            val progress = handoffProgressProvider().coerceIn(0f, 1f)
            val hasTargetGeometry = landingLayout?.canRender == true ||
                nativeSnapshotBoundsProvider?.invoke()?.let { it.width > 1f && it.height > 1f } == true
            // The card owns clipping only during the handoff. A zero-radius RoundRect still
            // records a path clip around the native player; release it at the detail endpoint.
            if (progress == 0f || !hasTargetGeometry) {
                drawContent()
                return@onDrawWithContent
            }
            val mediaFrame = frame(size.width.roundToInt(), size.height.roundToInt())
            val radius = clipCornerDp.toPx() * progress
            path.reset()
            path.addRoundRect(
                RoundRect(
                    Rect(
                        mediaFrame.offsetXPx.toFloat(),
                        mediaFrame.offsetYPx.toFloat(),
                        (mediaFrame.offsetXPx + mediaFrame.widthPx).toFloat(),
                        (mediaFrame.offsetYPx + mediaFrame.heightPx).toFloat(),
                    ),
                    CornerRadius(radius, radius),
                ),
            )
            clipPath(path) { this@onDrawWithContent.drawContent() }
        }
    }.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            if (!constraints.hasBoundedWidth || !constraints.hasBoundedHeight) {
                placeable.place(0, 0)
            } else {
                val transform = resolveVideoDetailReturnPlayerTransform(
                    placeable.width, placeable.height,
                    frame(placeable.width, placeable.height),
                )
                placeable.placeWithLayer(0, 0) {
                    transformOrigin = TransformOrigin(0f, 0f)
                    translationX = transform.translationXPx
                    translationY = transform.translationYPx
                    scaleX = transform.scale
                    scaleY = transform.scale
                }
            }
        }
    }
}
