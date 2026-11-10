package com.android.purebilibili.core.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Press-driven horizontal shift with a physical spring bounce.
 *
 * Idle state centers [content] inside the available space; while [pressed] is true the
 * whole content travels to the leading edge and settles with a slight overshoot, like a
 * mass attached to a spring. Releasing springs it back to center.
 *
 * Apply globally to any search-entry pill: drive [pressed] from the same
 * [MutableInteractionSource] attached to the container's `clickable`.
 */
@Composable
fun AppPressShiftContent(
    pressed: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val contentSize = remember { mutableStateOf(IntSize.Zero) }
        // Distance from centered position to the leading edge; zero when content fills the width.
        val maxShiftPx = with(density) {
            ((maxWidth - contentSize.value.width.toDp()) / 2).coerceAtLeast(0.dp).toPx()
        }
        val shiftProgress by animateFloatAsState(
            targetValue = if (pressed) 1f else 0f,
            animationSpec = spring(
                dampingRatio = 0.62f,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "pressShiftProgress",
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset { IntOffset((-maxShiftPx * shiftProgress).roundToInt(), 0) }
                .onSizeChanged { contentSize.value = it },
            content = content,
        )
    }
}

/** Convenience: pressed state derived from the container's interaction source. */
@Composable
fun rememberPressShiftPressedState(interactionSource: MutableInteractionSource): Boolean {
    return interactionSource.collectIsPressedAsState().value
}
