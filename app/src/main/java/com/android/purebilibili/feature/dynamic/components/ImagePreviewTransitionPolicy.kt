package com.android.purebilibili.feature.dynamic.components

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.math.roundToInt

private const val LAYOUT_PROGRESS_MIN = 0f
private const val LAYOUT_PROGRESS_MAX = 1f
private const val FALLBACK_START_SCALE = 0.96f
/** 一镜到底：进出场共用 Continuity 曲线与相近时长，避免 overshoot 二次弹。 */
private const val IMAGE_PREVIEW_OPEN_DURATION_MS = 320
private const val IMAGE_PREVIEW_DISMISS_DURATION_MS = 300
private const val IMAGE_PREVIEW_CANCEL_RECOVER_DURATION_MS = 180
private const val IMAGE_PREVIEW_BLUR_QUANTUM_PX = 2f

internal data class ImagePreviewTransitionFrame(
    val layoutProgress: Float,
    val visualProgress: Float,
    val cornerRadiusDp: Float,
    val fallbackScale: Float
)

internal data class ImagePreviewVisualFrame(
    val contentAlpha: Float,
    val backdropAlpha: Float,
    val blurRadiusPx: Float
)

internal data class ImagePreviewDismissMotion(
    /** 关闭落点：一镜到底直落到 0，不再 overshoot。 */
    val overshootTarget: Float,
    val settleTarget: Float,
    /** 主收缩时长：与进场接近，Continuity 先快后慢贴回缩略图。 */
    val collapseDurationMillis: Int,
    /** 预测返回取消后的回弹时长。 */
    val cancelRecoverDurationMillis: Int,
    /** 打开时长：与关闭同系，进出一镜对称。 */
    val openDurationMillis: Int
)

internal data class ImagePreviewDismissTransform(
    val scale: Float,
    val translationXPx: Float,
    val translationYPx: Float
)

internal data class ImagePreviewDismissRectFrame(
    val rect: Rect,
    val dismissFraction: Float
)

internal data class ImagePreviewVerticalDragFrame(
    val progress: Float,
    val scale: Float,
    val backdropAlphaMultiplier: Float
)

internal enum class ImagePreviewVerticalDismissDecision {
    DISMISS,
    SNAP_BACK
}

enum class ImagePreviewTextPlacement {
    OVERLAY_BOTTOM,
    TOP_BAR
}

data class ImagePreviewTextContent(
    val headline: String = "",
    val body: String = "",
    val perImageCaptions: List<String> = emptyList(),
    val placement: ImagePreviewTextPlacement = ImagePreviewTextPlacement.OVERLAY_BOTTOM,
    val commentContext: ImagePreviewCommentContext? = null
)

data class ImagePreviewCommentContext(
    val replyId: Long = 0L,
    val authorName: String = "",
    val avatarUrl: String = "",
    val timeText: String = "",
    val body: String = "",
    val originalSizeLabels: List<String> = emptyList(),
    val likeCount: Int = 0,
    val liked: Boolean = false,
    val onLikeClick: (() -> Unit)? = null,
    val onReplyClick: (() -> Unit)? = null
)

internal data class ImagePreviewResolvedText(
    val headline: String,
    val body: String,
    val pageIndicator: String
)

internal data class ImagePreviewTextTransform(
    val rotationX: Float,
    val alpha: Float,
    val translateYDp: Float
)

internal data class CommentImagePreviewPageTransform(
    val rotationY: Float,
    val pivotFractionX: Float,
    val translationXPx: Float,
    val scale: Float,
    val alpha: Float
)

internal data class ImagePreviewGalleryPageTransform(
    val rotationY: Float,
    val pivotFractionX: Float,
    val translationXPx: Float,
    val scale: Float,
    val alpha: Float
)

internal data class ImagePreviewOverlayPadding(
    val start: Dp,
    val top: Dp,
    val end: Dp,
    val bottom: Dp
)

