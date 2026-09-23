package com.android.purebilibili.feature.plugin.dlna

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tv
import com.android.purebilibili.core.plugin.CastPluginRoute
import com.android.purebilibili.feature.cast.SsdpDiscovery
import com.android.purebilibili.feature.cast.VisibleSsdpDevice

sealed class DlnaRouteSelection {
    data class Ssdp(val device: SsdpDiscovery.SsdpDevice) : DlnaRouteSelection()
}

internal fun toDlnaCastRoute(device: VisibleSsdpDevice): CastPluginRoute = CastPluginRoute(
    routeId = "ssdp:${device.device.location}",
    name = device.title,
    description = device.subtitle,
    icon = Icons.Rounded.Tv
)

data class DlnaRouteSnapshot(
    val routes: List<CastPluginRoute>,
    val ssdpCache: Map<String, SsdpDiscovery.SsdpDevice>
)

internal fun buildDlnaRouteSnapshot(
    visibleSsdpDevices: List<VisibleSsdpDevice>
): DlnaRouteSnapshot {
    val ssdpRoutes = visibleSsdpDevices.map { toDlnaCastRoute(it) }
    return DlnaRouteSnapshot(
        routes = ssdpRoutes,
        ssdpCache = visibleSsdpDevices.associate { toDlnaCastRoute(it).routeId to it.device }
    )
}

fun resolveDlnaRouteSelection(
    routeId: String,
    ssdpCache: Map<String, SsdpDiscovery.SsdpDevice>
): DlnaRouteSelection? {
    val separatorIndex = routeId.indexOf(':')
    if (separatorIndex < 0) return null
    val prefix = routeId.substring(0, separatorIndex)
    val suffix = routeId.substring(separatorIndex + 1)
    if (suffix.isEmpty()) return null

    return when (prefix) {
        "ssdp" -> {
            val device = ssdpCache[routeId] ?: return null
            DlnaRouteSelection.Ssdp(device)
        }
        else -> null
    }
}
