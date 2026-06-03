package com.android.purebilibili.feature.plugin

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import io.github.alexzhirkevich.cupertino.icons.CupertinoIcons
import io.github.alexzhirkevich.cupertino.icons.outlined.ServerRack
import kotlinx.coroutines.delay
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
private const val CDN_PROBE_SAMPLE_BYTES = 64 * 1024

data class PlaybackCdnRewriteResult(
    val videoUrls: List<String>,
    val audioUrls: List<String>,
    val regionLabel: String?
)

private data class CdnProbeMeasure(
    val success: Boolean,
    val latencyMs: Long?,
    val speedKbps: Long?
)

private data class CdnSettingsProbeTarget(
    val region: String,
    val hosts: List<String>
)

interface PlaybackCdnPlugin : Plugin {
    fun rewritePlaybackCandidates(
        videoUrls: List<String>,
        audioUrls: List<String>
    ): PlaybackCdnRewriteResult

    fun buildPlaybackCdnDiagnostics(
        videoUrls: List<String>
    ): List<CdnLineDiagnostic> = emptyList()

    suspend fun probePlaybackCdnCandidates(
        videoUrls: List<String>
    ): List<CdnLineDiagnostic> = emptyList()

    fun recordPlaybackCdnEvent(
        url: String,
        event: CdnHealthEvent
    ) = Unit
}

class CdnRegionPlugin : PlaybackCdnPlugin {
    override val id: String = CDN_REGION_PLUGIN_ID
    override val name: String = "CDN 属地优选"
    override val description: String = "按当前 IP 属地把同地区 B 站视频 CDN 排到线路候选前面"
    override val version: String = "1.1.0"
    override val author: String = "BiliPai项目组"
    override val icon: ImageVector = CupertinoIcons.Outlined.ServerRack
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

    override suspend fun onEnable() {
        val context = PluginManager.getContext()
        catalog = loadCdnRegionCatalog(context)
        cache = CdnRegionPluginStore.read(context)
        AppScope.ioScope.launch {
            delay(1_500L)
            refreshIpLocationIfNeeded()
        }
        Logger.d(TAG, "CDN 属地优选已启用，缓存地区=${cache.selectedRegion.ifBlank { "未命中" }}")
    }

    override suspend fun onDisable() {
        Logger.d(TAG, "CDN 属地优选已禁用")
    }

    override fun rewritePlaybackCandidates(
        videoUrls: List<String>,
        audioUrls: List<String>
    ): PlaybackCdnRewriteResult {
        val snapshot = cache
        if (snapshot.fallbackUsed) {
            return PlaybackCdnRewriteResult(
                videoUrls = sortCdnCandidatesByHealth(videoUrls, snapshot.healthByHost),
                audioUrls = sortCdnCandidatesByHealth(audioUrls, snapshot.healthByHost),
                regionLabel = null
            )
        }
        val hosts = resolveCdnRegionHosts(
            region = snapshot.selectedRegion,
            cachedHosts = snapshot.selectedHosts,
            catalog = catalog,
            isp = snapshot.location.isp
        )

        if (hosts.isEmpty()) {
            return PlaybackCdnRewriteResult(
                videoUrls = sortCdnCandidatesByHealth(videoUrls, snapshot.healthByHost),
                audioUrls = sortCdnCandidatesByHealth(audioUrls, snapshot.healthByHost),
                regionLabel = null
            )
        }

        val rewrittenVideoUrls = rewriteCdnUrlCandidates(videoUrls, hosts).urls
        val rewrittenAudioUrls = rewriteCdnUrlCandidates(audioUrls, hosts).urls
        return PlaybackCdnRewriteResult(
            videoUrls = sortCdnCandidatesByHealth(rewrittenVideoUrls, snapshot.healthByHost),
            audioUrls = sortCdnCandidatesByHealth(rewrittenAudioUrls, snapshot.healthByHost),
            regionLabel = snapshot.selectedRegion.takeIf { it.isNotBlank() }
        )
    }

    override fun buildPlaybackCdnDiagnostics(videoUrls: List<String>): List<CdnLineDiagnostic> {
        return buildCdnLineDiagnostics(videoUrls, cache.healthByHost)
    }

    override suspend fun probePlaybackCdnCandidates(videoUrls: List<String>): List<CdnLineDiagnostic> {
        val context = PluginManager.getContext()
        val now = System.currentTimeMillis()
        val current = CdnRegionPluginStore.read(context).also { cache = it }
        val candidates = resolveCdnProbeCandidates(
            urls = videoUrls,
            healthByHost = current.healthByHost,
            nowMs = now
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
        return buildCdnLineDiagnostics(videoUrls, next.healthByHost)
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

    @Composable
    override fun SettingsContent() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var snapshot by remember { mutableStateOf(cache) }
        var catalogSnapshot by remember { mutableStateOf(catalog) }
        var probing by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            catalogSnapshot = catalog.ifEmpty {
                loadCdnRegionCatalog(context).also { catalog = it }
            }
            snapshot = CdnRegionPluginStore.read(context).also { cache = it }
        }
        val probeTarget = resolveSettingsProbeTarget(snapshot, catalogSnapshot)
        val hostDiagnostics = buildCdnHostDiagnostics(
            hosts = probeTarget.hosts.take(5),
            healthByHost = snapshot.healthByHost
        )
        val regionText = snapshot.selectedRegion.ifBlank {
            probeTarget.region.ifBlank { "未命中" }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = "属地：${snapshot.location.province.ifBlank { "未知" }} / ${snapshot.location.city.ifBlank { "未知" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "运营商：${snapshot.location.isp.ifBlank { "未知" }}，命中区域：$regionText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            hostDiagnostics.forEachIndexed { index, diagnostic ->
                Text(
                    text = "${index + 1}. ${diagnostic.host} · ${formatCdnHostDiagnostic(diagnostic)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (hostDiagnostics.isEmpty()) {
                Text(
                    text = "暂无可检测候选 host，请稍后刷新属地或进入播放页检测当前线路。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                enabled = !probing && probeTarget.hosts.isNotEmpty(),
                onClick = {
                    probing = true
                    scope.launch {
                        snapshot = probeSelectedHosts(context, snapshot, probeTarget)
                        probing = false
                    }
                }
            ) {
                Text(if (probing) "检测中..." else "检测候选服务器")
            }
            Text(
                text = "检测仅手动触发，单次最多 5 个 host，同一 host 10 分钟冷却。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(modifier = Modifier.padding(top = 10.dp))
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
            val next = CdnRegionPluginCache(
                location = location,
                selectedRegion = selection.region,
                selectedHosts = selection.hosts,
                fallbackRegion = "",
                fallbackUsed = selection.fallbackUsed,
                refreshedAtMs = System.currentTimeMillis(),
                lastError = null,
                healthByHost = current.healthByHost
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
    val healthByHost: Map<String, CdnCandidateHealth> = emptyMap()
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
