// 文件路径: feature/plugin/SponsorBlockPlugin.kt
package com.android.purebilibili.feature.plugin

import coil3.request.crossfade
import com.android.purebilibili.core.ui.components.AppHorizontalDivider

import com.android.purebilibili.core.ui.components.AppSegmentOption
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
//  Material Icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import com.android.purebilibili.core.ui.AppAlertDialog
import com.android.purebilibili.core.ui.components.AppButton
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.components.AppOutlinedTextField
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.AppTextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.android.purebilibili.core.plugin.PlayerPluginApi
import com.android.purebilibili.core.plugin.PluginCapability
import com.android.purebilibili.core.plugin.PluginCapabilityManifest
import com.android.purebilibili.core.plugin.PluginManager
import com.android.purebilibili.core.plugin.PluginStore
import com.android.purebilibili.core.plugin.SkipAction
import com.android.purebilibili.core.ui.components.*
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.core.util.FormatUtils
import com.android.purebilibili.core.util.rememberNotificationPermissionState
import com.android.purebilibili.data.model.response.SponsorBlockMarkerMode
import com.android.purebilibili.data.model.response.SponsorSegment
import com.android.purebilibili.data.model.response.SponsorProgressMarker
import com.android.purebilibili.data.repository.SponsorBlockRepository
import com.android.purebilibili.feature.settings.SettingsSingleChoicePreference
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.io.File
import java.io.FileOutputStream
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel

private const val TAG = "SponsorBlockPlugin"
const val SPONSOR_BLOCK_PLUGIN_ID = "sponsor_block"
private const val SPONSOR_BLOCK_RECENT_COVER_ASPECT_RATIO = 16f / 10f
private val PLUGIN_EVENT_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

internal fun normalizeSponsorSegments(
    segments: List<SponsorSegment>
): List<SponsorSegment> {
    return segments
        .asSequence()
        .filter { segment -> segment.endTimeMs > segment.startTimeMs }
        .groupBy { segment -> "${segment.category}:${segment.actionType}" }
        .values
        .mapNotNull { candidates ->
            candidates.maxWithOrNull(
                compareBy<SponsorSegment> { it.locked }
                    .thenBy { it.votes }
                    .thenByDescending { it.startTimeMs }
                    .thenByDescending { it.endTimeMs }
            )
        }
        .sortedWith(compareBy<SponsorSegment> { it.startTimeMs }.thenBy { it.endTimeMs })
        .toList()
}

internal fun resolveSponsorProgressMarkers(
    segments: List<SponsorSegment>,
    markerMode: SponsorBlockMarkerMode,
    categoryColors: Map<String, String> = emptyMap(),
): List<SponsorProgressMarker> {
    if (markerMode == SponsorBlockMarkerMode.OFF) return emptyList()
    return segments.asSequence()
        .filter { segment ->
            when (markerMode) {
                SponsorBlockMarkerMode.OFF -> false
                SponsorBlockMarkerMode.SPONSOR_ONLY -> segment.category == com.android.purebilibili.data.model.response.SponsorCategory.SPONSOR
                SponsorBlockMarkerMode.ALL_SKIPPABLE -> true
            }
        }
        .map { segment ->
            SponsorProgressMarker(
                segmentId = segment.UUID,
                category = segment.category,
                startTimeMs = segment.startTimeMs,
                endTimeMs = segment.endTimeMs,
                colorHex = categoryColors[segment.category],
            )
        }
        .toList()
}

internal fun resetSkippedSegmentsForSeek(
    segments: List<SponsorSegment>,
    skippedIds: Set<String>,
    seekPositionMs: Long
): Set<String> {
    return skippedIds.filterTo(mutableSetOf()) { skippedId ->
        val segment = segments.firstOrNull { it.UUID == skippedId } ?: return@filterTo true
        seekPositionMs > segment.endTimeMs
    }
}

internal data class SponsorBlockAboutItemModel(
    val title: String,
    val subtitle: String?,
    val value: String?
)

internal fun resolveSponsorBlockAboutItemModel(): SponsorBlockAboutItemModel {
    return SponsorBlockAboutItemModel(
        title = "关于空降助手",
        subtitle = "BilibiliSponsorBlock",
        value = null
    )
}

/**
 *  空降助手插件
 * 
 * 基于 SponsorBlock 数据库自动跳过视频中的广告、赞助、片头片尾等片段。
 */
class SponsorBlockPlugin : PlayerPluginApi {
    
    override val id = SPONSOR_BLOCK_PLUGIN_ID
    override val name = "空降助手"
    override val description = "自动跳过视频中的广告、赞助、片头片尾等片段"
    override val version = "1.1.1"
    override val author = "BiliPai项目组"
    override val icon: ImageVector = Icons.Outlined.Send
    override val capabilityManifest: PluginCapabilityManifest = PluginCapabilityManifest(
        pluginId = id,
        displayName = name,
        version = version,
        apiVersion = 1,
        entryClassName = "com.android.purebilibili.feature.plugin.SponsorBlockPlugin",
        capabilities = setOf(
            PluginCapability.PLAYER_STATE,
            PluginCapability.PLAYER_CONTROL,
            PluginCapability.NETWORK
        )
    )
    
    // 当前视频的跳过片段
    private var segments: List<SponsorSegment> = emptyList()
    private var progressMarkers: List<SponsorProgressMarker> = emptyList()
    private var nextSegmentIndex: Int = 0
    private var activeSegment: SponsorSegment? = null
    
    // 已跳过的片段 UUID（防止重复跳过）
    private val skippedIds = mutableSetOf<String>()
    
    // 配置
    private var config: SponsorBlockConfig = SponsorBlockConfig()
    
    override suspend fun onEnable() {
        Logger.d(TAG, " 空降助手已启用")
    }
    
    override suspend fun onDisable() {
        segments = emptyList()
        progressMarkers = emptyList()
        skippedIds.clear()
        nextSegmentIndex = 0
        activeSegment = null
        Logger.d(TAG, "🔴 空降助手已禁用")
    }
    
