package com.android.purebilibili.feature.plugin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.android.purebilibili.core.plugin.Plugin
import com.android.purebilibili.core.plugin.feed.SubscriptionFeedStore
import com.android.purebilibili.core.plugin.feed.resolveSubscriptionTitle
import com.android.purebilibili.core.plugin.feed.resolveImportedSubscriptionTitles
import com.android.purebilibili.core.ui.AppAlertDialog
import com.android.purebilibili.core.ui.AppDialogAction
import com.android.purebilibili.core.ui.components.AppButton
import com.android.purebilibili.core.ui.components.AppCheckbox
import com.android.purebilibili.core.ui.components.AppOutlinedTextField
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppTextButton
import com.android.purebilibili.plugin.sdk.PluginCapability
import com.android.purebilibili.plugin.sdk.PluginCapabilityManifest

class SubscriptionFeedPlugin : Plugin {
    override val id: String = PLUGIN_ID
    override val name: String = "订阅"
    override val description: String = "关注喜欢的网站，在首页集中阅读更新；支持 RSS、Atom 和 OPML 导入。"
    override val version: String = "1.0.0"
    override val author: String = "BiliPai"
    override val capabilityManifest: PluginCapabilityManifest = PluginCapabilityManifest(
        pluginId = PLUGIN_ID,
        displayName = name,
        version = version,
        apiVersion = 1,
        entryClassName = SubscriptionFeedPlugin::class.java.name,
        capabilities = setOf(
            PluginCapability.FEED_SOURCE,
            PluginCapability.NETWORK,
            PluginCapability.PLUGIN_STORAGE,
        ),
    )

    @Composable
    override fun SettingsContent() {
        SubscriptionFeedSettings(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
    }

    @Composable
    override fun SettingsContent(modifier: Modifier) {
        SubscriptionFeedSettings(modifier)
    }

    companion object {
        const val PLUGIN_ID = "subscription_feed"
    }
}

@Composable
private fun SubscriptionFeedSettings(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val revision by SubscriptionFeedStore.revision.collectAsStateWithLifecycle()
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var importText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    var selecting by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var confirmBatchDelete by remember { mutableStateOf(false) }
    val feeds = remember(revision) { SubscriptionFeedStore.list(context) }
    LaunchedEffect(feeds) {
        selectedIds = selectedIds.intersect(feeds.map { it.id }.toSet())
        if (feeds.isEmpty()) selecting = false
    }
    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            importing = true
            try {
                val text = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            decodeSubscriptionFile(input.readBytes())
                        }.orEmpty()
                    }.getOrElse { "" }
                }
                applySubscriptionImport(context, text) { message, _ ->
                    error = message
                }
            } catch (failure: Exception) {
                if (failure is kotlinx.coroutines.CancellationException) throw failure
                error = "导入失败，请检查文件内容"
            } finally {
                importing = false
            }
        }
    }
    Column(
        modifier = modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppOutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { AppText("订阅地址") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        AppOutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { AppText("名称（可选，留空自动获取）") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        AppButton(
            onClick = {
                adding = true
                scope.launch {
                    try {
                        val result = withContext(Dispatchers.IO) {
                            resolveSubscriptionTitle(url, title).mapCatching { resolvedTitle ->
                                SubscriptionFeedStore.add(context, resolvedTitle, url).getOrThrow()
                            }
                        }
                        result.onSuccess {
                            title = ""
                            url = ""
                            error = null
                        }.onFailure { error = it.message }
                    } catch (failure: Exception) {
                        if (failure is kotlinx.coroutines.CancellationException) throw failure
                        error = "添加失败，请稍后重试"
                    } finally {
                        adding = false
                    }
                }
            },
            enabled = !adding,
            modifier = Modifier.align(Alignment.End),
        ) {
            AppText(if (adding) "正在获取名称" else "添加")
        }
        AppOutlinedTextField(
            value = importText,
            onValueChange = { importText = it },
            label = { AppText("批量导入：OPML、地址列表或 RSS 表格") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
            AppTextButton(
                onClick = { importLauncher.launch(arrayOf("*/*")) },
                enabled = !importing,
            ) {
                AppText("从文件导入")
            }
            AppButton(
                onClick = {
                    importing = true
                    scope.launch {
                        try {
                            val resolved = withContext(Dispatchers.IO) {
                                resolveImportPayload(importText)
                            }
                            applySubscriptionImport(context, resolved) { message, added ->
                                error = message
                                if (added) {
                                    importText = ""
                                }
                            }
                        } catch (failure: Exception) {
                            if (failure is kotlinx.coroutines.CancellationException) throw failure
                            error = "导入失败，请检查文件或地址"
                        } finally {
                            importing = false
                        }
                    }
                },
                enabled = !importing,
            ) {
                AppText(if (importing) "导入中" else "一键导入")
            }
        }
        error?.let { AppText(it) }
        if (feeds.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                AppText(
                    if (selecting) "已选 ${selectedIds.size} / ${feeds.size} 个" else "已添加 ${feeds.size} 个",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AppTextButton(onClick = {
                    selecting = !selecting
                    selectedIds = emptySet()
                }) { AppText(if (selecting) "取消" else "批量管理") }
            }
            if (selecting) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppTextButton(onClick = {
                        selectedIds = if (selectedIds.size == feeds.size) {
                            emptySet()
                        } else {
                            feeds.map { it.id }.toSet()
                        }
                    }) { AppText(if (selectedIds.size == feeds.size) "取消全选" else "全选") }
                    AppButton(
                        onClick = { confirmBatchDelete = true },
                        enabled = selectedIds.isNotEmpty(),
                    ) { AppText("删除所选（${selectedIds.size}）") }
                }
            }
        }
        feeds.forEach { feed ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (selecting) Modifier.toggleable(
                            value = feed.id in selectedIds,
                            role = Role.Checkbox,
                            onValueChange = { checked ->
                                selectedIds = if (checked) selectedIds + feed.id else selectedIds - feed.id
                            },
                        ) else Modifier
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (selecting) {
                    AppCheckbox(checked = feed.id in selectedIds, onCheckedChange = null)
                }
                Column(modifier = Modifier.weight(1f)) {
                    AppText(
                        text = feed.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    AppText(
                        text = feed.url,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (!selecting) {
                    AppTextButton(onClick = {
                        SubscriptionFeedStore.remove(context, feed.id)
                    }) {
                        AppText("删除")
                    }
                }
            }
        }
    }
    if (confirmBatchDelete) {
        AppAlertDialog(
            onDismissRequest = { confirmBatchDelete = false },
            title = { AppText("删除所选订阅？") },
            text = { AppText("将删除 ${selectedIds.size} 个订阅来源。") },
            confirmButton = {
                AppDialogAction(onClick = {
                    SubscriptionFeedStore.removeAll(context, selectedIds)
                    selectedIds = emptySet()
                    selecting = false
                    confirmBatchDelete = false
                }) { AppText("删除") }
            },
            dismissButton = {
                AppDialogAction(onClick = { confirmBatchDelete = false }) { AppText("取消") }
            },
        )
    }
}

private fun decodeSubscriptionFile(bytes: ByteArray): String {
    if (bytes.size >= 3 &&
        bytes[0] == 0xEF.toByte() &&
        bytes[1] == 0xBB.toByte() &&
        bytes[2] == 0xBF.toByte()
    ) {
        return bytes.copyOfRange(3, bytes.size).toString(Charsets.UTF_8)
    }
    if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
        return bytes.toString(Charsets.UTF_16LE)
    }
    if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
        return bytes.toString(Charsets.UTF_16BE)
    }
    return bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
}

