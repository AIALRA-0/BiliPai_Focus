package com.android.purebilibili.plugin.sdk

import kotlinx.serialization.Serializable

@Serializable
enum class PluginCapability {
    PLAYER_STATE,
    PLAYER_CONTROL,
    DANMAKU_STREAM,
    DANMAKU_MUTATION,
    PLAYBACK_CDN,
    RECOMMENDATION_CANDIDATES,
    LOCAL_HISTORY_READ,
    LOCAL_FEEDBACK_READ,
    NETWORK,
    PLUGIN_STORAGE,
    EXTERNAL_MEDIA_PLAYBACK,
    FEED_SOURCE
}

@Serializable
data class PluginCapabilityManifest(
    val pluginId: String,
    val displayName: String,
    val version: String,
    val apiVersion: Int,
    val entryClassName: String,
    val capabilities: Set<PluginCapability>
)
