package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp

@Suppress("UNUSED_PARAMETER")
@Composable
internal fun OverlayPlaybackButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    onDoubleClick: (() -> Unit)? = null,
    outerSize: Dp,
    innerSize: Dp,
    glyphSize: Dp,
    modifier: Modifier = Modifier
) {
    if (onDoubleClick != null) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(outerSize)
                .combinedClickable(
                    role = Role.Button,
                    onClick = onClick,
                    onDoubleClick = onDoubleClick
                )
        ) {
            OverlayPlaybackIcon(
                isPlaying = isPlaying,
                glyphSize = glyphSize
            )
        }
        return
    }

    AppIconButton(
        onClick = onClick,
        modifier = modifier.size(outerSize)
    ) {
        OverlayPlaybackIcon(
            isPlaying = isPlaying,
            glyphSize = glyphSize
        )
    }
}

@Composable
private fun OverlayPlaybackIcon(
    isPlaying: Boolean,
    glyphSize: Dp
) {
    AppIcon(
        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
        contentDescription = if (isPlaying) "暂停" else "播放",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(glyphSize)
    )
}
