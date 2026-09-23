// 文件路径: feature/settings/BottomBarSettingsScreen.kt
package com.android.purebilibili.feature.settings
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppHorizontalDivider

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress // [New]
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
import androidx.compose.ui.input.pointer.pointerInput // [New]
import androidx.compose.ui.zIndex // [New]
import androidx.compose.ui.draw.scale // [New]
import androidx.compose.animation.core.animateFloatAsState // [New]
import androidx.compose.animation.core.snap // [New]
import androidx.compose.animation.core.spring // [New]
import androidx.compose.ui.platform.LocalDensity // [New]
import androidx.compose.ui.geometry.Offset // [New]
import androidx.compose.ui.input.pointer.PointerInputChange // [New]
import com.android.purebilibili.core.util.rememberHapticFeedback // [New]
import com.android.purebilibili.core.util.HapticType // [New]
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.android.purebilibili.R
import com.android.purebilibili.core.store.HomeHeaderCollapseMode
import com.android.purebilibili.core.store.HomeTopLayoutOrder
import com.android.purebilibili.core.store.HomeTopRightAction
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.theme.BottomBarColors  //  统一底栏颜色配置
import com.android.purebilibili.core.theme.BottomBarColorPalette  //  调色板
import com.android.purebilibili.core.theme.BottomBarColorNames  //  颜色名称
import com.android.purebilibili.core.theme.LocalSettingsLiquidGlassEnabled
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.ui.AppSemanticIconFamily
import com.android.purebilibili.core.ui.rememberAppSemanticVisualPolicy
import com.android.purebilibili.core.ui.adaptive.resolveDeviceUiProfile
import com.android.purebilibili.core.ui.adaptive.resolveEffectiveMotionTier
import com.android.purebilibili.feature.settings.ui.SettingsPageScaffold
import com.android.purebilibili.feature.settings.ui.settingsScrollContentPadding
import com.android.purebilibili.core.util.LocalWindowSizeClass
import com.android.purebilibili.core.util.LocalAppWindowAdaptiveInfo
import kotlinx.coroutines.launch
import com.android.purebilibili.core.ui.components.*
import com.android.purebilibili.core.ui.animation.EntranceGroup
import com.android.purebilibili.core.ui.animation.entrance
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 *  底栏项目配置
 */
data class BottomBarTabConfig(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val isDefault: Boolean = true  // 是否为默认项（默认项不可删除）
)

data class TopTabConfig(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val fixedVisible: Boolean = false
)

@Composable
internal fun resolveBottomBarTabIcon(
    id: String,
    iconFamily: AppSemanticIconFamily = AppSemanticIconFamily.MATERIAL,
): ImageVector = resolveSettingsNavigationPreviewIcon(id, iconFamily, selected = false)

@Composable
internal fun resolveTopTabIcon(
    id: String,
    iconFamily: AppSemanticIconFamily = AppSemanticIconFamily.MATERIAL,
): ImageVector = resolveSettingsNavigationPreviewIcon(id, iconFamily, selected = false)

/**
 * 所有可用的底栏项目
 */
@Composable
internal fun resolveAllBottomBarTabs(
    iconFamily: AppSemanticIconFamily = AppSemanticIconFamily.MATERIAL,
): List<BottomBarTabConfig> = listOf(
    BottomBarTabConfig("HOME", "推荐", resolveBottomBarTabIcon("HOME", iconFamily), isDefault = true),
    BottomBarTabConfig("DYNAMIC", "动态", resolveBottomBarTabIcon("DYNAMIC", iconFamily), isDefault = true),
    BottomBarTabConfig("STORY", "短视频", resolveBottomBarTabIcon("STORY", iconFamily), isDefault = false),
    BottomBarTabConfig("HISTORY", "历史", resolveBottomBarTabIcon("HISTORY", iconFamily), isDefault = true),
    BottomBarTabConfig("LISTEN_VIDEO", "听视频", resolveBottomBarTabIcon("LISTEN_VIDEO", iconFamily), isDefault = true),
    BottomBarTabConfig("PROFILE", "我的", resolveBottomBarTabIcon("PROFILE", iconFamily), isDefault = true),
    BottomBarTabConfig("FAVORITE", "收藏", resolveBottomBarTabIcon("FAVORITE", iconFamily), isDefault = false),
    BottomBarTabConfig("LIVE", "直播", resolveBottomBarTabIcon("LIVE", iconFamily), isDefault = false),
    BottomBarTabConfig("WATCHLATER", "稍后看", resolveBottomBarTabIcon("WATCHLATER", iconFamily), isDefault = false),
    BottomBarTabConfig("SETTINGS", "设置", resolveBottomBarTabIcon("SETTINGS", iconFamily), isDefault = false),
    BottomBarTabConfig("PLUGINS", "插件中心", resolveBottomBarTabIcon("PLUGINS", iconFamily), isDefault = false)
)

