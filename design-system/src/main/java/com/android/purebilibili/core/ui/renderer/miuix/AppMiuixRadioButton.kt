package com.android.purebilibili.core.ui.renderer.miuix

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.android.purebilibili.core.ui.components.appDesktopFocusableItemVisuals
import top.yukonga.miuix.kmp.basic.RadioButton

@Composable
internal fun AppMiuixRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier,
    enabled: Boolean,
) {
    ProvideAppMiuixHapticFeedback {
        RadioButton(
            selected = selected,
            onClick = onClick,
            modifier = modifier.appDesktopFocusableItemVisuals(enabled && onClick != null),
            enabled = enabled,
        )
    }
}
