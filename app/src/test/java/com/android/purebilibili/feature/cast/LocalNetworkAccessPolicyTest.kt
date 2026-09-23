package com.android.purebilibili.feature.cast

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalNetworkAccessPolicyTest {
    @Test
    fun `api 37 uses local network permission`() {
        assertEquals(
            LocalNetworkPermissionRequirement.LOCAL_NETWORK,
            resolveLocalNetworkPermissionRequirement(37)
        )
        assertEquals(
            "android.permission.ACCESS_LOCAL_NETWORK",
            localNetworkRuntimePermissions(37).single()
        )
    }

    @Test
    fun `api 33 through 36 use nearby devices`() {
        (33..36).forEach { sdk ->
            assertEquals(
                LocalNetworkPermissionRequirement.NEARBY_WIFI_DEVICES,
                resolveLocalNetworkPermissionRequirement(sdk)
            )
        }
    }

    @Test
    fun `api 31 and 32 preserve location fallback`() {
        (31..32).forEach { sdk ->
            assertEquals(
                LocalNetworkPermissionRequirement.LOCATION,
                resolveLocalNetworkPermissionRequirement(sdk)
            )
            assertEquals(2, localNetworkRuntimePermissions(sdk).size)
        }
    }

    @Test
    fun `older versions need no runtime permission`() {
        assertEquals(LocalNetworkPermissionRequirement.NONE, resolveLocalNetworkPermissionRequirement(30))
        assertTrue(localNetworkRuntimePermissions(30).isEmpty())
    }
}
