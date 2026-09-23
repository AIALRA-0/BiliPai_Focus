package com.android.purebilibili.feature.settings
import com.android.purebilibili.core.ui.components.AppText

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.android.purebilibili.data.repository.BilibiliBlockedListSyncRepository
import com.android.purebilibili.data.repository.BlockedUpRepository
import com.android.purebilibili.data.repository.buildBlockedUpShareText
import com.android.purebilibili.data.repository.parseBlockedUpShareText
import com.android.purebilibili.feature.settings.ui.SettingsPageScaffold
import com.android.purebilibili.feature.settings.ui.settingsScrollContentPadding
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppAlertDialog
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.rememberAppBackIcon
import com.android.purebilibili.core.ui.components.AppButton
import com.android.purebilibili.core.ui.components.AppCircularProgressIndicator
import com.android.purebilibili.core.ui.components.AppOutlinedButton
import com.android.purebilibili.core.ui.components.AppTextField
import com.android.purebilibili.core.ui.components.AppTextButton
import com.android.purebilibili.core.ui.components.AppPreferenceSectionTitle
import com.android.purebilibili.core.ui.components.UserLevelBadge
import com.android.purebilibili.core.util.ShareUtils
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.purebilibili.core.ui.AdaptiveLoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedListScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { BlockedUpRepository(context) }
    val fileService = remember { BlockedListFileService(context.applicationContext) }
    val syncRepository = remember { BilibiliBlockedListSyncRepository(repository) }
    val blockedUps by repository.getAllBlockedUps().collectAsStateWithLifecycle(initialValue = emptyList())
    val latestBlockedUps by rememberUpdatedState(blockedUps)
    val scope = rememberCoroutineScope()
    var syncingBlockedList by remember { mutableStateOf(false) }
    var refreshingProfiles by remember { mutableStateOf(false) }
    var blockedListSyncMessage by remember { mutableStateOf<String?>(null) }
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                blockedListSyncMessage = "正在导出黑名单 JSON..."
                blockedListSyncMessage = fileService.exportJsonToUri(uri, latestBlockedUps).fold(
                    onSuccess = { "已导出 ${latestBlockedUps.size} 个黑名单用户到 JSON 文件" },
                    onFailure = { it.message ?: "导出黑名单 JSON 失败" }
                )
            }
        }
    }
    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                blockedListSyncMessage = "正在导入黑名单 JSON..."
                val text = fileService.readImportText(uri).getOrElse {
                    blockedListSyncMessage = it.message ?: "读取黑名单 JSON 失败"
                    return@launch
                }
                val items = parseBlockedUpShareText(text)
                blockedListSyncMessage = repository.importBlockedUps(items).message
            }
        }
    }

    val screenTitle = "黑名单管理"
    val backLabel = "返回"
    val bottomContentPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    SettingsPageScaffold(
        title = screenTitle,
        onBack = onBack,
        backContentDescription = backLabel,
        bottomContentPadding = bottomContentPadding,
        scrollHost = SettingsPageScrollHost.External,
    ) {
        BlockedListContent(
            blockedUps = blockedUps,
            syncingBlockedList = syncingBlockedList,
            refreshingProfiles = refreshingProfiles,
            blockedListSyncMessage = blockedListSyncMessage,
            onSyncBlockedList = {
                if (!syncingBlockedList) {
                    scope.launch {
                        syncingBlockedList = true
                        blockedListSyncMessage = "正在同步 B站黑名单..."
                        val result = syncRepository.importFromBilibili()
                        blockedListSyncMessage = result.fold(
                            onSuccess = { it.message },
                            onFailure = { it.message ?: "同步 B站黑名单失败" }
                        )
                        syncingBlockedList = false
                    }
                }
            },
            onRefreshProfiles = {
                if (!refreshingProfiles) {
                    scope.launch {
                        refreshingProfiles = true
                        blockedListSyncMessage = "正在刷新黑名单用户资料..."
                        blockedListSyncMessage = repository.refreshBlockedUpProfiles().message
                        refreshingProfiles = false
                    }
                }
            },
            onShareBlockedList = {
                ShareUtils.shareText(
                    context = context,
                    subject = "BiliPai 黑名单",
                    text = buildBlockedUpShareText(blockedUps),
                    chooserTitle = "分享黑名单"
                )
            },
            onExportBlockedListJson = {
                exportJsonLauncher.launch(buildBlockedListJsonFileName())
            },
            onImportBlockedListJsonRequest = {
                importJsonLauncher.launch(arrayOf("application/json", "text/plain", "text/*", "application/octet-stream"))
            },
            onImportBlockedList = { text ->
                scope.launch {
                    val items = parseBlockedUpShareText(text)
                    blockedListSyncMessage = repository.importBlockedUps(items).message
                }
            },
            onUnblock = { mid ->
                scope.launch {
                    blockedListSyncMessage = repository.unblockUpWithBilibiliSync(mid).message
                }
            },
        )
    }
}

