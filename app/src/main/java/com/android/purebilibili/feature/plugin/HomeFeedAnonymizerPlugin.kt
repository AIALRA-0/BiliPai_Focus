package com.android.purebilibili.feature.plugin

import android.content.ActivityNotFoundException
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.network.policy.HomeFeedAnonymizerRuntime
import com.android.purebilibili.core.network.policy.HomeFeedAnonymizerStatsSnapshot
import com.android.purebilibili.core.plugin.Plugin
import com.android.purebilibili.core.plugin.PluginCapability
import com.android.purebilibili.core.plugin.PluginCapabilityManifest
import com.android.purebilibili.core.util.Logger
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal const val HOME_FEED_ANONYMIZER_PLUGIN_ID = "home_feed_anonymizer"
private const val TAG = "HomeFeedAnonymizerPlugin"
private const val HOME_FEED_ANONYMIZER_WEB_ENDPOINT =
    "https://api.bilibili.com/x/web-interface/wbi/index/top/feed/rcmd"
private val HOME_FEED_ANONYMIZER_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

class HomeFeedAnonymizerPlugin : Plugin {
    override val id: String = HOME_FEED_ANONYMIZER_PLUGIN_ID
    override val name: String = "初见推荐"
    override val description: String = "仅在 Web 首页推荐接口隐藏登录 Cookie，让推荐流更接近未登录公共热门"
    override val version: String = "1.0.1"
    override val author: String = "BiliPai项目组"
    override val capabilityManifest: PluginCapabilityManifest = PluginCapabilityManifest(
        pluginId = id,
        displayName = name,
        version = version,
        apiVersion = 1,
        entryClassName = "com.android.purebilibili.feature.plugin.HomeFeedAnonymizerPlugin",
        capabilities = setOf(
            PluginCapability.RECOMMENDATION_CANDIDATES,
            PluginCapability.NETWORK
        )
    )

    override suspend fun onEnable() {
        HomeFeedAnonymizerRuntime.setEnabled(true)
        Logger.d(TAG, "初见推荐已启用：Web 首页推荐接口将不携带 Cookie")
    }

    override suspend fun onDisable() {
        HomeFeedAnonymizerRuntime.setEnabled(false)
        Logger.d(TAG, "初见推荐已禁用：Web 首页推荐接口恢复登录 Cookie")
    }

    @Composable
    override fun SettingsContent() {
        HomeFeedAnonymizerSettingsContent(enabled = HomeFeedAnonymizerRuntime.enabled)
    }
}

data class HomeFeedAnonymizerInfoRow(
    val label: String,
    val summary: String,
    val fullContent: String,
    val maxLines: Int,
    val url: String? = null
)

data class HomeFeedAnonymizerStatsUiModel(
    val stateRow: HomeFeedAnonymizerInfoRow,
    val totalRow: HomeFeedAnonymizerInfoRow,
    val lastHitRow: HomeFeedAnonymizerInfoRow,
    val scopeRow: HomeFeedAnonymizerInfoRow
) {
    val rows: List<HomeFeedAnonymizerInfoRow>
        get() = listOf(stateRow, totalRow, lastHitRow, scopeRow)
}

fun buildHomeFeedAnonymizerStatsUiModel(
    snapshot: HomeFeedAnonymizerStatsSnapshot,
    enabled: Boolean
): HomeFeedAnonymizerStatsUiModel {
    val stateSummary = if (enabled) "运行中" else "未启用"
    val lastHitSummary = snapshot.lastHitHost ?: "暂无命中"
    val lastHitAtMs = snapshot.lastHitAtMs
    val lastHitFullContent = if (lastHitAtMs == null) {
        "最近命中：暂无。启用后刷新首页推荐，命中 Web 首页推荐接口时会记录。"
    } else {
        buildString {
            appendLine("最近命中：${formatHomeFeedAnonymizerTime(lastHitAtMs)}")
            appendLine("Host：${snapshot.lastHitHost.orEmpty()}")
            append("Path：${snapshot.lastHitEncodedPath.orEmpty()}")
        }
    }

    return HomeFeedAnonymizerStatsUiModel(
        stateRow = HomeFeedAnonymizerInfoRow(
            label = "状态",
            summary = stateSummary,
            fullContent = "状态：$stateSummary。插件只影响 Web 首页推荐接口，不影响播放、评论、动态、收藏和移动端推荐流。",
            maxLines = 1
        ),
        totalRow = HomeFeedAnonymizerInfoRow(
            label = "匿名化请求",
            summary = "${snapshot.totalHits} 次",
            fullContent = "匿名化请求：${snapshot.totalHits} 次。本统计仅记录本机命中次数，不记录账号、Cookie、视频内容或推荐结果。",
            maxLines = 1
        ),
        lastHitRow = HomeFeedAnonymizerInfoRow(
            label = "最近命中",
            summary = lastHitSummary,
            fullContent = lastHitFullContent,
            maxLines = 1
        ),
        scopeRow = HomeFeedAnonymizerInfoRow(
            label = "影响范围",
            summary = "仅 Web 首页推荐接口",
            fullContent = "影响范围：仅 $HOME_FEED_ANONYMIZER_WEB_ENDPOINT。启用后该接口请求不携带 Cookie，其他接口保持原登录态。频繁刷新可能触发访客侧限流；首页异常时请先关闭插件验证。",
            maxLines = 2,
            url = HOME_FEED_ANONYMIZER_WEB_ENDPOINT
        )
    )
}

