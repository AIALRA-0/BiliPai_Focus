package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.ui.AppChromeSizeTokens
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.feature.home.components.BottomBarLiquidSegmentedControl
import top.yukonga.miuix.kmp.blur.Backdrop

private val beta21AdaptiveTabMinWidth = 72.dp

internal fun resolveAppAdaptiveTabMinWidth(
    requestedMinTabWidth: Dp,
    uiStyle: AppUiStyle,
    liquidGlassEnabled: Boolean,
): Dp {
    if (requestedMinTabWidth.isSpecified) return requestedMinTabWidth
    return if (uiStyle == AppUiStyle.MIUIX && !liquidGlassEnabled) {
        AppChromeSizeTokens.MinimumTouchTarget
    } else {
        beta21AdaptiveTabMinWidth
    }
}

/**
 * App-wide category/page tab contract. The shared renderer keeps MD3's animated underline
 * while liquid glass is off, and switches every theme to the moving glass capsule when reuse
 * is enabled. The disabled path always delegates to the active theme's native tab row.
 */
@Composable
fun <T> AppThemeAdaptiveTabRow(
    options: List<AppSegmentOption<T>>,
    selectedValue: T,
    onSelectionChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    scrollable: Boolean = false,
    minTabWidth: Dp = Dp.Unspecified,
    compactMiuixWhenTwoOptions: Boolean = true,
    height: Dp = AppChromeSizeTokens.BottomBarMatchedSegmentedControlHeightDp.dp,
    indicatorHeight: Dp = AppChromeSizeTokens.BottomBarMatchedSegmentedIndicatorHeightDp.dp,
    labelFontSize: TextUnit = TextUnit.Unspecified,
    dragSelectionEnabled: Boolean? = null,
    tapPressRefractionEnabled: Boolean = true,
    miuixBackdrop: Backdrop? = null,
    preferInlineContentStyle: Boolean = false,
    indicatorPositionProvider: (() -> Float)? = null,
    isScrollInProgressProvider: () -> Boolean = { false },
) {
    AppLiquidAwareTabRow(
        options = options,
        selectedValue = selectedValue,
        onSelectionChange = onSelectionChange,
        modifier = modifier,
        enabled = enabled,
        scrollable = scrollable,
        minTabWidth = minTabWidth,
        compactMiuixWhenTwoOptions = compactMiuixWhenTwoOptions,
        height = height,
        indicatorHeight = indicatorHeight,
        labelFontSize = labelFontSize,
        dragSelectionEnabled = dragSelectionEnabled,
        tapPressRefractionEnabled = tapPressRefractionEnabled,
        miuixBackdrop = miuixBackdrop,
        preferInlineContentStyle = preferInlineContentStyle,
        indicatorPositionProvider = indicatorPositionProvider,
        isScrollInProgressProvider = isScrollInProgressProvider,
    )
}

/**
 * App-level tab row that reuses the same liquid indicator and interaction contract as the
 * floating home dock. Scrollable rows keep horizontal scrolling and expose long-press drag
 * selection so drag-to-scroll and drag-to-select do not compete for the same gesture.
 */
