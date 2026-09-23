package com.android.purebilibili.feature.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.ui.AppAlertDialog
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppListItem
import com.android.purebilibili.core.ui.components.AppSwitch
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppTextButton
import com.android.purebilibili.core.util.CrashReporter
import kotlinx.coroutines.launch

/**
 * First-launch crash-tracking consent.
 * Uses native [AppAlertDialog] / switch / list item — no custom pink chrome.
 */
@Composable
fun CrashTrackingConsentDialog(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isEnabled by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    fun saveAndDismiss() {
        if (isSaving) return
        isSaving = true
        scope.launch {
            try {
                SettingsManager.setCrashTrackingEnabled(context, isEnabled)
                SettingsManager.setCrashTrackingConsentShown(context, true)
                CrashReporter.setEnabled(isEnabled)
                onDismiss()
            } finally {
                isSaving = false
            }
        }
    }

    AppAlertDialog(
        onDismissRequest = { /* require explicit confirm */ },
        properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = false,
        ),
        icon = {
            AppIcon(
                imageVector = Icons.Outlined.BugReport,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
        },
        title = {
            AppText(
                text = "帮助改进应用",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                AppText(
                    text = "为快速发现并修复问题，应用可收集崩溃报告与错误日志。\n\n" +
                        "默认仅启用崩溃追踪；使用情况统计默认关闭。播放器诊断日志仍可手动开启，便于排查黑屏、卡顿等播放问题。\n\n" +
                        "数据仅用于改善稳定性，之后可随时在「设置」中调整。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                AppListItem(
                    headlineContent = {
                        AppText(
                            text = "启用崩溃追踪",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    },
                    supportingContent = {
                        AppText(
                            text = if (isEnabled) "开启后将在崩溃时上传诊断信息" else "关闭后不上传崩溃报告",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingContent = {
                        AppSwitch(
                            checked = isEnabled,
                            onCheckedChange = { isEnabled = it },
                            enabled = !isSaving,
                        )
                    },
                )
            }
        },
        confirmButton = {
            AppTextButton(
                onClick = { saveAndDismiss() },
                enabled = !isSaving,
            ) {
                AppText("确定")
            }
        },
    )
}
