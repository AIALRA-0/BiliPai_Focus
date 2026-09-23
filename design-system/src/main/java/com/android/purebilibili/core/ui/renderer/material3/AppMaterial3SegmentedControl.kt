package com.android.purebilibili.core.ui.renderer.material3

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.components.AppSegmentOption
import com.android.purebilibili.core.ui.components.AppSegmentedControlColors
import com.android.purebilibili.core.ui.components.AppPrimaryScrollableTabRow
import com.android.purebilibili.core.ui.components.AppPrimaryTabRow
import com.android.purebilibili.core.ui.components.resolveAppSegmentedLabelFontSizeSp
import com.android.purebilibili.core.ui.components.resolveAppSegmentedSelectionIndex

@Composable
internal fun <T> AppMaterial3SegmentedControl(
    options: List<AppSegmentOption<T>>,
    selectedValue: T,
    enabled: Boolean,
    colors: AppSegmentedControlColors,
    modifier: Modifier,
    onSelectionChange: (T) -> Unit,
) {
    val longestLabelLength = remember(options) {
        options.maxOfOrNull { it.label.length } ?: 0
    }
    val labelFontSize = remember(options.size, longestLabelLength) {
        resolveAppSegmentedLabelFontSizeSp(options.size, longestLabelLength).sp
    }
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            val selected = option.value == selectedValue
            SegmentedButton(
                selected = selected,
                onClick = { onSelectionChange(option.value) },
                enabled = enabled,
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = colors.activeContainerColor,
                    activeContentColor = colors.activeContentColor,
                    inactiveContainerColor = Color.Transparent,
                    inactiveContentColor = colors.inactiveContentColor,
                    disabledActiveContainerColor = colors.activeContainerColor.copy(alpha = 0.35f),
                    disabledActiveContentColor = colors.activeContentColor.copy(alpha = 0.55f),
                    disabledInactiveContainerColor = Color.Transparent,
                    disabledInactiveContentColor = colors.inactiveContentColor.copy(alpha = 0.45f),
                ),
                border = SegmentedButtonDefaults.borderStroke(color = MaterialTheme.colorScheme.outline),
                modifier = Modifier.weight(1f),
                icon = {
                    if (options.size <= 3) SegmentedButtonDefaults.Icon(active = selected)
                },
            ) {
                Text(
                    text = option.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = labelFontSize),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
internal fun <T> AppMaterial3TabRow(
    options: List<AppSegmentOption<T>>,
    selectedValue: T,
    enabled: Boolean,
    scrollable: Boolean,
    minTabWidth: Dp,
    modifier: Modifier,
    allowLabelOverflow: Boolean = false,
    indicatorPositionProvider: (() -> Float)? = null,
    onSelectionChange: (T) -> Unit,
) {
    val selectedIndex = resolveAppSegmentedSelectionIndex(options, selectedValue)
    val longestLabelLength = remember(options) {
        options.maxOfOrNull { it.label.length } ?: 0
    }
    val labelFontSize = remember(options.size, longestLabelLength) {
        resolveAppSegmentedLabelFontSizeSp(options.size, longestLabelLength).sp
    }
    val tabs: @Composable () -> Unit = {
        options.forEach { option ->
            val selected = option.value == selectedValue
            // Keep Tab's `text =` slot so TabRow can subtract HorizontalTextPadding
            // when sizing the underline. Overflow the 16.dp padding instead of
            // ellipsizing 直播间 / UP主 / 默认排序 when many tabs share one row.
            Tab(
                selected = selected,
                onClick = { onSelectionChange(option.value) },
                enabled = enabled,
                text = {
                    Text(
                        text = option.label,
                        modifier = Modifier.then(
                            if (allowLabelOverflow) {
                                Modifier.wrapContentWidth(
                                    align = Alignment.CenterHorizontally,
                                    unbounded = true,
                                )
                            } else {
                                Modifier
                            }
                        ),
                        maxLines = 1,
                        softWrap = false,
                        overflow = if (allowLabelOverflow) {
                            TextOverflow.Visible
                        } else {
                            TextOverflow.Clip
                        },
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = labelFontSize),
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    )
                },
            )
        }
    }
    if (scrollable) {
        AppPrimaryScrollableTabRow(
            selectedTabIndex = selectedIndex,
            modifier = modifier.fillMaxWidth(),
            containerColor = Color.Transparent,
            edgePadding = 0.dp,
            minTabWidth = minTabWidth,
            indicatorPositionProvider = indicatorPositionProvider,
            tabs = tabs,
        )
    } else {
        AppPrimaryTabRow(
            selectedTabIndex = selectedIndex,
            modifier = modifier.fillMaxWidth(),
            containerColor = Color.Transparent,
            indicatorPositionProvider = indicatorPositionProvider,
            tabs = tabs,
        )
    }
}
