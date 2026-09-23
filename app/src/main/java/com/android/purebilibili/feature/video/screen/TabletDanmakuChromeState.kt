package com.android.purebilibili.feature.video.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.purebilibili.core.store.DanmakuSettings
import com.android.purebilibili.core.store.DanmakuSettingsScope
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.feature.video.danmaku.rememberDanmakuManager
import kotlinx.coroutines.launch

internal data class TabletDanmakuChromeState(
    val enabled: Boolean,
    val onToggle: () -> Unit,
)

@Composable
internal fun rememberTabletDanmakuChromeState(bvid: String): TabletDanmakuChromeState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val danmakuManager = rememberDanmakuManager(bvid)
    val danmakuSettings by SettingsManager
        .getDanmakuSettings(context, DanmakuSettingsScope.PORTRAIT)
        .collectAsStateWithLifecycle(initialValue = DanmakuSettings())
    val latestEnabled = rememberUpdatedState(danmakuSettings.enabled)
    val onToggle = remember(danmakuManager, context, scope) {
        {
            val newValue = !latestEnabled.value
            danmakuManager.isEnabled = newValue
            if (!newValue) {
                danmakuManager.clear()
            }
            scope.launch {
                SettingsManager.setDanmakuEnabled(
                    context,
                    newValue,
                    DanmakuSettingsScope.PORTRAIT,
                )
            }
            Unit
        }
    }
    return TabletDanmakuChromeState(
        enabled = danmakuSettings.enabled,
        onToggle = onToggle,
    )
}
