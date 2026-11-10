package com.android.purebilibili.feature.home.components

import com.android.purebilibili.core.ui.animation.LiquidIndicatorMotion

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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

/** Signed speed near the logical viewport edges; zero in the middle or invalid geometry. */
internal fun resolveDockDragEdgeScrollFraction(
    indicatorCenterPx: Float,
    viewportWidthPx: Float,
    edgeWidthPx: Float,
): Float {
    if (!indicatorCenterPx.isFinite() || !viewportWidthPx.isFinite() ||
        !edgeWidthPx.isFinite() || viewportWidthPx <= 0f || edgeWidthPx <= 0f
    ) return 0f
    val edge = edgeWidthPx.coerceAtMost(viewportWidthPx / 2f)
    return when {
        indicatorCenterPx < edge -> -((edge - indicatorCenterPx) / edge).coerceIn(0f, 1f)
        indicatorCenterPx > viewportWidthPx - edge ->
            ((indicatorCenterPx - viewportWidthPx + edge) / edge).coerceIn(0f, 1f)
        else -> 0f
    }
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


internal const val FLOATING_DOCK_INDICATOR_VELOCITY_DIVISOR = LiquidIndicatorMotion.VelocityDivisor
internal const val FLOATING_DOCK_INDICATOR_VELOCITY_SCALE_X_MULTIPLIER = 0.75f
internal const val FLOATING_DOCK_INDICATOR_VELOCITY_CLAMP = LiquidIndicatorMotion.VelocityClamp

enum class FloatingBottomBarGeometryMode { Dock, Segmented, TopNavigation, CompactDock }

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

/** The pill's overflow above/below the track, expressed before its layer scale. */
internal fun resolveFloatingDockChromaticGapBand(
    indicatorHeight: Float,
    shellHeight: Float,
    indicatorScaleY: Float,
    shellScaleY: Float = 1f,
): Float {
    if (!indicatorHeight.isFinite() || !shellHeight.isFinite() ||
        !indicatorScaleY.isFinite() || !shellScaleY.isFinite() ||
        indicatorHeight <= 0f || shellHeight <= 0f || indicatorScaleY <= 0f || shellScaleY <= 0f
    ) return 0f
    val localTrackHeight = shellHeight * shellScaleY / indicatorScaleY
    return ((indicatorHeight - localTrackHeight) / 2f).coerceIn(0f, indicatorHeight / 2f)
}

/** Motion feedback also works when pager follow bypasses the local press spring. */
internal fun resolveFloatingDockSelectionMotionProgress(position: Float, targetPosition: Float): Float {
    if (!position.isFinite() || !targetPosition.isFinite()) return 0f
    val distance = maxOf(abs(position - targetPosition), abs(position - position.roundToInt()))
    // Ignore tiny resting offsets; ramp to full optical feedback over a quarter slot.
    return ((distance - 0.015f) / 0.235f).coerceIn(0f, 1f)
}

internal fun resolveFloatingDockRefractionProgress(
    pressProgress: Float,
    tapPressRefractionEnabled: Boolean,
    isDragging: Boolean,
    isPagerScrolling: Boolean,
    selectionMotionProgress: Float = 0f,
): Float {
    val press = if (tapPressRefractionEnabled || isDragging || isPagerScrolling) pressProgress else 0f
    // Opting out of stationary tap feedback must never turn off a moving selection's lens.
    return maxOf(press, selectionMotionProgress).coerceIn(0f, 1f)
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
    if (geometryMode == FloatingBottomBarGeometryMode.TopNavigation) {
        // Top labels and icons share one inset shell. Slot width must not flatten
        // the indicator vertically; overflow is handled by the top rail's layout.
        return min(requestedHeightDp, shellHeightDp ?: requestedHeightDp).coerceAtLeast(0f)
    }
    if (geometryMode == FloatingBottomBarGeometryMode.Segmented) {
        // Keep the resting capsule wider than it is tall. Pressed height and
        // capture overflow are resolved separately, so drag bloom can exceed the shell.
        val insetHeight = shellHeightDp?.let { min(requestedHeightDp, (it - 6f).coerceAtLeast(0f)) }
            ?: requestedHeightDp
        return resolveSegmentedControlIndicatorHeightDp(tabWidthDp, insetHeight)
    }
    // The bottom dock retains its reference height and slot-aspect policy.
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
    return LiquidIndicatorMotion.scaleX(baseScaleX, velocity)
}

internal fun resolveFloatingDockIndicatorLayerScaleY(
    baseScaleY: Float,
    velocity: Float,
): Float {
    return LiquidIndicatorMotion.scaleY(baseScaleY, velocity)
}

internal fun resolveFloatingDockCapturedContentHorizontalScale(
    itemScale: Float,
    indicatorScaleX: Float,
    indicatorScaleY: Float,
): Float {
    val stretch = if (indicatorScaleY <= 0.001f) 1f else indicatorScaleX / indicatorScaleY
    return itemScale / stretch.coerceAtLeast(0.001f)
}

/** Extra half-slot around a content-sized endpoint belongs outside the visible shell. */
internal fun resolveFloatingDockContentShellInsetPx(slotWidthPx: Float, capsuleWidthPx: Float): Float =
    ((slotWidthPx - capsuleWidthPx.coerceIn(0f, slotWidthPx.coerceAtLeast(0f))) / 2f)
        .coerceAtLeast(0f)

/** Scroll viewport trims endpoint slack only while that content edge is visible. */
internal fun resolveFloatingDockScrollableShellInsetPx(
    slotWidthPx: Float,
    capsuleWidthPx: Float,
    distanceFromContentEdgePx: Float,
): Float = (resolveFloatingDockContentShellInsetPx(slotWidthPx, capsuleWidthPx) -
    distanceFromContentEdgePx.coerceAtLeast(0f)).coerceAtLeast(0f)

/** Interpolate label-sized capsules on the same continuous position as their translation. */
internal fun resolveFloatingDockContentIndicatorWidthPx(
    position: Float,
    contentWidthsPx: List<Float>,
    maximumWidthPx: Float,
): Float {
    if (contentWidthsPx.isEmpty() || !position.isFinite()) return maximumWidthPx
    val safePosition = position.coerceIn(0f, contentWidthsPx.lastIndex.toFloat())
    val start = safePosition.toInt()
    val end = (start + 1).coerceAtMost(contentWidthsPx.lastIndex)
    val fraction = safePosition - start
    val width = contentWidthsPx[start] + (contentWidthsPx[end] - contentWidthsPx[start]) * fraction
    return width.coerceIn(0f, maximumWidthPx.coerceAtLeast(0f))
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

/** Clamp in logical scroll coordinates so RTL and LTR share the same visible dock bounds. */
internal fun resolveFloatingDockViewportIndicatorPosition(
    position: Float,
    tabWidthPx: Float,
    tabsCount: Int,
    indicatorWidthPx: Float,
    scrollOffsetPx: Float,
    viewportWidthPx: Float,
    horizontalPaddingPx: Float,
    clampToViewport: Boolean = true,
): Float {
    // A browse-only rail owns selection by identity. Moving its viewport must not relocate
    // that selection onto a different visible label when the selected tab scrolls offscreen.
    if (!clampToViewport) return position
    if (tabWidthPx <= 0f || indicatorWidthPx <= 0f || viewportWidthPx <= 0f) return position
    val maxOffset = (tabWidthPx * tabsCount.coerceAtLeast(1) - indicatorWidthPx).coerceAtLeast(0f)
    val viewportStart = scrollOffsetPx.coerceIn(0f, maxOffset)
    val viewportEnd = (scrollOffsetPx + viewportWidthPx -
        horizontalPaddingPx.coerceAtLeast(0f) * 2f - indicatorWidthPx)
        .coerceIn(viewportStart, maxOffset)
    val offset = resolveFloatingDockIndicatorOffsetPx(
        position, tabWidthPx, tabsCount, indicatorWidthPx,
    ).coerceIn(viewportStart, viewportEnd)
    return (offset - (tabWidthPx - indicatorWidthPx) / 2f) / tabWidthPx
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

/** Every rail uses the same shell-relative bloom, including compact text categories. */
@Suppress("UNUSED_PARAMETER")
internal fun resolveFloatingDockIndicatorGeometry(
    shellHeightDp: Float,
    indicatorHeightDp: Float,
    geometryMode: FloatingBottomBarGeometryMode,
): com.android.purebilibili.core.ui.MatchedLiquidIndicatorGeometry =
    com.android.purebilibili.core.ui.resolveMatchedLiquidIndicatorGeometry(
        dockHeightDp = shellHeightDp,
        indicatorHeightDp = indicatorHeightDp,
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
    val geometry = resolveFloatingDockIndicatorGeometry(
        shellHeightDp = shellHeightDp,
        indicatorHeightDp = fittedIndicatorHeightDp,
        geometryMode = geometryMode,
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

/** 原生前景始终可见；只有玻璃捕获副本接管文字时才淡出底层。 */
internal fun resolveFloatingDockBaseContentAlpha(isLiquidGlass: Boolean, coverage: Float): Float =
    if (isLiquidGlass) 1f - coverage.coerceIn(0f, 1f) else 1f

/** 导航指示器与外壳使用相同内缩轮廓；横纵布局都先按移动轴/跨轴归一化。 */
internal fun resolveFloatingDockIndicatorCornerRadiusDp(
    shellThicknessDp: Float,
    indicatorThicknessDp: Float,
    indicatorSlotExtentDp: Float,
): Float {
    val inset = ((shellThicknessDp - indicatorThicknessDp) / 2f).coerceAtLeast(0f)
    return (shellThicknessDp / 2f - inset)
        .coerceIn(0f, minOf(indicatorThicknessDp, indicatorSlotExtentDp).coerceAtLeast(0f) / 2f)
}
