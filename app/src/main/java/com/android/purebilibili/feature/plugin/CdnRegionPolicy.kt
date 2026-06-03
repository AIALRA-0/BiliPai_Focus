package com.android.purebilibili.feature.plugin

import kotlinx.serialization.Serializable
import java.net.URI

@Serializable
data class IpLocationSnapshot(
    val addr: String = "",
    val country: String = "",
    val province: String = "",
    val city: String = "",
    val isp: String = ""
)

data class CdnRegionSelection(
    val region: String,
    val hosts: List<String>,
    val fallbackUsed: Boolean
)

data class CdnRewriteResult(
    val urls: List<String>,
    val rewrittenCount: Int
)

@Serializable
data class CdnCandidateHealth(
    val host: String = "",
    val firstFrameTimeoutCount: Int = 0,
    val bufferingCount: Int = 0,
    val playbackErrorCount: Int = 0,
    val readyCount: Int = 0,
    val manualProbeLatencyMs: Long? = null,
    val manualProbeSpeedKbps: Long? = null,
    val lastProbeAtMs: Long = 0L,
    val lastUpdatedAtMs: Long = 0L
)

enum class CdnHealthEvent {
    PLAYBACK_READY,
    FIRST_FRAME_TIMEOUT,
    AUDIO_TRACK_MISSING,
    PLAYER_ERROR,
    BUFFERING
}

data class CdnProbeLimit(
    val maxCandidates: Int = 5,
    val cooldownMs: Long = CDN_MANUAL_PROBE_COOLDOWN_MS
)

data class CdnProbeCandidate(
    val url: String,
    val host: String,
    val allowed: Boolean,
    val cooldownRemainingMs: Long
)

data class CdnLineDiagnostic(
    val index: Int,
    val host: String,
    val statusLabel: String,
    val latencyMs: Long?,
    val speedKbps: Long?,
    val errorCount: Int,
    val bufferingCount: Int,
    val lastProbeAtMs: Long,
    val score: Int
)

data class CdnHostDiagnostic(
    val host: String,
    val statusLabel: String,
    val latencyMs: Long?,
    val speedKbps: Long?,
    val errorCount: Int,
    val lastProbeAtMs: Long
)

internal fun selectCdnRegionForLocation(
    location: IpLocationSnapshot,
    catalog: Map<String, List<String>>,
    @Suppress("UNUSED_PARAMETER")
    fallbackRegion: () -> String = { "" }
): CdnRegionSelection {
    val available = catalog.filterValues { it.isNotEmpty() }
    val overseasHosts = available["海外"]
    val normalizedCountry = normalizeRegionName(location.country)

    if (available.isEmpty()) {
        return CdnRegionSelection(region = "", hosts = emptyList(), fallbackUsed = true)
    }

    val directCandidates = listOf(location.city, location.province, location.country)
        .flatMap { value -> listOf(value, normalizeRegionName(value)) }
        .filter { it.isNotBlank() }

    directCandidates.firstNotNullOfOrNull { candidate ->
        available[candidate]?.let { hosts ->
            return CdnRegionSelection(
                candidate,
                sortCdnRegionHostsForIsp(hosts, location.isp),
                fallbackUsed = false
            )
        }
    }

    if (normalizedCountry.isNotBlank() && normalizedCountry !in mainlandCountryNames) {
        overseasHosts?.let {
            return CdnRegionSelection(
                "海外",
                sortCdnRegionHostsForIsp(it, location.isp),
                fallbackUsed = false
            )
        }
    }

    val alias = resolveMainlandRegionAlias(location)
    if (alias != null) {
        available[alias]?.let {
            return CdnRegionSelection(
                alias,
                sortCdnRegionHostsForIsp(it, location.isp),
                fallbackUsed = false
            )
        }
    }

    return CdnRegionSelection(region = "", hosts = emptyList(), fallbackUsed = true)
}

