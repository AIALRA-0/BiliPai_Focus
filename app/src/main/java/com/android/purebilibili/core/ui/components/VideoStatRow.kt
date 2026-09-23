package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.feedContentTypography

/**
 * 视频播放量与弹幕数的紧凑统计行，可用于首页、相关推荐等视频卡片场景。
 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun VideoStatRow(
    playText: String,
    danmakuText: String,
    modifier: Modifier = Modifier,
    playIcon: ImageVector = Icons.Outlined.PlayCircleOutline,
    danmakuIcon: ImageVector = Icons.Outlined.Subtitles,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    textStyle: TextStyle = feedContentTypography().statistic,
) {
    FlowRow(
        modifier = modifier,
        itemVerticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        VideoStatItem(
            icon = playIcon,
            text = playText,
            contentColor = contentColor,
            textStyle = textStyle,
        )
        if (danmakuText.isNotBlank()) {
            VideoStatItem(
                icon = danmakuIcon,
                text = danmakuText,
                contentColor = contentColor,
                textStyle = textStyle,
            )
        }
    }
}

@Composable
private fun VideoStatItem(
    icon: ImageVector,
    text: String,
    contentColor: Color,
    textStyle: TextStyle,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        AppIcon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            // 13dp 会在部分高密度设备和 Miuix Icon 渲染器下把圆形描边量化到
            // 绘制边界之外，看起来像播放图标缺了一截。16dp 同时保留紧凑度和完整轮廓。
            modifier = Modifier.size(16.dp),
        )
        AppText(
            text = text,
            style = textStyle,
            color = contentColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
        )
    }
}
