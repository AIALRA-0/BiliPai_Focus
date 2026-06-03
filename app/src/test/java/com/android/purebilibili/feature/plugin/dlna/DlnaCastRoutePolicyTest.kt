package com.android.purebilibili.feature.plugin.dlna

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tv
import com.android.purebilibili.feature.cast.CastDeviceInfo
import com.android.purebilibili.feature.cast.SsdpDiscovery
import com.android.purebilibili.feature.cast.VisibleSsdpDevice
import org.junit.Assert.*
import org.junit.Test

class DlnaCastRoutePolicyTest {

    // ── toDlnaCastRoute ────────────────────────────────────────────────────────

    @Test
    fun `cling route uses udn and fallback labels`() {
        val device = CastDeviceInfo(
            udn = "uuid:tv-1",
            name = "",
            description = "",
            location = "http://192.168.1.10/root.xml"
        )
        val route = toDlnaCastRoute(device)

        assertEquals("cling:uuid:tv-1", route.routeId)
        assertEquals("Unknown Device", route.name)
        assertEquals("http://192.168.1.10/root.xml", route.description)
        assertEquals(Icons.Rounded.Tv, route.icon)
    }

    @Test
    fun `ssdp route uses location and visible labels`() {
        val device = SsdpDiscovery.SsdpDevice(
            location = "http://192.168.1.11/root.xml",
            server = "Linux DLNA",
            usn = "uuid:ssdp-1",
            st = "urn:schemas-upnp-org:device:MediaRenderer:1"
        )
        val visible = VisibleSsdpDevice(
            device = device,
            title = "Bedroom TV",
            subtitle = "Xiaomi"
        )
        val route = toDlnaCastRoute(visible)

        assertEquals("ssdp:http://192.168.1.11/root.xml", route.routeId)
        assertEquals("Bedroom TV", route.name)
        assertEquals("Xiaomi", route.description)
        assertEquals(Icons.Rounded.Tv, route.icon)
    }

    // ── resolveDlnaRouteSelection ──────────────────────────────────────────────

    @Test
    fun `route selection resolves cling route from cache`() {
        val castDevice = CastDeviceInfo(
            udn = "uuid:tv-1",
            name = "TV",
            description = "",
            location = "http://192.168.1.10/root.xml"
        )
        val clingCache = mapOf("cling:uuid:tv-1" to castDevice)

        val selection = resolveDlnaRouteSelection("cling:uuid:tv-1", clingCache, emptyMap())

        assertTrue(selection is DlnaRouteSelection.Cling)
        assertEquals(castDevice, (selection as DlnaRouteSelection.Cling).device)
    }

    @Test
    fun `route selection resolves ssdp route from cache`() {
        val ssdpDevice = SsdpDiscovery.SsdpDevice(
            location = "http://192.168.1.11/root.xml",
            server = "Linux DLNA",
            usn = "uuid:ssdp-1",
            st = "urn:schemas-upnp-org:device:MediaRenderer:1"
        )
        val ssdpCache = mapOf("ssdp:http://192.168.1.11/root.xml" to ssdpDevice)

        val selection = resolveDlnaRouteSelection(
            "ssdp:http://192.168.1.11/root.xml",
            emptyMap(),
            ssdpCache
        )

        assertTrue(selection is DlnaRouteSelection.Ssdp)
        assertEquals(ssdpDevice, (selection as DlnaRouteSelection.Ssdp).device)
    }

    @Test
    fun `route selection ignores unknown routes`() {
        assertNull(resolveDlnaRouteSelection("google:abc", emptyMap(), emptyMap()))
        assertNull(resolveDlnaRouteSelection("cling:", emptyMap(), emptyMap()))
        assertNull(resolveDlnaRouteSelection("ssdp:", emptyMap(), emptyMap()))
        assertNull(resolveDlnaRouteSelection("bad", emptyMap(), emptyMap()))
    }

    // ── buildDlnaRouteSnapshot ──────────────────────────────────────────────────

    @Test
    fun `snapshot builds routes and caches from cling and ssdp devices`() {
        val clingDevice = CastDeviceInfo(
            udn = "uuid:tv-1",
            name = "Living Room",
            description = "MediaRenderer",
            location = "http://192.168.1.10/root.xml"
        )
        val ssdpDevice = SsdpDiscovery.SsdpDevice(
            location = "http://192.168.1.11/root.xml",
            server = "Linux DLNA",
            usn = "uuid:ssdp-1",
            st = "urn:schemas-upnp-org:device:MediaRenderer:1"
        )
        val visibleSsdpDevice = VisibleSsdpDevice(
            device = ssdpDevice,
            title = "Bedroom TV",
            subtitle = "Xiaomi"
        )

        val snapshot = buildDlnaRouteSnapshot(listOf(clingDevice), listOf(visibleSsdpDevice))

        assertEquals(
            listOf("cling:uuid:tv-1", "ssdp:http://192.168.1.11/root.xml"),
            snapshot.routes.map { it.routeId }
        )
        assertEquals(clingDevice, snapshot.clingCache["cling:uuid:tv-1"])
        assertEquals(ssdpDevice, snapshot.ssdpCache["ssdp:http://192.168.1.11/root.xml"])
    }
}
