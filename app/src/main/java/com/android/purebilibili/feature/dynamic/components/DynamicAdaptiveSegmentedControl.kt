package com.android.purebilibili.feature.dynamic.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.android.purebilibili.feature.home.components.BottomBarLiquidSegmentedControl
import top.yukonga.miuix.kmp.blur.Backdrop

/** Liquid reuse is opt-in; the disabled path is the active theme's native segmented control. */
@Composable
internal fun DynamicAdaptiveSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    itemWidth: Dp?,
    height: Dp,
    indicatorHeight: Dp,
    labelFontSize: TextUnit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
) {
    if (items.isEmpty()) return
    BottomBarLiquidSegmentedControl(
        // Glass indicator fills the measured slot; native geometry keeps its defaults.
        liquidGlassAdaptIndicatorToLabel = !com.android.purebilibili.core.ui.LocalAppThemeConfig.current.liquidGlassEnabled,
        items = items,
        selectedIndex = selectedIndex,
        onSelected = onSelected,
        itemWidth = itemWidth,
        height = height,
        indicatorHeight = indicatorHeight,
        labelFontSize = labelFontSize,
        modifier = modifier,
        miuixBackdrop = backdrop,
        nativeSegmentedControl = true,
        liquidGlassEffectsEnabled = true,
        dragSelectionEnabled = items.size > 1,
        tapPressRefractionEnabled = true,
    )
}