internal fun resolveImagePreviewTransitionFrame(
    rawProgress: Float,
    hasSourceRect: Boolean,
    sourceCornerRadiusDp: Float
): ImagePreviewTransitionFrame {
    val layoutProgress = rawProgress.coerceIn(LAYOUT_PROGRESS_MIN, LAYOUT_PROGRESS_MAX)
    val visualProgress = rawProgress.coerceIn(0f, 1f)
    val cornerRadiusDp = resolveImagePreviewPresentedCornerRadiusDp(
        visualProgress = visualProgress,
        verticalDragProgress = 0f,
        hasSourceRect = hasSourceRect,
        sourceCornerRadiusDp = sourceCornerRadiusDp
    )
    val fallbackScale = lerpFloat(FALLBACK_START_SCALE, 1f, visualProgress)
    return ImagePreviewTransitionFrame(
        layoutProgress = layoutProgress,
        visualProgress = visualProgress,
        cornerRadiusDp = cornerRadiusDp,
        fallbackScale = fallbackScale
    )
}

/**
 * 全屏打开时圆角为 0；返回/竖滑退出时插值到缩略图圆角，贴回格子更自然。
 */
internal fun resolveImagePreviewPresentedCornerRadiusDp(
    visualProgress: Float,
    verticalDragProgress: Float,
    hasSourceRect: Boolean,
    sourceCornerRadiusDp: Float,
    openCornerRadiusDp: Float = 0f
): Float {
    if (!hasSourceRect) return openCornerRadiusDp.coerceAtLeast(0f)
    val source = sourceCornerRadiusDp.coerceAtLeast(0f)
    val open = openCornerRadiusDp.coerceAtLeast(0f)
    val morphCorner = lerpFloat(source, open, visualProgress.coerceIn(0f, 1f))
    val dragCorner = lerpFloat(open, source, verticalDragProgress.coerceIn(0f, 1f))
    return maxOf(morphCorner, dragCorner)
}

internal fun resolveImagePreviewVisualFrame(
    visualProgress: Float,
    transitionEnabled: Boolean,
    maxBlurRadiusPx: Float,
    blurEnabled: Boolean = true,
): ImagePreviewVisualFrame {
    val progress = visualProgress.coerceIn(0f, 1f)
    if (!transitionEnabled) {
        return ImagePreviewVisualFrame(
            contentAlpha = 1f,
            backdropAlpha = progress,
            blurRadiusPx = 0f
        )
    }

    return ImagePreviewVisualFrame(
        contentAlpha = lerpFloat(0.9f, 1f, progress),
        backdropAlpha = progress,
        blurRadiusPx = if (blurEnabled) {
            resolveImagePreviewBlurRadiusPx(
                visualProgress = progress,
                maxBlurRadiusPx = maxBlurRadiusPx,
            )
        } else {
            0f
        }
    )
}

internal fun resolveImagePreviewBlurRadiusPx(
    visualProgress: Float,
    maxBlurRadiusPx: Float,
): Float {
    val returnProgress = 1f - visualProgress.coerceIn(0f, 1f)
    val maxRadius = maxBlurRadiusPx.coerceAtLeast(0f)
    val easedRadius = maxRadius * returnProgress * returnProgress
    return ((easedRadius / IMAGE_PREVIEW_BLUR_QUANTUM_PX).roundToInt() *
        IMAGE_PREVIEW_BLUR_QUANTUM_PX).coerceIn(0f, maxRadius)
}

internal fun imagePreviewDismissMotion(): ImagePreviewDismissMotion {
    return ImagePreviewDismissMotion(
        // 一镜到底：单段连续 morph 到缩略图，不做 overshoot + spring 二次落点。
        overshootTarget = 0f,
        settleTarget = 0f,
        collapseDurationMillis = IMAGE_PREVIEW_DISMISS_DURATION_MS,
        cancelRecoverDurationMillis = IMAGE_PREVIEW_CANCEL_RECOVER_DURATION_MS,
        openDurationMillis = IMAGE_PREVIEW_OPEN_DURATION_MS
    )
}

