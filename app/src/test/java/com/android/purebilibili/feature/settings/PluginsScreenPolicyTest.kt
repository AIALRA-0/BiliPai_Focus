package com.android.purebilibili.feature.settings

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PluginsScreenPolicyTest {

    @Test
    fun sponsorBlockToggle_routesThroughSettingsPath() = runTest {
        var sponsorBlockToggleCount = 0
        var genericToggleCount = 0

        dispatchBuiltInPluginToggle(
            pluginId = "sponsor_block",
            enabled = true,
            onSponsorBlockToggle = { sponsorBlockToggleCount += 1 },
            onGenericPluginToggle = { _, _ -> genericToggleCount += 1 }
        )

        assertEquals(1, sponsorBlockToggleCount)
        assertEquals(0, genericToggleCount)
    }

    @Test
    fun nonSponsorBlockToggle_routesThroughGenericPluginPath() = runTest {
        var sponsorBlockToggleCount = 0
        var genericToggleCount = 0

        dispatchBuiltInPluginToggle(
            pluginId = "danmaku_enhance",
            enabled = false,
            onSponsorBlockToggle = { sponsorBlockToggleCount += 1 },
            onGenericPluginToggle = { _, _ -> genericToggleCount += 1 }
        )

        assertEquals(0, sponsorBlockToggleCount)
        assertEquals(1, genericToggleCount)
    }
}
