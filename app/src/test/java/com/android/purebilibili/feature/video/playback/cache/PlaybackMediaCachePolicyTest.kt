package com.android.purebilibili.feature.video.playback.cache

import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import com.android.purebilibili.core.player.buildPlaybackCacheKey
import com.android.purebilibili.core.player.createLazyCacheDataSourceFactory
import com.android.purebilibili.core.player.resolvePlaybackMediaCacheMaxBytes
import com.android.purebilibili.core.player.shouldUsePlaybackMediaCache
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(UnstableApi::class)
class PlaybackMediaCachePolicyTest {

    @Test
    fun cacheKeyPrefersExplicitKey() {
        val uri = "https://upos-sz-mirrorcos.bilivideo.com/video.m4s?deadline=1&sig=abc"

        assertEquals(
            "explicit-video-key",
            buildPlaybackCacheKey(rawUri = uri, explicitKey = "explicit-video-key")
        )
    }

    @Test
    fun cacheKeyDropsSignedQueryForHttpMediaUrl() {
        val first = "https://upos-sz-mirrorcos.bilivideo.com/path/video.m4s?deadline=1&sig=abc"
        val second = "https://upos-sz-mirrorcos.bilivideo.com/path/video.m4s?deadline=2&sig=def"

        assertEquals(
            buildPlaybackCacheKey(rawUri = first, explicitKey = null),
            buildPlaybackCacheKey(rawUri = second, explicitKey = null)
        )
        assertEquals(
            "https://upos-sz-mirrorcos.bilivideo.com/path/video.m4s",
            buildPlaybackCacheKey(rawUri = first, explicitKey = null)
        )
    }

    @Test
    fun cacheKeyKeepsDifferentPathsSeparate() {
        val video = "https://upos-sz-mirrorcos.bilivideo.com/video/track.m4s?sig=abc"
        val audio = "https://upos-sz-mirrorcos.bilivideo.com/audio/track.m4s?sig=abc"

        assertTrue(
            buildPlaybackCacheKey(rawUri = video, explicitKey = null) !=
                buildPlaybackCacheKey(rawUri = audio, explicitKey = null)
        )
    }

    @Test
    fun cacheKeyFallsBackToStringForHostlessUri() {
        val uri = "content://media/external/video/media/42"

        assertEquals(
            "content://media/external/video/media/42",
            buildPlaybackCacheKey(rawUri = uri, explicitKey = null)
        )
    }

    @Test
    fun playbackMediaCacheOnlyAppliesToHttpStreams() {
        assertTrue(shouldUsePlaybackMediaCache("https://example.com/video.m4s"))
        assertTrue(shouldUsePlaybackMediaCache("http://example.com/video.m4s"))
        assertFalse(shouldUsePlaybackMediaCache("file:///tmp/local.mpd"))
        assertFalse(shouldUsePlaybackMediaCache("content://media/external/video/media/42"))
    }

    @Test
    fun playbackMediaCacheBudgetIsFixedTo512MiB() {
        assertEquals(
            512L * 1024L * 1024L,
            resolvePlaybackMediaCacheMaxBytes()
        )
    }

    @Test
    fun cachedDataSourceIsResolvedOnlyWhenOpened() {
        var cacheResolveCount = 0
        val upstream = RecordingDataSource()
        val cachedUri = mockk<Uri>()
        val cached = RecordingDataSource(openLength = 17L, responseUri = cachedUri)
        val factory = createLazyCacheDataSourceFactory(
            upstreamFactory = DataSource.Factory { upstream },
            cachedDataSourceProvider = {
                cacheResolveCount += 1
                cached
            }
        )

        assertEquals(0, cacheResolveCount)
        val source = factory.createDataSource()
        assertEquals(0, cacheResolveCount)
        val listener = object : TransferListener {
            override fun onTransferInitializing(
                source: DataSource,
                dataSpec: DataSpec,
                isNetwork: Boolean
            ) = Unit

            override fun onTransferStart(
                source: DataSource,
                dataSpec: DataSpec,
                isNetwork: Boolean
            ) = Unit

            override fun onBytesTransferred(
                source: DataSource,
                dataSpec: DataSpec,
                isNetwork: Boolean,
                bytesTransferred: Int
            ) = Unit

            override fun onTransferEnd(
                source: DataSource,
                dataSpec: DataSpec,
                isNetwork: Boolean
            ) = Unit
        }
        source.addTransferListener(listener)

        val spec = mockk<DataSpec>()
        assertEquals(17L, source.open(spec))

        assertEquals(1, cacheResolveCount)
        assertEquals(1, cached.openCount)
        assertEquals(0, upstream.openCount)
        assertTrue(cached.hasTransferListener(listener))
        assertTrue(source.uri === cachedUri)
        assertEquals(mapOf("X-Test" to listOf("cached")), source.responseHeaders)

        source.close()
        assertEquals(1, cached.closeCount)
    }

    @Test
    fun failedCacheResolutionFallsBackToUpstreamAtOpen() {
        var cacheResolveCount = 0
        val upstream = RecordingDataSource(openLength = 23L)
        val factory = createLazyCacheDataSourceFactory(
            upstreamFactory = DataSource.Factory { upstream },
            cachedDataSourceProvider = {
                cacheResolveCount += 1
                null
            }
        )

        val source = factory.createDataSource()
        assertEquals(0, cacheResolveCount)
        assertEquals(
            23L,
            source.open(mockk<DataSpec>())
        )

        assertEquals(1, cacheResolveCount)
        assertEquals(1, upstream.openCount)
        assertEquals("upstream", source.responseHeaders["X-Test"]?.single())
        source.close()
        assertEquals(1, upstream.closeCount)
    }

    private class RecordingDataSource(
        private val openLength: Long = 0L,
        private val responseUri: Uri? = null
    ) : DataSource {
        var openCount = 0
            private set
        var closeCount = 0
            private set
        private val listeners = mutableSetOf<TransferListener>()

        fun hasTransferListener(listener: TransferListener): Boolean = listener in listeners

        override fun open(dataSpec: DataSpec): Long {
            openCount += 1
            return openLength
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int = -1

        override fun getUri(): Uri? = responseUri

        override fun getResponseHeaders(): Map<String, List<String>> = mapOf(
            "X-Test" to listOf(if (openLength == 17L) "cached" else "upstream")
        )

        override fun addTransferListener(transferListener: TransferListener) {
            listeners += transferListener
        }

        override fun close() {
            closeCount += 1
        }
    }
}
