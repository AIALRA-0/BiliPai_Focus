package com.android.purebilibili.plugin.sdk

interface PlayerPluginApi {
    val capabilityManifest: PluginCapabilityManifest

    suspend fun onVideoLoad(bvid: String, cid: Long)

    suspend fun onPositionUpdate(positionMs: Long): SkipAction?

    fun onUserSeek(positionMs: Long) {}

    fun onVideoEnd() {}
}

sealed class SkipAction {
    data object None : SkipAction()

    data class SkipTo(
        val positionMs: Long,
        val reason: String
    ) : SkipAction()

    data class ShowButton(
        val skipToMs: Long,
        val label: String,
        val segmentId: String
    ) : SkipAction()
}
