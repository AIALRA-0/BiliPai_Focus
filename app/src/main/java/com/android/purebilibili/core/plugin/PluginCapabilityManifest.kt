package com.android.purebilibili.core.plugin

typealias PluginCapability = com.android.purebilibili.plugin.sdk.PluginCapability
typealias PluginCapabilityManifest = com.android.purebilibili.plugin.sdk.PluginCapabilityManifest

data class PluginCapabilityGrants(
    val granted: Set<PluginCapability>
) {
    fun isGranted(capability: PluginCapability): Boolean = capability in granted
}

fun resolvePluginCapabilityGrants(
    manifest: PluginCapabilityManifest,
    userApprovedCapabilities: Set<PluginCapability>
): PluginCapabilityGrants {
    return PluginCapabilityGrants(
        granted = manifest.capabilities.intersect(userApprovedCapabilities)
    )
}

sealed interface RecommendationPluginAccessDecision {
    data object Granted : RecommendationPluginAccessDecision
    data class MissingCapabilities(
        val missing: Set<PluginCapability>
    ) : RecommendationPluginAccessDecision
}

fun validateRecommendationPluginAccess(
    grants: PluginCapabilityGrants
): RecommendationPluginAccessDecision {
    val required = setOf(PluginCapability.RECOMMENDATION_CANDIDATES)
    val missing = required.filterNot { grants.isGranted(it) }.toSet()
    return if (missing.isEmpty()) {
        RecommendationPluginAccessDecision.Granted
    } else {
        RecommendationPluginAccessDecision.MissingCapabilities(missing)
    }
}
