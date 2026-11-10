package com.android.purebilibili.feature.cast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SsdpCastClientTest {

    @Test
    fun `device URLBase resolves relative control paths`() {
        val xml = """
            <root xmlns="urn:schemas-upnp-org:device-1-0">
              <URLBase>http://192.168.1.9:8080/services/</URLBase>
              <device><serviceList><service>
                <serviceType>urn:schemas-upnp-org:service:AVTransport:1</serviceType>
                <controlURL>avtransport</controlURL>
              </service></serviceList></device>
            </root>
        """.trimIndent()
        assertEquals(
            "http://192.168.1.9:8080/services/avtransport",
            SsdpCastClient.parseAvTransportEndpoint(xml, "http://192.168.1.9:80/description.xml")?.controlUrl,
        )
    }

    @Test
    fun `metadata reports the actual media type and escapes media information`() {
        val metadata = SsdpCastClient.buildDidlMetadata(
            "http://192.168.1.2/manifest?a=1&b=2", "A & B", "作者", "application/dash+xml",
        )
        assertTrue(metadata.contains("protocolInfo=\"http-get:*:application/dash+xml:*\""))
        assertTrue(metadata.contains("A &amp; B"))
        assertTrue(metadata.contains("a=1&amp;b=2"))
        assertTrue(SsdpCastClient.buildDidlMetadata("x", "x", "x", "bad:type")
            .contains("protocolInfo=\"http-get:*:video/mp4:*\""))
    }

    @Test
    fun `cast poll interval drops to background cadence only while backgrounded`() {
        assertEquals(1_000L, resolveCastPlaybackPollIntervalMs(isInBackground = false))
        assertEquals(5_000L, resolveCastPlaybackPollIntervalMs(isInBackground = true))
    }

    @Test
    fun `parseAvTransportEndpoint resolves relative control URL`() {
        val descriptionXml = """
            <?xml version="1.0"?>
            <root xmlns="urn:schemas-upnp-org:device-1-0">
              <device>
                <serviceList>
                  <service>
                    <serviceType>urn:schemas-upnp-org:service:AVTransport:1</serviceType>
                    <controlURL>/MediaRenderer/AVTransport/Control</controlURL>
                  </service>
                </serviceList>
              </device>
            </root>
        """.trimIndent()

        val endpoint = SsdpCastClient.parseAvTransportEndpoint(
            descriptionXml = descriptionXml,
            descriptionLocation = "http://192.168.31.8:8899/rootDesc.xml"
        )

        assertNotNull(endpoint)
        assertEquals(
            "http://192.168.31.8:8899/MediaRenderer/AVTransport/Control",
            endpoint?.controlUrl
        )
        assertEquals("urn:schemas-upnp-org:service:AVTransport:1", endpoint?.serviceType)
    }

    @Test
    fun `parseDeviceProfile prefers embedded MediaRenderer over root device`() {
        val descriptionXml = """
            <?xml version="1.0"?>
            <root xmlns="urn:schemas-upnp-org:device-1-0">
              <device>
                <deviceType>urn:schemas-upnp-org:device:Basic:1</deviceType>
                <friendlyName>Root Hub</friendlyName>
                <modelName>Hub</modelName>
                <deviceList>
                  <device>
                    <deviceType>urn:schemas-upnp-org:device:MediaRenderer:1</deviceType>
                    <friendlyName>CastFlow TV</friendlyName>
                    <modelName>ATV</modelName>
                    <serviceList>
                      <service>
                        <serviceType>urn:schemas-upnp-org:service:AVTransport:1</serviceType>
                        <controlURL>/avt/control</controlURL>
                      </service>
                    </serviceList>
                  </device>
                </deviceList>
              </device>
            </root>
        """.trimIndent()

        val profile = SsdpCastClient.parseDeviceProfile(
            descriptionXml = descriptionXml,
            descriptionLocation = "http://192.168.1.20:8008/desc.xml"
        )

        assertNotNull(profile)
        assertEquals("CastFlow TV", profile?.friendlyName)
        assertEquals("ATV", profile?.modelName)
        assertEquals(
            "http://192.168.1.20:8008/avt/control",
            profile?.avTransportEndpoint?.controlUrl
        )
    }

    @Test
    fun `parseAvTransportEndpoint returns null when AVTransport not found`() {
        val descriptionXml = """
            <?xml version="1.0"?>
            <root>
              <device>
                <serviceList>
                  <service>
                    <serviceType>urn:schemas-upnp-org:service:RenderingControl:1</serviceType>
                    <controlURL>/MediaRenderer/RenderingControl/Control</controlURL>
                  </service>
                </serviceList>
              </device>
            </root>
        """.trimIndent()

        val endpoint = SsdpCastClient.parseAvTransportEndpoint(
            descriptionXml = descriptionXml,
            descriptionLocation = "http://192.168.31.8:8899/rootDesc.xml"
        )

        assertNull(endpoint)
    }

    @Test
    fun `buildSetUriActionBody escapes xml-sensitive characters`() {
        val body = SsdpCastClient.buildSetUriActionBody(
            serviceType = "urn:schemas-upnp-org:service:AVTransport:1",
            mediaUrl = "http://127.0.0.1:8901/proxy?url=a&b=c",
            metadata = "<tag attr=\"1\">value</tag>"
        )

        assertTrue(body.contains("&amp;"))
        assertTrue(body.contains("&lt;tag attr=&quot;1&quot;&gt;value&lt;/tag&gt;"))
    }

    @Test
    fun `buildSeekActionBody formats a DLNA relative time target`() {
        val body = SsdpCastClient.buildSeekActionBody(
            serviceType = "urn:schemas-upnp-org:service:AVTransport:1",
            positionMs = 3_726_500L
        )

        assertTrue(body.contains("<Unit>REL_TIME</Unit>"))
        assertTrue(body.contains("<Target>01:02:06</Target>"))
    }

    @Test
    fun `parseDlnaTimeMs parses valid duration and rejects malformed values`() {
        assertEquals(3_726_500L, SsdpCastClient.parseDlnaTimeMs("01:02:06.500"))
        assertEquals(0L, SsdpCastClient.parseDlnaTimeMs("NOT_IMPLEMENTED"))
        assertEquals(0L, SsdpCastClient.parseDlnaTimeMs("01:60:00"))
    }

    @Test
    fun `parseDeviceProfile returns null for blank XML`() {
        val profile = SsdpCastClient.parseDeviceProfile(
            descriptionXml = "   ",
            descriptionLocation = "http://192.168.31.8:8899/rootDesc.xml"
        )
        assertNull(profile)
    }

    @Test
    fun `parseDeviceProfile returns null for empty string`() {
        val profile = SsdpCastClient.parseDeviceProfile(
            descriptionXml = "",
            descriptionLocation = "http://192.168.31.8:8899/rootDesc.xml"
        )
        assertNull(profile)
    }

    @Test
    fun `parseDeviceProfile returns null for non-xml garbage`() {
        val profile = SsdpCastClient.parseDeviceProfile(
            descriptionXml = "{not xml at all}",
            descriptionLocation = "http://192.168.31.8:8899/rootDesc.xml"
        )
        assertNull(profile)
    }

    @Test
    fun `parseDeviceProfile accepts DOCTYPE with AVTransport`() {
        val descriptionXml = """
            <?xml version="1.0"?>
            <!DOCTYPE root [
              <!ENTITY myentity "myvalue">
            ]>
            <root xmlns="urn:schemas-upnp-org:device-1-0">
              <device>
                <friendlyName>TV with DOCTYPE</friendlyName>
                <modelName>Model X</modelName>
                <serviceList>
                  <service>
                    <serviceType>urn:schemas-upnp-org:service:AVTransport:1</serviceType>
                    <controlURL>/ctl</controlURL>
                  </service>
                </serviceList>
              </device>
            </root>
        """.trimIndent()

        val profile = SsdpCastClient.parseDeviceProfile(
            descriptionXml = descriptionXml,
            descriptionLocation = "http://192.168.31.8:8899/rootDesc.xml"
        )

        assertNotNull(profile)
        assertEquals("TV with DOCTYPE", profile?.friendlyName)
        assertEquals("Model X", profile?.modelName)
        assertEquals(
            "http://192.168.31.8:8899/ctl",
            profile?.avTransportEndpoint?.controlUrl
        )
    }

    @Test
    fun `parseDeviceProfile returns null for invalid XML even with DOCTYPE handling`() {
        val profile = SsdpCastClient.parseDeviceProfile(
            descriptionXml = "<root><unclosed>",
            descriptionLocation = "http://192.168.31.8:8899/rootDesc.xml"
        )
        assertNull(profile)
    }

    @Test
    fun `parseDeviceProfile with DOCTYPE and no AVTransport returns null endpoint`() {
        val descriptionXml = """
            <?xml version="1.0"?>
            <!DOCTYPE root [
              <!ENTITY foo "bar">
            ]>
            <root xmlns="urn:schemas-upnp-org:device-1-0">
              <device>
                <friendlyName>Non AV Device</friendlyName>
                <serviceList>
                  <service>
                    <serviceType>urn:schemas-upnp-org:service:RenderingControl:1</serviceType>
                    <controlURL>/rc/ctl</controlURL>
                  </service>
                </serviceList>
              </device>
            </root>
        """.trimIndent()

        val profile = SsdpCastClient.parseDeviceProfile(
            descriptionXml = descriptionXml,
            descriptionLocation = "http://192.168.31.8:8899/rootDesc.xml"
        )

        assertNotNull(profile)
        assertEquals("Non AV Device", profile?.friendlyName)
        assertNull(profile?.avTransportEndpoint)
    }

    @Test
    fun `parseDeviceProfile extracts friendly name and avtransport endpoint`() {
        val descriptionXml = """
            <?xml version="1.0"?>
            <root xmlns="urn:schemas-upnp-org:device-1-0">
              <device>
                <friendlyName>Living Room TV</friendlyName>
                <modelName>Xiaomi TV</modelName>
                <serviceList>
                  <service>
                    <serviceType>urn:schemas-upnp-org:service:AVTransport:1</serviceType>
                    <controlURL>/MediaRenderer/AVTransport/Control</controlURL>
                  </service>
                </serviceList>
              </device>
            </root>
        """.trimIndent()

        val profile = SsdpCastClient.parseDeviceProfile(
            descriptionXml = descriptionXml,
            descriptionLocation = "http://192.168.31.8:8899/rootDesc.xml"
        )

        assertNotNull(profile)
        assertEquals("Living Room TV", profile?.friendlyName)
        assertEquals("Xiaomi TV", profile?.modelName)
        assertEquals(
            "http://192.168.31.8:8899/MediaRenderer/AVTransport/Control",
            profile?.avTransportEndpoint?.controlUrl
        )
    }
}
