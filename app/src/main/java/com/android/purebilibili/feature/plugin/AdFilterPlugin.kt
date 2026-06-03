// 文件路径: feature/plugin/AdFilterPlugin.kt
package com.android.purebilibili.feature.plugin

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.purebilibili.core.plugin.FeedPlugin
import com.android.purebilibili.core.plugin.PluginCapability
import com.android.purebilibili.core.plugin.PluginCapabilityManifest
import com.android.purebilibili.core.plugin.PluginManager
import com.android.purebilibili.core.plugin.PluginStore
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.data.repository.SearchRepository
import com.android.purebilibili.core.ui.components.*
import io.github.alexzhirkevich.cupertino.CupertinoSwitch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val TAG = "AdFilterPlugin"
internal const val ADFILTER_PLUGIN_ID = "adfilter"
private const val AD_FILTER_CUSTOM_LIST_PREVIEW_LIMIT = 3
private const val AD_FILTER_PROFILE_REFRESH_LIMIT = 10
private val AD_FILTER_EVENT_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

internal data class AdFilterCustomListSummary(
    val countText: String,
    val previewText: String,
    val hiddenCountText: String?
)

internal fun resolveAdFilterCustomListSummary(
    items: List<String>,
    emptyText: String,
    previewLimit: Int = AD_FILTER_CUSTOM_LIST_PREVIEW_LIMIT
): AdFilterCustomListSummary {
    val safePreviewLimit = previewLimit.coerceAtLeast(1)
    val previewItems = items.take(safePreviewLimit)
    val hiddenCount = (items.size - previewItems.size).coerceAtLeast(0)
    return AdFilterCustomListSummary(
        countText = "${items.size} 个",
        previewText = if (items.isEmpty()) emptyText else previewItems.joinToString("、"),
        hiddenCountText = if (hiddenCount > 0) {
            "还有 ${hiddenCount} 个，展开查看全部"
        } else {
            null
        }
    )
}

internal fun resolveAdFilterCustomListVisibleItems(
    items: List<String>,
    expanded: Boolean,
    previewLimit: Int = AD_FILTER_CUSTOM_LIST_PREVIEW_LIMIT
): List<String> {
    return if (expanded) items else items.take(previewLimit.coerceAtLeast(1))
}

internal fun removeAdFilterCustomListItem(
    items: List<String>,
    item: String
): List<String> {
    return items.filterNot { it == item }
}

/**
 * 🚫 去广告增强插件 v2.0
 * 
 * 功能：
 * 1. 过滤广告/推广/商业合作内容
 * 2. 过滤标题党视频
 * 3. 过滤低质量视频（播放量低）
 * 4. UP主拉黑（按名称或MID）
 * 5. 自定义关键词屏蔽
 */
class AdFilterPlugin : FeedPlugin {
    
    override val id = ADFILTER_PLUGIN_ID
    override val name = "去广告增强"
    override val description = "过滤广告、拉黑UP主、屏蔽关键词"
    override val version = "2.0.0"
    override val author = "BiliPai项目组"
    override val icon: ImageVector = CupertinoIcons.Default.Xmark
    override val capabilityManifest: PluginCapabilityManifest = PluginCapabilityManifest(
        pluginId = id,
        displayName = name,
        version = version,
        apiVersion = 1,
        entryClassName = "com.android.purebilibili.feature.plugin.AdFilterPlugin",
        capabilities = setOf(
            PluginCapability.RECOMMENDATION_CANDIDATES,
            PluginCapability.LOCAL_FEEDBACK_READ,
            PluginCapability.PLUGIN_STORAGE
        )
    )
    
    private var config: AdFilterConfig = AdFilterConfig()
    private var filteredCount = 0
    
    //  配置版本号，用于检测是否需要重载
    @Volatile
    private var configVersion = 0
    @Volatile
    private var lastConfigReloadMs = 0L
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    //  内置广告关键词（强化版）
    private val AD_KEYWORDS = listOf(
        // 商业合作类
        "商业合作", "恰饭", "推广", "广告", "赞助", "植入",
        "合作推广", "品牌合作", "本期合作", "本视频由",
        // 平台推广类
        "官方活动", "官方推荐", "平台活动", "创作激励",
        // 淘宝/电商类
        "淘宝", "天猫", "京东", "拼多多", "双十一", "双11",
        "优惠券", "领券", "限时优惠", "好物推荐", "种草",
        // 游戏推广类
        "新游推荐", "游戏推广", "首发", "公测", "不删档"
    )
    
