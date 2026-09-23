package com.android.purebilibili.feature.download

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import com.android.purebilibili.core.ui.components.AppButton
import com.android.purebilibili.core.ui.components.AppCheckbox
import com.android.purebilibili.core.ui.components.AppFilterChip
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.components.AppOutlinedButton
import com.android.purebilibili.core.ui.components.AppRadioButton
import com.android.purebilibili.core.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.android.purebilibili.core.ui.appContentDialogWidth
import com.android.purebilibili.core.ui.resolveAppExpandedContentDialogLayoutPolicy
import com.android.purebilibili.core.ui.resolveAppContentDialogProperties
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppPopupSurface
import com.android.purebilibili.core.ui.AppPopupSurfaceType
import com.android.purebilibili.core.ui.ContainerLevel

@Composable
internal fun BatchDownloadDialog(
    title: String,
    candidates: List<BatchDownloadCandidate>,
    qualityOptions: List<Pair<Int, String>>,
    currentQuality: Int,
    downloadedIds: Set<String>,
    onConfirm: (Int, DownloadOptions, List<BatchDownloadCandidate>) -> Unit,
    onDismiss: () -> Unit
) {
    var workingCandidates by remember(candidates) { mutableStateOf(candidates) }
    var selectedQuality by remember(currentQuality) { mutableIntStateOf(currentQuality) }
    var includeDanmaku by remember { mutableStateOf(true) }
    val dialogLayout = remember { resolveAppExpandedContentDialogLayoutPolicy() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = resolveAppContentDialogProperties(
            usePlatformDefaultWidth = dialogLayout.usePlatformDefaultWidth,
        ),
    ) {
        BoxWithConstraints(
            modifier = Modifier.appContentDialogWidth(policy = dialogLayout, wrapHeight = false),
        ) {
            val screenHeightDp = maxHeight.value.toInt().coerceAtLeast(1)
            val dialogMaxHeight = resolveBatchDownloadDialogMaxHeight(screenHeightDp).dp
            val candidateListMaxHeight = resolveBatchDownloadCandidateListMaxHeight(
                screenHeightDp = screenHeightDp,
                qualityOptionCount = qualityOptions.size
            ).dp

            AppPopupSurface(
                type = AppPopupSurfaceType.DIALOG,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = dialogMaxHeight),
                shape = AppShapes.container(ContainerLevel.Floating),
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    AppText(
                        text = "批量缓存",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AppText(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppFilterChip(
                            selected = false,
                            onClick = {
                                workingCandidates = selectAllBatchDownloadCandidates(workingCandidates)
                            },
                            label = { AppText("全选") }
                        )
                        AppFilterChip(
                            selected = false,
                            onClick = {
                                workingCandidates = invertBatchDownloadCandidateSelection(workingCandidates)
                            },
                            label = { AppText("反选") }
                        )
                        AppFilterChip(
                            selected = false,
                            onClick = {
                                workingCandidates = selectOnlyUndownloadedBatchCandidates(
                                    candidates = workingCandidates,
                                    downloadedIds = downloadedIds
                                )
                            },
                            label = { AppText("仅未下载") }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    AppText(
                        text = "选择条目",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = candidateListMaxHeight)
                    ) {
                        items(workingCandidates, key = { it.id }) { candidate ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        workingCandidates = workingCandidates.map {
                                            if (it.id == candidate.id) it.copy(selected = !it.selected) else it
                                        }
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppCheckbox(
                                    checked = candidate.selected,
                                    onCheckedChange = { checked ->
                                        workingCandidates = workingCandidates.map {
                                            if (it.id == candidate.id) it.copy(selected = checked) else it
                                        }
                                    }
                                )
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    AppText(
                                        text = candidate.label,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    AppText(
                                        text = candidate.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (candidate.id in downloadedIds) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                                shape = AppShapes.container(ContainerLevel.Pill)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        AppText(
                                            text = "已存在",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { includeDanmaku = !includeDanmaku }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppCheckbox(
                            checked = includeDanmaku,
                            onCheckedChange = { includeDanmaku = it }
                        )
                        AppText(
                            text = "同时缓存弹幕",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    AppText(
                        text = "统一画质",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    qualityOptions.forEach { (qualityId, qualityLabel) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedQuality = qualityId }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppRadioButton(
                                selected = selectedQuality == qualityId,
                                onClick = { selectedQuality = qualityId }
                            )
                            AppText(
                                text = qualityLabel,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AppOutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            AppText("取消")
                        }
                        AppButton(
                            onClick = {
                                onConfirm(
                                    selectedQuality,
                                    DownloadOptions(includeDanmaku = includeDanmaku),
                                    workingCandidates
                                )
                            },
                            modifier = Modifier.weight(1f),
                            enabled = canConfirmBatchDownload(workingCandidates)
                        ) {
                            AppText("加入下载")
                        }
                    }
                }
            }
        }
    }
}
