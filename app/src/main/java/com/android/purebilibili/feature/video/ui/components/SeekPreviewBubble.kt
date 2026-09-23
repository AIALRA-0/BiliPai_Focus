package com.android.purebilibili.feature.video.ui.components

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

import coil3.request.crossfade

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import com.android.purebilibili.core.ui.components.AppText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Size
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.VideoshotData
import kotlin.math.roundToInt

internal data class SeekPreviewDestinationRect(
    val offset: IntOffset,
    val size: IntSize
)

internal enum class SeekPreviewBubblePlacement {
    Anchored,
    Centered
}

internal const val PORTRAIT_SEEK_PREVIEW_ASPECT_RATIO = 9f / 16f

private data class SeekPreviewBubbleStyle(
    val widthDp: Int,
    val heightDp: Int,
    val fallbackWidthDp: Int,
    val cornerRadiusDp: Int,
    val shadowElevationDp: Int,
    val timeHorizontalPaddingDp: Int,
    val timeVerticalPaddingDp: Int,
    val timeFontSp: Int,
    val deltaFontSp: Int
)

internal data class CompactSeekPreviewSize(
    val widthDp: Int,
    val heightDp: Int
)

internal data class SeekPreviewSourceCrop(
    val offsetX: Int,
    val offsetY: Int,
    val width: Int,
    val height: Int
)

internal fun resolveCompactSeekPreviewSize(
    sourceWidthPx: Int,
    sourceHeightPx: Int,
    screenWidthDp: Int,
    videoAspectRatio: Float? = null
): CompactSeekPreviewSize {
    val safeWidth = sourceWidthPx.coerceAtLeast(1)
    val safeHeight = sourceHeightPx.coerceAtLeast(1)
    val sourceAspectRatio = safeWidth.toFloat() / safeHeight.toFloat()
    val effectiveAspectRatio = videoAspectRatio
        ?.takeIf { it.isFinite() && it > 0f }
        ?: sourceAspectRatio
    val scale = when {
        screenWidthDp >= 840 -> 1.2f
        screenWidthDp >= 600 -> 1.1f
        else -> 1f
    }
    val isPortraitVideo = effectiveAspectRatio < 1f
    val baseWidthDp = if (isPortraitVideo) 120 else 144
    val widthDp = (baseWidthDp * scale).roundToInt()
    val minHeightDp = ((if (isPortraitVideo) 144 else 72) * scale).roundToInt()
    val maxHeightDp = ((if (isPortraitVideo) 224 else 164) * scale).roundToInt()
    val heightDp = (widthDp / effectiveAspectRatio)
        .roundToInt()
        .coerceIn(minHeightDp, maxHeightDp)
    return CompactSeekPreviewSize(widthDp = widthDp, heightDp = heightDp)
}

internal fun resolveSeekPreviewSourceCrop(
    sourceWidthPx: Int,
    sourceHeightPx: Int,
    videoAspectRatio: Float?
): SeekPreviewSourceCrop {
    val safeWidth = sourceWidthPx.coerceAtLeast(1)
    val safeHeight = sourceHeightPx.coerceAtLeast(1)
    val targetAspectRatio = videoAspectRatio
        ?.takeIf { it.isFinite() && it >= 0.1f && it < 1f }
        ?: return SeekPreviewSourceCrop(
            offsetX = 0,
            offsetY = 0,
            width = safeWidth,
            height = safeHeight
        )
    val sourceAspectRatio = safeWidth.toFloat() / safeHeight.toFloat()
    if (sourceAspectRatio > targetAspectRatio) {
        val cropWidth = (safeHeight * targetAspectRatio)
            .roundToInt()
            .coerceIn(1, safeWidth)
        return SeekPreviewSourceCrop(
            offsetX = ((safeWidth - cropWidth) / 2f).roundToInt(),
            offsetY = 0,
            width = cropWidth,
            height = safeHeight
        )
    }

    val cropHeight = (safeWidth / targetAspectRatio)
        .roundToInt()
        .coerceIn(1, safeHeight)
    return SeekPreviewSourceCrop(
        offsetX = 0,
        offsetY = ((safeHeight - cropHeight) / 2f).roundToInt(),
        width = safeWidth,
        height = cropHeight
    )
}

