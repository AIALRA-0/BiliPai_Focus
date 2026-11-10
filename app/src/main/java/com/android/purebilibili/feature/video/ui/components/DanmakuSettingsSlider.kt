package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppContentColors
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppSlider
import com.android.purebilibili.core.ui.components.AppText

/** 参数标题、当前值与恢复操作同排；拖动预览与松手保存各自独立。 */
@Composable
internal fun DanmakuSettingsSlider(
    label: String,
    valueText: (Float) -> String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    defaultValue: Float?,
    onValuePreview: (Float) -> Unit,
    onValueChangeFinished: (Float) -> Unit,
    modifier: Modifier = Modifier,
    commitOnDispose: Boolean = false,
) {
    var localValue by remember(valueRange) { mutableFloatStateOf(value.coerceIn(valueRange)) }
    var previewing by remember { mutableStateOf(false) }
    val latestCommit by rememberUpdatedState(onValueChangeFinished)
    LaunchedEffect(value, valueRange) {
        if (!previewing) localValue = value.coerceIn(valueRange)
    }
    DisposableEffect(commitOnDispose) {
        onDispose { if (commitOnDispose && previewing) latestCommit(localValue) }
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.ExtraSmall)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppText(
                text = label,
                modifier = Modifier.weight(1f).padding(end = AppSpacingTokens.Small),
                color = AppContentColors.secondaryText(),
                style = MaterialTheme.typography.bodyMedium
            )
            AppText(
                text = valueText(localValue),
                color = AppContentColors.secondaryText(),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
            )
            if (defaultValue != null) AppIconButton(
                enabled = kotlin.math.abs(localValue - defaultValue) > 0.0001f,
                onClick = {
                    previewing = false
                    localValue = defaultValue.coerceIn(valueRange)
                    onValuePreview(defaultValue)
                    onValueChangeFinished(defaultValue)
                },
                modifier = Modifier.size(36.dp).semantics {
                    contentDescription = "恢复${label}默认值 ${valueText(defaultValue)}"
                },
            ) {
                AppIcon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
        AppSlider(
            value = localValue,
            onValueChange = {
                previewing = true
                localValue = it
                onValuePreview(it)
            },
            onValueChangeFinished = { previewing = false; onValueChangeFinished(localValue) },
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = label
                    stateDescription = valueText(localValue)
                }
        )
    }
}

