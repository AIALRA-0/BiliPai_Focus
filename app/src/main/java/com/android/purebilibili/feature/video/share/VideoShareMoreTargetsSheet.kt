package com.android.purebilibili.feature.video.share

import com.android.purebilibili.core.ui.AppContentColors

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import com.android.purebilibili.core.ui.components.AppTextButton
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.android.purebilibili.core.ui.components.AppLazyColumn as LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.android.purebilibili.core.ui.AppModalBottomSheet
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.components.AppButton
import com.android.purebilibili.core.ui.components.AppListItem
import com.android.purebilibili.core.ui.components.AppText
import kotlinx.coroutines.launch

internal data class VideoShareAppTarget(
    val packageName: String,
    val activityClassName: String,
    val label: String,
    val icon: Drawable,
)

internal fun findVideoShareAppTargets(context: Context, mimeType: String): List<VideoShareAppTarget> =
    runCatching {
        val manager = context.packageManager
        val query = Intent(Intent.ACTION_SEND).apply { type = mimeType }
        val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            manager.queryIntentActivities(
                query,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            manager.queryIntentActivities(query, PackageManager.MATCH_DEFAULT_ONLY)
        }
        activities.asSequence()
            .filter { it.activityInfo != null && it.activityInfo.packageName != context.packageName }
            .groupBy { it.activityInfo.packageName }
            .mapNotNull { (packageName, candidates) ->
                val preferred = resolvePreferredShareActivity(candidates.map { candidate ->
                    ShareActivityCandidate(
                        packageName = packageName,
                        className = candidate.activityInfo.name,
                        label = candidate.loadLabel(manager).toString(),
                    )
                }) ?: return@mapNotNull null
                val resolved = candidates.firstOrNull { it.activityInfo.name == preferred.className }
                    ?: return@mapNotNull null
                VideoShareAppTarget(
                    packageName = packageName,
                    activityClassName = preferred.className,
                    label = resolved.loadLabel(manager).toString(),
                    icon = resolved.loadIcon(manager),
                )
            }
            .sortedBy { it.label.lowercase() }
    }.getOrDefault(emptyList())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VideoShareMoreTargetsSheet(
    shareMedia: VideoShareCoverFile?,
    onDismiss: () -> Unit,
    onTargetClick: (VideoShareAppTarget) -> Unit,
    onSystemChooserClick: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isLandscape = isLandscapeVideoShare()
    val maxListHeight = (LocalConfiguration.current.screenHeightDp - 220).coerceIn(160, 440).dp
    val mimeType = shareMedia?.mimeType ?: "text/plain"
    val targets = remember(context, mimeType) { findVideoShareAppTargets(context, mimeType) }
    var openingTarget by remember { mutableStateOf(false) }

    AppModalBottomSheet(
        onDismissRequest = { if (!openingTarget) onDismiss() },
        sheetState = sheetState,
        presentationOverride = videoSharePresentation(isLandscape),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isLandscape) Modifier.fillMaxHeight() else Modifier)
                .navigationBarsPadding()
                .padding(
                    horizontal = AppSpacingTokens.Large,
                    vertical = AppSpacingTokens.Small,
                ),
            verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
        ) {
            if (isLandscape) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppText("更多分享方式", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    AppTextButton(onClick = onDismiss, enabled = !openingTarget) { AppText("关闭") }
                }
            } else {
                AppText("更多分享方式", style = MaterialTheme.typography.headlineSmall)
                AppText("选择应用继续分享", style = MaterialTheme.typography.bodySmall)
            }
            LazyColumn(modifier = Modifier.fillMaxWidth().then(
                if (isLandscape) Modifier.weight(1f) else Modifier.heightIn(max = maxListHeight),
            )) {
                items(targets, key = { it.packageName }) { target ->
                    AppListItem(
                        headlineContent = { AppText(target.label) },
                        leadingContent = {
                            AndroidView(
                                factory = { viewContext -> ImageView(viewContext).apply {
                                    scaleType = ImageView.ScaleType.FIT_CENTER
                                } },
                                update = { it.setImageDrawable(target.icon) },
                                modifier = Modifier.size(48.dp),
                            )
                        },
                        modifier = Modifier.fillMaxWidth().clickable(enabled = !openingTarget) {
                            openingTarget = true
                            scope.launch {
                                hideVideoShareSheet(sheetState)
                                onTargetClick(target)
                            }
                        },
                    )
                }
                if (targets.isEmpty()) item { AppText("没有找到可接收此内容的应用") }
            }
            AppButton(
                onClick = {
                    openingTarget = true
                    scope.launch {
                        hideVideoShareSheet(sheetState)
                        onSystemChooserClick()
                    }
                },
                enabled = !openingTarget,
                modifier = Modifier.fillMaxWidth(),
            ) { AppText("系统分享面板") }
            if (!isLandscape) {
                AppText(
                    "系统面板的屏幕方向由设备决定",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppContentColors.secondaryText(),
                )
                AppButton(onClick = onDismiss, enabled = !openingTarget, modifier = Modifier.fillMaxWidth()) {
                    AppText("取消")
                }
            }
        }
    }
}
