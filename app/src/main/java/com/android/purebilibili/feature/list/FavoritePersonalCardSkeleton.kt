package com.android.purebilibili.feature.list

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.skeleton.ContentSkeletonBlock
import com.android.purebilibili.feature.personal.PersonalMediaCardFrame

/** Share the real card shell, cover constraints, padding and typography metrics. */
@Composable
internal fun FavoritePersonalCardSkeleton(stacked: Boolean, blockColor: Color, modifier: Modifier = Modifier) {
    @Composable
    fun TextPlaceholder(text: String, style: androidx.compose.ui.text.TextStyle) {
        AppText(text = text, style = style, modifier = Modifier.drawWithContent {
            drawRect(blockColor, size = size.copy(height = size.height * 0.65f))
        })
    }
    PersonalMediaCardFrame(
        modifier = modifier,
        stacked = stacked,
        enabled = false,
        onClick = {},
        headlineContent = { TextPlaceholder("加载收藏视频标题", MaterialTheme.typography.bodyMedium) },
        supportingContent = {
            Column {
                TextPlaceholder("UP 主名称", MaterialTheme.typography.bodySmall)
                TextPlaceholder("收藏时间", MaterialTheme.typography.labelSmall)
                TextPlaceholder("播放量 · 弹幕量", MaterialTheme.typography.labelSmall)
            }
        },
        coverContent = { ContentSkeletonBlock(blockColor, Modifier.fillMaxSize()) },
    )
}
