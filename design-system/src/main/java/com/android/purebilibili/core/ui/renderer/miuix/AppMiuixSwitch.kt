package com.android.purebilibili.core.ui.renderer.miuix

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.ui.LocalAppThemeConfig
import com.android.purebilibili.core.ui.LiquidSwitchThumbMode
import com.android.purebilibili.core.ui.components.appDesktopFocusableItemVisuals
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.SwitchDefaults

@Composable
internal fun AppMiuixSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier,
    enabled: Boolean,
) {
    val forcedThumb = when (LocalAppThemeConfig.current.liquidSwitchThumbMode) {
        LiquidSwitchThumbMode.THEME -> null
        LiquidSwitchThumbMode.DARK -> Color(0xFF202124)
        LiquidSwitchThumbMode.LIGHT -> Color.White
    }
    ProvideAppMiuixHapticFeedback {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = if (forcedThumb == null) SwitchDefaults.switchColors() else
                SwitchDefaults.switchColors(
                    checkedThumbColor = forcedThumb,
                    uncheckedThumbColor = forcedThumb,
                    disabledCheckedThumbColor = forcedThumb.copy(alpha = 0.38f),
                    disabledUncheckedThumbColor = forcedThumb.copy(alpha = 0.38f),
                ),
            modifier = modifier.appDesktopFocusableItemVisuals(enabled && onCheckedChange != null),
        )
    }
}
