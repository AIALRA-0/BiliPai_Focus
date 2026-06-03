package com.android.purebilibili.core.plugin

import android.content.Context
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CastPluginRoute(
    val routeId: String,
    val name: String,
    val description: String? = null,
    val icon: ImageVector? = null
)

data class CastPluginMediaRequest(
    val url: String,
    val title: String,
    val creator: String = "",
    val contentType: String = "video/mp4",
    val startPositionMs: Long = 0L,
    val autoplay: Boolean = true
)

data class CastPluginPlaybackState(
    val isActive: Boolean = false,
    val deviceLabel: String = "",
    val title: String = "",
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val canSeek: Boolean = true
)

private val defaultPlaybackState: StateFlow<CastPluginPlaybackState> =
    MutableStateFlow(CastPluginPlaybackState()).asStateFlow()

private val defaultIsDiscovering: StateFlow<Boolean> =
    MutableStateFlow(false).asStateFlow()

interface CastPluginApi : Plugin {
    val routes: StateFlow<List<CastPluginRoute>>
    val playbackState: StateFlow<CastPluginPlaybackState>
        get() = defaultPlaybackState
    val isDiscovering: StateFlow<Boolean>
        get() = defaultIsDiscovering

    fun startRouteDiscovery(context: Context)
    fun stopRouteDiscovery()
    fun refreshRouteDiscovery(context: Context) {
        startRouteDiscovery(context)
    }
    suspend fun cast(
        context: Context,
        route: CastPluginRoute,
        media: CastPluginMediaRequest
    ): Result<Unit>

    suspend fun play(): Result<Unit> =
        Result.failure(UnsupportedOperationException("播放控制不支持"))

    suspend fun pause(): Result<Unit> =
        Result.failure(UnsupportedOperationException("暂停控制不支持"))

    suspend fun seek(positionMs: Long): Result<Unit> =
        Result.failure(UnsupportedOperationException("进度控制不支持"))
}
