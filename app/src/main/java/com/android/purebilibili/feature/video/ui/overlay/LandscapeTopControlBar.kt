// 文件路径: feature/video/ui/overlay/LandscapeTopControlBar.kt
package com.android.purebilibili.feature.video.ui.overlay
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppText

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
//  已改用 MaterialTheme.colorScheme.primary
import com.android.purebilibili.core.ui.rememberAppCoinIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.rememberAppLikeFilledIcon
import com.android.purebilibili.core.ui.rememberAppLikeIcon
import com.android.purebilibili.core.ui.rememberAppMoreIcon
import com.android.purebilibili.core.ui.rememberAppShareIcon
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel

/**
 *  横屏顶部控制栏（官方 B 站样式）
 * 
 * 布局结构：
 * - 左侧：返回按钮 + 标题 + 观看人数
 * - 右侧：点赞(带数字) + 投币 + 分享 + 更多
 */
@Composable
fun LandscapeTopControlBar(
    title: String,
    onlineCount: String = "",
    // 操作按钮状态
    likeCount: Long = 0,
    isLiked: Boolean = false,
    hasCoin: Boolean = false,
    // 回调
    onBack: () -> Unit,
    onLikeClick: () -> Unit = {},
    onCoinClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coinIcon = rememberAppCoinIcon()
    val likeIcon = rememberAppLikeIcon()
    val likeFilledIcon = rememberAppLikeFilledIcon()
    val moreIcon = rememberAppMoreIcon()
    val shareIcon = rememberAppShareIcon()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.7f),
                        Color.Transparent
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        //  左侧：返回 + 标题
        AppIconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
            AppIcon(
                Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                 contentDescription = "返回",
                tint = Color.White
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            // 标题
            AppText(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // 观看人数
            if (onlineCount.isNotEmpty()) {
                AppText(
                    text = onlineCount,
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Normal
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        //  右侧：操作按钮（官方样式：图标+数字，横向排列）
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 点赞按钮（带数字）
            TopBarActionButton(
                icon = if (isLiked) likeFilledIcon else likeIcon,
                label = FormatUtils.formatStat(likeCount),
                isActive = isLiked,
                activeColor = MaterialTheme.colorScheme.primary,
                onClick = onLikeClick
            )
            
            // 投币按钮
            TopBarActionButton(
                icon = coinIcon,
                label = if (hasCoin) "已投" else "",
                isActive = hasCoin,
                activeColor = Color(0xFFFFCA28),
                onClick = onCoinClick
            )
            
            // 分享按钮
            AppIconButton(onClick = onShareClick, modifier = Modifier.size(36.dp)) {
                AppIcon(
                    shareIcon,
                    contentDescription = "分享",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            // 更多按钮
            AppIconButton(onClick = onMoreClick, modifier = Modifier.size(36.dp)) {
                AppIcon(
                    moreIcon,
                    contentDescription = "更多",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 *  顶部栏操作按钮
 */
@Composable
private fun TopBarActionButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean = false,
    activeColor: Color = Color.Unspecified,  //  默认用主题色
    onClick: () -> Unit
) {
    AppSurface(
        onClick = onClick,
        color = Color.Transparent,
        shape = AppShapes.container(ContainerLevel.Tag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            AppIcon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) (if (activeColor == Color.Unspecified) MaterialTheme.colorScheme.primary else activeColor) else Color.White,
                modifier = Modifier.size(22.dp)
            )
            if (label.isNotEmpty()) {
                Spacer(modifier = Modifier.width(4.dp))
                AppText(
                    text = label,
                    color = if (isActive) (if (activeColor == Color.Unspecified) MaterialTheme.colorScheme.primary else activeColor) else Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
