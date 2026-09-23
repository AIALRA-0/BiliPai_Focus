package com.android.purebilibili.feature.video.ui.components
import com.android.purebilibili.core.ui.components.AppText

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.ui.AppAlertDialog
import com.android.purebilibili.core.ui.AppModalBottomSheet
import com.android.purebilibili.data.model.response.FavFolder
import com.android.purebilibili.feature.video.policy.resolveFavoriteFolderMediaId
import com.android.purebilibili.core.ui.AdaptiveLoadingIndicator
import com.android.purebilibili.core.ui.components.AppButton
import com.android.purebilibili.core.ui.components.AppCheckbox
import com.android.purebilibili.core.ui.components.AppCircularProgressIndicator
import com.android.purebilibili.core.ui.components.AppOutlinedTextField
import com.android.purebilibili.core.ui.components.AppTextButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteFolderSheet(
    folders: List<FavFolder>,
    isLoading: Boolean,
    selectedFolderIds: Set<Long>,
    isSaving: Boolean,
    onFolderToggle: (FavFolder) -> Unit,
    onSaveClick: () -> Unit,
    onDismissRequest: () -> Unit,
    onCreateFolder: (String, String, Boolean) -> Unit = { _, _, _ -> }
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val maxSheetHeight = remember(configuration.screenHeightDp) {
        (configuration.screenHeightDp.dp * 0.72f).coerceAtLeast(360.dp)
    }

    if (showCreateDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, intro, isPrivate ->
                onCreateFolder(title, intro, isPrivate)
                showCreateDialog = false
            }
        )
    }

    AppModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
        ) {
            // Title row keeps 新建 on the end without covering the subtitle.
            // The subtitle is a separate full-width line under the title row.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AppText(
                        text = "添加到收藏夹",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        // Reserve end space so a longer title cannot run under 新建.
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 56.dp),
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                    AppTextButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.align(Alignment.CenterEnd),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        AppText("新建")
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                AppText(
                    text = "可勾选一个或多个收藏夹，将视频收藏到自己的收藏夹",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
            
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                        contentAlignment = Alignment.Center
                ) {
                    AdaptiveLoadingIndicator()
                }
            } else if (folders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AppText("暂无收藏夹", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(folders, key = { resolveFavoriteFolderMediaId(it) }) { folder ->
                        FavoriteFolderItem(
                            folder = folder,
                            selected = selectedFolderIds.contains(resolveFavoriteFolderMediaId(folder)),
                            onClick = { onFolderToggle(folder) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .padding(top = 8.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppText(
                    text = "已选择 ${selectedFolderIds.size} 个收藏夹",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                AppButton(
                    onClick = onSaveClick,
                    enabled = !isLoading && !isSaving
                ) {
                    if (isSaving) {
                        AppCircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        AppText("保存")
                    }
                }
            }
        }
    }
}

@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var intro by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { AppText("新建收藏夹") },
        text = {
            Column {
                AppOutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { AppText("标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                AppOutlinedTextField(
                    value = intro,
                    onValueChange = { intro = it },
                    label = { AppText("简介 (选填)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppCheckbox(
                        checked = isPrivate,
                        onCheckedChange = { isPrivate = it }
                    )
                    AppText("设为私密")
                }
            }
        },
        confirmButton = {
            AppButton(
                onClick = { 
                    if (title.isNotBlank()) {
                        onConfirm(title, intro, isPrivate)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                AppText("创建")
            }
        },
        dismissButton = {
            AppTextButton(onClick = onDismiss) {
                AppText("取消")
            }
        }
    )
}

@Composable
fun FavoriteFolderItem(
    folder: FavFolder,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            AppText(
                text = folder.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            AppText(
                text = "${folder.media_count}个内容",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AppCheckbox(
            checked = selected,
            onCheckedChange = { onClick() }
        )
    }
}
