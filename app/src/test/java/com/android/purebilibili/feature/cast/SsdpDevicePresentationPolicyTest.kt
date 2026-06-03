package com.android.purebilibili.feature.cast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SsdpDevicePresentationPolicyTest {

    // --- associateNotNullBy (per-device exception isolation) ---

    @Test
    fun `associateNotNullBy maps successful devices while ignoring failed ones`() {
        val data = listOf("a", "b", "c", "d")
        val result = data.associateNotNullBy(
            keySelector = { it },
            valueSelector = { key ->
                when (key) {
                    "a" -> "success-a"
                    "b" -> null // device without AVTransport
                    "c" -> throw RuntimeException("socket timeout") // crashing device
                    "d" -> "success-d"
                    else -> null
                }
            }
        )
        assertEquals(2, result.size)
        assertEquals("success-a", result["a"])
        assertEquals("success-d", result["d"])
        assertNull(result["b"])
        assertNull(result["c"])
    }

    @Test
    fun `associateNotNullBy returns empty map when all valueSelectors return null`() {
        val data = listOf("a", "b")
        val result = data.associateNotNullBy(
            keySelector = { it },
            valueSelector = { null }
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `associateNotNullBy returns empty map for empty input`() {
        val result = emptyList<String>().associateNotNullBy(
            keySelector = { it },
            valueSelector = { it }
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `associateNotNullBy preserves insertion order`() {
        val data = listOf("first", "second", "third")
        val result = data.associateNotNullBy(
            keySelector = { it },
            valueSelector = { it.uppercase() }
        )
        val keys = result.keys.toList()
        assertEquals(listOf("first", "second", "third"), keys)
    }

    // --- cling / ssdp device presentation ---

    @Test
    fun `cling devices require avtransport to be considered castable`() {
        assertTrue(shouldIncludeClingDevice(hasAvTransport = true))
        assertTrue(!shouldIncludeClingDevice(hasAvTransport = false))
    }

    @Test
    fun `ssdp fallback devices stay hidden when cling devices already exist`() {
        val visible = resolveVisibleSsdpDevices(
            clingDevices = listOf(
                CastDeviceInfo(
                    udn = "renderer-1",
                    name = "Living Room TV",
                    description = "MediaRenderer",
                    location = "http://192.168.31.8:8899/rootDesc.xml"
                )
            ),
            ssdpDevices = listOf(
                SsdpDiscovery.SsdpDevice(
                    location = "http://192.168.31.9:8899/rootDesc.xml",
                    server = "Linux/3.10 DLNADOC/1.50",
                    usn = "uuid:renderer-2",
                    st = "urn:schemas-upnp-org:device:MediaRenderer:1"
                )
            ),
            profiles = mapOf(
                "http://192.168.31.9:8899/rootDesc.xml" to SsdpCastClient.SsdpDeviceProfile(
                    friendlyName = "Bedroom TV",
                    modelName = "Xiaomi",
                    avTransportEndpoint = SsdpCastClient.AvTransportEndpoint(
                        controlUrl = "http://192.168.31.9:8899/control",
                        serviceType = "urn:schemas-upnp-org:service:AVTransport:1"
                    )
                )
            )
        )

        assertTrue(visible.isEmpty())
    }

    @Test
    fun `ssdp fallback devices require avtransport and use friendly name`() {
        val visible = resolveVisibleSsdpDevices(
            clingDevices = emptyList(),
            ssdpDevices = listOf(
                SsdpDiscovery.SsdpDevice(
                    location = "http://192.168.31.9:8899/rootDesc.xml",
                    server = "Linux/3.10 DLNADOC/1.50",
                    usn = "uuid:renderer-2",
                    st = "urn:schemas-upnp-org:device:MediaRenderer:1"
                ),
                SsdpDiscovery.SsdpDevice(
                    location = "http://192.168.31.10:8899/rootDesc.xml",
                    server = "Random NAS",
                    usn = "uuid:nas-1",
                    st = "upnp:rootdevice"
                )
            ),
            profiles = mapOf(
                "http://192.168.31.9:8899/rootDesc.xml" to SsdpCastClient.SsdpDeviceProfile(
                    friendlyName = "Bedroom TV",
                    modelName = "Xiaomi",
                    avTransportEndpoint = SsdpCastClient.AvTransportEndpoint(
                        controlUrl = "http://192.168.31.9:8899/control",
                        serviceType = "urn:schemas-upnp-org:service:AVTransport:1"
                    )
                ),
                "http://192.168.31.10:8899/rootDesc.xml" to SsdpCastClient.SsdpDeviceProfile(
                    friendlyName = "NAS",
                    modelName = "Storage",
                    avTransportEndpoint = null
                )
            )
        )

        assertEquals(1, visible.size)
        assertEquals("Bedroom TV", visible.first().title)
        assertEquals("Xiaomi", visible.first().subtitle)
    }
}
