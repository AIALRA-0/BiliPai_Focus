package com.android.purebilibili.feature.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.theme.LocalUiPreset
import com.android.purebilibili.core.theme.UiPreset
import com.android.purebilibili.core.ui.animation.horizontalDragGesture
import com.android.purebilibili.core.ui.animation.rememberDampedDragAnimationState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

@Composable
internal fun <T> IOSSlidingSegmentedSetting(
    title: String,
    options: List<PlaybackSegmentOption<T>>,
    selectedValue: T,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    onSelectionChange: (T) -> Unit
) {
    val uiPreset = LocalUiPreset.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = if (uiPreset == UiPreset.MD3) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.bodyLarge
            },
            color = if (uiPreset == UiPreset.MD3) {
                MiuixTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (uiPreset == UiPreset.MD3) {
                    MiuixTheme.colorScheme.onSurfaceVariantSummary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        IOSSlidingSegmentedControl(
            options = options,
            selectedValue = selectedValue,
            enabled = enabled,
            onSelectionChange = onSelectionChange
        )
    }
}

@Composable
internal fun <T> IOSSlidingSegmentedControl(
    options: List<PlaybackSegmentOption<T>>,
    selectedValue: T,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onSelectionChange: (T) -> Unit
) {
    if (options.isEmpty()) return
    val uiPreset = LocalUiPreset.current
    if (uiPreset == UiPreset.MD3) {
        Md3SegmentedControl(
            options = options,
            selectedValue = selectedValue,
            modifier = modifier,
            enabled = enabled,
            onSelectionChange = onSelectionChange
        )
        return
    }
    IOSSlidingSegmentedControlImpl(
        options = options,
        selectedValue = selectedValue,
        modifier = modifier,
        enabled = enabled,
        onSelectionChange = onSelectionChange
    )
}

@Composable
private fun <T> Md3SegmentedControl(
    options: List<PlaybackSegmentOption<T>>,
    selectedValue: T,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onSelectionChange: (T) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp)
    ) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option.value == selectedValue,
                    onClick = { onSelectionChange(option.value) },
                    enabled = enabled,
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = options.size
                    ),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MiuixTheme.colorScheme.secondaryContainer,
                        activeContentColor = MiuixTheme.colorScheme.onSecondaryContainer,
                        inactiveContainerColor = Color.Transparent,
                        inactiveContentColor = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        disabledActiveContainerColor = MiuixTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                        disabledActiveContentColor = MiuixTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.55f),
                        disabledInactiveContainerColor = Color.Transparent,
                        disabledInactiveContentColor = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = option.label,
                        maxLines = 1,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> IOSSlidingSegmentedControlImpl(
    options: List<PlaybackSegmentOption<T>>,
    selectedValue: T,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onSelectionChange: (T) -> Unit
) {
    val selectedIndex = resolveSelectionIndex(options = options, selectedValue = selectedValue)
    val dragState = rememberDampedDragAnimationState(
        initialIndex = selectedIndex,
        itemCount = options.size,
        onIndexChanged = { index ->
            options.getOrNull(index)?.let { option ->
                onSelectionChange(option.value)
            }
        }
    )
    val containerShape = RoundedCornerShape(20.dp)
    val indicatorShape = RoundedCornerShape(16.dp)
    val density = LocalDensity.current

    LaunchedEffect(selectedIndex) {
        dragState.updateIndex(selectedIndex)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(containerShape)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 0.42f else 0.28f)
            )
            .border(
                width = 0.8.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.10f else 0.05f),
                shape = containerShape
            )
            .padding(3.dp)
    ) {
        val segmentWidth = maxWidth / options.size
        val dragModifier = if (enabled && options.size > 1) {
            Modifier.horizontalDragGesture(
                dragState = dragState,
                itemWidthPx = with(density) { segmentWidth.toPx() }
            )
        } else {
            Modifier
        }
        val indicatorOffsetX = segmentWidth * dragState.value

        Box(
            modifier = Modifier
                .offset(x = indicatorOffsetX)
                .fillMaxWidth(1f / options.size)
                .height(40.dp)
                .clip(indicatorShape)
                .background(
                    if (enabled) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    }
                )
                .border(
                    width = 0.6.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.12f else 0.06f),
                    shape = indicatorShape
                )
        )

        Row(modifier = Modifier.fillMaxWidth().then(dragModifier)) {
            val activeIndex = dragState.value.roundToInt().coerceIn(0, options.lastIndex)
            options.forEachIndexed { index, option ->
                val isSelected = index == activeIndex
                val labelColor by animateColorAsState(
                    targetValue = when {
                        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = tween(durationMillis = 180),
                    label = "segmentedLabelColor"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(indicatorShape)
                        .clickable(
                            enabled = enabled,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onSelectionChange(option.value)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option.label,
                        color = labelColor,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
