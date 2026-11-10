package com.android.purebilibili.feature.video.ui.components

import com.android.purebilibili.core.ui.components.AppGlassMenu
import com.android.purebilibili.core.ui.components.AppGlassMenuPlacement
import com.android.purebilibili.core.ui.components.AppGlassMenuSource
import com.android.purebilibili.core.ui.AppContentColors
import com.android.purebilibili.core.ui.components.AppText

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import com.android.purebilibili.core.ui.components.AppLazyColumn as LazyColumn
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
import com.android.purebilibili.core.ui.AppModalPresentation
import android.content.res.Configuration
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
    modifier: Modifier = Modifier,
    source: AppGlassMenuSource? = LocalFavoriteFolderMenuAnchor.current?.source,
    onCreateFolder: (String, String, Boolean) -> Unit = { _, _, _ -> }
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val glass = com.android.purebilibili.core.ui.components.appGlassMenuAvailable()
    val sidePanel = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
        configuration.screenWidthDp >= 600
    val maxSheetHeight = (configuration.screenHeightDp.dp * 0.72f)
        .coerceAtLeast(360.dp)
        .coerceAtMost(configuration.screenHeightDp.dp)

    if (showCreateDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, intro, isPrivate ->
                onCreateFolder(title, intro, isPrivate)
                showCreateDialog = false
            }
        )
    }

    val body: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (sidePanel) Modifier.fillMaxHeight() else Modifier.heightIn(max = maxSheetHeight))
        ) {
            // Title row keeps 新建 on the end without covering the subtitle.
            // The subtitle is a separate full-width line under the title row.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (sidePanel) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppText(
                            "添加到收藏夹",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                        AppTextButton(onClick = { showCreateDialog = true }) { AppText("新建") }
                        AppTextButton(onClick = onDismissRequest, enabled = !isSaving) { AppText("关闭") }
                    }
                } else Box(
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
                if (!sidePanel) {
                    Spacer(modifier = Modifier.height(4.dp))
                    AppText(
                        text = "可选择多个收藏夹",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppContentColors.secondaryText(),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (sidePanel) Modifier.weight(1f) else Modifier.height(120.dp)),
                        contentAlignment = Alignment.Center
                ) {
                    AdaptiveLoadingIndicator()
                }
            } else if (folders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (sidePanel) Modifier.weight(1f) else Modifier.height(120.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AppText("暂无收藏夹", color = AppContentColors.secondaryText())
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier = Modifier.weight(1f, fill = sidePanel)
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
                    // 同窗玻璃宿主已处理系统栏，原生弹层保留原有底部 inset。
                    .then(if (glass) Modifier else Modifier.navigationBarsPadding())
                    .padding(top = 8.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppText(
                    text = "已选择 ${selectedFolderIds.size} 个收藏夹",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppContentColors.secondaryText(),
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
                            color = AppContentColors.onAccent()
                        )
                    } else {
                        AppText("保存")
                    }
                }
            }
        }
    }
    // 液态玻璃显式选择同窗锚点形变；关闭玻璃继续使用原生底部／侧边弹层。
    if (AppGlassMenu(
            expanded = true,
            modifier = modifier,
            onDismissRequest = onDismissRequest,
            title = "添加到收藏夹",
            placement = if (source?.isAvailable == true) AppGlassMenuPlacement.ANCHOR_END
                else AppGlassMenuPlacement.CENTER,
            source = source,
            minWidth = 0.dp,
            maxWidth = if (sidePanel) 360.dp else 420.dp,
            maxHeight = if (sidePanel) (configuration.screenHeightDp - 32).coerceAtLeast(1).dp
                else maxSheetHeight,
            interactiveContent = true,
            showScrollbar = false,
            useAvailableHeight = true,
            content = body,
        )) return
    AppModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        presentationOverride = if (sidePanel) AppModalPresentation.SideSheet else null,
        dragHandle = if (sidePanel) null else ({ BottomSheetDefaults.DragHandle() }),
    ) {
        body()
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
                color = AppContentColors.primaryText()
            )
            Spacer(modifier = Modifier.height(2.dp))
            AppText(
                text = "${folder.media_count}个内容",
                style = MaterialTheme.typography.bodySmall,
                color = AppContentColors.secondaryText()
            )
        }
        AppCheckbox(
            checked = selected,
            onCheckedChange = { onClick() }
        )
    }
}
