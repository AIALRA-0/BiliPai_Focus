// File: feature/video/ui/components/SeekPreviewBubble.kt
package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.request.ImageRequest
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.VideoshotData
import kotlin.math.roundToInt

/**
 * 进度条拖动预览气泡
 * 
 * 显示视频缩略图和目标时间，类似 B 站网页版效果
 */
@Composable
fun SeekPreviewBubble(
    videoshotData: VideoshotData?,
    targetPositionMs: Long,
    currentPositionMs: Long,
    durationMs: Long,
    offsetX: Float,            // 水平偏移量 (相对于进度条左端)
    containerWidth: Float,      // 进度条容器宽度
    modifier: Modifier = Modifier
) {
    // 计算气泡位置（限制在容器边界内）
    val bubbleWidth = 160.dp
    val bubbleHeight = 90.dp
    val bubbleWidthPx = with(LocalDensity.current) { bubbleWidth.toPx() }
    val halfBubble = bubbleWidthPx / 2
    
    // 限制气泡水平位置在容器内
    // [修复] 当 containerWidth 小于 bubbleWidth 时（居中显示场景），跳过位置限制
    val clampedOffsetX = if (containerWidth > bubbleWidthPx) {
        offsetX.coerceIn(halfBubble, containerWidth - halfBubble)
    } else {
        halfBubble // 居中显示时，直接使用半宽偏移
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

    Box(
        modifier = modifier
            .offset { IntOffset((clampedOffsetX - halfBubble).toInt(), 0) }
            .shadow(6.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .width(bubbleWidth)
            .height(bubbleHeight)
            .background(Color.Black)
    ) {
        SeekPreviewImage(
            videoshotData = videoshotData,
            currentPreviewInfo = currentPreviewInfo
        )
        
        // 2. 底部渐变遮罩 (中间层) - 仅在文字区域
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(40.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        )

        // 3. 时间标签 (顶层)
        SeekPreviewTimeLabel(
            targetPositionMs = targetPositionMs,
            currentPositionMs = currentPositionMs
        )
    }
}

@Composable
private fun SeekPreviewImage(
    videoshotData: VideoshotData?,
    currentPreviewInfo: Triple<String, Int, Int>?
) {
    if (currentPreviewInfo == null || videoshotData == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "预览加载中...",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
        return
    }

    val context = LocalContext.current
    val (rawImageUrl, spriteOffsetX, spriteOffsetY) = currentPreviewInfo
    val imageUrl = if (rawImageUrl.startsWith("//")) "https:$rawImageUrl" else rawImageUrl
    val thumbWidthPx = videoshotData.img_x_size
    val thumbHeightPx = videoshotData.img_y_size

    val painter = coil.compose.rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(imageUrl)
            .size(coil.size.Size.ORIGINAL)
            .crossfade(false)
            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
            .build()
    )

    when (val painterState = painter.state) {
        is coil.compose.AsyncImagePainter.State.Loading -> {
            Box(Modifier.fillMaxSize().background(Color.DarkGray), contentAlignment = Alignment.Center) {
                Text("...", color = Color.White, fontSize = 12.sp)
            }
        }
        is coil.compose.AsyncImagePainter.State.Error -> {
            Box(Modifier.fillMaxSize().background(Color.Red), contentAlignment = Alignment.Center) {
                Text("×", color = Color.White, fontSize = 16.sp)
            }
        }
        is coil.compose.AsyncImagePainter.State.Success -> {
            androidx.compose.foundation.Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val drawable = painterState.result.drawable
                val bitmap = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap ?: return@Canvas

                val inputWidth = bitmap.width
                val inputHeight = bitmap.height
                val expectedWidth = thumbWidthPx * videoshotData.img_x_len
                val expectedHeight = thumbHeightPx * videoshotData.img_y_len
                val scaleX = inputWidth.toFloat() / expectedWidth.toFloat()
                val scaleY = inputHeight.toFloat() / expectedHeight.toFloat()
                val realOffsetX = (spriteOffsetX * scaleX).toInt()
                val realOffsetY = (spriteOffsetY * scaleY).toInt()
                val realCropWidth = (thumbWidthPx * scaleX).toInt()
                val realCropHeight = (thumbHeightPx * scaleY).toInt()

                drawImage(
                    image = bitmap.asImageBitmap(),
                    srcOffset = IntOffset(realOffsetX, realOffsetY),
                    srcSize = IntSize(realCropWidth, realCropHeight),
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(size.width.toInt(), size.height.toInt())
                )
            }
        }
        else -> Unit
    }
}

