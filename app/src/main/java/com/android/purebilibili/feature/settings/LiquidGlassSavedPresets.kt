package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.ui.AppContentColors

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppAlertDialog
import com.android.purebilibili.core.ui.AppDialogAction
import com.android.purebilibili.core.ui.components.AppOutlinedTextField
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppTextButton
import com.android.purebilibili.feature.settings.share.SavedSettingsProfile
import com.android.purebilibili.feature.settings.share.SettingsShareService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun LiquidGlassSavedPresets(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val service = remember(context.applicationContext) { SettingsShareService(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var profiles by remember { mutableStateOf<List<SavedSettingsProfile>>(emptyList()) }
    var busy by remember { mutableStateOf(true) }
    var showSave by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<SavedSettingsProfile?>(null) }

    fun notify(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    // Serialize button operations and always release the busy state on cancellation.
    fun runOperation(operation: suspend () -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                operation()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                notify(error.message ?: "预设操作失败")
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(service) {
        try {
            profiles = service.listLiquidGlassProfiles()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            notify(error.message ?: "读取预设失败")
        } finally {
            busy = false
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            AppText(
                text = "我的液态玻璃预设",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f).padding(top = 12.dp),
            )
            AppTextButton(enabled = !busy, onClick = {
                name = ""
                showSave = true
            }) {
                AppText("保存当前")
            }
        }
        AppText(
            text = "保存当前调节参数，点击名称一键恢复；分享的 JSON 可通过下方“导入设置”恢复。",
            style = MaterialTheme.typography.bodySmall,
            color = AppContentColors.secondaryText(),
        )
        if (profiles.isEmpty()) {
            AppText(
                text = if (busy) "正在读取预设…" else "还没有保存的预设",
                style = MaterialTheme.typography.bodySmall,
                color = AppContentColors.secondaryText(),
            )
        }
        profiles.forEach { profile ->
            Column(modifier = Modifier.fillMaxWidth()) {
                AppTextButton(
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        runOperation {
                            service.restoreLiquidGlassProfile(profile)
                            notify("已恢复：${profile.name}")
                        }
                    },
                ) { AppText(profile.name) }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    AppTextButton(enabled = !busy, onClick = {
                        runOperation {
                            val uri = service.shareLiquidGlassProfile(profile)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_TEXT, "BiliPai 液态玻璃预设：${profile.name}")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "分享液态玻璃预设"))
                        }
                    }) { AppText("分享") }
                    AppTextButton(enabled = !busy, onClick = { deleteTarget = profile }) {
                        AppText("删除")
                    }
                }
            }
        }
    }

    if (showSave) {
        AppAlertDialog(
            onDismissRequest = { if (!busy) showSave = false },
            title = { AppText("保存液态玻璃预设") },
            text = {
                AppOutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(80) },
                    labelText = "预设名称",
                    placeholderText = "例如：清晰通透",
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                AppTextButton(enabled = !busy && name.isNotBlank(), onClick = {
                    val presetName = name.trim()
                    runOperation {
                        service.saveLiquidGlassProfile(presetName)
                        profiles = service.listLiquidGlassProfiles()
                        showSave = false
                        notify("预设已保存")
                    }
                }) { AppText(if (busy) "正在保存" else "保存") }
            },
            dismissButton = {
                AppTextButton(enabled = !busy, onClick = { showSave = false }) { AppText("取消") }
            },
        )
    }

    deleteTarget?.let { profile ->
        AppAlertDialog(
            onDismissRequest = { if (!busy) deleteTarget = null },
            title = { AppText("删除预设？") },
            text = { AppText("删除“${profile.name}”后，当前已应用的效果仍会保留。") },
            confirmButton = {
                AppDialogAction(onClick = {
                    runOperation {
                        service.deleteLiquidGlassProfile(profile)
                        profiles = service.listLiquidGlassProfiles()
                        deleteTarget = null
                    }
                }) { AppText("删除") }
            },
            dismissButton = {
                AppTextButton(enabled = !busy, onClick = { deleteTarget = null }) { AppText("取消") }
            },
        )
    }
}
