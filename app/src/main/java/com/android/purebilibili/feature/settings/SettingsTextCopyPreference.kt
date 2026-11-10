package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.ui.AppContentColors

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.ui.components.AppSwitchPreference
import kotlinx.coroutines.launch

@Composable
internal fun SettingsTextCopyPreference() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val enabled by SettingsManager.getGlobalTextTapCopyEnabled(context)
        .collectAsStateWithLifecycle(initialValue = false)
    SettingsItemAnchor("animation.global_text_tap_copy_enabled") {
        AppSwitchPreference(
            icon = rememberSettingsSemanticIcon(SettingsIconRole.COPY_TEXT),
            title = settingItemTitle("animation.global_text_tap_copy_enabled"),
            subtitle = "点按正文文字即可复制",
            checked = enabled,
            onCheckedChange = { scope.launch { SettingsManager.setGlobalTextTapCopyEnabled(context, it) } },
            iconTint = AppContentColors.secondaryText(),
        )
    }
}
