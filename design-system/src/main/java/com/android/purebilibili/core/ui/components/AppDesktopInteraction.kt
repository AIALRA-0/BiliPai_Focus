package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.hoverable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme

/** Shared desktop affordances without adding a second focus target around the control. */
@Composable
fun Modifier.appDesktopInteractionVisuals(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    shape: Shape? = null,
): Modifier {
    if (!enabled) return this
    val focused by interactionSource.collectIsFocusedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val focusColor = MaterialTheme.colorScheme.primary
    val hoverColor = MaterialTheme.colorScheme.onSurface
    return this
        .pointerHoverIcon(PointerIcon.Hand)
        .drawWithContent {
            drawContent()
            val outline = shape?.createOutline(size, layoutDirection, this)
            if (hovered) {
                if (outline != null) {
                    drawOutline(outline, hoverColor.copy(alpha = 0.06f))
                } else {
                    drawRoundRect(
                        color = hoverColor.copy(alpha = 0.06f),
                        cornerRadius = CornerRadius(12.dp.toPx()),
                    )
                }
            }
            if (focused) {
                val stroke = Stroke(width = 2.dp.toPx())
                if (outline != null) {
                    drawOutline(outline, focusColor, style = stroke)
                } else {
                    drawRoundRect(
                        color = focusColor,
                        cornerRadius = CornerRadius(12.dp.toPx()),
                        style = stroke,
                    )
                }
            }
        }
}

/** Focus/hover visuals for controls whose implementation does not expose its interaction source. */
@Composable
fun Modifier.appDesktopFocusableItemVisuals(
    enabled: Boolean = true,
    shape: Shape? = null,
): Modifier {
    if (!enabled) return this
    val hoverInteractionSource = remember { MutableInteractionSource() }
    val hovered by hoverInteractionSource.collectIsHoveredAsState()
    var focused by remember { mutableStateOf(false) }
    val focusColor = MaterialTheme.colorScheme.primary
    val hoverColor = MaterialTheme.colorScheme.onSurface
    return this
        .pointerHoverIcon(PointerIcon.Hand)
        .hoverable(hoverInteractionSource)
        .onFocusChanged { focused = it.isFocused }
        .drawWithContent {
            drawContent()
            val outline = shape?.createOutline(size, layoutDirection, this)
            if (hovered) {
                if (outline != null) {
                    drawOutline(outline, hoverColor.copy(alpha = 0.06f))
                } else {
                    drawRoundRect(
                        color = hoverColor.copy(alpha = 0.06f),
                        cornerRadius = CornerRadius(12.dp.toPx()),
                    )
                }
            }
            if (focused) {
                val stroke = Stroke(width = 2.dp.toPx())
                if (outline != null) {
                    drawOutline(outline, focusColor, style = stroke)
                } else {
                    drawRoundRect(
                        color = focusColor,
                        cornerRadius = CornerRadius(12.dp.toPx()),
                        style = stroke,
                    )
                }
            }
        }
}