    //  标题党关键词（强化版）
    private val CLICKBAIT_KEYWORDS = listOf(
        "震惊", "惊呆了", "太厉害了", "绝了", "离谱", "疯了",
        "价值几万", "价值百万", "价值千万", "一定要看", "必看",
        "看哭了", "泪目", "破防了", "DNA动了", "YYDS",
        "封神", "炸裂", "神作", "预定年度", "史诗级",
        "99%的人不知道", "你一定不知道", "居然是这样",
        "原来是这样", "真相了", "曝光", "揭秘", "独家"
    )
    
    override suspend fun onEnable() {
        filteredCount = 0
        loadConfigSuspend()
        Logger.d(TAG, " 去广告增强v2.0已启用")
        Logger.d(TAG, " 拉黑UP主: ${config.blockedUpNames.size}个, 屏蔽关键词: ${config.blockedKeywords.size}个")
    }
    
    override suspend fun onDisable() {
        Logger.d(TAG, "🔴 去广告增强已禁用，本次过滤了 $filteredCount 条内容")
        filteredCount = 0
    }
    
    override fun shouldShowItem(item: VideoItem): Boolean {
        //  每次过滤前确保配置是最新的
        reloadConfigAsync()
        
        val title = item.title
        val upName = item.owner.name
        val upMid = item.owner.mid
        val viewCount = item.stat.view
        
        // 1️⃣ 检查UP主拉黑列表（按名称） - 支持模糊匹配和简繁体
        val blockedName = findBlockedUpName(upName)
        if (blockedName != null) {
            filteredCount++
            recordFilteredItem(
                item = item,
                reasonType = AdFilterReasonType.BLOCKED_UP,
                matchedText = blockedName
            )
            Logger.d(TAG, "🚫 拉黑UP主[名称]: $upName - $title (列表: ${config.blockedUpNames})")
            return false
        }
        
        // 2️⃣ 检查UP主拉黑列表（按MID）
        if (config.blockedUpMids.contains(upMid)) {
            filteredCount++
            recordFilteredItem(
                item = item,
                reasonType = AdFilterReasonType.BLOCKED_UP,
                matchedText = upMid.toString()
            )
            Logger.d(TAG, "🚫 拉黑UP主[MID]: $upMid - $title")
            return false
        }
        
        // 3️⃣ 检测广告/推广关键词
        if (config.filterSponsored) {
            val keyword = AD_KEYWORDS.firstOrNull { title.contains(it, ignoreCase = true) }
            if (keyword != null) {
                filteredCount++
                recordFilteredItem(
                    item = item,
                    reasonType = AdFilterReasonType.SPONSORED,
                    matchedText = keyword
                )
                Logger.d(TAG, "🚫 过滤广告: $title (UP: $upName)")
                return false
            }
        }
        
        // 4️⃣ 检测标题党
        if (config.filterClickbait) {
            val keyword = CLICKBAIT_KEYWORDS.firstOrNull { title.contains(it, ignoreCase = true) }
            if (keyword != null) {
                filteredCount++
                recordFilteredItem(
                    item = item,
                    reasonType = AdFilterReasonType.CLICKBAIT,
                    matchedText = keyword
                )
                Logger.d(TAG, "🚫 过滤标题党: $title")
                return false
            }
        }
        
        // 5️⃣ 检测自定义屏蔽关键词
        if (config.blockedKeywords.isNotEmpty()) {
            for (keyword in config.blockedKeywords) {
                if (keyword.isNotBlank() && title.contains(keyword, ignoreCase = true)) {
                    filteredCount++
                    recordFilteredItem(
                        item = item,
                        reasonType = AdFilterReasonType.CUSTOM_KEYWORD,
                        matchedText = keyword
                    )
                    Logger.d(TAG, "🚫 自定义屏蔽: $title (关键词: $keyword)")
                    return false
                }
            }
        }
        
        // 6️⃣ 过滤低质量视频（播放量过低）
        if (config.filterLowQuality && viewCount > 0 && viewCount < config.minViewCount) {
            filteredCount++
            recordFilteredItem(
                item = item,
                reasonType = AdFilterReasonType.LOW_VIEW,
                matchedText = "${viewCount} 播放"
            )
            Logger.d(TAG, "🚫 低播放量: $title (播放: $viewCount)")
            return false
        }
        
        return true
    }
    
