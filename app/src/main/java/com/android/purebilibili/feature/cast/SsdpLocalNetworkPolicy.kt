package com.android.purebilibili.feature.cast

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.LinkProperties
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Prefer real LAN transports for SSDP/DLNA discovery.
 *
 * Dual-network phones (Wi-Fi + cellular) and VPN often make [ConnectivityManager.activeNetwork]
 * unsuitable for multicast: cellular cannot deliver LAN SSDP, and VPN hijacks the default route.
 */
internal data class SsdpLocalNetworkBinding(
    val network: Network?,
    val networkInterface: NetworkInterface?,
    val ipv4Address: Inet4Address?,
    val transportLabel: String,
)

internal fun scoreLocalNetwork(
    hasWifi: Boolean,
    hasEthernet: Boolean,
    hasVpn: Boolean,
    notVpnCapability: Boolean,
): Int {
    if (!hasWifi && !hasEthernet) return -1
    var score = 0
    if (hasWifi) score += 10
    if (hasEthernet) score += 8
    if (!hasVpn) score += 5
    if (notVpnCapability) score += 3
    return score
}

internal fun resolveSsdpLocalNetworkBinding(
    connectivityManager: ConnectivityManager,
): SsdpLocalNetworkBinding {
    val candidates = connectivityManager.allNetworks.mapNotNull { network ->
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return@mapNotNull null
        val hasWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val hasEthernet = caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        val hasVpn = caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        val notVpn = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
        val score = scoreLocalNetwork(
            hasWifi = hasWifi,
            hasEthernet = hasEthernet,
            hasVpn = hasVpn,
            notVpnCapability = notVpn,
        )
        if (score < 0) return@mapNotNull null
        val transportLabel = when {
            hasWifi && hasVpn -> "wifi+vpn"
            hasWifi -> "wifi"
            hasEthernet && hasVpn -> "ethernet+vpn"
            hasEthernet -> "ethernet"
            else -> "local"
        }
        Triple(network, score, transportLabel)
    }

    val preferred = candidates.maxByOrNull { it.second }
    if (preferred == null) {
        return SsdpLocalNetworkBinding(
            network = connectivityManager.activeNetwork,
            networkInterface = null,
            ipv4Address = null,
            transportLabel = "none",
        )
    }

    val network = preferred.first
    val linkProperties = connectivityManager.getLinkProperties(network)
    val ipv4 = resolveIpv4Address(linkProperties)
    val nif = ipv4?.let { runCatching { NetworkInterface.getByInetAddress(it) }.getOrNull() }

    return SsdpLocalNetworkBinding(
        network = network,
        networkInterface = nif,
        ipv4Address = ipv4,
        transportLabel = preferred.third,
    )
}

internal fun resolveIpv4Address(linkProperties: LinkProperties?): Inet4Address? {
    return linkProperties
        ?.linkAddresses
        ?.map { it.address }
        ?.firstOrNull { address -> address is Inet4Address && !address.isLoopbackAddress }
        as? Inet4Address
}

/**
 * Accept both M-SEARCH unicast replies and passive ssdp:alive NOTIFY adverts.
 */
internal fun isUsableSsdpDiscoveryMessage(firstLine: String, nts: String): Boolean {
    val line = firstLine.trim()
    if (line.startsWith("HTTP/1.", ignoreCase = true) && line.contains("200")) return true
    if (line.startsWith("NOTIFY", ignoreCase = true)) {
        return nts.isBlank() || nts.equals("ssdp:alive", ignoreCase = true)
    }
    return false
}