private val defaultTopTabIds = listOf("RECOMMEND", "FOLLOW", "POPULAR", "LIVE", "GAME")

@Composable
internal fun resolveAllTopTabs(
    iconFamily: AppSemanticIconFamily = AppSemanticIconFamily.MATERIAL,
): List<TopTabConfig> = listOf(
    TopTabConfig("RECOMMEND", "推荐", resolveTopTabIcon("RECOMMEND", iconFamily)),
    TopTabConfig("FOLLOW", "关注", resolveTopTabIcon("FOLLOW", iconFamily)),
    TopTabConfig("POPULAR", "热门", resolveTopTabIcon("POPULAR", iconFamily)),
    TopTabConfig("LIVE", "直播", resolveTopTabIcon("LIVE", iconFamily)),
    TopTabConfig("ANIME", "追番", resolveTopTabIcon("ANIME", iconFamily)),
    TopTabConfig("GAME", "游戏", resolveTopTabIcon("GAME", iconFamily)),
    TopTabConfig("PARTITION", "分区", resolveTopTabIcon("PARTITION", iconFamily)),
    TopTabConfig("KNOWLEDGE", "知识", resolveTopTabIcon("KNOWLEDGE", iconFamily)),
    TopTabConfig("TECH", "科技", resolveTopTabIcon("TECH", iconFamily)),
    TopTabConfig("SUBSCRIPTIONS", "订阅", resolveTopTabIcon("SUBSCRIPTIONS", iconFamily))
)

