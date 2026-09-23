// 文件路径: feature/video/ui/overlay/LandscapeUpInfo.kt
package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel

/**
 *  横屏 UP 主信息组件
 * 
 * 显示在横屏左上角，包含：
 * - UP 主头像（圆形）
 * - UP 主名字
 */
@Composable
fun LandscapeUpInfo(
    avatarUrl: String,
    upName: String,
    modifier: Modifier = Modifier
) {
    AppSurface(
        modifier = modifier,
        shape = AppShapes.container(ContainerLevel.Floating),
        color = Color.Black.copy(alpha = 0.5f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // UP 主头像
            AsyncImage(
                model = avatarUrl,
                contentDescription = "UP主头像",
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.3f)),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // UP 主名字
            AppText(
                text = upName,
                color = Color.White,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 120.dp)
            )
        }
    }
}

/**
 *  横屏观看人数组件
 */
@Composable
fun LandscapeViewerCount(
    count: String,
    modifier: Modifier = Modifier
) {
    if (count.isEmpty()) return
    
    AppSurface(
        modifier = modifier,
        shape = AppShapes.container(ContainerLevel.Card),
        color = Color.Black.copy(alpha = 0.5f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            // 观看图标（使用文字代替）
            AppText(
                text = "👁",
                style = MaterialTheme.typography.labelSmall
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            AppText(
                text = count,
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
