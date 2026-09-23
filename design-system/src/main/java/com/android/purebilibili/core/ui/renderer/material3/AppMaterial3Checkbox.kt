package com.android.purebilibili.core.ui.renderer.material3

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.android.purebilibili.core.ui.components.AppCheckboxColors
import com.android.purebilibili.core.ui.components.appDesktopInteractionVisuals

@Composable
internal fun AppMaterial3Checkbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier,
    enabled: Boolean,
    colors: AppCheckboxColors?,
    interactionSource: MutableInteractionSource?,
) {
    val resolvedInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val nativeColors = colors?.let {
        CheckboxDefaults.colors(
            checkedColor = it.checkedColor,
            uncheckedColor = it.uncheckedColor,
        )
    } ?: CheckboxDefaults.colors()
    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.appDesktopInteractionVisuals(
            resolvedInteractionSource,
            enabled && onCheckedChange != null,
        ),
        enabled = enabled,
        colors = nativeColors,
        interactionSource = resolvedInteractionSource,
    )
}
