package com.android.purebilibili.feature.live.components
import com.android.purebilibili.core.ui.components.AppHorizontalDivider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.AppModalBottomSheet
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppSegmentOption
import com.android.purebilibili.core.ui.components.AppThemeAdaptiveTabRow
import com.android.purebilibili.core.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.feature.live.resolveLiveSheetVisualSpec
import com.android.purebilibili.feature.live.AnchorInfo
import com.android.purebilibili.feature.live.RoomInfo
import com.android.purebilibili.feature.live.formatLiveDuration
import com.android.purebilibili.feature.live.formatLiveViewerCount
import com.android.purebilibili.data.model.response.LiveContributionRankItem
import com.android.purebilibili.data.repository.LiveContributionRankType
import com.android.purebilibili.data.repository.LiveRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveContributionRankSheet(
    roomTitle: String,
    anchorInfo: AnchorInfo,
    roomInfo: RoomInfo,
    onDismiss: () -> Unit
) {
    val visualSpec = remember { resolveLiveSheetVisualSpec() }
    var selectedTab by remember { mutableIntStateOf(0) }
    var items by remember { mutableStateOf<List<LiveContributionRankItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val rankTypes = remember {
        listOf(
            LiveContributionRankType.ONLINE,
            LiveContributionRankType.DAILY,
            LiveContributionRankType.WEEKLY,
            LiveContributionRankType.MONTHLY
        )
    }

    LaunchedEffect(roomInfo.roomId, anchorInfo.uid, selectedTab) {
        isLoading = true
        error = null
        LiveRepository.getLiveContributionRank(
            roomId = roomInfo.roomId,
            ruid = anchorInfo.uid,
            type = rankTypes[selectedTab]
        ).onSuccess {
            items = it
            isLoading = false
        }.onFailure {
            error = it.message ?: "获取高能榜失败"
            isLoading = false
        }
    }

    AppModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AppSpacingTokens.ExtraLarge,
                    vertical = AppSpacingTokens.Small
                ),
            verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Large)
        ) {
            AppText(
                text = "高能榜",
                color = AppSurfaceTokens.onSurface(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            AppText(
                text = roomTitle.ifBlank { anchorInfo.uname },
                color = AppSurfaceTokens.onSurfaceVariantSummary(),
                style = MaterialTheme.typography.bodySmall
            )
            AppThemeAdaptiveTabRow(
                options = rankTypes.mapIndexed { index, type ->
                    AppSegmentOption(index, type.title)
                },
                selectedValue = selectedTab,
                onSelectionChange = { selectedTab = it },
                modifier = Modifier.fillMaxWidth(),
            )
            AppSurface(
                shape = AppShapes.container(ContainerLevel.Card),
                color = AppSurfaceTokens.cardContainer()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppSpacingTokens.Large),
                        verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Medium)
                    ) {
                        RankMetricRow("主播", anchorInfo.uname.ifBlank { "直播间" })
                        RankMetricRow("人气", formatLiveViewerCount(roomInfo.online))
                        RankMetricRow("观看", roomInfo.watchedText.ifBlank { "暂无数据" })
                        RankMetricRow("高能观众", roomInfo.onlineRankText.ifBlank { "暂无数据" })
                        RankMetricRow("开播时长", formatLiveDuration(roomInfo.liveStartTime).ifBlank { "刚刚开播" })
                    }
                    AppHorizontalDivider(color = AppSurfaceTokens.divider())
                    when {
                        isLoading -> {
                            AppText(
                                text = "榜单加载中…",
                                color = AppSurfaceTokens.onSurfaceVariantSummary(),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(AppSpacingTokens.Large)
                            )
                        }
                        error != null -> {
                            AppText(
                                text = error ?: "",
                                color = AppSurfaceTokens.onSurfaceVariantSummary(),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(AppSpacingTokens.Large)
                            )
                        }
                        items.isEmpty() -> {
                            AppText(
                                text = "当前榜单暂无数据",
                                color = AppSurfaceTokens.onSurfaceVariantSummary(),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(AppSpacingTokens.Large)
                            )
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = visualSpec.contributionListMaxHeightDp.dp),
                                verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small)
                            ) {
                                items(items, key = { "${it.uid}_${it.rank}" }) { item ->
                                    RankItemRow(item = item)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RankMetricRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        AppText(
            text = label,
            color = AppSurfaceTokens.onSurfaceVariantSummary(),
            style = MaterialTheme.typography.bodySmall
        )
        androidx.compose.foundation.layout.Spacer(Modifier.width(AppSpacingTokens.Small))
        AppText(
            text = value,
            color = AppSurfaceTokens.onSurface(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RankItemRow(
    item: LiveContributionRankItem
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = AppSpacingTokens.Large,
                vertical = AppSpacingTokens.Small
            ),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Medium)
        ) {
            AppText(
                text = item.rank.takeIf { it > 0 }?.toString() ?: "-",
                color = AppSurfaceTokens.primary(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Column(modifier = Modifier.weight(1f)) {
                AppText(
                    text = item.name.ifBlank { "用户" },
                    color = AppSurfaceTokens.onSurface(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val medal = item.medalInfo
                if (medal != null && medal.medalName.isNotBlank()) {
                    AppText(
                        text = "${medal.medalName} Lv.${medal.level}",
                        color = AppSurfaceTokens.onSurfaceVariantSummary(),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
        AppText(
            text = item.score.takeIf { it > 0 }?.toString() ?: "-",
            color = AppSurfaceTokens.onSurface(),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = AppSpacingTokens.Small)
        )
    }
}
