package com.android.purebilibili.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.blur.unifiedBlur
import dev.chrisbanes.haze.HazeState

@Composable
fun TopReadabilityChrome(
    modifier: Modifier = Modifier,
    height: Dp = 104.dp,
    surfaceColor: Color = AppSurfaceTokens.groupedListContainer(),
    surfaceAlpha: Float = 0.74f,
    hazeState: HazeState? = null,
    hazeEnabled: Boolean = false
) {
    val heightDp = height.value.toInt()
    val spec = remember(heightDp, surfaceColor, surfaceAlpha, hazeState, hazeEnabled) {
        resolveTopReadabilityChromeSpec(
            requestedHeightDp = heightDp,
            surfaceColor = surfaceColor,
            surfaceAlpha = surfaceAlpha,
            hazeRequested = hazeEnabled,
            hasHazeState = hazeState != null
        )
    }
    if (spec.heightDp <= 0) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(spec.heightDp.dp)
            .then(
                if (spec.useHaze && hazeState != null) {
                    Modifier.unifiedBlur(
                        hazeState = hazeState,
                        shape = RectangleShape
                    )
                } else {
                    Modifier
                }
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        spec.surfaceColor.copy(alpha = spec.surfaceAlpha),
                        spec.surfaceColor.copy(alpha = spec.surfaceAlpha * 0.72f),
                        spec.surfaceColor.copy(alpha = spec.bottomAlpha)
                    )
                )
            )
    )
}
