package com.android.purebilibili.core.plugin

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CastPluginApiTest {

    private fun stubPlugin(): CastPluginApi = object : CastPluginApi {
        override val id = "test"
        override val name = "Test"
        override val description = "Test plugin"
        override val version = "1.0"
        override val routes = MutableStateFlow(emptyList<CastPluginRoute>())
        override fun startRouteDiscovery(context: Context) {}
        override fun stopRouteDiscovery() {}
        override suspend fun cast(
            context: Context,
            route: CastPluginRoute,
            media: CastPluginMediaRequest
        ): Result<Unit> = Result.failure(UnsupportedOperationException())
    }

    @Test
    fun `default playback state is inactive`() {
        val api = stubPlugin()
        assertFalse(api.playbackState.value.isActive)
    }

    @Test
    fun `default playback state is stable singleton`() {
        val api = stubPlugin()
        val first = api.playbackState
        val second = api.playbackState
        assertSame(first, second)
    }

    @Test
    fun `default playback state has zeroed position and duration`() {
        val api = stubPlugin()
        val state = api.playbackState.value
        assertEquals(0L, state.currentPositionMs)
        assertEquals(0L, state.durationMs)
        assertEquals(0L, state.bufferedPositionMs)
    }

    @Test
    fun `CastPluginMediaRequest default startPositionMs is 0`() {
        val request = CastPluginMediaRequest(url = "https://example.com/video.mp4", title = "Test")
        assertEquals(0L, request.startPositionMs)
    }

    @Test
    fun `CastPluginMediaRequest default autoplay is true`() {
        val request = CastPluginMediaRequest(url = "https://example.com/video.mp4", title = "Test")
        assertTrue(request.autoplay)
    }
}
