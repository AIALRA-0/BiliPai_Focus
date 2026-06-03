// 文件路径: feature/video/ui/overlay/LandscapeRightSidebar.kt
package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.rememberAppBookmarkIcon
import com.android.purebilibili.core.ui.rememberAppCoinIcon
import com.android.purebilibili.core.ui.rememberAppLikeFilledIcon
import com.android.purebilibili.core.ui.rememberAppLikeIcon
import com.android.purebilibili.core.ui.rememberAppMoreIcon
import com.android.purebilibili.core.ui.rememberAppShareIcon
import com.android.purebilibili.core.util.FormatUtils

/**
 *  横屏右侧操作栏
 * 
 * 仿官方 B 站设计，竖向排列的操作按钮：
 * - 分享
 * - 收藏（带数字）
 * - 投币
 * - 点赞（带数字）
 * - 截图
 * - 更多
 */
@Composable
fun LandscapeRightSidebar(
    likeCount: Long = 0,
    favoriteCount: Long = 0,
    coinCount: Int = 0,
    isLiked: Boolean = false,
    isFavorited: Boolean = false,
    hasCoin: Boolean = false,
    //  [官方适配] 倍速和比例参数
    currentSpeed: Float = 1.0f,
    currentRatio: String = "适应",
    onLikeClick: () -> Unit = {},
    onFavoriteClick: () -> Unit = {},
    onCoinClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onScreenshotClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    onSpeedClick: () -> Unit = {},
    onRatioClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val shareIcon = rememberAppShareIcon()
    val favoriteIcon = rememberAppBookmarkIcon()
    val coinIcon = rememberAppCoinIcon()
    val likeIcon = rememberAppLikeIcon()
    val likeFilledIcon = rememberAppLikeFilledIcon()
    val moreIcon = rememberAppMoreIcon()

    // 右侧渐变背景 + 按钮列
    Box(
        modifier = modifier
            .width(80.dp)
            .fillMaxHeight()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.4f)
                    )
                )
            )
            .navigationBarsPadding()
            .padding(end = 8.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            //  [官方适配] 倍速按钮（放在顶部）
            SidebarTextButton(
                text = if (currentSpeed == 1.0f) "倍速" else "${currentSpeed}x",
                isHighlighted = currentSpeed != 1.0f,
                onClick = onSpeedClick
            )
            
            //  [官方适配] 比例按钮
            SidebarTextButton(
                text = currentRatio,
                isHighlighted = currentRatio != "适应",
                onClick = onRatioClick
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 分享按钮
            SidebarActionButton(
                icon = shareIcon,
                label = "分享",
                onClick = onShareClick
            )
            
            // 收藏按钮
            SidebarActionButton(
                icon = favoriteIcon,
                label = FormatUtils.formatStat(favoriteCount),
                isActive = isFavorited,
                activeColor = Color(0xFFFFD700), // 金色
                onClick = onFavoriteClick
            )
            
            // 投币按钮
            SidebarActionButton(
                icon = coinIcon,
                label = if (hasCoin) "已投" else "投币",
                isActive = hasCoin,
                activeColor = Color(0xFFFFCA28), // 亮金色
                onClick = onCoinClick
            )
            
            // 点赞按钮
            SidebarActionButton(
                icon = if (isLiked) likeFilledIcon else likeIcon,
                label = FormatUtils.formatStat(likeCount),
                isActive = isLiked,
                activeColor = MaterialTheme.colorScheme.primary,
                onClick = onLikeClick
            )
            
            // 更多按钮
            SidebarActionButton(
                icon = moreIcon,
                label = "更多",
                onClick = onMoreClick
            )
        }
    }
}

/**
 *  [官方适配] 侧边栏文字按钮（倍速/比例）
 */
@Composable
private fun SidebarTextButton(
    text: String,
    isHighlighted: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Black.copy(alpha = 0.5f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            color = if (isHighlighted) MaterialTheme.colorScheme.primary else Color.White,
            fontSize = 12.sp,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

/**
 *  单个侧边栏按钮
 */
@Composable
private fun SidebarActionButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean = false,
    activeColor: Color = Color.Unspecified,  //  默认用主题色
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "scale"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // 图标背景
        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.4f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isActive) (if (activeColor == Color.Unspecified) MaterialTheme.colorScheme.primary else activeColor) else Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // 标签文字
        Text(
            text = label,
            color = if (isActive) (if (activeColor == Color.Unspecified) MaterialTheme.colorScheme.primary else activeColor) else Color.White.copy(alpha = 0.9f),
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}
