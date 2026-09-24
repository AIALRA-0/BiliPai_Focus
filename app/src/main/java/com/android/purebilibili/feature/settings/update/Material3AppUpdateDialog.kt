package com.android.purebilibili.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.purebilibili.R
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.components.AppButton
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppLinearProgressIndicator
import com.android.purebilibili.core.ui.components.AppOutlinedButton
import com.android.purebilibili.core.ui.components.AppTextButton
import com.android.purebilibili.core.ui.AppAlertDialog
import com.android.purebilibili.core.ui.ContainerLevel

/** Material 3 renderer: native AlertDialog hierarchy and standard action buttons. */
@Composable
internal fun Material3AppUpdateDialog(
    state: AppUpdateDialogState,
    actions: AppUpdateDialogActions,
) {
    AppAlertDialog(
        onDismissRequest = actions.onDismissRequest,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = if (state.showReleaseNotesOnly) "更新日志" else "发现新版本",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "版本 ${state.update.latestVersion}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                AppIconButton(
                    onClick = actions.onDismissRequest,
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                ) {
                    Icon(
                        imageVector = rememberMaterialSymbol(R.drawable.ms_close_24),
                        contentDescription = "关闭",
                    )
                }
            }
        },
        text = {
            Material3UpdateContent(state = state, actions = actions)
        },
        confirmButton = {
            if (!state.showReleaseNotesOnly) {
                AppButton(
                    onClick = actions.onPrimaryAction,
                    enabled = !state.downloadState.isActiveDownload(),
                    colors = ButtonDefaults.buttonColors(),
                    elevation = ButtonDefaults.buttonElevation(),
                ) {
                    Text(
                        text = appUpdatePrimaryLabel(state.downloadState, state.hasAsset),
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        },
        dismissButton = if (state.showReleaseNotesOnly) {
            null
        } else {
            {
                AppTextButton(
                    onClick = if (state.downloadState.isActiveDownload()) {
                        actions.onCancelDownload
                    } else {
                        actions.onDismissRequest
                    },
                ) {
                    Text(
                        text = if (state.downloadState.isActiveDownload()) "取消下载" else "稍后",
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        },
        shape = AppShapes.container(ContainerLevel.Dialog),
    )
}

@Composable
private fun Material3UpdateContent(
    state: AppUpdateDialogState,
    actions: AppUpdateDialogActions,
    modifier: Modifier = Modifier,
) {
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val contentMaxHeightDp = remember(screenHeightDp) {
        resolveAppUpdateScrollableContentMaxHeight(
            screenHeightDp = screenHeightDp,
            heightFraction = 0.48f,
            minHeightDp = 180,
            maxHeightDp = 380,
        )
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = contentMaxHeightDp.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Material3VersionCard(state = state)
            if (!state.showReleaseNotesOnly && state.downloadState.status != AppUpdateDownloadStatus.IDLE) {
                Material3DownloadStatus(state = state.downloadState)
            }
            Material3ReleaseNotes(releaseNotes = state.update.releaseNotes)
        }
        Material3DownloadChannels(actions = actions)
    }
}

@Composable
private fun Material3VersionCard(
    state: AppUpdateDialogState,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.container(ContainerLevel.Card),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Material3VersionValue(
                    label = "当前版本",
                    value = state.update.currentVersion,
                    modifier = Modifier.weight(1f),
                )
                Material3VersionValue(
                    label = "更新至",
                    value = state.update.latestVersion,
                    emphasized = true,
                    modifier = Modifier.weight(1f),
                )
            }
            state.preferredAssetName?.takeUnless { state.showReleaseNotesOnly }?.let { assetName ->
                Text(
                    text = "安装包  $assetName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                )
            }
        }
    }
}

@Composable
private fun Material3VersionValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
        )
    }
}

@Composable
private fun Material3DownloadStatus(
    state: AppUpdateDownloadState,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.container(ContainerLevel.Card),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = appUpdateStatusText(state),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            if (state.isActiveDownload()) {
                if (state.totalBytes > 0L) {
                    AppLinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    AppLinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            if (state.status == AppUpdateDownloadStatus.COMPLETED) {
                Text(
                    text = if (state.checksumProvided) {
                        "SHA-256 完整性校验已通过"
                    } else {
                        "此 Release 未提供 SHA-256 校验信息"
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun Material3ReleaseNotes(
    releaseNotes: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "本次更新",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        parseUpdateReleaseNotes(releaseNotes).forEach { block ->
            when (block) {
                is AppUpdateReleaseNotesBlock.Heading -> Text(
                    text = block.text,
                    style = if (block.level == 1) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.titleSmall
                    },
                    fontWeight = FontWeight.SemiBold,
                )
                is AppUpdateReleaseNotesBlock.Bullet -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = "•", color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = block.text,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AppUpdateReleaseNotesBlock.Divider -> androidx.compose.material3.HorizontalDivider()
                is AppUpdateReleaseNotesBlock.Paragraph -> Text(
                    text = block.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Material3DownloadChannels(
    actions: AppUpdateDialogActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "其他下载渠道",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .widthIn(max = 320.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppOutlinedButton(
                onClick = actions.onOpenRelease,
                modifier = Modifier
                    .weight(1f)
                    .sizeIn(minHeight = 48.dp),
            ) {
                Text("正式版下载", maxLines = 1, softWrap = false)
            }
            AppOutlinedButton(
                onClick = actions.onOpenTestRelease,
                modifier = Modifier
                    .weight(1f)
                    .sizeIn(minHeight = 48.dp),
            ) {
                Text("测试版下载", maxLines = 1, softWrap = false)
            }
        }
    }
}