    /**
     *  检查UP主名称是否在拉黑列表中
     * 支持：精确匹配、模糊匹配(contains)、简繁体转换
     */
    private fun isUpNameBlocked(upName: String): Boolean {
        return findBlockedUpName(upName) != null
    }

    private fun findBlockedUpName(upName: String): String? {
        val normalizedUpName = normalizeChineseChars(upName.lowercase())
        
        return config.blockedUpNames.firstOrNull { blockedName ->
            val normalizedBlocked = normalizeChineseChars(blockedName.lowercase())
            
            // 精确匹配（忽略大小写和简繁体）
            normalizedUpName == normalizedBlocked ||
            // 模糊匹配：UP名包含拉黑词
            normalizedUpName.contains(normalizedBlocked) ||
            // 模糊匹配：拉黑词包含UP名
            normalizedBlocked.contains(normalizedUpName)
        }
    }

    private fun recordFilteredItem(
        item: VideoItem,
        reasonType: AdFilterReasonType,
        matchedText: String
    ) {
        val record = buildAdFilterRecord(
            item = item,
            reasonType = reasonType,
            matchedText = matchedText
        )
        ioScope.launch {
            runCatching {
                AdFilterInsightStore.appendRecord(
                    PluginManager.getContext(),
                    enrichAdFilterRecordUpProfile(record)
                )
            }.onFailure { error ->
                Logger.w(TAG, "记录过滤历史失败: ${error.message}")
            }
        }
    }

    private suspend fun enrichAdFilterRecordUpProfile(record: AdFilterRecord): AdFilterRecord {
        if (record.upFaceUrl.isNotBlank() || record.upMid <= 0L) return record
        val profile = fetchAdFilterUpProfileByMid(
            mid = record.upMid,
            fallbackName = record.upName
        ) ?: return record
        return record.copy(
            upName = record.upName.ifBlank { profile.name },
            upFaceUrl = profile.faceUrl,
            upMid = record.upMid.takeIf { it > 0L } ?: profile.mid
        )
    }
    
    /**
     *  简繁体字符转换表
     * 常用字符的简体→繁体映射，方便双向比较
     */
    private val SIMPLIFIED_TO_TRADITIONAL = mapOf(
        '说' to '說', '话' to '話', '语' to '語', '请' to '請', '让' to '讓',
        '这' to '這', '那' to '那', '哪' to '哪', '谁' to '誰', '什' to '什',
        '时' to '時', '间' to '間', '门' to '門', '网' to '網', '电' to '電',
        '视' to '視', '频' to '頻', '机' to '機', '会' to '會', '员' to '員',
        '学' to '學', '习' to '習', '写' to '寫', '画' to '畫', '图' to '圖',
        '书' to '書', '读' to '讀', '听' to '聽', '看' to '看', '见' to '見',
        '现' to '現', '发' to '發', '开' to '開', '关' to '關', '头' to '頭',
        '脑' to '腦', '乐' to '樂', '欢' to '歡', '爱' to '愛', '国' to '國',
        '华' to '華', '东' to '東', '车' to '車', '马' to '馬', '鸟' to '鳥'
    )
    
    /**
     * 将字符串中的繁体字统一转换为简体字（用于比较）
     */
    private fun normalizeChineseChars(text: String): String {
        val traditionalToSimplified = SIMPLIFIED_TO_TRADITIONAL.entries.associate { it.value to it.key }
        return text.map { char ->
            traditionalToSimplified[char] ?: char
        }.joinToString("")
    }
    
