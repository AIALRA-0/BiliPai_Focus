package com.android.purebilibili.feature.video.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.SubtitlesOff
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton

/** Native Material equivalent of PiliPlus' dedicated danmaku on/off control. */
@Composable
fun NativeDanmakuToggleButton(
    enabled: Boolean,
    onToggle: () -> Unit,
    activeTint: Color,
    inactiveTint: Color,
    modifier: Modifier = Modifier,
    iconSize: Dp = 22.dp,
) {
    AppIconButton(
        onClick = onToggle,
        modifier = modifier,
    ) {
        AppIcon(
            imageVector = if (enabled) Icons.Outlined.Subtitles else Icons.Outlined.SubtitlesOff,
            contentDescription = if (enabled) "关闭弹幕" else "开启弹幕",
            tint = if (enabled) activeTint else inactiveTint,
            modifier = Modifier.size(iconSize),
        )
    }
}