    override suspend fun onVideoLoad(bvid: String, cid: Long) {
        // 重置状态
        segments = emptyList()
        progressMarkers = emptyList()
        skippedIds.clear()
        nextSegmentIndex = 0
        activeSegment = null
        lastPositionMs = 0
        lastAutoSkipTime = 0
        
        //  [修复] 加载配置
        loadConfigSuspend()
        
        // 加载片段数据
        try {
            segments = normalizeSponsorSegments(
                SponsorBlockRepository.getSegments(
                    bvid = bvid,
                    cid = cid,
                    categories = config.requestedCategories,
                    baseUrl = config.serverBaseUrl
                )
            ).filter { segment ->
                config.behaviorFor(segment.category) != SponsorBlockSegmentBehavior.DISABLED &&
                    segment.duration >= config.minimumSegmentDurationSeconds
            }
            progressMarkers = resolveSponsorProgressMarkers(segments, config.markerMode, config.categoryColorHex)
            Logger.d(
                TAG,
                " Loaded ${segments.size} SponsorBlock segments for $bvid, autoSkip=${config.autoSkip}, markers=${progressMarkers.size}"
            )
        } catch (e: Exception) {
            Logger.w(TAG, " 加载片段失败: ${e.message}")
        }
    }
    
    // 记录上次播放位置，用于检测回拉
    private var lastPositionMs: Long = 0
    // 记录上次自动跳过的时间，用于防止跳过后的瞬间回拉误判
    private var lastAutoSkipTime: Long = 0
    
    override suspend fun onPositionUpdate(positionMs: Long): SkipAction? {
        if (segments.isEmpty()) return SkipAction.None
        
        // [修复] 检测用户回拉进度条
        // 增加防抖逻辑：如果是自动跳过后的 3 秒内，不进行回拉检测，且不更新 lastPositionMs（防止被异常值污染）
        val isGracePeriod = System.currentTimeMillis() - lastAutoSkipTime < 3000
        
        if (!isGracePeriod) {
            if (positionMs < lastPositionMs - 2000) {  // 回拉超过2秒
                val nextSkippedIds =
                    resetSkippedSegmentsForSeek(
                        segments = segments,
                        skippedIds = skippedIds.toSet(),
                        seekPositionMs = positionMs
                    )
                skippedIds.clear()
                skippedIds.addAll(nextSkippedIds)
                nextSegmentIndex = findCandidateSegmentIndex(positionMs)
            }
            // 只有在非 Grace Period 才更新 lastPositionMs
            // 这样如果出现跳过后的瞬间 0ms/149ms 异常值，会被忽略，保留上次的高位值
            lastPositionMs = positionMs
        } else {
            // Grace Period 内，如果 positionMs 这是正常的推移（比 lastPositionMs 大），也可以更新
            // 但如果变小了（疑似 glitch），则保持 lastPositionMs 不变
            if (positionMs > lastPositionMs) {
                lastPositionMs = positionMs
            }
        }
        
        while (nextSegmentIndex < segments.size) {
            val candidate = segments[nextSegmentIndex]
            if (candidate.UUID in skippedIds || positionMs > candidate.endTimeMs) {
                nextSegmentIndex += 1
                continue
            }
            break
        }

        val segment = segments.getOrNull(nextSegmentIndex)
            ?.takeIf { candidate -> positionMs in candidate.startTimeMs..candidate.endTimeMs }
            ?: run {
                activeSegment = null
                return SkipAction.None
        }
        activeSegment = segment
        val behavior = config.behaviorFor(segment.category)
        if (behavior == SponsorBlockSegmentBehavior.DISABLED) {
            skippedIds.add(segment.UUID)
            nextSegmentIndex += 1
            activeSegment = null
            return SkipAction.None
        }
        
        when {
            segment.isMuteType -> {
                skippedIds.add(segment.UUID)
                nextSegmentIndex += 1
                activeSegment = null
                return SkipAction.Mute(
                    untilMs = segment.endTimeMs,
                    reason = "已静音: ${segment.categoryName}",
                    segmentId = segment.UUID,
                    showToast = config.skipToastEnabled,
                )
            }
            segment.isMarkerOnlyType -> {
                skippedIds.add(segment.UUID)
                nextSegmentIndex += 1
                activeSegment = null
                return SkipAction.None
            }
            else -> when (behavior) {
            SponsorBlockSegmentBehavior.AUTOMATIC -> {
            skippedIds.add(segment.UUID)
            lastAutoSkipTime = System.currentTimeMillis() // 记录跳过时间
            nextSegmentIndex += 1
            activeSegment = null
            //  记录空降助手跳过事件
            com.android.purebilibili.core.util.AnalyticsHelper.logSponsorBlockSkip(
                videoId = segment.UUID,
                segmentType = segment.categoryName
            )
            return SkipAction.SkipTo(
                positionMs = segment.endTimeMs,
                reason = "已跳过: ${segment.categoryName}",
                segmentId = segment.UUID,
                startMs = segment.startTimeMs,
                category = segment.category,
                categoryName = segment.categoryName,
                showToast = config.skipToastEnabled,
            )
            }
            SponsorBlockSegmentBehavior.SKIP_ONCE -> {
                skippedIds.add(segment.UUID)
                nextSegmentIndex += 1
                activeSegment = null
                return SkipAction.SkipTo(
                    positionMs = segment.endTimeMs,
                    reason = "本次跳过: ${segment.categoryName}",
                    segmentId = segment.UUID,
                    startMs = segment.startTimeMs,
                    category = segment.category,
                    categoryName = segment.categoryName,
                    showToast = config.skipToastEnabled,
                )
            }
            SponsorBlockSegmentBehavior.MANUAL -> {
                Logger.d(TAG, "🔘 显示跳过按钮: ${segment.categoryName}")
                return SkipAction.ShowButton(
                    skipToMs = segment.endTimeMs,
                    label = "跳过${segment.categoryName}",
                    segmentId = segment.UUID
                )
            }
            SponsorBlockSegmentBehavior.MARKER_ONLY,
            SponsorBlockSegmentBehavior.DISABLED -> {
                skippedIds.add(segment.UUID)
                nextSegmentIndex += 1
                activeSegment = null
                return SkipAction.None
            }
            }
        }
    }

    override fun onUserSeek(positionMs: Long) {
        val nextSkippedIds =
            resetSkippedSegmentsForSeek(
                segments = segments,
                skippedIds = skippedIds.toSet(),
                seekPositionMs = positionMs
            )
        skippedIds.clear()
        skippedIds.addAll(nextSkippedIds)
        nextSegmentIndex = findCandidateSegmentIndex(positionMs)
        activeSegment = segments.getOrNull(nextSegmentIndex)
            ?.takeIf { segment -> positionMs in segment.startTimeMs..segment.endTimeMs }
        lastPositionMs = positionMs
        if (positionMs >= 0L) {
            lastAutoSkipTime = 0L
        }
    }
    
