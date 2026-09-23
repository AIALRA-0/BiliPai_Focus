package com.android.purebilibili.feature.plugin
import com.android.purebilibili.core.ui.components.AppHorizontalDivider

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import com.android.purebilibili.core.ui.components.AppButton
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.components.AppOutlinedTextField
import com.android.purebilibili.core.ui.components.AppSwitch
import com.android.purebilibili.core.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.android.purebilibili.R
import com.android.purebilibili.core.coroutines.AppScope
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.plugin.Plugin
import com.android.purebilibili.core.plugin.PluginCapability
import com.android.purebilibili.core.plugin.PluginCapabilityManifest
import com.android.purebilibili.core.plugin.PluginManager
import com.android.purebilibili.core.plugin.PluginStore
import com.android.purebilibili.core.util.Logger
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Socket

const val CDN_REGION_PLUGIN_ID = "cdn_region"
private const val TAG = "CdnRegionPlugin"
private const val CDN_PROBE_SAMPLE_BYTES = 32 * 1024
private const val CDN_REALTIME_PROBE_INTERVAL_MS = 30_000L
private const val CDN_ACTIVE_PLAYBACK_SESSION_TTL_MS = 10 * 60_000L

data class PlaybackCdnRewriteResult(
    val candidates: List<PlaybackCdnCandidate>,
    val regionLabel: String?,
    val cacheKeysByUrl: Map<String, String> = emptyMap()
) {
    val videoUrls: List<String> get() = candidates.map { it.videoUrl }
    val audioUrls: List<String> get() = candidates.map { it.audioUrl.orEmpty() }
    val sources: List<PlaybackCdnCandidateSource> get() = candidates.map { it.source }
}

private data class CdnProbeMeasure(
    val success: Boolean,
    val latencyMs: Long?,
    val speedKbps: Long?
)

private data class CdnSettingsProbeTarget(
    val region: String,
    val hosts: List<String>
)

/**
 * Kept in memory only. Signed playurl addresses must never be written to plugin storage because
 * they expire and would otherwise look like a reusable CDN rule on the next playback session.
 */
private data class CdnActivePlaybackSession(
    val candidates: List<PlaybackCdnCandidate>,
    val updatedAtMs: Long
)

interface PlaybackCdnPlugin : Plugin {
    fun rewritePlaybackCandidates(
        videoUrls: List<String>,
        audioUrls: List<String>
    ): PlaybackCdnRewriteResult

    fun buildPlaybackCdnDiagnostics(
        videoUrls: List<String>,
        sources: List<PlaybackCdnCandidateSource> = emptyList()
    ): List<CdnLineDiagnostic> = emptyList()

    suspend fun probePlaybackCdnCandidates(
        videoUrls: List<String>,
        sources: List<PlaybackCdnCandidateSource> = emptyList()
    ): List<CdnLineDiagnostic> = emptyList()

    fun recordPlaybackCdnEvent(
        url: String,
        event: CdnHealthEvent
    ) = Unit

    fun isAdaptivePrefetchEnabled(): Boolean = false
}

class CdnRegionPlugin : PlaybackCdnPlugin {
    override val id: String = CDN_REGION_PLUGIN_ID
    override val name: String = "CDN 智能选线"
    override val description: String = "在 B 站当前授权的签名 CDN 候选中选线，并可选预缓存未来 DASH 分片"
    override val version: String = "1.4.0"
    override val author: String = "BiliPai项目组"
    override val icon: ImageVector = Icons.Outlined.Dns
    override val capabilityManifest: PluginCapabilityManifest = PluginCapabilityManifest(
        pluginId = id,
        displayName = name,
        version = version,
        apiVersion = 1,
        entryClassName = "com.android.purebilibili.feature.plugin.CdnRegionPlugin",
        capabilities = setOf(
            PluginCapability.PLAYBACK_CDN,
            PluginCapability.NETWORK,
            PluginCapability.PLUGIN_STORAGE
        )
    )

    @Volatile
    private var cache: CdnRegionPluginCache = CdnRegionPluginCache()

    @Volatile
    private var catalog: Map<String, List<String>> = emptyMap()

    @Volatile
    private var compiledCustomRules: List<Pair<CdnCustomRule, Regex>> = emptyList()

    @Volatile
    private var activePlaybackSession: CdnActivePlaybackSession? = null