private fun resolveSeekPreviewBubbleStyle(widthDp: Int): SeekPreviewBubbleStyle {
    return when {
        widthDp >= 1600 -> SeekPreviewBubbleStyle(
            widthDp = 232,
            heightDp = 130,
            fallbackWidthDp = 140,
            cornerRadiusDp = 16,
            shadowElevationDp = 10,
            timeHorizontalPaddingDp = 14,
            timeVerticalPaddingDp = 10,
            timeFontSp = 15,
            deltaFontSp = 13
        )
        widthDp >= 840 -> SeekPreviewBubbleStyle(
            widthDp = 214,
            heightDp = 120,
            fallbackWidthDp = 132,
            cornerRadiusDp = 14,
            shadowElevationDp = 8,
            timeHorizontalPaddingDp = 13,
            timeVerticalPaddingDp = 9,
            timeFontSp = 14,
            deltaFontSp = 12
        )
        else -> SeekPreviewBubbleStyle(
            widthDp = 188,
            heightDp = 106,
            fallbackWidthDp = 120,
            cornerRadiusDp = 12,
            shadowElevationDp = 7,
            timeHorizontalPaddingDp = 12,
            timeVerticalPaddingDp = 8,
            timeFontSp = 13,
            deltaFontSp = 12
        )
    }
}

internal fun resolveSeekPreviewBubbleOffsetPx(
    placement: SeekPreviewBubblePlacement,
    offsetX: Float,
    containerWidth: Float,
    bubbleWidthPx: Float
): Int {
    if (placement == SeekPreviewBubblePlacement.Centered) return 0
    if (containerWidth <= bubbleWidthPx || containerWidth <= 0f) return 0

    val halfBubble = bubbleWidthPx / 2f
    return (offsetX.coerceIn(halfBubble, containerWidth - halfBubble) - halfBubble).roundToInt()
}

internal fun resolveSeekPreviewAnchorPositionMs(
    videoshotData: VideoshotData?,
    targetPositionMs: Long,
    durationMs: Long
): Long {
    return videoshotData?.resolvePreviewAnchorPositionMs(
        positionMs = targetPositionMs,
        durationMs = durationMs
    ) ?: targetPositionMs.coerceAtLeast(0L)
}

internal fun resolveSeekPreviewDestinationRect(
    sourceWidthPx: Int,
    sourceHeightPx: Int,
    containerWidthPx: Int,
    containerHeightPx: Int
): SeekPreviewDestinationRect {
    val safeSourceWidth = sourceWidthPx.coerceAtLeast(1)
    val safeSourceHeight = sourceHeightPx.coerceAtLeast(1)
    val safeContainerWidth = containerWidthPx.coerceAtLeast(1)
    val safeContainerHeight = containerHeightPx.coerceAtLeast(1)

    val sourceAspectRatio = safeSourceWidth.toFloat() / safeSourceHeight.toFloat()
    val containerAspectRatio = safeContainerWidth.toFloat() / safeContainerHeight.toFloat()
    val (destinationWidth, destinationHeight) = if (sourceAspectRatio >= containerAspectRatio) {
        val height = (safeContainerWidth / sourceAspectRatio)
            .roundToInt()
            .coerceIn(1, safeContainerHeight)
        safeContainerWidth to height
    } else {
        val width = (safeContainerHeight * sourceAspectRatio)
            .roundToInt()
            .coerceIn(1, safeContainerWidth)
        width to safeContainerHeight
    }

    return SeekPreviewDestinationRect(
        offset = IntOffset(
            x = ((safeContainerWidth - destinationWidth) / 2f).roundToInt(),
            y = ((safeContainerHeight - destinationHeight) / 2f).roundToInt()
        ),
        size = IntSize(destinationWidth, destinationHeight)
    )
}

