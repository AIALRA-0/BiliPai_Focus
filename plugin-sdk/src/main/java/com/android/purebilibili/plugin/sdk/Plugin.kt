package com.android.purebilibili.plugin.sdk

/** Shared identity and application-wide lifecycle for every plugin capability. */
interface Plugin {
    val capabilityManifest: PluginCapabilityManifest
    val id: String get() = capabilityManifest.pluginId
    val name: String get() = capabilityManifest.displayName
    val version: String get() = capabilityManifest.version
    suspend fun onEnable() {}
    suspend fun onDisable() {}
}