@Composable
fun BlockedListContent(
    blockedUps: List<com.android.purebilibili.core.database.entity.BlockedUp>,
    syncingBlockedList: Boolean = false,
    refreshingProfiles: Boolean = false,
    blockedListSyncMessage: String? = null,
    onSyncBlockedList: (() -> Unit)? = null,
    onRefreshProfiles: (() -> Unit)? = null,
    onShareBlockedList: (() -> Unit)? = null,
    onExportBlockedListJson: (() -> Unit)? = null,
    onImportBlockedListJsonRequest: (() -> Unit)? = null,
    onImportBlockedList: ((String) -> Unit)? = null,
    onUnblock: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    if (showImportDialog && onImportBlockedList != null) {
        AppAlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { AppText("导入黑名单") },
            text = {
                AppTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    label = "粘贴分享出来的黑名单文本",
                    singleLine = false,
                    minLines = 5
                )
            },
            confirmButton = {
                AppTextButton(
                    onClick = {
                        onImportBlockedList(importText)
                        showImportDialog = false
                        importText = ""
                    },
                    enabled = importText.isNotBlank()
                ) {
                    AppText("导入")
                }
            },
            dismissButton = {
                AppTextButton(onClick = { showImportDialog = false }) {
                    AppText("取消")
                }
            }
        )
    }

    if (blockedUps.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppText(
                text = "暂无屏蔽的 UP 主",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (onSyncBlockedList != null) {
                Spacer(modifier = Modifier.height(16.dp))
                BlockedListSyncAction(
                    syncing = syncingBlockedList,
                    refreshingProfiles = refreshingProfiles,
                    message = blockedListSyncMessage,
                    onSync = onSyncBlockedList,
                    onRefreshProfiles = onRefreshProfiles,
                    onShareBlockedList = onShareBlockedList,
                    onExportBlockedListJson = onExportBlockedListJson,
                    onImportBlockedListJsonRequest = onImportBlockedListJsonRequest,
                    onImportBlockedListRequest = if (onImportBlockedList != null) {
                        { showImportDialog = true }
                    } else {
                        null
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = settingsScrollContentPadding(extraHorizontal = 16.dp, extraVertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                if (onSyncBlockedList != null) {
                    BlockedListSyncAction(
                        syncing = syncingBlockedList,
                        refreshingProfiles = refreshingProfiles,
                        message = blockedListSyncMessage,
                        onSync = onSyncBlockedList,
                        onRefreshProfiles = onRefreshProfiles,
                        onShareBlockedList = onShareBlockedList,
                        onExportBlockedListJson = onExportBlockedListJson,
                        onImportBlockedListJsonRequest = onImportBlockedListJsonRequest,
                        onImportBlockedListRequest = if (onImportBlockedList != null) {
                            { showImportDialog = true }
                        } else {
                            null
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                AppPreferenceSectionTitle("已屏蔽的 UP 主")
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            items(blockedUps, key = { it.mid }) { up ->
                BlockedUpItem(
                    mid = up.mid,
                    name = up.name,
                    face = up.face,
                    level = up.level,
                    sign = up.sign,
                    vipLabel = up.vipLabel,
                    officialTitle = up.officialTitle,
                    follower = up.follower,
                    archiveCount = up.archiveCount,
                    isDeleted = up.isDeleted,
                    onUnblock = { onUnblock(up.mid) }
                )
            }
        }
    }
}

@Composable
private fun BlockedListSyncAction(
    syncing: Boolean,
    refreshingProfiles: Boolean,
    message: String?,
    onSync: () -> Unit,
    onRefreshProfiles: (() -> Unit)?,
    onShareBlockedList: (() -> Unit)?,
    onExportBlockedListJson: (() -> Unit)?,
    onImportBlockedListJsonRequest: (() -> Unit)?,
    onImportBlockedListRequest: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.container(ContainerLevel.Card))
            .background(AppSurfaceTokens.cardContainer())
            .padding(12.dp)
    ) {
        AppButton(
            onClick = onSync,
            enabled = !syncing && !refreshingProfiles,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            if (syncing) {
                AppCircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            AppText(if (syncing) "同步中" else "同步 B站黑名单")
        }
        if (
            onRefreshProfiles != null ||
            onExportBlockedListJson != null ||
            onImportBlockedListJsonRequest != null ||
            onShareBlockedList != null ||
            onImportBlockedListRequest != null
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onRefreshProfiles != null) {
                    AppOutlinedButton(
                        onClick = onRefreshProfiles,
                        enabled = !syncing && !refreshingProfiles,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (refreshingProfiles) {
                            AdaptiveLoadingIndicator(
                                size = 16.dp,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        AppText(if (refreshingProfiles) "刷新中" else "刷新资料")
                    }
                }
                if (onExportBlockedListJson != null) {
                    AppOutlinedButton(
                        onClick = onExportBlockedListJson,
                        enabled = !syncing && !refreshingProfiles,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AppText("导出 JSON 文件")
                    }
                }
                if (onImportBlockedListJsonRequest != null) {
                    AppOutlinedButton(
                        onClick = onImportBlockedListJsonRequest,
                        enabled = !syncing && !refreshingProfiles,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AppText("导入 JSON 文件")
                    }
                }
                if (onImportBlockedListRequest != null) {
                    AppOutlinedButton(
                        onClick = onImportBlockedListRequest,
                        enabled = !syncing && !refreshingProfiles,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AppText("粘贴导入")
                    }
                }
                if (onShareBlockedList != null) {
                    AppOutlinedButton(
                        onClick = onShareBlockedList,
                        enabled = !syncing && !refreshingProfiles,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AppText("分享文本")
                    }
                }
            }
        }
        if (!message.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            AppText(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BlockedUpItem(
    mid: Long,
    name: String,
    face: String,
    level: Int?,
    sign: String,
    vipLabel: String,
    officialTitle: String,
    follower: Long?,
    archiveCount: Int?,
    isDeleted: Boolean,
    onUnblock: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.container(ContainerLevel.Card))
            .background(AppSurfaceTokens.cardContainer())
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = face,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.Gray.copy(alpha = 0.2f)),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppText(
                    text = name.ifBlank { "UP主$mid" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                level?.let {
                    Spacer(modifier = Modifier.width(6.dp))
                    UserLevelBadge(level = it, height = 12.dp)
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            AppText(
                text = buildBlockedUpMetaLine(
                    mid = mid,
                    level = level,
                    vipLabel = vipLabel,
                    officialTitle = officialTitle,
                    follower = follower,
                    archiveCount = archiveCount,
                    isDeleted = isDeleted
                ),
                style = MaterialTheme.typography.bodySmall,
                color = if (isDeleted) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
            if (sign.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                AppText(
                    text = sign.trim(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
        
        AppTextButton(onClick = onUnblock) {
            AppText("解除屏蔽", color = Color.Red)
        }
    }
}

internal fun buildBlockedUpMetaLine(
    mid: Long,
    level: Int?,
    vipLabel: String,
    officialTitle: String,
    follower: Long?,
    archiveCount: Int?,
    isDeleted: Boolean
): String {
    val parts = mutableListOf("UID $mid")
    level?.let { parts += "LV$it" }
    if (isDeleted) parts += "疑似已注销"
    if (vipLabel.isNotBlank()) parts += vipLabel
    if (officialTitle.isNotBlank()) parts += officialTitle
    follower?.let { parts += "粉丝 $it" }
    archiveCount?.let { parts += "投稿 $it" }
    return parts.joinToString(" · ")
}