@Composable
internal fun SeekPreviewBubble(
    videoshotData: VideoshotData?,
    targetPositionMs: Long,
    currentPositionMs: Long,
    durationMs: Long,
    offsetX: Float,
    containerWidth: Float,
    placement: SeekPreviewBubblePlacement = SeekPreviewBubblePlacement.Anchored,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val style = remember(configuration.screenWidthDp) {
        resolveSeekPreviewBubbleStyle(configuration.screenWidthDp)
    }
    val bubbleWidthPx = with(LocalDensity.current) { style.widthDp.dp.toPx() }
    val bubbleOffsetX = remember(placement, offsetX, containerWidth, bubbleWidthPx) {
        resolveSeekPreviewBubbleOffsetPx(
            placement = placement,
            offsetX = offsetX,
            containerWidth = containerWidth,
            bubbleWidthPx = bubbleWidthPx
        )
    }
    val previewAnchorPositionMs = remember(videoshotData, targetPositionMs, durationMs) {
        resolveSeekPreviewAnchorPositionMs(
            videoshotData = videoshotData,
            targetPositionMs = targetPositionMs,
            durationMs = durationMs
        )
    }
    val currentPreviewInfo = remember(videoshotData, previewAnchorPositionMs, durationMs) {
        videoshotData?.getPreviewInfo(previewAnchorPositionMs, durationMs)
    }

    AppSurface(
        color = Color.Black.copy(alpha = 0.92f),
        shape = RoundedCornerShape(style.cornerRadiusDp.dp),
        shadowElevation = style.shadowElevationDp.dp,
        modifier = modifier
            .then(
                if (placement == SeekPreviewBubblePlacement.Anchored) {
                    Modifier.offset { IntOffset(bubbleOffsetX, 0) }
                } else {
                    Modifier
                }
            )
            .width(style.widthDp.dp)
            .height(style.heightDp.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            SeekPreviewImage(
                videoshotData = videoshotData,
                currentPreviewInfo = currentPreviewInfo,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(style.cornerRadiusDp.dp))
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.18f),
                                Color.Black.copy(alpha = 0.82f)
                            )
                        )
                    )
            )

            SeekPreviewTimeLabel(
                targetPositionMs = targetPositionMs,
                currentPositionMs = currentPositionMs,
                timeFontSp = style.timeFontSp,
                deltaFontSp = style.deltaFontSp,
                horizontalPaddingDp = style.timeHorizontalPaddingDp,
                verticalPaddingDp = style.timeVerticalPaddingDp
            )
        }
    }
}

/** 竖屏拖动样式：小预览图独立显示，时间位于图片下方。 */
@Composable
internal fun CompactSeekPreview(
    videoshotData: VideoshotData,
    targetPositionMs: Long,
    durationMs: Long,
    videoAspectRatio: Float? = null,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val previewSize = remember(
        videoshotData.img_x_size,
        videoshotData.img_y_size,
        configuration.screenWidthDp,
        videoAspectRatio
    ) {
        resolveCompactSeekPreviewSize(
            sourceWidthPx = videoshotData.img_x_size,
            sourceHeightPx = videoshotData.img_y_size,
            screenWidthDp = configuration.screenWidthDp,
            videoAspectRatio = videoAspectRatio
        )
    }
    val previewAnchorPositionMs = remember(videoshotData, targetPositionMs, durationMs) {
        resolveSeekPreviewAnchorPositionMs(
            videoshotData = videoshotData,
            targetPositionMs = targetPositionMs,
            durationMs = durationMs
        )
    }
    val currentPreviewInfo = remember(videoshotData, previewAnchorPositionMs, durationMs) {
        videoshotData.getPreviewInfo(previewAnchorPositionMs, durationMs)
    }
    val previewShape = AppShapes.container(ContainerLevel.Field)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppSurface(
            color = Color.Black.copy(alpha = 0.9f),
            shape = previewShape,
            shadowElevation = 8.dp,
            modifier = Modifier
                .width(previewSize.widthDp.dp)
                .height(previewSize.heightDp.dp)
                .border(1.dp, Color.White.copy(alpha = 0.78f), previewShape)
                .clip(previewShape)
        ) {
            SeekPreviewImage(
                videoshotData = videoshotData,
                currentPreviewInfo = currentPreviewInfo,
                videoAspectRatio = videoAspectRatio,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val shadow = Shadow(
                color = Color.Black.copy(alpha = 0.65f),
                offset = Offset(0f, 1.2f),
                blurRadius = 4f
            )
            AppText(
                text = FormatUtils.formatDuration((targetPositionMs / 1000L).toInt()),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium.copy(shadow = shadow)
            )
            AppText(
                text = " / ${FormatUtils.formatDuration((durationMs / 1000L).toInt())}",
                color = Color.White.copy(alpha = 0.62f),
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.titleMedium.copy(shadow = shadow)
            )
        }
    }
}

