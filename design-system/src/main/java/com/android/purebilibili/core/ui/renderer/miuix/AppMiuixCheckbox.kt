package com.android.purebilibili.core.ui.renderer.miuix

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import com.android.purebilibili.core.ui.components.AppCheckboxColors
import com.android.purebilibili.core.ui.components.appDesktopFocusableItemVisuals
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.CheckboxDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.android.purebilibili.core.theme.resolveControlGlyphColor

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
            // Explicit fills need their own paired checkmark; unedited native Miuix stays native.
            checkedForegroundColor = resolveControlGlyphColor(
                MiuixTheme.colorScheme.onPrimary, it.checkedColor, MiuixTheme.colorScheme.surface,
            ),
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
