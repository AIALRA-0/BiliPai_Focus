package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 *  iOS 风格毛玻璃卡片
 * 
 * 使用半透明背景和模糊效果创建类似 iOS 的磨砂玻璃效果
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    glassAlpha: Float = 0.15f,
    borderAlpha: Float = 0.2f,
    content: @Composable BoxScope.() -> Unit
) {
    //  使用 MaterialTheme 颜色代替硬编码
    val surfaceColor = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outline
    
    // 毛玻璃颜色 - 使用 surface 色
    val glassColor = surfaceColor.copy(alpha = glassAlpha + 0.5f)
    
    // 边框颜色 - 使用 outline 色
    val borderColor = outlineColor.copy(alpha = borderAlpha)
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(glassColor)
            .border(
                width = 0.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(cornerRadius)
            ),
        content = content
    )
}

/** Cover duration overlay. Matches the home feed shadow text, not a black capsule. */
@Composable
fun GlassDurationTag(
    duration: String,
    modifier: Modifier = Modifier
) {
    com.android.purebilibili.feature.home.components.cards.VideoCardCoverDurationText(
        text = duration,
        modifier = modifier,
    )
}
