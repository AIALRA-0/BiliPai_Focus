package com.android.purebilibili.feature.home.components.liquid

import com.android.purebilibili.core.ui.AppSpacingTokens

import com.android.purebilibili.core.ui.OpticalContrastPalette

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.innerShadow as officialInnerShadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset

@Immutable
data class InnerShadow(
    val radius: Dp = AppSpacingTokens.ExtraLarge,
    val offset: DpOffset = DpOffset(AppSpacingTokens.None, radius),
    val color: Color = OpticalContrastPalette.Shadow.copy(alpha = 0.15f),
    val alpha: Float = 1f,
    val blendMode: BlendMode = DrawScope.DefaultBlendMode,
) {
    companion object {
        @Stable
        val Default: InnerShadow = InnerShadow()
    }
}

/** Converts caller-owned material parameters to the official shadow scope without changing layout. */
fun Modifier.innerShadow(
    shape: Shape,
    shadow: () -> InnerShadow?,
): Modifier = officialInnerShadow(shape) {
    val current = shadow()
    radius = current?.radius?.toPx()?.coerceAtLeast(0f) ?: 0f
    offset = current?.offset?.let { Offset(it.x.toPx(), it.y.toPx()) } ?: Offset.Zero
    color = current?.color ?: Color.Transparent
    alpha = current?.alpha?.coerceIn(0f, 1f) ?: 0f
    blendMode = current?.blendMode ?: BlendMode.SrcOver
}