    override suspend fun onEnable() {
        val context = PluginManager.getContext()
        catalog = loadCdnRegionCatalog(context)
        cache = CdnRegionPluginStore.read(context)
        compiledCustomRules = compileCdnCustomRules(cache.customRules)
        AppScope.ioScope.launch {
            delay(1_500L)
            refreshIpLocationIfNeeded()
        }
        Logger.d(TAG, "CDN 属地优选已启用，缓存地区=${cache.selectedRegion.ifBlank { "未命中" }}")
    }

    override suspend fun onDisable() {
        activePlaybackSession = null
        Logger.d(TAG, "CDN 属地优选已禁用")
    }

    override fun rewritePlaybackCandidates(
        videoUrls: List<String>,
        audioUrls: List<String>
    ): PlaybackCdnRewriteResult {
        val snapshot = cache
        val originalCandidates = buildPlaybackCdnCandidates(videoUrls, audioUrls)
        publishActivePlaybackCandidates(originalCandidates)
        if (!snapshot.experimentalRewriteEnabled) {
            val safeCandidates = sortSafeSignedPlaybackCandidates(
                candidates = originalCandidates,
                healthByHost = snapshot.healthByHost
            )
            return PlaybackCdnRewriteResult(
                candidates = safeCandidates,
                regionLabel = null,
                cacheKeysByUrl = buildPlaybackCdnCacheKeys(safeCandidates)
            )
        }
        val customCandidates = rewritePlaybackCdnCandidatesForCompiledCustomRules(
            candidates = originalCandidates,
            compiledRules = compiledCustomRules
        )
        val hosts = if (snapshot.fallbackUsed) emptyList() else resolveCdnRegionHosts(
            region = snapshot.selectedRegion,
            cachedHosts = snapshot.selectedHosts,
            catalog = catalog,
            isp = snapshot.location.isp
        )
        val regionCandidates = rewritePlaybackCdnCandidatesForRegion(originalCandidates, hosts)
        val candidates = selectPlaybackCdnCandidatesForMode(
                customCandidates = customCandidates,
                regionCandidates = regionCandidates,
                originalCandidates = originalCandidates,
                strictCustomCdn = snapshot.strictCustomCdn,
                healthByHost = snapshot.healthByHost
            )
        return PlaybackCdnRewriteResult(
            candidates = candidates,
            regionLabel = snapshot.selectedRegion.takeIf { hosts.isNotEmpty() },
            cacheKeysByUrl = buildPlaybackCdnCacheKeys(candidates)
        )
    }

    override fun buildPlaybackCdnDiagnostics(
        videoUrls: List<String>,
        sources: List<PlaybackCdnCandidateSource>
    ): List<CdnLineDiagnostic> {
        return buildCdnLineDiagnostics(videoUrls, cache.healthByHost, sources)
    }

    override suspend fun probePlaybackCdnCandidates(
        videoUrls: List<String>,
        sources: List<PlaybackCdnCandidateSource>
    ): List<CdnLineDiagnostic> {
        return probePlaybackCdnCandidatesInternal(
            videoUrls = videoUrls,
            sources = sources,
            ignoreCooldown = false
        )
    }

    private suspend fun probePlaybackCdnCandidatesInternal(
        videoUrls: List<String>,
        sources: List<PlaybackCdnCandidateSource>,
        ignoreCooldown: Boolean
    ): List<CdnLineDiagnostic> {
        val context = PluginManager.getContext()
        val now = System.currentTimeMillis()
        val current = CdnRegionPluginStore.read(context).also { cache = it }
        val candidates = resolveCdnProbeCandidates(
            urls = videoUrls,
            healthByHost = current.healthByHost,
            nowMs = now,
            limit = CdnProbeLimit(
                maxCandidates = 3,
                cooldownMs = if (ignoreCooldown) 0L else CDN_MANUAL_PROBE_COOLDOWN_MS
            )
        )
        var nextHealth = current.healthByHost
        candidates.filter { it.allowed }.forEach { candidate ->
            val result = probePlaybackUrl(candidate.url)
            val previous = nextHealth[candidate.host] ?: CdnCandidateHealth(host = candidate.host)
            nextHealth = nextHealth + (
                candidate.host to recordCdnProbeResult(
                    current = previous,
                    latencyMs = result.latencyMs,
                    speedKbps = result.speedKbps,
                    success = result.success,
                    nowMs = System.currentTimeMillis()
                )
            )
        }
        val next = current.copy(healthByHost = nextHealth)
        cache = next
        CdnRegionPluginStore.write(context, next)
        return buildCdnLineDiagnostics(videoUrls, next.healthByHost, sources)
    }

