package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

@Suppress("UNUSED_PARAMETER")
@Composable
internal fun OverlayPlaybackButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    outerSize: Dp,
    innerSize: Dp,
    glyphSize: Dp,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(outerSize)
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "暂停" else "播放",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(glyphSize)
        )
    }
}