    /** 手动跳过时调用，标记片段已跳过 */
    fun markAsSkipped(segmentId: String): SponsorSegment? {
        skippedIds.add(segmentId)
        val segment = segments.firstOrNull { it.UUID == segmentId }
        Logger.d(TAG, " 手动跳过完成: $segmentId")
        return segment
    }

    fun getProgressMarkers(): List<SponsorProgressMarker> = progressMarkers
    fun getActiveSegment(): SponsorSegment? = activeSegment
    fun isCommunityContributionEnabled(): Boolean = config.communityContributionEnabled
    fun getCommunityServerBaseUrl(): String = config.serverBaseUrl

    /** Sends an optional, explicitly consented community view ping for a skipped segment. */
    suspend fun uploadViewedSegmentIfEnabled(segmentId: String) {
        if (!shouldUploadSponsorBlockView(config) || segmentId.isBlank()) return
        SponsorBlockRepository.uploadViewedSegment(
            baseUrl = config.serverBaseUrl,
            segmentId = segmentId
        ).onFailure { error ->
            Logger.w(TAG, "空降助手社区跳过记录上传失败: ${error.message}")
        }
    }

    /**
     * Submits a user-confirmed segment to the configured BilibiliSponsorBlock-compatible server.
     * Callers must collect the time range from an explicit playback UI action.
     */
    suspend fun submitCommunitySegment(
        bvid: String,
        cid: Long,
        videoDurationSeconds: Float,
        startMs: Long,
        endMs: Long,
        category: String,
        actionType: String
    ): Result<List<SponsorSegment>> {
        if (!config.communityContributionEnabled) {
            return Result.failure(IllegalStateException("请先在空降助手设置中允许提交社区片段"))
        }
        if (bvid.isBlank() || endMs <= startMs || startMs < 0L) {
            return Result.failure(IllegalArgumentException("片段时间范围无效"))
        }
        if (category !in com.android.purebilibili.data.model.response.SponsorCategory.ALL_CATEGORIES) {
            return Result.failure(IllegalArgumentException("不支持的片段类别"))
        }
        if (actionType !in sponsorBlockAllowedActionTypes(category)) {
            return Result.failure(IllegalArgumentException("该类别不支持所选动作"))
        }
        return SponsorBlockRepository.submitSegments(
            baseUrl = config.serverBaseUrl,
            bvid = bvid,
            cid = cid,
            userId = config.userId,
            videoDurationSeconds = videoDurationSeconds,
            segments = listOf(
                SponsorBlockRepository.SegmentSubmission(
                    segment = listOf(startMs / 1000f, endMs / 1000f),
                    category = category,
                    actionType = actionType,
                )
            )
        )
    }

    suspend fun voteOnCommunitySegment(segmentId: String, voteType: Int): Result<Unit> {
        if (segmentId.isBlank()) return Result.failure(IllegalArgumentException("片段标识无效"))
        return SponsorBlockRepository.voteOnSegment(config.serverBaseUrl, config.userId, segmentId, voteType = voteType)
    }

    private fun findCandidateSegmentIndex(positionMs: Long): Int {
        val index = segments.indexOfFirst { segment -> positionMs <= segment.endTimeMs }
        return if (index >= 0) index else segments.size
    }
    
    override fun onVideoEnd() {
        segments = emptyList()
        progressMarkers = emptyList()
        skippedIds.clear()
        lastPositionMs = 0
        nextSegmentIndex = 0
        activeSegment = null
    }

    /**  suspend版本的配置加载 */
    private suspend fun loadConfigSuspend() {
        try {
            val context = PluginManager.getContext()
            val jsonStr = PluginStore.getConfigJson(context, id)
            if (jsonStr != null) {
                config = Json.decodeFromString<SponsorBlockConfig>(jsonStr).normalized()
            } else {
                //  没有保存的配置时，使用默认值
                config = SponsorBlockConfig(autoSkip = true)
            }
            Logger.d(TAG, "Loaded SponsorBlock config: autoSkip=${config.autoSkip}, markerMode=${config.markerMode}")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to load config", e)
            config = SponsorBlockConfig(autoSkip = true)
        }
    }
    
    @Composable
    override fun SettingsContent() {
        SponsorBlockSettingsContent(modifier = Modifier.padding(16.dp))
    }

