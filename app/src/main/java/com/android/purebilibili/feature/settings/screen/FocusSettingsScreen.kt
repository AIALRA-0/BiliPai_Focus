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
        title = "Focus 专属",
        onBack = onBack,
        backContentDescription = androidx.compose.ui.res.stringResource(R.string.common_back),
        bottomContentPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        lazyListContent = {
            item {
                Text(
                    text = "Focus 专属功能包括关注分组过滤、相关推荐和历史清空入口。首页标签与搜索首页各区块都在现有设置中管理。新安装默认显示关注；订阅标签需启用订阅插件。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
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
                        icon = rememberSettingsSemanticIcon(SettingsIconRole.VERTICAL_RECOMMENDATIONS),
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
