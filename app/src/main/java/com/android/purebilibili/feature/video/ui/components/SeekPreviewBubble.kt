package com.android.purebilibili.feature.video.ui.components

import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
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

    Surface(
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

@Composable
private fun SeekPreviewImage(
    videoshotData: VideoshotData?,
    currentPreviewInfo: Triple<String, Int, Int>?,
    modifier: Modifier = Modifier
) {
    if (currentPreviewInfo == null || videoshotData == null) {
        Box(
            modifier = modifier.background(Color(0xFF101010)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "预览加载中",
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 12.sp
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

    when (val painterState = painter.state) {
        is AsyncImagePainter.State.Loading -> {
            Box(
                modifier = modifier.background(Color(0xFF1A1A1A)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "...", color = Color.White, fontSize = 13.sp)
            }
        }
        is AsyncImagePainter.State.Error -> {
            Box(
                modifier = modifier.background(Color(0xFF1A1A1A)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "预览不可用", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
        is AsyncImagePainter.State.Success -> {
            Canvas(modifier = modifier) {
                val bitmap = (painterState.result.drawable as? BitmapDrawable)?.bitmap ?: return@Canvas

                val expectedWidth = (videoshotData.img_x_size * videoshotData.img_x_len).coerceAtLeast(1)
                val expectedHeight = (videoshotData.img_y_size * videoshotData.img_y_len).coerceAtLeast(1)
                val scaleX = bitmap.width.toFloat() / expectedWidth.toFloat()
                val scaleY = bitmap.height.toFloat() / expectedHeight.toFloat()
                val cropOffsetX = (spriteOffsetX * scaleX).roundToInt().coerceAtLeast(0)
                val cropOffsetY = (spriteOffsetY * scaleY).roundToInt().coerceAtLeast(0)
                val cropWidth = (videoshotData.img_x_size * scaleX)
                    .roundToInt()
                    .coerceIn(1, (bitmap.width - cropOffsetX).coerceAtLeast(1))
                val cropHeight = (videoshotData.img_y_size * scaleY)
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
        Text(
            text = FormatUtils.formatDuration((targetPositionMs / 1000L).toInt()),
            color = Color.White,
            fontSize = timeFontSp.sp,
            fontWeight = FontWeight.SemiBold,
            style = androidx.compose.ui.text.TextStyle(shadow = shadow)
        )

        val deltaSeconds = (targetPositionMs - currentPositionMs) / 1000L
        if (deltaSeconds != 0L) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
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

    Surface(
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
            Text(
                text = FormatUtils.formatDuration((targetPositionMs / 1000L).toInt()),
                color = Color.White,
                fontSize = style.timeFontSp.sp,
                fontWeight = FontWeight.SemiBold
            )

            val deltaSeconds = (targetPositionMs - currentPositionMs) / 1000L
            if (deltaSeconds != 0L) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (deltaSeconds > 0L) "+${deltaSeconds}s" else "${deltaSeconds}s",
                    color = if (deltaSeconds > 0L) Color(0xFF8CD48C) else Color(0xFFFF8A80),
                    fontSize = style.deltaFontSp.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