    @Composable
    override fun SettingsContent(modifier: Modifier) {
        SponsorBlockSettingsContent(modifier = modifier)
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun SponsorBlockSettingsContent(modifier: Modifier) {
        val context = LocalContext.current
        val uriHandler = LocalUriHandler.current
        val scope = rememberCoroutineScope()
        var autoSkip by remember { mutableStateOf(config.autoSkip) }
        var markerMode by remember { mutableStateOf(config.markerMode) }
        var dailySummaryNotificationEnabled by remember { mutableStateOf(config.dailySummaryNotificationEnabled) }
        var dailySummaryNotificationPrefix by remember { mutableStateOf(config.dailySummaryNotificationPrefix) }
        var categorySettings by remember { mutableStateOf(resolveSponsorBlockCategorySettings(config)) }
        var communityTrackingEnabled by remember { mutableStateOf(config.communityTrackingEnabled) }
        var communityContributionEnabled by remember { mutableStateOf(config.communityContributionEnabled) }
        var skipToastEnabled by remember { mutableStateOf(config.skipToastEnabled) }
        var serverStatus by remember { mutableStateOf<SponsorBlockRepository.ServerStatus?>(null) }
        var communityUserInfo by remember { mutableStateOf<SponsorBlockRepository.CommunityUserInfo?>(null) }
        var showServerDialog by remember { mutableStateOf(false) }
        var serverDraft by remember { mutableStateOf(config.serverBaseUrl) }
        var showDurationDialog by remember { mutableStateOf(false) }
        var durationDraft by remember { mutableStateOf(config.minimumSegmentDurationSeconds.toString()) }
        var showCommunityUserDialog by remember { mutableStateOf(false) }
        var showRegenerateUserIdDialog by remember { mutableStateOf(false) }
        var userIdDraft by remember { mutableStateOf(config.userId) }
        var colorDialogCategory by remember { mutableStateOf<SponsorBlockCategorySetting?>(null) }
        var colorDraft by remember { mutableStateOf("") }
        var insightRecords by remember { mutableStateOf<List<SponsorBlockSkipRecord>>(emptyList()) }
        val aboutItem = remember { resolveSponsorBlockAboutItemModel() }
        val markerOptions = remember {
            SponsorBlockMarkerMode.entries.map { mode ->
                AppSegmentOption(
                    value = mode,
                    label = mode.label
                )
            }
        }
        val insightSummary = remember(insightRecords) {
            resolveSponsorBlockInsightSummary(
                records = insightRecords,
                dayStartMs = currentLocalDayStartMs()
            )
        }
        fun persistConfig(nextConfig: SponsorBlockConfig) {
            config = nextConfig.normalized()
            scope.launch {
                PluginStore.setConfigJson(context, id, Json.encodeToString(config))
                scheduleSponsorBlockDailySummary(context, config.dailySummaryNotificationEnabled)
            }
        }
        fun showNotificationToast(message: String) {
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
        fun sendTestNotification() {
            val posted = postSponsorBlockTestNotification(context, config)
            showNotificationToast(if (posted) "测试通知已发送" else "系统通知未开启")
        }
        val notificationPermission = rememberNotificationPermissionState { granted ->
            if (granted) {
                sendTestNotification()
            } else {
                showNotificationToast("通知权限未开启")
            }
        }
        
        // 加载配置
        LaunchedEffect(Unit) {
            loadConfigSuspend()
            autoSkip = config.autoSkip
            markerMode = config.markerMode
            dailySummaryNotificationEnabled = config.dailySummaryNotificationEnabled
            dailySummaryNotificationPrefix = config.dailySummaryNotificationPrefix
            categorySettings = resolveSponsorBlockCategorySettings(config)
            communityTrackingEnabled = config.communityTrackingEnabled
            communityContributionEnabled = config.communityContributionEnabled
            skipToastEnabled = config.skipToastEnabled
            userIdDraft = config.userId
            serverDraft = config.serverBaseUrl
            durationDraft = config.minimumSegmentDurationSeconds.toString()
            insightRecords = SponsorBlockInsightStore.readRecords(context)
            scheduleSponsorBlockDailySummary(context, config.dailySummaryNotificationEnabled)
        }
        
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SponsorBlockInsightPanel(
                summary = insightSummary,
                onShareClick = { shareSponsorBlockInsightImage(context, insightSummary) }
            )

            SponsorBlockSettingsSection(title = "片段规则", subtitle = "按类别选择总是跳过、本次跳过、手动跳过、仅显示或禁用") {
                AppSwitchPreference(
                    icon = Icons.Outlined.Bolt,
                    title = "全部自动跳过",
                    subtitle = "关闭后，所有类别先改为显示跳过按钮",
                    checked = autoSkip,
                    onCheckedChange = { newValue ->
                        autoSkip = newValue
                        val behavior = if (newValue) SponsorBlockSegmentBehavior.AUTOMATIC else SponsorBlockSegmentBehavior.MANUAL
                        persistConfig(config.copy(autoSkip = newValue, categoryBehaviorRaw = config.categoryBehaviorRaw.mapValues { behavior.name }))
                        categorySettings = resolveSponsorBlockCategorySettings(config)
                    },
                    iconTint = MaterialTheme.colorScheme.tertiary,
                )
                SponsorBlockCategorySettingsSection(
                    settings = categorySettings,
                    onBehaviorChange = { category, behavior ->
                        persistConfig(config.copy(categoryBehaviorRaw = config.categoryBehaviorRaw + (category to behavior.name)))
                        categorySettings = resolveSponsorBlockCategorySettings(config)
                        autoSkip = categorySettings.all { it.behavior == SponsorBlockSegmentBehavior.AUTOMATIC }
                    },
                    onColorClick = { setting ->
                        colorDialogCategory = setting
                        colorDraft = config.categoryColorHex[setting.category] ?: setting.defaultColorHex
                    },
                )
                AppPreference(
                    icon = Icons.Outlined.Timer,
                    title = "最短片段时长",
                    subtitle = "短于该时长的社区片段不会参与跳过或提示",
                    value = "${config.minimumSegmentDurationSeconds}s",
                    onClick = { showDurationDialog = true },
                    iconTint = MaterialTheme.colorScheme.tertiary,
                )
            }

            SponsorBlockSettingsSection(title = "进度条") {
                SettingsSingleChoicePreference(
                    title = "进度条提示：${markerMode.label}",
                    subtitle = "关闭、仅提示恰饭，或显示全部可跳过片段",
                    options = markerOptions,
                    selectedValue = markerMode,
                    onSelectionChange = { newValue ->
                        markerMode = newValue
                        persistConfig(config.copy(markerModeRaw = newValue.name))
                        progressMarkers = resolveSponsorProgressMarkers(segments, newValue, config.categoryColorHex)
                    },
                )
            }

            SponsorBlockSettingsSection(title = "通知") {
                AppSwitchPreference(
                    icon = Icons.Outlined.Info,
                    title = "跳过提示",
                    subtitle = "自动跳过时显示简短提示",
                    checked = skipToastEnabled,
                    onCheckedChange = { enabled ->
                        skipToastEnabled = enabled
                        persistConfig(config.copy(skipToastEnabled = enabled))
                    },
                    iconTint = MaterialTheme.colorScheme.primary,
                )
                AppSwitchPreference(
                    icon = Icons.Outlined.Notifications,
                    title = "每日汇总通知",
                    subtitle = "当天有跳过记录时，汇总跳过次数和节省时间",
                    checked = dailySummaryNotificationEnabled,
                    onCheckedChange = { newValue ->
                        dailySummaryNotificationEnabled = newValue
                        persistConfig(config.copy(dailySummaryNotificationEnabled = newValue))
                    },
                    iconTint = MaterialTheme.colorScheme.primary,
                )
                if (dailySummaryNotificationEnabled) {
                    AppOutlinedTextField(
                        value = dailySummaryNotificationPrefix,
                        onValueChange = { nextValue ->
                            dailySummaryNotificationPrefix = nextValue
                            persistConfig(config.copy(dailySummaryNotificationPrefix = nextValue))
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        singleLine = true,
                        label = { AppText("通知文案前缀") },
                        textStyle = MaterialTheme.typography.bodyMedium,
                    )
                }
                AppPreference(
                    icon = Icons.Outlined.Notifications,
                    title = "发送测试通知",
                    subtitle = "确认通知权限和展示效果，不写入跳过记录",
                    onClick = { notificationPermission.launchWithPermission { sendTestNotification() } },
                    iconTint = MaterialTheme.colorScheme.primary,
                )
            }

            SponsorBlockSettingsSection(title = "社区与隐私", subtitle = "所有发送都指向下方服务器；默认关闭，提交片段始终需要逐次确认") {
                AppPreference(
                    icon = Icons.Outlined.Dns,
                    title = "社区服务器",
                    subtitle = serverStatus?.message ?: config.serverBaseUrl,
                    value = if (serverStatus?.reachable == true) "正常" else null,
                    onClick = { showServerDialog = true },
                    iconTint = MaterialTheme.colorScheme.secondary,
                )
                AppSwitchPreference(
                    icon = Icons.Outlined.BarChart,
                    title = "上传跳过贡献",
                    subtitle = "仅在实际跳过时上传片段匿名 UUID；不会上传连续观看轨迹",
                    checked = communityTrackingEnabled,
                    onCheckedChange = { enabled ->
                        communityTrackingEnabled = enabled
                        persistConfig(config.copy(communityTrackingEnabled = enabled))
                    },
                    iconTint = MaterialTheme.colorScheme.tertiary,
                )
                AppSwitchPreference(
                    icon = Icons.Outlined.Send,
                    title = "允许提交社区片段",
                    subtitle = "允许播放页标记起止时间；提交前会显示类别、时间和服务器供确认",
                    checked = communityContributionEnabled,
                    onCheckedChange = { enabled ->
                        communityContributionEnabled = enabled
                        persistConfig(config.copy(communityContributionEnabled = enabled))
                    },
                    iconTint = MaterialTheme.colorScheme.tertiary,
                )
                AppPreference(
                    icon = Icons.Outlined.Person,
                    title = "社区用户 ID",
                    subtitle = "${config.userId.take(8)}… · 用于归属你的片段贡献",
                    onClick = {
                        showCommunityUserDialog = true
                        scope.launch {
                            communityUserInfo = SponsorBlockRepository.getCommunityUserInfo(config.serverBaseUrl, config.userId)
                                .getOrElse { error ->
                                    showNotificationToast(error.message ?: "无法读取社区用户信息")
                                    null
                                }
                        }
                    },
                    iconTint = MaterialTheme.colorScheme.tertiary,
                )
            }

            SponsorBlockSettingsSection(title = "关于") {
                AppPreference(
                    icon = Icons.Outlined.Info,
                    title = aboutItem.title,
                    subtitle = aboutItem.subtitle,
                    value = aboutItem.value,
                    onClick = { uriHandler.openUri("https://github.com/hanydd/BilibiliSponsorBlock") },
                    iconTint = MaterialTheme.colorScheme.secondary,
                )
            }
        }

        if (showDurationDialog) {
            AppAlertDialog(
                onDismissRequest = { showDurationDialog = false },
                title = { AppText("最短片段时长") },
                text = {
                    AppTextField(
                        value = durationDraft,
                        onValueChange = { durationDraft = it },
                        label = "秒数",
                        singleLine = true
                    )
                },
                confirmButton = {
                    AppTextButton(onClick = {
                        val value = durationDraft.toFloatOrNull()
                        if (value == null || value < 0f) {
                            showNotificationToast("请输入大于或等于 0 的秒数")
                        } else {
                            persistConfig(config.copy(minimumSegmentDurationSeconds = value))
                            showDurationDialog = false
                        }
                    }) { AppText("保存") }
                },
                dismissButton = { AppTextButton(onClick = { showDurationDialog = false }) { AppText("取消") } }
            )
        }

        if (showServerDialog) {
            AppAlertDialog(
                onDismissRequest = { showServerDialog = false },
                title = { AppText("社区服务器") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppText("自定义服务器会接收片段查询、可选跳过记录和你确认提交的片段。")
                        AppTextField(
                            value = serverDraft,
                            onValueChange = { serverDraft = it },
                            label = "API 地址",
                            placeholder = "https://example.com/api",
                            singleLine = true
                        )
                        communityUserInfo?.let { info ->
                            AppText("${info.userName.ifBlank { "匿名用户" }} · 已贡献 ${info.segmentCount} 段 · 节省 ${info.minutesSaved.toInt()} 分钟")
                        }
                    }
                },
                confirmButton = {
                    AppTextButton(onClick = {
                        val normalized = normalizeSponsorBlockServerUrl(serverDraft)
                        if (normalized == null) {
                            showNotificationToast("请输入有效的 http/https API 地址")
                        } else {
                            persistConfig(config.copy(serverBaseUrl = normalized))
                            serverDraft = normalized
                            scope.launch {
                                serverStatus = SponsorBlockRepository.checkServerStatus(normalized)
                                communityUserInfo = SponsorBlockRepository.getCommunityUserInfo(normalized, config.userId)
                                    .getOrNull()
                            }
                        }
                    }) { AppText("保存并检测") }
                },
                dismissButton = {
                    AppTextButton(onClick = {
                        persistConfig(config.copy(serverBaseUrl = SponsorBlockRepository.DEFAULT_BASE_URL))
                        serverDraft = SponsorBlockRepository.DEFAULT_BASE_URL
                        showServerDialog = false
                    }) { AppText("恢复默认") }
                }
            )
        }

        if (showCommunityUserDialog) {
            AppAlertDialog(
                onDismissRequest = { showCommunityUserDialog = false },
                title = { AppText("社区用户 ID") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppText(config.userId, style = MaterialTheme.typography.bodySmall)
                        AppTextField(
                            value = userIdDraft,
                            onValueChange = { userIdDraft = it },
                            label = "用户 ID",
                            singleLine = true,
                        )
                        communityUserInfo?.let { info ->
                            AppText("${info.userName.ifBlank { "匿名用户" }} · 已贡献 ${info.segmentCount} 段 · 节省 ${info.minutesSaved.toInt()} 分钟")
                        }
                        AppText("此 ID 仅用于当前服务器上的贡献归属。重新生成后，旧贡献不会迁移。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        AppTextButton(onClick = { showRegenerateUserIdDialog = true }) { AppText("重新生成 ID") }
                    }
                },
                confirmButton = {
                    AppTextButton(onClick = {
                        validateSponsorBlockUserId(userIdDraft)?.let(::showNotificationToast) ?: run {
                            persistConfig(config.copy(userId = userIdDraft.trim()))
                            showNotificationToast("用户 ID 已保存")
                        }
                    }) { AppText("保存") }
                },
                dismissButton = {
                    AppTextButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                        clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("社区用户 ID", config.userId))
                        showNotificationToast("用户 ID 已复制")
                    }) { AppText("复制") }
                },
            )
        }

        if (showRegenerateUserIdDialog) {
            AppAlertDialog(
                onDismissRequest = { showRegenerateUserIdDialog = false },
                title = { AppText("重新生成社区用户 ID？") },
                text = { AppText("旧 ID 的贡献不会迁移到新 ID。") },
                confirmButton = {
                    AppTextButton(onClick = {
                        persistConfig(config.copy(userId = generateSponsorBlockUserId()))
                        showRegenerateUserIdDialog = false
                        showCommunityUserDialog = false
                    }) { AppText("确认重新生成") }
                },
                dismissButton = { AppTextButton(onClick = { showRegenerateUserIdDialog = false }) { AppText("取消") } },
            )
        }

        colorDialogCategory?.let { setting ->
            AppAlertDialog(
                onDismissRequest = { colorDialogCategory = null },
                title = { AppText("${setting.title}进度条颜色") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppText("输入 #RRGGBB；留空可恢复默认颜色。")
                        AppTextField(value = colorDraft, onValueChange = { colorDraft = it }, label = "颜色", singleLine = true)
                    }
                },
                confirmButton = {
                    AppTextButton(onClick = {
                        val normalized = colorDraft.trim().uppercase()
                        if (normalized.isNotBlank() && !Regex("^#[0-9A-F]{6}$").matches(normalized)) {
                            showNotificationToast("颜色格式应为 #RRGGBB")
                        } else {
                            persistConfig(config.copy(categoryColorHex = config.categoryColorHex + (setting.category to normalized)))
                            colorDialogCategory = null
                        }
                    }) { AppText("保存") }
                },
                dismissButton = { AppTextButton(onClick = { colorDialogCategory = null }) { AppText("取消") } },
            )
        }
    }
}