internal fun resolveImagePreviewDismissTransform(
    transitionProgress: Float,
    sourceRect: Rect?,
    displayedImageRect: Rect?
): ImagePreviewDismissTransform {
    if (sourceRect == null || displayedImageRect == null) {
        return ImagePreviewDismissTransform(
            scale = 1f,
            translationXPx = 0f,
            translationYPx = 0f
        )
    }

    // 几何插值保持线性；速度曲线只交给 Animatable 的 Continuity easing。
    val dismissFraction = resolveImagePreviewDismissFraction(transitionProgress)
    val targetScale = min(
        sourceRect.width / displayedImageRect.width,
        sourceRect.height / displayedImageRect.height
    ).coerceIn(0f, 1f)
    val containerCenterX = (displayedImageRect.left + displayedImageRect.right) / 2f
    val containerCenterY = (displayedImageRect.top + displayedImageRect.bottom) / 2f
    val sourceCenterX = (sourceRect.left + sourceRect.right) / 2f
    val sourceCenterY = (sourceRect.top + sourceRect.bottom) / 2f
    val targetTranslationX = sourceCenterX - containerCenterX
    val targetTranslationY = sourceCenterY - containerCenterY

    return ImagePreviewDismissTransform(
        scale = lerpFloat(1f, targetScale, dismissFraction).coerceAtLeast(0.01f),
        translationXPx = lerpFloat(0f, targetTranslationX, dismissFraction),
        translationYPx = lerpFloat(0f, targetTranslationY, dismissFraction)
    )
}

internal fun resolveImagePreviewDismissRectFrame(
    transitionProgress: Float,
    sourceRect: Rect?,
    displayedImageRect: Rect?
): ImagePreviewDismissRectFrame? {
    if (sourceRect == null || displayedImageRect == null) return null

    val dismissFraction = resolveImagePreviewDismissFraction(transitionProgress)
    val displayedCenterX = (displayedImageRect.left + displayedImageRect.right) / 2f
    val displayedCenterY = (displayedImageRect.top + displayedImageRect.bottom) / 2f
    val sourceCenterX = (sourceRect.left + sourceRect.right) / 2f
    val sourceCenterY = (sourceRect.top + sourceRect.bottom) / 2f
    val width = lerpFloat(displayedImageRect.width, sourceRect.width, dismissFraction)
    val height = lerpFloat(displayedImageRect.height, sourceRect.height, dismissFraction)
    val centerX = lerpFloat(displayedCenterX, sourceCenterX, dismissFraction)
    val centerY = lerpFloat(displayedCenterY, sourceCenterY, dismissFraction)

    return ImagePreviewDismissRectFrame(
        rect = Rect(
            left = centerX - width / 2f,
            top = centerY - height / 2f,
            right = centerX + width / 2f,
            bottom = centerY + height / 2f
        ),
        dismissFraction = dismissFraction
    )
}

internal fun resolveImagePreviewDismissStartRect(
    previewSurfaceRect: Rect?,
    displayedImageRect: Rect?,
    preferPreviewSurface: Boolean
): Rect? {
    return if (preferPreviewSurface) {
        previewSurfaceRect ?: displayedImageRect
    } else {
        displayedImageRect ?: previewSurfaceRect
    }
}

internal fun resolveImagePreviewOverlayPadding(
    safeInsetStart: Dp,
    safeInsetTop: Dp,
    safeInsetEnd: Dp,
    safeInsetBottom: Dp,
    extraHorizontal: Dp = 16.dp,
    extraVertical: Dp = 16.dp
): ImagePreviewOverlayPadding {
    val resolvedHorizontal = extraHorizontal.coerceAtLeast(0.dp)
    val resolvedVertical = extraVertical.coerceAtLeast(0.dp)
    return ImagePreviewOverlayPadding(
        start = safeInsetStart.coerceAtLeast(0.dp) + resolvedHorizontal,
        top = safeInsetTop.coerceAtLeast(0.dp) + resolvedVertical,
        end = safeInsetEnd.coerceAtLeast(0.dp) + resolvedHorizontal,
        bottom = safeInsetBottom.coerceAtLeast(0.dp) + resolvedVertical
    )
}

