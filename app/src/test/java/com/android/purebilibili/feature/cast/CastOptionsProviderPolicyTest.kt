package com.android.purebilibili.feature.plugin.googlecast

import org.junit.Assert.assertEquals
import org.junit.Test

class CastOptionsProviderPolicyTest {

    @Test
    fun `default receiver app id is the standard CAF default media receiver`() {
        val appId = CastReceiverPolicy.resolveReceiverApplicationId()
        assertEquals("CC1AD845", appId)
    }

    @Test
    fun `additional session providers returns null`() {
        val providers = CastReceiverPolicy.resolveAdditionalSessionProviders()
        assertEquals(null, providers)
    }

    @Test
    fun `provider class name constant matches the actual class`() {
        assertEquals(
            "com.android.purebilibili.feature.plugin.googlecast.CastOptionsProvider",
            CastReceiverPolicy.OPTIONS_PROVIDER_CLASS_NAME
        )
    }
}
