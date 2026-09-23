package com.android.purebilibili.feature.dynamic.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.purebilibili.core.store.HomeSettings
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.ui.components.AppNativeSegmentedControl
import com.android.purebilibili.core.ui.components.AppSegmentOption
import com.android.purebilibili.feature.home.components.BottomBarLiquidSegmentedControl
import top.yukonga.miuix.kmp.blur.Backdrop

/** Liquid reuse is opt-in; the disabled path is the active theme's native segmented control. */
@Composable
internal fun DynamicAdaptiveSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    itemWidth: Dp,
    height: Dp,
    indicatorHeight: Dp,
    labelFontSize: TextUnit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
) {
    if (items.isEmpty()) return
    val context = LocalContext.current
    val homeSettings by SettingsManager.getHomeSettings(context)
        .collectAsStateWithLifecycle(initialValue = HomeSettings())
    if (homeSettings.androidNativeLiquidGlassEnabled) {
        BottomBarLiquidSegmentedControl(
            items = items,
            selectedIndex = selectedIndex,
            onSelected = onSelected,
            itemWidth = itemWidth,
            height = height,
            indicatorHeight = indicatorHeight,
            labelFontSize = labelFontSize,
            modifier = modifier,
            miuixBackdrop = backdrop,
            liquidGlassEffectsEnabled = true,
            dragSelectionEnabled = items.size > 1,
            tapPressRefractionEnabled = true,
        )
    } else {
        val options = remember(items) {
            items.mapIndexed { index, label -> AppSegmentOption(index, label) }
        }
        AppNativeSegmentedControl(
            options = options,
            selectedValue = selectedIndex,
            modifier = modifier,
            onSelectionChange = onSelected,
        )
    }
}
