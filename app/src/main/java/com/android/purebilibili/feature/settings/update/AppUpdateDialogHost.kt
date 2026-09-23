package com.android.purebilibili.feature.settings

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import kotlinx.coroutines.delay
import java.io.File

internal data class AppUpdateChannelUrls(
    val stableReleaseUrl: String,
    val prereleaseReleasesUrl: String,
)

internal fun appUpdateChannelUrls(releasesPageUrl: String): AppUpdateChannelUrls {
    val releasesBaseUrl = releasesPageUrl.trimEnd('/')
    return AppUpdateChannelUrls(
        stableReleaseUrl = "$releasesBaseUrl/latest",
        prereleaseReleasesUrl = releasesBaseUrl,
    )
}

/** Owns update state and delegates rendering to the active theme's native dialog. */
@Composable
internal fun AppUpdateDialogHost(
    update: AppUpdateCheckResult,
    showReleaseNotesOnly: Boolean = false,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val preferredAsset = remember(update.assets) { selectPreferredAppUpdateAsset(update.assets) }
    val channelUrls = remember {
        val focusEndpoint = AppUpdateChecker.resolveEndpointCandidates().first()
        appUpdateChannelUrls(focusEndpoint.releasesPageUrl)
    }
    val expectedSha256 = remember(preferredAsset, update.buildMetadata) {
        preferredAsset?.let { resolveAppUpdateExpectedSha256(it, update.buildMetadata) }
    }
    var downloadState by remember(update.latestVersion) { mutableStateOf(AppUpdateDownloadState()) }

    LaunchedEffect(update.latestVersion, expectedSha256) {
        while (true) {
            downloadState = AppUpdateDownloadCoordinator.readState(
                context = context,
                releaseVersion = update.latestVersion,
                checksumProvided = expectedSha256 != null,
            )
            delay(500)
        }
    }

    val onPrimaryAction: () -> Unit = {
        val completedFile = downloadState.filePath
            ?.takeIf { downloadState.status == AppUpdateDownloadStatus.COMPLETED }
            ?.let(::File)
            ?.takeIf(File::exists)
        when {
            completedFile != null -> {
                val action = installDownloadedAppUpdate(context, completedFile)
                if (action == AppUpdateInstallAction.OPEN_UNKNOWN_SOURCES_SETTINGS) {
                    Toast.makeText(context, "请先允许安装未知来源应用", Toast.LENGTH_SHORT).show()
                }
            }
            preferredAsset != null -> AppUpdateDownloadCoordinator.enqueue(
                context = context,
                asset = preferredAsset,
                releaseVersion = update.latestVersion,
                expectedSha256 = expectedSha256,
            )
            else -> uriHandler.openUri(update.releaseUrl)
        }
        Unit
    }
    val onCancelDownload: () -> Unit = {
        preferredAsset?.let { AppUpdateDownloadCoordinator.cancel(context, it.name) }
        Unit
    }
    val dialogState = AppUpdateDialogState(
        update = update,
        downloadState = downloadState,
        preferredAssetName = preferredAsset?.name,
        hasAsset = preferredAsset != null,
        showReleaseNotesOnly = showReleaseNotesOnly,
    )
    val actions = AppUpdateDialogActions(
        onPrimaryAction = onPrimaryAction,
        onCancelDownload = onCancelDownload,
        onOpenRelease = { uriHandler.openUri(channelUrls.stableReleaseUrl) },
        onOpenTestRelease = { uriHandler.openUri(channelUrls.prereleaseReleasesUrl) },
        onDismissRequest = onDismissRequest,
    )

    when (LocalAppUiStyle.current) {
        AppUiStyle.MATERIAL3 -> Material3AppUpdateDialog(state = dialogState, actions = actions)
        AppUiStyle.MIUIX -> MiuixAppUpdateDialog(state = dialogState, actions = actions)
    }
}

internal data class AppUpdateDialogState(
    val update: AppUpdateCheckResult,
    val downloadState: AppUpdateDownloadState,
    val preferredAssetName: String?,
    val hasAsset: Boolean,
    val showReleaseNotesOnly: Boolean,
)

internal data class AppUpdateDialogActions(
    val onPrimaryAction: () -> Unit,
    val onCancelDownload: () -> Unit,
    val onOpenRelease: () -> Unit,
    val onOpenTestRelease: () -> Unit,
    val onDismissRequest: () -> Unit,
)

internal fun appUpdatePrimaryLabel(state: AppUpdateDownloadState, hasAsset: Boolean): String = when {
    state.status == AppUpdateDownloadStatus.COMPLETED -> "安装更新"
    state.status == AppUpdateDownloadStatus.QUEUED -> "等待下载"
    state.status == AppUpdateDownloadStatus.DOWNLOADING -> "下载中"
    !hasAsset -> "前往下载"
    else -> "下载更新"
}

internal fun appUpdateStatusText(state: AppUpdateDownloadState): String = when (state.status) {
    AppUpdateDownloadStatus.QUEUED -> "等待网络后开始下载"
    AppUpdateDownloadStatus.DOWNLOADING -> "正在下载 · ${(state.progress * 100).toInt()}%"
    AppUpdateDownloadStatus.COMPLETED -> "下载完成，可以安装"
    AppUpdateDownloadStatus.FAILED -> state.errorMessage ?: "下载失败，请稍后重试"
    AppUpdateDownloadStatus.IDLE -> ""
}

internal fun AppUpdateDownloadState.isActiveDownload(): Boolean {
    return status == AppUpdateDownloadStatus.DOWNLOADING || status == AppUpdateDownloadStatus.QUEUED
}