@Composable
private fun SponsorBlockSettingsSection(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppText(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        if (subtitle != null) {
            AppText(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AppSurface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
            shape = AppShapes.container(ContainerLevel.Card),
        ) {
            Column(content = content)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SponsorBlockCategorySettingsSection(
    settings: List<SponsorBlockCategorySetting>,
    onBehaviorChange: (String, SponsorBlockSegmentBehavior) -> Unit,
    onColorClick: (SponsorBlockCategorySetting) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(12.dp)) {
        settings.forEach { setting ->
            AppSurface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
                shape = AppShapes.container(ContainerLevel.Card),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppText(setting.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    AppText(setting.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SponsorBlockSegmentBehavior.entries.forEach { behavior ->
                            AppFilterChip(
                                selected = setting.behavior == behavior,
                                onClick = { onBehaviorChange(setting.category, behavior) },
                                label = { AppText(behavior.label) },
                            )
                        }
                    }
                    AppTextButton(onClick = { onColorClick(setting) }) { AppText("自定义进度条颜色") }
                }
            }
        }
    }
}

@Composable
private fun SponsorBlockInsightPanel(
    summary: SponsorBlockInsightSummary,
    onShareClick: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.container(ContainerLevel.Card))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
            .padding(14.dp)
    ) {
        val useWideLayout = maxWidth >= 460.dp
        if (useWideLayout) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SponsorBlockSummaryRail(
                    summary = summary,
                    onShareClick = onShareClick,
                    modifier = Modifier.widthIn(min = 156.dp, max = 190.dp)
                )
                SponsorBlockRecentSection(
                    summary = summary,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SponsorBlockSummaryHeader(summary = summary)
                SponsorBlockCompactStats(summary = summary)
                SponsorBlockPeriodStats(summary = summary)
                SponsorBlockFavoriteSection(summary = summary)
                AppButton(
                    onClick = onShareClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AppText("分享统计图片")
                }
                SponsorBlockRecentSection(summary = summary)
            }
        }
    }
}

