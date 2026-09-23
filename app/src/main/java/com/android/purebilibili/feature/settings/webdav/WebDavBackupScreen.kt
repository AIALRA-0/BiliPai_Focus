package com.android.purebilibili.feature.settings.webdav

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import com.android.purebilibili.core.ui.AdaptiveLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.components.AppSwitch
import com.android.purebilibili.core.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.R
import com.android.purebilibili.core.theme.iOSBlue
import com.android.purebilibili.core.theme.iOSGreen
import com.android.purebilibili.core.theme.iOSOrange
import com.android.purebilibili.core.theme.iOSPink
import com.android.purebilibili.feature.settings.SettingsPageScrollHost
import com.android.purebilibili.feature.settings.rememberThemeAwareSettingsIcon
import com.android.purebilibili.feature.settings.ui.SettingsPageScaffold
import com.android.purebilibili.feature.settings.ui.settingsScrollContentPadding
import com.android.purebilibili.core.ui.AppAlertDialog
import com.android.purebilibili.core.ui.AppDialogAction
import com.android.purebilibili.core.ui.components.AppPreference
import com.android.purebilibili.core.ui.components.AppPreferenceDivider
import com.android.purebilibili.core.ui.components.AppPreferenceGroup
import com.android.purebilibili.core.ui.components.AppPreferenceSectionTitle
import com.android.purebilibili.core.ui.components.AppTextField
import com.android.purebilibili.core.ui.components.AppSwitchPreference
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Reset
import top.yukonga.miuix.kmp.icon.extended.UploadCloud

