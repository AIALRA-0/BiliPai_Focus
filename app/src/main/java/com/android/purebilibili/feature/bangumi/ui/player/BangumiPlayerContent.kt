// 文件路径: feature/bangumi/ui/player/BangumiPlayerContent.kt
package com.android.purebilibili.feature.bangumi.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.theme.resolveAdaptivePrimaryAccentColors
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.data.model.response.BangumiDetail
import com.android.purebilibili.data.model.response.BangumiEpisode
import com.android.purebilibili.feature.bangumi.BANGUMI_FOLLOW_STATUS_OPTIONS
import com.android.purebilibili.feature.bangumi.BANGUMI_FOLLOW_STATUS_UNFOLLOW
import com.android.purebilibili.feature.bangumi.BANGUMI_FOLLOW_STATUS_WATCHING
import com.android.purebilibili.feature.bangumi.isBangumiFollowed
import com.android.purebilibili.feature.bangumi.resolveBangumiFollowStatusLabel

/**
 * 番剧播放内容区域
 */
@Composable
fun BangumiPlayerContent(
    detail: BangumiDetail,
    currentEpisode: BangumiEpisode,
    onEpisodeClick: (BangumiEpisode) -> Unit,
    onFollowStatusSelect: (Int) -> Unit
) {
    val isFollowing = isBangumiFollowed(detail.userStatus)
    var showFollowStatusDialog by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // 标题和信息
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = detail.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "正在播放：${currentEpisode.title} ${currentEpisode.longTitle}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                detail.stat?.let { stat ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${FormatUtils.formatStat(stat.views)}播放 · ${FormatUtils.formatStat(stat.danmakus)}弹幕",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        // 追番按钮
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Button(
                    onClick = {
                        if (isFollowing) {
                            showFollowStatusDialog = true
                        } else {
                            onFollowStatusSelect(BANGUMI_FOLLOW_STATUS_WATCHING)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = if (isFollowing) {
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    } else {
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    }
                ) {
                    Icon(
                        if (isFollowing) CupertinoIcons.Default.Checkmark else CupertinoIcons.Default.Plus,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(resolveBangumiFollowStatusLabel(detail.userStatus))
                }
            }
        }
        
        // 剧集选择
        if (!detail.episodes.isNullOrEmpty()) {
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
                
                // 选集标题和快速跳转
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "选集 (${detail.episodes.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    
                    // 当集数超过 50 时显示快速跳转
                    if (detail.episodes.size > 50) {
                        var showJumpDialog by remember { mutableStateOf(false) }
                        
                        Surface(
                            onClick = { showJumpDialog = true },
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "跳转",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // 快速跳转对话框
                        if (showJumpDialog) {
                            EpisodeJumpDialog(
                                totalEpisodes = detail.episodes.size,
                                onJump = { epNumber ->
                                    val targetEpisode = detail.episodes.getOrNull(epNumber - 1)
                                    if (targetEpisode != null) {
                                        onEpisodeClick(targetEpisode)
                                    }
                                    showJumpDialog = false
                                },
                                onDismiss = { showJumpDialog = false }
                            )
                        }
                    }
                }
            }
            
            // 对于超长剧集，添加范围选择器
            if (detail.episodes.size > 50) {
                item {
                    val episodesPerPage = 50
                    val totalPages = (detail.episodes.size + episodesPerPage - 1) / episodesPerPage
                    var selectedPage by remember { mutableIntStateOf(0) }
                    
                    // 当前集所在的页
                    val currentEpisodeIndex = detail.episodes.indexOfFirst { it.id == currentEpisode.id }
                    LaunchedEffect(currentEpisodeIndex) {
                        if (currentEpisodeIndex >= 0) {
                            selectedPage = currentEpisodeIndex / episodesPerPage
                        }
                    }
                    
                    // 范围选择器
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        items(totalPages) { page ->
                            val start = page * episodesPerPage + 1
                            val end = minOf((page + 1) * episodesPerPage, detail.episodes.size)
                            val isCurrentPage = page == selectedPage
                            
                            Surface(
                                onClick = { selectedPage = page },
                                color = if (isCurrentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = "$start-$end",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    fontSize = 12.sp,
                                    color = if (isCurrentPage) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    // 当前页的剧集
                    val pageStart = selectedPage * episodesPerPage
                    val pageEnd = minOf(pageStart + episodesPerPage, detail.episodes.size)
                    val pageEpisodes = detail.episodes.subList(pageStart, pageEnd)
                    
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(pageEpisodes) { episode ->
                            EpisodeChipSelectable(
                                episode = episode,
                                isSelected = episode.id == currentEpisode.id,
                                onClick = { onEpisodeClick(episode) }
                            )
                        }
                    }
                }
            } else {
                // 普通剧集列表
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(detail.episodes) { episode ->
                            EpisodeChipSelectable(
                                episode = episode,
                                isSelected = episode.id == currentEpisode.id,
                                onClick = { onEpisodeClick(episode) }
                            )
                        }
                    }
                }
            }
        }
        
        // 简介
        if (detail.evaluate.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "简介",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = detail.evaluate,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }
    }

    if (showFollowStatusDialog) {
        AlertDialog(
            onDismissRequest = { showFollowStatusDialog = false },
            title = { Text("追番状态") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BANGUMI_FOLLOW_STATUS_OPTIONS.forEach { option ->
                        Surface(
                            onClick = {
                                showFollowStatusDialog = false
                                onFollowStatusSelect(option.status)
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (detail.userStatus?.followStatus == option.status) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = option.label,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFollowStatusDialog = false
                        onFollowStatusSelect(BANGUMI_FOLLOW_STATUS_UNFOLLOW)
                    }
                ) {
                    Text("取消追番")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFollowStatusDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }
}

/**
 * 可选择的集数卡片
 */
@Composable
fun EpisodeChipSelectable(
    episode: BangumiEpisode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val selectedColors = resolveAdaptivePrimaryAccentColors(MaterialTheme.colorScheme)

    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) selectedColors.backgroundColor else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = episode.title.ifEmpty { "第${episode.id}话" },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            color = if (isSelected) selectedColors.contentColor else MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * 快速跳转集数对话框
 */
@Composable
fun EpisodeJumpDialog(
    totalEpisodes: Int,
    onJump: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("跳转到第几集") },
        text = {
            Column {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { 
                        inputText = it.filter { char -> char.isDigit() }
                        errorMessage = null
                    },
                    label = { Text("集数 (1-$totalEpisodes)") },
                    singleLine = true,
                    isError = errorMessage != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val epNumber = inputText.toIntOrNull()
                    if (epNumber == null || epNumber < 1 || epNumber > totalEpisodes) {
                        errorMessage = "请输入 1-$totalEpisodes 之间的数字"
                    } else {
                        onJump(epNumber)
                    }
                }
            ) {
                Text("跳转")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 错误内容显示
 */
@Composable
fun BangumiErrorContent(
    message: String,
    isVipRequired: Boolean,
    isLoginRequired: Boolean = false,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onLogin: () -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // 根据错误类型显示不同图标
            Text(
                text = when {
                    isVipRequired -> "👑"
                    isLoginRequired -> ""
                    else -> ""
                },
                fontSize = 48.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (isVipRequired) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "开通大会员即可观看",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            // 登录按钮
            if (isLoginRequired) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onLogin,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("去登录")
                }
            }
            if (canRetry) {
                Spacer(modifier = Modifier.height(if (isLoginRequired) 12.dp else 24.dp))
                if (isLoginRequired) {
                    TextButton(onClick = onRetry) { Text("重试") }
                } else {
                    Button(onClick = onRetry) { Text("重试") }
                }
            }
        }
    }
}
