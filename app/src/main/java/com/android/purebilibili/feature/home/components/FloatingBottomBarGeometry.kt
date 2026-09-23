package com.android.purebilibili.feature.home.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Home dock resting indicator is 56dp in a typically ~75dp slot (~1.35). */
internal const val FLOATING_DOCK_MIN_INDICATOR_ASPECT = 1.35f

internal const val FLOATING_DOCK_PREDICTIVE_BACK_EDGE_DP = 24f

/** Geometry copied from the Miuix liquid navigation sample for the standard bottom dock. */
internal const val MIUIX_UPSTREAM_DOCK_SHELL_HEIGHT_DP = 64f

internal const val MIUIX_UPSTREAM_DOCK_SHELL_LENS_DP = 24f

internal const val MIUIX_UPSTREAM_DOCK_PRESS_BLOOM_DP = 16f

internal const val MIUIX_UPSTREAM_DOCK_INDICATOR_LENS_HEIGHT_DP = 10f

internal const val MIUIX_UPSTREAM_DOCK_INDICATOR_LENS_AMOUNT_DP = 14f

internal const val MIUIX_UPSTREAM_DOCK_INNER_SHADOW_RADIUS_DP = 8f

internal const val FLOATING_DOCK_TAB_PRESS_SCALE_EXTRA = 0.2f

internal const val DOCK_HIGHLIGHT_COMPACT_RADIUS_FACTOR = 1.2f
internal const val DOCK_HIGHLIGHT_SLOT_COVER_FACTOR = 0.5f
internal const val DOCK_PILL_HIGHLIGHT_BASE_WIDTH_DP = 1f
internal const val DOCK_PILL_HIGHLIGHT_MAX_WIDTH_DP = 2f
internal const val DOCK_PILL_HIGHLIGHT_ASPECT_SPAN = 8f

/**
 * Home dock tabs are nearly square, so [size.minDimension] * 1.2 already covers the pill.
 * Two-item rows such as 番剧/影视 stretch to a long capsule; keep the compact radius as a
 * floor and expand to half the slot so the press highlight still fills the selected item.
 */
internal fun resolveDockInteractiveHighlightRadiusPx(
    shellMinDimensionPx: Float,
    tabWidthPx: Float,
): Float {
    val compactRadius = shellMinDimensionPx.coerceAtLeast(0f) * DOCK_HIGHLIGHT_COMPACT_RADIUS_FACTOR
    val slotCoverRadius = tabWidthPx.coerceAtLeast(0f) * DOCK_HIGHLIGHT_SLOT_COVER_FACTOR
    return max(compactRadius, slotCoverRadius)
}

/** Specular rim stays 1dp on compact docks and thickens slightly on long capsules. */
internal fun resolveDockPillHighlightWidthDp(
    indicatorWidthDp: Float,
    indicatorHeightDp: Float,
): Float {
    if (indicatorWidthDp <= 0f || indicatorHeightDp <= 0f) {
        return DOCK_PILL_HIGHLIGHT_BASE_WIDTH_DP
    }
    val aspect = indicatorWidthDp / indicatorHeightDp
    val extra = ((aspect - FLOATING_DOCK_MIN_INDICATOR_ASPECT) / DOCK_PILL_HIGHLIGHT_ASPECT_SPAN)
        .coerceIn(0f, 1f)
    return DOCK_PILL_HIGHLIGHT_BASE_WIDTH_DP +
        extra * (DOCK_PILL_HIGHLIGHT_MAX_WIDTH_DP - DOCK_PILL_HIGHLIGHT_BASE_WIDTH_DP)
}

private const val FLOATING_DOCK_MAX_VELOCITY_SCALE_X = 1.25f
private const val FLOATING_DOCK_MAX_VELOCITY_SCALE_Y = 1.2f
private const val FLOATING_DOCK_PANEL_OFFSET_DP = 4f

internal data class FloatingDockCaptureInsets(
    val horizontalDp: Float,
    val verticalDp: Float,
)

/**
 * The 64dp bottom dock uses the Miuix upstream geometry unchanged. Short chrome
 * (search 36dp, top tabs ~40dp) is not defined by that sample, so it derives the
 * same geometry proportionally from its height. This keeps top and bottom
 * refraction from meeting in the middle as a dark line.
 */
internal fun resolveFloatingDockGeometryScale(
    shellHeightDp: Float,
    referenceShellHeightDp: Float = MIUIX_UPSTREAM_DOCK_SHELL_HEIGHT_DP,
): Float {
    if (shellHeightDp <= 0f || referenceShellHeightDp <= 0f) return 0f
    return (shellHeightDp / referenceShellHeightDp).coerceIn(0f, 1f)
}

