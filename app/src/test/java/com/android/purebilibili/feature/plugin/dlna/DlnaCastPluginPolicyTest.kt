package com.android.purebilibili.feature.plugin.dlna

import com.android.purebilibili.core.plugin.CastDiscoveryRequirement
import kotlin.test.Test
import kotlin.test.assertEquals

class DlnaCastPluginPolicyTest {
    @Test
    fun `dlna declares raw local network discovery`() {
        assertEquals(
            CastDiscoveryRequirement.RAW_LOCAL_NETWORK,
            DlnaCastPlugin().discoveryRequirement
        )
    }
}
