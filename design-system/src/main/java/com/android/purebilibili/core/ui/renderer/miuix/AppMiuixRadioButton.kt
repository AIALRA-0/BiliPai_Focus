package com.android.purebilibili.core.ui.renderer.miuix

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.android.purebilibili.core.ui.components.appDesktopFocusableItemVisuals
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.RadioButtonDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.android.purebilibili.core.theme.resolveControlGlyphColor

@Composable
internal fun AppMiuixRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier,
    enabled: Boolean,
) {
    val scheme = MiuixTheme.colorScheme
    val colors = if (com.android.purebilibili.core.ui.isMiuixNonGlassEnabled()) {
        RadioButtonDefaults.radioButtonColors()
    } else RadioButtonDefaults.radioButtonColors(
        selectedColor = resolveControlGlyphColor(scheme.primary, scheme.surface, scheme.surface),
    )
    ProvideAppMiuixHapticFeedback(scene = com.android.purebilibili.core.ui.AppHapticScene.MENUS) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            modifier = modifier.appDesktopFocusableItemVisuals(enabled && onClick != null),
            enabled = enabled,
            colors = colors,
        )
    }
}
