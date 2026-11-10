package com.android.purebilibili.plugin.sdk

/** Application-wide definition; each player/video owns an independent session. */
interface PlayerPluginApi : Plugin {
    fun createSession(): PlayerPluginSession
}

/** Mutable playback state belongs here, never on the shared plugin definition. */
interface PlayerPluginSession {
    suspend fun onVideoLoad(bvid: String, cid: Long)

    suspend fun onPositionUpdate(positionMs: Long): SkipAction?

    fun onUserSeek(positionMs: Long) {}

    fun onVideoEnd() {}
}

sealed class SkipAction {
    data object None : SkipAction()

    data class SkipTo(
        val positionMs: Long,
        val reason: String,
        val segmentId: String? = null,
        val startMs: Long? = null,
        val category: String? = null,
        val categoryName: String? = null,
        val showToast: Boolean = true,
    ) : SkipAction()

    data class ShowButton(
        val skipToMs: Long,
        val label: String,
        val segmentId: String
    ) : SkipAction()

    data class Mute(
        val untilMs: Long,
        val reason: String,
        val segmentId: String? = null,
        val showToast: Boolean = true,
    ) : SkipAction()
}
