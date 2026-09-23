package com.android.purebilibili.core.ui.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntRect
import com.android.purebilibili.core.util.AppWindowAdaptiveInfo
import com.android.purebilibili.core.util.LocalAppWindowAdaptiveInfo

internal fun resolveOccludingHingeInputBounds(
    adaptiveInfo: AppWindowAdaptiveInfo,
    containerWidthPx: Int,
    containerHeightPx: Int,
): IntRect? {
    return resolveOccludingHingeInputBoundsList(
        adaptiveInfo = adaptiveInfo,
        containerWidthPx = containerWidthPx,
        containerHeightPx = containerHeightPx,
    ).firstOrNull()
}

internal fun resolveOccludingHingeInputBoundsList(
    adaptiveInfo: AppWindowAdaptiveInfo,
    containerWidthPx: Int,
    containerHeightPx: Int,
): List<IntRect> {
    val hinges = adaptiveInfo.foldingFeature.hinges
    if (hinges.isNotEmpty()) {
        return com.android.purebilibili.core.util.resolveOccludingHingeBounds(
            hinges = hinges,
            containerWidthPx = containerWidthPx,
            containerHeightPx = containerHeightPx,
        )
    }
    if (!adaptiveInfo.foldingFeature.isOccluding) return emptyList()
    val hingeBounds = adaptiveInfo.foldingFeature.hingeBounds ?: return emptyList()
    val left = hingeBounds.left.coerceIn(0, containerWidthPx)
    val top = hingeBounds.top.coerceIn(0, containerHeightPx)
    val right = hingeBounds.right.coerceIn(0, containerWidthPx)
    val bottom = hingeBounds.bottom.coerceIn(0, containerHeightPx)
    if (right <= left || bottom <= top) return emptyList()
    return listOf(IntRect(left = left, top = top, right = right, bottom = bottom))
}

/**
 * Final input-safety net for a physically occluding fold.
 *
 * Feature layouts should still place content on either side of the hinge. This transparent layer
 * only guarantees that a forgotten or transiently moving control cannot receive input through the
 * occluded bounds while posture or window size is changing.
 */
@Composable
internal fun HingeOcclusionInputShield(
    modifier: Modifier = Modifier,
    adaptiveInfo: AppWindowAdaptiveInfo = LocalAppWindowAdaptiveInfo.current,
) {
    val occludingHingeCount = adaptiveInfo.foldingFeature.hinges.count { hinge -> hinge.isOccluding }
        .coerceAtLeast(if (adaptiveInfo.foldingFeature.isOccluding) 1 else 0)
    if (occludingHingeCount <= 0) {
        return
    }

    Layout(
        modifier = modifier,
        content = {
            repeat(occludingHingeCount) {
                Box(
                    modifier = Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial).changes.forEach { change ->
                                    change.consume()
                                }
                            }
                        }
                    },
                )
            }
        },
    ) { measurables, constraints ->
        val containerWidth = constraints.maxWidth
        val containerHeight = constraints.maxHeight
        val exclusionBounds = resolveOccludingHingeInputBoundsList(
            adaptiveInfo = adaptiveInfo,
            containerWidthPx = containerWidth,
            containerHeightPx = containerHeight,
        )
        val shields = measurables.zip(exclusionBounds) { measurable, bounds ->
            measurable.measure(
                Constraints.fixed(width = bounds.width, height = bounds.height),
            ) to bounds
        }

        layout(containerWidth, containerHeight) {
            shields.forEach { (shield, bounds) ->
                shield.place(x = bounds.left, y = bounds.top)
            }
        }
    }
}
