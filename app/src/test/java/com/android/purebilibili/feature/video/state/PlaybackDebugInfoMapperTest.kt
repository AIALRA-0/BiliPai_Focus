package com.android.purebilibili.feature.video.state

import androidx.media3.common.Player
import androidx.media3.common.Format
import com.android.purebilibili.feature.video.ui.overlay.PlaybackDebugInfo
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackDebugInfoMapperTest {

    @Test
    fun applyVideoFormatDebugInfo_formatsResolutionCodecBitrateAndFrameRate() {
        val result = applyVideoFormatDebugInfo(
            current = PlaybackDebugInfo(),
            format = Format.Builder()
                .setWidth(1920)
                .setHeight(1080)
                .setSampleMimeType("video/hevc")
                .setPeakBitrate(8_400_000)
                .setFrameRate(59.94f)
                .build(),
            decoderName = "c2.qti.hevc.decoder"
        )

        assertEquals("1920 x 1080", result.resolution)
        assertEquals("8.4 Mbps", result.videoBitrate)
        assertEquals("HEVC", result.videoCodec)
        assertEquals("59.94 fps", result.frameRate)
        assertEquals("c2.qti.hevc.decoder", result.videoDecoder)
    }

    @Test
    fun applyAudioFormatDebugInfo_formatsCodecAndBitrate() {
        val result = applyAudioFormatDebugInfo(
            current = PlaybackDebugInfo(),
            format = Format.Builder()
                .setSampleMimeType("audio/mp4a-latm")
                .setPeakBitrate(192_000)
                .build(),
            decoderName = "c2.android.aac.decoder"
        )

        assertEquals("192 kbps", result.audioBitrate)
        assertEquals("AAC", result.audioCodec)
        assertEquals("c2.android.aac.decoder", result.audioDecoder)
    }

    @Test
    fun applyPlaybackStateDebugInfo_formatsStateFlags() {
        val result = applyPlaybackStateDebugInfo(
            current = PlaybackDebugInfo(),
            playbackState = Player.STATE_READY,
            playWhenReady = true,
            isPlaying = false
        )

        assertEquals("READY", result.playbackState)
        assertEquals("true", result.playWhenReady)
        assertEquals("false", result.isPlaying)
    }

    @Test
    fun applyRenderedFirstFrameDebugInfo_marksFirstFrameAndLastVideoEvent() {
        val result = applyRenderedFirstFrameDebugInfo(
            current = PlaybackDebugInfo()
        )

        assertEquals("rendered", result.firstFrame)
        assertEquals("first frame rendered", result.lastVideoEvent)
    }

    @Test
    fun applyDroppedVideoFramesDebugInfo_accumulatesFrameDrops() {
        val result = applyDroppedVideoFramesDebugInfo(
            current = PlaybackDebugInfo(droppedFrames = "5"),
            droppedFrameCount = 7
        )

        assertEquals("12", result.droppedFrames)
        assertEquals("dropped 7 frames", result.lastVideoEvent)
    }

    @Test
    fun applyBandwidthEstimateAndPlaybackEvents_formatReadableDiagnostics() {
        val withBandwidth = applyBandwidthEstimateDebugInfo(
            current = PlaybackDebugInfo(),
            bitrateEstimate = 8_600_000L
        )
        val withEvents = applyAudioEventDebugInfo(
            current = applyVideoEventDebugInfo(
                current = withBandwidth,
                eventSummary = "video decoder initialized"
            ),
            eventSummary = "audio sink recovered"
        )

        assertEquals("8.6 Mbps", withEvents.bandwidthEstimate)
        assertEquals("video decoder initialized", withEvents.lastVideoEvent)
        assertEquals("audio sink recovered", withEvents.lastAudioEvent)
    }
}
