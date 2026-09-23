package com.android.purebilibili.feature.settings.screen

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import com.android.purebilibili.core.ui.components.AppNavigationDrawerItem
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.rememberAdaptivePreferenceIconContentColor
import com.android.purebilibili.core.ui.components.rememberAdaptivePreferenceIconContainerColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.purebilibili.R
import com.android.purebilibili.core.ui.AppSplitLayout
import com.android.purebilibili.core.ui.AppSplitPane
import com.android.purebilibili.core.ui.rememberAppSplitLayoutState
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.AppTopBar
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.LocalAppThemeConfig
import com.android.purebilibili.core.ui.performance.isLowBlurBudgetForced
import com.android.purebilibili.core.ui.blur.BlurSurfaceType
import com.android.purebilibili.core.ui.blur.hazeSourceCompat
import com.android.purebilibili.core.ui.blur.recoverableBlurEnabled
import com.android.purebilibili.core.ui.blur.rememberRecoverableHazeState
import com.android.purebilibili.core.ui.blur.shouldAllowRenderEffectBackedHazeEffect
import com.android.purebilibili.core.ui.blur.unifiedBlur
import com.android.purebilibili.core.ui.rememberAppBackIcon
import com.android.purebilibili.feature.home.components.BiliPaiImmersiveTopBar
import com.android.purebilibili.feature.home.components.shouldUseBiliPaiProgressiveTopBlur
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import com.android.purebilibili.core.util.LocalWindowSizeClass
import com.android.purebilibili.feature.settings.SettingsHomeSearchEntry
import com.android.purebilibili.feature.settings.SettingsRootCategory
import com.android.purebilibili.feature.settings.canonicalSettingsRootCategory
import com.android.purebilibili.feature.settings.rememberSettingsEntryVisual
import com.android.purebilibili.feature.settings.resolveSettingsRootCategoryOrder
import com.android.purebilibili.feature.settings.resolveSettingsSiblingIconTints
import com.android.purebilibili.feature.settings.resolveSettingsTabletLayoutPolicy
import com.android.purebilibili.feature.settings.resolveSettingsVisualSpec
import com.android.purebilibili.feature.settings.shouldRenderSettingsTabletDetailPane

