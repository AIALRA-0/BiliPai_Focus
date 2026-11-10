package com.android.purebilibili.feature.settings.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import com.android.purebilibili.core.util.AppFoldPosture
import com.android.purebilibili.core.util.LocalAppWindowAdaptiveInfo
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import com.android.purebilibili.feature.settings.isSettingsSearchNavKey
import com.android.purebilibili.feature.settings.isSettingsSubtreeNavKey
import com.android.purebilibili.feature.settings.resolveSettingsCategoryNavKey
import com.android.purebilibili.feature.settings.resolveSettingsTabletShellCategory
import com.android.purebilibili.feature.settings.shouldUseSettingsSplitLayout
import com.android.purebilibili.feature.settings.ui.SettingsOpaqueSurfaceHost
import com.android.purebilibili.navigation3.BiliPaiNavKey

/** 与设置脚手架使用相同的窗口和铰链条件，避免单栏退化时误禁用页面导航。 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun settingsHasPersistentPanes(): Boolean {
    val directive = calculatePaneScaffoldDirective(currentWindowAdaptiveInfoV2())
    return shouldUseSettingsSplitLayout(LocalConfiguration.current.screenWidthDp) &&
        directive.maxHorizontalPartitions > 1 &&
        LocalAppWindowAdaptiveInfo.current.posture != AppFoldPosture.Tabletop
}

@Composable
internal fun SettingsTabletNavEntryShell(
    key: BiliPaiNavKey,
    onSystemBack: () -> Unit,
    onPushKey: (BiliPaiNavKey) -> Unit,
    content: @Composable () -> Unit,
) {
    SettingsOpaqueSurfaceHost {
        SettingsTabletRouteShell(
            key = key,
            onBack = onSystemBack,
            onCategoryClick = { category -> onPushKey(resolveSettingsCategoryNavKey(category)) },
            onSearchOpen = { onPushKey(BiliPaiNavKey.SettingsSearch) },
            phoneContent = content,
        )
    }
}

@Composable
internal fun SettingsTabletRouteShell(
    key: BiliPaiNavKey,
    onBack: () -> Unit,
    onCategoryClick: (com.android.purebilibili.feature.settings.SettingsRootCategory) -> Unit,
    onSearchOpen: () -> Unit,
    phoneContent: @Composable () -> Unit,
) {
    val configuration = LocalConfiguration.current
    if (shouldUseSettingsSplitLayout(widthDp = configuration.screenWidthDp) && isSettingsSubtreeNavKey(key)) {
        SettingsTabletShell(
            selectedCategory = resolveSettingsTabletShellCategory(key),
            onCategoryClick = onCategoryClick,
            onBack = onBack,
            onSearchOpen = onSearchOpen,
            isSearchActive = isSettingsSearchNavKey(key),
            rightPane = {
                Box(modifier = Modifier.fillMaxSize()) {
                    phoneContent()
                }
            },
        )
    } else {
        phoneContent()
    }
}
