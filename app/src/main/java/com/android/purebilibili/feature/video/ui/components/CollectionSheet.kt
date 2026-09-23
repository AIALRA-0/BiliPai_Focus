// 文件路径: feature/video/ui/components/CollectionSheet.kt
package com.android.purebilibili.feature.video.ui.components

import coil3.request.crossfade
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppHorizontalDivider

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.ui.rememberAppClearIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.data.model.response.UgcEpisode
import com.android.purebilibili.data.model.response.UgcSeason
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.feature.home.components.cards.VideoCardCoverDurationText

/**
 *  视频合集底部弹窗
 * 显示合集中的所有视频列表
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionSheet(
    ugcSeason: UgcSeason,
    currentBvid: String,
    currentCid: Long = 0L,
    onDismiss: () -> Unit,
    onEpisodeClick: (UgcEpisode) -> Unit
) {
    val context = LocalContext.current
    val clearIcon = rememberAppClearIcon()
    val scope = rememberCoroutineScope()
    val allEpisodes = remember(ugcSeason.sections) { ugcSeason.sections.flatMap { it.episodes } }
    val collectionSubscriptionId = remember(ugcSeason) { resolveCollectionSubscriptionId(ugcSeason) }
    val currentAid = remember(allEpisodes, currentBvid, currentCid) {
        resolveCurrentUgcEpisodeAid(
            episodes = allEpisodes,
            currentBvid = currentBvid,
            currentCid = currentCid
        )
    }
    val storedSortMode by SettingsManager
        .getCollectionSortMode(context, collectionSubscriptionId)
        .collectAsStateWithLifecycle(initialValue = CollectionSortMode.ASCENDING)
    var localSortMode by remember(collectionSubscriptionId) {
        mutableStateOf<CollectionSortMode?>(null)
    }
    LaunchedEffect(storedSortMode) {
        if (localSortMode == storedSortMode) {
            localSortMode = null
        }
    }
    val sortMode = localSortMode ?: storedSortMode
    val sortedEpisodes = remember(allEpisodes, sortMode, currentBvid, currentCid) {
        sortCollectionEpisodes(
            episodes = allEpisodes,
            sortMode = sortMode,
            currentBvid = currentBvid,
            currentCid = currentCid
        )
    }
    val currentIndex = resolveCurrentUgcEpisodeIndex(
        episodes = sortedEpisodes,
        currentBvid = currentBvid,
        currentCid = currentCid
    )
    
    com.android.purebilibili.core.ui.AppModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        windowInsets = WindowInsets(0.dp)  //  沉浸式
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)  //  确保整个区域有背景色
        ) {
            //  标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    AppText(
                        text = ugcSeason.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    AppText(
                        text = "共 ${allEpisodes.size} 个视频",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                CollectionSubscriptionButton(
                    collectionId = collectionSubscriptionId,
                    currentBvid = currentBvid,
                    currentAid = currentAid,
                    fontSize = MaterialTheme.typography.labelMedium.fontSize
                )

                AppIconButton(onClick = onDismiss) {
                    AppIcon(
                        clearIcon,
                        contentDescription = "关闭",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            AppHorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            val sortModes = remember { CollectionSortMode.entries.toList() }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AppText(
                    text = "排序",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CommentSegmentedControl(
                    items = sortModes.map(::resolveCollectionSortLabel),
                    selectedIndex = sortModes.indexOf(sortMode).coerceAtLeast(0),
                    onScaleChange = { index ->
                        sortModes.getOrNull(index)?.let { nextMode ->
                            localSortMode = nextMode
                            scope.launch {
                                SettingsManager.setCollectionSortMode(context, collectionSubscriptionId, nextMode)
                            }
                        }
                    }
                )
            }

            AppHorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            
            //  视频列表
            val listState = androidx.compose.foundation.lazy.rememberLazyListState()
            
            // 自动滚动到当前播放的视频
            LaunchedEffect(currentIndex) {
                if (currentIndex != -1) {
                    // 稍微延迟一下以确保布局完成
                    kotlinx.coroutines.delay(100)
                    listState.scrollToItem(currentIndex)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(max = 400.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                itemsIndexed(sortedEpisodes, key = { _, episode -> episode.id }) { index, episode ->
                    val isCurrentEpisode = isCurrentUgcEpisode(
                        currentBvid = currentBvid,
                        currentCid = currentCid,
                        episode = episode
                    )
                    val publishTimeText = remember(episode.arc?.pubdate, episode.arc?.ctime) {
                        resolveCollectionEpisodePublishTimeText(episode)
                    }
                    val metadataText = remember(publishTimeText, isCurrentEpisode) {
                        buildList {
                            if (publishTimeText.isNotBlank()) add(publishTimeText)
                            if (isCurrentEpisode) add("正在播放")
                        }.joinToString(" · ")
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                if (!isCurrentEpisode) {
                                    onEpisodeClick(episode)
                                }
                            }
                            .background(
                                if (isCurrentEpisode) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else Color.Transparent
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        //  视频封面缩略图
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .aspectRatio(16f / 9f)
                                .clip(AppShapes.container(ContainerLevel.Chip))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            // 封面图
                            episode.arc?.pic?.let { pic ->
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(FormatUtils.fixImageUrl(pic))
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = episode.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                            
                            episode.arc?.duration?.let { duration ->
                                if (duration > 0) {
                                    VideoCardCoverDurationText(
                                        text = formatDuration(duration),
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(4.dp),
                                    )
                                }
                            }
                            
                            // 正在播放覆盖层
                            if (isCurrentEpisode) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AppIcon(
                                        Icons.Outlined.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        //  视频信息
                        Column(modifier = Modifier.weight(1f)) {
                            AppText(
                                text = episode.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isCurrentEpisode) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isCurrentEpisode) FontWeight.SemiBold 
                                            else FontWeight.Normal,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            if (metadataText.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                AppText(
                                    text = metadataText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isCurrentEpisode) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
            
            //  内容底部间距 + 导航栏区域填充（合并为一个 Spacer，用 surface 色填充）
            val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp + navBarHeight)  // 16dp 内容间距 + 导航栏高度
                    .background(MaterialTheme.colorScheme.surface)
            )
        }
    }
}

/**
 * 格式化时长
 */
private fun formatDuration(seconds: Int): String {
    return FormatUtils.formatDuration(seconds)
}