internal fun rewriteCdnUrlCandidates(
    originalUrls: List<String>,
    preferredHosts: List<String>
): CdnRewriteResult {
    if (originalUrls.isEmpty() || preferredHosts.isEmpty()) {
        return CdnRewriteResult(urls = originalUrls.distinct(), rewrittenCount = 0)
    }

    val rewritten = buildList {
        originalUrls.forEach { original ->
            preferredHosts.forEach { host ->
                rewriteBilivideoHost(original, host)?.let { add(it) }
            }
        }
        addAll(originalUrls)
    }.distinct()

    return CdnRewriteResult(
        urls = rewritten,
        rewrittenCount = (rewritten.size - originalUrls.distinct().size).coerceAtLeast(0)
    )
}

internal fun sortCdnCandidatesByHealth(
    urls: List<String>,
    healthByHost: Map<String, CdnCandidateHealth>
): List<String> {
    return urls.distinct().mapIndexed { index, url ->
        val host = hostFromCdnUrl(url)
        Triple(url, index, scoreCdnCandidate(healthByHost[host]))
    }.sortedWith(
        compareByDescending<Triple<String, Int, Int>> { it.third }
            .thenBy { it.second }
    ).map { it.first }
}

internal fun scoreCdnCandidate(health: CdnCandidateHealth?): Int {
    if (health == null) return 1_000
    val speedScore = ((health.manualProbeSpeedKbps ?: 0L) / 128L).coerceAtMost(500L).toInt()
    val latencyPenalty = ((health.manualProbeLatencyMs ?: 0L) / 20L).coerceAtMost(250L).toInt()
    val readyScore = health.readyCount.coerceAtMost(10) * 25
    val errorPenalty = health.playbackErrorCount * 220 +
        health.firstFrameTimeoutCount * 180 +
        health.bufferingCount * 55
    return 1_000 + speedScore + readyScore - latencyPenalty - errorPenalty
}

internal fun recordCdnHealthEvent(
    current: CdnCandidateHealth,
    event: CdnHealthEvent,
    nowMs: Long
): CdnCandidateHealth {
    return when (event) {
        CdnHealthEvent.PLAYBACK_READY -> current.copy(
            readyCount = current.readyCount + 1,
            lastUpdatedAtMs = nowMs
        )
        CdnHealthEvent.FIRST_FRAME_TIMEOUT -> current.copy(
            firstFrameTimeoutCount = current.firstFrameTimeoutCount + 1,
            lastUpdatedAtMs = nowMs
        )
        CdnHealthEvent.AUDIO_TRACK_MISSING,
        CdnHealthEvent.PLAYER_ERROR -> current.copy(
            playbackErrorCount = current.playbackErrorCount + 1,
            lastUpdatedAtMs = nowMs
        )
        CdnHealthEvent.BUFFERING -> current.copy(
            bufferingCount = current.bufferingCount + 1,
            lastUpdatedAtMs = nowMs
        )
    }
}

internal fun recordCdnProbeResult(
    current: CdnCandidateHealth,
    latencyMs: Long?,
    speedKbps: Long?,
    success: Boolean,
    nowMs: Long
): CdnCandidateHealth {
    val base = current.copy(
        manualProbeLatencyMs = latencyMs,
        manualProbeSpeedKbps = speedKbps,
        lastProbeAtMs = nowMs,
        lastUpdatedAtMs = nowMs
    )
    return if (success) {
        base.copy(readyCount = base.readyCount + 1)
    } else {
        base.copy(playbackErrorCount = base.playbackErrorCount + 1)
    }
}

internal fun resolveCdnProbeCandidates(
    urls: List<String>,
    healthByHost: Map<String, CdnCandidateHealth>,
    nowMs: Long,
    limit: CdnProbeLimit = CdnProbeLimit()
): List<CdnProbeCandidate> {
    return urls.distinctBy(::hostFromCdnUrl)
        .filter { hostFromCdnUrl(it).isNotBlank() }
        .take(limit.maxCandidates)
        .map { url ->
            val host = hostFromCdnUrl(url)
            val elapsed = nowMs - (healthByHost[host]?.lastProbeAtMs ?: 0L)
            val cooldownRemaining = (limit.cooldownMs - elapsed).coerceAtLeast(0L)
            CdnProbeCandidate(
                url = url,
                host = host,
                allowed = cooldownRemaining == 0L,
                cooldownRemainingMs = cooldownRemaining
            )
        }
}

