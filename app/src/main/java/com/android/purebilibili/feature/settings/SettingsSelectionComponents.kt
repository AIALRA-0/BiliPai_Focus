package com.android.purebilibili.feature.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.android.purebilibili.core.ui.components.AppChoiceOption
import com.android.purebilibili.core.ui.components.AppSegmentOption
import com.android.purebilibili.core.ui.components.AppSingleChoicePreference

@Composable
internal fun <T> SettingsSingleChoicePreference(
    title: String,
    options: List<AppSegmentOption<T>>,
    selectedValue: T,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    onSelectionChange: (T) -> Unit,
) {
    AppSingleChoicePreference(
        title = title,
        selectedValue = selectedValue,
        options = options.map { option ->
            AppChoiceOption(value = option.value, label = option.label)
        },
        onValueChange = onSelectionChange,
        modifier = modifier,
        icon = icon,
        subtitle = subtitle,
        enabled = enabled,
        iconTint = iconTint,
    )
}
