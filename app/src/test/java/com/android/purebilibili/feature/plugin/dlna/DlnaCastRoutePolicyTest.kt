package com.android.purebilibili.feature.plugin.dlna

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tv
import com.android.purebilibili.feature.cast.SsdpDiscovery
import com.android.purebilibili.feature.cast.VisibleSsdpDevice
import org.junit.Assert.*
import org.junit.Test

class DlnaCastRoutePolicyTest {

    // ── toDlnaCastRoute ────────────────────────────────────────────────────────

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
            ssdpCache
        )

        assertTrue(selection is DlnaRouteSelection.Ssdp)
        assertEquals(ssdpDevice, (selection as DlnaRouteSelection.Ssdp).device)
    }

    @Test
    fun `route selection ignores unknown routes`() {
        assertNull(resolveDlnaRouteSelection("google:abc", emptyMap()))
        assertNull(resolveDlnaRouteSelection("ssdp:", emptyMap()))
        assertNull(resolveDlnaRouteSelection("bad", emptyMap()))
    }

    // ── buildDlnaRouteSnapshot ──────────────────────────────────────────────────

    @Test
    fun `snapshot builds routes and cache from ssdp devices`() {
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

        val snapshot = buildDlnaRouteSnapshot(listOf(visibleSsdpDevice))

        assertEquals(
            listOf("ssdp:http://192.168.1.11/root.xml"),
            snapshot.routes.map { it.routeId }
        )
        assertEquals(ssdpDevice, snapshot.ssdpCache["ssdp:http://192.168.1.11/root.xml"])
    }
}
