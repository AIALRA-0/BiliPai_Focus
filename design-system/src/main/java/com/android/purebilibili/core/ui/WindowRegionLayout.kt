package com.android.purebilibili.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/** Places pane roots in physical safe regions, independently of UI density and layout direction. */
@Composable
fun WindowRegionLayout(
    regionProvider: (IntSize, IntOffset) -> List<IntRect>,
    primaryContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    secondaryContent: (@Composable () -> Unit)? = null,
    tertiaryContent: (@Composable () -> Unit)? = null,
) {
    var windowOrigin by remember { mutableStateOf(IntOffset.Zero) }
    Layout(
        modifier = modifier
            .clipToBounds()
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInWindow()
                val next = IntOffset(position.x.roundToInt(), position.y.roundToInt())
                if (windowOrigin != next) windowOrigin = next
            },
        content = {
            Box(Modifier.clipToBounds()) { primaryContent() }
            if (secondaryContent != null) Box(Modifier.clipToBounds()) { secondaryContent() }
            if (tertiaryContent != null) Box(Modifier.clipToBounds()) { tertiaryContent() }
        },
    ) { measurables, constraints ->
        val size = IntSize(constraints.maxWidth, constraints.maxHeight)
        val regions = regionProvider(size, windowOrigin)
        val empty = IntRect(0, 0, 0, 0)
        val slots = buildList {
            add(regions.firstOrNull() ?: empty)
            if (secondaryContent != null) add(if (regions.size > 1) regions.last() else empty)
            if (tertiaryContent != null) add(if (regions.size > 2) regions[1] else empty)
        }
        val placeables = measurables.mapIndexed { index, measurable ->
            val region = slots[index]
            measurable.measure(Constraints.fixed(region.width, region.height)) to region
        }
        layout(size.width, size.height) {
            placeables.forEach { (placeable, region) -> placeable.place(region.left, region.top) }
        }
    }
}
