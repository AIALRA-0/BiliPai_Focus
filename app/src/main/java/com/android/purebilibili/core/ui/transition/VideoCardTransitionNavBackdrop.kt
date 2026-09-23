package com.android.purebilibili.core.ui.transition

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color

@Composable
internal fun VideoCardTransitionNavBackdrop(
    visible: Boolean,
    progressProvider: () -> Float,
    phase: VideoCardTransitionBackgroundPhase,
    isLightBackground: Boolean,
    modifier: Modifier = Modifier,
    baseBackgroundColor: Color = MaterialTheme.colorScheme.background,
) {
    if (!visible) return
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val frame = resolveVideoCardTransitionNavBackdropFrame(
                    progress = progressProvider(),
                    phase = phase,
                    isLightBackground = isLightBackground,
                )
                val backdropColor = resolveVideoCardTransitionNavBackdropColor(
                    baseBackgroundColor = baseBackgroundColor,
                    frame = frame,
                )
                drawRect(backdropColor)
                VideoCardTransitionDiagnostics.onNavBackdropDrawn()
            },
    )
}
