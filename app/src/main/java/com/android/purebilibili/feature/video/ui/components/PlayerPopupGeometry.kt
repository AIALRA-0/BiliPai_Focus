package com.android.purebilibili.feature.video.ui.components

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

internal data class PlayerPopupViewport(val bounds: IntRect, val maxHeight: Int)

/** 缩窗后的旧锚点收回当前安全区，避免菜单仍围绕窗口外的按钮位置测量。 */
internal fun resolvePlayerPopupAnchorBounds(anchor: IntRect, safe: IntRect): IntRect = IntRect(
    anchor.left.coerceIn(safe.left, safe.right),
    anchor.top.coerceIn(safe.top, safe.bottom),
    anchor.right.coerceIn(safe.left, safe.right),
    anchor.bottom.coerceIn(safe.top, safe.bottom),
)

/** All values are host-window pixels, including the measured control-row anchor. */
internal fun resolvePlayerPopupViewport(
    safeWindowBounds: IntRect,
    requestedMaxHeight: Int?,
    horizontalMargin: Int,
    verticalMargin: Int,
    anchorBounds: IntRect? = null,
    anchorGap: Int = 0,
): PlayerPopupViewport {
    val horizontalInset = horizontalMargin.coerceIn(0, safeWindowBounds.width.coerceAtLeast(0) / 2)
    val verticalInset = verticalMargin.coerceIn(0, safeWindowBounds.height.coerceAtLeast(0) / 2)
    val bounds = IntRect(
        safeWindowBounds.left + horizontalInset,
        safeWindowBounds.top + verticalInset,
        safeWindowBounds.right - horizontalInset,
        safeWindowBounds.bottom - verticalInset,
    )
    val availableHeight = if (anchorBounds == null) {
        bounds.height
    } else {
        maxOf(
            (bounds.bottom - anchorBounds.bottom - anchorGap).coerceIn(0, bounds.height),
            (anchorBounds.top - bounds.top - anchorGap).coerceIn(0, bounds.height),
        )
    }
    return PlayerPopupViewport(
        bounds = bounds,
        maxHeight = (requestedMaxHeight ?: availableHeight).coerceIn(0, availableHeight),
    )
}

internal fun resolvePlayerPopupOffset(
    viewport: IntRect,
    popupSize: IntSize,
    placement: PlayerListPopupPlacement,
    anchorBounds: IntRect,
    layoutDirection: LayoutDirection,
    anchorGap: Int,
    horizontalAnchorMargin: Int = 0,
): IntOffset {
    val endX = if (layoutDirection == LayoutDirection.Ltr) {
        anchorBounds.right - popupSize.width - horizontalAnchorMargin
    } else {
        anchorBounds.left + horizontalAnchorMargin
    }
    val belowY = anchorBounds.bottom + anchorGap
    val aboveY = anchorBounds.top - anchorGap - popupSize.height
    val belowSpace = viewport.bottom - belowY
    val aboveSpace = anchorBounds.top - anchorGap - viewport.top
    val x = when (placement) {
        PlayerListPopupPlacement.CENTER -> viewport.left + (viewport.width - popupSize.width) / 2
        PlayerListPopupPlacement.END_BOTTOM -> if (layoutDirection == LayoutDirection.Ltr) {
            viewport.right - popupSize.width
        } else {
            viewport.left
        }
        PlayerListPopupPlacement.START -> if (layoutDirection == LayoutDirection.Ltr) {
            anchorBounds.left + horizontalAnchorMargin
        } else {
            anchorBounds.right - popupSize.width - horizontalAnchorMargin
        }
        PlayerListPopupPlacement.END,
        PlayerListPopupPlacement.ANCHOR_END -> endX
    }
    val y = when (placement) {
        PlayerListPopupPlacement.CENTER -> viewport.top + (viewport.height - popupSize.height) / 2
        PlayerListPopupPlacement.END_BOTTOM -> viewport.bottom - popupSize.height
        PlayerListPopupPlacement.ANCHOR_END -> when {
            popupSize.height <= aboveSpace -> aboveY
            popupSize.height <= belowSpace -> belowY
            aboveSpace >= belowSpace -> aboveY
            else -> belowY
        }
        PlayerListPopupPlacement.START,
        PlayerListPopupPlacement.END -> when {
            popupSize.height <= belowSpace -> belowY
            popupSize.height <= aboveSpace -> aboveY
            else -> anchorBounds.top + (anchorBounds.height - popupSize.height) / 2
        }
    }
    return IntOffset(
        x.coerceIn(viewport.left, (viewport.right - popupSize.width).coerceAtLeast(viewport.left)),
        y.coerceIn(viewport.top, (viewport.bottom - popupSize.height).coerceAtLeast(viewport.top)),
    )
}