internal fun resolveImagePreviewDraggedDisplayRect(
    displayedImageRect: Rect?,
    translationYPx: Float,
    scale: Float
): Rect? {
    if (displayedImageRect == null) return null
    val safeScale = scale.coerceAtLeast(0.01f)
    val width = displayedImageRect.width * safeScale
    val height = displayedImageRect.height * safeScale
    val centerX = (displayedImageRect.left + displayedImageRect.right) / 2f
    val centerY = (displayedImageRect.top + displayedImageRect.bottom) / 2f + translationYPx
    return Rect(
        left = centerX - width / 2f,
        top = centerY - height / 2f,
        right = centerX + width / 2f,
        bottom = centerY + height / 2f
    )
}

internal fun shouldEnableImagePreviewVerticalDismiss(zoomScale: Float): Boolean {
    return zoomScale <= 1.01f
}

internal fun resolveImagePreviewVerticalDragFrame(
    dragOffsetYPx: Float,
    containerHeightPx: Float
): ImagePreviewVerticalDragFrame {
    val denominator = (containerHeightPx.coerceAtLeast(1f) * 0.28f).coerceAtLeast(120f)
    val progress = (kotlin.math.abs(dragOffsetYPx) / denominator).coerceIn(0f, 1f)
    return ImagePreviewVerticalDragFrame(
        progress = progress,
        scale = lerpFloat(1f, 0.9f, progress),
        backdropAlphaMultiplier = lerpFloat(1f, 0.18f, progress)
    )
}

internal fun resolveImagePreviewVerticalDismissDecision(
    dragOffsetYPx: Float,
    containerHeightPx: Float
): ImagePreviewVerticalDismissDecision {
    val threshold = maxOf(120f, containerHeightPx.coerceAtLeast(1f) * 0.14f)
    return if (kotlin.math.abs(dragOffsetYPx) >= threshold) {
        ImagePreviewVerticalDismissDecision.DISMISS
    } else {
        ImagePreviewVerticalDismissDecision.SNAP_BACK
    }
}

internal fun resolveImagePreviewDismissBackdropAlpha(
    visualProgress: Float
): Float {
    // 一镜到底：遮罩与 morph 进度线性同步，落点时立刻露底，减少「关完还黑一下」。
    return visualProgress.coerceIn(0f, 1f)
}

/**
 * Chrome（顶栏/评论条）比图片 morph 更早淡出，避免控件跟着缩变形。
 */
internal fun resolveImagePreviewChromeAlpha(
    visualProgress: Float,
    isDismissing: Boolean
): Float {
    val progress = visualProgress.coerceIn(0f, 1f)
    if (!isDismissing) return progress
    // 前半段基本清掉 chrome，后半段只剩干净的图片飞回。
    return ((progress - 0.35f) / 0.65f).coerceIn(0f, 1f)
}

internal fun resolveImagePreviewText(
    textContent: ImagePreviewTextContent?,
    currentPage: Int,
    totalPages: Int
): ImagePreviewResolvedText? {
    if (textContent == null) return null
    val headline = textContent.headline.trim().take(48)
    val pageCaption = textContent.perImageCaptions
        .getOrNull(currentPage)
        ?.trim()
        .orEmpty()
    val body = if (pageCaption.isNotEmpty()) {
        pageCaption.take(150)
    } else {
        textContent.body.trim().take(150)
    }
    val indicator = if (totalPages > 1 && currentPage >= 0) {
        "${currentPage + 1} / $totalPages"
    } else {
        ""
    }
    if (headline.isEmpty() && body.isEmpty() && indicator.isEmpty()) return null
    return ImagePreviewResolvedText(
        headline = headline,
        body = body,
        pageIndicator = indicator
    )
}