    override fun recordPlaybackCdnEvent(url: String, event: CdnHealthEvent) {
        val host = hostFromCdnUrl(url)
        if (host.isBlank()) return
        val now = System.currentTimeMillis()
        val current = cache
        val previous = current.healthByHost[host] ?: CdnCandidateHealth(host = host)
        val next = current.copy(
            healthByHost = current.healthByHost + (host to recordCdnHealthEvent(previous, event, now))
        )
        cache = next
        AppScope.ioScope.launch {
            CdnRegionPluginStore.write(PluginManager.getContext(), next)
        }
    }

    override fun isAdaptivePrefetchEnabled(): Boolean {
        return cache.prefetchEnabled && !cache.experimentalRewriteEnabled
    }

    @Composable
    override fun SettingsContent() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var snapshot by remember { mutableStateOf(cache) }
        var catalogSnapshot by remember { mutableStateOf(catalog) }
        var probing by remember { mutableStateOf(false) }
        var customRules by remember(snapshot.customRules) { mutableStateOf(snapshot.customRules) }
        var strictCustomCdn by remember(snapshot.strictCustomCdn) { mutableStateOf(snapshot.strictCustomCdn) }
        var prefetchEnabled by remember(snapshot.prefetchEnabled) { mutableStateOf(snapshot.prefetchEnabled) }
        var experimentalRewriteEnabled by remember(snapshot.experimentalRewriteEnabled) {
            mutableStateOf(snapshot.experimentalRewriteEnabled)
        }
        var customRuleError by remember { mutableStateOf<String?>(null) }
        var activeCandidates by remember { mutableStateOf<List<PlaybackCdnCandidate>>(emptyList()) }
        var liveDiagnostics by remember { mutableStateOf<List<CdnLineDiagnostic>>(emptyList()) }
        var lastLiveProbeAtMs by remember { mutableStateOf<Long?>(null) }