    //  公开方法：添加UP主到拉黑列表
    fun blockUploader(name: String, mid: Long) {
        if (name.isNotBlank() && !config.blockedUpNames.contains(name)) {
            config = config.copy(blockedUpNames = config.blockedUpNames + name)
        }
        if (mid > 0 && !config.blockedUpMids.contains(mid)) {
            config = config.copy(blockedUpMids = config.blockedUpMids + mid)
        }
        saveConfig()
        Logger.d(TAG, "➕ 已拉黑UP主: $name (MID: $mid)")
    }
    
    //  公开方法：移除UP主拉黑
    fun unblockUploader(name: String, mid: Long) {
        config = config.copy(
            blockedUpNames = config.blockedUpNames - name,
            blockedUpMids = config.blockedUpMids - mid
        )
        saveConfig()
        Logger.d(TAG, "➖ 已解除拉黑: $name (MID: $mid)")
    }
    
    private fun saveConfig() {
        ioScope.launch {
            try {
                val context = PluginManager.getContext()
                PluginStore.setConfigJson(context, id, Json.encodeToString(config))
            } catch (e: Exception) {
                Logger.e(TAG, "保存配置失败", e)
            }
        }
    }
    
    private suspend fun loadConfigSuspend() {
        try {
            val context = PluginManager.getContext()
            val jsonStr = PluginStore.getConfigJson(context, id)
            if (jsonStr != null) {
                config = Json.decodeFromString<AdFilterConfig>(jsonStr)
            }
        } catch (e: Exception) {
            Logger.e(TAG, "加载配置失败", e)
        }
    }
    
    /**
     *  同步重载配置
     * 确保每次过滤使用最新的拉黑列表
     */
    private fun reloadConfigAsync() {
        val now = System.currentTimeMillis()
        if (now - lastConfigReloadMs < 1000L) return
        lastConfigReloadMs = now
        
        ioScope.launch {
            try {
                val context = PluginManager.getContext()
                val jsonStr = PluginStore.getConfigJson(context, id)
                if (jsonStr != null) {
                    val newConfig = Json.decodeFromString<AdFilterConfig>(jsonStr)
                    // 只有配置真的变了才更新
                    if (newConfig != config) {
                        config = newConfig
                        configVersion++
                        Logger.d(TAG, " 配置已重载 v$configVersion: 拉黑UP主=${config.blockedUpNames}")
                    }
                }
            } catch (_: Exception) {
                // 静默失败，使用现有配置
            }
        }
    }
    
    @Composable
    override fun SettingsContent() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var filterSponsored by remember { mutableStateOf(config.filterSponsored) }
        var filterClickbait by remember { mutableStateOf(config.filterClickbait) }
        var filterLowQuality by remember { mutableStateOf(config.filterLowQuality) }
        var blockedUpNames by remember { mutableStateOf(config.blockedUpNames) }
        var blockedKeywords by remember { mutableStateOf(config.blockedKeywords) }
        var upListExpanded by remember { mutableStateOf(false) }
        var keywordListExpanded by remember { mutableStateOf(false) }
        var insightRecords by remember { mutableStateOf<List<AdFilterRecord>>(emptyList()) }
        var cachedUpProfiles by remember { mutableStateOf<List<AdFilterUpProfile>>(emptyList()) }
        
        // 输入对话框状态
        var showAddUpDialog by remember { mutableStateOf(false) }
        var showAddKeywordDialog by remember { mutableStateOf(false) }
        var inputText by remember { mutableStateOf("") }
        fun persistConfig(updatedConfig: AdFilterConfig) {
            config = updatedConfig
            scope.launch { PluginStore.setConfigJson(context, id, Json.encodeToString(updatedConfig)) }
        }
        
