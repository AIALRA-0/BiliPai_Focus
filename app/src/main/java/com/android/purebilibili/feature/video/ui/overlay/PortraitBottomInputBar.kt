package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import com.android.purebilibili.feature.video.ui.components.NativeDanmakuToggleButton

/**
 * 竖屏模式底部的输入栏
 * 包含：发弹幕输入框、推荐弹幕/表情、功能按钮
 */
@Composable
fun PortraitBottomInputBar(
    onInputClick: () -> Unit,
    danmakuEnabled: Boolean,
    onDanmakuToggle: () -> Unit,
    onRotateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val layoutPolicy = remember(configuration.screenWidthDp) {
        resolvePortraitBottomInputBarLayoutPolicy(
            widthDp = configuration.screenWidthDp
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(
                horizontal = layoutPolicy.horizontalPaddingDp.dp,
                vertical = layoutPolicy.verticalPaddingDp.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 输入框 (伪装)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(layoutPolicy.inputHeightDp.dp)
                .clip(RoundedCornerShape((layoutPolicy.inputHeightDp / 2).dp))
                .background(Color.White.copy(alpha = 0.2f))
                .clickable { onInputClick() }
                .padding(horizontal = layoutPolicy.inputHorizontalPaddingDp.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            AppText(
                text = "发弹幕...",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = layoutPolicy.inputFontSp.sp
            )
        }
        
        Spacer(modifier = Modifier.width(layoutPolicy.afterInputSpacingDp.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(layoutPolicy.actionSpacingDp.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NativeDanmakuToggleButton(
                enabled = danmakuEnabled,
                onToggle = onDanmakuToggle,
                activeTint = MaterialTheme.colorScheme.primary,
                inactiveTint = Color.White,
                modifier = Modifier.size(layoutPolicy.actionButtonSizeDp.dp),
                iconSize = layoutPolicy.actionIconSizeDp.dp,
            )
            PortraitInputIconButton(
                icon = Icons.Rounded.ScreenRotation,
                desc = "切换横屏",
                layoutPolicy = layoutPolicy,
                onClick = onRotateClick
            )
        }
    }
}

@Composable
private fun PortraitInputIconButton(
    icon: ImageVector,
    desc: String,
    layoutPolicy: PortraitBottomInputBarLayoutPolicy,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(layoutPolicy.actionButtonSizeDp.dp)
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.24f) else Color.Transparent
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        AppIcon(
            imageVector = icon,
            contentDescription = desc,
            tint = if (selected) MaterialTheme.colorScheme.primary else Color.White,
            modifier = Modifier.size(layoutPolicy.actionIconSizeDp.dp)
        )
    }
}
