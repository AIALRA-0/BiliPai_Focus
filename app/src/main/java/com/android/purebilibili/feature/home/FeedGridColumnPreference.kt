package com.android.purebilibili.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.android.purebilibili.core.store.HomeSettings
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.util.WindowWidthSizeClass
import kotlinx.coroutines.launch

internal val HomeSettings.effectiveGridColumnCount: Int
    get() = if (gridColumnCustomizationEnabled) gridColumnCount else 0

internal val HomeSettings.effectiveGridColumnCountCompact: Int
    get() = if (gridColumnCustomizationEnabled) gridColumnCountCompact else 0

internal val HomeSettings.gridColumnPinchEnabled: Boolean
    get() = gridColumnCustomizationEnabled && pinchToChangeGridColumnsEnabled

internal class FeedGridColumnPreference(
    val columns: Int,
    val onChange: (Int) -> Unit,
    val onGestureEnd: (Int) -> Unit,
)

internal fun resolveFeedGridColumnPreference(
    customizationEnabled: Boolean,
    defaultColumns: Int,
    pageColumns: Int?,
    bounds: IntRange,
    minimumColumns: Int = 1,
): Int {
    val requestedColumns = if (customizationEnabled) pageColumns ?: defaultColumns else defaultColumns
    // 默认列数也受当前窗口上限约束；临时降列不修改保存值，放大后恢复。
    val effectiveMinimum = if (customizationEnabled) minimumColumns.coerceIn(1, bounds.last) else 1
    return requestedColumns.coerceIn(effectiveMinimum, bounds.last)
}

/** 网格行为与材质无关；窄屏和宽屏分别保存，关闭共用后仅保留各页面自己的手势选择。 */
@Composable
internal fun rememberFeedGridColumnPreference(
    settings: HomeSettings,
    widthSizeClass: WindowWidthSizeClass,
    defaultColumns: Int,
    bounds: IntRange,
    customizationApplies: Boolean = true,
    minimumColumns: Int = 1,
): FeedGridColumnPreference {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val compact = isCompactHomeFeedScreen(widthSizeClass)
    var pageColumns by rememberSaveable(
        settings.gridColumnCountCompact,
        settings.gridColumnCount,
        settings.gridColumnsSharedAcrossPages,
        settings.gridColumnCustomizationEnabled,
    ) { mutableStateOf(mapOf<Boolean, Int>()) }
    val customizationEnabled = settings.gridColumnCustomizationEnabled && customizationApplies
    val columns = resolveFeedGridColumnPreference(
        customizationEnabled = customizationEnabled,
        defaultColumns = defaultColumns,
        pageColumns = pageColumns[compact],
        bounds = bounds,
        minimumColumns = minimumColumns,
    )
    return FeedGridColumnPreference(
        columns = columns,
        onChange = { pageColumns = pageColumns + (compact to it.coerceIn(bounds)) },
        onGestureEnd = { finalColumns ->
            val count = finalColumns.coerceIn(bounds)
            pageColumns = pageColumns + (compact to count)
            if (customizationEnabled && settings.gridColumnsSharedAcrossPages) {
                scope.launch {
                    if (compact) SettingsManager.setGridColumnCountCompact(context, count)
                    else SettingsManager.setGridColumnCount(context, count)
                }
            }
        },
    )
}
