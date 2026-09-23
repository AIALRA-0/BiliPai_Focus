package com.android.purebilibili.feature.plugin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.purebilibili.core.plugin.PluginManager
import com.android.purebilibili.core.plugin.formatClockHour
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.components.AppDropdownMenu
import com.android.purebilibili.core.ui.components.AppDropdownMenuItem
import com.android.purebilibili.core.ui.components.AppFilterChip
import com.android.purebilibili.core.ui.components.AppHorizontalDivider
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppOutlinedButton
import com.android.purebilibili.core.ui.components.AppSlider
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppSwitchPreference
import com.android.purebilibili.core.ui.components.AppText

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EyeProtectionSettings(
    plugin: EyeProtectionPlugin,
    modifier: Modifier = Modifier
) {
    var uiConfig by remember { mutableStateOf(plugin.snapshotConfig()) }
    val isActive by plugin.isNightModeActive.collectAsStateWithLifecycle()
    val usageMinutes by plugin.usageMinutesFlow.collectAsStateWithLifecycle()
    val pluginEnabled = PluginManager.plugins.any {
        it.plugin.id == EYE_PROTECTION_PLUGIN_ID && it.enabled
    }

    LaunchedEffect(Unit) {
        plugin.loadConfigSuspend()
        uiConfig = plugin.snapshotConfig()
    }
    DisposableEffect(Unit) {
        plugin.setSettingsPreviewEnabled(true)
        onDispose { plugin.setSettingsPreviewEnabled(false) }
    }

    fun updateConfig(newConfig: EyeProtectionConfig, refreshVisual: Boolean = true) {
        uiConfig = newConfig
        plugin.commitConfig(newConfig, refreshVisual)
    }

    val nextReminder = remember(
        usageMinutes,
        uiConfig.usageDurationMinutes,
        uiConfig.usageReminderEnabled,
        plugin.snoozeUntilMinute()
    ) {
        resolveMinutesUntilReminder(
            usageMinutes = usageMinutes,
            intervalMinutes = uiConfig.usageDurationMinutes,
            snoozeUntilMinute = plugin.snoozeUntilMinute(),
            reminderEnabled = uiConfig.usageReminderEnabled
        )
    }
    val status = remember(
        pluginEnabled,
        isActive,
        uiConfig,
        usageMinutes,
        nextReminder
    ) {
        resolveEyeProtectionStatusCopy(
            pluginEnabled = pluginEnabled,
            isActive = isActive,
            forceEnabled = uiConfig.forceEnabled,
            nightModeEnabled = uiConfig.nightModeEnabled,
            startHour = uiConfig.nightModeStartHour,
            endHour = uiConfig.nightModeEndHour,
            brightnessPercent = (uiConfig.brightnessLevel * 100).toInt(),
            warmPercent = (uiConfig.warmFilterStrength * 100).toInt(),
            usageMinutes = usageMinutes,
            reminderEnabled = uiConfig.usageReminderEnabled,
            nextReminderInMinutes = nextReminder
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        EyeProtectionStatusCard(status = status)

        Spacer(modifier = Modifier.height(8.dp))
        AppSwitchPreference(
            icon = Icons.Outlined.Lightbulb,
            title = "立即开启护眼",
            subtitle = "手动开启，不受时间段限制",
            checked = uiConfig.forceEnabled,
            onCheckedChange = { enabled ->
                updateConfig(uiConfig.copy(forceEnabled = enabled))
            },
            iconTint = MaterialTheme.colorScheme.tertiary
        )
        AppHorizontalDivider(
            modifier = Modifier.padding(start = 56.dp),
            color = AppSurfaceTokens.divider()
        )
        AppSwitchPreference(
            icon = Icons.Outlined.DarkMode,
            title = "定时护眼",
            subtitle = "${formatClockHour(uiConfig.nightModeStartHour)} - ${formatClockHour(uiConfig.nightModeEndHour)} 自动开启",
            checked = uiConfig.nightModeEnabled,
            onCheckedChange = { enabled ->
                updateConfig(uiConfig.copy(nightModeEnabled = enabled))
            }
        )
        if (uiConfig.nightModeEnabled) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 56.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimePickerDropdown(
                    modifier = Modifier.weight(1f),
                    selectedHour = uiConfig.nightModeStartHour,
                    onHourSelected = { hour ->
                        updateConfig(uiConfig.copy(nightModeStartHour = hour))
                    },
                    label = "开始"
                )
                TimePickerDropdown(
                    modifier = Modifier.weight(1f),
                    selectedHour = uiConfig.nightModeEndHour,
                    onHourSelected = { hour ->
                        updateConfig(uiConfig.copy(nightModeEndHour = hour))
                    },
                    label = "结束"
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            AppText(
                text = "进入时段渐变",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 56.dp)
            )
            AppText(
                text = "到点后慢慢变暖，结束前再慢慢退出，避免突然变色",
                style = MaterialTheme.typography.bodySmall,
                color = AppSurfaceTokens.onSurfaceVariantSummary(),
                modifier = Modifier.padding(start = 56.dp, top = 4.dp, bottom = 8.dp)
            )
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 56.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0 to "关闭", 15 to "15 分钟", 20 to "20 分钟", 30 to "30 分钟").forEach { (minutes, label) ->
                    AppFilterChip(
                        selected = uiConfig.scheduleRampMinutes == minutes,
                        onClick = { updateConfig(uiConfig.copy(scheduleRampMinutes = minutes)) },
                        modifier = Modifier.defaultMinSize(minWidth = 84.dp),
                        label = {
                            AppText(
                                text = label,
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                        }
                    )
                }
            }
        }

        AppHorizontalDivider(
            modifier = Modifier.padding(start = 56.dp, top = 12.dp),
            color = AppSurfaceTokens.divider()
        )
        AppSwitchPreference(
            icon = Icons.Outlined.Movie,
            title = "播放时减弱滤镜",
            subtitle = "看视频时降低暖色和降亮，减少画面偏色",
            checked = uiConfig.weakenDuringPlayback,
            onCheckedChange = { enabled ->
                updateConfig(uiConfig.copy(weakenDuringPlayback = enabled))
            }
        )
        AppSwitchPreference(
            icon = Icons.Outlined.Visibility,
            title = "生效时轻提示",
            subtitle = "滤镜真正开始工作时，顶部短暂提示一次",
            checked = uiConfig.showEffectHint,
            onCheckedChange = { enabled ->
                updateConfig(uiConfig.copy(showEffectHint = enabled), refreshVisual = false)
            }
        )

        AppHorizontalDivider(
            modifier = Modifier.padding(start = 56.dp),
            color = AppSurfaceTokens.divider()
        )
        AppSwitchPreference(
            icon = Icons.Outlined.Schedule,
            title = "关怀提醒",
            subtitle = "定时提醒休息、看远处、放松肩颈",
            checked = uiConfig.usageReminderEnabled,
            onCheckedChange = { enabled ->
                updateConfig(uiConfig.copy(usageReminderEnabled = enabled), refreshVisual = false)
            },
            iconTint = MaterialTheme.colorScheme.secondary
        )
        if (uiConfig.usageReminderEnabled) {
            AppSwitchPreference(
                icon = Icons.Outlined.NotificationsNone,
                title = "仅夜间提醒",
                subtitle = "白天减少打扰，护眼开启时再提醒",
                checked = uiConfig.remindOnlyDuringNight,
                onCheckedChange = { enabled ->
                    updateConfig(uiConfig.copy(remindOnlyDuringNight = enabled), refreshVisual = false)
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            ChipChoiceRow(
                title = "提醒频率",
                options = listOf(20, 30, 45, 60),
                selected = uiConfig.usageDurationMinutes,
                onSelect = { minutes ->
                    updateConfig(
                        plugin.persistSliderToPreset(uiConfig.copy(usageDurationMinutes = minutes)),
                        refreshVisual = false
                    )
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            ChipChoiceRow(
                title = "稍后提醒",
                options = listOf(5, 10, 15, 20),
                selected = uiConfig.reminderSnoozeMinutes,
                onSelect = { minutes ->
                    updateConfig(
                        plugin.persistSliderToPreset(uiConfig.copy(reminderSnoozeMinutes = minutes)),
                        refreshVisual = false
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        AppText(
            text = "关怀强度",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        AppText(
            text = presetDescription(uiConfig.carePreset),
            style = MaterialTheme.typography.bodySmall,
            color = AppSurfaceTokens.onSurfaceVariantSummary(),
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                EyeCarePreset.GENTLE to "轻柔",
                EyeCarePreset.BALANCED to "平衡",
                EyeCarePreset.FOCUS to "专注"
            ).forEach { (preset, label) ->
                AppFilterChip(
                    selected = uiConfig.carePreset == preset,
                    onClick = { updateConfig(plugin.applyPreset(uiConfig, preset)) },
                    label = {
                        AppText(text = label, maxLines = 1, overflow = TextOverflow.Clip)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        AppText(
            text = "显示调节",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        AppText(
            text = "当前页是实时预览。离开后按立即开启或定时规则生效。",
            style = MaterialTheme.typography.bodySmall,
            color = AppSurfaceTokens.onSurfaceVariantSummary(),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        SliderRow(
            icon = Icons.Filled.LightMode,
            title = "亮度",
            valueLabel = "${(uiConfig.brightnessLevel * 100).toInt()}%",
            value = uiConfig.brightnessLevel,
            valueRange = 0.3f..1.0f,
            onValueChange = { value ->
                val next = plugin.persistSliderToPreset(uiConfig.copy(brightnessLevel = value))
                uiConfig = next
                plugin.commitConfig(next, refreshVisual = false)
                plugin.previewBrightness(value)
            },
            onValueChangeFinished = { plugin.commitConfig(uiConfig) }
        )
        SliderRow(
            icon = Icons.Outlined.LightMode,
            title = "暖色",
            valueLabel = "${(uiConfig.warmFilterStrength * 100).toInt()}%",
            value = uiConfig.warmFilterStrength,
            valueRange = 0f..0.5f,
            onValueChange = { value ->
                val next = plugin.persistSliderToPreset(uiConfig.copy(warmFilterStrength = value))
                uiConfig = next
                plugin.commitConfig(next, refreshVisual = false)
                plugin.previewWarmFilter(value)
            },
            onValueChangeFinished = { plugin.commitConfig(uiConfig) }
        )

        AppText(
            text = "护眼滤镜不影响触摸。建议搭配系统深色模式一起用。",
            style = MaterialTheme.typography.bodySmall,
            color = AppSurfaceTokens.onSurfaceVariantSummary(),
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppIcon(
                imageVector = Icons.Outlined.FavoriteBorder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
            AppText(
                text = "照顾好自己，视频永远看得完。",
                style = MaterialTheme.typography.bodySmall,
                color = AppSurfaceTokens.onSurfaceVariantSummary()
            )
        }
    }
}

@Composable
private fun EyeProtectionStatusCard(status: EyeProtectionStatusCopy) {
    val container = if (status.isActive) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        AppSurfaceTokens.cardContainer()
    }
    val content = MaterialTheme.colorScheme.onSurface
    AppSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapes.container(ContainerLevel.Card),
        color = container
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppIcon(
                imageVector = if (status.isActive) Icons.Outlined.DarkMode else Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = if (status.isActive) content else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                AppText(
                    text = status.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = content
                )
                AppText(
                    text = status.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = content.copy(alpha = 0.82f)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipChoiceRow(
    title: String,
    options: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    AppText(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(start = 56.dp)
    )
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 56.dp, top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { minutes ->
            AppFilterChip(
                selected = selected == minutes,
                onClick = { onSelect(minutes) },
                modifier = Modifier.defaultMinSize(minWidth = 84.dp),
                label = {
                    AppText(
                        text = "${minutes}分钟",
                        softWrap = false,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            )
        }
    }
}

@Composable
private fun SliderRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.padding(horizontal = 6.dp))
            AppText(title, style = MaterialTheme.typography.bodyLarge)
        }
        AppText(
            text = valueLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = AppSurfaceTokens.onSurfaceVariantSummary()
        )
    }
    AppSlider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun TimePickerDropdown(
    modifier: Modifier = Modifier,
    selectedHour: Int,
    onHourSelected: (Int) -> Unit,
    label: String
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxWidth()) {
        AppOutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    AppText(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = AppSurfaceTokens.onSurfaceVariantSummary()
                    )
                    AppText(
                        text = formatClockHour(selectedHour),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                AppIcon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null
                )
            }
        }
        AppDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            (0..23).forEach { hour ->
                AppDropdownMenuItem(
                    text = { AppText(formatClockHour(hour)) },
                    trailingIcon = {
                        if (hour == selectedHour) {
                            AppText(
                                text = "当前",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    onClick = {
                        onHourSelected(hour)
                        expanded = false
                    }
                )
            }
        }
    }
}
