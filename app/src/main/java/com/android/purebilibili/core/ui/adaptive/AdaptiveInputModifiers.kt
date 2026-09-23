package com.android.purebilibili.core.ui.adaptive

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.util.LocalAppWindowAdaptiveInfo

/** Lightweight pointer-only lift used by card surfaces on mouse/trackpad devices. */
@Composable
internal fun Modifier.adaptiveCardHoverEffect(
    shape: Shape,
    enabled: Boolean = resolveInputDevicePolicy(LocalAppWindowAdaptiveInfo.current).enableHoverEffects,
): Modifier {
    if (!enabled) return this

    val interactionSource = remember { MutableInteractionSource() }
    val hovered = interactionSource.collectIsHoveredAsState()
    val transition = updateTransition(
        targetState = hovered.value,
        label = "adaptiveCardHover",
    )
    val scale = transition.animateFloat(
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )
        },
        label = "adaptiveCardHoverScale",
    ) { isHovered -> if (isHovered) 1.025f else 1f }
    val shadowElevationDp = transition.animateFloat(
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )
        },
        label = "adaptiveCardHoverShadow",
    ) { isHovered -> if (isHovered) 10f else 0f }

    return hoverable(interactionSource = interactionSource)
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            shadowElevation = shadowElevationDp.value.dp.toPx()
            this.shape = shape
            clip = false
        }
}