internal fun buildCdnLineDiagnostics(
    urls: List<String>,
    healthByHost: Map<String, CdnCandidateHealth>
): List<CdnLineDiagnostic> {
    return urls.distinct().mapIndexed { index, url ->
        val host = hostFromCdnUrl(url)
        val health = healthByHost[host]
        val errorCount = (health?.playbackErrorCount ?: 0) + (health?.firstFrameTimeoutCount ?: 0)
        CdnLineDiagnostic(
            index = index,
            host = host.ifBlank { "未知线路" },
            statusLabel = resolveCdnHealthStatusLabel(health),
            latencyMs = health?.manualProbeLatencyMs,
            speedKbps = health?.manualProbeSpeedKbps,
            errorCount = errorCount,
            bufferingCount = health?.bufferingCount ?: 0,
            lastProbeAtMs = health?.lastProbeAtMs ?: 0L,
            score = scoreCdnCandidate(health)
        )
    }
}

internal fun buildCdnHostDiagnostics(
    hosts: List<String>,
    healthByHost: Map<String, CdnCandidateHealth>
): List<CdnHostDiagnostic> {
    return hosts.distinct().map { host ->
        val health = healthByHost[host]
        CdnHostDiagnostic(
            host = host,
            statusLabel = resolveCdnHealthStatusLabel(health),
            latencyMs = health?.manualProbeLatencyMs,
            speedKbps = health?.manualProbeSpeedKbps,
            errorCount = (health?.playbackErrorCount ?: 0) + (health?.firstFrameTimeoutCount ?: 0),
            lastProbeAtMs = health?.lastProbeAtMs ?: 0L
        )
    }
}

internal fun resolveCdnHealthStatusLabel(health: CdnCandidateHealth?): String {
    if (health == null || health.lastUpdatedAtMs <= 0L) return "未检测"
    val errors = health.playbackErrorCount + health.firstFrameTimeoutCount
    return when {
        errors >= 2 -> "不稳定"
        health.bufferingCount >= 3 -> "易缓冲"
        health.manualProbeSpeedKbps != null && health.manualProbeSpeedKbps >= 2_048L -> "速度较好"
        health.manualProbeLatencyMs != null && health.manualProbeLatencyMs <= 250L -> "延迟较低"
        health.readyCount > 0 -> "可用"
        else -> "已检测"
    }
}

internal fun shouldRefreshCdnIpLocation(
    enabled: Boolean,
    nowMs: Long,
    lastRefreshMs: Long,
    ttlMs: Long = CDN_REGION_LOCATION_TTL_MS,
    hasSelection: Boolean
): Boolean {
    if (!enabled) return false
    if (!hasSelection) return true
    if (lastRefreshMs <= 0L) return true
    return nowMs - lastRefreshMs >= ttlMs
}

internal fun resolveCdnRegionHosts(
    region: String,
    cachedHosts: List<String>,
    catalog: Map<String, List<String>>,
    isp: String = ""
): List<String> {
    val catalogHosts = catalog[region].orEmpty()
    if (cachedHosts.isEmpty()) return sortCdnRegionHostsForIsp(catalogHosts, isp)
    if (catalogHosts.isEmpty()) return sortCdnRegionHostsForIsp(cachedHosts.distinct(), isp)
    return if (cachedHosts.all { it in catalogHosts }) {
        sortCdnRegionHostsForIsp(cachedHosts.distinct(), isp)
    } else {
        sortCdnRegionHostsForIsp(catalogHosts, isp)
    }
}

internal fun hasUsableCdnRegionSelection(
    region: String,
    cachedHosts: List<String>,
    catalog: Map<String, List<String>>
): Boolean {
    if (region.isBlank() || cachedHosts.isEmpty()) return false
    val catalogHosts = catalog[region].orEmpty()
    if (catalogHosts.isEmpty()) return true
    return cachedHosts.all { it in catalogHosts }
}

internal const val CDN_REGION_LOCATION_TTL_MS: Long = 24L * 60L * 60L * 1000L
internal const val CDN_MANUAL_PROBE_COOLDOWN_MS: Long = 10L * 60L * 1000L

