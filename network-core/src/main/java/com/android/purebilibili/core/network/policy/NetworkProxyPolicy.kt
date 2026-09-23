package com.android.purebilibili.core.network.policy

import java.net.InetSocketAddress
import java.net.Proxy

/**
 * App-level HTTP proxy settings for API traffic (login, feed, passport).
 * Distinct from cast [LocalProxyServer], which only rewrites media URLs for DLNA.
 */
data class AppHttpProxySettings(
    val enabled: Boolean = false,
    val host: String = "",
    val portText: String = "",
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
 * Prefer configured app proxy; otherwise fall back to system proxy list
 * (VPN / Android system proxy). Empty system list becomes [Proxy.NO_PROXY].
 */
fun selectAppHttpProxies(
    settings: AppHttpProxySettings,
    systemProxies: List<Proxy>,
): List<Proxy> {
    val custom = resolveAppHttpProxyOrNull(settings)
    if (custom != null) return listOf(custom)
    return systemProxies.ifEmpty { listOf(Proxy.NO_PROXY) }
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
        "已开启 · $endpoint"
    } else {
        "已关闭 · $endpoint"
    }
}

fun sanitizeProxyHostInput(raw: String): String = raw.trim().removePrefix("http://").removePrefix("https://")
    .substringBefore('/')
    .substringBefore(':')
    .trim()

fun sanitizeProxyPortInput(raw: String): String = raw.filter { it.isDigit() }.take(5)
