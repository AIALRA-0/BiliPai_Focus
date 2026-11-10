package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

@Composable
internal fun OverlayPlaybackButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    outerSize: Dp,
    glyphSize: Dp,
    modifier: Modifier = Modifier
) {
    AppIconButton(
        onClick = onClick,
        modifier = modifier.size(outerSize)
    ) {
        AppIcon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "暂停" else "播放",
            tint = Color.White,
            modifier = Modifier.size(glyphSize)
        )
    }
}
