// 文件路径: feature/settings/PluginsScreen.kt
package com.android.purebilibili.feature.settings
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppText

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.android.purebilibili.R
import com.android.purebilibili.core.plugin.ExternalPluginInstallDecision
import com.android.purebilibili.core.plugin.evaluateExternalPluginInstall
import com.android.purebilibili.core.plugin.js.BiliPaiJsPluginInstallStore
import com.android.purebilibili.core.plugin.js.BiliPaiJsPluginManifest
import com.android.purebilibili.core.plugin.js.BiliPaiJsRuntime
import com.android.purebilibili.core.plugin.js.InstalledBiliPaiJsPlugin
import com.android.purebilibili.core.plugin.js.resolveBiliPaiJsPluginCapabilities
import com.android.purebilibili.core.plugin.kotlinpkg.ExternalKotlinPluginInstallStore
import com.android.purebilibili.core.plugin.kotlinpkg.ExternalKotlinPluginPackagePreview
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.plugin.skin.UiSkinImportPackageResolver
import com.android.purebilibili.core.plugin.skin.UiSkinImportMode
import com.android.purebilibili.core.plugin.skin.InstalledUiSkinPackage
import com.android.purebilibili.core.plugin.skin.UiSkinInstallStore
import com.android.purebilibili.core.plugin.skin.UiSkinPackagePreview
import com.android.purebilibili.core.plugin.skin.UiSkinSelection
import com.android.purebilibili.core.plugin.skin.UiSkinSettingsStore
import com.android.purebilibili.core.plugin.skin.rememberUiSkinState
import com.android.purebilibili.core.plugin.PluginInfo
import com.android.purebilibili.core.plugin.PluginManager
import com.android.purebilibili.core.plugin.PluginStore
import com.android.purebilibili.core.plugin.json.JsonPluginStatsNotificationConfig
import com.android.purebilibili.core.plugin.json.persistJsonPluginStatsNotificationConfig
import com.android.purebilibili.core.plugin.json.postJsonPluginStatsTestNotification
import com.android.purebilibili.core.plugin.json.readJsonPluginStatsNotificationConfig
import com.android.purebilibili.core.plugin.json.scheduleJsonPluginStatsSummary
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.core.theme.iOSPink  // 插件图标色
import com.android.purebilibili.core.theme.iOSBlue
import com.android.purebilibili.core.theme.iOSGreen
import com.android.purebilibili.core.theme.iOSOrange
import com.android.purebilibili.core.theme.iOSPurple
import com.android.purebilibili.core.theme.iOSTeal
import com.android.purebilibili.core.theme.resolveAccessibleContainerColors
import com.android.purebilibili.feature.settings.SettingsLocalBackHandler
import com.android.purebilibili.feature.settings.screen.SkinCatalogScreen
import com.android.purebilibili.feature.settings.ui.SettingsBottomBarScrollEffect
import com.android.purebilibili.feature.settings.ui.SettingsPageScaffold
import com.android.purebilibili.feature.settings.ui.settingsScrollContentPadding
import com.android.purebilibili.core.ui.AppAlertDialog
import com.android.purebilibili.core.ui.LocalBottomBarContentPadding
import com.android.purebilibili.core.ui.adaptiveSquircleBackground
import com.android.purebilibili.core.ui.components.AppAdaptiveSwitch
import com.android.purebilibili.core.ui.components.AppCircularProgressIndicator
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppOutlinedButton
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppTextField
import com.android.purebilibili.core.ui.components.AppTextButton
import com.android.purebilibili.core.ui.components.rememberAdaptivePreferenceIconContentColor
import com.android.purebilibili.core.ui.components.rememberAdaptivePreferenceIconContainerColor
import com.android.purebilibili.core.ui.components.rememberAdaptiveSemanticIconTint
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.util.rememberNotificationPermissionState
import com.android.purebilibili.core.plugin.resolvePluginListActivityLabel
import com.android.purebilibili.feature.plugin.EYE_PROTECTION_PLUGIN_ID
import com.android.purebilibili.feature.plugin.EyeProtectionPlugin
import com.android.purebilibili.feature.plugin.SPONSOR_BLOCK_PLUGIN_ID
import com.android.purebilibili.feature.settings.buildUiSkinImagePreviewItems
import com.android.purebilibili.feature.settings.buildUiSkinPackagePreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel

internal suspend fun dispatchBuiltInPluginToggle(
    pluginId: String,
    enabled: Boolean,
    onSponsorBlockToggle: suspend (Boolean) -> Unit,
    onGenericPluginToggle: suspend (String, Boolean) -> Unit
) {
    if (pluginId == SPONSOR_BLOCK_PLUGIN_ID) {
        onSponsorBlockToggle(enabled)
    } else {
        onGenericPluginToggle(pluginId, enabled)
    }
}

internal fun downloadUiSkinRemotePackage(url: String): ByteArray {
    val request = Request.Builder()
        .url(url)
        .header("User-Agent", "BiliPai")
        .build()
    NetworkModule.okHttpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            throw IllegalArgumentException("皮肤资源下载失败: HTTP ${response.code}")
        }
        return response.body.bytes()
    }
}

internal fun resolveUiSkinImportErrorMessage(rawMessage: String?): String {
    val message = rawMessage?.takeIf { it.isNotBlank() } ?: return "皮肤包导入失败"
    if (message.contains("装扮存档解压后内容超过 33554432 字节")) {
        return "装扮存档资源较大，已放宽导入限制；请重新选择该装扮包导入"
    }
    return message
}

internal fun downloadJsRemotePlugin(url: String): String {
    val request = Request.Builder()
        .url(url)
        .header("User-Agent", "BiliPai")
        .build()
    NetworkModule.okHttpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            throw IllegalArgumentException("JS 插件下载失败: HTTP ${response.code}")
        }
        return response.body.string()
    }
}

private data class BiliPaiJsPluginPreview(
    val manifest: BiliPaiJsPluginManifest,
    val script: String,
    val sourceUrl: String?
)

