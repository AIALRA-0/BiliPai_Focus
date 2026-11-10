package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.ui.AppContentColors

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
    iconTint: Color = AppContentColors.accent(),
    /** 可选的每项前导视觉，例如色彩风格的三色小样；各材质菜单共用前导内容槽。 */
    optionLeading: (@Composable (T) -> Unit)? = null,
    onSelectionChange: (T) -> Unit,
) {
    AppSingleChoicePreference(
        title = title.substringBefore("："),
        selectedValue = selectedValue,
        options = options.map { option ->
            AppChoiceOption(
                value = option.value,
                label = option.label,
                description = option.description,
            )
        },
        onValueChange = onSelectionChange,
        modifier = modifier,
        icon = icon,
        subtitle = subtitle,
        enabled = enabled,
        iconTint = iconTint,
        optionLeading = optionLeading,
    )
}