internal fun sortCdnRegionHostsForIsp(
    hosts: List<String>,
    isp: String
): List<String> {
    val carrier = resolveCarrierKey(isp)
    if (carrier == null) return hosts.distinct()
    return hosts.distinct().sortedWith(
        compareByDescending<String> { host -> scoreHostForCarrier(host, carrier) }
    )
}

internal fun maskIpAddressForLog(addr: String): String {
    val trimmed = addr.trim()
    if (trimmed.isBlank()) return ""
    val ipv4Parts = trimmed.split(".")
    if (ipv4Parts.size == 4 && ipv4Parts.all { it.isNotBlank() }) {
        return "${ipv4Parts[0]}.${ipv4Parts[1]}.*.*"
    }
    val ipv6Parts = trimmed.split(":")
    if (ipv6Parts.size > 2) {
        return ipv6Parts.take(2).joinToString(":") + ":*"
    }
    return "***"
}

private val mainlandCountryNames = setOf(
    "中国",
    "中国大陆",
    "中华人民共和国"
)

private val provinceRegionAliases = mapOf(
    "上海" to "上海",
    "北京" to "北京",
    "天津" to "天津",
    "重庆" to "成都",
    "广东" to "广州",
    "四川" to "成都",
    "陕西" to "西安",
    "湖北" to "武汉",
    "江苏" to "南京",
    "黑龙江" to "哈市",
    "内蒙古" to "呼市",
    "香港" to "香港",
    "澳门" to "澳门"
)

private val cityRegionAliases = mapOf(
    "重庆" to "成都",
    "广州" to "广州",
    "深圳" to "深圳",
    "成都" to "成都",
    "西安" to "西安",
    "武汉" to "武汉",
    "南京" to "南京",
    "哈尔滨" to "哈市",
    "呼和浩特" to "呼市",
    "香港" to "香港",
    "澳门" to "澳门"
)

private fun resolveCarrierKey(isp: String): String? {
    val normalized = isp.trim().lowercase()
    return when {
        normalized.contains("电信") || normalized.contains("chinanet") ||
            normalized.contains("telecom") -> "ct"
        normalized.contains("移动") || normalized.contains("cmcc") ||
            normalized.contains("mobile") -> "cm"
        normalized.contains("联通") || normalized.contains("unicom") -> "cu"
        normalized.contains("广电") || normalized.contains("broadcast") -> "gd"
        else -> null
    }
}

private fun scoreHostForCarrier(host: String, carrier: String): Int {
    val normalized = host.lowercase()
    return when {
        normalized.contains("-$carrier-") -> 3
        normalized.contains("-$carrier.") -> 3
        normalized.contains("$carrier-") -> 2
        normalized.contains(carrier) -> 1
        else -> 0
    }
}

private fun resolveMainlandRegionAlias(location: IpLocationSnapshot): String? {
    val city = normalizeRegionName(location.city)
    cityRegionAliases[city]?.let { return it }

    val province = normalizeRegionName(location.province)
    return provinceRegionAliases[province]
}

private fun normalizeRegionName(value: String): String {
    return value.trim()
        .removeSuffix("特别行政区")
        .removeSuffix("壮族自治区")
        .removeSuffix("回族自治区")
        .removeSuffix("维吾尔自治区")
        .removeSuffix("自治区")
        .removeSuffix("省")
        .removeSuffix("市")
}

private fun rewriteBilivideoHost(url: String, newHost: String): String? {
    val uri = runCatching { URI(url) }.getOrNull() ?: return null
    val originalHost = uri.host ?: return null
    if (originalHost != "bilivideo.com" && !originalHost.endsWith(".bilivideo.com")) return null
    if (newHost.isBlank()) return null

    val scheme = uri.scheme ?: return null
    val userInfo = uri.rawUserInfo?.let { "$it@" }.orEmpty()
    val port = if (uri.port >= 0) ":${uri.port}" else ""
    val path = uri.rawPath.orEmpty()
    val query = uri.rawQuery?.let { "?$it" }.orEmpty()
    val fragment = uri.rawFragment?.let { "#$it" }.orEmpty()
    return "$scheme://$userInfo$newHost$port$path$query$fragment"
}

internal fun hostFromCdnUrl(url: String): String {
    return runCatching { URI(url).host.orEmpty().lowercase() }.getOrDefault("")
}
