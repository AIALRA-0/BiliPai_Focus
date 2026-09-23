package com.android.purebilibili.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle

/** Shared content-card decisions for feed / search / dynamic list shells. */
data class ContentCardSurfaceSpec(
    val usesTonalContainerTreatment: Boolean,
    val cornerLevel: ContainerLevel,
    val borderWidthDp: Float,
    val borderAlpha: Float,
    val tonalElevationDp: Float,
    val shadowElevationDp: Float
)

fun resolveContentCardSurfaceSpec(
    uiStyle: AppUiStyle
): ContentCardSurfaceSpec = when (uiStyle) {
    AppUiStyle.MIUIX -> ContentCardSurfaceSpec(
        usesTonalContainerTreatment = true,
        cornerLevel = ContainerLevel.Card,
        borderWidthDp = 0.8f,
        borderAlpha = 0.22f,
        tonalElevationDp = 0f,
        shadowElevationDp = 0f
    )
    AppUiStyle.MATERIAL3 -> ContentCardSurfaceSpec(
        usesTonalContainerTreatment = false,
        cornerLevel = ContainerLevel.Card,
        borderWidthDp = 0f,
        borderAlpha = 0f,
        tonalElevationDp = 0f,
        shadowElevationDp = 0f
    )
}

@Composable
fun rememberContentCardSurfaceSpec(): ContentCardSurfaceSpec {
    val uiStyle = LocalAppUiStyle.current
    return remember(uiStyle) {
        resolveContentCardSurfaceSpec(uiStyle)
    }
}

fun resolveContentCardCornerDp(
    uiStyle: AppUiStyle
): Dp = AppShapes.resolveContainerCornerDp(
    level = ContainerLevel.Card,
    uiStyle = uiStyle
)
