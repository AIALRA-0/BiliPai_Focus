package com.android.purebilibili.feature.cast

internal data class VisibleSsdpDevice(
    val device: SsdpDiscovery.SsdpDevice,
    val title: String,
    val subtitle: String
)

internal fun isSsdpRendererAdvertisement(device: SsdpDiscovery.SsdpDevice): Boolean =
    device.st.contains(":device:MediaRenderer:", ignoreCase = true) ||
        device.st.contains(":service:AVTransport:", ignoreCase = true)

internal fun preferSsdpDeviceAdvertisement(
    previous: SsdpDiscovery.SsdpDevice?,
    incoming: SsdpDiscovery.SsdpDevice,
): SsdpDiscovery.SsdpDevice =
    if (previous == null || (!isSsdpRendererAdvertisement(previous) && isSsdpRendererAdvertisement(incoming))) incoming
    else previous

internal inline fun <T, K, V> Iterable<T>.associateNotNullBy(
    keySelector: (T) -> K,
    valueSelector: (T) -> V?
): Map<K, V> {
    val result = LinkedHashMap<K, V>()
    for (item in this) {
        val value = try {
            valueSelector(item)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        } ?: continue
        result[keySelector(item)] = value
    }
    return result
}

internal fun resolveVisibleSsdpDevices(
    ssdpDevices: List<SsdpDiscovery.SsdpDevice>,
    profiles: Map<String, SsdpCastClient.SsdpDeviceProfile>
): List<VisibleSsdpDevice> {
    return ssdpDevices
        .distinctBy { it.location }
        .mapNotNull { device ->
            val profile = profiles[device.location]
            if (profile == null) {
                if (!isSsdpRendererAdvertisement(device)) return@mapNotNull null
                return@mapNotNull VisibleSsdpDevice(
                    device = device,
                    title = "DLNA 设备 · ${runCatching { java.net.URI(device.location).host }.getOrNull() ?: device.location}",
                    subtitle = "设备信息暂不可用，点击时重试",
                )
            }
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