        // 加载配置
        LaunchedEffect(Unit) {
            loadConfigSuspend()
            filterSponsored = config.filterSponsored
            filterClickbait = config.filterClickbait
            filterLowQuality = config.filterLowQuality
            blockedUpNames = config.blockedUpNames
            blockedKeywords = config.blockedKeywords
            insightRecords = AdFilterInsightStore.readRecords(context)
            cachedUpProfiles = AdFilterInsightStore.readUpProfiles(context)
            cachedUpProfiles = refreshMissingAdFilterUpProfiles(
                context = context,
                records = insightRecords,
                blockedUpNames = blockedUpNames,
                cachedUpProfiles = cachedUpProfiles
            )
        }
        val insightSummary = remember(insightRecords, blockedUpNames, cachedUpProfiles) {
            resolveAdFilterInsightSummary(
                records = insightRecords,
                blockedUpNames = blockedUpNames,
                cachedUpProfiles = cachedUpProfiles
            )
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ========== 过滤开关 ==========
            
            // 商业合作过滤
            IOSSwitchItem(
                icon = CupertinoIcons.Default.Xmark,
                title = "过滤广告推广",
                subtitle = "隐藏商业合作、恰饭、推广等内容",
                checked = filterSponsored,
                onCheckedChange = { newValue ->
                    filterSponsored = newValue
                    config = config.copy(filterSponsored = newValue)
                    scope.launch { PluginStore.setConfigJson(context, id, Json.encodeToString(config)) }
                },
                iconTint = Color(0xFFE91E63)
            )
            
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
            
            // 标题党过滤
            IOSSwitchItem(
                icon = CupertinoIcons.Default.Star,
                title = "过滤标题党",
                subtitle = "隐藏震惊体、夸张标题视频",
                checked = filterClickbait,
                onCheckedChange = { newValue ->
                    filterClickbait = newValue
                    config = config.copy(filterClickbait = newValue)
                    scope.launch { PluginStore.setConfigJson(context, id, Json.encodeToString(config)) }
                },
                iconTint = Color(0xFFFF9800)
            )
            
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
            
            // 低质量过滤
            IOSSwitchItem(
                icon = CupertinoIcons.Default.Xmark,
                title = "过滤低播放量",
                subtitle = "隐藏播放量低于1000的视频",
                checked = filterLowQuality,
                onCheckedChange = { newValue ->
                    filterLowQuality = newValue
                    config = config.copy(filterLowQuality = newValue)
                    scope.launch { PluginStore.setConfigJson(context, id, Json.encodeToString(config)) }
                },
                iconTint = Color(0xFF9E9E9E)
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            AdFilterInsightPanel(summary = insightSummary)

            Spacer(modifier = Modifier.height(16.dp))
            
            AdFilterCustomListSection(
                title = "UP主拉黑",
                items = blockedUpNames,
                blockedUpProfiles = insightSummary.blockedUpProfiles,
                emptyText = "暂无拉黑的UP主",
                expanded = upListExpanded,
                icon = CupertinoIcons.Default.Person,
                itemIconTint = Color(0xFFE91E63),
                addButtonText = "添加UP主拉黑",
                onExpandedChange = { upListExpanded = it },
                onAddClick = { showAddUpDialog = true },
                onRemove = { name ->
                    blockedUpNames = removeAdFilterCustomListItem(blockedUpNames, name)
                    persistConfig(config.copy(blockedUpNames = blockedUpNames))
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            AdFilterCustomListSection(
                title = "自定义屏蔽关键词",
                items = blockedKeywords,
                emptyText = "暂无自定义屏蔽词",
                expanded = keywordListExpanded,
                icon = CupertinoIcons.Default.Tag,
                itemIconTint = MaterialTheme.colorScheme.error,
                addButtonText = "添加屏蔽关键词",
                onExpandedChange = { keywordListExpanded = it },
                onAddClick = { showAddKeywordDialog = true },
                onRemove = { keyword ->
                    blockedKeywords = removeAdFilterCustomListItem(blockedKeywords, keyword)
                    persistConfig(config.copy(blockedKeywords = blockedKeywords))
                }
            )
        }
        
        // ========== 对话框 ==========
        
        // 添加UP主对话框
        if (showAddUpDialog) {
            AlertDialog(
                onDismissRequest = { showAddUpDialog = false; inputText = "" },
                title = { Text("添加UP主拉黑") },
                text = {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        label = { Text("UP主名称") },
                        placeholder = { Text("输入UP主名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                blockedUpNames = blockedUpNames + inputText.trim()
                                persistConfig(config.copy(blockedUpNames = blockedUpNames))
                                scope.launch {
                                    cachedUpProfiles = refreshMissingAdFilterUpProfiles(
                                        context = context,
                                        records = insightRecords,
                                        blockedUpNames = blockedUpNames,
                                        cachedUpProfiles = cachedUpProfiles
                                    )
                                }
                            }
                            showAddUpDialog = false
                            inputText = ""
                        }
                    ) { Text("添加") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddUpDialog = false; inputText = "" }) { Text("取消") }
                }
            )
        }
        
        // 添加关键词对话框
        if (showAddKeywordDialog) {
            AlertDialog(
                onDismissRequest = { showAddKeywordDialog = false; inputText = "" },
                title = { Text("添加屏蔽关键词") },
                text = {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        label = { Text("关键词") },
                        placeholder = { Text("输入要屏蔽的关键词") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                blockedKeywords = blockedKeywords + inputText.trim()
                                persistConfig(config.copy(blockedKeywords = blockedKeywords))
                            }
                            showAddKeywordDialog = false
                            inputText = ""
                        }
                    ) { Text("添加") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddKeywordDialog = false; inputText = "" }) { Text("取消") }
                }
            )
        }
    }
}

