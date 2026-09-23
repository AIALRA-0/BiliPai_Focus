package com.android.purebilibili.feature.plugin.js

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.components.AppAssistChip
import com.android.purebilibili.core.ui.AdaptiveLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import com.android.purebilibili.core.ui.components.AppSegmentOption
import com.android.purebilibili.core.ui.components.AppThemeAdaptiveTabRow
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.components.AppOutlinedTextField
import com.android.purebilibili.core.ui.AppScaffold
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.AppTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.android.purebilibili.core.plugin.js.BiliPaiJsMediaItem
import com.android.purebilibili.core.plugin.js.BiliPaiJsModule
import com.android.purebilibili.core.plugin.js.BiliPaiJsParam
import com.android.purebilibili.core.plugin.js.BiliPaiJsPluginInstallStore
import com.android.purebilibili.core.plugin.js.BiliPaiJsRuntime
import com.android.purebilibili.core.plugin.js.ExternalMediaLaunchStore
import com.android.purebilibili.core.plugin.js.InstalledBiliPaiJsPlugin
import com.android.purebilibili.core.plugin.js.isPlayable
import com.android.purebilibili.core.plugin.js.resolveBiliPaiJsMediaImageCandidates
import com.android.purebilibili.core.plugin.js.resolveBiliPaiJsMediaStreams
import com.android.purebilibili.core.ui.rememberAppBackIcon
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiliPaiJsPluginContentScreen(
    pluginId: String,
    onBack: () -> Unit,
    onPlayMedia: (String) -> Unit
) {
    val context = LocalContext.current
    val store = remember(context) { BiliPaiJsPluginInstallStore.createDefault(context) }
    val runtime = remember(context) { BiliPaiJsRuntime(context) }
    val scope = rememberCoroutineScope()
    var installed by remember(pluginId) {
        mutableStateOf(store.listInstalledPlugins().firstOrNull { it.manifest.id == pluginId })
    }
    var selectedModule by remember(installed) {
        mutableStateOf(installed?.manifest?.modules?.firstOrNull())
    }
    val paramValues = remember(pluginId, selectedModule) {
        mutableStateMapOf<String, String>().apply {
            val module = selectedModule ?: return@apply
            putAll(
                resolveBiliPaiJsInitialParamValues(
                    params = module.params,
                    savedValues = readBiliPaiJsParamValues(context, pluginId, module)
                )
            )
        }
    }
    var items by remember { mutableStateOf<List<BiliPaiJsMediaItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadModule(module: BiliPaiJsModule) {
        val current = installed ?: return
        selectedModule = module
        persistBiliPaiJsParamValues(context, current.manifest.id, module, paramValues)
        isLoading = true
        errorMessage = null
        scope.launch {
            val result = runtime.loadModuleItems(
                installed = current,
                module = module,
                paramsJson = buildParamsJson(module, paramValues)
            )
            isLoading = false
            result.onSuccess { loadedItems ->
                items = loadedItems
                errorMessage = if (loadedItems.isEmpty()) "插件没有返回媒体内容" else null
            }.onFailure { error ->
                items = emptyList()
                errorMessage = error.message ?: "插件内容加载失败"
            }
        }
    }

    LaunchedEffect(selectedModule, installed?.enabled) {
        val module = selectedModule
        val current = installed
        if (module != null && current?.enabled == true) {
            loadModule(module)
        }
    }

    AppScaffold(
        topBar = {
            AppTopBar(
                title = installed?.manifest?.title ?: "JS 插件内容",
                navigationIcon = {
                    AppIconButton(onClick = onBack) {
                        AppIcon(rememberAppBackIcon(), contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        val current = installed
        when {
            current == null -> EmptyState(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                text = "插件不存在或已删除"
            )
            !current.enabled -> EmptyState(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                text = "插件已禁用，请先在插件中心启用"
            )
            else -> PluginContent(
                modifier = Modifier.padding(padding),
                installed = current,
                selectedModule = selectedModule,
                paramValues = paramValues,
                mediaItems = items,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onSelectModule = { module -> loadModule(module) },
                onReload = { selectedModule?.let(::loadModule) },
                onPlayItem = { item ->
                    val streams = resolveBiliPaiJsMediaStreams(item)
                    if (streams.isNotEmpty()) {
                        onPlayMedia(
                            ExternalMediaLaunchStore.put(
                                title = item.title,
                                coverUrl = item.coverUrl,
                                streams = streams
                            )
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun PluginContent(
    modifier: Modifier,
    installed: InstalledBiliPaiJsPlugin,
    selectedModule: BiliPaiJsModule?,
    paramValues: MutableMap<String, String>,
    mediaItems: List<BiliPaiJsMediaItem>,
    isLoading: Boolean,
    errorMessage: String?,
    onSelectModule: (BiliPaiJsModule) -> Unit,
    onReload: () -> Unit,
    onPlayItem: (BiliPaiJsMediaItem) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AppText(
                text = installed.manifest.description.ifBlank { "${installed.manifest.id} · v${installed.manifest.version}" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            val modules = installed.manifest.modules
            if (modules.isNotEmpty() && selectedModule != null) {
                AppThemeAdaptiveTabRow(
                    options = modules.map { module -> AppSegmentOption(module, module.title) },
                    selectedValue = selectedModule,
                    onSelectionChange = onSelectModule,
                    scrollable = modules.size > 4,
                    dragSelectionEnabled = modules.size > 1,
                    tapPressRefractionEnabled = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        selectedModule?.params?.takeIf { it.isNotEmpty() }?.let { params ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    params.forEach { param ->
                        AppOutlinedTextField(
                            value = paramValues[param.name] ?: param.defaultValue,
                            onValueChange = { paramValues[param.name] = it },
                            label = { AppText(param.title) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    AppAssistChip(onClick = onReload, label = { AppText("重新加载") })
                }
            }
        }
        if (isLoading) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AdaptiveLoadingIndicator(size = 22.dp)
                    Spacer(modifier = Modifier.size(8.dp))
                    AppText("正在加载插件内容")
                }
            }
        }
        errorMessage?.let { message ->
            item {
                AppText(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        val flattenedItems = flattenMediaItems(mediaItems)
        items(
            count = flattenedItems.size,
            key = { index -> resolveBiliPaiJsMediaItemLazyKey(index, flattenedItems[index]) }
        ) { index ->
            val item = flattenedItems[index]
            BiliPaiJsMediaItemRow(
                item = item,
                onPlay = { onPlayItem(item) }
            )
        }
    }
}

@Composable
private fun BiliPaiJsMediaItemRow(
    item: BiliPaiJsMediaItem,
    onPlay: () -> Unit
) {
    val streams = remember(item) { resolveBiliPaiJsMediaStreams(item) }
    val imageCandidates = remember(item) { resolveBiliPaiJsMediaImageCandidates(item) }
    var imageCandidateIndex by remember(imageCandidates) { mutableIntStateOf(0) }
    val imageUrl = imageCandidates.getOrNull(imageCandidateIndex)
    val imageState = when {
        imageCandidates.isEmpty() -> PluginMediaImageState.NO_IMAGE
        imageUrl == null -> PluginMediaImageState.LOAD_FAILED
        else -> PluginMediaImageState.LOADING
    }
    AppSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.container(ContainerLevel.Field))
            .clickable(enabled = item.isPlayable, onClick = onPlay),
        color = AppSurfaceTokens.cardContainer(),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (imageUrl.isNullOrBlank()) {
                PluginMediaImagePlaceholder(
                    title = item.title,
                    state = imageState,
                    modifier = Modifier.size(width = 96.dp, height = 56.dp)
                )
            } else {
                Box(modifier = Modifier.size(width = 96.dp, height = 56.dp)) {
                    PluginMediaImagePlaceholder(
                        title = item.title,
                        state = PluginMediaImageState.LOADING,
                        modifier = Modifier.fillMaxSize()
                    )
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(AppShapes.container(ContainerLevel.Chip)),
                        contentScale = ContentScale.Crop,
                        onError = {
                            imageCandidateIndex += 1
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                AppText(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (item.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    AppText(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
                if (streams.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    AppText(
                        text = "线路 ${streams.size} 条",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (item.isPlayable) {
                AppText(
                    text = "播放",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PluginMediaImagePlaceholder(title: String) {
    PluginMediaImagePlaceholder(title = title, state = PluginMediaImageState.LOADING, modifier = Modifier)
}

@Composable
private fun PluginMediaImagePlaceholder(
    title: String,
    state: PluginMediaImageState,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .clip(AppShapes.container(ContainerLevel.Chip)),
        contentAlignment = Alignment.Center
    ) {
        AppSurface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        ) {}
        AppText(
            text = when (state) {
                PluginMediaImageState.NO_IMAGE -> "无图"
                PluginMediaImageState.LOAD_FAILED -> "失败"
                PluginMediaImageState.LOADING -> title.take(2).ifBlank { "TV" }
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private enum class PluginMediaImageState {
    LOADING,
    NO_IMAGE,
    LOAD_FAILED
}

@Composable
private fun EmptyState(
    modifier: Modifier,
    text: String
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AppText(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun buildParamsJson(
    module: BiliPaiJsModule,
    values: Map<String, String>
): String {
    return buildJsonObject {
        module.params.forEach { param ->
            put(param.name, JsonPrimitive(values[param.name] ?: param.defaultValue))
        }
    }.toString()
}

internal fun resolveBiliPaiJsInitialParamValues(
    params: List<BiliPaiJsParam>,
    savedValues: Map<String, String>
): Map<String, String> {
    return params.associate { param ->
        param.name to (savedValues[param.name] ?: param.defaultValue)
    }
}

internal fun buildBiliPaiJsParamPreferenceKey(
    pluginId: String,
    moduleId: String,
    paramName: String
): String {
    return "js_param_${safePreferencePart(pluginId)}_${safePreferencePart(moduleId)}_${safePreferencePart(paramName)}"
}

private fun readBiliPaiJsParamValues(
    context: android.content.Context,
    pluginId: String,
    module: BiliPaiJsModule
): Map<String, String> {
    val prefs = context.getSharedPreferences("bilipai_js_plugin_params", android.content.Context.MODE_PRIVATE)
    val moduleId = module.id.ifBlank { module.functionName }
    return module.params.associate { param ->
        val key = buildBiliPaiJsParamPreferenceKey(pluginId, moduleId, param.name)
        param.name to prefs.getString(key, param.defaultValue).orEmpty()
    }
}

private fun persistBiliPaiJsParamValues(
    context: android.content.Context,
    pluginId: String,
    module: BiliPaiJsModule,
    values: Map<String, String>
) {
    val prefs = context.getSharedPreferences("bilipai_js_plugin_params", android.content.Context.MODE_PRIVATE)
    val moduleId = module.id.ifBlank { module.functionName }
    prefs.edit().apply {
        module.params.forEach { param ->
            putString(
                buildBiliPaiJsParamPreferenceKey(pluginId, moduleId, param.name),
                values[param.name] ?: param.defaultValue
            )
        }
    }.apply()
}

private fun safePreferencePart(value: String): String {
    return value.replace(Regex("[^A-Za-z0-9_.-]"), "_")
}

private fun flattenMediaItems(items: List<BiliPaiJsMediaItem>): List<BiliPaiJsMediaItem> {
    return buildList {
        items.forEach { item ->
            add(item)
            addAll(item.childItems)
        }
    }
}

internal fun resolveBiliPaiJsMediaItemLazyKey(index: Int, item: BiliPaiJsMediaItem): String {
    return "js_media_${index}_${item.id}"
}