@Composable
private fun SeekPreviewImage(
    videoshotData: VideoshotData?,
    currentPreviewInfo: Triple<String, Int, Int>?,
    videoAspectRatio: Float? = null,
    modifier: Modifier = Modifier
) {
    if (currentPreviewInfo == null || videoshotData == null) {
        Box(
            modifier = modifier.background(Color(0xFF101010)),
            contentAlignment = Alignment.Center
        ) {
            AppText(
                text = "预览加载中",
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodySmall
            )
        }
        return
    }

    val context = LocalContext.current
    val (rawImageUrl, spriteOffsetX, spriteOffsetY) = currentPreviewInfo
    val imageUrl = if (rawImageUrl.startsWith("//")) "https:$rawImageUrl" else rawImageUrl
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(imageUrl)
            .size(Size.ORIGINAL)
            .crossfade(false)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build(),
        contentScale = ContentScale.Crop
    )

    val currentPainterState by painter.state.collectAsState()
    when (val painterState = currentPainterState) {
        is AsyncImagePainter.State.Loading -> {
            Box(
                modifier = modifier.background(Color(0xFF1A1A1A)),
                contentAlignment = Alignment.Center
            ) {
                AppText(text = "...", color = Color.White, style = MaterialTheme.typography.bodySmall)
            }
        }
        is AsyncImagePainter.State.Error -> {
            Box(
                modifier = modifier.background(Color(0xFF1A1A1A)),
                contentAlignment = Alignment.Center
            ) {
                AppText(text = "预览不可用", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
            }
        }
        is AsyncImagePainter.State.Success -> {
            Canvas(modifier = modifier) {
                val bitmap = (painterState.result.image as? coil3.BitmapImage)?.bitmap ?: return@Canvas

                val expectedWidth = (videoshotData.img_x_size * videoshotData.img_x_len).coerceAtLeast(1)
                val expectedHeight = (videoshotData.img_y_size * videoshotData.img_y_len).coerceAtLeast(1)
                val scaleX = bitmap.width.toFloat() / expectedWidth.toFloat()
                val scaleY = bitmap.height.toFloat() / expectedHeight.toFloat()
                val sourceCrop = resolveSeekPreviewSourceCrop(
                    sourceWidthPx = videoshotData.img_x_size,
                    sourceHeightPx = videoshotData.img_y_size,
                    videoAspectRatio = videoAspectRatio
                )
                val cropOffsetX = ((spriteOffsetX + sourceCrop.offsetX) * scaleX)
                    .roundToInt()
                    .coerceAtLeast(0)
                val cropOffsetY = ((spriteOffsetY + sourceCrop.offsetY) * scaleY)
                    .roundToInt()
                    .coerceAtLeast(0)
                val cropWidth = (sourceCrop.width * scaleX)
                    .roundToInt()
                    .coerceIn(1, (bitmap.width - cropOffsetX).coerceAtLeast(1))
                val cropHeight = (sourceCrop.height * scaleY)
                    .roundToInt()
                    .coerceIn(1, (bitmap.height - cropOffsetY).coerceAtLeast(1))
                val destinationRect = resolveSeekPreviewDestinationRect(
                    sourceWidthPx = cropWidth,
                    sourceHeightPx = cropHeight,
                    containerWidthPx = size.width.roundToInt(),
                    containerHeightPx = size.height.roundToInt()
                )

                drawImage(
                    image = bitmap.asImageBitmap(),
                    srcOffset = IntOffset(cropOffsetX, cropOffsetY),
                    srcSize = IntSize(cropWidth, cropHeight),
                    dstOffset = destinationRect.offset,
                    dstSize = destinationRect.size
                )
            }
        }
        else -> Unit
    }
}