fun buildHomeFeedAnonymizerCreditRows(): List<HomeFeedAnonymizerInfoRow> {
    return listOf(
        HomeFeedAnonymizerInfoRow(
            label = "原作者",
            summary = "wangdaodao",
            fullContent = "原作者：wangdaodao。TabulaBili 原项目提出了清理 B 站首页推荐 Cookie 的思路。\nhttps://github.com/wangdaodaodao/TabulaBili",
            maxLines = 1,
            url = "https://github.com/wangdaodaodao/TabulaBili"
        ),
        HomeFeedAnonymizerInfoRow(
            label = "Plus 改版",
            summary = "tjsky",
            fullContent = "Plus 改版：tjsky。TabulaBili-Plus 提供了更完整的说明和扩展分发版本。\nhttps://github.com/tjsky/TabulaBili",
            maxLines = 1,
            url = "https://github.com/tjsky/TabulaBili"
        ),
        HomeFeedAnonymizerInfoRow(
            label = "介绍来源",
            summary = "Appinn 介绍帖",
            fullContent = "介绍来源：Appinn 小众软件论坛《TabulaBili-Plus：让 B 站个性化推荐算法“彻底失忆”的 Chrome 扩展》。\nhttps://meta.appinn.net/t/topic/85526",
            maxLines = 1,
            url = "https://meta.appinn.net/t/topic/85526"
        )
    )
}

private fun formatHomeFeedAnonymizerTime(epochMs: Long): String {
    return Instant.ofEpochMilli(epochMs)
        .atZone(ZoneId.systemDefault())
        .format(HOME_FEED_ANONYMIZER_TIME_FORMATTER)
}

@Composable
private fun HomeFeedAnonymizerSettingsContent(enabled: Boolean) {
    var refreshKey by remember { mutableIntStateOf(0) }
    var detailRow by remember { mutableStateOf<HomeFeedAnonymizerInfoRow?>(null) }
    val stats = remember(refreshKey, enabled) {
        buildHomeFeedAnonymizerStatsUiModel(
            snapshot = HomeFeedAnonymizerRuntime.statsSnapshot,
            enabled = enabled
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HomeFeedAnonymizerSection(title = "可视化统计") {
            stats.rows.forEach { row ->
                HomeFeedAnonymizerInfoRowView(
                    row = row,
                    onShowDetail = { detailRow = row }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { refreshKey += 1 }) {
                    Text("刷新统计")
                }
                TextButton(
                    onClick = {
                        HomeFeedAnonymizerRuntime.resetStats()
                        refreshKey += 1
                    }
                ) {
                    Text("重置统计")
                }
            }
        }

        HomeFeedAnonymizerSection(title = "致谢与来源") {
            buildHomeFeedAnonymizerCreditRows().forEach { row ->
                HomeFeedAnonymizerInfoRowView(
                    row = row,
                    onShowDetail = { detailRow = row }
                )
            }
            Text(
                text = "BiliPai 仅实现 Android 端内置插件形态，保留原项目与 Plus 改版来源说明。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    detailRow?.let { row ->
        HomeFeedAnonymizerDetailDialog(
            row = row,
            onDismiss = { detailRow = null }
        )
    }
}

@Composable
private fun HomeFeedAnonymizerSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f)
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeFeedAnonymizerInfoRowView(
    row: HomeFeedAnonymizerInfoRow,
    onShowDetail: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    row.url?.let { url ->
                        runCatching { uriHandler.openUri(url) }
                    }
                },
                onLongClick = onShowDetail
            )
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = row.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(86.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = row.summary,
            style = MaterialTheme.typography.bodySmall,
            color = if (row.url == null) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.primary
            },
            maxLines = row.maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HomeFeedAnonymizerDetailDialog(
    row: HomeFeedAnonymizerInfoRow,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(row.label) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "长按并拖拽选择需要复制的内容",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                SelectionContainer {
                    Text(
                        text = row.fullContent,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }
            }
        },
        confirmButton = {
            if (row.url != null) {
                TextButton(
                    onClick = {
                        try {
                            uriHandler.openUri(row.url)
                        } catch (_: ActivityNotFoundException) {
                        } catch (_: IllegalArgumentException) {
                        }
                    }
                ) {
                    Text("打开链接")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}