@Composable
fun SettingsTabletShell(
    selectedCategory: SettingsRootCategory?,
    onCategoryClick: (SettingsRootCategory) -> Unit,
    onBack: () -> Unit,
    onSearchOpen: () -> Unit,
    modifier: Modifier = Modifier,
    isSearchActive: Boolean = false,
    rightPane: @Composable () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val layoutPolicy = remember(configuration.screenWidthDp) {
        resolveSettingsTabletLayoutPolicy(widthDp = configuration.screenWidthDp)
    }
    val categories = remember { resolveSettingsRootCategoryOrder() }
    val splitLayoutState = rememberAppSplitLayoutState()
    val isDetailActive = shouldRenderSettingsTabletDetailPane(
        selectedCategory = selectedCategory,
        isSearchActive = isSearchActive,
    )
    LaunchedEffect(isDetailActive) {
        if (isDetailActive) {
            splitLayoutState.navigateTo(AppSplitPane.Secondary)
        }
    }
    val categoryIconTints = remember(categories.size) {
        resolveSettingsSiblingIconTints(categories.size)
    }
    val useThreePaneLayout =
        LocalWindowSizeClass.current.shouldUseThreePaneLayout && selectedCategory != null
    val emptyDetailPane: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppSurfaceTokens.groupedListContainer())
                .padding(layoutPolicy.detailPanePaddingDp.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppText(
                    text = "选择设置分类",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                AppText(
                    text = "从左侧选择一个分类以查看和修改设置",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    val detailPane: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppSurfaceTokens.groupedListContainer())
                .padding(horizontal = layoutPolicy.detailPanePaddingDp.dp),
        ) {
            rightPane()
        }
    }
    AppSplitLayout(
        modifier = modifier.fillMaxSize(),
        primaryRatio = layoutPolicy.primaryRatio,
        primaryContent = {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(AppSurfaceTokens.groupedListContainer()),
            ) {
                val config = LocalAppThemeConfig.current
                val progressive = shouldUseBiliPaiProgressiveTopBlur(
                    enabled = config.progressiveTopBlurEnabled && !config.headerBlurEnabled,
                    hasBackdrop = true,
                ) && !isLowBlurBudgetForced()
                val fadeActive = config.progressiveTopFadeEnabled && !config.headerBlurEnabled
                val tabletChromeBackdrop = if (progressive) rememberLayerBackdrop() else null
                val tabletHazeState = if (
                    config.headerBlurEnabled && !progressive &&
                        shouldAllowRenderEffectBackedHazeEffect(Build.VERSION.SDK_INT)
                ) rememberRecoverableHazeState() else null
                val tabletHazeReady = tabletHazeState?.let { recoverableBlurEnabled(it) } == true
                BiliPaiImmersiveTopBar(
                    backdrop = tabletChromeBackdrop,
                    enabled = progressive,
                    headerBlurActive = tabletHazeReady,
                    surfaceColor = AppSurfaceTokens.groupedListContainer(),
                    fadeEnabled = fadeActive,
                ) {
                    AppTopBar(
                        title = stringResource(R.string.settings_title),
                        modifier = if (tabletHazeReady) {
                            Modifier
                                .unifiedBlur(
                                    hazeState = requireNotNull(tabletHazeState),
                                    surfaceType = BlurSurfaceType.HEADER,
                                )
                                .background(
                                    AppSurfaceTokens.groupedListContainer()
                                        .copy(alpha = AppSurfaceTokens.FrostedScrimAlpha)
                                )
                        } else Modifier,
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = if (progressive || fadeActive || tabletHazeReady) {
                                androidx.compose.ui.graphics.Color.Transparent
                            } else {
                                AppSurfaceTokens.groupedListContainer()
                            },
                            scrolledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        ),
                        navigationIcon = {
                            AppIconButton(onClick = onBack) {
                                AppIcon(
                                    imageVector = rememberAppBackIcon(),
                                    contentDescription = stringResource(R.string.common_back),
                                )
                            }
                        },
                    )
                }
                Column(
                    modifier = Modifier
                    .weight(1f)
                    .then(
                            if (tabletChromeBackdrop != null) {
                                Modifier.layerBackdrop(tabletChromeBackdrop)
                            } else {
                                Modifier
                            }
                        )
                        .then(if (tabletHazeReady) Modifier.hazeSourceCompat(requireNotNull(tabletHazeState)) else Modifier)
                        .padding(
                            horizontal = layoutPolicy.masterPanePaddingDp.dp,
                            vertical = 8.dp,
                        ),
                ) {
                    SettingsHomeSearchEntry(onClick = onSearchOpen)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        itemsIndexed(categories) { index, category ->
                            val visual = rememberSettingsEntryVisual(category.searchTarget)
                            val effectiveIconTint = rememberAdaptivePreferenceIconContainerColor(
                                categoryIconTints[index]
                            )
                            val iconContentColor = rememberAdaptivePreferenceIconContentColor(effectiveIconTint)
                            val selected = selectedCategory?.let(::canonicalSettingsRootCategory) == category
                            AppNavigationDrawerItem(
                                label = {
                                    Column {
                                        AppText(category.title, fontWeight = FontWeight.Medium)
                                        AppText(
                                            text = category.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                        )
                                    }
                                },
                                selected = selected,
                                onClick = {
                                    if (!selected) {
                                        splitLayoutState.navigateTo(AppSplitPane.Secondary)
                                        onCategoryClick(category)
                                    }
                                },
                                icon = {
                                    Box(
                                        modifier = Modifier
                                            .size(resolveSettingsVisualSpec().categoryIconBubbleSize)
                                            .clip(AppShapes.container(ContainerLevel.Field))
                                            .background(effectiveIconTint),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (visual.icon != null) {
                                            AppIcon(
                                                imageVector = visual.icon,
                                                contentDescription = null,
                                                tint = iconContentColor,
                                                modifier = Modifier.size(visual.iconSizeDp.dp),
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        },
        secondaryContent = {
            if (useThreePaneLayout && !isSearchActive) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppSurfaceTokens.groupedListContainer())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AppText(
                        text = selectedCategory?.title ?: stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    AppText(
                        text = selectedCategory?.subtitle ?: "选择左侧分类以查看设置",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                if (!isDetailActive) emptyDetailPane() else detailPane()
            }
        },
        tertiaryContent = if (useThreePaneLayout && !isSearchActive) detailPane else null,
        state = splitLayoutState,
    )
}
