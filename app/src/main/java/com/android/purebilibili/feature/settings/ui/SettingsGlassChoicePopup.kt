package com.android.purebilibili.feature.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.popup.AppGlassPopupHost
import com.android.purebilibili.feature.home.components.resolveLiquidGlassTuning
import com.android.purebilibili.feature.settings.rememberSettingsHomeConfiguration

/** 设置页保留独立底栏避让；材质与手势由公共同窗宿主提供。 */
@Composable
internal fun SettingsGlassChoicePopupHost(
    glassAvailable: Boolean,
    bottomInset: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    if (!glassAvailable) {
        content()
        return
    }
    val settings = rememberSettingsHomeConfiguration()
    val tuning = remember(settings.liquidGlassProgress, settings.liquidGlassAdvancedSettings, settings.liquidGlassReadabilityMode) {
        resolveLiquidGlassTuning(settings.liquidGlassProgress, settings.liquidGlassAdvancedSettings,
            settings.liquidGlassReadabilityMode)
    }
    AppGlassPopupHost(
        glassAvailable = true,
        tuning = tuning,
        bottomInset = bottomInset,
        content = content,
    )
}
