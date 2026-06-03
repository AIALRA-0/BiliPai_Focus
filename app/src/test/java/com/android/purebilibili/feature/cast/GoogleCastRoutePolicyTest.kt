package com.android.purebilibili.feature.plugin.googlecast

import androidx.mediarouter.media.MediaRouter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GoogleCastRoutePolicyTest {

    // --- toCastPluginRoute ---

    @Test
    fun `returns null for default route when isDefaultOrBluetooth true`() {
        val result = toCastPluginRoute(
            routeId = "default_route",
            name = "Phone",
            description = null,
            deviceType = MediaRouter.RouteInfo.DEVICE_TYPE_UNKNOWN,
            isDefaultOrBluetooth = true,
            supportsCastCategory = true
        )
        assertNull(result)
    }

    @Test
    fun `returns null for bluetooth route when isDefaultOrBluetooth true`() {
        val result = toCastPluginRoute(
            routeId = "bt_route",
            name = "Headphones",
            description = null,
            deviceType = MediaRouter.RouteInfo.DEVICE_TYPE_UNKNOWN,
            isDefaultOrBluetooth = true,
            supportsCastCategory = true
        )
        assertNull(result)
    }

    @Test
    fun `returns null when supportsCastCategory false`() {
        val result = toCastPluginRoute(
            routeId = "speaker_route",
            name = "Speaker",
            description = null,
            deviceType = MediaRouter.RouteInfo.DEVICE_TYPE_UNKNOWN,
            isDefaultOrBluetooth = false,
            supportsCastCategory = false
        )
        assertNull(result)
    }

    @Test
    fun `maps valid cast route routeId name description correctly`() {
        val result = toCastPluginRoute(
            routeId = "cast_route_1",
            name = "Living Room TV",
            description = "Chromecast with Google TV",
            deviceType = MediaRouter.RouteInfo.DEVICE_TYPE_TV,
            isDefaultOrBluetooth = false,
            supportsCastCategory = true
        )
        assertNotNull(result)
        assertEquals("cast_route_1", result!!.routeId)
        assertEquals("Living Room TV", result.name)
        assertEquals("Chromecast with Google TV", result.description)
    }

    @Test
    fun `blank name fallback to Unknown Device`() {
        val result = toCastPluginRoute(
            routeId = "cast_route_2",
            name = "   ",
            description = null,
            deviceType = MediaRouter.RouteInfo.DEVICE_TYPE_TV,
            isDefaultOrBluetooth = false,
            supportsCastCategory = true
        )
        assertNotNull(result)
        assertEquals("Unknown Device", result!!.name)
    }

    @Test
    fun `empty name fallback to Unknown Device`() {
        val result = toCastPluginRoute(
            routeId = "cast_route_3",
            name = "",
            description = null,
            deviceType = MediaRouter.RouteInfo.DEVICE_TYPE_UNKNOWN,
            isDefaultOrBluetooth = false,
            supportsCastCategory = true
        )
        assertNotNull(result)
        assertEquals("Unknown Device", result!!.name)
    }

    @Test
    fun `preserves null description`() {
        val result = toCastPluginRoute(
            routeId = "cast_route_4",
            name = "Bedroom",
            description = null,
            deviceType = MediaRouter.RouteInfo.DEVICE_TYPE_UNKNOWN,
            isDefaultOrBluetooth = false,
            supportsCastCategory = true
        )
        assertNotNull(result)
        assertNull(result!!.description)
    }
}