@Composable
fun <T> AppLiquidAwareTabRow(
    options: List<AppSegmentOption<T>>,
    selectedValue: T,
    onSelectionChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    scrollable: Boolean = false,
    minTabWidth: Dp = Dp.Unspecified,
    compactMiuixWhenTwoOptions: Boolean = true,
    height: Dp = AppChromeSizeTokens.BottomBarMatchedSegmentedControlHeightDp.dp,
    indicatorHeight: Dp = AppChromeSizeTokens.BottomBarMatchedSegmentedIndicatorHeightDp.dp,
    labelFontSize: TextUnit = TextUnit.Unspecified,
    dragSelectionEnabled: Boolean? = null,
    tapPressRefractionEnabled: Boolean = true,
    miuixBackdrop: Backdrop? = null,
    preferInlineContentStyle: Boolean = false,
    indicatorPositionProvider: (() -> Float)? = null,
    isScrollInProgressProvider: () -> Boolean = { false },
) {
    if (options.isEmpty()) return
    val uiStyle = LocalAppUiStyle.current
    val liquidGlassEnabled = com.android.purebilibili.core.ui.LocalAppThemeConfig.current.liquidGlassEnabled
    val resolvedMinTabWidth = resolveAppAdaptiveTabMinWidth(
        requestedMinTabWidth = minTabWidth,
        uiStyle = uiStyle,
        liquidGlassEnabled = liquidGlassEnabled,
    )
    if (!liquidGlassEnabled) {
        if (uiStyle == AppUiStyle.MIUIX && options.size <= 2 && compactMiuixWhenTwoOptions) {
            AppNativeSegmentedControl(
                options = options,
                selectedValue = selectedValue,
                onSelectionChange = onSelectionChange,
                modifier = modifier,
                enabled = enabled,
                indicatorPositionProvider = indicatorPositionProvider,
            )
        } else {
            AppNativeTabRow(
                options = options,
                selectedValue = selectedValue,
                onSelectionChange = onSelectionChange,
                modifier = modifier,
                enabled = enabled,
                scrollable = scrollable,
                minTabWidth = resolvedMinTabWidth,
                compactMiuixWhenTwoOptions = compactMiuixWhenTwoOptions,
                height = height,
                allowLabelOverflow = true,
                indicatorPositionProvider = indicatorPositionProvider,
            )
        }
        return
    }
    val selectedIndex = options.indexOfFirst { it.value == selectedValue }.coerceAtLeast(0)
    // All enabled liquid docks support direct dragging, including scrollable rails.
    val resolvedDragSelectionEnabled = dragSelectionEnabled ?: (enabled && options.size > 1)
    // Give every tab enough room for its longest label. The row itself remains
    // horizontally scrollable, so labels are never ellipsized or clipped on
    // narrow phones; this also applies to shared rows such as UP space tabs.
    val readableTabWidth = resolveReadableNativeTabMinWidth(
        requestedMinWidth = resolvedMinTabWidth,
        labels = options.map { it.label },
        allowLabelOverflow = true,
    )
    val viewportMaxWidth = LocalConfiguration.current.screenWidthDp.dp
    // Compact 2-option segmented controls should not turn into scrollable containers.
    val isCompact = (compactMiuixWhenTwoOptions && options.size <= 2) || (minTabWidth.isSpecified && !scrollable)
    // Liquid rows and MD3 retain beta.21's 72dp default and overflow contract. Only the
    // non-glass Miuix renderer uses beta.22's 48dp accessibility minimum.
    val needsHorizontalScroll = scrollable && !isCompact && (options.size > 4 || readableTabWidth > resolvedMinTabWidth)
    if (needsHorizontalScroll) {
        val scrollState = rememberScrollState()
        val density = LocalDensity.current
        BoxWithConstraints(
            modifier = modifier
                .widthIn(max = viewportMaxWidth)
                .liquidDockViewport(),
            contentAlignment = Alignment.CenterStart,
        ) {
            val viewportWidthPx = with(density) { maxWidth.toPx() }
            val itemWidthPx = with(density) { readableTabWidth.toPx() }
            val totalContentWidthPx = with(density) {
                (readableTabWidth * options.size + AppSpacingTokens.ExtraSmall * 2).toPx()
            }
            val contentOverflows = totalContentWidthPx > viewportWidthPx
            val dragFollowEdgePaddingPx = with(density) { AppSpacingTokens.Medium.toPx() }
            KeepScrollableTabSelectionVisible(
                scrollState = scrollState,
                selectedIndex = selectedIndex,
                itemWidthPx = itemWidthPx,
                viewportWidthPx = viewportWidthPx,
                contentPaddingPx = with(density) { AppSpacingTokens.ExtraSmall.toPx() },
            )
            BottomBarLiquidSegmentedControl(
                items = options.map { it.label },
                selectedIndex = selectedIndex,
                onSelected = { index ->
                    options.getOrNull(index)?.let { onSelectionChange(it.value) }
                },
                modifier = Modifier.liquidDockViewport(),
                scrollState = scrollState,
                enabled = enabled,
                itemWidth = readableTabWidth,
                height = height,
                indicatorHeight = indicatorHeight,
                labelFontSize = labelFontSize,
                liquidGlassEffectsEnabled = true,
                dragSelectionEnabled = resolvedDragSelectionEnabled,
                tapPressRefractionEnabled = tapPressRefractionEnabled,
                miuixBackdrop = miuixBackdrop,
                preferInlineContentStyle = preferInlineContentStyle,
                indicatorPositionProvider = indicatorPositionProvider,
                onIndicatorPositionChanged = { position ->
                    scrollState.dispatchRawDelta(
                        resolveScrollableTabIndicatorFollowDeltaPx(
                            indicatorPosition = position,
                            itemWidthPx = itemWidthPx,
                            viewportWidthPx = viewportWidthPx,
                            currentScrollPx = scrollState.value.toFloat(),
                            contentPaddingPx = with(density) {
                                AppSpacingTokens.ExtraSmall.toPx()
                            },
                            edgePaddingPx = dragFollowEdgePaddingPx,
                        )
                    )
                },
                isScrollInProgressProvider = isScrollInProgressProvider,
                externalPagerMotionEffectsEnabled = indicatorPositionProvider != null,
            )
        }
    } else {
        val rowModifier = if (isCompact) {
            modifier.wrapContentWidth(Alignment.CenterHorizontally)
        } else {
            modifier
        }
        val rowItemWidth = if (isCompact) {
            readableTabWidth
        } else {
            null
        }
        BottomBarLiquidSegmentedControl(
            items = options.map { it.label },
            selectedIndex = selectedIndex,
            onSelected = { index ->
                options.getOrNull(index)?.let { onSelectionChange(it.value) }
            },
            modifier = rowModifier,
            enabled = enabled,
            itemWidth = rowItemWidth,
            height = height,
            indicatorHeight = indicatorHeight,
            labelFontSize = labelFontSize,
            liquidGlassEffectsEnabled = true,
            dragSelectionEnabled = resolvedDragSelectionEnabled,
            tapPressRefractionEnabled = tapPressRefractionEnabled,
            miuixBackdrop = miuixBackdrop,
            preferInlineContentStyle = preferInlineContentStyle,
            indicatorPositionProvider = indicatorPositionProvider,
            isScrollInProgressProvider = isScrollInProgressProvider,
            externalPagerMotionEffectsEnabled = indicatorPositionProvider != null,
        )
    }
}
