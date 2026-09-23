package com.android.purebilibili.feature.cast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SsdpLocalNetworkPolicyTest {

    @Test
    fun `wifi without vpn scores highest among local transports`() {
        val wifi = scoreLocalNetwork(
            hasWifi = true,
            hasEthernet = false,
            hasVpn = false,
            notVpnCapability = true
        )
        val wifiVpn = scoreLocalNetwork(
            hasWifi = true,
            hasEthernet = false,
            hasVpn = true,
            notVpnCapability = false
        )
        val cellular = scoreLocalNetwork(
            hasWifi = false,
            hasEthernet = false,
            hasVpn = false,
            notVpnCapability = true
        )

        assertTrue(wifi > wifiVpn)
        assertEquals(-1, cellular)
    }

    @Test
    fun `ethernet is accepted as local transport`() {
        val score = scoreLocalNetwork(
            hasWifi = false,
            hasEthernet = true,
            hasVpn = false,
            notVpnCapability = true
        )
        assertTrue(score > 0)
    }

    @Test
    fun `usable ssdp messages accept 200 OK and alive notify`() {
        assertTrue(isUsableSsdpDiscoveryMessage("HTTP/1.1 200 OK", nts = ""))
        assertTrue(isUsableSsdpDiscoveryMessage("NOTIFY * HTTP/1.1", nts = "ssdp:alive"))
        assertFalse(isUsableSsdpDiscoveryMessage("NOTIFY * HTTP/1.1", nts = "ssdp:byebye"))
        assertFalse(isUsableSsdpDiscoveryMessage("M-SEARCH * HTTP/1.1", nts = ""))
    }

    @Test
    fun `parseResponse accepts notify alive advertisements`() {
        val message = """
            NOTIFY * HTTP/1.1
            HOST: 239.255.255.250:1900
            CACHE-CONTROL: max-age=1800
            LOCATION: http://192.168.1.20:49152/description.xml
            NT: urn:schemas-upnp-org:device:MediaRenderer:1
            NTS: ssdp:alive
            USN: uuid:castflow-1::urn:schemas-upnp-org:device:MediaRenderer:1
            SERVER: Linux/5.10 UPnP/1.0 CastFlow/1.0
        """.trimIndent().replace("\n", "\r\n")

        val device = SsdpDiscovery.parseResponse(message)
        assertNotNull(device)
        assertEquals("http://192.168.1.20:49152/description.xml", device?.location)
        assertEquals("uuid:castflow-1::urn:schemas-upnp-org:device:MediaRenderer:1", device?.usn)
        assertEquals("urn:schemas-upnp-org:device:MediaRenderer:1", device?.st)
        assertTrue(device?.server?.contains("CastFlow") == true)
    }

    @Test
    fun `parseResponse rejects byebye notify`() {
        val message = """
            NOTIFY * HTTP/1.1
            LOCATION: http://192.168.1.20:49152/description.xml
            NT: urn:schemas-upnp-org:device:MediaRenderer:1
            NTS: ssdp:byebye
            USN: uuid:gone
        """.trimIndent().replace("\n", "\r\n")

        assertNull(SsdpDiscovery.parseResponse(message))
    }

    @Test
    fun `parseResponse still accepts classic m-search replies`() {
        val message = """
            HTTP/1.1 200 OK
            CACHE-CONTROL: max-age=1800
            EXT:
            LOCATION: http://192.168.31.8:8899/rootDesc.xml
            SERVER: Linux/3.10 UPnP/1.0
            ST: urn:schemas-upnp-org:service:AVTransport:1
            USN: uuid:renderer-1::urn:schemas-upnp-org:service:AVTransport:1
        """.trimIndent().replace("\n", "\r\n")

        val device = SsdpDiscovery.parseResponse(message)
        assertNotNull(device)
        assertEquals("http://192.168.31.8:8899/rootDesc.xml", device?.location)
    }
}
