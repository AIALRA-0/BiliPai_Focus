package com.android.purebilibili.core.network.policy

import java.net.InetSocketAddress
import java.net.Proxy

enum class ProxyRouteMode {
    /** 所有流量都走应用代理（历史行为）。 */
    GLOBAL,

    /** 仅命中 [AppHttpProxySettings.proxiedDomains] 的域名走代理，其余直连。 */
    SPLIT,
}

/**
 * App-level HTTP proxy settings for API traffic (login, feed, passport).
 * Distinct from cast [LocalProxyServer], which only rewrites media URLs for DLNA.
 */
data class AppHttpProxySettings(
    val enabled: Boolean = false,
    val host: String = "",
    val portText: String = "",
    val routeMode: ProxyRouteMode = ProxyRouteMode.GLOBAL,
    /** 分流模式的域名规则，每条形如 "aicu.cc"；后缀匹配其子域。 */
    val proxiedDomains: List<String> = emptyList(),
)

fun parseProxyPort(portText: String): Int? {
    val port = portText.trim().toIntOrNull() ?: return null
    return port.takeIf { it in 1..65535 }
}

fun isValidProxyHost(host: String): Boolean {
    val normalized = host.trim()
    if (normalized.isEmpty() || normalized.length > 253) return false
    if (normalized.any { it.isWhitespace() }) return false
    return true
}

fun isAppHttpProxyConfigured(settings: AppHttpProxySettings): Boolean {
    return isValidProxyHost(settings.host) && parseProxyPort(settings.portText) != null
}

fun resolveAppHttpProxyOrNull(settings: AppHttpProxySettings): Proxy? {
    if (!settings.enabled) return null
    val host = settings.host.trim()
    val port = parseProxyPort(settings.portText) ?: return null
    if (!isValidProxyHost(host)) return null
    return Proxy(Proxy.Type.HTTP, InetSocketAddress(host, port))
}

/**
 * 域名规则匹配：后缀匹配，规则 "aicu.cc" 命中 "aicu.cc" 与 "www.aicu.cc"。
 * 忽略大小写、协议前缀、路径与端口混入；host 为空或规则为空一律不命中。
 */
fun matchesProxyDomain(rules: List<String>, host: String?): Boolean {
    val target = host?.trim()?.lowercase()?.substringBefore(':')?.removeSuffix(".") ?: return false
    if (target.isEmpty()) return false
    return rules.any { raw ->
        val rule = raw.trim()
            .lowercase()
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore('/')
            .substringBefore(':')
            .removePrefix("*.")
            .removePrefix("www.")
            .removeSuffix(".")
            .trim()
        rule.isNotEmpty() && (target == rule || target.endsWith(".$rule"))
    }
}

private fun resolveDirectProxies(systemProxies: List<Proxy>): List<Proxy> =
    systemProxies.ifEmpty { listOf(Proxy.NO_PROXY) }

/**
 * Prefer configured app proxy; otherwise fall back to system proxy list
 * (VPN / Android system proxy). Empty system list becomes [Proxy.NO_PROXY].
 *
 * - GLOBAL：所有请求走应用代理（保持历史行为）。
 * - SPLIT：仅 [host] 命中域名规则时走应用代理。代理之后追加 [Proxy.NO_PROXY]，
 *   OkHttp 按路由列表依次尝试，代理不可用时自动回落直连；未命中的请求直连。
 */
fun selectAppHttpProxies(
    settings: AppHttpProxySettings,
    systemProxies: List<Proxy>,
    host: String? = null,
): List<Proxy> {
    val custom = resolveAppHttpProxyOrNull(settings)
    if (custom == null) return resolveDirectProxies(systemProxies)
    val useProxy = when (settings.routeMode) {
        ProxyRouteMode.GLOBAL -> true
        ProxyRouteMode.SPLIT -> matchesProxyDomain(settings.proxiedDomains, host)
    }
    if (!useProxy) return resolveDirectProxies(systemProxies)
    return if (settings.routeMode == ProxyRouteMode.SPLIT) {
        listOf(custom, Proxy.NO_PROXY)
    } else {
        listOf(custom)
    }
}

fun formatAppHttpProxyEndpoint(settings: AppHttpProxySettings): String {
    val host = settings.host.trim()
    val port = parseProxyPort(settings.portText)
    return if (host.isNotEmpty() && port != null) {
        "$host:$port"
    } else {
        "未配置"
    }
}

fun formatAppHttpProxySummary(settings: AppHttpProxySettings): String {
    val endpoint = formatAppHttpProxyEndpoint(settings)
    return if (settings.enabled) {
        when (settings.routeMode) {
            ProxyRouteMode.GLOBAL -> "已开启 · $endpoint"
            ProxyRouteMode.SPLIT -> "已开启（仅指定域名） · $endpoint"
        }
    } else {
        "已关闭 · $endpoint"
    }
}

/** 把多行输入解析成去重后的规则列表（空行跳过）。 */
fun parseProxyDomainRules(raw: String): List<String> = raw.lineSequence()
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .map { it.removePrefix("https://").removePrefix("http://").substringBefore('/').substringBefore(':').removePrefix("www.") }
    .distinct()
    .toList()

fun sanitizeProxyHostInput(raw: String): String = raw.trim().removePrefix("http://").removePrefix("https://")
    .substringBefore('/')
    .substringBefore(':')
    .trim()

fun sanitizeProxyPortInput(raw: String): String = raw.filter { it.isDigit() }.take(5)