internal fun resolveCompactDockLensDp(shellHeightDp: Float): Float =
    MIUIX_UPSTREAM_DOCK_SHELL_LENS_DP * resolveFloatingDockGeometryScale(shellHeightDp)

internal fun resolveCompactDockPressBloomDp(shellHeightDp: Float): Float =
    MIUIX_UPSTREAM_DOCK_PRESS_BLOOM_DP * resolveFloatingDockGeometryScale(shellHeightDp)

internal fun resolveFloatingDockEffectPaddingDp(
    refractionAmountDp: Float,
    pressBloomDp: Float,
): Float = refractionAmountDp.coerceAtLeast(0f) + pressBloomDp.coerceAtLeast(0f)

internal fun resolveCompactDockIndicatorLensHeightDp(shellHeightDp: Float): Float =
    MIUIX_UPSTREAM_DOCK_INDICATOR_LENS_HEIGHT_DP *
        resolveFloatingDockGeometryScale(shellHeightDp)

internal fun resolveCompactDockIndicatorLensAmountDp(shellHeightDp: Float): Float =
    MIUIX_UPSTREAM_DOCK_INDICATOR_LENS_AMOUNT_DP *
        resolveFloatingDockGeometryScale(shellHeightDp)

internal fun resolveCompactDockInnerShadowRadiusDp(shellHeightDp: Float): Float =
    MIUIX_UPSTREAM_DOCK_INNER_SHADOW_RADIUS_DP *
        resolveFloatingDockGeometryScale(shellHeightDp)

internal fun resolveCompactDockTabPressScale(shellHeightDp: Float): Float =
    1f + FLOATING_DOCK_TAB_PRESS_SCALE_EXTRA * resolveFloatingDockGeometryScale(shellHeightDp)

/**
 * Half the extra height a pressed indicator needs beyond the dock.
 * Compact 40dp chrome must reserve this or the 78/56 bloom is clipped by
 * siblings such as the detail-page pager.
 */
internal fun resolveCompactDockScaleOverflowDp(
    shellHeightDp: Float,
    indicatorHeightDp: Float,
): Float {
    val geometry = com.android.purebilibili.core.ui.resolveMatchedLiquidIndicatorGeometry(
        dockHeightDp = shellHeightDp,
        indicatorHeightDp = indicatorHeightDp,
    )
    return ((geometry.pressedHeightDp - shellHeightDp) / 2f).coerceAtLeast(0f)
}

/** HyperIsland 静止几何：64dp 壳配 56dp 指示器，即上下各留 4dp（= 壳高 × 4/64）。 */
internal const val FLOATING_DOCK_REST_INDICATOR_INSET_RATIO = 4f / 64f

/** 静止时指示器单侧的垂直留白，按参考项目比例换算，任何壳高都得到相同的视觉边距。 */
internal fun resolveFloatingDockRestIndicatorVerticalInsetDp(
    shellHeightDp: Float,
): Float {
    if (shellHeightDp <= 0f) return 0f
    return shellHeightDp * FLOATING_DOCK_REST_INDICATOR_INSET_RATIO
}

/**
 * Press bloom needs extra vertical room. Only consume it from the layout when the
 * caller did not already lock height to the shell; otherwise the 56dp rest pill is
 * clamped to the same height as the 64dp dock and the idle inset becomes 0.
 */
internal fun shouldReserveFloatingDockScaleOverflow(
    incomingMaxHeightPx: Int,
    shellHeightPx: Int,
    overflowPx: Int,
): Boolean {
    if (overflowPx <= 0 || shellHeightPx <= 0) return false
    if (incomingMaxHeightPx == Constraints.Infinity) return true
    return incomingMaxHeightPx >= shellHeightPx + overflowPx * 2
}

internal fun Modifier.floatingDockScaleOverflow(
    overflow: Dp,
    shellHeight: Dp,
): Modifier = layout { measurable, constraints ->
    val overflowPx = overflow.roundToPx().coerceAtLeast(0)
    val shellPx = shellHeight.roundToPx().coerceAtLeast(0)
    val reserve = shouldReserveFloatingDockScaleOverflow(
        incomingMaxHeightPx = if (constraints.hasBoundedHeight) {
            constraints.maxHeight
        } else {
            Constraints.Infinity
        },
        shellHeightPx = shellPx,
        overflowPx = overflowPx,
    )
    if (!reserve) {
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            placeable.placeRelative(0, 0)
        }
    } else {
        val innerMaxHeight = if (constraints.hasBoundedHeight) {
            (constraints.maxHeight - overflowPx * 2).coerceAtLeast(0)
        } else {
            constraints.maxHeight
        }
        val placeable = measurable.measure(
            constraints.copy(minHeight = 0, maxHeight = innerMaxHeight)
        )
        val width = placeable.width.coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = (placeable.height + overflowPx * 2)
            .coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(width, height) {
            placeable.placeRelative(0, overflowPx)
        }
    }
}

