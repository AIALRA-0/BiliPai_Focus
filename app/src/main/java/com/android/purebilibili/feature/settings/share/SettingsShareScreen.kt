package com.android.purebilibili.feature.settings.share

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.BuildConfig
import com.android.purebilibili.R
import com.android.purebilibili.core.theme.iOSBlue
import com.android.purebilibili.core.theme.iOSGreen
import com.android.purebilibili.core.theme.iOSOrange
import com.android.purebilibili.core.theme.iOSPink
import com.android.purebilibili.core.theme.iOSPurple
import com.android.purebilibili.core.ui.IOSAlertDialog
import com.android.purebilibili.core.ui.IOSDialogAction
import com.android.purebilibili.core.ui.components.IOSClickableItem
import com.android.purebilibili.core.ui.components.IOSDivider
import com.android.purebilibili.core.ui.components.IOSGroup
import com.android.purebilibili.core.ui.components.IOSSectionTitle
import com.android.purebilibili.core.ui.iOSLargeTitleBar
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@Composable
fun SettingsShareScreen(
    onBack: () -> Unit,
    viewModel: SettingsShareViewModel = viewModel()
) {
    val context = LocalContext.current
    val screenTitle = stringResource(R.string.settings_share_title)
    val backLabel = stringResource(R.string.common_back)
    val shareChooserTitle = stringResource(R.string.settings_share_chooser_title)
    val shareOpenFailed = stringResource(R.string.settings_share_open_failed)
    val importConfirmLabel = stringResource(R.string.settings_share_import_confirm)
    val viewSkippedLabel = stringResource(R.string.settings_share_view_skipped)
    val cancelLabel = stringResource(R.string.common_cancel)
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val hazeState = com.android.purebilibili.core.ui.blur.rememberRecoverableHazeState()
    val scrollOffset by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 2000f
            else listState.firstVisibleItemScrollOffset.toFloat()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportToUri(uri)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.loadImportPreview(uri)
        }
    }

    LaunchedEffect(uiState.pendingShareUri) {
        val shareUri = uiState.pendingShareUri ?: return@LaunchedEffect
        runCatching {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, shareUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, shareChooserTitle))
        }.onFailure {
            Toast.makeText(
                context,
                it.message ?: shareOpenFailed,
                Toast.LENGTH_SHORT
            ).show()
        }
        viewModel.consumeShareUri()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState),
            contentPadding = PaddingValues(top = 118.dp, bottom = 24.dp)
        ) {
            item {
                IOSSectionTitle("执行状态")
                IOSGroup {
                    IOSClickableItem(
                        icon = if (uiState.isBusy) Icons.Filled.Info else Icons.Filled.CheckCircle,
                        title = if (uiState.isBusy) "正在处理" else "最近状态",
                        value = uiState.statusMessage ?: "尚未执行导入导出操作",
                        onClick = if (uiState.statusMessage != null) ({ viewModel.clearStatus() }) else null,
                        iconTint = if (uiState.isBusy) iOSOrange else iOSGreen,
                        showChevron = false
                    )
                }
            }

            item {
                IOSSectionTitle("说明")
                IOSGroup {
                    IOSClickableItem(
                        icon = Icons.Filled.CheckCircle,
                        title = "会一起分享的内容",
                        subtitle = "外观、播放、手势、弹幕、导航等可交流设置",
                        onClick = null,
                        iconTint = iOSGreen,
                        showChevron = false
                    )
                    IOSDivider(startIndent = 66.dp)
                    IOSClickableItem(
                        icon = Icons.Filled.Lock,
                        title = "会自动跳过的内容",
                        subtitle = "账号、下载路径、WebDAV、隐私与设备相关配置",
                        onClick = null,
                        iconTint = iOSPurple,
                        showChevron = false
                    )
                }
            }

            item {
                IOSSectionTitle("操作")
                IOSGroup {
                    IOSClickableItem(
                        icon = Icons.Filled.Download,
                        title = "导出到文件",
                        subtitle = "生成可分享的设置文件（JSON）",
                        onClick = {
                            exportLauncher.launch(
                                buildSettingsShareFileName(
                                    appVersion = BuildConfig.VERSION_NAME,
                                    epochMs = System.currentTimeMillis()
                                )
                            )
                        },
                        iconTint = iOSBlue
                    )
                    IOSDivider(startIndent = 66.dp)
                    IOSClickableItem(
                        icon = Icons.Filled.Share,
                        title = "分享导出文件",
                        subtitle = "导出后直接调起系统分享",
                        onClick = { viewModel.prepareShare() },
                        iconTint = iOSGreen
                    )
                    IOSDivider(startIndent = 66.dp)
                    IOSClickableItem(
                        icon = Icons.Filled.UploadFile,
                        title = "从文件导入",
                        subtitle = "预览可导入内容后再一键应用",
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                        iconTint = iOSPink
                    )
                }
            }

            item {
                IOSSectionTitle("文件格式")
                IOSGroup {
                    IOSClickableItem(
                        icon = Icons.Filled.Info,
                        title = "设置包（JSON）",
                        subtitle = "支持用户查看，也支持应用内一键导入",
                        value = "格式版本 v$SETTINGS_SHARE_SCHEMA_VERSION",
                        onClick = null,
                        iconTint = iOSOrange,
                        showChevron = false
                    )
                }
            }
        }

        iOSLargeTitleBar(
            title = screenTitle,
            scrollOffset = scrollOffset,
            leadingContent = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = backLabel
                    )
                }
            },
            hazeState = hazeState
        )

        if (uiState.isBusy) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }

    val pendingImportSession = uiState.pendingImportSession
    if (pendingImportSession != null) {
        var showRawKeys by remember(pendingImportSession) { mutableStateOf(false) }
        IOSAlertDialog(
            onDismissRequest = { viewModel.dismissImportPreview() },
            title = {
                Text(
                    text = "导入设置",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = buildImportPreviewSummary(pendingImportSession),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (showRawKeys && pendingImportSession.preview.skippedKeys.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = pendingImportSession.preview.skippedKeys.joinToString(separator = "\n"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                IOSDialogAction(onClick = { viewModel.confirmImport() }) {
                    Text(importConfirmLabel)
                }
            },
            dismissButton = {
                IOSDialogAction(
                    onClick = {
                        if (pendingImportSession.preview.skippedKeys.isNotEmpty() && !showRawKeys) {
                            showRawKeys = true
                        } else {
                            viewModel.dismissImportPreview()
                        }
                    }
                ) {
                    Text(
                        if (pendingImportSession.preview.skippedKeys.isNotEmpty() && !showRawKeys) {
                            viewSkippedLabel
                        } else {
                            cancelLabel
                        }
                    )
                }
            }
        )
    }
}

private fun buildImportPreviewSummary(session: SettingsShareImportSession): String {
    val sectionSummary = session.preview.importableSections
        .joinToString(separator = " / ") { it.label }
        .ifBlank { "无可导入分类" }
    val skippedCount = session.preview.skippedKeys.size
    val skippedSummary = if (skippedCount > 0) {
        "将跳过 $skippedCount 项本机专属或未知配置"
    } else {
        "没有需要跳过的项目"
    }
    return buildString {
        appendLine("配置名：${session.profile.profileName}")
        appendLine("来源版本：${session.profile.appVersion}")
        appendLine("导出时间：${session.profile.exportedAtIso}")
        appendLine("可导入分类：$sectionSummary")
        append(skippedSummary)
    }.trim()
}
