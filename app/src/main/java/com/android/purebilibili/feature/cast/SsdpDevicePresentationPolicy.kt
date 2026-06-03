package com.android.purebilibili.feature.cast

internal data class VisibleSsdpDevice(
    val device: SsdpDiscovery.SsdpDevice,
    val title: String,
    val subtitle: String
)

internal fun shouldIncludeClingDevice(
    hasAvTransport: Boolean
): Boolean = hasAvTransport

internal inline fun <T, K, V> Iterable<T>.associateNotNullBy(
    keySelector: (T) -> K,
    valueSelector: (T) -> V?
): Map<K, V> {
    val result = LinkedHashMap<K, V>()
    for (item in this) {
        val value = runCatching { valueSelector(item) }.getOrNull() ?: continue
        result[keySelector(item)] = value
    }
    return result
}

internal fun resolveVisibleSsdpDevices(
    clingDevices: List<CastDeviceInfo>,
    ssdpDevices: List<SsdpDiscovery.SsdpDevice>,
    profiles: Map<String, SsdpCastClient.SsdpDeviceProfile>
): List<VisibleSsdpDevice> {
    if (clingDevices.isNotEmpty()) return emptyList()

    return ssdpDevices
        .distinctBy { it.location }
        .mapNotNull { device ->
            val profile = profiles[device.location] ?: return@mapNotNull null
            if (profile.avTransportEndpoint == null) return@mapNotNull null

            VisibleSsdpDevice(
                device = device,
                title = profile.friendlyName
                    .takeIf { it.isNotBlank() }
                    ?: device.server.ifBlank { "DLNA Device" },
                subtitle = profile.modelName
                    ?.takeIf { it.isNotBlank() }
                    ?: device.st.substringAfterLast(":").ifBlank { device.location }
            )
        }
}
