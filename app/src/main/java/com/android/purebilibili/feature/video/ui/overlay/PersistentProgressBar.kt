package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

internal fun resolvePersistentProgressFraction(current: Long, duration: Long): Float {
    return resolveProgressFraction(
        positionMs = current,
        durationMs = duration
    )
}

@Composable
internal fun PersistentBottomProgressBar(
    current: Long,
    duration: Long,
    modifier: Modifier = Modifier
) {
    val progress = resolvePersistentProgressFraction(current = current, duration = duration)
    if (duration <= 0L) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(
                color = Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(0.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(2.dp)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}
