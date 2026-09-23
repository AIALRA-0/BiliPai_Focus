package com.android.purebilibili.feature.download

import com.android.purebilibili.core.ui.components.VideoListLayoutToggle
import com.android.purebilibili.core.ui.components.resolveVideoListColumns
import com.android.purebilibili.core.ui.components.rememberVideoListLayoutControl
import com.android.purebilibili.core.ui.components.videoListItemModifier
import com.android.purebilibili.core.ui.components.AnimatedVideoListItem
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders

import coil3.request.crossfade
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.AppSpacingTokens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
//  Material Icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.ui.ImmersiveAppScaffold as AppScaffold
import com.android.purebilibili.core.ui.AppTopBar
import com.android.purebilibili.core.ui.rememberAppBackIcon
import com.android.purebilibili.core.ui.components.AppCard
import com.android.purebilibili.core.ui.components.AppCardDefaults
import com.android.purebilibili.core.ui.components.AppCardShape
import com.android.purebilibili.core.ui.components.AppCircularProgressIndicator
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.util.NetworkUtils
import kotlinx.coroutines.delay
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel

/**
 *  离线缓存列表页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadListScreen(
    onBack: () -> Unit,
    onVideoClick: (String) -> Unit,  // bvid - 在线播放
    onOfflineVideoClick: (String) -> Unit = {}  // 🔧 [新增] taskId - 离线播放
) {
    val context = LocalContext.current
    val listLayout = rememberVideoListLayoutControl(defaultSingleColumn = true)
    val gridState = rememberLazyGridState()
    val columns = resolveVideoListColumns(
        listLayout.singleColumn,
        androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.toFloat(),
    )
    val tasks by DownloadManager.tasks.collectAsStateWithLifecycle()
    var isNetworkAvailable by remember(context) { mutableStateOf(NetworkUtils.isNetworkAvailable(context)) }
    val customDownloadPath by SettingsManager.getDownloadPath(context).collectAsStateWithLifecycle(initialValue = null)
    val downloadExportTreeUri by SettingsManager.getDownloadExportTreeUri(context).collectAsStateWithLifecycle(initialValue = null)
    val taskList = tasks.values.toList().sortedByDescending { it.createdAt }
    val currentDir = resolveDisplayedDownloadLocation(
        defaultManagedPath = remember(context) { SettingsManager.getDefaultDownloadPath(context) },
        customManagedPath = customDownloadPath,
        exportTreeUri = downloadExportTreeUri
    )

    LaunchedEffect(context) {
        while (true) {
            isNetworkAvailable = NetworkUtils.isNetworkAvailable(context)
            delay(1_500L)
        }
    }

    AppScaffold(
        topBar = {
            AppTopBar(
                title = "离线缓存",
                navigationIcon = {
                    AppIconButton(onClick = onBack) {
                        AppIcon(rememberAppBackIcon(), contentDescription = "返回")
                    }
                },
                actions = {
                    VideoListLayoutToggle(
                        singleColumn = listLayout.singleColumn,
                        onClick = listLayout.toggle,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (taskList.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(AppSpacingTokens.Large))
                    AppText(
                        text = "暂无缓存视频",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(AppSpacingTokens.Small))
                    AppText(
                        text = "在视频详情页点击「缓存」按钮下载",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                state = gridState,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    ,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 16.dp, bottom = padding.calculateBottomPadding() + 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(taskList, key = { it.id }) { task ->
                    AnimatedVideoListItem(modifier = videoListItemModifier(), enabled = true) {
                        val playableOffline = remember(task.filePath, task.status) {
                            isDownloadTaskPlayableOffline(task)
                        }
                        DownloadTaskItem(
                            task = task,
                            stacked = columns > 1,
                            onClick = {
                                when (resolveDownloadTaskClickTarget(task, isNetworkAvailable = isNetworkAvailable)) {
                                    DownloadTaskClickTarget.OfflinePlayer -> onOfflineVideoClick(task.id)
                                    null -> Unit
                                }
                            },
                            onPauseResume = {
                                if (task.isDownloading) {
                                    DownloadManager.pauseDownload(task.id)
                                } else if (task.canResume) {
                                    DownloadManager.startDownload(task.id)
                                }
                            },
                            onDelete = {
                                DownloadManager.removeTask(task.id)
                            },
                            offlinePlayable = playableOffline
                        )
                    }
                }

                // [新增] 显示当前存储路径
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AppText(
                            text = "存储位置",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(AppSpacingTokens.ExtraSmall))
                        AppText(
                            text = currentDir,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun DownloadTaskItem(
    task: DownloadTask,
    onClick: () -> Unit,
    onPauseResume: () -> Unit,
    onDelete: () -> Unit,
    stacked: Boolean = false,
    offlinePlayable: Boolean
) {
    val cover: @Composable (Modifier) -> Unit = { coverModifier ->
        // 封面
        Box(
            modifier = coverModifier
                .aspectRatio(16f / 9f)
                .clip(AppShapes.container(ContainerLevel.Chip))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            // 🖼️ [修复] 优先使用本地封面（无网络时也能显示）
            val localCoverFile = task.localCoverPath?.let { java.io.File(it) }
            val coverSource = if (localCoverFile?.exists() == true) {
                // 使用本地缓存的封面
                localCoverFile
            } else {
                // Fallback 到网络URL
                val coverUrl = task.cover.let { url ->
                    if (url.startsWith("http://")) url.replace("http://", "https://")
                    else url
                }
                coil3.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(coverUrl)
                    .httpHeaders(NetworkHeaders.Builder().set("Referer", "https://www.bilibili.com").build())
                    .crossfade(true)
                    .build()
            }

            AsyncImage(
                model = coverSource,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // 进度/状态覆盖层
            if (!task.isComplete) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    when (task.status) {
                        DownloadStatus.QUEUED -> {
                            AppText("排队中", color = Color.White, style = MaterialTheme.typography.labelMedium)
                        }
                        DownloadStatus.DOWNLOADING, DownloadStatus.MERGING -> {
                            AppCircularProgressIndicator(
                                progress = { resolveDownloadTaskProgress(task) },
                                modifier = Modifier.size(32.dp),
                                color = Color.White,
                                strokeWidth = 3.dp
                            )
                        }
                        DownloadStatus.PAUSED -> {
                            AppText("已暂停", color = Color.White, style = MaterialTheme.typography.labelMedium)
                        }
                        DownloadStatus.FAILED -> {
                            AppText("失败", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                        }
                        else -> {}
                    }
                }
            }

            // 画质标签
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(AppSpacingTokens.ExtraSmall)
                    .background(
                        Color.Black.copy(alpha = 0.7f),
                        AppShapes.container(ContainerLevel.Tag)
                    )
                    .padding(horizontal = AppSpacingTokens.ExtraSmall, vertical = AppSpacingTokens.Micro)
            ) {
                AppText(
                    text = task.qualityDesc,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

    }
    val info: @Composable () -> Unit = {
        // 信息
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            AppText(
                text = task.title,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(AppSpacingTokens.ExtraSmall))

            resolveDownloadTaskSecondaryText(task)?.let { secondaryText ->
                AppText(
                    text = secondaryText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(AppSpacingTokens.ExtraSmall))
            }

            AppText(
                text = task.ownerName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(AppSpacingTokens.ExtraSmall))

            // 状态文字
            val statusText = when (task.status) {
                DownloadStatus.QUEUED -> "排队中..."
                DownloadStatus.PENDING -> "等待中..."
                DownloadStatus.DOWNLOADING -> "下载中 ${resolveDownloadTaskProgressPercent(task)}%"
                DownloadStatus.MERGING -> "处理中..."
                DownloadStatus.COMPLETED -> "已完成"
                DownloadStatus.PAUSED -> "已暂停"
                DownloadStatus.FAILED -> task.errorMessage ?: "下载失败"
            }
            AppText(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = when (task.status) {
                    DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.secondary
                    DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.primary
                }
            )

            val assetSummary = resolveDownloadAssetSummary(task)
            val assetTexts = listOfNotNull(
                assetSummary.videoText,
                assetSummary.audioText,
                assetSummary.danmakuText
            )
            if (assetTexts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(AppSpacingTokens.ExtraSmall))
                AppText(
                    text = assetTexts.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (task.isComplete && !offlinePlayable) {
                Spacer(modifier = Modifier.height(AppSpacingTokens.ExtraSmall))
                AppText(
                    text = if (!task.exportedFileUri.isNullOrBlank()) {
                        "已导出到自定义目录，当前列表不直接离线播放"
                    } else {
                        "本地缓存文件不可用"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

    }
    val actions: @Composable () -> Unit = {
        // 操作按钮
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 暂停/继续
            if (task.isDownloading || task.canResume) {
                AppIconButton(onClick = onPauseResume) {
                    AppIcon(
                        imageVector = if (task.isDownloading) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = if (task.isDownloading) "暂停" else "继续",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 删除
            AppIconButton(onClick = onDelete) {
                AppIcon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = AppCardShape.Semantic(ContainerLevel.Card),
        colors = AppCardDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        if (stacked) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                cover(Modifier.fillMaxWidth())
                info()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { actions() }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Bottom) {
                cover(Modifier.width(120.dp))
                Spacer(Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f)) { info() }
                actions()
            }
        }
    }
}
