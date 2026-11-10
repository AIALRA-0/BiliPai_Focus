package com.android.purebilibili.feature.live

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.ui.AppIconStyle
import com.android.purebilibili.core.ui.AppThemeConfig
import com.android.purebilibili.core.ui.LocalAppIconStyle
import com.android.purebilibili.core.ui.LocalAppThemeConfig
import com.android.purebilibili.core.ui.ProvideAppContentColors

/** 直播间保留深色播放配色，菜单材质跟随用户液态玻璃开关。 */
internal fun liveRoomThemeConfig(appConfig: AppThemeConfig): AppThemeConfig = appConfig.copy(
    liquidControlsUseThemeColor = false,
    nativeMiuixPopupsEnabled = false,
    headerBlurEnabled = false,
    bottomBarBlurEnabled = false,
    progressiveTopBlurEnabled = false,
    progressiveTopFadeEnabled = false,
)

@Composable
internal fun LiveRoomTheme(content: @Composable () -> Unit) {
    val accent = MaterialTheme.colorScheme
    val roomColors = darkColorScheme(
        primary = accent.primary,
        onPrimary = accent.onPrimary,
        secondary = accent.secondary,
        tertiary = accent.tertiary,
    )
    CompositionLocalProvider(
        LocalAppThemeConfig provides liveRoomThemeConfig(LocalAppThemeConfig.current),
        LocalAppUiStyle provides AppUiStyle.MATERIAL3,
        LocalAppIconStyle provides AppIconStyle.MD3_STANDARD,
    ) {
        MaterialTheme(colorScheme = roomColors) { ProvideAppContentColors(content = content) }
    }
}
