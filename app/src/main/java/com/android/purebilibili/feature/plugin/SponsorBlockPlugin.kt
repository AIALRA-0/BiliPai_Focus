// 文件路径: feature/plugin/SponsorBlockPlugin.kt
package com.android.purebilibili.feature.plugin

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
//  Cupertino Icons - iOS SF Symbols 风格图标
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.*
import io.github.alexzhirkevich.cupertino.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.platform.LocalUriHandler
import com.android.purebilibili.core.plugin.PlayerPlugin
import com.android.purebilibili.core.plugin.PluginManager
import com.android.purebilibili.core.plugin.PluginStore
import com.android.purebilibili.core.plugin.SkipAction
import com.android.purebilibili.core.ui.components.*
import com.android.purebilibili.core.util.Logger
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.data.model.response.SponsorBlockMarkerMode
import com.android.purebilibili.data.model.response.SponsorSegment
import com.android.purebilibili.data.model.response.SponsorProgressMarker
import com.android.purebilibili.data.repository.SponsorBlockRepository
import com.android.purebilibili.feature.settings.IOSSlidingSegmentedSetting
import com.android.purebilibili.feature.settings.PlaybackSegmentOption
import io.github.alexzhirkevich.cupertino.CupertinoSwitch
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.coroutines.flow.first

private const val TAG = "SponsorBlockPlugin"
const val SPONSOR_BLOCK_PLUGIN_ID = "sponsor_block"

internal fun normalizeSponsorSegments(
    segments: List<SponsorSegment>
): List<SponsorSegment> {
    return segments
        .asSequence()
        .filter { segment -> segment.isSkipType && segment.endTimeMs > segment.startTimeMs }
        .groupBy { segment -> segment.category }
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
    markerMode: SponsorBlockMarkerMode
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
                endTimeMs = segment.endTimeMs
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
class SponsorBlockPlugin : PlayerPlugin {
    
    override val id = SPONSOR_BLOCK_PLUGIN_ID
    override val name = "空降助手"
    override val description = "自动跳过视频中的广告、赞助、片头片尾等片段"
    override val version = "1.0.0"
    override val author = "YangY"
    override val icon: ImageVector = CupertinoIcons.Default.Paperplane
    
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
            segments = normalizeSponsorSegments(SponsorBlockRepository.getSegments(bvid, cid = cid))
            progressMarkers = resolveSponsorProgressMarkers(
                segments = segments,
                markerMode = config.markerMode
            )
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
        
        // 如果配置为自动跳过
        if (config.autoSkip) {
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
                reason = "已跳过: ${segment.categoryName}"
            )
        }
        
        //  [修复] 非自动跳过模式：返回 ShowButton 让 UI 显示跳过按钮
        Logger.d(TAG, "🔘 显示跳过按钮: ${segment.categoryName}")
        return SkipAction.ShowButton(
            skipToMs = segment.endTimeMs,
            label = "跳过${segment.categoryName}",
            segmentId = segment.UUID
        )
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
    fun markAsSkipped(segmentId: String) {
        skippedIds.add(segmentId)
        Logger.d(TAG, " 手动跳过完成: $segmentId")
    }

    fun getProgressMarkers(): List<SponsorProgressMarker> = progressMarkers
    fun getActiveSegment(): SponsorSegment? = activeSegment

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
        val context = LocalContext.current
        val uriHandler = LocalUriHandler.current
        val scope = rememberCoroutineScope()
        var autoSkip by remember { mutableStateOf(config.autoSkip) }
        var markerMode by remember { mutableStateOf(config.markerMode) }
        val aboutItem = remember { resolveSponsorBlockAboutItemModel() }
        val markerOptions = remember {
            SponsorBlockMarkerMode.entries.map { mode ->
                PlaybackSegmentOption(
                    value = mode,
                    label = mode.label
                )
            }
        }
        
        // 加载配置
        LaunchedEffect(Unit) {
            loadConfigSuspend()
            autoSkip = config.autoSkip
            markerMode = config.markerMode
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 使用原设置组件 - 自动跳过
            IOSSwitchItem(
                icon = CupertinoIcons.Default.Bolt,
                title = "自动跳过",
                subtitle = "关闭后将显示手动跳过按钮而非自动跳过",
                checked = autoSkip,
                onCheckedChange = { newValue ->
                    autoSkip = newValue
                    config = config.copy(autoSkip = newValue)
                    scope.launch {
                        PluginStore.setConfigJson(context, id, Json.encodeToString(config))
                    }
                },
                iconTint = androidx.compose.ui.graphics.Color(0xFFFF9800) // iOS Orange
            )

            androidx.compose.material3.HorizontalDivider(
                modifier = Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            IOSSlidingSegmentedSetting(
                title = "进度条提示：${markerMode.label}",
                subtitle = "可选关闭、仅提示恰饭，或显示全部可跳过片段",
                options = markerOptions,
                selectedValue = markerMode,
                onSelectionChange = { newValue ->
                    markerMode = newValue
                    config = config.copy(markerModeRaw = newValue.name)
                    progressMarkers = resolveSponsorProgressMarkers(
                        segments = segments,
                        markerMode = newValue
                    )
                    scope.launch {
                        PluginStore.setConfigJson(context, id, Json.encodeToString(config))
                    }
                }
            )
            
            androidx.compose.material3.HorizontalDivider(
                modifier = Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            
            // 使用原设置组件 - 关于空降助手
            IOSClickableItem(
                icon = CupertinoIcons.Default.InfoCircle,
                title = aboutItem.title,
                subtitle = aboutItem.subtitle,
                value = aboutItem.value,
                onClick = { uriHandler.openUri("https://github.com/hanydd/BilibiliSponsorBlock") },
                iconTint = androidx.compose.ui.graphics.Color(0xFF2196F3) // iOS Blue
            )
        }
    }
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
    val skipInteraction: Boolean = true
) {
    val markerMode: SponsorBlockMarkerMode
        get() = com.android.purebilibili.data.model.response.resolveSponsorBlockMarkerMode(markerModeRaw)

    fun normalized(): SponsorBlockConfig = copy(markerModeRaw = markerMode.name)

    companion object {
        fun default(): SponsorBlockConfig = SponsorBlockConfig()
    }
}

private val SponsorBlockMarkerMode.label: String
    get() = when (this) {
        SponsorBlockMarkerMode.OFF -> "关闭"
        SponsorBlockMarkerMode.SPONSOR_ONLY -> "仅恰饭"
        SponsorBlockMarkerMode.ALL_SKIPPABLE -> "全部可跳过"
    }
