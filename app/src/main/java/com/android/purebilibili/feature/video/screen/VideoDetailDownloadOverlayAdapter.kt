// 文件路径: feature/video/screen/VideoDetailScreen.kt
package com.android.purebilibili.feature.video.screen
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import com.android.purebilibili.core.ui.components.AppIcon
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.components.AppText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.purebilibili.core.ui.AppModalBottomSheet
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.rememberAppCollectionIcon
import com.android.purebilibili.core.ui.rememberAppDownloadIcon
import com.android.purebilibili.core.ui.rememberAppMusicIcon
import com.android.purebilibili.core.ui.rememberAppPhotoIcon
import com.android.purebilibili.feature.video.viewmodel.VideoPlaybackUiState
import com.android.purebilibili.feature.video.viewmodel.VideoPlaybackViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VideoDetailDownloadOverlayAdapter(
    viewModel: VideoPlaybackViewModel,
    uiState: VideoPlaybackUiState,
) {
    //  [新增] 下载选项菜单 & 画质选择
    val showDownloadDialog by viewModel.showDownloadDialog.collectAsStateWithLifecycle()
    val successForDownload = uiState as? VideoPlaybackUiState.Success
    val downloadTasks by com.android.purebilibili.feature.download.DownloadManager.tasks.collectAsStateWithLifecycle()

    // 本地状态控制画质选择弹窗
    var showQualitySelection by remember { mutableStateOf(false) }
    var showBatchDownloadDialog by remember { mutableStateOf(false) }

    if (showDownloadDialog && successForDownload != null) {
        val batchDownloadCandidates = remember(successForDownload.info) {
            com.android.purebilibili.feature.download.resolveBatchDownloadCandidates(
                successForDownload.info
            )
        }
        AppModalBottomSheet(
            onDismissRequest = { viewModel.closeDownloadDialog() },
            containerColor = AppSurfaceTokens.surface(),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                AppText(
                    text = "下载选项",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )

                // 1. 缓存视频
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // 检查任务状态
                            val existingTask = com.android.purebilibili.feature.download.DownloadManager.getVideoTask(successForDownload.info.bvid, successForDownload.info.cid)
                            if (existingTask != null && !existingTask.isFailed) {
                                if (existingTask.isComplete) viewModel.toast("视频已缓存")
                                else viewModel.toast("正在下载中...")
                                viewModel.closeDownloadDialog()
                            } else {
                                // 打开画质选择
                                showQualitySelection = true
                                viewModel.closeDownloadDialog()
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIcon(
                        imageVector = rememberAppDownloadIcon(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        AppText(
                            text = "缓存视频",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        AppText(
                            text = "选择画质缓存当前视频",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (batchDownloadCandidates.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showBatchDownloadDialog = true
                                viewModel.closeDownloadDialog()
                            }
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(
                            imageVector = rememberAppCollectionIcon(),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            AppText(
                                text = "批量缓存",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            AppText(
                                text = "选择多个分P或合集条目统一加入下载",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 2. 下载音频
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val task = com.android.purebilibili.feature.download.DownloadTask(
                                bvid = successForDownload.info.bvid,
                                cid = successForDownload.info.cid,
                                title = successForDownload.info.title,
                                cover = successForDownload.info.pic,
                                ownerName = successForDownload.info.owner.name,
                                ownerFace = successForDownload.info.owner.face,
                                duration = 0, // 音频不需要 duration?
                                quality = 0,
                                qualityDesc = "音频",
                                videoUrl = "",
                                audioUrl = successForDownload.audioUrl ?: "",
                                isAudioOnly = true,
                                isVerticalVideo = false
                            )
                            if (task.audioUrl.isNotEmpty()) {
                                val started = com.android.purebilibili.feature.download.DownloadManager.addTask(task)
                                if (started) viewModel.toast("已开始下载音频")
                                else viewModel.toast("该任务已在下载中或已完成")
                            } else {
                                viewModel.toast("无法获取音频地址")
                            }
                            viewModel.closeDownloadDialog()
                        }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIcon(
                        imageVector = rememberAppMusicIcon(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        AppText(
                            text = "下载音频",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        AppText(
                            text = "仅保存音频文件",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 3. 保存封面
                val scope = rememberCoroutineScope()
                val context = LocalContext.current // 获取 Context

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val coverUrl = successForDownload.info.pic
                            val title = successForDownload.info.title
                            if (coverUrl.isNotEmpty()) {
                                scope.launch {
                                    val success = com.android.purebilibili.feature.download.DownloadManager.saveImageToGallery(
                                        context,
                                        coverUrl,
                                        title
                                    )
                                    // Toast 已经在 saveImageToGallery 内部或者需要外部调用?
                                    // VideoPlayerOverlay 是自己调用的。
                                    // context 是必要的。
                                    if (success) viewModel.toast("封面已保存到相册")
                                    else viewModel.toast("保存失败")
                                }
                            } else {
                                viewModel.toast("无法获取封面地址")
                            }
                            viewModel.closeDownloadDialog()
                        }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIcon(
                        imageVector = rememberAppPhotoIcon(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        AppText(
                            text = "保存封面",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        AppText(
                            text = "保存当前视频封面到相册",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // 缓存视频 - 画质选择弹窗 (当 showQualitySelection 为 true 时显示)
    if (showQualitySelection && successForDownload != null) {
        val sortedQualityOptions = successForDownload.qualityIds
            .zip(successForDownload.qualityLabels)
            .sortedByDescending { it.first }
        val highestQuality = sortedQualityOptions.firstOrNull()?.first ?: successForDownload.currentQuality

        com.android.purebilibili.feature.download.DownloadQualityDialog(
            title = successForDownload.info.title,
            qualityOptions = sortedQualityOptions,
            currentQuality = highestQuality,
            onQualitySelected = { quality, options ->
                viewModel.downloadWithQuality(quality, options)
                showQualitySelection = false
            },
            onDismiss = { showQualitySelection = false }
        )
    }

    if (showBatchDownloadDialog && successForDownload != null) {
        val batchDownloadCandidates = remember(successForDownload.info) {
            com.android.purebilibili.feature.download.resolveBatchDownloadCandidates(
                successForDownload.info
            )
        }
        val downloadedCandidateIds = remember(downloadTasks) {
            downloadTasks.values
                .filter { !it.isFailed && !it.isAudioOnly }
                .map { "${it.bvid}#${it.cid}" }
                .toSet()
        }
        val sortedQualityOptions = successForDownload.qualityIds
            .zip(successForDownload.qualityLabels)
            .sortedByDescending { it.first }
        val highestQuality = sortedQualityOptions.firstOrNull()?.first ?: successForDownload.currentQuality

        com.android.purebilibili.feature.download.BatchDownloadDialog(
            title = successForDownload.info.title,
            candidates = batchDownloadCandidates,
            qualityOptions = sortedQualityOptions,
            currentQuality = highestQuality,
            downloadedIds = downloadedCandidateIds,
            onConfirm = { quality, options, selectedCandidates ->
                viewModel.downloadBatchWithQuality(
                    qualityId = quality,
                    options = options,
                    candidates = selectedCandidates
                )
                showBatchDownloadDialog = false
            },
            onDismiss = { showBatchDownloadDialog = false }
        )
    }
}