internal fun resolveImagePreviewTextTransform(pageOffsetFraction: Float): ImagePreviewTextTransform {
    val clampedOffset = pageOffsetFraction.coerceIn(-1f, 1f)
    val absOffset = kotlin.math.abs(clampedOffset)
    return ImagePreviewTextTransform(
        rotationX = absOffset * 22f,
        alpha = (1f - absOffset * 0.38f).coerceIn(0.62f, 1f),
        translateYDp = absOffset * 8f
    )
}

internal fun shouldShowImagePreviewText(
    hasText: Boolean,
    textVisible: Boolean
): Boolean = hasText && textVisible

internal fun resolveImagePreviewInitialTextVisibility(
    hasText: Boolean,
    defaultVisible: Boolean
): Boolean = hasText && defaultVisible

internal fun resolveImagePreviewTextVisibilityAfterToggle(currentVisible: Boolean): Boolean {
    return !currentVisible
}

internal fun resolveCommentImagePreviewPageTransform(
    pageOffsetFraction: Float,
    containerWidthPx: Float
): CommentImagePreviewPageTransform {
    val clampedOffset = pageOffsetFraction.coerceIn(-1f, 1f)
    val absOffset = kotlin.math.abs(clampedOffset)
    val safeWidth = containerWidthPx.coerceAtLeast(1f)
    val pivot = when {
        clampedOffset > 0.001f -> 1f
        clampedOffset < -0.001f -> 0f
        else -> 0.5f
    }
    return CommentImagePreviewPageTransform(
        rotationY = -clampedOffset * 72f,
        pivotFractionX = pivot,
        translationXPx = -clampedOffset * safeWidth * 0.23f,
        scale = lerpFloat(1f, 0.86f, absOffset),
        alpha = lerpFloat(1f, 0.76f, absOffset)
    )
}

/**
 * 普通画廊的可选 3D 翻页。角度和位移比评论面板更轻，减少边缘拉伸与切页时的黑缝。
 */
internal fun resolveImagePreviewGalleryPageTransform(
    pageOffsetFraction: Float,
    containerWidthPx: Float
): ImagePreviewGalleryPageTransform {
    val clampedOffset = pageOffsetFraction.coerceIn(-1f, 1f)
    val absOffset = kotlin.math.abs(clampedOffset)
    val pivot = when {
        clampedOffset > 0.001f -> 1f
        clampedOffset < -0.001f -> 0f
        else -> 0.5f
    }
    return ImagePreviewGalleryPageTransform(
        rotationY = -clampedOffset * 48f,
        pivotFractionX = pivot,
        translationXPx = -clampedOffset * containerWidthPx.coerceAtLeast(1f) * 0.08f,
        scale = lerpFloat(1f, 0.94f, absOffset),
        alpha = lerpFloat(1f, 0.84f, absOffset)
    )
}

internal fun resolveCommentImageOriginalSizeLabel(sizeKb: Float?): String {
    val safeSize = sizeKb?.takeIf { it > 0f } ?: return "查看原图"
    return if (safeSize >= 1024f) {
        "查看原图 (${String.format(java.util.Locale.US, "%.1f", safeSize / 1024f)}M)"
    } else {
        "查看原图 (${safeSize.toInt()}K)"
    }
}

internal fun shouldHandleImagePreviewLongPressSave(
    longPressSaveEnabled: Boolean,
    imageUrl: String,
    isSaving: Boolean
): Boolean {
    return longPressSaveEnabled && imageUrl.isNotBlank() && !isSaving
}

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}

/**
 * 进度 1 = 全屏打开，0 = 落回缩略图。
 * 几何插值线性，避免与 Animatable easing 叠加重映射导致末段发黏。
 */
private fun resolveImagePreviewDismissFraction(transitionProgress: Float): Float {
    return (1f - transitionProgress.coerceIn(0f, 1f)).coerceIn(0f, 1f)
}