/**
 *  导航设置页面
 * 支持底栏、顶部标签和平板侧边栏配置
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomBarSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val androidLiquidGlassEnabled by SettingsManager
        .getAndroidNativeLiquidGlassEnabled(context)
        .collectAsStateWithLifecycle(initialValue = false)
    val screenTitle = stringResource(R.string.bottom_bar_management_title)
    val backLabel = stringResource(R.string.common_back)
    val bottomContentPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    SettingsPageScaffold(
        title = screenTitle,
        onBack = onBack,
        backContentDescription = backLabel,
        bottomContentPadding = bottomContentPadding,
        scrollHost = SettingsPageScrollHost.External,
    ) {
        CompositionLocalProvider(LocalSettingsLiquidGlassEnabled provides androidLiquidGlassEnabled) {
            BottomBarSettingsContent()
        }
    }
}

@Composable
fun BottomBarSettingsContent(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val iconFamily = rememberAppSemanticVisualPolicy().effectiveIconFamily
    val windowSizeClass = LocalWindowSizeClass.current
    val displayContext = LocalAppWindowAdaptiveInfo.current.displayContext
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val focusRequest by SettingsSearchFocusController.request.collectAsStateWithLifecycle()
    val deviceUiProfile = remember(windowSizeClass.widthSizeClass) {
        resolveDeviceUiProfile(
            widthSizeClass = windowSizeClass.widthSizeClass
        )
    }
    LaunchedEffect(focusRequest?.token) {
        val request = focusRequest ?: return@LaunchedEffect
        if (request.target != SettingsSearchTarget.BOTTOM_BAR) return@LaunchedEffect
        val index = resolveBottomBarSettingsScrollIndex(request.focusId) ?: return@LaunchedEffect
        listState.animateScrollToItem(index)
        SettingsSearchFocusController.clear(request.token)
    }
    val allBottomBarTabs = resolveAllBottomBarTabs(iconFamily)
    val allTopTabs = resolveAllTopTabs(iconFamily)

    
    // 读取当前配置
    val order by SettingsManager.getBottomBarOrder(context).collectAsStateWithLifecycle(initialValue = listOf("HOME", "DYNAMIC", "HISTORY", "LISTEN_VIDEO", "PROFILE"))
    val visibleTabs by SettingsManager.getBottomBarVisibleTabs(context).collectAsStateWithLifecycle(initialValue = setOf("HOME", "DYNAMIC", "HISTORY", "LISTEN_VIDEO", "PROFILE"))
    val topTabOrder by SettingsManager.getTopTabOrder(context).collectAsStateWithLifecycle(initialValue = defaultTopTabIds)
    val topTabVisible by SettingsManager.getTopTabVisibleTabs(context).collectAsStateWithLifecycle(initialValue = defaultTopTabIds.toSet())
    val hideTopTabs by SettingsManager.getHideTopTabs(context)
        .collectAsStateWithLifecycle(initialValue = false)
    val topTabLabelMode by SettingsManager.getTopTabLabelMode(context)
        .collectAsStateWithLifecycle(initialValue = SettingsManager.TopTabLabelMode.TEXT_ONLY)
    val bottomBarLabelMode by SettingsManager.getBottomBarLabelMode(context)
        .collectAsStateWithLifecycle(initialValue = 0)
    val homeTopLayoutOrder by SettingsManager.getHomeTopLayoutOrder(context)
        .collectAsStateWithLifecycle(initialValue = HomeTopLayoutOrder.SEARCH_THEN_TABS)
    val homeHeaderCollapseMode by SettingsManager.getHomeHeaderCollapseMode(context)
        .collectAsStateWithLifecycle(initialValue = HomeHeaderCollapseMode.BOTH)

    val homeTopRightAction by SettingsManager.getHomeTopRightAction(context)
        .collectAsStateWithLifecycle(initialValue = HomeTopRightAction.SETTINGS)
    val isBottomBarFloating by SettingsManager.getBottomBarFloating(context)
        .collectAsStateWithLifecycle(initialValue = true)
    val navigationIconCrossScaleEnabled by SettingsManager
        .getNavigationIconCrossScaleEnabled(context)
        .collectAsStateWithLifecycle(initialValue = false)
    val bottomBarSearchEnabled by SettingsManager.getBottomBarSearchEnabled(context)
        .collectAsStateWithLifecycle(initialValue = false)
    val isLargeScreenCapable = windowSizeClass.isTabletDevice ||
        displayContext.isKnownFoldableDevice
    val tabletUseSidebar by SettingsManager.getTabletUseSidebar(context)
        .collectAsStateWithLifecycle(initialValue = isLargeScreenCapable)
    val sidebarAccountSwitcherEnabled by SettingsManager.getSidebarAccountSwitcherEnabled(context)
        .collectAsStateWithLifecycle(initialValue = true)
    
    // 可编辑的本地状态
    var localOrder by remember(order) { mutableStateOf(order) }
    var localVisibleTabs by remember(visibleTabs) { mutableStateOf(visibleTabs) }
    var localTopTabOrder by remember(topTabOrder) {
        mutableStateOf(
            (topTabOrder + allTopTabs.map { it.id })
                .distinct()
                .filter { id -> allTopTabs.any { it.id == id } }
        )
    }
    var localTopTabVisible by remember(topTabVisible, topTabOrder) {
        mutableStateOf(
            // 老配置可能超过上限：按用户顺序（含默认补全）裁剪到 SettingsManager.MAX_TOP_TABS
            (topTabOrder + allTopTabs.map { it.id })
                .distinct()
                .filter { id -> topTabVisible.any { it == id } && allTopTabs.any { it.id == id } }
                .take(SettingsManager.MAX_TOP_TABS)
                .toSet()
        )
    }
    
    // [新增] 监听顺序变化并保存
    fun onOrderChanged(fromIndex: Int, toIndex: Int) {
        val currentVisibleTabsList = localOrder.filter { it in localVisibleTabs }
        if (fromIndex in currentVisibleTabsList.indices && toIndex in currentVisibleTabsList.indices) {
            val fromId = currentVisibleTabsList[fromIndex]
            val toId = currentVisibleTabsList[toIndex]
            
            val globalFrom = localOrder.indexOf(fromId)
            val globalTo = localOrder.indexOf(toId)
            
            if (globalFrom != -1 && globalTo != -1) {
                // 交换位置
                val newOrder = localOrder.toMutableList()
                val item = newOrder.removeAt(globalFrom)
                newOrder.add(globalTo, item)
                localOrder = newOrder
            }
        }
    }
    
    //  [新增] 读取项目颜色配置
    val itemColors by SettingsManager.getBottomBarItemColors(context).collectAsStateWithLifecycle(initialValue = emptyMap())
    val itemLabels by SettingsManager.getBottomBarItemLabels(context)
        .collectAsStateWithLifecycle(initialValue = emptyMap())
    
    // 保存配置
    fun saveConfig() {
        scope.launch {
            SettingsManager.setBottomBarOrder(context, localOrder)
            SettingsManager.setBottomBarVisibleTabs(context, localVisibleTabs)
        }
    }

    fun saveTopTabConfig() {
        scope.launch {
            SettingsManager.setTopTabOrder(context, localTopTabOrder)
            SettingsManager.setTopTabVisibleTabs(context, localTopTabVisible)
        }
    }

    fun moveTopTab(tabId: String, direction: Int) {
        val visibleOrder = localTopTabOrder.filter { it in localTopTabVisible }
        val from = visibleOrder.indexOf(tabId)
        if (from < 0) return
        val to = (from + direction).coerceIn(0, visibleOrder.lastIndex)
        if (to == from) return

        val toId = visibleOrder[to]
        val globalFrom = localTopTabOrder.indexOf(tabId)
        val globalTo = localTopTabOrder.indexOf(toId)
        if (globalFrom < 0 || globalTo < 0) return

        val mutable = localTopTabOrder.toMutableList()
        val item = mutable.removeAt(globalFrom)
        mutable.add(globalTo, item)
        localTopTabOrder = mutable
        saveTopTabConfig()
    }
    
    //  [新增] 保存颜色配置
    fun saveItemColor(itemId: String, colorIndex: Int) {
        scope.launch {
            SettingsManager.setBottomBarItemColor(context, itemId, colorIndex)
        }
    }

    EntranceGroup {
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize(),
        contentPadding = settingsScrollContentPadding(extraHorizontal = 16.dp, extraVertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
            // 说明文字
            item {
                Box(modifier = Modifier.entrance()) {
                    AppText(
                        text = "集中管理底部导航、首页顶部标签和平板侧边栏。底栏项目最少 2 个，最多 5 个。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Box(modifier = Modifier.entrance()) {
                    AppPreferenceSectionTitle("导航行为")
                }
            }
            item {
                Box(modifier = Modifier.entrance()) {
                    AppPreferenceGroup {
                        AppSwitchPreference(
                            icon = rememberSettingsSemanticIcon(SettingsIconRole.FLOATING_BOTTOM_BAR),
                            title = "悬浮底栏",
                            subtitle = "开启后底栏与屏幕边缘留出间距；关闭后贴近底部显示",
                            checked = isBottomBarFloating,
                            onCheckedChange = { enabled ->
                                scope.launch { SettingsManager.setBottomBarFloating(context, enabled) }
                            },
                            iconTint = com.android.purebilibili.core.theme.iOSPurple,
                        )
                        AppPreferenceDivider()
                        AppSwitchPreference(
                            icon = rememberSettingsSemanticIcon(SettingsIconRole.NAV_ICON_CROSS_SCALE),
                            title = "导航图标交叉缩放",
                            subtitle = "指示器滑动途中图标短暂放大，停稳后恢复原始大小",
                            checked = navigationIconCrossScaleEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    SettingsManager.setNavigationIconCrossScaleEnabled(context, enabled)
                                }
                            },
                            iconTint = com.android.purebilibili.core.theme.iOSBlue,
                        )
                        AppPreferenceDivider()
                        AppSwitchPreference(
                            icon = rememberSettingsSemanticIcon(SettingsIconRole.BOTTOM_BAR_SEARCH),
                            title = "底栏搜索联动",
                            subtitle = "显示独立搜索胶囊，并与推荐和视频小横条随滚动自然收拢或展开",
                            checked = bottomBarSearchEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch { SettingsManager.setBottomBarSearchEnabled(context, enabled) }
                            },
                            iconTint = com.android.purebilibili.core.theme.iOSTeal,
                        )
                    }
                }
            }
            
            // 底部导航
            item {
                Box(modifier = Modifier.entrance()) {
                    AppPreferenceSectionTitle("底部导航")
                }
            }

            item {
                Box(modifier = Modifier.entrance()) {
                    AppPreferenceGroup {
                        val visibilityMode by SettingsManager.getBottomBarVisibilityMode(context).collectAsStateWithLifecycle(initialValue = SettingsManager.BottomBarVisibilityMode.ALWAYS_VISIBLE)
                        SettingsSingleChoicePreference(
                            icon = com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_visibility_24),
                            iconTint = com.android.purebilibili.core.theme.iOSOrange,
                            title = "显示模式",
                            subtitle = visibilityMode.description,
                            options = SettingsManager.BottomBarVisibilityMode.entries.map { mode ->
                                AppSegmentOption(mode, mode.label)
                            },
                            selectedValue = visibilityMode,
                            onSelectionChange = { mode ->
                                scope.launch { SettingsManager.setBottomBarVisibilityMode(context, mode) }
                            },
                        )
                        AppPreferenceDivider()
                        SettingsSingleChoicePreference(
                            icon = com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_label_24),
                            iconTint = com.android.purebilibili.core.theme.iOSPurple,
                            title = "标签样式",
                            options = listOf(
                                AppSegmentOption(0, "图标 + 文字"),
                                AppSegmentOption(1, "仅图标"),
                                AppSegmentOption(2, "仅文字"),
                            ),
                            selectedValue = bottomBarLabelMode,
                            onSelectionChange = { mode ->
                                scope.launch { SettingsManager.setBottomBarLabelMode(context, mode) }
                            },
                        )
                    }
                }
            }

            // 顶部标签
            item {
                Box(modifier = Modifier.entrance()) {
                    AppPreferenceSectionTitle("顶部标签")
                }
            }

            item {
                Box(modifier = Modifier.entrance()) {
                    AppPreferenceGroup {
                        AppSwitchPreference(
                            icon = com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_visibility_off_24),
                            iconTint = com.android.purebilibili.core.theme.iOSBlue,
                            title = "完全隐藏顶部标签",
                            subtitle = "开启后首页不显示顶部标签栏，提供纯净沉浸浏览",
                            checked = hideTopTabs,
                            onCheckedChange = { hide ->
                                scope.launch { SettingsManager.setHideTopTabs(context, hide) }
                            },
                        )
                        if (!hideTopTabs) {
                            AppPreferenceDivider()
                            SettingsSingleChoicePreference(
                                icon = com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_list_alt_24),
                                iconTint = com.android.purebilibili.core.theme.iOSBlue,
                                title = "顶部标签样式",
                                options = listOf(
                                    AppSegmentOption(SettingsManager.TopTabLabelMode.ICON_AND_TEXT, "图标 + 文字"),
                                    AppSegmentOption(SettingsManager.TopTabLabelMode.ICON_ONLY, "仅图标"),
                                    AppSegmentOption(SettingsManager.TopTabLabelMode.TEXT_ONLY, "仅文字"),
                                ),
                                selectedValue = topTabLabelMode,
                                onSelectionChange = { mode ->
                                    scope.launch { SettingsManager.setTopTabLabelMode(context, mode) }
                                },
                            )
                        }
                        AppPreferenceDivider()
                        SettingsSingleChoicePreference(
                            icon = if (homeTopRightAction == HomeTopRightAction.INBOX) {
                                com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_mail_24)
                            } else {
                                com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_settings_24)
                            },
                            iconTint = com.android.purebilibili.core.theme.iOSOrange,
                            title = "首页右上角入口",
                            options = HomeTopRightAction.entries.map { action ->
                                AppSegmentOption(action, action.label)
                            },
                            selectedValue = homeTopRightAction,
                            onSelectionChange = { action ->
                                scope.launch { SettingsManager.setHomeTopRightAction(context, action) }
                            },
                        )
                        if (!hideTopTabs) {
                            AppPreferenceDivider()
                            SettingsSingleChoicePreference(
                                icon = com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_reorder_24),
                                iconTint = com.android.purebilibili.core.theme.iOSPurple,
                                title = "首页顶部布局",
                                options = HomeTopLayoutOrder.entries.map { order ->
                                    AppSegmentOption(order, order.label)
                                },
                                selectedValue = homeTopLayoutOrder,
                                onSelectionChange = { order ->
                                    scope.launch { SettingsManager.setHomeTopLayoutOrder(context, order) }
                                },
                            )
                        }
                        AppPreferenceDivider()
                        SettingsSingleChoicePreference(
                                icon = com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_troubleshoot_24),
                                iconTint = com.android.purebilibili.core.theme.iOSTeal,
                                title = "全局顶栏显示",
                                subtitle = when (homeHeaderCollapseMode) {
                                    HomeHeaderCollapseMode.OFF -> "首页和二级列表的沉浸顶栏始终显示"
                                    HomeHeaderCollapseMode.SEARCH_ONLY -> "下滑时仅收起顶部搜索框，标签页留在顶部"
                                    HomeHeaderCollapseMode.BOTH -> "首页、历史、收藏和稍后再看等页面离开顶部后收起沉浸顶栏，回顶后恢复"
                                    HomeHeaderCollapseMode.TABS_ONLY -> "下滑时仅收起标签页，搜索框保持显示"
                                },
                                options = listOf(
                                    AppSegmentOption(HomeHeaderCollapseMode.OFF, "始终显示"),
                                    AppSegmentOption(HomeHeaderCollapseMode.SEARCH_ONLY, "仅收起搜索"),
                                    AppSegmentOption(HomeHeaderCollapseMode.BOTH, "全部收起"),
                                ),
                                selectedValue = if (homeHeaderCollapseMode == HomeHeaderCollapseMode.TABS_ONLY) {
                                    HomeHeaderCollapseMode.BOTH
                                } else {
                                    homeHeaderCollapseMode
                                },
                                onSelectionChange = { mode ->
                                    scope.launch { SettingsManager.setHomeHeaderCollapseMode(context, mode) }
                                },
                            )
                        if (!hideTopTabs) {
                            AppHorizontalDivider()
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                            AppText(
                                text = "可调整顶部标签的显示/隐藏和顺序，第一位会直接显示在首页顶部。最多显示 ${SettingsManager.MAX_TOP_TABS} 个标签。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val visibleTopOrder = localTopTabOrder.filter { it in localTopTabVisible }
                            AppText(
                                text = "已显示（上下按钮可排序）",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            visibleTopOrder.forEachIndexed { index, id ->
                                val tab = allTopTabs.firstOrNull { it.id == id } ?: return@forEachIndexed
                                val showIcon = topTabLabelMode != SettingsManager.TopTabLabelMode.TEXT_ONLY
                                val showText = topTabLabelMode != SettingsManager.TopTabLabelMode.ICON_ONLY
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(AppShapes.container(ContainerLevel.Card))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (showIcon) {
                                        AppIcon(
                                            imageVector = tab.icon,
                                            contentDescription = if (showText) null else tab.label,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    if (showIcon && showText) Spacer(modifier = Modifier.width(10.dp))
                                    if (showText) {
                                        AppText(
                                            text = tab.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                    if (tab.fixedVisible) {
                                        AppText(
                                            text = "固定",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    AppIconButton(
                                        onClick = { moveTopTab(tab.id, -1) },
                                        enabled = !tab.fixedVisible && index > 0
                                    ) {
                                        AppIcon(
                                            com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_keyboard_arrow_up_24),
                                            contentDescription = "上移",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    AppIconButton(
                                        onClick = { moveTopTab(tab.id, 1) },
                                        enabled = !tab.fixedVisible && index < visibleTopOrder.lastIndex
                                    ) {
                                        AppIcon(
                                            com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_keyboard_arrow_down_24),
                                            contentDescription = "下移",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            AppText(
                                text = "可用标签",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            allTopTabs.forEach { tab ->
                                val isVisibleTab = tab.id in localTopTabVisible
                                val canToggle = if (tab.fixedVisible) {
                                    false
                                } else if (isVisibleTab) {
                                    localTopTabVisible.size > 2
                                } else {
                                    localTopTabVisible.size < SettingsManager.MAX_TOP_TABS
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AppText(
                                        text = tab.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    AppAdaptiveSwitch(
                                        checked = isVisibleTab,
                                        onCheckedChange = { checked ->
                                            if (!canToggle) return@AppAdaptiveSwitch
                                            localTopTabVisible = if (checked) {
                                                localTopTabVisible + tab.id
                                            } else {
                                                localTopTabVisible - tab.id
                                            }
                                            if (checked && tab.id !in localTopTabOrder) {
                                                localTopTabOrder = localTopTabOrder + tab.id
                                            }
                                            saveTopTabConfig()
                                        },
                                        enabled = canToggle
                                    )
                                }
                            }
                        }
                        }
                    }
                }
            }

            // 平板导航
            item {
                Box(modifier = Modifier.entrance()) {
                    AppPreferenceSectionTitle("平板导航")
                }
            }

            item {
                Box(modifier = Modifier.entrance()) {
                    AppPreferenceGroup {
                        AppSwitchPreference(
                            icon = com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_view_sidebar_24),
                            title = "侧边导航栏",
                            subtitle = if (isLargeScreenCapable) {
                                "平板、大屏或展开后的折叠屏建议开启；仅在当前窗口足够宽时显示侧栏"
                            } else {
                                "在平板横屏或大屏布局中使用侧边栏代替底部导航"
                            },
                            checked = tabletUseSidebar,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    SettingsManager.setTabletUseSidebar(context, checked)
                                }
                            },
                            iconTint = com.android.purebilibili.core.theme.iOSBlue
                        )
                        AppSwitchPreference(
                            icon = com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_swap_horiz_24),
                            title = "侧边栏账号切换",
                            subtitle = "在平板首页侧边栏底部显示切换账号按钮",
                            checked = sidebarAccountSwitcherEnabled,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    SettingsManager.setSidebarAccountSwitcherEnabled(context, checked)
                                }
                            },
                            iconTint = com.android.purebilibili.core.theme.iOSBlue
                        )
                    }
                }
            }

            // 当前底栏预览
            item {
                Box(modifier = Modifier.entrance()) {
                    AppPreferenceSectionTitle("当前底栏")
                }
            }
            
            item {
                Box(modifier = Modifier.entrance()) {
                    BottomBarPreview(
                        tabs = localOrder.filter { it in localVisibleTabs }
                            .mapNotNull { id -> allBottomBarTabs.find { it.id == id } }
                            .map { tab -> tab.copy(label = itemLabels[tab.id] ?: tab.label) },
                        labelMode = bottomBarLabelMode,
                        onMove = { from, to -> onOrderChanged(from, to) },
                        onDragEnd = { saveConfig() }
                    )
                }
            }
            
            // 可用项目列表
            item {
                Box(modifier = Modifier.entrance()) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        AppPreferenceSectionTitle("可用项目")
                    }
                }
            }
            
            item {
                Box(modifier = Modifier.entrance()) {
                    AppPreferenceGroup {
                        allBottomBarTabs.forEachIndexed { index, tab ->
                            if (index > 0) {
                                AppHorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                            }
                            BottomBarTabItem(
                                tab = tab,
                                customLabel = itemLabels[tab.id].orEmpty(),
                                isVisible = tab.id in localVisibleTabs,
                                colorIndex = itemColors[tab.id] ?: BottomBarColors.getDefaultColorIndex(tab.id),
                                canToggle = if (tab.id in localVisibleTabs) {
                                    // 已显示的项目：至少保留 2 个可见
                                    localVisibleTabs.size > 2
                                } else {
                                    // 未显示的项目：最多显示 5 个
                                    localVisibleTabs.size < 5
                                },
                                onToggle = { visible ->
                                    localVisibleTabs = if (visible) {
                                        localVisibleTabs + tab.id
                                    } else {
                                        localVisibleTabs - tab.id
                                    }
                                    // 如果是新增项目，加到顺序末尾
                                    if (visible && tab.id !in localOrder) {
                                        localOrder = localOrder + tab.id
                                    }
                                    saveConfig()
                                },
                                onColorChange = { newColorIndex ->
                                    saveItemColor(tab.id, newColorIndex)
                                },
                                onLabelChange = { label ->
                                    scope.launch {
                                        SettingsManager.setBottomBarItemLabel(context, tab.id, label)
                                    }
                                }
                            )
                        }
                    }
                }
            }
            
            // 顺序调整说明
            item {
                Box(modifier = Modifier.entrance()) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        AppText(
                            text = " 长按图标并拖拽可调整显示顺序",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            // 重置按钮
            item {
                Box(modifier = Modifier.entrance()) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        AppOutlinedButton(
                            onClick = {
                                localOrder = listOf("HOME", "DYNAMIC", "HISTORY", "LISTEN_VIDEO", "PROFILE")
                                localVisibleTabs = setOf("HOME", "DYNAMIC", "HISTORY", "LISTEN_VIDEO", "PROFILE")
                                localTopTabOrder = defaultTopTabIds
                                localTopTabVisible = defaultTopTabIds.toSet()
                                saveConfig()
                                saveTopTabConfig()
                                scope.launch {
                                    // 重置为设备类型默认：平板开侧栏，手机开底栏
                                    SettingsManager.setTabletUseSidebar(context, isLargeScreenCapable)
                                    SettingsManager.clearBottomBarItemLabels(context)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            AppIcon(com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_refresh_24), contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            AppText("重置为默认")
                        }
                    }
                }
            }
        }
    }
    }


/**
 * 底栏预览组件（支持长按拖拽排序）
 */
