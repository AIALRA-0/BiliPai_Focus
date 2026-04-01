package com.android.purebilibili.feature.cast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SsdpDiscoveryPolicyTest {

    @Test
    fun `search payloads keep precise targets and add compatibility fallbacks`() {
        val payloads = SsdpDiscovery.resolveSsdpSearchPayloads()

        assertTrue(payloads.any { it.contains("ST: urn:schemas-upnp-org:device:MediaRenderer:1") })
        assertTrue(payloads.any { it.contains("ST: urn:schemas-upnp-org:service:AVTransport:1") })
        assertTrue(payloads.any { it.contains("ST: upnp:rootdevice") })
        assertTrue(payloads.any { it.contains("ST: ssdp:all") })
        assertEquals(payloads.size, payloads.distinct().size)
    }
}
