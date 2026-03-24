package com.android.purebilibili.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.store.FocusSettings
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.ui.components.IOSDivider
import com.android.purebilibili.core.ui.components.IOSGroup
import com.android.purebilibili.core.ui.components.IOSSectionTitle
import com.android.purebilibili.core.ui.components.IOSSwitchItem
import com.android.purebilibili.core.ui.rememberAppBackIcon
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by SettingsManager.getFocusSettings(context).collectAsState(initial = FocusSettings())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Focus",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        androidx.compose.material3.Icon(
                            imageVector = rememberAppBackIcon(),
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Focus 会默认收紧首页、搜索、历史与详情入口\n关注分组管理入口在动态页顶部",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }

            item {
                IOSSectionTitle("首页")
                IOSGroup {
                    IOSSwitchItem(
                        icon = Icons.Outlined.Home,
                        title = "显示推荐",
                        subtitle = "只控制首页顶部入口，不删除推荐流实现",
                        checked = settings.showHomeRecommendTab,
                        onCheckedChange = { enabled ->
                            scope.launch { SettingsManager.setFocusHomeRecommendTabVisible(context, enabled) }
                        }
                    )
                    IOSDivider(startIndent = 66.dp)
                    IOSSwitchItem(
                        icon = Icons.Outlined.Home,
                        title = "显示关注",
                        subtitle = "控制首页顶部关注标签显隐",
                        checked = settings.showHomeFollowTab,
                        onCheckedChange = { enabled ->
                            scope.launch { SettingsManager.setFocusHomeFollowTabVisible(context, enabled) }
                        }
                    )
                    IOSDivider(startIndent = 66.dp)
                    IOSSwitchItem(
                        icon = Icons.Outlined.Home,
                        title = "显示热门",
                        subtitle = "控制首页顶部热门标签显隐",
                        checked = settings.showHomePopularTab,
                        onCheckedChange = { enabled ->
                            scope.launch { SettingsManager.setFocusHomePopularTabVisible(context, enabled) }
                        }
                    )
                    IOSDivider(startIndent = 66.dp)
                    IOSSwitchItem(
                        icon = Icons.Outlined.Home,
                        title = "显示直播",
                        subtitle = "控制首页顶部直播标签显隐",
                        checked = settings.showHomeLiveTab,
                        onCheckedChange = { enabled ->
                            scope.launch { SettingsManager.setFocusHomeLiveTabVisible(context, enabled) }
                        }
                    )
                    IOSDivider(startIndent = 66.dp)
                    IOSSwitchItem(
                        icon = Icons.Outlined.Home,
                        title = "显示游戏",
                        subtitle = "控制首页顶部游戏标签显隐",
                        checked = settings.showHomeGameTab,
                        onCheckedChange = { enabled ->
                            scope.launch { SettingsManager.setFocusHomeGameTabVisible(context, enabled) }
                        }
                    )
                    IOSDivider(startIndent = 66.dp)
                    IOSSwitchItem(
                        icon = Icons.Outlined.Tune,
                        title = "显示分区按钮",
                        subtitle = "控制首页顶部右侧分区入口",
                        checked = settings.showHomePartitionButton,
                        onCheckedChange = { enabled ->
                            scope.launch { SettingsManager.setFocusHomePartitionButtonVisible(context, enabled) }
                        }
                    )
                }
            }

            item {
                IOSSectionTitle("关注")
                IOSGroup {
                    IOSSwitchItem(
                        icon = Icons.Outlined.Tune,
                        title = "启用关注过滤",
                        subtitle = "关闭后保留分组和归属，但动态与首页关注不再按分组隐藏内容",
                        checked = settings.enableFollowGroupFiltering,
                        onCheckedChange = { enabled ->
                            scope.launch { SettingsManager.setFocusFollowGroupFilteringEnabled(context, enabled) }
                        }
                    )
                }
            }

            item {
                IOSSectionTitle("视频")
                IOSGroup {
                    IOSSwitchItem(
                        icon = Icons.Outlined.Tune,
                        title = "显示相关推荐",
                        subtitle = "控制视频详情页中的相关推荐与更多推荐区块",
                        checked = settings.showVideoRelatedVideosSection,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                SettingsManager.setFocusVideoRelatedVideosSectionVisible(context, enabled)
                            }
                        }
                    )
                }
            }

            item {
                IOSSectionTitle("搜索")
                IOSGroup {
                    IOSSwitchItem(
                        icon = Icons.Outlined.Search,
                        title = "显示热门搜索",
                        subtitle = "保留搜索联想、搜索发现、搜索结果和历史记录",
                        checked = settings.showSearchHotSection,
                        onCheckedChange = { enabled ->
                            scope.launch { SettingsManager.setSearchHotSectionEnabled(context, enabled) }
                        }
                    )
                }
            }

            item {
                IOSSectionTitle("历史记录")
                IOSGroup {
                    IOSSwitchItem(
                        icon = Icons.Outlined.DeleteOutline,
                        title = "显示一键清空",
                        subtitle = "在观看历史页顶部显示清空全部入口",
                        checked = settings.showHistoryClearAllAction,
                        onCheckedChange = { enabled ->
                            scope.launch { SettingsManager.setFocusHistoryClearAllActionEnabled(context, enabled) }
                        }
                    )
                }
            }
        }
    }
}

