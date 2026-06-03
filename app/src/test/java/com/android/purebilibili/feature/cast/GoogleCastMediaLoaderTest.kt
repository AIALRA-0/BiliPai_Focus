package com.android.purebilibili.feature.plugin.googlecast

import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleCastMediaLoaderTest {

    private fun requireMediaInfo(request: MediaLoadRequestData): MediaInfo = requireNotNull(request.mediaInfo)
    private fun requireMetadata(request: MediaLoadRequestData): MediaMetadata = requireNotNull(requireMediaInfo(request).metadata)

    @Test
    fun `buildMediaLoadRequest sets URL on MediaInfo`() {
        val request = GoogleCastMediaLoader.buildMediaLoadRequest(
            url = "https://example.com/video.mp4",
            title = "Test Video"
        )

        assertEquals("https://example.com/video.mp4", requireMediaInfo(request).contentId)
    }

    @Test
    fun `buildMediaLoadRequest uses default content type video mp4`() {
        val request = GoogleCastMediaLoader.buildMediaLoadRequest(
            url = "https://example.com/video.mp4",
            title = "Test Video"
        )

        assertEquals("video/mp4", requireMediaInfo(request).contentType)
    }

    @Test
    fun `buildMediaLoadRequest sets buffered stream type`() {
        val request = GoogleCastMediaLoader.buildMediaLoadRequest(
            url = "https://example.com/video.mp4",
            title = "Test Video"
        )

        assertEquals(MediaInfo.STREAM_TYPE_BUFFERED, requireMediaInfo(request).streamType)
    }

    @Test
    fun `buildMediaLoadRequest sets autoplay to true`() {
        val request = GoogleCastMediaLoader.buildMediaLoadRequest(
            url = "https://example.com/video.mp4",
            title = "Test Video"
        )

        assertEquals(true, request.autoplay)
    }

    @Test
    fun `buildMediaLoadRequest accepts startPositionMs and sets it on currentTime`() {
        val request = GoogleCastMediaLoader.buildMediaLoadRequest(
            url = "https://example.com/video.mp4",
            title = "Test Video",
            startPositionMs = 15000L
        )

        assertEquals(15000L, request.currentTime)
    }

    @Test
    fun `buildMediaLoadRequest accepts autoplay false`() {
        val request = GoogleCastMediaLoader.buildMediaLoadRequest(
            url = "https://example.com/video.mp4",
            title = "Test Video",
            autoplay = false
        )

        assertEquals(false, request.autoplay)
    }

    @Test
    fun `buildMediaLoadRequest accepts custom content type`() {
        val request = GoogleCastMediaLoader.buildMediaLoadRequest(
            url = "https://example.com/stream.m3u8",
            title = "HLS Stream",
            contentType = "application/x-mpegURL"
        )

        assertEquals("application/x-mpegURL", requireMediaInfo(request).contentType)
    }

    @Test
    fun `buildMediaLoadRequest preserves application dash xml content type for DASH manifests`() {
        val request = GoogleCastMediaLoader.buildMediaLoadRequest(
            url = "https://example.com/manifest.mpd",
            title = "DASH Stream",
            contentType = "application/dash+xml"
        )

        assertEquals("application/dash+xml", requireMediaInfo(request).contentType)
    }

    @Test
    fun `buildMediaLoadRequest creates metadata of movie type`() {
        val request = GoogleCastMediaLoader.buildMediaLoadRequest(
            url = "https://example.com/video.mp4",
            title = "Test"
        )

        assertEquals(MediaMetadata.MEDIA_TYPE_MOVIE, requireMetadata(request).mediaType)
    }

    @Test
    fun `resolveGoogleCastMediaMetadata sets title`() {
        val policy = GoogleCastMediaLoader.resolveGoogleCastMediaMetadata(
            title = "My Video Title",
            creator = ""
        )

        assertEquals("My Video Title", policy.title)
    }

    @Test
    fun `resolveGoogleCastMediaMetadata sets creator as subtitle`() {
        val policy = GoogleCastMediaLoader.resolveGoogleCastMediaMetadata(
            title = "My Video",
            creator = "Channel Name"
        )

        assertEquals("Channel Name", policy.subtitle)
    }

    @Test
    fun `resolveGoogleCastMediaMetadata does not set subtitle when creator is blank`() {
        val policy = GoogleCastMediaLoader.resolveGoogleCastMediaMetadata(
            title = "My Video",
            creator = "   "
        )

        assertNull(policy.subtitle)
    }

    @Test
    fun `resolveGoogleCastMediaMetadata does not set subtitle when creator is empty`() {
        val policy = GoogleCastMediaLoader.resolveGoogleCastMediaMetadata(
            title = "My Video",
            creator = ""
        )

        assertNull(policy.subtitle)
    }

    @Test
    fun `resolveGoogleCastMediaMetadata uses BiliPai Video fallback for blank title`() {
        val policy = GoogleCastMediaLoader.resolveGoogleCastMediaMetadata(
            title = "   ",
            creator = ""
        )

        assertEquals("BiliPai Video", policy.title)
    }

    @Test
    fun `resolveGoogleCastMediaMetadata uses BiliPai Video fallback for empty title`() {
        val policy = GoogleCastMediaLoader.resolveGoogleCastMediaMetadata(
            title = "",
            creator = ""
        )

        assertEquals("BiliPai Video", policy.title)
    }

    // --- session wait policy ---

    @Test
    fun `shouldContinueWaiting with no session and no remote client within timeout`() {
        assertTrue(
            GoogleCastMediaLoader.shouldContinueWaitingForSession(
                sessionExists = false,
                remoteClientReady = false,
                elapsedMs = 1_000L,
                timeoutMs = 5_000L
            )
        )
    }

    @Test
    fun `shouldContinueWaiting with session but no remote client within timeout`() {
        assertTrue(
            GoogleCastMediaLoader.shouldContinueWaitingForSession(
                sessionExists = true,
                remoteClientReady = false,
                elapsedMs = 3_000L,
                timeoutMs = 5_000L
            )
        )
    }

    @Test
    fun `shouldStopWaiting when session and remote client both ready`() {
        assertFalse(
            GoogleCastMediaLoader.shouldContinueWaitingForSession(
                sessionExists = true,
                remoteClientReady = true,
                elapsedMs = 1_000L,
                timeoutMs = 5_000L
            )
        )
    }

    @Test
    fun `shouldStopWaiting when timeout exceeded even with no remote client`() {
        assertFalse(
            GoogleCastMediaLoader.shouldContinueWaitingForSession(
                sessionExists = true,
                remoteClientReady = false,
                elapsedMs = 5_000L,
                timeoutMs = 5_000L
            )
        )
    }

    @Test
    fun `shouldStopWaiting when timeout exceeded with no session`() {
        assertFalse(
            GoogleCastMediaLoader.shouldContinueWaitingForSession(
                sessionExists = false,
                remoteClientReady = false,
                elapsedMs = 10_001L,
                timeoutMs = 5_000L
            )
        )
    }
}