private suspend fun resolveImportPayload(raw: String): String {
    val text = raw.trim()
    if (!text.contains('\n') && com.android.purebilibili.core.plugin.feed.isHttpFeedUrl(text)) {
        val body = com.android.purebilibili.core.plugin.feed.fetchFeedXml(text).getOrNull()
        if (body != null && (body.contains("<opml", ignoreCase = true) || body.contains("<outline", ignoreCase = true))) {
            return body
        }
    }
    return text
}

private suspend fun applySubscriptionImport(
    context: android.content.Context,
    text: String,
    onResult: (String?, Boolean) -> Unit,
) {
    val imported = withContext(Dispatchers.IO) {
        com.android.purebilibili.core.plugin.feed.parseSubscriptionImport(text)
    }
    if (imported.isEmpty()) {
        onResult("没有解析到订阅地址", false)
        return
    }
    val resolved = resolveImportedSubscriptionTitles(imported)
    if (resolved.isEmpty()) {
        onResult("没有可识别的 RSS 或 Atom 地址", false)
        return
    }
    val added = withContext(Dispatchers.IO) { SubscriptionFeedStore.addAll(context, resolved) }
    val skipped = imported.size - resolved.size
    onResult(
        when {
            added == 0 && skipped > 0 -> "没有新增订阅：${resolved.size} 个已存在，$skipped 个地址无法识别"
            added == 0 -> "这 ${resolved.size} 个地址都已经在列表里"
            skipped > 0 -> "已导入 $added 个订阅，跳过 $skipped 个无法识别的地址"
            else -> "已导入 $added 个订阅"
        },
        added > 0,
    )
}
