package com.android.purebilibili.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.purebilibili.R
import com.android.purebilibili.core.store.FocusSettings
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.ui.components.AppPreferenceDivider
import com.android.purebilibili.core.ui.components.AppPreferenceGroup
import com.android.purebilibili.core.ui.components.AppPreferenceSectionTitle
import com.android.purebilibili.core.ui.components.AppSwitchPreference
import com.android.purebilibili.feature.settings.ui.SettingsPageScaffold
import kotlinx.coroutines.launch

@Composable
fun FocusSettingsScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by SettingsManager
        .getFocusSettings(context)
        .collectAsStateWithLifecycle(initialValue = FocusSettings())

    SettingsPageScaffold(
        title = "Focus",
        onBack = onBack,
        backContentDescription = androidx.compose.ui.res.stringResource(R.string.common_back),
        bottomContentPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        lazyListContent = {
            item {
                Text(
                    text = "Focus 会默认收紧首页、搜索、历史与详情入口\n关注分组管理入口在动态页顶部",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }

            item {
                FocusSettingsSection(title = "首页") {
                    AppSwitchPreference(
                        icon = rememberSettingsSemanticIcon(SettingsIconRole.HOME_FEED),
                        title = "显示推荐",
                        subtitle = "只控制首页顶部入口，不删除推荐流实现",
                        checked = settings.showHomeRecommendTab,
                        onCheckedChange = { enabled ->
                            scope.launch { SettingsManager.setFocusHomeRecommendTabVisible(context, enabled) }
                        },
                    )
                    AppPreferenceDivider()
                    AppSwitchPreference(
                        icon = rememberSettingsSemanticIcon(SettingsIconRole.FOLLOW_BUTTON),
                        title = "显示关注",
                        subtitle = "控制首页顶部关注标签显隐",
                        checked = settings.showHomeFollowTab,
                        onCheckedChange = { enabled ->
                            scope.launch { SettingsManager.setFocusHomeFollowTabVisible(context, enabled) }
                        },
                    )
                    AppPreferenceDivider()
                    AppSwitchPreference(
                        icon = rememberSettingsSemanticIcon(SettingsIconRole.ANALYTICS),
                        title = "显示热门",
                        subtitle = "控制首页顶部热门标签显隐",
                        checked = settings.showHomePopularTab,
                        onCheckedChange = { enabled ->
                            scope.launch { SettingsManager.setFocusHomePopularTabVisible(context, enabled) }
                        },
                    )
                    AppPreferenceDivider()
                    AppSwitchPreference(
                        icon = rememberSettingsSemanticIcon(SettingsIconRole.LIVE_SURFACE_TRANSITION),
                        title = "显示直播",
                        subtitle = "控制首页顶部直播标签显隐",
                        checked = settings.showHomeLiveTab,
                        onCheckedChange = { enabled ->
                            scope.launch { SettingsManager.setFocusHomeLiveTabVisible(context, enabled) }
                        },
                    )
                    AppPreferenceDivider()
                    AppSwitchPreference(
                        icon = rememberSettingsSemanticIcon(SettingsIconRole.PGC_TIMELINE),
                        title = "显示番剧",
                        subtitle = "控制首页顶部番剧标签显隐",
                        checked = settings.showHomeAnimeTab,
                        onCheckedChange = { enabled ->
                            scope.launch { SettingsManager.setFocusHomeAnimeTabVisible(context, enabled) }
                        },
                    )
                    AppPreferenceDivider()
                    AppSwitchPreference(
                        icon = rememberSettingsSemanticIcon(SettingsIconRole.EASTER_EGG),
                        title = "显示游戏",
                        subtitle = "控制首页顶部游戏标签显隐",
                        checked = settings.showHomeGameTab,
                        onCheckedChange = { enabled ->
                            scope.launch { SettingsManager.setFocusHomeGameTabVisible(context, enabled) }
                        },
                    )
                    AppPreferenceDivider()
                    AppSwitchPreference(
                        icon = rememberSettingsSemanticIcon(SettingsIconRole.TIPS),
                        title = "显示知识",
                        subtitle = "控制首页顶部知识标签显隐",
                        checked = settings.showHomeKnowledgeTab,
                        onCheckedChange = { enabled ->
                            scope.launch { SettingsManager.setFocusHomeKnowledgeTabVisible(context, enabled) }
                        },
                    )
                    AppPreferenceDivider()
                    AppSwitchPreference(
                        icon = rememberSettingsSemanticIcon(SettingsIconRole.DIAGNOSTICS),
                        title = "显示科技",
                        subtitle = "控制首页顶部科技标签显隐",
                        checked = settings.showHomeTechTab,
                        onCheckedChange = { enabled ->
                            scope.launch { SettingsManager.setFocusHomeTechTabVisible(context, enabled) }
                        },
                    )
                    AppPreferenceDivider()
                    AppSwitchPreference(
                        icon = rememberSettingsSemanticIcon(SettingsIconRole.GRID_COLUMNS),
                        title = "显示分区按钮",
                        subtitle = "控制首页顶部右侧分区入口",
                        checked = settings.showHomePartitionButton,
                        onCheckedChange = { enabled ->
                            scope.launch { SettingsManager.setFocusHomePartitionButtonVisible(context, enabled) }
                        },
                    )
                }
            }

            item {
                FocusSettingsSection(title = "关注") {
                    AppSwitchPreference(
                        icon = rememberSettingsSemanticIcon(SettingsIconRole.FOLLOW_BUTTON),
                        title = "启用关注过滤",
                        subtitle = "关闭后保留分组和归属，但动态与首页关注不再按分组隐藏内容",
                        checked = settings.enableFollowGroupFiltering,
                        onCheckedChange = { enabled ->
                            scope.launch { SettingsManager.setFocusFollowGroupFilteringEnabled(context, enabled) }
                        },
                    )
                }
            }

            item {
                FocusSettingsSection(title = "视频") {
                    AppSwitchPreference(
                        icon = rememberSettingsSemanticIcon(SettingsIconRole.RELATED_VIDEO_TRANSITION),
                        title = "显示相关推荐",
                        subtitle = "控制视频详情页中的相关推荐与更多推荐区块",
                        checked = settings.showVideoRelatedVideosSection,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                SettingsManager.setFocusVideoRelatedVideosSectionVisible(context, enabled)
                            }
                        },
                    )
                }
            }

            item {
                FocusSettingsSection(title = "搜索") {
                    AppSwitchPreference(
                        icon = rememberSettingsSemanticIcon(SettingsIconRole.TIPS),
                        title = "显示大家都在搜",
                        subtitle = "在搜索首页显示热搜关键词区块",
                        checked = settings.showSearchHotSection,
                        onCheckedChange = { enabled ->
                            scope.launch { SettingsManager.setSearchHotSectionEnabled(context, enabled) }
                        },
                    )
                    AppPreferenceDivider()
                    AppSwitchPreference(
                        icon = rememberSettingsSemanticIcon(SettingsIconRole.OPEN_SOURCE_HOME),
                        title = "显示搜索发现",
                        subtitle = "在搜索首页显示搜索发现区块",
                        checked = settings.showSearchDiscoverSection,
                        onCheckedChange = { enabled ->
                            scope.launch { SettingsManager.setSearchDiscoverSectionEnabled(context, enabled) }
                        },
                    )
                    AppPreferenceDivider()
                    AppSwitchPreference(
                        icon = rememberSettingsSemanticIcon(SettingsIconRole.PRIVACY_HISTORY),
                        title = "显示搜索历史",
                        subtitle = "在搜索首页显示搜索历史列表",
                        checked = settings.showSearchHistorySection,
                        onCheckedChange = { enabled ->
                            scope.launch { SettingsManager.setSearchHistorySectionEnabled(context, enabled) }
                        },
                    )
                }
            }

            item {
                FocusSettingsSection(title = "历史记录") {
                    AppSwitchPreference(
                        icon = rememberSettingsSemanticIcon(SettingsIconRole.CLEAR_CACHE),
                        title = "显示一键清空",
                        subtitle = "在观看历史页顶部显示清空全部入口",
                        checked = settings.showHistoryClearAllAction,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                SettingsManager.setFocusHistoryClearAllActionEnabled(context, enabled)
                            }
                        },
                    )
                }
            }
        },
    )
}

@Composable
private fun FocusSettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppPreferenceSectionTitle(title)
        AppPreferenceGroup(content = content)
    }
}