private suspend fun refreshMissingAdFilterUpProfiles(
    context: Context,
    records: List<AdFilterRecord>,
    blockedUpNames: List<String>,
    cachedUpProfiles: List<AdFilterUpProfile>
): List<AdFilterUpProfile> = withContext(Dispatchers.IO) {
    val knownProfiles = resolveAdFilterKnownUpProfiles(
        records = records,
        cachedUpProfiles = cachedUpProfiles
    )
    val recordCandidates = records
        .sortedByDescending { it.timestampMs }
        .filter { record ->
            record.upName.isNotBlank() &&
                resolveAdFilterRecordUpFaceUrl(record, knownProfiles).isBlank()
        }
        .map { record -> record.upName to record.upMid }
    val blockedCandidates = blockedUpNames.map { name -> name to 0L }
    val missingCandidates = (recordCandidates + blockedCandidates)
        .distinctBy { (name, mid) ->
            if (mid > 0L) "mid:$mid" else "name:${name.lowercase()}"
        }
        .filter { (name, mid) ->
            findAdFilterProfileForRefresh(knownProfiles, name, mid)?.faceUrl.isNullOrBlank()
        }
        .take(AD_FILTER_PROFILE_REFRESH_LIMIT)

    val refreshedProfiles = missingCandidates.mapNotNull { (name, mid) ->
        when {
            mid > 0L -> fetchAdFilterUpProfileByMid(mid = mid, fallbackName = name)
            else -> fetchAdFilterUpProfileByName(name)
        }
    }
    if (refreshedProfiles.isNotEmpty()) {
        AdFilterInsightStore.upsertUpProfiles(context, refreshedProfiles)
    }
    AdFilterInsightStore.readUpProfiles(context)
}

private suspend fun fetchAdFilterUpProfileByMid(
    mid: Long,
    fallbackName: String
): AdFilterUpProfile? {
    if (mid <= 0L) return null
    val response = runCatching {
        NetworkModule.api.getUserCard(mid = mid, photo = true)
    }.getOrNull()
    val card = response?.data?.card
    if (response?.code != 0 || card == null) return null
    val name = card.name.ifBlank { fallbackName }
    if (name.isBlank() && card.face.isBlank()) return null
    return AdFilterUpProfile(
        name = name,
        faceUrl = card.face,
        mid = card.mid.toLongOrNull() ?: mid,
        updatedAtMs = System.currentTimeMillis()
    )
}

private suspend fun fetchAdFilterUpProfileByName(name: String): AdFilterUpProfile? {
    val trimmedName = name.trim()
    if (trimmedName.isBlank()) return null
    val result = SearchRepository.searchUp(keyword = trimmedName, page = 1).getOrNull()?.first.orEmpty()
    val matched = result.firstOrNull { it.uname.equals(trimmedName, ignoreCase = true) }
        ?: result.firstOrNull { item ->
            item.uname.contains(trimmedName, ignoreCase = true) ||
                trimmedName.contains(item.uname, ignoreCase = true)
        }
        ?: return null
    val searchProfile = AdFilterUpProfile(
        name = matched.uname.ifBlank { trimmedName },
        faceUrl = matched.upic,
        mid = matched.mid,
        updatedAtMs = System.currentTimeMillis()
    )
    if (searchProfile.faceUrl.isNotBlank() || searchProfile.mid <= 0L) {
        return searchProfile
    }
    return fetchAdFilterUpProfileByMid(
        mid = searchProfile.mid,
        fallbackName = searchProfile.name
    ) ?: searchProfile
}