@Composable
private fun BoxScope.SeekPreviewTimeLabel(
    targetPositionMs: Long,
    currentPositionMs: Long,
    timeFontSp: Int,
    deltaFontSp: Int,
    horizontalPaddingDp: Int,
    verticalPaddingDp: Int
) {
    Row(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(
                start = horizontalPaddingDp.dp,
                end = horizontalPaddingDp.dp,
                bottom = verticalPaddingDp.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val shadow = Shadow(
            color = Color.Black.copy(alpha = 0.5f),
            offset = Offset(0f, 1.2f),
            blurRadius = 4f
        )
        AppText(
            text = FormatUtils.formatDuration((targetPositionMs / 1000L).toInt()),
            color = Color.White,
            fontSize = timeFontSp.sp,
            fontWeight = FontWeight.SemiBold,
            style = androidx.compose.ui.text.TextStyle(shadow = shadow)
        )

        val deltaSeconds = (targetPositionMs - currentPositionMs) / 1000L
        if (deltaSeconds != 0L) {
            Spacer(modifier = Modifier.width(6.dp))
            AppText(
                text = if (deltaSeconds > 0L) "+${deltaSeconds}s" else "${deltaSeconds}s",
                color = if (deltaSeconds > 0L) Color(0xFF8CD48C) else Color(0xFFFF8A80),
                fontSize = deltaFontSp.sp,
                fontWeight = FontWeight.Medium,
                style = androidx.compose.ui.text.TextStyle(shadow = shadow)
            )
        }
    }
}

@Composable
internal fun SeekPreviewBubbleSimple(
    targetPositionMs: Long,
    currentPositionMs: Long,
    offsetX: Float,
    containerWidth: Float,
    placement: SeekPreviewBubblePlacement = SeekPreviewBubblePlacement.Anchored,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val style = remember(configuration.screenWidthDp) {
        resolveSeekPreviewBubbleStyle(configuration.screenWidthDp)
    }
    val bubbleWidthPx = with(LocalDensity.current) { style.fallbackWidthDp.dp.toPx() }
    val bubbleOffsetX = remember(placement, offsetX, containerWidth, bubbleWidthPx) {
        resolveSeekPreviewBubbleOffsetPx(
            placement = placement,
            offsetX = offsetX,
            containerWidth = containerWidth,
            bubbleWidthPx = bubbleWidthPx
        )
    }

    AppSurface(
        color = Color.Black.copy(alpha = 0.9f),
        shape = RoundedCornerShape(style.cornerRadiusDp.dp),
        shadowElevation = style.shadowElevationDp.dp,
        modifier = modifier
            .then(
                if (placement == SeekPreviewBubblePlacement.Anchored) {
                    Modifier.offset { IntOffset(bubbleOffsetX, 0) }
                } else {
                    Modifier
                }
            )
            .width(style.fallbackWidthDp.dp)
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = style.timeHorizontalPaddingDp.dp,
                vertical = style.timeVerticalPaddingDp.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppText(
                text = FormatUtils.formatDuration((targetPositionMs / 1000L).toInt()),
                color = Color.White,
                fontSize = style.timeFontSp.sp,
                fontWeight = FontWeight.SemiBold
            )

            val deltaSeconds = (targetPositionMs - currentPositionMs) / 1000L
            if (deltaSeconds != 0L) {
                Spacer(modifier = Modifier.height(2.dp))
                AppText(
                    text = if (deltaSeconds > 0L) "+${deltaSeconds}s" else "${deltaSeconds}s",
                    color = if (deltaSeconds > 0L) Color(0xFF8CD48C) else Color(0xFFFF8A80),
                    fontSize = style.deltaFontSp.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
