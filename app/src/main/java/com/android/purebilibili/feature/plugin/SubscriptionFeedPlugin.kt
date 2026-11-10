package com.android.purebilibili.feature.plugin

import com.android.purebilibili.core.plugin.BuiltinPluginIds

import com.android.purebilibili.core.ui.AppContentColors

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
import com.android.purebilibili.core.plugin.BuiltinPlugin
import com.android.purebilibili.core.plugin.feed.FeedConditionalStore
import com.android.purebilibili.core.plugin.feed.SavedSubscriptionFeed
import com.android.purebilibili.core.plugin.feed.SubscriptionFeedStore
import com.android.purebilibili.core.plugin.feed.buildSubscriptionOpml
import com.android.purebilibili.core.plugin.feed.resolveSubscriptionTitle
import com.android.purebilibili.core.ui.AppAlertDialog
import com.android.purebilibili.core.ui.AppDialogAction
import com.android.purebilibili.core.ui.components.AppButton
import com.android.purebilibili.core.ui.components.AppCheckbox
import com.android.purebilibili.core.ui.components.AppOutlinedTextField
import com.android.purebilibili.core.ui.components.AppSwitch
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppTextButton
import com.android.purebilibili.plugin.sdk.PluginCapability
import com.android.purebilibili.plugin.sdk.PluginCapabilityManifest

class SubscriptionFeedPlugin : BuiltinPlugin {
    override val id: String = BuiltinPluginIds.SUBSCRIPTION_FEED
    override val name: String = "订阅"
    override val description: String = "关注喜欢的网站，在首页集中阅读更新；支持 RSS、Atom 和 OPML 导入。"
    override val version: String = "1.0.0"
    override val author: String = "BiliPai"
    override val capabilityManifest: PluginCapabilityManifest = PluginCapabilityManifest(
        pluginId = BuiltinPluginIds.SUBSCRIPTION_FEED,
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
    var editingGroupFeed by remember { mutableStateOf<SavedSubscriptionFeed?>(null) }
    var feeds by remember { mutableStateOf<List<SavedSubscriptionFeed>>(emptyList()) }
    LaunchedEffect(revision) {
        feeds = SubscriptionFeedStore.list(context)
    }
    LaunchedEffect(feeds) {
        selectedIds = selectedIds.intersect(feeds.map { it.id }.toSet())
        if (feeds.isEmpty()) selecting = false
    }
    // 兜底：进入设置页时补全历史遗留的占位标题（上次后台补全未完成的条目）。
    LaunchedEffect(Unit) {
        com.android.purebilibili.core.coroutines.AppScope.ioScope.launch {
            com.android.purebilibili.core.plugin.feed.refreshMissingSubscriptionTitles(context)
        }
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
    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/xml"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val opml = buildSubscriptionOpml(SubscriptionFeedStore.listBlocking(context))
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(opml.toByteArray(Charsets.UTF_8))
                    } ?: error("无法写入所选位置")
                }
                error = null
            } catch (failure: Exception) {
                if (failure is kotlinx.coroutines.CancellationException) throw failure
                error = "导出失败，请重试"
            }
        }
    }
    Column(
        modifier = modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppText(
            text = "添加 RSS 或 Atom 地址，在首页集中阅读更新。可批量导入 OPML、地址列表或 RSS 表格。",
            color = AppContentColors.secondaryText(),
            style = MaterialTheme.typography.bodyMedium,
        )
        AppOutlinedTextField(
            value = url,
            onValueChange = { url = it },
            labelText = "订阅地址",
            placeholderText = "https://example.com/feed.xml",
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        AppOutlinedTextField(
            value = title,
            onValueChange = { title = it },
            labelText = "名称（可选，留空自动获取）",
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
            labelText = "批量导入",
            placeholderText = "粘贴 OPML、地址列表或 RSS 表格，每行一个地址",
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
            AppTextButton(
                onClick = { exportLauncher.launch("bilipai-subscriptions.opml") },
                enabled = feeds.isNotEmpty() && !importing,
            ) {
                AppText("导出 OPML")
            }
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
                    color = AppContentColors.secondaryText(),
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
                        color = if (feed.enabled) {
                            AppContentColors.primaryText()
                        } else {
                            AppContentColors.secondaryText()
                        },
                    )
                    AppText(
                        text = if (feed.group.isBlank()) feed.url else "${feed.group} · ${feed.url}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = AppContentColors.secondaryText(),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (!selecting) {
                    AppTextButton(onClick = { editingGroupFeed = feed }) {
                        AppText(if (feed.group.isBlank()) "分组" else feed.group)
                    }
                    AppSwitch(
                        checked = feed.enabled,
                        onCheckedChange = { checked ->
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    SubscriptionFeedStore.setEnabled(context, feed.id, checked)
                                }
                            }
                        },
                    )
                    AppTextButton(onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                runCatching { FeedConditionalStore.clear(context, setOf(feed.url)) }
                                SubscriptionFeedStore.remove(context, feed.id)
                            }
                        }
                    }) {
                        AppText("删除")
                    }
                }
            }
        }
    }
    // editingGroupFeed 是委托属性，智能转换不可用；orEmpty() 也不适用于对象类型。
    editingGroupFeed?.let { feed ->
        var groupInput by remember(feed.id) { mutableStateOf(feed.group) }
        AppAlertDialog(
            onDismissRequest = { editingGroupFeed = null },
            title = { AppText("设置分组") },
            text = {
                Column {
                    AppOutlinedTextField(
                        value = groupInput,
                        onValueChange = { groupInput = it },
                        labelText = "分组名称（留空表示未分组）",
                        placeholderText = "技术博客",
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                AppDialogAction(onClick = {
                    val target = feed
                    val group = groupInput
                    editingGroupFeed = null
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            SubscriptionFeedStore.setGroup(context, target.id, group)
                        }
                    }
                }) { AppText("保存") }
            },
            dismissButton = {
                AppDialogAction(onClick = { editingGroupFeed = null }) { AppText("取消") }
            },
        )
    }
    if (confirmBatchDelete) {
        AppAlertDialog(
            onDismissRequest = { confirmBatchDelete = false },
            title = { AppText("删除所选订阅？") },
            text = { AppText("将删除 ${selectedIds.size} 个订阅来源。") },
            confirmButton = {
                AppDialogAction(onClick = {
                    // 固定本次删除目标，避免下面清空选择后协程读取到空集合。
                    val idsToRemove = selectedIds.toSet()
                    val removedUrls = feeds.filter { it.id in idsToRemove }.map { it.url }.toSet()
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            runCatching { FeedConditionalStore.clear(context, removedUrls) }
                            SubscriptionFeedStore.removeAll(context, idsToRemove)
                        }
                    }
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
    // 导入本身零网络请求：条目即时入库，缺标题的用地址占位，后台再逐个补全。
    val added = withContext(Dispatchers.IO) { SubscriptionFeedStore.addAll(context, imported) }
    if (added == 0) {
        onResult("这 ${imported.size} 个地址都已经在列表里", false)
        return
    }
    val skipped = imported.size - added
    onResult(

        when {
            skipped > 0 -> "已导入 $added 个订阅，跳过 $skipped 个无效或重复地址；标题将自动补全"
            else -> "已导入 $added 个订阅；标题将自动补全"
        },
        true,
    )
    // 用应用级作用域：离开设置页也继续补全，多次导入由 resolver 内部互斥串行。
    com.android.purebilibili.core.coroutines.AppScope.ioScope.launch {
        com.android.purebilibili.core.plugin.feed.refreshMissingSubscriptionTitles(context)
    }
}