@Composable
private fun BoxScope.SeekPreviewTimeLabel(
    targetPositionMs: Long,
    currentPositionMs: Long
) {
    Row(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = FormatUtils.formatDuration((targetPositionMs / 1000).toInt()),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black.copy(alpha = 0.5f),
                    blurRadius = 4f
                )
            )
        )

        val deltaSeconds = (targetPositionMs - currentPositionMs) / 1000
        if (deltaSeconds != 0L) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (deltaSeconds > 0) "+${deltaSeconds}s" else "${deltaSeconds}s",
                color = if (deltaSeconds > 0) Color(0xFF81C784) else Color(0xFFE57373),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        blurRadius = 4f
                    )
                )
            )
        }
    }
}

internal fun resolveSeekPreviewAnchorPositionMs(
    videoshotData: VideoshotData?,
    targetPositionMs: Long,
    durationMs: Long
): Long {
    if (videoshotData == null || durationMs <= 0L) return targetPositionMs

    val frameIndex = videoshotData.resolveSeekPreviewFrameIndex(
        targetPositionMs = targetPositionMs,
        durationMs = durationMs
    ) ?: return targetPositionMs

    val timelineMs = videoshotData.resolveTimelineMs(durationMs)
    if (timelineMs != null && frameIndex in timelineMs.indices) {
        return timelineMs[frameIndex]
    }

    val perImage = videoshotData.img_x_len * videoshotData.img_y_len
    val totalFrames = videoshotData.image.size * perImage
    if (totalFrames <= 0) return targetPositionMs

    return (durationMs * frameIndex) / totalFrames
}

private fun VideoshotData.resolveSeekPreviewFrameIndex(
    targetPositionMs: Long,
    durationMs: Long
): Int? {
    if (image.isEmpty()) return null

    val perImage = img_x_len * img_y_len
    if (perImage <= 0) return null

    val timelineMs = resolveTimelineMs(durationMs)
    return when {
        timelineMs != null -> {
            var low = 0
            var high = timelineMs.size - 1
            var resultIndex = 0

            while (low <= high) {
                val mid = (low + high) / 2
                if (timelineMs[mid] <= targetPositionMs) {
                    resultIndex = mid
                    low = mid + 1
                } else {
                    high = mid - 1
                }
            }
            resultIndex
        }
        durationMs > 0L -> {
            val totalFrames = image.size * perImage
            if (totalFrames <= 0) return null
            val ratio = (targetPositionMs.toFloat() / durationMs).coerceIn(0f, 1f)
            (ratio * (totalFrames - 1)).roundToInt()
        }
        else -> null
    }
}

private fun VideoshotData.resolveTimelineMs(durationMs: Long): List<Long>? {
    if (index.isEmpty()) return null

    return if (index.lastOrNull()?.let { last -> last > 0 && last < durationMs / 2 } == true) {
        index.map { it * 1000 }
    } else {
        index
    }
}

/**
 * 简化版预览气泡（仅显示时间，无缩略图）
 * 
 * 用于无 videoshot 数据时的降级显示
 */
@Composable
fun SeekPreviewBubbleSimple(
    targetPositionMs: Long,
    currentPositionMs: Long,
    offsetX: Float,
    containerWidth: Float,
    modifier: Modifier = Modifier
) {
    val bubbleWidth = 100.dp
    val bubbleWidthPx = with(LocalDensity.current) { bubbleWidth.toPx() }
    val halfBubble = bubbleWidthPx / 2
    val clampedOffsetX = offsetX.coerceIn(halfBubble, containerWidth - halfBubble)
    
    Box(
        modifier = modifier
            .offset { IntOffset((clampedOffsetX - halfBubble).toInt(), 0) }
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 目标时间
            Text(
                text = FormatUtils.formatDuration((targetPositionMs / 1000).toInt()),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            // 时间差
            val deltaSeconds = (targetPositionMs - currentPositionMs) / 1000
            if (deltaSeconds != 0L) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (deltaSeconds > 0) "+${deltaSeconds}s" else "${deltaSeconds}s",
                    color = if (deltaSeconds > 0) Color(0xFF4CAF50) else Color(0xFFFF5252),
                    fontSize = 12.sp
                )
            }
        }
    }
}
