package com.android.purebilibili.core.plugin

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PluginManagerPolicyTest {

    @Test
    fun pendingEnabledOverride_winsDuringRegistrationAndIsConsumed() {
        val pendingOverrides = mutableMapOf(
            "sponsor_block" to true
        )

        val resolved = consumePendingPluginEnabledState(
            pluginId = "sponsor_block",
            storedEnabled = false,
            pendingEnabledOverrides = pendingOverrides
        )

        assertTrue(resolved)
        assertFalse(pendingOverrides.containsKey("sponsor_block"))
    }

    @Test
    fun storedEnabledState_usedWhenNoPendingOverrideExists() {
        val pendingOverrides = mutableMapOf<String, Boolean>()

        val resolved = consumePendingPluginEnabledState(
            pluginId = "sponsor_block",
            storedEnabled = false,
            pendingEnabledOverrides = pendingOverrides
        )

        assertEquals(false, resolved)
    }

    @Test
    fun updatePluginEnabledStateReturnsANewSnapshot() {
        val plugin = object : Plugin {
            override val id = "test"
            override val name = "Test"
            override val description = "Test plugin"
            override val version = "1.0.0"
        }
        val original = listOf(PluginInfo(plugin, enabled = false))

        val updated = updatePluginEnabledState(
            plugins = original,
            pluginId = plugin.id,
            enabled = true
        )

        assertFalse(original.first().enabled)
        assertTrue(updated.first().enabled)
    }
}