@Composable
private fun BottomBarPreview(
    tabs: List<BottomBarTabConfig>,
    labelMode: Int,
    onMove: (Int, Int) -> Unit,
    onDragEnd: () -> Unit
) {
    // 触感反馈
    val haptic = rememberHapticFeedback()
    
    // 拖拽状态
    var draggingItemIndex by remember { mutableStateOf<Int?>(null) }
    var draggingItemCenter by remember { mutableFloatStateOf(0f) }
    
    AppSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.container(ContainerLevel.Dialog)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 2.dp
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp)
        ) {
            val totalWidth = maxWidth
            val itemWidth = totalWidth / tabs.size.coerceAtLeast(1)
            val density = LocalDensity.current
            
            // 全局手势检测区域
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .zIndex(20f) // 确保在最上层接收触摸事件
                    .pointerInput(tabs.size, itemWidth) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                val index = (offset.x / itemWidth.toPx()).toInt().coerceIn(0, tabs.lastIndex)
                                draggingItemIndex = index
                                draggingItemCenter = offset.x.coerceIn(0f, totalWidth.toPx())
                                haptic.invoke(HapticType.MEDIUM)
                            },
                            onDragEnd = {
                                draggingItemIndex = null
                                onDragEnd()
                            },
                            onDragCancel = {
                                draggingItemIndex = null
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                draggingItemCenter = (draggingItemCenter + dragAmount.x).coerceIn(0f, totalWidth.toPx())
                                
                                // 计算新索引
                                val newIndex = (draggingItemCenter / itemWidth.toPx()).toInt()
                                    .coerceIn(0, tabs.lastIndex)
                                
                                if (newIndex != draggingItemIndex) {
                                    if (draggingItemIndex != null) {
                                        onMove(draggingItemIndex!!, newIndex)
                                        draggingItemIndex = newIndex
                                        haptic.invoke(HapticType.LIGHT)
                                    }
                                }
                            }
                        )
                    }
            )

            tabs.forEachIndexed { index, tab ->
                key(tab.id) {
                    val isDragging = index == draggingItemIndex
                    val zIndex = if (isDragging) 10f else 0f
                    val scale by androidx.compose.animation.core.animateFloatAsState(if (isDragging) 1.2f else 1f, label = "scale")
                    
                    // 计算目标 X 位置
                    val targetX = if (isDragging) {
                         // 拖拽时：跟随手指中心
                         with(density) { draggingItemCenter.toDp() - (itemWidth / 2) }
                    } else {
                        // 静止时：网格位置
                        itemWidth * index
                    }
                    
                    val animatedX by androidx.compose.animation.core.animateDpAsState(
                        targetValue = targetX,
                        animationSpec = if (isDragging) androidx.compose.animation.core.snap() else androidx.compose.animation.core.spring(),
                        label = "offset"
                    )
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(itemWidth)
                            .offset { IntOffset(x = animatedX.roundToPx(), y = 0) }
                            .zIndex(zIndex)
                            .scale(scale)
                            // 移除单独的 pointerInput
                    ) {
                        val showIcon = labelMode != 2
                        val showText = labelMode != 1
                        if (showIcon) {
                            AppIcon(
                                imageVector = tab.icon,
                                contentDescription = if (showText) null else tab.label,
                                tint = if (index == 0 && !isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        if (showIcon && showText) Spacer(modifier = Modifier.height(2.dp))
                        if (showText) {
                            AppText(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (index == 0 && !isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 底栏项目单项
 */
@Composable
private fun BottomBarTabItem(
    tab: BottomBarTabConfig,
    customLabel: String,
    isVisible: Boolean,
    colorIndex: Int,
    canToggle: Boolean,
    onToggle: (Boolean) -> Unit,
    onColorChange: (Int) -> Unit,
    onLabelChange: (String) -> Unit,
) {
    //  MD3(MATERIAL3)主题下可用项目图标跟随主题色,不再使用多彩色板;
    //  颜色选择弹窗仅对保留多彩色的预设开放。
    val uiStyle = LocalAppUiStyle.current
    val isMaterial3 = uiStyle == AppUiStyle.MATERIAL3
    val itemColor = if (isMaterial3) {
        MaterialTheme.colorScheme.primary
    } else {
        BottomBarColors.getColorByIndex(colorIndex)
    }
    val itemContainerColor = if (isMaterial3) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        itemColor
    }
    val iconContentColor = rememberAdaptivePreferenceIconContentColor(itemContainerColor)
    
    //  颜色选择弹窗状态
    var showColorPicker by remember { mutableStateOf(false) }
    var showLabelEditor by remember { mutableStateOf(false) }
    var labelDraft by remember(customLabel) { mutableStateOf(customLabel) }
    val displayLabel = customLabel.ifBlank { tab.label }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标 -  点击可更换颜色(仅多彩色预设开放)
        Box(
            modifier = Modifier
                .size(48.dp)
                .appDesktopFocusableItemVisuals(enabled = !isMaterial3)
                .clickable(
                    enabled = !isMaterial3,
                    role = Role.Button,
                ) { showColorPicker = true },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(AppShapes.container(ContainerLevel.Field))
                    .background(itemContainerColor),
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(
                    imageVector = tab.icon,
                    contentDescription = null,
                    tint = iconContentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(14.dp))
        
        // 名称
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable {
                    labelDraft = customLabel
                    showLabelEditor = true
                }
                .padding(vertical = 4.dp)
        ) {
            AppText(
                text = displayLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            AppText(
                text = if (customLabel.isBlank()) {
                    "点击名称自定义文字；点击图标更换颜色"
                } else {
                    "默认：${tab.label}；点击名称修改"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // 开关
        AppAdaptiveSwitch(
            checked = isVisible,
            onCheckedChange = { newValue -> if (canToggle) onToggle(newValue) },
            enabled = canToggle
        )
    }

    if (showLabelEditor) {
        com.android.purebilibili.core.ui.AppAlertDialog(
            onDismissRequest = { showLabelEditor = false },
            title = { AppText("自定义${tab.label}文字") },
            text = {
                AppTextField(
                    value = labelDraft,
                    onValueChange = { labelDraft = it.take(12) },
                    modifier = Modifier.fillMaxWidth(),
                    label = "底栏文字",
                    placeholder = tab.label,
                    supportingText = {
                        AppText("最多 12 个字符；留空使用默认文字")
                    },
                )
            },
            confirmButton = {
                AppTextButton(
                    onClick = {
                        onLabelChange(labelDraft)
                        showLabelEditor = false
                    }
                ) {
                    AppText("保存")
                }
            },
            dismissButton = {
                AppTextButton(
                    onClick = {
                        onLabelChange("")
                        showLabelEditor = false
                    }
                ) {
                    AppText("恢复默认")
                }
            },
        )
    }
    
    //  颜色选择弹窗
    if (showColorPicker) {
        com.android.purebilibili.core.ui.AppAlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { AppText("选择${tab.label}颜色") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BottomBarColorPalette.forEachIndexed { index, color ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(AppShapes.container(ContainerLevel.Field))
                                .clickable {
                                    onColorChange(index)
                                    showColorPicker = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(AppShapes.container(ContainerLevel.Chip))
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            AppText(
                                text = BottomBarColorNames[index],
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (index == colorIndex) {
                                AppIcon(
                                    com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_check_24),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                com.android.purebilibili.core.ui.AppDialogAction(
                    onClick = { showColorPicker = false }
                ) {
                    AppText("取消", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}
