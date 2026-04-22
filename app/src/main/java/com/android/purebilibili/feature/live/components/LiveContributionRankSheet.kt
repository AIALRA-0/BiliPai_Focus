package com.android.purebilibili.feature.live.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "高能榜",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = roomTitle.ifBlank { anchorInfo.uname },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
            TabRow(selectedTabIndex = selectedTab) {
                rankTypes.forEachIndexed { index, type ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(type.title) }
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RankMetricRow("主播", anchorInfo.uname.ifBlank { "直播间" })
                        RankMetricRow("人气", formatLiveViewerCount(roomInfo.online))
                        RankMetricRow("观看", roomInfo.watchedText.ifBlank { "暂无数据" })
                        RankMetricRow("高能观众", roomInfo.onlineRankText.ifBlank { "暂无数据" })
                        RankMetricRow("开播时长", formatLiveDuration(roomInfo.liveStartTime).ifBlank { "刚刚开播" })
                    }
                    HorizontalDivider()
                    when {
                        isLoading -> {
                            Text(
                                text = "榜单加载中…",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        error != null -> {
                            Text(
                                text = error ?: "",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        items.isEmpty() -> {
                            Text(
                                text = "当前榜单暂无数据",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(items, key = { "${it.uid}_${it.rank}" }) { item ->
                                    RankItemRow(item = item)
                                }
                            }
                        }
                    }
                }
            }
            HorizontalDivider()
            Text(
                text = "榜单已接入真实数据，后续可继续补齐更完整的样式和用户跳转。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(bottom = 18.dp)
            )
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
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = item.rank.takeIf { it > 0 }?.toString() ?: "-",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Column {
                Text(
                    text = item.name.ifBlank { "用户" },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                val medal = item.medalInfo
                if (medal != null && medal.medalName.isNotBlank()) {
                    Text(
                        text = "${medal.medalName} Lv.${medal.level}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }
        Text(
            text = item.score.takeIf { it > 0 }?.toString() ?: "-",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp
        )
    }
}