private enum class WebDavEditMode {
    SERVER,
    ACCOUNT,
    REMOTE_DIR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebDavBackupScreen(
    onBack: () -> Unit,
    viewModel: WebDavBackupViewModel = viewModel()
) {
    val screenTitle = stringResource(R.string.webdav_backup_title)
    val backLabel = stringResource(R.string.common_back)
    val refreshLabel = stringResource(R.string.common_refresh)
    val saveLabel = stringResource(R.string.common_save)
    val cancelLabel = stringResource(R.string.common_cancel)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showEditDialog by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var editMode by remember { mutableStateOf(WebDavEditMode.SERVER) }

    var draftBaseUrl by remember { mutableStateOf("") }
    var draftUsername by remember { mutableStateOf("") }
    var draftPassword by remember { mutableStateOf("") }
    var draftRemoteDir by remember { mutableStateOf(DEFAULT_WEBDAV_REMOTE_DIR) }
    var draftEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(showEditDialog) {
        if (showEditDialog) {
            draftBaseUrl = uiState.config.baseUrl
            draftUsername = uiState.config.username
            draftPassword = uiState.config.password
            draftRemoteDir = uiState.config.remoteDir
            draftEnabled = uiState.config.enabled
        }
    }

    val bottomContentPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    SettingsPageScaffold(
        title = screenTitle,
        onBack = onBack,
        backContentDescription = backLabel,
        bottomContentPadding = bottomContentPadding,
        scrollHost = SettingsPageScrollHost.External,
        actions = {
            AppIconButton(onClick = { viewModel.refreshRemoteBackups() }) {
                AppIcon(
                    imageVector = com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_refresh_fill_24),
                    contentDescription = refreshLabel,
                )
            }
        },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = settingsScrollContentPadding(extraBottom = 24.dp),
            ) {
            item {
                AppPreferenceSectionTitle("连接状态")
                AppPreferenceGroup {
                    val statusText = when {
                        uiState.isBusy -> uiState.statusMessage ?: "正在处理..."
                        // 恢复会覆盖本地持久化文件，进程重启后才能稳定读取新内容。
                        uiState.restoreRequiresRestart -> uiState.statusMessage ?: "恢复完成，重启应用后生效"
                        !uiState.statusMessage.isNullOrBlank() -> uiState.statusMessage ?: ""
                        else -> "尚未执行操作"
                    }
                    AppPreference(
                        icon = if (uiState.restoreRequiresRestart) com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_warning_fill_24) else com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_info_fill_24),
                        title = if (uiState.restoreRequiresRestart) "需要重启应用" else "执行状态",
                        value = statusText,
                        onClick = if (uiState.statusMessage != null) ({ viewModel.clearStatus() }) else null,
                        iconTint = if (uiState.restoreRequiresRestart) iOSOrange else iOSBlue,
                        showChevron = false
                    )
                }
            }

            item {
                AppPreferenceSectionTitle("配置")
                AppPreferenceGroup {
                    // 配置项图标按“能力/服务器/账号/目录”映射，降低识别成本。
                    AppSwitchPreference(
                        icon = com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_cloud_fill_24),
                        title = "启用 WebDAV 云备份",
                        subtitle = "开启后每天自动备份，同时保留手动备份能力",
                        checked = uiState.config.enabled,
                        onCheckedChange = { viewModel.setEnabled(it) },
                        iconTint = iOSBlue
                    )
                    AppPreferenceDivider(startIndent = 66.dp)
                    AppPreference(
                        icon = com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_storage_fill_24),
                        title = "服务器",
                        value = uiState.config.baseUrl.ifBlank { "未配置" },
                        onClick = {
                            editMode = WebDavEditMode.SERVER
                            showEditDialog = true
                        },
                        iconTint = iOSBlue
                    )
                    AppPreferenceDivider(startIndent = 66.dp)
                    AppPreference(
                        icon = com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_person_fill_24),
                        title = "用户名",
                        value = uiState.config.username.ifBlank { "未配置" },
                        onClick = {
                            editMode = WebDavEditMode.ACCOUNT
                            showEditDialog = true
                        },
                        iconTint = iOSBlue
                    )
                    AppPreferenceDivider(startIndent = 66.dp)
                    AppPreference(
                        icon = com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_drive_folder_upload_fill_24),
                        title = "远端目录",
                        value = uiState.config.remoteDir,
                        onClick = {
                            editMode = WebDavEditMode.REMOTE_DIR
                            showEditDialog = true
                        },
                        iconTint = iOSBlue
                    )
                }
            }

            item {
                AppPreferenceSectionTitle("操作")
                AppPreferenceGroup {
                    AppPreference(
                        icon = rememberThemeAwareSettingsIcon(
                            materialSymbolResource = R.drawable.ms_cloud_done_24,
                            miuixIcon = MiuixIcons.UploadCloud,
                        ),
                        title = "测试连接",
                        subtitle = "验证账号与目录可用性",
                        onClick = { viewModel.testConnection() },
                        iconTint = iOSGreen
                    )
                    AppPreferenceDivider(startIndent = 66.dp)
                    AppPreference(
                        icon = com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_backup_fill_24),
                        title = "立即备份",
                        subtitle = "上传当前设置与插件配置",
                        onClick = { viewModel.backupNow() },
                        iconTint = iOSBlue
                    )
                    AppPreferenceDivider(startIndent = 66.dp)
                    AppPreference(
                        icon = rememberThemeAwareSettingsIcon(
                            materialSymbolResource = R.drawable.ms_settings_backup_restore_24,
                            miuixIcon = MiuixIcons.Reset,
                        ),
                        title = "恢复最新备份",
                        subtitle = "会覆盖本地设置，建议先手动备份",
                        onClick = { showRestoreConfirm = true },
                        iconTint = iOSPink
                    )
                    AppPreferenceDivider(startIndent = 66.dp)
                    AppPreference(
                        icon = com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_refresh_fill_24),
                        title = "刷新远端列表",
                        subtitle = "读取 WebDAV 目录中的备份文件",
                        onClick = { viewModel.refreshRemoteBackups() },
                        iconTint = iOSOrange
                    )
                }
            }

            item {
                AppPreferenceSectionTitle("远端备份")
                AppPreferenceGroup {
                    if (uiState.remoteBackups.isEmpty()) {
                        AppPreference(
                            icon = com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_inventory2_fill_24),
                            title = "暂无备份",
                            value = "可先点击“立即备份”生成第一份快照",
                            onClick = null,
                            iconTint = iOSOrange,
                            showChevron = false
                        )
                    } else {
                        uiState.remoteBackups.take(10).forEachIndexed { index, entry ->
                            AppPreference(
                                icon = com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_folder_copy_fill_24),
                                title = entry.fileName,
                                value = "${entry.sizeBytes} B",
                                onClick = null,
                                iconTint = iOSBlue,
                                showChevron = false
                            )
                            if (index != uiState.remoteBackups.take(10).lastIndex) {
                                AppPreferenceDivider(startIndent = 66.dp)
                            }
                        }
                    }
                }
            }
            }

            if (uiState.isBusy) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AdaptiveLoadingIndicator()
                }
            }
        }
    }

    if (showEditDialog) {
        val dialogTitle = when (editMode) {
            WebDavEditMode.SERVER -> "服务器地址"
            WebDavEditMode.ACCOUNT -> "账号信息"
            WebDavEditMode.REMOTE_DIR -> "远端目录"
        }
        AppAlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { AppText(dialogTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    when (editMode) {
                        WebDavEditMode.SERVER -> {
                            AppTextField(
                                value = draftBaseUrl,
                                onValueChange = { draftBaseUrl = it },
                                label = "服务器地址",
                                placeholder = "https://dav.example.com/remote.php/dav/files/<user>",
                                singleLine = true
                            )
                        }

                        WebDavEditMode.ACCOUNT -> {
                            AppTextField(
                                value = draftUsername,
                                onValueChange = { draftUsername = it },
                                label = "用户名",
                                singleLine = true
                            )
                            AppTextField(
                                value = draftPassword,
                                onValueChange = { draftPassword = it },
                                label = "密码",
                                singleLine = true
                            )
                        }

                        WebDavEditMode.REMOTE_DIR -> {
                            AppTextField(
                                value = draftRemoteDir,
                                onValueChange = { draftRemoteDir = it },
                                label = "远端目录",
                                placeholder = "/BiliPai/backups",
                                singleLine = true
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppText("启用备份", modifier = Modifier.weight(1f))
                                AppSwitch(
                                    checked = draftEnabled,
                                    onCheckedChange = { draftEnabled = it }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                AppDialogAction(onClick = {
                    viewModel.saveConfig(
                        WebDavBackupConfig(
                            baseUrl = draftBaseUrl,
                            username = draftUsername,
                            password = draftPassword,
                            remoteDir = draftRemoteDir,
                            enabled = draftEnabled
                        )
                    )
                    showEditDialog = false
                }) {
                    AppText(saveLabel)
                }
            },
            dismissButton = {
                AppDialogAction(onClick = { showEditDialog = false }) {
                    AppText(cancelLabel)
                }
            }
        )
    }

    if (showRestoreConfirm) {
        AppAlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { AppText("确认恢复最新备份") },
            text = {
                AppText("恢复会覆盖当前本地设置与插件配置。建议先执行一次“立即备份”。")
            },
            confirmButton = {
                AppDialogAction(onClick = {
                    showRestoreConfirm = false
                    viewModel.restoreLatest()
                }) {
                    AppText("继续恢复")
                }
            },
            dismissButton = {
                AppDialogAction(onClick = { showRestoreConfirm = false }) {
                    AppText(cancelLabel)
                }
            }
        )
    }
}