internal const val FLOATING_DOCK_INDICATOR_VELOCITY_DIVISOR = 10f
internal const val FLOATING_DOCK_INDICATOR_VELOCITY_SCALE_X_MULTIPLIER = 0.75f
internal const val FLOATING_DOCK_INDICATOR_VELOCITY_CLAMP = 0.2f

enum class FloatingBottomBarGeometryMode { Dock, Segmented, TopNavigation }

internal fun resolveFloatingDockSlotWidthPx(
    containerWidthPx: Float,
    horizontalPaddingPx: Float,
    itemCount: Int,
): Float = (containerWidthPx - horizontalPaddingPx.coerceAtLeast(0f) * 2f)
    .coerceAtLeast(0f) / itemCount.coerceAtLeast(1)

internal fun resolveFloatingDockDragReferenceWidthPx(
    tabWidthPx: Float,
    horizontalPaddingPx: Float,
): Float = (tabWidthPx.coerceAtLeast(0f) * 5f + horizontalPaddingPx.coerceAtLeast(0f) * 2f)
    .coerceAtLeast(1f)

internal fun resolveFloatingDockRefractionProgress(
    pressProgress: Float,
    tapPressRefractionEnabled: Boolean,
    isDragging: Boolean,
    isPagerScrolling: Boolean,
): Float = if (tapPressRefractionEnabled || isDragging || isPagerScrolling) {
    pressProgress.coerceIn(0f, 1f)
} else {
    0f
}

internal fun resolveFloatingDockIndicatorHeightDp(
    requestedHeightDp: Float,
    tabWidthDp: Float,
    geometryMode: FloatingBottomBarGeometryMode = FloatingBottomBarGeometryMode.Dock,
    shellHeightDp: Float? = null,
    proportionalReferenceWidthDp: Float? = null,
): Float {
    if (requestedHeightDp <= 0f) return 0f
    if (tabWidthDp <= 0f) return requestedHeightDp
    if (geometryMode != FloatingBottomBarGeometryMode.Dock) {
        // Match the home dock's 2dp resting inset. The aspect constraint still wins
        // on narrow slots; never widen the pill across neighbouring destinations.
        val insetHeight = shellHeightDp?.let { (it - 4f).coerceAtLeast(0f) }
            ?: requestedHeightDp
        return if (geometryMode == FloatingBottomBarGeometryMode.TopNavigation) {
            min(insetHeight, tabWidthDp / FLOATING_DOCK_MIN_INDICATOR_ASPECT)
        } else {
            resolveSegmentedControlIndicatorHeightDp(tabWidthDp, insetHeight)
        }
    }
    // Dock 模式同样留出静止垂直边距，按 HyperIsland 的 64/56 比例（壳高 × 4/64）。
    // 兜底：调用方即使把高度传满壳高，指示器也不会贴住上下边缘。
    val restingInsetDp = shellHeightDp
        ?.let { resolveFloatingDockRestIndicatorVerticalInsetDp(it) }
        ?: 0f
    val restingHeightDp = (shellHeightDp ?: requestedHeightDp) - restingInsetDp * 2f
    val cappedHeightDp = min(requestedHeightDp, restingHeightDp).coerceAtLeast(0f)
    if (proportionalReferenceWidthDp != null && proportionalReferenceWidthDp > 0f) {
        val widthScale = (tabWidthDp / proportionalReferenceWidthDp).coerceIn(0f, 1f)
        return cappedHeightDp * widthScale
    }
    // A slot that is already wider than the pill can keep the authored height.
    // Forcing the 1.35 aspect here flattens icon+label after search takes a side slot.
    if (tabWidthDp >= cappedHeightDp) return cappedHeightDp
    val maxHeightForCapsule = tabWidthDp / FLOATING_DOCK_MIN_INDICATOR_ASPECT
    return min(cappedHeightDp, maxHeightForCapsule)
}

internal fun resolveFloatingDockIndicatorLayerScaleX(
    baseScaleX: Float,
    velocity: Float,
): Float {
    val normalizedVelocity = velocity / FLOATING_DOCK_INDICATOR_VELOCITY_DIVISOR
    val velocityScale = (normalizedVelocity * FLOATING_DOCK_INDICATOR_VELOCITY_SCALE_X_MULTIPLIER)
        .coerceIn(-FLOATING_DOCK_INDICATOR_VELOCITY_CLAMP, FLOATING_DOCK_INDICATOR_VELOCITY_CLAMP)
    return baseScaleX / (1f - velocityScale)
}

