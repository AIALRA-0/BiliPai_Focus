package com.android.purebilibili.plugin.sdk

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class PluginSdkManifestTest {

    @Test
    fun manifestRoundTripsAsStablePluginJson() {
        val manifest = PluginCapabilityManifest(
            pluginId = "dev.example.today_watch_remix",
            displayName = "今日推荐单 Remix",
            version = "1.0.0",
            apiVersion = 1,
            entryClassName = "dev.example.TodayWatchRemixPlugin",
            capabilities = setOf(
                PluginCapability.RECOMMENDATION_CANDIDATES,
                PluginCapability.LOCAL_HISTORY_READ
            )
        )

        val encoded = Json.encodeToString(manifest)
        val decoded = Json.decodeFromString<PluginCapabilityManifest>(encoded)

        assertEquals(manifest, decoded)
    }

    @Test
    fun capabilityEnumUsesStableSerializedNames() {
        val encoded = Json.encodeToString(PluginCapability.NETWORK)

        assertEquals("\"NETWORK\"", encoded)
    }
}