private fun findAdFilterProfileForRefresh(
    profiles: List<AdFilterUpProfile>,
    name: String,
    mid: Long
): AdFilterUpProfile? {
    return profiles.firstOrNull { profile ->
        mid > 0L && profile.mid == mid
    } ?: profiles.firstOrNull { profile ->
        profile.name.equals(name, ignoreCase = true)
    }
}

@Composable
private fun AdFilterInsightPanel(summary: AdFilterInsightSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "过滤效果",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "展示最近实际隐藏的视频",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "${summary.totalFilteredCount} 条",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AdFilterStatTile(
                title = "UP 拉黑",
                value = "${summary.blockedUpCount}",
                modifier = Modifier.weight(1f)
            )
            AdFilterStatTile(
                title = "广告推广",
                value = "${summary.sponsoredRecords.size}",
                modifier = Modifier.weight(1f)
            )
            AdFilterStatTile(
                title = "标题党",
                value = "${summary.clickbaitRecords.size}",
                modifier = Modifier.weight(1f)
            )
            AdFilterStatTile(
                title = "低播放",
                value = "${summary.lowViewRecords.size}",
                modifier = Modifier.weight(1f)
            )
        }

        AdFilterRecordSection(
            title = "过滤广告推广",
            emptyText = "暂无广告推广过滤记录",
            records = summary.sponsoredRecords,
            upProfiles = summary.upProfiles
        )
        AdFilterRecordSection(
            title = "过滤标题党",
            emptyText = "暂无标题党过滤记录",
            records = summary.clickbaitRecords,
            upProfiles = summary.upProfiles
        )
        AdFilterRecordSection(
            title = "过滤低播放量",
            emptyText = "暂无低播放量过滤记录",
            records = summary.lowViewRecords,
            upProfiles = summary.upProfiles
        )
        AdFilterRecordSection(
            title = "自定义关键词",
            emptyText = "暂无关键词过滤记录",
            records = summary.customKeywordRecords,
            upProfiles = summary.upProfiles
        )
    }
}

@Composable
private fun AdFilterStatTile(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun AdFilterRecordSection(
    title: String,
    emptyText: String,
    records: List<AdFilterRecord>,
    upProfiles: List<AdFilterUpProfile>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.58f))
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                records.take(3).forEach { record ->
                    AdFilterRecordCard(
                        record = record,
                        upFaceUrl = resolveAdFilterRecordUpFaceUrl(record, upProfiles)
                    )
                }
            }
        }
    }
}