internal fun resolveFloatingDockIndicatorLayerScaleY(
    baseScaleY: Float,
    velocity: Float,
): Float {
    val normalizedVelocity = velocity / FLOATING_DOCK_INDICATOR_VELOCITY_DIVISOR
    val velocityScale = (normalizedVelocity * 0.25f)
        .coerceIn(-FLOATING_DOCK_INDICATOR_VELOCITY_CLAMP, FLOATING_DOCK_INDICATOR_VELOCITY_CLAMP)
    return baseScaleY * (1f - velocityScale)
}

internal fun resolveFloatingDockCapturedContentHorizontalScale(
    itemScale: Float,
    indicatorScaleX: Float,
    indicatorScaleY: Float,
): Float {
    val stretch = if (indicatorScaleY <= 0.001f) 1f else indicatorScaleX / indicatorScaleY
    return itemScale / stretch.coerceAtLeast(0.001f)
}

internal fun resolveFloatingDockIndicatorOffsetPx(
    position: Float,
    tabWidthPx: Float,
    tabsCount: Int,
    indicatorWidthPx: Float,
): Float {
    if (tabWidthPx <= 0f || indicatorWidthPx <= 0f) return 0f
    val safeTabsCount = tabsCount.coerceAtLeast(1)
    val centeredOffset = position * tabWidthPx + (tabWidthPx - indicatorWidthPx) / 2f
    val maxOffset = (tabWidthPx * safeTabsCount - indicatorWidthPx).coerceAtLeast(0f)
    return centeredOffset.coerceIn(0f, maxOffset)
}

internal fun resolveFloatingDockIndicatorContentAlignmentPx(
    position: Float,
    tabWidthPx: Float,
    tabsCount: Int,
    indicatorWidthPx: Float,
): Float {
    val centeredOffset = position * tabWidthPx + (tabWidthPx - indicatorWidthPx) / 2f
    return resolveFloatingDockIndicatorOffsetPx(
        position = position,
        tabWidthPx = tabWidthPx,
        tabsCount = tabsCount,
        indicatorWidthPx = indicatorWidthPx,
    ) - centeredOffset
}

internal fun resolveFloatingDockClippedContentTranslationPx(
    position: Float,
    tabWidthPx: Float,
    tabsCount: Int,
    indicatorWidthPx: Float,
): Float = -resolveFloatingDockIndicatorOffsetPx(
    position = position,
    tabWidthPx = tabWidthPx,
    tabsCount = tabsCount,
    indicatorWidthPx = indicatorWidthPx,
)

internal fun resolveFloatingDockCaptureInsets(
    shellHeightDp: Float,
    requestedIndicatorHeightDp: Float,
    indicatorWidthDp: Float,
    geometryMode: FloatingBottomBarGeometryMode = FloatingBottomBarGeometryMode.Dock,
): FloatingDockCaptureInsets {
    if (shellHeightDp <= 0f || indicatorWidthDp <= 0f) {
        return FloatingDockCaptureInsets(horizontalDp = 0f, verticalDp = 0f)
    }
    val fittedIndicatorHeightDp = resolveFloatingDockIndicatorHeightDp(
        requestedHeightDp = requestedIndicatorHeightDp,
        tabWidthDp = indicatorWidthDp,
        geometryMode = geometryMode,
        shellHeightDp = shellHeightDp,
    )
    val geometry = com.android.purebilibili.core.ui.resolveMatchedLiquidIndicatorGeometry(
        dockHeightDp = shellHeightDp,
        indicatorHeightDp = fittedIndicatorHeightDp,
    )
    val samplingReachDp = max(
        resolveCompactDockIndicatorLensHeightDp(shellHeightDp),
        resolveCompactDockIndicatorLensAmountDp(shellHeightDp),
    )
    val horizontalScale = geometry.pressedScale * FLOATING_DOCK_MAX_VELOCITY_SCALE_X
    val verticalPressedHeightDp = geometry.pressedHeightDp * FLOATING_DOCK_MAX_VELOCITY_SCALE_Y
    return FloatingDockCaptureInsets(
        horizontalDp = indicatorWidthDp * (horizontalScale - 1f).coerceAtLeast(0f) / 2f +
            samplingReachDp + FLOATING_DOCK_PANEL_OFFSET_DP,
        verticalDp = ((verticalPressedHeightDp - shellHeightDp) / 2f).coerceAtLeast(0f) +
            samplingReachDp + FLOATING_DOCK_PANEL_OFFSET_DP,
    )
}

internal fun resolveFloatingDockDragEdgeInsetPx(
    systemInsetPx: Float,
    fallbackPx: Float,
): Float = max(systemInsetPx, fallbackPx)

internal fun shouldAcceptFloatingDockDragAtWindowX(
    windowX: Float,
    screenWidthPx: Float,
    leftInsetPx: Float,
    rightInsetPx: Float,
): Boolean {
    if (screenWidthPx <= 0f) return true
    return windowX >= leftInsetPx && windowX <= screenWidthPx - rightInsetPx
}
