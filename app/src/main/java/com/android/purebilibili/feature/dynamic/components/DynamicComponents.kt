package com.android.purebilibili.feature.dynamic.components

import com.android.purebilibili.core.ui.AppSpacingTokens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.rememberContentCardSurfaceSpec

/**
 *  Dynamic 模块专用的 GlassCard 组件
 *  针对列表性能进行了微调，减少过多层级
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    borderWidth: Dp? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val surfaceSpec = rememberContentCardSurfaceSpec()
    val resolvedShape = shape ?: AppShapes.borderedContainer(ContainerLevel.Card)
    val usesOutlinedContainer = surfaceSpec.borderWidthDp > 0f
    val resolvedBackground = backgroundColor ?: if (usesOutlinedContainer) {
        AppSurfaceTokens.surfaceContainer().copy(alpha = 0.92f)
    } else {
        AppSurfaceTokens.surface().copy(alpha = 0.6f)
    }
    val resolvedBorderColor = borderColor ?: if (usesOutlinedContainer) {
        AppSurfaceTokens.divider().copy(alpha = surfaceSpec.borderAlpha)
    } else {
        AppSurfaceTokens.divider().copy(alpha = 0.2f)
    }
    val resolvedBorderWidth = borderWidth ?: if (usesOutlinedContainer) {
        surfaceSpec.borderWidthDp.dp
    } else {
        AppSpacingTokens.Micro / 4
    }
    Box(
        modifier = modifier
            .clip(resolvedShape)
            .background(resolvedBackground)
            .border(resolvedBorderWidth, resolvedBorderColor, resolvedShape)
    ) {
        content()
    }
}
