package com.android.purebilibili.core.ui.renderer.miuix

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import com.android.purebilibili.core.ui.components.AppCheckboxColors
import com.android.purebilibili.core.ui.components.appDesktopFocusableItemVisuals
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.CheckboxDefaults

@Composable
internal fun AppMiuixCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier,
    enabled: Boolean,
    colors: AppCheckboxColors?,
) {
    val nativeColors = colors?.let {
        CheckboxDefaults.checkboxColors(
            checkedBackgroundColor = it.checkedColor,
            uncheckedBackgroundColor = it.uncheckedColor,
        )
    } ?: CheckboxDefaults.checkboxColors()
    ProvideAppMiuixHapticFeedback {
        Checkbox(
            state = if (checked) ToggleableState.On else ToggleableState.Off,
            onClick = onCheckedChange?.let { callback -> { callback(!checked) } },
            modifier = modifier.appDesktopFocusableItemVisuals(enabled && onCheckedChange != null),
            colors = nativeColors,
            enabled = enabled,
        )
    }
}