        LaunchedEffect(Unit) {
            catalogSnapshot = catalog.ifEmpty {
                loadCdnRegionCatalog(context).also { catalog = it }
            }
            snapshot = CdnRegionPluginStore.read(context).also { cache = it }
        }
        LaunchedEffect(Unit) {
            while (currentCoroutineContext().isActive) {
                val session = activePlaybackSessionOrNull()
                activeCandidates = session?.candidates.orEmpty()
                if (session != null) {
                    liveDiagnostics = probePlaybackCdnCandidatesInternal(
                        videoUrls = session.candidates.map { it.videoUrl },
                        sources = session.candidates.map { it.source },
                        ignoreCooldown = true
                    )
                    snapshot = cache
                    lastLiveProbeAtMs = System.currentTimeMillis()
                } else {
                    liveDiagnostics = emptyList()
                    lastLiveProbeAtMs = null
                }
                delay(CDN_REALTIME_PROBE_INTERVAL_MS)
            }
        }
        val probeTarget = resolveSettingsProbeTarget(snapshot, catalogSnapshot)
        val regionText = snapshot.selectedRegion.ifBlank {
            probeTarget.region.ifBlank { "未命中" }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            AppText(
                text = "安全签名选线",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            AppText(
                text = "打开视频后会自动比较 B 站当前返回的签名主/备线路；不会改写 Host，也不会保存过期链接。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            AppText(
                text = "当前播放线路 · 实时检测",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            when {
                activeCandidates.isEmpty() -> {
                    AppText(
                        text = "打开任意视频并开始播放后，这里会自动显示最多 3 条授权线路的实时质量，无需填写地域或规则。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    AppText(
                        text = "正在检测当前会话；每 ${CDN_REALTIME_PROBE_INTERVAL_MS / 1_000} 秒刷新一次，仅使用 32 KiB 小范围请求。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    liveDiagnostics.forEachIndexed { index, diagnostic ->
                        AppText(
                            text = "${index + 1}. ${diagnostic.host} · ${formatCdnLineDiagnostic(diagnostic)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (liveDiagnostics.isEmpty()) {
                        AppText(
                            text = "正在建立首轮检测…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    lastLiveProbeAtMs?.let {
                        AppText(
                            text = "最近更新：刚刚",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                AppText(
                    text = "自适应预缓存",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )
                AppSwitch(
                    checked = prefetchEnabled,
                    onCheckedChange = { enabled ->
                        prefetchEnabled = enabled
                        val next = snapshot.copy(prefetchEnabled = enabled)
                        snapshot = next
                        cache = next
                        scope.launch { CdnRegionPluginStore.write(context, next) }
                    }
                )
            }
            AppText(
                text = "仅在 DASH 可读取分片索引且播放器已有至少 15 秒缓冲时下载未来分片；默认关闭以避免额外流量、电量和磁盘消耗。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                AppText(
                    text = "实验性属地 / URL 改写",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )
                AppSwitch(
                    checked = experimentalRewriteEnabled,
                    onCheckedChange = { enabled ->
                        experimentalRewriteEnabled = enabled
                        val next = snapshot.copy(experimentalRewriteEnabled = enabled)
                        snapshot = next
                        cache = next
                        scope.launch { CdnRegionPluginStore.write(context, next) }
                    }
                )
            }
            AppText(
                text = "兼容旧版地域 Host 表和高级 URL 替换。它不参与安全自动选线或预缓存，可能因签名与线路策略变化而不可用。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (experimentalRewriteEnabled) {
                val hostDiagnostics = buildCdnHostDiagnostics(
                    hosts = probeTarget.hosts.take(5),
                    healthByHost = snapshot.healthByHost
                )
                AppHorizontalDivider(modifier = Modifier.padding(top = 10.dp))
                Spacer(modifier = Modifier.height(12.dp))
                AppText(
                    text = "实验性属地检测",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                AppText(
                    text = "属地：${snapshot.location.province.ifBlank { "未知" }} / ${snapshot.location.city.ifBlank { "未知" }}；运营商：${snapshot.location.isp.ifBlank { "未知" }}；命中区域：$regionText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                hostDiagnostics.forEachIndexed { index, diagnostic ->
                    AppText(
                        text = "${index + 1}. ${diagnostic.host} · ${formatCdnHostDiagnostic(diagnostic)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                AppButton(
                    enabled = !probing && probeTarget.hosts.isNotEmpty(),
                    onClick = {
                        probing = true
                        scope.launch {
                            snapshot = probeSelectedHosts(context, snapshot, probeTarget)
                            probing = false
                        }
                    }
                ) {
                    AppText(if (probing) "检测中..." else "检测实验线路")
                }
                AppHorizontalDivider(modifier = Modifier.padding(top = 10.dp))
                Spacer(modifier = Modifier.height(12.dp))
                AppText(
                    text = "高级 URL 替换",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                AppText(
                    text = "规则按顺序匹配完整播放 URL，仅应用首条命中规则。替换结果必须是 HTTPS 且指向 B 站或当前候选 CDN。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                AppText(
                    text = "严格使用自定义 CDN",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )
                AppSwitch(
                    checked = strictCustomCdn,
                    onCheckedChange = { strictCustomCdn = it }
                )
            }
            AppText(
                text = "开启后，只要有自定义规则成功生成播放地址，就不再加入属地和原始线路；没有规则命中时仍会自动回退，避免无法播放。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            customRules.forEachIndexed { index, rule ->
                Spacer(modifier = Modifier.height(8.dp))
                CdnCustomRuleEditor(
                    index = index,
                    rule = rule,
                    canMoveUp = index > 0,
                    canMoveDown = index < customRules.lastIndex,
                    onChange = { updated ->
                        customRules = customRules.toMutableList().also { it[index] = updated }
                        customRuleError = null
                    },
                    onMoveUp = {
                        customRules = customRules.toMutableList().also { rules ->
                            rules[index] = rules[index - 1].also { rules[index - 1] = rules[index] }
                        }
                    },
                    onMoveDown = {
                        customRules = customRules.toMutableList().also { rules ->
                            rules[index] = rules[index + 1].also { rules[index + 1] = rules[index] }
                        }
                    },
                    onRemove = {
                        customRules = customRules.filterIndexed { itemIndex, _ -> itemIndex != index }
                        customRuleError = null
                    }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                AppButton(onClick = {
                    customRules = customRules + CdnCustomRule(enabled = false)
                    customRuleError = null
                }) {
                    AppText("新增规则")
                }
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                AppButton(onClick = {
                    val error = validateCdnCustomRules(customRules)
                        .firstOrNull { it.error != null }
                        ?.error
                    if (error != null) {
                        customRuleError = error
                    } else {
                        val next = snapshot.copy(
                            customRules = customRules,
                            strictCustomCdn = strictCustomCdn
                        )
                        snapshot = next
                        cache = next
                        compiledCustomRules = compileCdnCustomRules(next.customRules)
                        scope.launch { CdnRegionPluginStore.write(context, next) }
                    }
                }) {
                    AppText("保存规则")
                }
            }
            customRuleError?.let { error ->
                AppText(
                    text = error,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            AppHorizontalDivider(modifier = Modifier.padding(top = 10.dp))
            }
        }
    }

    private fun publishActivePlaybackCandidates(candidates: List<PlaybackCdnCandidate>) {
        activePlaybackSession = CdnActivePlaybackSession(
            candidates = candidates.distinctBy { it.videoUrl }.take(3),
            updatedAtMs = System.currentTimeMillis()
        )
    }

    private fun activePlaybackSessionOrNull(): CdnActivePlaybackSession? {
        val session = activePlaybackSession ?: return null
        return session.takeIf {
            System.currentTimeMillis() - it.updatedAtMs <= CDN_ACTIVE_PLAYBACK_SESSION_TTL_MS
        }
    }

    private fun formatCdnLineDiagnostic(diagnostic: CdnLineDiagnostic): String {
        return buildList {
            diagnostic.latencyMs?.let { add("${it}ms") }
            diagnostic.speedKbps?.let { add("${it}Kbps") }
            add(diagnostic.statusLabel)
            if (diagnostic.errorCount > 0) add("失败 ${diagnostic.errorCount}")
            if (diagnostic.bufferingCount > 0) add("缓冲 ${diagnostic.bufferingCount}")
        }.distinct().joinToString(" · ")
    }

    @Composable
    private fun CdnCustomRuleEditor(
        index: Int,
        rule: CdnCustomRule,
        canMoveUp: Boolean,
        canMoveDown: Boolean,
        onChange: (CdnCustomRule) -> Unit,
        onMoveUp: () -> Unit,
        onMoveDown: () -> Unit,
        onRemove: () -> Unit
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row {
                AppText(
                    text = "规则 ${index + 1}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )
                AppText("启用", style = MaterialTheme.typography.bodySmall)
                AppSwitch(checked = rule.enabled, onCheckedChange = { onChange(rule.copy(enabled = it)) })
            }
            AppOutlinedTextField(
                value = rule.pattern,
                onValueChange = { onChange(rule.copy(pattern = it)) },
                label = { AppText("匹配正则") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            AppOutlinedTextField(
                value = rule.replacement,
                onValueChange = { onChange(rule.copy(replacement = it)) },
                label = { AppText("替换内容") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row {
                AppButton(enabled = canMoveUp, onClick = onMoveUp) { AppText("上移") }
                Spacer(modifier = Modifier.padding(horizontal = 2.dp))
                AppButton(enabled = canMoveDown, onClick = onMoveDown) { AppText("下移") }
                Spacer(modifier = Modifier.padding(horizontal = 2.dp))
                AppButton(onClick = onRemove) { AppText("删除") }
            }
        }
    }

    private suspend fun probeSelectedHosts(
        context: Context,
        current: CdnRegionPluginCache,
        target: CdnSettingsProbeTarget
    ): CdnRegionPluginCache {
        var nextHealth = current.healthByHost
        val now = System.currentTimeMillis()
        target.hosts.take(5).forEach { host ->
            val previous = nextHealth[host] ?: CdnCandidateHealth(host = host)
            val elapsed = now - previous.lastProbeAtMs
            if (previous.lastProbeAtMs > 0L && elapsed < CDN_MANUAL_PROBE_COOLDOWN_MS) {
                return@forEach
            }
            val result = probeHostTlsPort(host)
            nextHealth = nextHealth + (
                host to recordCdnProbeResult(
                    current = previous,
                    latencyMs = result.latencyMs,
                    speedKbps = null,
                    success = result.success,
                    nowMs = System.currentTimeMillis()
                )
            )
        }
        val nextRegion = current.selectedRegion.ifBlank { target.region }
        val nextHosts = current.selectedHosts.ifEmpty { target.hosts }
        return current.copy(
            selectedRegion = nextRegion,
            selectedHosts = nextHosts,
            fallbackUsed = current.fallbackUsed && nextRegion.isBlank(),
            healthByHost = nextHealth
        ).also {
            cache = it
            CdnRegionPluginStore.write(context, it)
        }
    }

    private fun resolveSettingsProbeTarget(
        snapshot: CdnRegionPluginCache,
        catalogSnapshot: Map<String, List<String>>
    ): CdnSettingsProbeTarget {
        val selectedHosts = resolveCdnRegionHosts(
            region = snapshot.selectedRegion,
            cachedHosts = snapshot.selectedHosts,
            catalog = catalogSnapshot,
            isp = snapshot.location.isp
        )
        if (selectedHosts.isNotEmpty()) {
            return CdnSettingsProbeTarget(
                region = snapshot.selectedRegion,
                hosts = selectedHosts
            )
        }

        val selection = selectCdnRegionForLocation(
            location = snapshot.location,
            catalog = catalogSnapshot
        )
        return CdnSettingsProbeTarget(
            region = selection.region,
            hosts = selection.hosts
        )
    }

    private fun formatCdnHostDiagnostic(diagnostic: CdnHostDiagnostic): String {
        val parts = buildList {
            if (diagnostic.latencyMs != null) {
                add("延迟 ${diagnostic.latencyMs}ms")
            }
            if (diagnostic.speedKbps != null) {
                add("速度 ${diagnostic.speedKbps}Kbps")
            }
            when {
                diagnostic.lastProbeAtMs > 0L && diagnostic.latencyMs == null -> add("检测失败")
                diagnostic.lastProbeAtMs <= 0L -> add("未检测")
            }
            add(diagnostic.statusLabel)
            if (diagnostic.errorCount > 0) {
                add("失败 ${diagnostic.errorCount} 次")
            }
        }
        return parts.distinct().joinToString(" · ")
    }

    private suspend fun probePlaybackUrl(url: String): CdnProbeMeasure {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val startedAt = System.nanoTime()
            runCatching {
                val request = Request.Builder()
                    .url(url)
                    .header("Range", "bytes=0-${CDN_PROBE_SAMPLE_BYTES - 1}")
                    .header("Referer", "https://www.bilibili.com")
                    .build()
                NetworkModule.playbackOkHttpClient.newCall(request).execute().use { response ->
                    val bytesRead = response.body.byteStream().use { input ->
                        val buffer = ByteArray(8 * 1024)
                        var total = 0
                        while (total < CDN_PROBE_SAMPLE_BYTES) {
                            val read = input.read(buffer, 0, minOf(buffer.size, CDN_PROBE_SAMPLE_BYTES - total))
                            if (read <= 0) break
                            total += read
                        }
                        total
                    }
                    val elapsedMs = ((System.nanoTime() - startedAt) / 1_000_000L).coerceAtLeast(1L)
                    CdnProbeMeasure(
                        success = response.isSuccessful || response.code == 206,
                        latencyMs = elapsedMs,
                        speedKbps = if (bytesRead > 0) (bytesRead * 8L / elapsedMs).coerceAtLeast(1L) else null
                    )
                }
            }.getOrElse { error ->
                if (error is CancellationException) throw error
                Logger.w(TAG, "CDN 播放候选检测失败: ${hostFromCdnUrl(url)} ${error.message}")
                CdnProbeMeasure(success = false, latencyMs = null, speedKbps = null)
            }
        }
    }

    private suspend fun probeHostTlsPort(host: String): CdnProbeMeasure {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val startedAt = System.nanoTime()
            runCatching {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, 443), 3_000)
                }
                val elapsedMs = ((System.nanoTime() - startedAt) / 1_000_000L).coerceAtLeast(1L)
                CdnProbeMeasure(success = true, latencyMs = elapsedMs, speedKbps = null)
            }.getOrElse { error ->
                if (error is CancellationException) throw error
                Logger.w(TAG, "CDN host 检测失败: $host ${error.message}")
                CdnProbeMeasure(success = false, latencyMs = null, speedKbps = null)
            }
        }
    }

    private suspend fun refreshIpLocationIfNeeded() {
        val context = PluginManager.getContext()
        val enabled = PluginStore.isEnabled(context, id)
        val loadedCatalog = catalog.ifEmpty {
            loadCdnRegionCatalog(context).also { catalog = it }
        }
        if (loadedCatalog.isEmpty()) return
        val current = CdnRegionPluginStore.read(context).also { cache = it }
        val hasSelection = hasUsableCdnRegionSelection(
            region = current.selectedRegion,
            cachedHosts = current.selectedHosts,
            catalog = loadedCatalog
        )

        if (!shouldRefreshCdnIpLocation(
                enabled = enabled,
                nowMs = System.currentTimeMillis(),
                lastRefreshMs = current.refreshedAtMs,
                hasSelection = hasSelection && !current.fallbackUsed
            )
        ) {
            val resolvedHosts = resolveCdnRegionHosts(
                region = current.selectedRegion,
                cachedHosts = current.selectedHosts,
                catalog = loadedCatalog,
                isp = current.location.isp
            )
            if (current.selectedHosts != resolvedHosts) {
                val corrected = current.copy(
                    selectedHosts = resolvedHosts
                )
                cache = corrected
                CdnRegionPluginStore.write(context, corrected)
            }
            return
        }

        try {
            val response = NetworkModule.api.getIpZone()
            val data = response.data
            if (response.code != 0 || data == null) {
                error(response.message.ifBlank { "IP 属地接口返回 code=${response.code}" })
            }

            val location = IpLocationSnapshot(
                addr = data.addr,
                country = data.country,
                province = data.province,
                city = data.city,
                isp = data.isp
            )
            val selection = selectCdnRegionForLocation(
                location = location,
                catalog = loadedCatalog
            )
            val next = current.copy(
                location = location,
                selectedRegion = selection.region,
                selectedHosts = selection.hosts,
                fallbackRegion = "",
                fallbackUsed = selection.fallbackUsed,
                refreshedAtMs = System.currentTimeMillis(),
                lastError = null
            )
            cache = next
            CdnRegionPluginStore.write(context, next)
            Logger.d(
                TAG,
                "CDN 属地刷新成功: ip=${maskIpAddressForLog(location.addr)}, " +
                    "${location.country}/${location.province}/${location.city}, isp=${location.isp.ifBlank { "未知" }} -> " +
                    (selection.region.ifBlank { "未命中" })
            )
        } catch (e: Exception) {
            val preserved = current.copy(lastError = e.message ?: e.javaClass.simpleName)
            cache = preserved
            CdnRegionPluginStore.write(context, preserved)
            Logger.w(TAG, "CDN 属地刷新失败，保留旧缓存: ${e.message}")
        }
    }
}

@Serializable
internal data class CdnRegionPluginCache(
    val location: IpLocationSnapshot = IpLocationSnapshot(),
    val selectedRegion: String = "",
    val selectedHosts: List<String> = emptyList(),
    val fallbackRegion: String = "",
    val fallbackUsed: Boolean = false,
    val refreshedAtMs: Long = 0L,
    val lastError: String? = null,
    val healthByHost: Map<String, CdnCandidateHealth> = emptyMap(),
    val customRules: List<CdnCustomRule> = emptyList(),
    val strictCustomCdn: Boolean = false,
    val prefetchEnabled: Boolean = false,
    val experimentalRewriteEnabled: Boolean = false
)

internal object CdnRegionPluginStore {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun read(context: Context): CdnRegionPluginCache {
        val raw = PluginStore.getConfigJson(context, CDN_REGION_PLUGIN_ID) ?: return CdnRegionPluginCache()
        return runCatching { json.decodeFromString<CdnRegionPluginCache>(raw) }
            .getOrDefault(CdnRegionPluginCache())
    }

    suspend fun write(context: Context, cache: CdnRegionPluginCache) {
        PluginStore.setConfigJson(
            context = context,
            pluginId = CDN_REGION_PLUGIN_ID,
            configJson = json.encodeToString(cache)
        )
    }
}

internal fun loadCdnRegionCatalog(context: Context): Map<String, List<String>> {
    return runCatching {
        context.resources.openRawResource(R.raw.cdn_region_catalog).bufferedReader().use { reader ->
            Json.decodeFromString<Map<String, List<String>>>(reader.readText())
        }.filterValues { hosts -> hosts.any { it.isNotBlank() } }
            .mapValues { (_, hosts) -> hosts.filter { it.isNotBlank() }.distinct() }
    }.getOrElse { error ->
        Logger.w(TAG, "读取 CDN catalog 失败: ${error.message}")
        emptyMap()
    }
}
