package com.android.purebilibili.feature.plugin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.android.purebilibili.core.plugin.Plugin
import com.android.purebilibili.core.plugin.feed.SubscriptionFeedStore
import com.android.purebilibili.core.ui.components.AppButton
import com.android.purebilibili.core.ui.components.AppOutlinedTextField
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppTextButton
import com.android.purebilibili.plugin.sdk.PluginCapability
import com.android.purebilibili.plugin.sdk.PluginCapabilityManifest

class SubscriptionFeedPlugin : Plugin {
    override val id: String = PLUGIN_ID
    override val name: String = "订阅"
    override val description: String = "添加 RSS 或 Atom 地址。启用并保存后，首页会出现「订阅」标签。"
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
        SubscriptionFeedSettings(Modifier.fillMaxWidth())
    }

    @Composable
    override fun SettingsContent(modifier: Modifier) {
        SubscriptionFeedSettings(modifier)
    }

    companion object {
        const val PLUGIN_ID = "subscription_feed"
    }
}

private const val SUBSCRIPTION_DISCLAIMER =
    "免责声明：订阅内容来自你自行添加的第三方地址，由本应用在本地请求和排版。应用不托管、不审核这些内容。源站打不开、超时、摘要不全或正文缺失，都由源站决定。请只导入你信任的地址。"

@Composable
private fun SubscriptionFeedSettings(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var revision by remember { mutableIntStateOf(0) }
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var importText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var importing by remember { mutableStateOf(false) }
    val feeds = remember(revision) { SubscriptionFeedStore.list(context) }
    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    decodeSubscriptionFile(input.readBytes())
                }.orEmpty()
            }.getOrElse { "" }
            withContext(Dispatchers.Main) {
                applySubscriptionImport(context, text) { message, added ->
                    error = message
                    if (added) revision += 1
                }
            }
        }
    }
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppText(SUBSCRIPTION_DISCLAIMER)
        AppText("添加单个 http 或 https 地址，或一次导入 OPML 和每行一个地址的列表。首页标签设置里可以隐藏「订阅」。")
        AppOutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { AppText("名称") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        AppOutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { AppText("订阅地址") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        AppButton(
            onClick = {
                SubscriptionFeedStore.add(context, title, url)
                    .onSuccess {
                        title = ""
                        url = ""
                        error = null
                        revision += 1
                    }
                    .onFailure { error = it.message }
            },
            modifier = Modifier.align(Alignment.End),
        ) {
            AppText("添加")
        }
        AppOutlinedTextField(
            value = importText,
            onValueChange = { importText = it },
            label = { AppText("OPML 或地址列表") },
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
                        val resolved = withContext(Dispatchers.IO) {
                            resolveImportPayload(importText)
                        }
                        applySubscriptionImport(context, resolved) { message, added ->
                            error = message
                            if (added) {
                                importText = ""
                                revision += 1
                            }
                        }
                        importing = false
                    }
                },
                enabled = !importing,
            ) {
                AppText(if (importing) "导入中" else "一键导入")
            }
        }
        error?.let { AppText(it) }
        if (feeds.isNotEmpty()) {
            AppText("已添加 ${feeds.size} 个", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        feeds.forEach { feed ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
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
                AppTextButton(onClick = {
                    SubscriptionFeedStore.remove(context, feed.id)
                    revision += 1
                }) {
                    AppText("删除")
                }
            }
        }
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

private fun applySubscriptionImport(
    context: android.content.Context,
    text: String,
    onResult: (String?, Boolean) -> Unit,
) {
    val imported = com.android.purebilibili.core.plugin.feed.parseSubscriptionImport(text)
    if (imported.isEmpty()) {
        onResult("没有解析到订阅地址", false)
        return
    }
    val added = SubscriptionFeedStore.addAll(context, imported)
    onResult(
        if (added == 0) "这 ${imported.size} 个地址都已经在列表里" else "已导入 $added 个订阅，共解析 ${imported.size} 个",
        added > 0,
    )
}
