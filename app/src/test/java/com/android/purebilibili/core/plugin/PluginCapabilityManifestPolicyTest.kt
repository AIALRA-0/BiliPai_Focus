package com.android.purebilibili.core.plugin

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PluginCapabilityManifestPolicyTest {

    @Test
    fun networkAndStorageCapabilities_requireExplicitGrant() {
        val manifest = PluginCapabilityManifest(
            pluginId = "open.recommendation.cloud",
            displayName = "云端推荐",
            version = "1.0.0",
            apiVersion = 1,
            entryClassName = "dev.example.CloudRecommendationPlugin",
            capabilities = setOf(
                PluginCapability.RECOMMENDATION_CANDIDATES,
                PluginCapability.LOCAL_HISTORY_READ,
                PluginCapability.NETWORK,
                PluginCapability.PLUGIN_STORAGE
            )
        )

        val grants = resolvePluginCapabilityGrants(
            manifest = manifest,
            userApprovedCapabilities = setOf(
                PluginCapability.RECOMMENDATION_CANDIDATES,
                PluginCapability.LOCAL_HISTORY_READ
            )
        )

        assertTrue(grants.isGranted(PluginCapability.RECOMMENDATION_CANDIDATES))
        assertTrue(grants.isGranted(PluginCapability.LOCAL_HISTORY_READ))
        assertFalse(grants.isGranted(PluginCapability.NETWORK))
        assertFalse(grants.isGranted(PluginCapability.PLUGIN_STORAGE))
    }

    @Test
    fun missingRequiredRecommendationCapabilities_blocksRecommendationApi() {
        val grants = PluginCapabilityGrants(
            granted = setOf(PluginCapability.LOCAL_HISTORY_READ)
        )

        val decision = validateRecommendationPluginAccess(grants)

        assertEquals(
            RecommendationPluginAccessDecision.MissingCapabilities(
                setOf(PluginCapability.RECOMMENDATION_CANDIDATES)
            ),
            decision
        )
    }

    @Test
    fun capabilityManifest_roundTripsAsJsonForExternalPluginPackages() {
        val manifest = PluginCapabilityManifest(
            pluginId = "dev.example.today_watch_remix",
            displayName = "今日推荐单 Remix",
            version = "1.2.0",
            apiVersion = 1,
            entryClassName = "dev.example.TodayWatchRemixPlugin",
            capabilities = setOf(
                PluginCapability.RECOMMENDATION_CANDIDATES,
                PluginCapability.LOCAL_HISTORY_READ
            )
        )

        val encoded = Json.encodeToString(PluginCapabilityManifest.serializer(), manifest)
        val decoded = Json.decodeFromString(PluginCapabilityManifest.serializer(), encoded)

        assertEquals(manifest, decoded)
    }
}
