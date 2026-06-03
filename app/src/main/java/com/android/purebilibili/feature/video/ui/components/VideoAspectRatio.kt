// 文件路径: feature/video/ui/components/VideoAspectRatio.kt
package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.ui.AspectRatioFrameLayout
import kotlin.math.roundToInt
//  已改用 MaterialTheme.colorScheme.primary

/**
 * 视频比例枚举
 * `resizeMode` 保留给旧偏好映射，真实渲染使用 `playerResizeMode`。
 */
enum class VideoAspectRatio(
    val displayName: String,
    val resizeMode: Int,
    val targetAspectRatio: Float? = null,
    val playerResizeMode: Int = resizeMode
) {
    FIT("适应", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FILL("填充", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    RATIO_16_9(
        "16:9",
        AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH,
        targetAspectRatio = 16f / 9f,
        playerResizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    ),
    RATIO_4_3(
        "4:3",
        AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT,
        targetAspectRatio = 4f / 3f,
        playerResizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    ),
    STRETCH("拉伸", AspectRatioFrameLayout.RESIZE_MODE_FILL);
    
    companion object {
        fun fromResizeMode(mode: Int): VideoAspectRatio {
            return entries.find { it.resizeMode == mode } ?: FIT
        }
    }
}

internal data class VideoViewportLayout(
    val width: Int,
    val height: Int
)

internal fun resolveVideoViewportLayout(
    containerWidth: Int,
    containerHeight: Int,
    aspectRatio: VideoAspectRatio
): VideoViewportLayout {
    val safeWidth = containerWidth.coerceAtLeast(1)
    val safeHeight = containerHeight.coerceAtLeast(1)
    val targetAspectRatio = aspectRatio.targetAspectRatio
        ?.takeIf { it.isFinite() && it > 0f }
        ?: return VideoViewportLayout(width = safeWidth, height = safeHeight)

    val containerAspectRatio = safeWidth.toFloat() / safeHeight.toFloat()
    return if (containerAspectRatio > targetAspectRatio) {
        VideoViewportLayout(
            width = (safeHeight * targetAspectRatio).roundToInt().coerceIn(1, safeWidth),
            height = safeHeight
        )
    } else {
        VideoViewportLayout(
            width = safeWidth,
            height = (safeWidth / targetAspectRatio).roundToInt().coerceIn(1, safeHeight)
        )
    }
}

/**
 * 视频比例选择菜单
 */
@Composable
fun AspectRatioMenu(
    currentRatio: VideoAspectRatio,
    onRatioSelected: (VideoAspectRatio) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.widthIn(min = 120.dp, max = 200.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.85f),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 标题
            Text(
                text = "画面比例",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // 比例选项
            VideoAspectRatio.entries.forEach { ratio ->
                val isSelected = ratio == currentRatio
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                    onClick = {
                        onRatioSelected(ratio)
                        onDismiss()
                    }
                ) {
                    Text(
                        text = ratio.displayName,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

/**
 * 比例按钮（用于底部控制栏）
 */
@Composable
fun AspectRatioButton(
    currentRatio: VideoAspectRatio,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = Color.Black.copy(alpha = 0.5f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                CupertinoIcons.Default.Star,
                contentDescription = "画面比例",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = currentRatio.displayName,
                color = Color.White,
                fontSize = 12.sp
            )
        }
    }
}