/**
 *  插件中心页面
 * 
 * 显示所有可用插件，支持启用/禁用和配置。
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PluginsScreen(
    onBack: () -> Unit,
    initialImportUrl: String? = null,
    onOpenJsPlugin: (String) -> Unit = {}
) {
    // Top-level state for managing plugins and editing
    val plugins by PluginManager.pluginsFlow.collectAsStateWithLifecycle()
    val jsonPlugins by com.android.purebilibili.core.plugin.json.JsonPluginManager.plugins.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val screenTitle = stringResource(R.string.plugins_center_title)
    val backLabel = stringResource(R.string.common_back)
    
    //  编辑插件状态
    var editingPlugin by remember { mutableStateOf<com.android.purebilibili.core.plugin.json.JsonRulePlugin?>(null) }
    var selectedBuiltInPluginId by remember { mutableStateOf<String?>(null) }
    var showSkinCatalog by remember { mutableStateOf(false) }
    
    //  如果正在编辑插件，显示编辑器全屏覆盖 (Mobile behavior)
    //  In Tablet, this will be handled differently.
    editingPlugin?.let { plugin ->
        SettingsLocalBackHandler { editingPlugin = null }
        JsonPluginEditorScreen(
            plugin = plugin,
            onBack = { editingPlugin = null },
            onSave = { updated ->
                com.android.purebilibili.core.plugin.json.JsonPluginManager.updatePlugin(updated)
            }
        )
        return
    }

    selectedBuiltInPluginId?.let { pluginId ->
        val pluginInfo = plugins.firstOrNull { it.plugin.id == pluginId }
        if (pluginInfo != null) {
            SettingsLocalBackHandler { selectedBuiltInPluginId = null }
            PluginDetailScreen(
                pluginInfo = pluginInfo,
                onBack = { selectedBuiltInPluginId = null }
            )
            return
        }
    }

    if (showSkinCatalog) {
        SettingsLocalBackHandler { showSkinCatalog = false }
        SkinCatalogScreen(
            onBack = { showSkinCatalog = false },
            onInstalled = { showSkinCatalog = false }
        )
        return
    }

    val bottomContentPadding = LocalBottomBarContentPadding.current

    SettingsPageScaffold(
        title = screenTitle,
        onBack = onBack,
        backContentDescription = backLabel,
        bottomContentPadding = bottomContentPadding,
        scrollHost = SettingsPageScrollHost.External,
    ) {
        PluginsContent(
            plugins = plugins,
            jsonPlugins = jsonPlugins,
            onOpenBuiltInPlugin = { selectedBuiltInPluginId = it },
            onEditJsonPlugin = { editingPlugin = it },
            initialImportUrl = initialImportUrl,
            onOpenJsPlugin = onOpenJsPlugin,
            onOpenSkinCatalog = { showSkinCatalog = true },
        )
    }
}

@Composable
fun PluginsContent(
    modifier: Modifier = Modifier,
    plugins: List<com.android.purebilibili.core.plugin.PluginInfo>,
    jsonPlugins: List<com.android.purebilibili.core.plugin.json.LoadedJsonPlugin>,
    onOpenBuiltInPlugin: (String) -> Unit,
    onEditJsonPlugin: (com.android.purebilibili.core.plugin.json.JsonRulePlugin) -> Unit,
    initialImportUrl: String? = null,
    onOpenJsPlugin: (String) -> Unit = {},
    onOpenSkinCatalog: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val effectMatchHintsEnabledFlow = remember(context) {
        PluginStore.effectMatchHintsEnabledFlow(context)
    }
    val effectMatchHintsEnabled by effectMatchHintsEnabledFlow
        .collectAsStateWithLifecycle(initialValue = false)
    val listState = rememberLazyListState()
    SettingsBottomBarScrollEffect(listState)
    val contentBottomPadding = LocalBottomBarContentPadding.current + 16.dp

    // Statistics
    val totalPlugins = plugins.size + jsonPlugins.size
    val enabledPlugins = plugins.count { it.enabled } + jsonPlugins.count { it.enabled }
    
    // Local UI states
    var jsonStatsNotificationEnabled by remember(context) {
        mutableStateOf(readJsonPluginStatsNotificationConfig(context).enabled)
    }
    fun showToast(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }
    fun sendJsonStatsTestNotification() {
        val posted = postJsonPluginStatsTestNotification(context)
        showToast(if (posted) "测试通知已发送" else "系统通知未开启")
    }
    val notificationPermission = rememberNotificationPermissionState { granted ->
        if (granted) {
            sendJsonStatsTestNotification()
        } else {
            showToast("通知权限未开启")
        }
    }
    
    //  导入插件对话框状态
    var showImportDialog by remember { mutableStateOf(false) }
    var importUrl by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }
    var isPreviewLoading by remember { mutableStateOf(false) }
    var showPreviewDialog by remember { mutableStateOf(false) }
    var previewPlugin by remember { mutableStateOf<com.android.purebilibili.core.plugin.json.JsonRulePlugin?>(null) }
    var previewSourceUrl by remember { mutableStateOf<String?>(null) }
    var initialImportConsumed by remember(initialImportUrl) { mutableStateOf(false) }
    val kotlinPluginStore = remember(context) {
        ExternalKotlinPluginInstallStore.createDefault(context)
    }
    val jsPluginStore = remember(context) {
        BiliPaiJsPluginInstallStore.createDefault(context)
    }
    val jsRuntime = remember(context) {
        BiliPaiJsRuntime(context)
    }
    var installedKotlinPackages by remember {
        mutableStateOf(kotlinPluginStore.listInstalledPackages())
    }
    var installedJsPlugins by remember {
        mutableStateOf(jsPluginStore.listInstalledPlugins())
    }
    var kotlinPreview by remember {
        mutableStateOf<Pair<ExternalKotlinPluginPackagePreview, ExternalPluginInstallDecision>?>(null)
    }
    var kotlinPackageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var kotlinImportError by remember { mutableStateOf<String?>(null) }
    var isKotlinPackageLoading by remember { mutableStateOf(false) }
    var showJsImportDialog by remember { mutableStateOf(false) }
    var jsImportUrl by remember { mutableStateOf("") }
    var jsImportError by remember { mutableStateOf<String?>(null) }
    var jsPreview by remember { mutableStateOf<BiliPaiJsPluginPreview?>(null) }
    var isJsPreviewLoading by remember { mutableStateOf(false) }
    var isJsInstalling by remember { mutableStateOf(false) }
    val uiSkinStore = remember(context) {
        UiSkinInstallStore.createDefault(context)
    }
    val uiSkinState by rememberUiSkinState(context)
    var installedUiSkins by remember {
        mutableStateOf(uiSkinStore.listInstalledPackages())
    }
    var uiSkinPreview by remember { mutableStateOf<UiSkinPackagePreview?>(null) }
    var uiSkinPackageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var uiSkinPreviewAssetFiles by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var uiSkinImportMode by remember { mutableStateOf(UiSkinImportMode.FULL_SKIN) }
    var uiSkinInstalledPreview by remember { mutableStateOf<InstalledUiSkinPackage?>(null) }
    var uiSkinPendingDelete by remember { mutableStateOf<InstalledUiSkinPackage?>(null) }
    var uiSkinImportError by remember { mutableStateOf<String?>(null) }
    var isUiSkinPackageLoading by remember { mutableStateOf(false) }
    val uiSkinPackagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isUiSkinPackageLoading = true
        uiSkinImportError = null
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw IllegalArgumentException("无法读取皮肤包")
                    val importPackage = UiSkinImportPackageResolver.resolve(
                        inputBytes = bytes,
                        remotePackageFetcher = ::downloadUiSkinRemotePackage
                    ).getOrThrow()
                    val preview = uiSkinStore.previewPackage(importPackage.packageBytes).getOrThrow()
                    val previewAssetFiles = uiSkinStore.extractPreviewAssetFiles(
                        preview = preview,
                        packageBytes = importPackage.packageBytes
                    ).getOrThrow()
                    Triple(preview, importPackage.packageBytes, previewAssetFiles)
                }
            }
            isUiSkinPackageLoading = false
            result.onSuccess { (preview, bytes, previewAssetFiles) ->
                uiSkinPreview = preview
                uiSkinPackageBytes = bytes
                uiSkinPreviewAssetFiles = previewAssetFiles
                uiSkinImportMode = UiSkinImportMode.FULL_SKIN
            }.onFailure { error ->
                uiSkinImportError = resolveUiSkinImportErrorMessage(error.message)
            }
        }
    }
    
    //  测试对话框状态
    var testingPluginId by remember { mutableStateOf<String?>(null) }
    var testResult by remember { mutableStateOf<Triple<Int, Int, List<com.android.purebilibili.data.model.response.VideoItem>>?>(null) }
    var testingSampleVideos by remember { mutableStateOf<List<com.android.purebilibili.data.model.response.VideoItem>>(emptyList()) }
    val importTint = rememberAdaptivePreferenceIconContainerColor(iOSBlue)
    val jsImportTint = rememberAdaptivePreferenceIconContainerColor(iOSTeal)
    val kotlinPackageTint = rememberAdaptivePreferenceIconContainerColor(MaterialTheme.colorScheme.primary)
    val uiSkinTint = rememberAdaptivePreferenceIconContainerColor(MaterialTheme.colorScheme.tertiary)
    val importIconContentColor = rememberAdaptivePreferenceIconContentColor(importTint)
    val jsImportIconContentColor = rememberAdaptivePreferenceIconContentColor(jsImportTint)
    val kotlinPackageIconContentColor = rememberAdaptivePreferenceIconContentColor(kotlinPackageTint)
    val uiSkinIconContentColor = rememberAdaptivePreferenceIconContentColor(uiSkinTint)
    val kotlinPackagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isKotlinPackageLoading = true
        kotlinImportError = null
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw IllegalArgumentException("无法读取插件包")
                    val preview = kotlinPluginStore.previewPackage(bytes).getOrThrow()
                    val decision = evaluateExternalPluginInstall(
                        packageDescriptor = preview.descriptor,
                        trustedSignerSha256 = emptySet()
                    )
                    bytes to (preview to decision)
                }
            }
            isKotlinPackageLoading = false
            result.onSuccess { (bytes, previewAndDecision) ->
                kotlinPackageBytes = bytes
                kotlinPreview = previewAndDecision
            }.onFailure { error ->
                kotlinImportError = error.message ?: "预览失败"
            }
        }
    }
    val jsPluginPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isJsPreviewLoading = true
        jsImportError = null
        scope.launch {
            val scriptResult = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.readBytes().decodeToString()
                    } ?: throw IllegalArgumentException("无法读取 JS 插件")
                }
            }
            val result = scriptResult.fold(
                onSuccess = { script ->
                    jsRuntime.previewManifest(script).map { manifest ->
                        BiliPaiJsPluginPreview(
                            manifest = manifest,
                            script = script,
                            sourceUrl = uri.toString()
                        )
                    }
                },
                onFailure = { error -> Result.failure<BiliPaiJsPluginPreview>(error) }
            )
            isJsPreviewLoading = false
            result.onSuccess { preview ->
                jsPreview = preview
            }.onFailure { error ->
                jsImportError = error.message ?: "JS 插件预览失败"
            }
        }
    }

    fun validateImportUrlOrError(raw: String): String? {
        val normalized = raw.trim()
        if (normalized.isBlank()) return "请输入链接地址"
        val uri = Uri.parse(normalized)
        val scheme = uri.scheme?.lowercase()
        if (scheme !in listOf("http", "https") || uri.host.isNullOrBlank()) {
            return "请输入有效的 http/https 地址"
        }
        return null
    }

    fun requestPreview(rawUrl: String) {
        val normalizedUrl = rawUrl.trim()
        val validationError = validateImportUrlOrError(normalizedUrl)
        if (validationError != null) {
            importError = validationError
            return
        }

        importError = null
        isPreviewLoading = true
        scope.launch {
            val result = com.android.purebilibili.core.plugin.json.JsonPluginManager.previewFromUrl(normalizedUrl)
            isPreviewLoading = false
            if (result.isSuccess) {
                previewPlugin = result.getOrNull()
                previewSourceUrl = normalizedUrl
                showPreviewDialog = true
                showImportDialog = false
            } else {
                importError = result.exceptionOrNull()?.message ?: "预览失败"
                showImportDialog = true
            }
        }
    }

    fun requestJsPreview(rawUrl: String) {
        val normalizedUrl = rawUrl.trim()
        val validationError = validateImportUrlOrError(normalizedUrl)
        if (validationError != null) {
            jsImportError = validationError
            return
        }

        jsImportError = null
        isJsPreviewLoading = true
        scope.launch {
            val scriptResult = withContext(Dispatchers.IO) {
                runCatching { downloadJsRemotePlugin(normalizedUrl) }
            }
            val result = scriptResult.fold(
                onSuccess = { script ->
                    jsRuntime.previewManifest(script).map { manifest ->
                        BiliPaiJsPluginPreview(
                            manifest = manifest,
                            script = script,
                            sourceUrl = normalizedUrl
                        )
                    }
                },
                onFailure = { error -> Result.failure<BiliPaiJsPluginPreview>(error) }
            )
            isJsPreviewLoading = false
            result.onSuccess { preview ->
                jsPreview = preview
                showJsImportDialog = false
            }.onFailure { error ->
                jsImportError = error.message ?: "JS 插件预览失败"
                showJsImportDialog = true
            }
        }
    }

    LaunchedEffect(initialImportUrl) {
        if (!initialImportConsumed && !initialImportUrl.isNullOrBlank()) {
            initialImportConsumed = true
            importUrl = initialImportUrl.trim()
            requestPreview(importUrl)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = settingsScrollContentPadding(
            extraTop = 16.dp,
            extraBottom = contentBottomPadding,
        )
    ) {
            
            // 标题说明
            item {
                AppText(
                    text = "已安装插件".uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 32.dp, bottom = 8.dp)
                )
            }
            
            // 插件列表
            item {
                AppSurface(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(AppShapes.container(ContainerLevel.Card)),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Column {
                        plugins.forEachIndexed { index, pluginInfo ->
                            PluginItem(
                                pluginInfo = pluginInfo,
                                iconTint = getPluginColor(index),
                                onToggle = { enabled ->
                                    scope.launch {
                                        dispatchBuiltInPluginToggle(
                                            pluginId = pluginInfo.plugin.id,
                                            enabled = enabled,
                                            onSponsorBlockToggle = { sponsorEnabled ->
                                                SettingsManager.setSponsorBlockEnabled(context, sponsorEnabled)
                                            },
                                            onGenericPluginToggle = { pluginId, pluginEnabled ->
                                                PluginManager.setEnabled(pluginId, pluginEnabled)
                                            }
                                        )
                                    }
                                },
                                onOpen = { onOpenBuiltInPlugin(pluginInfo.plugin.id) }
                            )
                            if (index < plugins.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(0.5.dp)
                                        .padding(start = 66.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                )
                            }
                        }
                    }
                }
            }
            
            // 统计信息
            item {
                val enabledCount = plugins.count { it.enabled }
                AppText(
                    text = "${plugins.size} 个插件，$enabledCount 个已启用",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 32.dp, top = 16.dp)
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                AppText(
                    text = "提示设置",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 32.dp, bottom = 8.dp)
                )
                AppSurface(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(AppShapes.container(ContainerLevel.Card)),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch {
                                    PluginStore.setEffectMatchHintsEnabled(
                                        context,
                                        !effectMatchHintsEnabled
                                    )
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            AppText(
                                text = "显示插件生效提示",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            AppText(
                                text = "去广告或弹幕增强命中时显示顶部提示",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        AppAdaptiveSwitch(
                            checked = effectMatchHintsEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    PluginStore.setEffectMatchHintsEnabled(context, enabled)
                                }
                            }
                        )
                    }
                }
            }
            
            //  导入外部插件按钮
            item {
                Spacer(modifier = Modifier.height(24.dp))
                AppText(
                    text = "高级/实验功能",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 32.dp, bottom = 4.dp)
                )
                AppText(
                    text = "JSON、JS、Kotlin 包与装扮等扩展能力",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 32.dp, bottom = 8.dp)
                )
            }
            
            item {
                AppSurface(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(AppShapes.container(ContainerLevel.Card))
                        .clickable { showImportDialog = true },
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .adaptiveSquircleBackground(importTint, 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AppIcon(
                                imageVector = com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_cloud_download_24),
                                contentDescription = null,
                                tint = importIconContentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            AppText(
                                text = "导入外部插件",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            AppText(
                                text = "通过链接安装 JSON 规则插件，安装前预览能力",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        AppIcon(
                            imageVector = com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_add_24),
                            contentDescription = null,
                            tint = importTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                AppSurface(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(AppShapes.container(ContainerLevel.Card)),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                    tonalElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .adaptiveSquircleBackground(jsImportTint, 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AppIcon(
                                    imageVector = com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_terminal_24),
                                    contentDescription = null,
                                    tint = jsImportIconContentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                AppText(
                                    text = "导入 JS 媒体插件",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                AppText(
                                    text = if (isJsPreviewLoading) {
                                        "正在预览 JS 插件..."
                                    } else {
                                        "支持链接或本地 .js，预览 manifest 和权限后安装"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppOutlinedButton(
                                onClick = { showJsImportDialog = true },
                                enabled = !isJsPreviewLoading
                            ) {
                                AppText("链接")
                            }
                            AppOutlinedButton(
                                onClick = { jsPluginPicker.launch("*/*") },
                                enabled = !isJsPreviewLoading
                            ) {
                                AppText("本地文件")
                            }
                        }
                    }
                }
            }

            if (jsImportError != null) {
                item {
                    AppText(
                        text = jsImportError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
                    )
                }
            }

            if (installedJsPlugins.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    AppSurface(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clip(AppShapes.container(ContainerLevel.Card)),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        Column {
                            installedJsPlugins.forEachIndexed { index, installed ->
                                InstalledJsPluginItem(
                                    installed = installed,
                                    onToggle = { enabled ->
                                        jsPluginStore.setEnabled(installed.manifest.id, enabled)
                                        installedJsPlugins = jsPluginStore.listInstalledPlugins()
                                    },
                                    onOpen = { onOpenJsPlugin(installed.manifest.id) },
                                    onDelete = {
                                        jsPluginStore.removePlugin(installed.manifest.id)
                                        installedJsPlugins = jsPluginStore.listInstalledPlugins()
                                    }
                                )
                                if (index < installedJsPlugins.lastIndex) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(0.5.dp)
                                            .padding(start = 16.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                AppSurface(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(AppShapes.container(ContainerLevel.Card))
                        .clickable(enabled = !isKotlinPackageLoading) {
                            kotlinPackagePicker.launch("*/*")
                        },
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .adaptiveSquircleBackground(kotlinPackageTint, 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AppIcon(
                                imageVector = com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_shield_fill_24),
                                contentDescription = null,
                                tint = kotlinPackageIconContentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            AppText(
                                text = "开放 Kotlin 插件包",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            AppText(
                                text = if (isKotlinPackageLoading) {
                                    "正在读取 .bpplugin..."
                                } else {
                                    "选择 .bpplugin，展示 SHA-256、签名状态和敏感能力"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        AppSurface(
                            shape = AppShapes.container(ContainerLevel.Chip),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                        ) {
                            AppText(
                                text = "预览",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            if (kotlinImportError != null) {
                item {
                    AppText(
                        text = kotlinImportError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
                    )
                }
            }

            if (installedKotlinPackages.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    AppSurface(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clip(AppShapes.container(ContainerLevel.Card)),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        Column {
                            buildInstalledExternalPluginUiModels(installedKotlinPackages)
                                .forEachIndexed { index, installed ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        AppText(
                                            text = installed.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        AppText(
                                            text = "${installed.subtitle} · ${installed.stateText}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        AppText(
                                            text = installed.packageHashText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (index < installedKotlinPackages.lastIndex) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(0.5.dp)
                                                .padding(start = 16.dp)
                                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        )
                                    }
                                }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                AppText(
                    text = "界面皮肤",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 32.dp, bottom = 8.dp)
                )
            }

            item {
                AppSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppShapes.container(ContainerLevel.Card)),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenSkinCatalog() }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .adaptiveSquircleBackground(uiSkinTint, 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AppIcon(
                                    imageVector = com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_cloud_fill_24),
                                    contentDescription = null,
                                    tint = uiSkinIconContentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                AppText(
                                    text = "在线装扮目录",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                AppText(
                                    text = "浏览 Rovniced/bilibili-skin 主题存档，真实预览后一键导入",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            AppSurface(
                                shape = AppShapes.container(ContainerLevel.Chip),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                            ) {
                                AppText(
                                    text = "在线",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .padding(start = 16.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isUiSkinPackageLoading) {
                                    uiSkinPackagePicker.launch("*/*")
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .adaptiveSquircleBackground(uiSkinTint, 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AppIcon(
                                    imageVector = com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_brush_fill_24),
                                    contentDescription = null,
                                    tint = uiSkinIconContentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                AppText(
                                    text = "导入界面皮肤包",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                AppText(
                                    text = if (isUiSkinPackageLoading) {
                                        "正在读取 .bpskin..."
                                    } else {
                                        "选择 .bpskin、主题目录 ZIP 或装扮 _package.zip，只保存资源和启用记录"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            AppSurface(
                                shape = AppShapes.container(ContainerLevel.Chip),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                            ) {
                                AppText(
                                    text = "资源包",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (uiSkinImportError != null) {
                item {
                    AppText(
                        text = uiSkinImportError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
                    )
                }
            }

            if (installedUiSkins.isNotEmpty()) {
                item {
                    AppSurface(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .clip(AppShapes.container(ContainerLevel.Card)),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        Column {
                            installedUiSkins.forEachIndexed { index, skin ->
                                val isActive = uiSkinState.enabled &&
                                    uiSkinState.activeSkin?.installId == skin.installId
                                InstalledUiSkinItem(
                                    skin = skin,
                                    isActive = isActive,
                                    onToggle = { enabled ->
                                        UiSkinSettingsStore.setSelection(
                                            context = context,
                                            selection = UiSkinSelection(
                                                enabled = enabled,
                                                selectedSkinId = if (enabled) skin.skinId else null,
                                                selectedInstallId = if (enabled) skin.installId else null
                                            )
                                        )
                                    },
                                    onPreview = { uiSkinInstalledPreview = skin },
                                    onDelete = { uiSkinPendingDelete = skin }
                                )
                                if (index < installedUiSkins.lastIndex) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(0.5.dp)
                                            .padding(start = 16.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            //  已安装的 JSON 插件列表
            if (jsonPlugins.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    JsonPluginStatsNotificationSection(
                        enabled = jsonStatsNotificationEnabled,
                        onEnabledChange = { enabled ->
                            jsonStatsNotificationEnabled = enabled
                            persistJsonPluginStatsNotificationConfig(
                                context,
                                JsonPluginStatsNotificationConfig(enabled = enabled)
                            )
                            scheduleJsonPluginStatsSummary(context, enabled)
                        },
                        onSendTest = {
                            notificationPermission.launchWithPermission {
                                sendJsonStatsTestNotification()
                            }
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    val filterStats by com.android.purebilibili.core.plugin.json.JsonPluginManager.filterStats.collectAsStateWithLifecycle()
                    
                    AppSurface(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clip(AppShapes.container(ContainerLevel.Card)),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        Column {
                            jsonPlugins.forEachIndexed { index, loadedPlugin ->
                                JsonPluginItem(
                                    loaded = loadedPlugin,
                                    filterCount = filterStats[loadedPlugin.plugin.id] ?: 0,
                                    onToggle = { enabled ->
                                        com.android.purebilibili.core.plugin.json.JsonPluginManager.setEnabled(
                                            loadedPlugin.plugin.id, enabled
                                        )
                                    },
                                    onEdit = {
                                        //  Callback to editing
                                        onEditJsonPlugin(loadedPlugin.plugin)
                                    },
                                    onDelete = {
                                        com.android.purebilibili.core.plugin.json.JsonPluginManager.removePlugin(
                                            loadedPlugin.plugin.id
                                        )
                                    },
                                    onResetStats = {
                                        com.android.purebilibili.core.plugin.json.JsonPluginManager.resetStats(loadedPlugin.plugin.id)
                                        android.widget.Toast.makeText(
                                            context,
                                            "统计已重置",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    onTest = {
                                        //  获取首页样本视频进行测试
                                        scope.launch {
                                            try {
                                                // 从 API 获取样本视频
                                                val result = com.android.purebilibili.data.repository.VideoRepository.getHomeVideos(0)
                                                result.onSuccess { videos ->
                                                    val sampleVideos = videos.take(20)
                                                    testingSampleVideos = sampleVideos
                                                    val (original, filtered) = com.android.purebilibili.core.plugin.json.JsonPluginManager.testPluginRules(
                                                        loadedPlugin.plugin.id, sampleVideos
                                                    )
                                                    val blockedVideos = com.android.purebilibili.core.plugin.json.JsonPluginManager.getFilteredVideosByPlugin(
                                                        loadedPlugin.plugin.id, sampleVideos
                                                    )
                                                    testResult = Triple(original, filtered, blockedVideos)
                                                    testingPluginId = loadedPlugin.plugin.id
                                                }.onFailure {
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "获取测试数据失败",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            } catch (e: Exception) {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "测试失败: ${e.message}",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                )
                                if (index < jsonPlugins.lastIndex) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 52.dp)
                                            .height(0.5.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // 底部说明
            item {
                Spacer(modifier = Modifier.height(24.dp))
                AppText(
                    text = "插件可以扩展应用功能，如自动跳过广告、过滤推荐内容等。\n启用插件后可点击展开查看详细设置。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    
    if (showJsImportDialog) {
        AppAlertDialog(
            onDismissRequest = {
                if (!isJsPreviewLoading) {
                    showJsImportDialog = false
                    jsImportUrl = ""
                    jsImportError = null
                }
            },
            icon = { AppIcon(com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_terminal_24), contentDescription = null) },
            title = { AppText("导入 JS 媒体插件") },
            text = {
                Column {
                    AppText(
                        text = "输入 JS 插件链接。安装前只展示插件信息、模块和权限，确认后默认保持禁用。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    AppTextField(
                        value = jsImportUrl,
                        onValueChange = {
                            jsImportUrl = it
                            jsImportError = null
                        },
                        label = "JS 插件链接",
                        placeholder = "例如：https://example.com/plugin.js",
                        singleLine = true,
                        isError = jsImportError != null,
                        supportingText = jsImportError?.let { { AppText(it, color = MaterialTheme.colorScheme.error) } }
                    )
                    if (isJsPreviewLoading) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AppCircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            AppText("正在预览...")
                        }
                    }
                }
            },
            confirmButton = {
                AppTextButton(
                    onClick = { requestJsPreview(jsImportUrl) },
                    enabled = !isJsPreviewLoading
                ) {
                    AppText("预览")
                }
            },
            dismissButton = {
                AppTextButton(
                    onClick = {
                        showJsImportDialog = false
                        jsImportUrl = ""
                        jsImportError = null
                    },
                    enabled = !isJsPreviewLoading
                ) {
                    AppText("取消")
                }
            }
        )
    }

    jsPreview?.let { preview ->
        AppAlertDialog(
            onDismissRequest = {
                if (!isJsInstalling) {
                    jsPreview = null
                }
            },
            icon = { AppIcon(com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_shield_fill_24), contentDescription = null) },
            title = { AppText("JS 插件预览") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppText(
                        text = preview.manifest.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    AppText(
                        text = "${preview.manifest.id} · v${preview.manifest.version} · ${preview.manifest.author.ifBlank { "未知作者" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AppText(
                        text = preview.manifest.description.ifBlank { "无描述" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AppText(
                        text = "模块：${preview.manifest.modules.joinToString("、") { it.title }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AppText(
                        text = "来源：${preview.sourceUrl ?: "本地文件"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PluginCapabilityDetailSection(
                        capabilities = resolveBiliPaiJsPluginCapabilities(preview.manifest)
                    )
                    AppText(
                        text = "外部 JS 由用户信任源提供；不会暴露登录 Cookie、Token、本地文件路径或 Android 对象。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    if (isJsInstalling) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppCircularProgressIndicator(modifier = Modifier.size(18.dp))
                            AppText("正在安装...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                AppTextButton(
                    enabled = !isJsInstalling,
                    onClick = {
                        isJsInstalling = true
                        val result = jsPluginStore.installPlugin(
                            manifest = preview.manifest,
                            script = preview.script,
                            sourceUrl = preview.sourceUrl,
                            grantedCapabilities = preview.manifest.permissions
                        )
                        isJsInstalling = false
                        result.onSuccess {
                            installedJsPlugins = jsPluginStore.listInstalledPlugins()
                            jsPreview = null
                            jsImportUrl = ""
                            jsImportError = null
                            android.widget.Toast.makeText(
                                context,
                                "JS 插件已安装，默认未启用",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }.onFailure { error ->
                            jsImportError = error.message ?: "JS 插件安装失败"
                        }
                    }
                ) {
                    AppText("确认安装")
                }
            },
            dismissButton = {
                AppTextButton(
                    enabled = !isJsInstalling,
                    onClick = { jsPreview = null }
                ) {
                    AppText("取消")
                }
            }
        )
    }

    //  导入插件对话框
    if (showImportDialog) {
        AppAlertDialog(
            onDismissRequest = { 
                showImportDialog = false
                importUrl = ""
                importError = null
            },
            icon = { AppIcon(com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_cloud_download_24), contentDescription = null) },
            title = { AppText("导入外部插件") },
            text = {
                Column {
                    AppText(
                        text = "输入 JSON 规则插件链接（支持任意返回 JSON 的 http/https 地址）",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    AppTextField(
                        value = importUrl,
                        onValueChange = { 
                            importUrl = it
                            importError = null
                        },
                        label = "插件链接",
                        placeholder = "例如：https://example.com/plugin.json",
                        singleLine = true,
                        isError = importError != null,
                        supportingText = importError?.let { { AppText(it, color = MaterialTheme.colorScheme.error) } }
                    )
                    
                    if (isImporting) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AppCircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            AppText("正在安装...")
                        }
                    }
                    if (isPreviewLoading) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AppCircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            AppText("正在加载插件信息...")
                        }
                    }
                }
            },
            confirmButton = {
                AppTextButton(
                    onClick = {
                        requestPreview(importUrl)
                    },
                    enabled = !isImporting && !isPreviewLoading
                ) {
                    AppText("预览")
                }
            },
            dismissButton = {
                AppTextButton(
                    onClick = { 
                        showImportDialog = false
                        importUrl = ""
                        importError = null
                    },
                    enabled = !isImporting && !isPreviewLoading
                ) {
                    AppText("取消")
                }
            }
        )
    }

    if (isPreviewLoading && !showImportDialog) {
        AppAlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { AppText("加载中") },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppCircularProgressIndicator(modifier = Modifier.size(20.dp))
                    AppText("正在加载插件信息...")
                }
            }
        )
    }

    if (showPreviewDialog) {
        val plugin = previewPlugin
        val sourceUrl = previewSourceUrl
        if (plugin != null && sourceUrl != null) {
            AppAlertDialog(
                onDismissRequest = {
                    if (!isImporting) {
                        showPreviewDialog = false
                    }
                },
                icon = {
                    if (!plugin.iconUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = plugin.iconUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(AppShapes.container(ContainerLevel.Chip))
                        )
                    } else {
                        AppIcon(com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_extension_24), contentDescription = null)
                    }
                },
                title = { AppText("安装插件预览") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppText(
                            text = plugin.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        AppText(
                            text = plugin.description.ifEmpty { "无描述" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        AppText(
                            text = "作者：${plugin.author}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        AppText(
                            text = "版本：${plugin.version} · 类型：${plugin.type} · 规则数：${plugin.rules.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        PluginCapabilityDetailSection(
                            capabilities = resolveJsonRulePluginCapabilities(plugin.type)
                        )
                        if (isImporting) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AppCircularProgressIndicator(modifier = Modifier.size(18.dp))
                                AppText(
                                    text = "正在安装...",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    AppTextButton(
                        enabled = !isImporting,
                        onClick = {
                            isImporting = true
                            scope.launch {
                                val result = com.android.purebilibili.core.plugin.json.JsonPluginManager
                                    .importFromUrl(sourceUrl)
                                isImporting = false
                                if (result.isSuccess) {
                                    showPreviewDialog = false
                                    importUrl = ""
                                    importError = null
                                    previewPlugin = null
                                    previewSourceUrl = null
                                    android.widget.Toast.makeText(
                                        context,
                                        "插件 \"${result.getOrNull()?.name}\" 安装成功！",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    importError = result.exceptionOrNull()?.message ?: "安装失败"
                                    showImportDialog = true
                                }
                            }
                        }
                    ) {
                        AppText("确认安装")
                    }
                },
                dismissButton = {
                    AppTextButton(
                        enabled = !isImporting,
                        onClick = { showPreviewDialog = false }
                    ) {
                        AppText("取消")
                    }
                }
            )
        }
    }

    kotlinPreview?.let { (preview, decision) ->
        val previewModel = buildExternalPluginInstallPreview(decision)
        AppAlertDialog(
            onDismissRequest = {
                if (!isImporting) {
                    kotlinPreview = null
                    kotlinPackageBytes = null
                }
            },
            icon = { AppIcon(com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_shield_fill_24), contentDescription = null) },
            title = { AppText("Kotlin 插件包预览") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppText(
                        text = previewModel.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    AppText(
                        text = previewModel.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AppText(
                        text = previewModel.packageHashText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AppText(
                        text = "${previewModel.signerText} · ${buildExternalPluginPayloadSummary(preview.payloadEntries)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PluginCapabilityDetailSection(
                        capabilities = preview.descriptor.manifest.capabilities
                    )
                    if (decision is ExternalPluginInstallDecision.Rejected) {
                        AppText(
                            text = decision.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                AppTextButton(
                    enabled = decision is ExternalPluginInstallDecision.RequiresUserApproval,
                    onClick = {
                        val bytes = kotlinPackageBytes ?: return@AppTextButton
                        val result = kotlinPluginStore.installPreview(
                            preview = preview,
                            packageBytes = bytes,
                            grantedCapabilities = preview.descriptor.manifest.capabilities
                        )
                        result.onSuccess {
                            installedKotlinPackages = kotlinPluginStore.listInstalledPackages()
                            kotlinPreview = null
                            kotlinPackageBytes = null
                            android.widget.Toast.makeText(
                                context,
                                "插件包已保存，当前不会运行",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }.onFailure { error ->
                            kotlinImportError = error.message ?: "安装失败"
                            kotlinPreview = null
                            kotlinPackageBytes = null
                        }
                    }
                ) {
                    AppText("保存授权")
                }
            },
            dismissButton = {
                AppTextButton(
                    onClick = {
                        kotlinPreview = null
                        kotlinPackageBytes = null
                    }
                ) {
                    AppText("取消")
                }
            }
        )
    }

    uiSkinPreview?.let { preview ->
        val previewModel = buildUiSkinPackagePreview(preview)
        val imagePreviewItems = buildUiSkinImagePreviewItems(uiSkinPreviewAssetFiles)
        AppAlertDialog(
            onDismissRequest = {
                if (!isImporting) {
                    uiSkinPreview = null
                    uiSkinPackageBytes = null
                    uiSkinPreviewAssetFiles = emptyMap()
                }
            },
            icon = { AppIcon(com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_brush_fill_24), contentDescription = null) },
            title = { AppText("界面皮肤包预览") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppText(
                        text = previewModel.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    AppText(
                        text = previewModel.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AppText(
                        text = previewModel.packageHashText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AppText(
                        text = previewModel.assetSummaryText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AppText(
                        text = previewModel.sourceText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AppText(
                        text = previewModel.licenseText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AppText(
                        text = "${previewModel.shareText} · ${previewModel.officialAssetText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (preview.manifest.containsOfficialAssets) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    if (preview.manifest.assets.homeProfileBackground != null ||
                        preview.manifest.assets.homeProfileSquaredBackground != null ||
                        preview.manifest.assets.homeProfileVideoBackground != null
                    ) {
                        AppText("导入方式", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppTextButton(onClick = { uiSkinImportMode = UiSkinImportMode.FULL_SKIN }) {
                                AppText(if (uiSkinImportMode == UiSkinImportMode.FULL_SKIN) "✓ 完整皮肤" else "完整皮肤")
                            }
                            AppTextButton(onClick = {
                                uiSkinImportMode = UiSkinImportMode.PERSONAL_BACKGROUND_ONLY
                            }) {
                                AppText(
                                    if (uiSkinImportMode == UiSkinImportMode.PERSONAL_BACKGROUND_ONLY) {
                                        "✓ 仅个人背景"
                                    } else {
                                        "仅个人背景"
                                    }
                                )
                            }
                        }
                    }
                    UiSkinImagePreviewGrid(items = imagePreviewItems)
                    AppText(
                        text = "宿主只保存资源和启用记录，不执行代码；可替换首页皮肤图标和装饰层，不替换底栏液态玻璃链路。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                AppTextButton(
                    onClick = {
                        val bytes = uiSkinPackageBytes ?: return@AppTextButton
                        val result = runCatching {
                            val packageToInstall = when (uiSkinImportMode) {
                                UiSkinImportMode.FULL_SKIN -> bytes
                                UiSkinImportMode.PERSONAL_BACKGROUND_ONLY ->
                                    UiSkinImportPackageResolver.restrictToPersonalBackground(bytes).getOrThrow()
                            }
                            val installPreview = uiSkinStore.previewPackage(packageToInstall).getOrThrow()
                            uiSkinStore.installPreview(installPreview, packageToInstall).getOrThrow()
                        }
                        result.onSuccess { installed ->
                            installedUiSkins = uiSkinStore.listInstalledPackages()
                            UiSkinSettingsStore.setSelection(
                                context = context,
                                selection = UiSkinSelection(
                                    enabled = true,
                                    selectedSkinId = installed.skinId,
                                    selectedInstallId = installed.installId
                                )
                            )
                            uiSkinPreview = null
                            uiSkinPackageBytes = null
                            uiSkinPreviewAssetFiles = emptyMap()
                            android.widget.Toast.makeText(
                                context,
                                "皮肤包已保存并启用",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }.onFailure { error ->
                            uiSkinImportError = error.message ?: "皮肤包导入失败"
                            uiSkinPreview = null
                            uiSkinPackageBytes = null
                            uiSkinPreviewAssetFiles = emptyMap()
                        }
                    }
                ) {
                    AppText("保存并启用")
                }
            },
            dismissButton = {
                AppTextButton(
                    onClick = {
                        uiSkinPreview = null
                        uiSkinPackageBytes = null
                        uiSkinPreviewAssetFiles = emptyMap()
                    }
                ) {
                    AppText("取消")
                }
            }
        )
    }

    uiSkinInstalledPreview?.let { installed ->
        val isActive = uiSkinState.enabled && uiSkinState.activeSkin?.installId == installed.installId
        val previewModel = buildInstalledUiSkinPreview(
            installed = installed,
            isActive = isActive
        )
        val imagePreviewItems = buildUiSkinImagePreviewItems(installed.assetFiles)
        AppAlertDialog(
            onDismissRequest = { uiSkinInstalledPreview = null },
            icon = { AppIcon(com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_visibility_24), contentDescription = null) },
            title = { AppText("皮肤预览") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppText(
                        text = previewModel.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    AppText(
                        text = previewModel.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AppText(
                        text = previewModel.packageHashText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AppText(
                        text = previewModel.assetSummaryText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AppText(
                        text = previewModel.sourceText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AppText(
                        text = previewModel.licenseText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AppText(
                        text = "${previewModel.shareText} · ${previewModel.officialAssetText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (installed.manifest.containsOfficialAssets) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    UiSkinImagePreviewGrid(items = imagePreviewItems)
                }
            },
            confirmButton = {
                AppTextButton(
                    onClick = {
                        UiSkinSettingsStore.setSelection(
                            context = context,
                            selection = UiSkinSelection(
                                enabled = true,
                                selectedSkinId = installed.skinId,
                                selectedInstallId = installed.installId
                            )
                        )
                        uiSkinInstalledPreview = null
                        android.widget.Toast.makeText(
                            context,
                            "已启用皮肤预览",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    enabled = !isActive
                ) {
                    AppText(if (isActive) "已启用" else "启用预览")
                }
            },
            dismissButton = {
                AppTextButton(onClick = { uiSkinInstalledPreview = null }) {
                    AppText("关闭")
                }
            }
        )
    }

    uiSkinPendingDelete?.let { skin ->
        AppAlertDialog(
            onDismissRequest = { uiSkinPendingDelete = null },
            icon = { AppIcon(com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_delete_24), contentDescription = null) },
            title = { AppText("删除皮肤") },
            text = { AppText("确定要删除皮肤 \"${skin.displayName}\" 吗？删除后会清理本地包和已解压资源。") },
            confirmButton = {
                AppTextButton(
                    onClick = {
                        val wasActive = uiSkinState.enabled &&
                            uiSkinState.activeSkin?.installId == skin.installId
                        val result = uiSkinStore.deleteInstalledPackage(skin.installId)
                        result.onSuccess { deleted ->
                            if (deleted) {
                                installedUiSkins = uiSkinStore.listInstalledPackages()
                                if (wasActive) {
                                    UiSkinSettingsStore.setSelection(
                                        context = context,
                                        selection = UiSkinSelection()
                                    )
                                }
                                android.widget.Toast.makeText(
                                    context,
                                    "皮肤已删除",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                            uiSkinPendingDelete = null
                        }.onFailure { error ->
                            uiSkinPendingDelete = null
                            uiSkinImportError = error.message ?: "皮肤删除失败"
                        }
                    }
                ) {
                    AppText("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                AppTextButton(onClick = { uiSkinPendingDelete = null }) {
                    AppText("取消")
                }
            }
        )
    }
    
    //  测试结果对话框
    testingPluginId?.let { pluginId ->
        testResult?.let { (original, filtered, blockedVideos) ->
            val pluginName = jsonPlugins.find { it.plugin.id == pluginId }?.plugin?.name ?: "未知插件"
            TestResultDialog(
                pluginName = pluginName,
                originalCount = original,
                filteredCount = filtered,
                filteredVideos = blockedVideos,
                onDismiss = {
                    testingPluginId = null
                    testResult = null
                }
            )
        }
    }
}

@Composable
private fun UiSkinImagePreviewGrid(
    items: List<com.android.purebilibili.feature.settings.UiSkinImagePreviewItem>
) {
    if (items.isEmpty()) {
        AppText(
            text = "图片预览：未找到可展示资源",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.take(6).forEach { item ->
            Column(
                modifier = Modifier.width(88.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (item.isVideo) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(AppShapes.container(ContainerLevel.Chip))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        AppIcon(
                            imageVector = com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_play_circle_24),
                            contentDescription = item.label,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    AsyncImage(
                        model = item.localPath,
                        contentDescription = item.label,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(AppShapes.container(ContainerLevel.Chip))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
                AppText(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun InstalledUiSkinItem(
    skin: InstalledUiSkinPackage,
    isActive: Boolean,
    onToggle: (Boolean) -> Unit,
    onPreview: () -> Unit,
    onDelete: () -> Unit
) {
    val previewModel = remember(skin, isActive) {
        buildInstalledUiSkinPreview(
            installed = skin,
            isActive = isActive
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            AppText(
                text = skin.displayName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            AppText(
                text = buildInstalledUiSkinSubtitle(skin.manifest),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp,
                maxLines = 2
            )
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            AppAdaptiveSwitch(
                checked = isActive,
                onCheckedChange = onToggle
            )
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                AppIconButton(onClick = onPreview) {
                    AppIcon(
                        imageVector = com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_visibility_24),
                        contentDescription = "预览皮肤",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                AppIconButton(
                    onClick = onDelete,
                    enabled = previewModel.canDelete
                ) {
                    AppIcon(
                        imageVector = com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_delete_24),
                        contentDescription = "删除皮肤",
                        tint = if (previewModel.canDelete) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        },
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PluginItem(
    pluginInfo: PluginInfo,
    iconTint: Color,
    onToggle: (Boolean) -> Unit,
    onOpen: () -> Unit
) {
    val plugin = pluginInfo.plugin
    val effectiveIconTint = rememberAdaptivePreferenceIconContainerColor(iconTint)
    val iconContentColor = rememberAdaptivePreferenceIconContentColor(effectiveIconTint)
    val eyeActivityFlow = remember(plugin.id) {
        (plugin as? EyeProtectionPlugin)?.isNightModeActive
            ?: kotlinx.coroutines.flow.MutableStateFlow(false)
    }
    val eyeActive by eyeActivityFlow.collectAsStateWithLifecycle()
    val activityLabel = resolvePluginListActivityLabel(
        enabled = pluginInfo.enabled,
        unavailable = plugin.unavailable,
        effectActive = plugin.id == EYE_PROTECTION_PLUGIN_ID && eyeActive
    )
    
    Column {
        // 主行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpen() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .adaptiveSquircleBackground(effectiveIconTint, 10.dp),
                contentAlignment = Alignment.Center
            ) {
                AppIcon(
                    imageVector = when (plugin.id) {
                        com.android.purebilibili.feature.plugin.SubscriptionFeedPlugin.PLUGIN_ID ->
                            com.android.purebilibili.feature.settings.rememberMaterialSymbol(
                                com.android.purebilibili.R.drawable.ms_rss_feed_24,
                            )
                        else -> plugin.icon ?: com.android.purebilibili.feature.settings.rememberMaterialSymbol(
                            com.android.purebilibili.R.drawable.ms_extension_24,
                        )
                    },
                    contentDescription = null,
                    tint = iconContentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            // 标题和描述
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppText(
                        text = plugin.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    AppText(
                        text = "v${plugin.version}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (activityLabel != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        AppSurface(
                            shape = AppShapes.container(ContainerLevel.Tag),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            AppText(
                                text = activityLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    //  暂不可用标签
                    if (plugin.unavailable) {
                        val unavailableColors = resolveAccessibleContainerColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                            contentColor = MaterialTheme.colorScheme.error,
                            backgroundColor = MaterialTheme.colorScheme.surface,
                            fallbackContentColors = listOf(
                                MaterialTheme.colorScheme.onErrorContainer,
                                MaterialTheme.colorScheme.onSurface,
                            ),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        AppSurface(
                            shape = AppShapes.container(ContainerLevel.Tag),
                            color = unavailableColors.containerColor,
                        ) {
                            AppText(
                                text = "暂不可用",
                                style = MaterialTheme.typography.labelSmall,
                                color = unavailableColors.contentColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                AppText(
                    text = plugin.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                //  显示作者
                if (plugin.author != "Unknown") {
                    AppText(
                        text = "作者：${plugin.author}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }
                PluginCapabilityChips(
                    capabilities = plugin.capabilityManifest.capabilities,
                    showAuthorizationLabels = false,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 开关
            AppAdaptiveSwitch(
                checked = pluginInfo.enabled,
                onCheckedChange = { enabled ->
                    if (!plugin.unavailable) onToggle(enabled)
                },
                enabled = !plugin.unavailable
            )
            
            // 进入详情
            AppIcon(
                imageVector = com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_keyboard_arrow_right_24),
                contentDescription = "打开插件详情",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(20.dp)
            )
        }
        
    }
}

@Composable
private fun PluginDetailScreen(
    pluginInfo: PluginInfo,
    onBack: () -> Unit,
) {
    val plugin = pluginInfo.plugin
    val bottomContentPadding = LocalBottomBarContentPadding.current
    SettingsPageScaffold(
        title = plugin.name,
        onBack = onBack,
        backContentDescription = "返回插件中心",
        bottomContentPadding = bottomContentPadding + 16.dp,
        scrollHost = SettingsPageScrollHost.LazyColumn,
        lazyListContent = {
            if (plugin.id != com.android.purebilibili.feature.plugin.SubscriptionFeedPlugin.PLUGIN_ID) {
                item {
                    AppSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                        shape = AppShapes.container(ContainerLevel.Card),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            AppText(
                                text = plugin.description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (plugin.author != "Unknown") {
                                AppText(
                                    text = "${plugin.author} · v${plugin.version}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                            PluginCapabilityDetailSection(
                                capabilities = plugin.capabilityManifest.capabilities,
                                showAuthorizationLabels = false,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                    }
                }
            }
            item {
                if (plugin.unavailable) {
                    AppText(
                        text = plugin.unavailableReason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
                    )
                } else {
                    plugin.SettingsContent(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    )
                }
            }
        },
    )
}

@Composable
private fun InstalledJsPluginItem(
    installed: InstalledBiliPaiJsPlugin,
    onToggle: (Boolean) -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                AppText(
                    text = installed.manifest.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                AppText(
                    text = "${installed.manifest.id} · v${installed.manifest.version} · ${if (installed.enabled) "已启用" else "未启用"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AppText(
                    text = installed.manifest.description.ifBlank { installed.sourceUrl ?: "本地 JS 插件" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                PluginCapabilityChips(
                    capabilities = installed.grantedCapabilities,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            AppAdaptiveSwitch(
                checked = installed.enabled,
                onCheckedChange = onToggle
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppTextButton(
                onClick = onOpen,
                enabled = installed.enabled
            ) {
                AppText("打开内容")
            }
            AppTextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                AppText("删除")
            }
        }
    }
}

@Composable
private fun PluginCapabilityChips(
    capabilities: Set<com.android.purebilibili.core.plugin.PluginCapability>,
    showAuthorizationLabels: Boolean = true,
    modifier: Modifier = Modifier
) {
    val models = remember(capabilities) { resolvePluginCapabilityUiModels(capabilities) }
    if (models.isEmpty()) return
    val colorScheme = MaterialTheme.colorScheme
    val approvalColors = resolveAccessibleContainerColors(
        containerColor = colorScheme.tertiaryContainer.copy(alpha = 0.62f),
        contentColor = colorScheme.onTertiaryContainer,
        backgroundColor = colorScheme.surface,
        fallbackContentColors = listOf(colorScheme.onSurface, colorScheme.onBackground),
    )
    val standardColors = resolveAccessibleContainerColors(
        containerColor = colorScheme.surfaceVariant.copy(alpha = 0.62f),
        contentColor = colorScheme.onSurfaceVariant,
        backgroundColor = colorScheme.surface,
        fallbackContentColors = listOf(colorScheme.onSurface, colorScheme.onBackground),
    )
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        models.forEach { model ->
            AppSurface(
                shape = AppShapes.container(ContainerLevel.Chip),
                color = if (showAuthorizationLabels && model.requiresExplicitApproval) {
                    approvalColors.containerColor
                } else {
                    standardColors.containerColor
                }
            ) {
                AppText(
                    text = if (showAuthorizationLabels && model.requiresExplicitApproval) {
                        "${model.label} · 需授权"
                    } else {
                        model.label
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (showAuthorizationLabels && model.requiresExplicitApproval) {
                        approvalColors.contentColor
                    } else {
                        standardColors.contentColor
                    },
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun PluginCapabilityDetailSection(
    capabilities: Set<com.android.purebilibili.core.plugin.PluginCapability>,
    showAuthorizationLabels: Boolean = true,
    modifier: Modifier = Modifier
) {
    val models = remember(capabilities) { resolvePluginCapabilityUiModels(capabilities) }
    if (models.isEmpty()) return
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppText(
            text = if (showAuthorizationLabels) "能力与授权" else "能力",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        models.forEach { model ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                AppText(
                    text = if (showAuthorizationLabels && model.requiresExplicitApproval) {
                        "${model.label} · 安装前确认"
                    } else {
                        model.label
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                AppText(
                    text = model.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 获取插件对应的颜色
 */
private fun getPluginColor(index: Int): Color {
    val colors = listOf(iOSTeal, iOSOrange, iOSBlue, iOSGreen, iOSPurple, iOSPink)
    return colors[index % colors.size]
}

@Composable
private fun JsonPluginStatsNotificationSection(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onSendTest: () -> Unit
) {
    val notificationIconTint = rememberAdaptivePreferenceIconContainerColor(iOSPurple)
    val notificationIconContentColor = rememberAdaptivePreferenceIconContentColor(notificationIconTint)
    AppSurface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(AppShapes.container(ContainerLevel.Card)),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .adaptiveSquircleBackground(notificationIconTint, 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AppIcon(
                        imageVector = com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_notifications_24),
                        contentDescription = null,
                        tint = notificationIconContentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    AppText(
                        text = "插件统计通知",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    AppText(
                        text = "每天汇总 JSON 规则插件过滤数量",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                AppAdaptiveSwitch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 66.dp)
                    .height(0.5.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )
            AppTextButton(
                onClick = onSendTest,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = iOSPurple)
            ) {
                AppIcon(com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_notifications_24), null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                AppText("发送测试通知", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/**
 * JSON 规则插件列表项
 */
@Composable
private fun JsonPluginItem(
    loaded: com.android.purebilibili.core.plugin.json.LoadedJsonPlugin,
    filterCount: Int,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onResetStats: () -> Unit = {},
    onTest: () -> Unit = {}
) {
    val plugin = loaded.plugin
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    val jsonPluginTint = rememberAdaptivePreferenceIconContainerColor(iOSPurple)
    val jsonPluginIconContentColor = rememberAdaptivePreferenceIconContentColor(jsonPluginTint)
    
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .adaptiveSquircleBackground(jsonPluginTint, 10.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!plugin.iconUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = plugin.iconUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(AppShapes.container(ContainerLevel.Field))
                    )
                } else {
                    AppIcon(
                        imageVector = com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_terminal_24),
                        contentDescription = null,
                        tint = jsonPluginIconContentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            // 信息
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppText(
                        text = plugin.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    AppText(
                        text = "v${plugin.version}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                AppText(
                    text = plugin.description.ifEmpty { plugin.type },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppText(
                        text = "作者：${plugin.author}",
                        style = MaterialTheme.typography.labelSmall,
                        color = iOSPurple
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    //  统计始终显示
                    AppSurface(
                        shape = AppShapes.container(ContainerLevel.Tag),
                        color = if (filterCount > 0) 
                            iOSGreen.copy(alpha = 0.15f)
                        else 
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        AppText(
                            text = "已过滤 $filterCount 项",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (filterCount > 0) iOSGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                PluginCapabilityChips(
                    capabilities = resolveJsonRulePluginCapabilities(plugin.type),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            
            // 开关
            AppAdaptiveSwitch(
                checked = loaded.enabled,
                onCheckedChange = onToggle
            )
            
            // 展开箭头
            AppIcon(
                imageVector = if (isExpanded) com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_keyboard_arrow_up_24) else com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_keyboard_arrow_down_24),
                contentDescription = if (isExpanded) "收起" else "展开",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(20.dp)
            )
        }
        
        //  展开的操作区域
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            AppSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 66.dp, end = 16.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = AppShapes.container(ContainerLevel.Chip)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 测试规则按钮
                    AppTextButton(
                        onClick = onTest,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = iOSBlue
                        )
                    ) {
                        AppIcon(com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_lightbulb_24), null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        AppText("测试规则", style = MaterialTheme.typography.labelMedium)
                    }
                    
                    // 重置统计按钮
                    AppTextButton(
                        onClick = onResetStats,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = iOSOrange
                        )
                    ) {
                        AppIcon(com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_refresh_24), null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        AppText("重置统计", style = MaterialTheme.typography.labelMedium)
                    }
                    
                    // 编辑按钮
                    AppTextButton(
                        onClick = onEdit,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = iOSPurple
                        )
                    ) {
                        AppIcon(com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_terminal_24), null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        AppText("编辑", style = MaterialTheme.typography.labelMedium)
                    }
                    
                    // 删除按钮
                    AppTextButton(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        AppIcon(com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_delete_24), null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        AppText("删除", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
    
    // 删除确认对话框
    if (showDeleteDialog) {
        AppAlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { AppText("删除插件") },
            text = { AppText("确定要删除插件 \"${plugin.name}\" 吗？") },
            confirmButton = {
                AppTextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    AppText("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                AppTextButton(onClick = { showDeleteDialog = false }) {
                    AppText("取消")
                }
            }
        )
    }
}

/**
 *  测试结果对话框
 */
@Composable
private fun TestResultDialog(
    pluginName: String,
    originalCount: Int,
    filteredCount: Int,
    filteredVideos: List<com.android.purebilibili.data.model.response.VideoItem>,
    onDismiss: () -> Unit
) {
    val blockedCount = originalCount - filteredCount
    val dialogIconTint = rememberAdaptiveSemanticIconTint(iOSBlue)
    val resultContainerColor = com.android.purebilibili.core.theme.opaqueCompositeOver(
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        MaterialTheme.colorScheme.surface,
    )

    AppAlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            AppIcon(
                com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_lightbulb_24),
                contentDescription = null,
                tint = dialogIconTint
            )
        },
        title = { AppText("规则测试结果") },
        text = {
            Column {
                AppText(
                    text = "插件：$pluginName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // 统计卡片
                AppSurface(
                    shape = AppShapes.container(ContainerLevel.Chip),
                    color = resultContainerColor,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AppText(
                                text = "$originalCount",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            AppText(
                                text = "测试视频",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AppText(
                                text = "$blockedCount",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (blockedCount > 0) iOSGreen else MaterialTheme.colorScheme.onSurface
                            )
                            AppText(
                                text = "被过滤",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AppText(
                                text = "$filteredCount",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            AppText(
                                text = "保留",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // 被过滤的视频列表
                if (filteredVideos.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    AppText(
                        text = "被过滤的视频示例：",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Column {
                        filteredVideos.take(3).forEach { video ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppIcon(
                                    com.android.purebilibili.feature.settings.rememberMaterialSymbol(com.android.purebilibili.R.drawable.ms_delete_24),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    AppText(
                                        text = video.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    AppText(
                                        text = "时长：${formatDuration(video.duration)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        if (filteredVideos.size > 3) {
                            AppText(
                                text = "……还有 ${filteredVideos.size - 3} 个视频",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                } else if (blockedCount == 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    AppText(
                        text = " 当前测试样本中没有符合过滤条件的视频",
                        style = MaterialTheme.typography.bodySmall,
                        color = iOSGreen
                    )
                }
            }
        },
        confirmButton = {
            AppTextButton(onClick = onDismiss) {
                AppText("确定")
            }
        }
    )
}

/**
 * 格式化时长（秒 -> 分:秒）
 */
private fun formatDuration(seconds: Int): String {
    return FormatUtils.formatDuration(seconds)
}
