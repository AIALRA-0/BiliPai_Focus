package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.ui.renderer.material3.AppMaterial3Checkbox
import com.android.purebilibili.core.ui.renderer.miuix.AppMiuixCheckbox

@Immutable
data class AppCheckboxColors(
    val checkedColor: Color,
    val uncheckedColor: Color,
)

object AppCheckboxDefaults {
    fun colors(
        checkedColor: Color,
        uncheckedColor: Color,
    ): AppCheckboxColors = AppCheckboxColors(
        checkedColor = checkedColor,
        uncheckedColor = uncheckedColor,
    )
}

@Composable
fun AppCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: AppCheckboxColors? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    when (LocalAppUiStyle.current) {
        AppUiStyle.MATERIAL3 -> AppMaterial3Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
            colors = colors,
            interactionSource = interactionSource,
        )
        AppUiStyle.MIUIX -> AppMiuixCheckbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
            colors = colors,
        )
    }
}