@Composable
private fun AdFilterRecordCard(
    record: AdFilterRecord,
    upFaceUrl: String
) {
    val context = LocalContext.current
    var showDetailDialog by remember(record.timestampMs, record.bvid, record.videoTitle) {
        mutableStateOf(false)
    }
    if (showDetailDialog) {
        AdFilterRecordDetailDialog(
            record = record,
            onDismiss = { showDetailDialog = false }
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .combinedClickable(
                onClick = {},
                onLongClick = { showDetailDialog = true }
            )
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(FormatUtils.fixImageUrl(record.videoCoverUrl))
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(92.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = record.videoTitle.ifBlank { "未知视频" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(FormatUtils.fixImageUrl(upFaceUrl))
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Text(
                    text = record.upName.ifBlank { "未知 UP" },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AdFilterChip(text = record.reasonLabel)
                if (record.matchedText.isNotBlank()) {
                    AdFilterChip(text = record.matchedText)
                }
            }
            Text(
                text = "播放 ${FormatUtils.formatStat(record.viewCount.toLong())}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AdFilterRecordDetailDialog(
    record: AdFilterRecord,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("过滤详情") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AdFilterDetailLine("视频", record.videoTitle.ifBlank { "未知视频" })
                AdFilterDetailLine("UP 主", record.upName.ifBlank { "未知 UP" })
                AdFilterDetailLine("过滤类型", record.reasonLabel)
                if (record.matchedText.isNotBlank()) {
                    AdFilterDetailLine("命中内容", record.matchedText)
                }
                AdFilterDetailLine("播放量", FormatUtils.formatStat(record.viewCount.toLong()))
                AdFilterDetailLine("过滤时间", formatAdFilterEventTime(record.timestampMs))
                if (record.bvid.isNotBlank()) {
                    AdFilterDetailLine("BVID", record.bvid)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("知道了")
            }
        }
    )
}

@Composable
private fun AdFilterDetailLine(
    label: String,
    value: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AdFilterChip(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun AdFilterListItemAvatar(
    icon: ImageVector,
    tint: Color,
    faceUrl: String
) {
    val context = LocalContext.current
    if (faceUrl.isNotBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(FormatUtils.fixImageUrl(faceUrl))
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    } else {
        Icon(
            icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun AdFilterCustomListSection(
    title: String,
    items: List<String>,
    blockedUpProfiles: List<AdFilterBlockedUpProfile> = emptyList(),
    emptyText: String,
    expanded: Boolean,
    icon: ImageVector,
    itemIconTint: Color,
    addButtonText: String,
    onExpandedChange: (Boolean) -> Unit,
    onAddClick: () -> Unit,
    onRemove: (String) -> Unit
) {
    val summary = remember(items, emptyText) {
        resolveAdFilterCustomListSummary(items = items, emptyText = emptyText)
    }
    val visibleItems = remember(items, expanded) {
        resolveAdFilterCustomListVisibleItems(items = items, expanded = expanded)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { onExpandedChange(!expanded) }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f)
            ) {
                Text(
                    text = summary.countText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Icon(
                imageVector = if (expanded) {
                    CupertinoIcons.Default.ChevronUp
                } else {
                    CupertinoIcons.Default.ChevronDown
                },
                contentDescription = if (expanded) "收起$title" else "展开$title",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f),
                modifier = Modifier
                    .padding(start = 6.dp)
                    .size(18.dp)
            )
        }

        Text(
            text = summary.previewText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = if (summary.hiddenCountText == null) 0.dp else 2.dp)
        )

        if (!expanded) {
            summary.hiddenCountText?.let { hiddenCountText ->
                Text(
                    text = hiddenCountText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
                )
            }
        }

        if (expanded && visibleItems.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                visibleItems.forEach { item ->
                    val blockedUpProfile = blockedUpProfiles.firstOrNull { it.name == item }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AdFilterListItemAvatar(
                            icon = icon,
                            tint = itemIconTint,
                            faceUrl = blockedUpProfile?.faceUrl.orEmpty()
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (blockedUpProfile != null && blockedUpProfile.filteredCount > 0) {
                                Text(
                                    text = "已过滤 ${blockedUpProfile.filteredCount} 条",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(
                            onClick = { onRemove(item) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                CupertinoIcons.Default.Xmark,
                                contentDescription = "移除$item",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onAddClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(CupertinoIcons.Default.Plus, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(addButtonText)
        }
    }
}

/**
 * 去广告配置 v2.0
 */
@Serializable
data class AdFilterConfig(
    // 基础过滤开关
    val filterSponsored: Boolean = true,    // 过滤广告推广
    val filterClickbait: Boolean = true,    // 过滤标题党
    val filterLowQuality: Boolean = false,  // 过滤低质量
    val minViewCount: Int = 1000,           // 最低播放量
    
    // UP主拉黑
    val blockedUpNames: List<String> = emptyList(),  // 拉黑UP主名称
    val blockedUpMids: List<Long> = emptyList(),     // 拉黑UP主MID
    
    // 自定义关键词
    val blockedKeywords: List<String> = emptyList()  // 自定义屏蔽词
)

private fun formatAdFilterEventTime(timestampMs: Long): String {
    return Instant.ofEpochMilli(timestampMs)
        .atZone(ZoneId.systemDefault())
        .format(AD_FILTER_EVENT_TIME_FORMATTER)
}