@Composable
private fun SponsorBlockSummaryRail(
    summary: SponsorBlockInsightSummary,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SponsorBlockSummaryHeader(summary = summary)
        SponsorBlockStatTile(
            title = "今日节省",
            value = summary.todaySavedText,
            modifier = Modifier.fillMaxWidth()
        )
        SponsorBlockStatTile(
            title = "累计节省",
            value = summary.totalSavedText,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SponsorBlockStatTile(
                title = "次数",
                value = "${summary.totalSkipCount}",
                modifier = Modifier.weight(1f)
            )
            SponsorBlockStatTile(
                title = "UP",
                value = "${summary.uniqueUpCount}",
                modifier = Modifier.weight(1f)
            )
        }
        SponsorBlockPeriodStats(summary = summary)
        SponsorBlockFavoriteSection(summary = summary)
        AppButton(
            onClick = onShareClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            AppText("分享统计图片")
        }
    }
}

@Composable
private fun SponsorBlockSummaryHeader(summary: SponsorBlockInsightSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            AppText(
                text = "跳过统计",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            AppText(
                text = "记录只保存在本地",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .clip(AppShapes.container(ContainerLevel.Pill))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            AppText(
                text = "${summary.totalSkipCount} 次",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SponsorBlockCompactStats(summary: SponsorBlockInsightSummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.container(ContainerLevel.Card))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SponsorBlockCompactStatItem(
            title = "今日",
            value = summary.todaySavedText,
            modifier = Modifier.weight(1f)
        )
        SponsorBlockCompactStatItem(
            title = "累计",
            value = summary.totalSavedText,
            modifier = Modifier.weight(1f)
        )
        SponsorBlockCompactStatItem(
            title = "次数",
            value = "${summary.totalSkipCount}",
            modifier = Modifier.weight(1f)
        )
        SponsorBlockCompactStatItem(
            title = "UP",
            value = "${summary.uniqueUpCount}",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SponsorBlockCompactStatItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        AppText(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        AppText(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SponsorBlockPeriodStats(summary: SponsorBlockInsightSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.container(ContainerLevel.Card))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AppText(
            text = "频次对比",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        summary.periodStats.forEach { stat ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppText(
                    text = stat.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                AppText(
                    text = "${stat.skipCount} 次 · ${stat.uniqueVideoCount} 个视频",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SponsorBlockFavoriteSection(summary: SponsorBlockInsightSummary) {
    val topVideoText = summary.topVideo?.let {
        "${it.title.ifBlank { it.bvid }} · ${it.skipCount} 次"
    } ?: "暂无"
    val topUpText = summary.topUp?.let {
        "${it.name.ifBlank { "未知UP" }} · ${it.skipCount} 次"
    } ?: "暂无"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.container(ContainerLevel.Card))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SponsorBlockFavoriteRow(title = "最常跳过视频", value = topVideoText)
        SponsorBlockFavoriteRow(title = "最常命中 UP", value = topUpText)
    }
}

@Composable
private fun SponsorBlockFavoriteRow(title: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        AppText(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AppText(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SponsorBlockRecentSection(
    summary: SponsorBlockInsightSummary,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (summary.recentRecords.isEmpty()) {
            SponsorBlockEmptyInsight()
        } else {
            AppText(
                text = "最近跳过",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            summary.recentRecords.forEach { record ->
                SponsorBlockRecordRow(record = record)
            }
        }
    }
}

@Composable
private fun SponsorBlockStatTile(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(AppShapes.container(ContainerLevel.Card))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AppText(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AppText(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SponsorBlockEmptyInsight() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.container(ContainerLevel.Card))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.62f))
            .padding(horizontal = 14.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        AppText(
            text = "还没有跳过记录",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SponsorBlockRecordRow(record: SponsorBlockSkipRecord) {
    val context = LocalContext.current
    var showDetailDialog by remember(record.timestampMs, record.segmentId) { mutableStateOf(false) }
    if (showDetailDialog) {
        SponsorBlockRecordDetailDialog(
            record = record,
            onDismiss = { showDetailDialog = false }
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.container(ContainerLevel.Card))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            .combinedClickable(
                onClick = {},
                onLongClick = { showDetailDialog = true }
            )
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(FormatUtils.fixImageUrl(record.videoCoverUrl))
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(SPONSOR_BLOCK_RECENT_COVER_ASPECT_RATIO)
                .clip(AppShapes.container(ContainerLevel.Chip))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            AppText(
                text = record.videoTitle.ifBlank { "未知视频" },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(FormatUtils.fixImageUrl(record.upFaceUrl))
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                AppText(
                    text = record.upName.ifBlank { "未知 UP" },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SponsorBlockChip(text = record.segmentCategoryName)
                SponsorBlockChip(text = record.triggerLabel)
            }
            AppText(
                text = "${record.progressText}  ·  节省 ${record.savedText}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SponsorBlockRecordDetailDialog(
    record: SponsorBlockSkipRecord,
    onDismiss: () -> Unit
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { AppText("跳过详情") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SponsorBlockDetailLine("视频", record.videoTitle.ifBlank { "未知视频" })
                SponsorBlockDetailLine("UP 主", record.upName.ifBlank { "未知 UP" })
                SponsorBlockDetailLine("片段类型", record.segmentCategoryName)
                SponsorBlockDetailLine("来源", record.triggerLabel)
                SponsorBlockDetailLine("跳转区间", record.progressText)
                SponsorBlockDetailLine("节省时间", record.savedText)
                SponsorBlockDetailLine("跳过时间", formatPluginEventTime(record.timestampMs))
                if (record.bvid.isNotBlank()) {
                    SponsorBlockDetailLine("BVID", record.bvid)
                }
            }
        },
        confirmButton = {
            AppTextButton(onClick = onDismiss) {
                AppText("知道了")
            }
        }
    )
}

@Composable
private fun SponsorBlockDetailLine(
    label: String,
    value: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        AppText(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AppText(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SponsorBlockChip(text: String) {
    Box(
        modifier = Modifier
            .clip(AppShapes.container(ContainerLevel.Pill))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        AppText(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun shareSponsorBlockInsightImage(
    context: Context,
    summary: SponsorBlockInsightSummary
) {
    runCatching {
        val bitmap = buildSponsorBlockInsightBitmap(summary)
        val shareDir = File(context.cacheDir, "shared_images").apply { mkdirs() }
        val shareFile = File(shareDir, "sponsor_block_insight.png")
        FileOutputStream(shareFile).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            shareFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, buildSponsorBlockInsightShareText(summary))
            putExtra(Intent.EXTRA_SUBJECT, "BiliPai 空降助手统计")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "分享空降助手统计"))
    }.onFailure { error ->
        Logger.e(TAG, "分享空降助手统计图片失败: ${error.message}")
        android.widget.Toast.makeText(context, "分享图片生成失败", android.widget.Toast.LENGTH_SHORT).show()
    }
}

private fun buildSponsorBlockInsightBitmap(summary: SponsorBlockInsightSummary): Bitmap {
    val width = 1080
    val horizontalPadding = 72f
    val bitmap = Bitmap.createBitmap(width, 1480, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(AndroidColor.rgb(18, 24, 34))

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        textSize = 54f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(190, 202, 218)
        textSize = 30f
    }
    val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(255, 214, 102)
        textSize = 42f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(230, 236, 244)
        textSize = 32f
    }
    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(34, 44, 59)
    }

    var y = 96f
    canvas.drawText("BiliPai 空降助手统计", horizontalPadding, y, titlePaint)
    y += 54f
    canvas.drawText("记录只保存在本地，可一键分享", horizontalPadding, y, subtitlePaint)

    y += 58f
    drawSponsorShareCard(canvas, cardPaint, horizontalPadding, y, width - horizontalPadding, y + 190f)
    canvas.drawText("累计跳过", horizontalPadding + 34f, y + 62f, subtitlePaint)
    canvas.drawText("${summary.totalSkipCount} 次", horizontalPadding + 34f, y + 128f, valuePaint)
    canvas.drawText("累计节省 ${summary.totalSavedText}", horizontalPadding + 430f, y + 128f, valuePaint)

    y += 245f
    summary.periodStats.forEach { stat ->
        drawSponsorShareCard(canvas, cardPaint, horizontalPadding, y, width - horizontalPadding, y + 118f)
        canvas.drawText(stat.label, horizontalPadding + 30f, y + 48f, bodyPaint)
        canvas.drawText("${stat.skipCount} 次 / ${stat.uniqueVideoCount} 个视频", horizontalPadding + 260f, y + 48f, bodyPaint)
        canvas.drawText("节省 ${stat.savedText}", horizontalPadding + 260f, y + 90f, subtitlePaint)
        y += 136f
    }

    y += 18f
    val topVideo = summary.topVideo?.let {
        "最常跳过视频：${it.title.ifBlank { it.bvid }}（${it.skipCount} 次）"
    } ?: "最常跳过视频：暂无"
    val topUp = summary.topUp?.let {
        "最常命中 UP：${it.name.ifBlank { "未知UP" }}（${it.skipCount} 次，${it.uniqueVideoCount} 个视频）"
    } ?: "最常命中 UP：暂无"
    listOf(topVideo, topUp).forEach { line ->
        wrapShareText(line, bodyPaint, width - horizontalPadding * 2 - 40f).forEach { wrapped ->
            canvas.drawText(wrapped, horizontalPadding, y, bodyPaint)
            y += 44f
        }
        y += 16f
    }

    canvas.drawText("由 BiliPai 生成", horizontalPadding, 1408f, subtitlePaint)
    return bitmap
}

private fun drawSponsorShareCard(
    canvas: Canvas,
    paint: Paint,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float
) {
    canvas.drawRoundRect(left, top, right, bottom, 28f, 28f, paint)
}

private fun wrapShareText(
    text: String,
    paint: Paint,
    maxWidth: Float
): List<String> {
    if (paint.measureText(text) <= maxWidth) return listOf(text)
    val result = mutableListOf<String>()
    var current = ""
    text.forEach { char ->
        val next = current + char
        if (paint.measureText(next) > maxWidth && current.isNotEmpty()) {
            result += current
            current = char.toString()
        } else {
            current = next
        }
    }
    if (current.isNotEmpty()) result += current
    return result
}

/**
 * 空降助手配置
 */
@Serializable
data class SponsorBlockConfig(
    val autoSkip: Boolean = true,
    val markerModeRaw: String = SponsorBlockMarkerMode.SPONSOR_ONLY.name,
    val skipSponsor: Boolean = true,
    val skipIntro: Boolean = true,
    val skipOutro: Boolean = true,
    val skipInteraction: Boolean = true,
    val categoryBehaviorRaw: Map<String, String> = emptyMap(),
    val categoryColorHex: Map<String, String> = emptyMap(),
    val minimumSegmentDurationSeconds: Float = 0f,
    val skipToastEnabled: Boolean = true,
    val serverBaseUrl: String = SponsorBlockRepository.DEFAULT_BASE_URL,
    val userId: String = "",
    val communityTrackingEnabled: Boolean = false,
    val communityContributionEnabled: Boolean = false,
    val dailySummaryNotificationEnabled: Boolean = false,
    val dailySummaryNotificationPrefix: String = DEFAULT_DAILY_SUMMARY_PREFIX
) {
    val markerMode: SponsorBlockMarkerMode
        get() = com.android.purebilibili.data.model.response.resolveSponsorBlockMarkerMode(markerModeRaw)

    val requestedCategories: List<String>
        get() = com.android.purebilibili.data.model.response.SponsorCategory.ALL_CATEGORIES
            .filter { behaviorFor(it) != SponsorBlockSegmentBehavior.DISABLED }

    fun behaviorFor(category: String): SponsorBlockSegmentBehavior {
        val legacyFallback = if (autoSkip) SponsorBlockSegmentBehavior.AUTOMATIC else SponsorBlockSegmentBehavior.MANUAL
        return resolveSponsorBlockSegmentBehavior(category, categoryBehaviorRaw, legacyFallback)
    }

    fun normalized(): SponsorBlockConfig {
        val migratedBehaviors = if (categoryBehaviorRaw.isEmpty()) {
            defaultSponsorBlockCategoryBehaviors(
                autoSkip = autoSkip,
                skipSponsor = skipSponsor,
                skipIntro = skipIntro,
                skipOutro = skipOutro,
                skipInteraction = skipInteraction
            ).mapValues { it.value.name }
        } else {
            categoryBehaviorRaw.mapValues { (_, value) ->
                SponsorBlockSegmentBehavior.entries.firstOrNull { it.name == value }?.name
                    ?: SponsorBlockSegmentBehavior.MANUAL.name
            }
        }
        return copy(
            markerModeRaw = markerMode.name,
            categoryBehaviorRaw = migratedBehaviors,
            categoryColorHex = categoryColorHex.mapNotNull { (category, color) ->
                color.takeIf { category in com.android.purebilibili.data.model.response.SponsorCategory.ALL_CATEGORIES &&
                    Regex("^#[0-9a-fA-F]{6}$").matches(it) }?.let { category to it.uppercase() }
            }.toMap(),
            minimumSegmentDurationSeconds = minimumSegmentDurationSeconds.coerceAtLeast(0f),
            serverBaseUrl = normalizeSponsorBlockServerUrl(serverBaseUrl)
                ?: SponsorBlockRepository.DEFAULT_BASE_URL,
            userId = userId.ifBlank(::generateSponsorBlockUserId)
        )
    }

    companion object {
        const val DEFAULT_DAILY_SUMMARY_PREFIX = "今日空降助手已帮你节省"

        fun default(): SponsorBlockConfig = SponsorBlockConfig()
    }
}

private val SponsorBlockMarkerMode.label: String
    get() = when (this) {
        SponsorBlockMarkerMode.OFF -> "关闭"
        SponsorBlockMarkerMode.SPONSOR_ONLY -> "仅恰饭"
        SponsorBlockMarkerMode.ALL_SKIPPABLE -> "全部可跳过"
    }

private fun formatPluginEventTime(timestampMs: Long): String {
    return Instant.ofEpochMilli(timestampMs)
        .atZone(ZoneId.systemDefault())
        .format(PLUGIN_EVENT_TIME_FORMATTER)
}
